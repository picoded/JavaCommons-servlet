package picoded.servlet;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.junit.*;

import picoded.servlet.util.EmbeddedServlet;
import picoded.core.conv.*;
import picoded.core.web.RequestHttp;
import picoded.core.common.*;

public class CorePage_test {
	
	//
	// The test vars to use
	//
	int testPort = 0; //Test port to use
	EmbeddedServlet testServlet = null; //Test servlet to use
	
	//
	// Standard setup and teardown
	//
	@Before
	public void setUp() {
		testPort = ServletTestConfig.issuePortNumber();
		testServlet = null;
	}
	
	@After
	public void tearDown() throws Exception {
		if (testServlet != null) {
			testServlet.close();
			testServlet = null;
		}
	}
	
	//
	// Simple get test
	//
	public static class SimpleHelloWorld extends CorePage {
		// Message to put
		@Override
		protected void doRequest(PrintWriter writer) throws Exception {
			writer.println("<h1>Hello World</h1>");
		}
	}
	
	//
	// Assertion test case for multiple reuse
	//
	public void helloWorldAssert(String testUrl, String testString) {
		if (testString == null) {
			testString = "<h1>Hello World</h1>";
		}
		
		assertEquals(testString, RequestHttp.get(testUrl, null).toString().trim());
		assertEquals(testString, RequestHttp.post(testUrl, null).toString().trim());
		assertEquals(testString, RequestHttp.put(testUrl, null).toString().trim());
		assertEquals(testString, RequestHttp.delete(testUrl, null).toString().trim());
	}
	
	@Test
	public void simpleHelloWorldTest() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new SimpleHelloWorld()));
		helloWorldAssert("http://localhost:" + testPort + "/test/", null);
	}
	
	//
	// Testing and fixing the PrintWriter.print bug not outputing final result
	//
	public static class SpecialSymbolsTesting extends CorePage {
		// Message to put
		@Override
		protected void doRequest(PrintWriter writer) throws Exception {
			writer.println("Test *>> This");
		}
	}
	
	@Test
	public void outputPrintBugFixing() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new SpecialSymbolsTesting()));
		helloWorldAssert("http://localhost:" + testPort + "/test/", "Test *>> This");
	}
	
}