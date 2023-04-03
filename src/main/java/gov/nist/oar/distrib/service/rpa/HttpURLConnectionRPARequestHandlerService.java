package gov.nist.oar.distrib.service.rpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oar.distrib.service.rpa.exceptions.InternalServerErrorException;
import gov.nist.oar.distrib.service.rpa.exceptions.InvalidRecaptchaException;
import gov.nist.oar.distrib.service.rpa.exceptions.InvalidRequestException;
import gov.nist.oar.distrib.service.rpa.exceptions.RecaptchaClientException;
import gov.nist.oar.distrib.service.rpa.exceptions.RecaptchaServerException;
import gov.nist.oar.distrib.service.rpa.exceptions.RecaptchaVerificationFailedException;
import gov.nist.oar.distrib.service.rpa.exceptions.RecordNotFoundException;
import gov.nist.oar.distrib.service.rpa.exceptions.RequestProcessingException;
import gov.nist.oar.distrib.service.rpa.model.JWTToken;
import gov.nist.oar.distrib.service.rpa.model.RecaptchaResponse;
import gov.nist.oar.distrib.service.rpa.model.Record;
import gov.nist.oar.distrib.service.rpa.model.RecordStatus;
import gov.nist.oar.distrib.service.rpa.model.RecordWrapper;
import gov.nist.oar.distrib.service.rpa.model.UserInfoWrapper;
import gov.nist.oar.distrib.web.RPAConfiguration;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * An implementation of the RPARequestHandlerService that uses HttpURLConnection to send HTTP requests and
 * receives responses from the Salesforce service.
 */
public class HttpURLConnectionRPARequestHandlerService implements IRPARequestHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpURLConnectionRPARequestHandlerService.class);

    /**
     * The key for the getRecord endpoint in the Salesforce endpoints configuration.
     */
    private final static String GET_RECORD_ENDPOINT_KEY = "get-record-endpoint";

    /**
     * The key for the createRecord endpoint in the Salesforce endpoints configuration.
     */
    private final static String CREATE_RECORD_ENDPOINT_KEY = "create-record-endpoint";

    /**
     * The key for the updateRecord endpoint in the Salesforce endpoints configuration.
     */
    private final static String UPDATE_RECORD_ENDPOINT_KEY = "update-record-endpoint";

    /**
     * The RPA configuration.
     */
    private final RPAConfiguration rpaConfiguration;

    /**
     * The HTTPURLConnection factory.
     */
    private HttpURLConnectionFactory connectionFactory;

    /**
     * The JWT helper.
     */
    private JWTHelper jwtHelper;

    /**
     * The Recaptcha helper.
     */
    private RecaptchaHelper recaptchaHelper;

    /**
     * The record response handler.
     */
    private RecordResponseHandler recordResponseHandler;

    /**
     * Sets the HTTP URL connection factory.
     *
     * @param connectionFactory The HTTP URL connection factory to set.
     */
    public void setHttpURLConnectionFactory(HttpURLConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Sets the JWT helper.
     *
     * @param jwtHelper The JWT helper to set.
     */
    public void setJWTHelper(JWTHelper jwtHelper) {
        this.jwtHelper = jwtHelper;
    }

    /**
     * Sets the Recaptcha helper.
     *
     * @param recaptchaHelper The Recaptcha helper to set.
     */
    public void setRecaptchaHelper(RecaptchaHelper recaptchaHelper) {
        this.recaptchaHelper = recaptchaHelper;
    }


    /**
     * Sets the RecordResponseHandler instance to handle the response of the record operations.
     *
     * @param recordResponseHandler the RecordResponseHandler instance to set
     */
    public void setRecordResponseHandler(RecordResponseHandler recordResponseHandler) {
        this.recordResponseHandler = recordResponseHandler;
    }

    /**
     * Constructs a new instance of the service using the given RPA configuration.
     *
     * @param rpaConfiguration The RPA configuration to use for this service.
     */
    public HttpURLConnectionRPARequestHandlerService(RPAConfiguration rpaConfiguration) {
        // Initialize instance variables
        this.rpaConfiguration = rpaConfiguration;
        this.connectionFactory = url -> (HttpURLConnection) url.openConnection();

        // Initialize JWT helper
        this.jwtHelper = JWTHelper.getInstance();
        this.jwtHelper.setKeyRetriever(new JKSKeyRetriever());
        this.jwtHelper.setConfig(rpaConfiguration);
        this.jwtHelper.setHttpURLConnectionFactory(this.connectionFactory);

        // Initialize Recaptcha helper
        this.recaptchaHelper = new RecaptchaHelper();
        this.recaptchaHelper.setHttpURLConnectionFactory(this.connectionFactory);

        // Set RecordResponseHandler
        this.recordResponseHandler = new RecordResponseHandlerImpl(this.rpaConfiguration, this.connectionFactory);

        // Log RPA configuration coming from the config server
        LOGGER.debug("RPA_CONFIGURATION=" + this.rpaConfiguration.toString());
    }

    public RPAConfiguration getConfig() {
        return this.rpaConfiguration;
    }

    /**
     * Retrieves a record from Salesforce with the given record ID.
     *
     * @param recordId The ID of the record to retrieve.
     * @return The RecordWrapper object that contains the retrieved record.
     * @throws RecordNotFoundException    If the specified record was not found.
     * @throws RequestProcessingException If there was an error retrieving the record.
     */
    @Override
    public RecordWrapper getRecord(String recordId) throws RecordNotFoundException, RequestProcessingException {
        // Get the endpoint path for retrieving records
        String getRecordUri = getConfig().getSalesforceEndpoints().get(GET_RECORD_ENDPOINT_KEY);
        // Retrieve the JWTToken
        JWTToken token = jwtHelper.getToken();
        // Build the URL for this request
        String url;
        try {
            url = new URIBuilder(token.getInstanceUrl())
                    .setPath(getRecordUri + "/" + recordId)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new RequestProcessingException("Error building URI: " + e.getMessage());
        }

        // Create the HttpURLConnection and send the GET request
        RecordWrapper recordWrapper;
        HttpURLConnection connection = null;
        try {
            URL requestUrl = new URL(url);
            connection = connectionFactory.createHttpURLConnection(requestUrl);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token.getAccessToken());

            int responseCode = connection.getResponseCode();

            // Check if request is successful
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    // Handle the response
                    recordWrapper = new ObjectMapper().readValue(response.toString(), RecordWrapper.class);
                }
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // Handle the error HTTP_NOT_FOUND response
                throw RecordNotFoundException.fromRecordId(recordId);
            } else {
                // Handle any other error response
                throw new RequestProcessingException("Error response from salesforce service: " + connection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            // Handle the URL Malformed error
            LOGGER.debug("Invalid URL: " + e.getMessage());
            throw new RequestProcessingException("Invalid URL: " + e.getMessage());
        } catch (IOException e) {
            // Handle the I/O error
            LOGGER.debug("Error sending GET request: " + e.getMessage());
            throw new RequestProcessingException("I/O error: " + e.getMessage());
        } finally {
            // Close the connection
            if (connection != null) {
                connection.disconnect();
            }
        }
        return recordWrapper;
    }

    /**
     * Creates a new record in Salesforce using the information from the provided UserInfoWrapper object.
     *
     * @param userInfoWrapper The UserInfoWrapper object containing the data to create the new record.
     * @return The RecordWrapper object containing the newly created record.
     * @throws InvalidRecaptchaException If the reCAPTCHA verification fails.
     * @throws InvalidRequestException   If the request is invalid or incomplete.
     */
    @Override
    public RecordWrapper createRecord(UserInfoWrapper userInfoWrapper) throws InvalidRequestException,
            RequestProcessingException, RecaptchaVerificationFailedException {
        int responseCode;
        // Initialize return value
        RecordWrapper newRecordWrapper;
        // First, we verify the reCAPTCHA
        String recaptchaToken = userInfoWrapper.getRecaptcha();
        RecaptchaResponse recaptchaResponse;
        try {
            recaptchaResponse = verifyRecaptcha(recaptchaToken);
        } catch (RecaptchaServerException e) {
            // if error is between our service and Google reCAPTCHA service
            throw new RequestProcessingException(e.getMessage());
        } catch (RecaptchaClientException e) {
            // if error is caused by end user (reCAPTCHA response invalid for example)
            throw new InvalidRequestException(e.getMessage());
        }
        // We proceed only if reCAPTCHA validation was successful
        if (recaptchaResponse.isSuccess()) {
            // Get path
            String createRecordUri = getConfig().getSalesforceEndpoints().get(CREATE_RECORD_ENDPOINT_KEY);

            // Get token
            JWTToken token = jwtHelper.getToken();

            // Build URL
            String url;
            try {
                url = new URIBuilder(token.getInstanceUrl())
                        .setPath(createRecordUri)
                        .build()
                        .toString();
            } catch (URISyntaxException e) {
                throw new RequestProcessingException("Error building URI: " + e.getMessage());
            }

            // Sanitize user input
            UserInfoWrapper cleanUserInfoWrapper = HTMLSanitizer.sanitize(userInfoWrapper);
            String postPayload;
            try {
                postPayload = new ObjectMapper().writeValueAsString(cleanUserInfoWrapper);
            } catch (JsonProcessingException e) {
                LOGGER.debug("Error while serializing user input: " + e.getMessage());
                throw new RequestProcessingException("Error while serializing user input: " + e.getMessage());
            }

            // Send POST request
            HttpURLConnection connection = null;

            try {
                URL requestUrl = new URL(url);
                connection = connectionFactory.createHttpURLConnection(requestUrl);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + token.getAccessToken());
                // Set payload
                byte[] payloadBytes = postPayload.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true); // tell connection we are writing data to the output stream
                OutputStream os = connection.getOutputStream();
                os.write(payloadBytes);
                os.flush();
                os.close();

                responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) { // If created
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;

                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        // Handle the response
                        newRecordWrapper = new ObjectMapper().readValue(response.toString(), RecordWrapper.class);
                    }
                } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) { // If bad request
                    LOGGER.debug("Invalid request: " + connection.getResponseMessage());
                    throw new InvalidRequestException("Invalid request: " + connection.getResponseMessage());
                } else {
                    // Handle any other error response
                    LOGGER.debug("Error response from Salesforce service: " + connection.getResponseMessage());
                    throw new RequestProcessingException("Error response from Salesforce service: " + connection.getResponseMessage());
                }

            } catch (MalformedURLException e) {
                // Handle the URL Malformed error
                LOGGER.debug("Invalid URL: " + e.getMessage());
                throw new RequestProcessingException("Invalid URL: " + e.getMessage());
            } catch (IOException e) {
                // Handle the I/O error
                LOGGER.debug("Error sending GET request: " + e.getMessage());
                throw new RequestProcessingException("I/O error: " + e.getMessage());
            } finally {
                // Close the connection
                if (connection != null) {
                    connection.disconnect();
                }
            }

        } else { // reCAPTCHA verification not successful
            throw new RecaptchaVerificationFailedException("reCAPTCHA verification failed");
        }

        // Check if success and handle accordingly
        if (newRecordWrapper != null) {
            this.recordResponseHandler.onRecordCreationSuccess(newRecordWrapper.getRecord());
        } else {
            // we expect a record to be created every time we call createRecord
            // if newRecordWrapped is null, it means creation failed
            this.recordResponseHandler.onRecordCreationFailure(responseCode);
        }

        return newRecordWrapper;
    }

    /**
     * Verifies the reCAPTCHA token.
     *
     * @param recaptchaToken the reCAPTCHA token to verify, this what the reCAPTCHA widget returns
     * @return the reCAPTCHA response
     * @throws RecaptchaServerException if there is an error while communicating with the Google reCAPTCHA service
     * @throws RecaptchaClientException if the reCAPTCHA client request is invalid
     */
    private RecaptchaResponse verifyRecaptcha(String recaptchaToken) throws RecaptchaServerException,
            RecaptchaClientException {
        RecaptchaResponse recaptchaResponse = recaptchaHelper.verifyRecaptcha(
                getConfig().getRecaptchaSecret(),
                recaptchaToken
        );
        LOGGER.debug("reCAPTCHA Google Response: " + recaptchaResponse.toString());
        return recaptchaResponse;
    }

    /**
     * Updates the status of a record with a given ID.
     *
     * @param recordId The ID of the record to update.
     * @param status   The status to update the record with.
     * @return The {@link RecordStatus} object representing the updated record status.
     * @throws RecordNotFoundException    If the record with the given ID is not found.
     * @throws InvalidRequestException    If the request is invalid.
     * @throws RequestProcessingException If there is an error processing the request.
     */
    @Override
    public RecordStatus updateRecord(String recordId, String status) throws RecordNotFoundException,
            InvalidRequestException
            , RequestProcessingException {

        // Initialize return object
        RecordStatus recordStatus;

        // Get endpoint
        String updateRecordUri = getConfig().getSalesforceEndpoints().get(UPDATE_RECORD_ENDPOINT_KEY);

        // PATCH request payload
        // Approval_Status__c is how SF service expect the key
        String patchPayload = new JSONObject().put("Approval_Status__c", status).toString();
        LOGGER.debug("PATCH_PAYLOAD=" + patchPayload);

        // Get token
        JWTToken token = jwtHelper.getToken();

        // Build request URL
        String url;
        try {
            url = new URIBuilder(token.getInstanceUrl())
                    .setPath(updateRecordUri)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new RequestProcessingException("Error building URI: " + e.getMessage());
        }

        // Send PATCH request
        HttpURLConnection connection = null;
        try {
            URL requestUrl = new URL(url);
            connection = connectionFactory.createHttpURLConnection(requestUrl);
            connection.setRequestMethod("PATCH");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token.getAccessToken());
            // Set payload
            byte[] payloadBytes = patchPayload.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true); // tell connection we are writing data to the output stream
            OutputStream os = connection.getOutputStream();
            os.write(payloadBytes);
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // If success
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    // Handle the response
                    recordStatus = new ObjectMapper().readValue(response.toString(), RecordStatus.class);
                }
            } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) { // If bad request
                LOGGER.debug("Invalid request: " + connection.getResponseMessage());
                throw new InvalidRequestException("Invalid request: " + connection.getResponseMessage());
            } else {
                // Handle any other error response
                LOGGER.debug("Error response from Salesforce service: " + connection.getResponseMessage());
                throw new RequestProcessingException("Error response from Salesforce service: " + connection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            // Handle the URL Malformed error
            LOGGER.debug("Invalid URL: " + e.getMessage());
            throw new RequestProcessingException("Invalid URL: " + e.getMessage());
        } catch (IOException e) {
            // Handle the I/O error
            LOGGER.debug("Error sending GET request: " + e.getMessage());
            throw new RequestProcessingException("I/O error: " + e.getMessage());
        } finally {
            // Close the connection
            if (connection != null) {
                connection.disconnect();
            }
        }

        // Retrieve updated record from SF service
        Record record = this.getRecord(recordId).getRecord();

        // Check if status is approved
        if (recordStatus.getApprovalStatus().toLowerCase().contains("approved")) {
            this.recordResponseHandler.onRecordUpdateApproved(record);
        } else {
            this.recordResponseHandler.onRecordUpdateDeclined(record);
        }

        return recordStatus;
    }


    /**
     * Sends an HTTP request to the specified URL with the given payload using the specified request method,
     * and returns the response as an instance of the given response type.
     *
     * TODO: refactor this class to use this method
     *
     * @param url          The URL to send the request to.
     * @param method       The HTTP request method to use (e.g. GET, POST, PUT, DELETE, etc.).
     * @param payload      The payload to send with the request (can be null if no payload is required).
     * @param responseType The class representing the expected response type.
     * @param <T>          The type of the expected response.
     * @return The response as an instance of the specified response type.
     *
     * @throws InvalidRequestException      If the request is invalid (e.g. missing required parameters).
     * @throws InternalServerErrorException If the server returns an error response that is not covered by
     *                                      any of the above exceptions.
     */
    @SuppressWarnings("unused")
    private <T> T sendHttpRequest(String url, String method, String payload, Class<T> responseType)
            throws InvalidRequestException, InternalServerErrorException {
        HttpURLConnection connection = null;
        try {
            URL requestUrl = new URL(url);
            connection = connectionFactory.createHttpURLConnection(requestUrl);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + jwtHelper.getToken().getAccessToken());

            if (payload != null) {
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }

                    return new ObjectMapper().readValue(response.toString(), responseType);
                }
            } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw new InvalidRequestException("Invalid request: " + connection.getResponseMessage());
            } else {
                throw new InternalServerErrorException("Error response from service: " + connection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            throw new InternalServerErrorException("Invalid URL: " + e.getMessage());
        } catch (IOException e) {
            throw new InternalServerErrorException("Error sending request: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
