package picoded.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import picoded.servlet.util.EmbeddedServlet;
import picoded.core.web.RequestHttpClient;
import picoded.core.web.ResponseHttp;
import picoded.servlet.annotation.*;

public class BasePage_test {
	
	//
	// The test folders to use 
	//
	
	int testPort = 0;
	EmbeddedServlet testServlet = null;
	RequestHttpClient httpClient = null;
	
	@Before
	public void setUp() {
		testPort = ServletTestConfig.issuePortNumber();
		testServlet = null;
		httpClient = new RequestHttpClient();
	}
	
	@After
	public void teardown() {
		if (testServlet != null) {
			testServlet.close();
			testServlet = null;
		}
	}
	
	/**
	 * Hello world test class
	 */
	public static class HelloWorld extends BasePage {
		@RequestPath("hello")
		public void helloWorld() {
			getPrintWriter().println("world");
		}
	}
	
	@Test
	public void testHelloPath() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:"+testPort+"/hello";
		ResponseHttp response = httpClient.get(testUrl, null, null, null);
		assertEquals("world", response.toString());
	}
	
}