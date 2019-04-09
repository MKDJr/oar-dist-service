/**
 * This software was developed at the National Institute of Standards and Technology by employees of
 * the Federal Government in the course of their official duties. Pursuant to title 17 Section 105
 * of the United States Code this software is not subject to copyright protection and is in the
 * public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its
 * use by other parties, and makes no guarantees, expressed or implied, about its quality,
 * reliability, or any other characteristic. We would appreciate acknowledgement if the software is
 * used. This software can be redistributed and/or modified freely provided that any derivative
 * works bear some notice that they are derived from it, and any modified versions bear some notice
 * that they have been modified.
 * @author: Deoyani Nandrekar-Heinis
 */
package gov.nist.oar.distrib.datapackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nist.oar.distrib.DistributionException;
import gov.nist.oar.distrib.InputLimitException;
import gov.nist.oar.distrib.web.objects.BundleRequest;
import gov.nist.oar.distrib.web.objects.FileRequest;

/**
 * @author Deoyani Nandrekar-Heinis
 *
 */
public class DefaultDataPackagerTest {

    private static long mxFileSize = 1000000;
    private static int numberofFiles = 100;
    private static FileRequest[] inputfileList = new FileRequest[2];
    private static BundleRequest bundleRequest;
    protected static Logger logger = LoggerFactory.getLogger(DefaultDataPackagerTest.class);
    DefaultDataPackager dp;
    private static String val1 = "";
    private static String val2 = "";

    public static void createInput() throws JsonParseException, JsonMappingException, IOException {

	val1 = "{\"filePath\":\"/1894/license.pdf\",\"downloadUrl\":\"https://s3.amazonaws.com/nist-midas/1894/license.pdf\"}";
	val2 = "{\"filePath\":\"/1894/license2.pdf\",\"downloadUrl\":\"https://s3.amazonaws.com/nist-midas/1894/license.pdf\"}";
	createBundle();
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
	createInput();
    }

    @Before
    public void construct() {
	dp = new DefaultDataPackager(inputfileList, mxFileSize, numberofFiles);
    }

    @Test
    public void testSize() throws MalformedURLException, IOException {
	assertEquals(dp.getTotalSize(), 62562);
    }

    @Test
    public void testNumOfFiles() throws IOException {
	assertEquals(dp.getFilesCount(), 2);
    }

    @Test
    public void testValidateRequest() throws DistributionException, MalformedURLException, IOException, InputLimitException {
	createInput();
	dp = new DefaultDataPackager(inputfileList, mxFileSize, numberofFiles);
	assertTrue(dp.getFilesCount() < numberofFiles);
	assertTrue(dp.getTotalSize() < mxFileSize);
	int countBefore = 2;
	dp.validateRequest();
	int countAfter = dp.getFilesCount();
	assertTrue("No duplicates!", countBefore == countAfter);
    }

    @Test
    public void testValidateBundleRequest() throws DistributionException, MalformedURLException, IOException, InputLimitException {
	val1 = "{\"filePath\":\"/1894/license.pdf\",\"downloadUrl\":\"https://s3.amazonaws.com/nist-midas/1894/license.pdf\"}";
	val2 = "{\"filePath\":\"/1894/license2.pdf\",\"downloadUrl\":\"https://s3.amazonaws.com/nist-midas/1894/license.pdf\"}";
	createBundle();
	int countBefore = 2;
	dp = new DefaultDataPackager(bundleRequest, mxFileSize, numberofFiles);
	dp.validateInput();
	dp.validateBundleRequest();
	assertTrue(dp.getFilesCount() < numberofFiles);
	assertTrue(dp.getTotalSize() < mxFileSize);
	int countAfter = dp.getFilesCount();
	assertTrue("No duplicates!", countBefore == countAfter);

    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testValidateBundleRequestError() throws DistributionException, MalformedURLException, IOException {
	val1 = "{\"filePath\":\"/1894/license.pdf\",\"jgkdfghjkdf\":\"https://s3.amazonaws.com/nist-midas/1894/license.pdf\"}";
	val2 = "{\"filePath\":\"/1894/license2.pdf\",\"downloadUrl\":\"https://s3.amazonaws.com/nist-midas/1894/license.pdf\"}";
	exception.expect(JsonMappingException.class);
	createBundle();
    }

    @Test
    public void TestErrorUrls() throws JsonParseException, JsonMappingException, IOException, InputLimitException {
	val1 = "{\"filePath\":\"/srd/srd13_B-049.json\",\"downloadUrl\":\"http://www.nist.gov/srd/srd_data/testfile.json\"}";
	val2 = "{\"filePath\":\"/srd/srd13_B-050.json\",\"downloadUrl\":\"http://www.nist.gov/srd/srd_data/testfile2.json\"}";
	createBundle();
	dp = new DefaultDataPackager(bundleRequest, mxFileSize, numberofFiles);
	dp.validateInput();
	exception.expect(InputLimitException.class);
	dp.validateBundleRequest();
    }

    private static void createBundle() throws JsonParseException, JsonMappingException, IOException {
	inputfileList = new FileRequest[2];
	ObjectMapper mapper = new ObjectMapper();
	FileRequest testval1 = mapper.readValue(val1, FileRequest.class);
	FileRequest testval2 = mapper.readValue(val2, FileRequest.class);
	inputfileList[0] = testval1;
	inputfileList[1] = testval2;
	bundleRequest = new BundleRequest("testdatabundle", inputfileList);
    }
}
