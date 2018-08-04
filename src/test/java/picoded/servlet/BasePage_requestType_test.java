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
import picoded.servlet.util.EmbeddedServlet;
import picoded.servlet.ServletRequestMap;
import picoded.core.web.RequestHttp;
import picoded.core.web.ResponseHttp;
import picoded.servlet.annotation.*;

import javax.servlet.ServletRequest;

public class BasePage_requestType_test {
	
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
	 * Hello world test class
	 */
	public static class HelloWorld extends BasePage {
		@RequestPath("type/single")
		@RequestType("GET")
		public void helloWorld() {
			getPrintWriter().println("world");
		}
		
		@RequestPath("type/multiple")
		@RequestType({ "GET", "POST" })
		public void multiple_requestType() {
			getPrintWriter().println("world");
		}
		
		@RequestPath("type/none")
		public void no_requestType() {
			getPrintWriter().println("world");
		}
		
	}
	
	@Test
	public void test_singleType() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/type/single";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
	}
	
	@Test
	public void test_multipleType() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/type/multiple";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
	}
	
	@Test
	public void test_noRequestType() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/type/multiple";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
	}
	
	@Test
	public void test_invalidRequestType_multiple() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/type/multiple";
		ResponseHttp response = RequestHttp.put(testUrl, null, null, null);
		assertEquals("<h1>404 Error</h1>\n" + "The requested resource is not avaliable Q.Q\n" + "\n"
			+ "Request URI : /type/multiple", response.toString().trim());
		response = RequestHttp.delete(testUrl, null, null, null);
		assertEquals("<h1>404 Error</h1>\n" + "The requested resource is not avaliable Q.Q\n" + "\n"
			+ "Request URI : /type/multiple", response.toString().trim());
	}
	
	@Test
	public void test_invalidRequestType_single() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/type/single";
		ResponseHttp response = RequestHttp.put(testUrl, null, null, null);
		assertEquals("<h1>404 Error</h1>\n" + "The requested resource is not avaliable Q.Q\n" + "\n"
			+ "Request URI : /type/single", response.toString().trim());
		response = RequestHttp.delete(testUrl, null, null, null);
		assertEquals("<h1>404 Error</h1>\n" + "The requested resource is not avaliable Q.Q\n" + "\n"
			+ "Request URI : /type/single", response.toString().trim());
	}
	
}