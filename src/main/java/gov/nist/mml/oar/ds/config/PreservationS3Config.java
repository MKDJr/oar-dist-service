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
 * 
 * @author:Harold Affo (Prometheus Computing, LLC)
 */
package gov.nist.mml.oar.ds.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * This is the preservation S3 Config class responsible of starting the s3 client
 *
 */
@Configuration
public class PreservationS3Config {

  @Value("${cloud.aws.preservation.credentials.accessKey}")
  private String accessKey;

  @Value("${cloud.aws.preservation.credentials.secretKey}")
  private String secretKey;

  @Value("${cloud.aws.preservation.region}")
  private String region;

  @Bean(name = "preservationAWSCredentials")
  public BasicAWSCredentials basicAWSCredentials() {
    return new BasicAWSCredentials(accessKey, secretKey);
  }

  @Bean
  public AmazonS3Client amazonS3Client(
      @Qualifier("preservationAWSCredentials") AWSCredentials awsCredentials) {
    AmazonS3Client amazonS3Client = new AmazonS3Client(awsCredentials);
    amazonS3Client.setRegion(Region.getRegion(Regions.fromName(region)));
    return amazonS3Client;
  }
}
