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

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nist.oar.distrib.datapackage.ObjectUtils;

public class ObjectUtilsTest {

    @Test
    public void testIsAllowedURL() throws IOException {
        // this is what is currently set in production
        String allowed = "nist.gov|s3.amazonaws.com/nist-midas";

        assertTrue(ObjectUtils.isAllowedURL("https://nist.gov/datafile.dat", allowed));
        assertTrue(ObjectUtils.isAllowedURL("http://srd.nist.gov/srd13/datafile.dat", allowed));
        assertTrue(ObjectUtils.isAllowedURL("https://s3.amazonaws.com/nist-midas/bigbag.zip", allowed));
        assertTrue(ObjectUtils.isAllowedURL("http://srdnist.gov/srd13/datafile.dat", allowed));

        assertFalse("Don't allow the domain part appear anywhere in the URL path",
                    ObjectUtils.isAllowedURL("http://example.com/nist.gov/anyolfile.exe", allowed));
        assertFalse("Pay attention to field boundaries",
                    ObjectUtils.isAllowedURL("https://s3.amazonaws.com/nist-midas-games/doom.zip",
                                             allowed));
    }
    
    @Test
    public void testGetUrlStatus(){
	String testurlError = "https://data.nist.gov/od/ds/69BEF4C29F700451E053B357068186906918/ngc0055%2B3.con.fits"; 
	String testUrlRedirect = "http://www.nist.gov/srd/srd_data/srd13_B-049.json";
	
	UrlStatusLocation urlLoc = ObjectUtils.getURLStatus(testurlError);
	assertEquals(urlLoc.getStatus(), 404);
	urlLoc = ObjectUtils.getURLStatus(testUrlRedirect); 
	assertEquals(urlLoc.getStatus(), 301);
	
    }
    
    @Test
    public void testUrlCode(){
	String expectedMessage = "The requested file by given URL is not found on server.";
	String message = ObjectUtils.getStatusMessage(404);
	assertEquals(message, expectedMessage);
	expectedMessage = "The given URL is malformed.";
	message = ObjectUtils.getStatusMessage(400);
	assertEquals(message, expectedMessage);
    }

}
