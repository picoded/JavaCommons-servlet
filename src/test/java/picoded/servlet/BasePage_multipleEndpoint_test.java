package picoded.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import picoded.core.conv.ConvertJSON;
import picoded.core.struct.GenericConvertMap;
import picoded.core.struct.GenericConvertHashMap;
import picoded.core.struct.GenericConvertMap;
import picoded.servlet.util.EmbeddedServlet;
import picoded.servlet.ServletRequestMap;
import picoded.servlet.internal.*;
import picoded.core.web.RequestHttp;
import picoded.core.web.ResponseHttp;
import picoded.servlet.annotation.*;

import javax.servlet.ServletRequest;

/**
 * Test for a base page with multiple endpoints
 * And ensuring the right endpoint is called (and responds)
 */
public class BasePage_multipleEndpoint_test {
	
	//
	// The test folders to use 
	//
	
	int testPort = 0;
	EmbeddedServlet testServlet = null;
	
	@Before
	public void setUp() {
		testPort = ServletTestConfig.issuePortNumber();
		testServlet = null;
	}
	
	@After
	public void teardown() {
		if (testServlet != null) {
			testServlet.close();
			testServlet = null;
		}
	}
	
	/**
	 * Multiple endpoints
	 */
	public static class HelloWorld extends BasePage {
		@RequestPath("hello")
		public void helloWorld() {
			getPrintWriter().println("good");
		}
		
		@RequestPath("*")
		public void fallback() {
			getPrintWriter().println("bad");
		}
	}
	
	@Test
	public void testMultipleEndpoint_inOneClas() throws Exception {
		// Setup servlet
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		
		// Make a request expecting "good"
		String testUrl = "http://127.0.0.1:" + testPort + "/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("good", response.toString().trim());
		
		// Make a request expecting "bad"
		testUrl = "http://127.0.0.1:" + testPort + "/something";
		response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("bad", response.toString().trim());
	}
	
	/**
	 * Base endpoints
	 */
	public static class BaseEndpoint extends BasePage {
		@RequestPath("*")
		@RequestType({ "GET", "POST", "DELETE", "PUT" })
		public void fallback() {
			getPrintWriter().println("bad");
		}
	}
	
	/**
	 * Extended overwriting endpoints
	 */
	public static class ExtendedEndpoints extends BaseEndpoint {
		@RequestPath("hello")
		@RequestType("POST")
		public void goodResult() {
			getPrintWriter().println("good");
		}
		
		@RequestPath("various")
		@RequestType("POST")
		public void various() {
			getPrintWriter().println("bad");
		}
		
		@RequestPath("red")
		@RequestType("POST")
		public void red() {
			getPrintWriter().println("bad");
		}
		
		@RequestPath("hearings")
		@RequestType("POST")
		public void hearings() {
			getPrintWriter().println("bad");
		}
	}
	
	@Test
	public void testMultipleEndpoint_inExtendedClas() throws Exception {
		// Setup servlet
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new ExtendedEndpoints()));
		
		// Reuse variables
		String testUrl;
		ResponseHttp response;
		
		// Make a request expecting "good"
		testUrl = "http://127.0.0.1:" + testPort + "/hello";
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("good", response.toString().trim());
		
		// Make a request expecting "good"
		testUrl = "http://127.0.0.1:" + testPort + "/hello/";
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("good", response.toString().trim());
		
		// Make a request expecting "bad"
		testUrl = "http://127.0.0.1:" + testPort + "/something";
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("bad", response.toString().trim());
		
		// Make a request expecting "bad"
		testUrl = "http://127.0.0.1:" + testPort + "/something/else";
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("bad", response.toString().trim());
	}
	
}