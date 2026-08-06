package picoded.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import picoded.servlet.util.EmbeddedServlet;
import picoded.core.web.RequestHttp;
import picoded.core.web.ResponseHttp;
import picoded.servlet.annotation.*;

public class BasePage_reroute_test {
	
	//
	// The test folders to use
	//
	
	int testPort = 0;
	EmbeddedServlet testServlet = null;
	
	@BeforeEach
	public void setUp() {
		testPort = ServletTestConfig.issuePortNumber();
		testServlet = null;
	}
	
	@AfterEach
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
	
	/**
	 * Hello world with interceptors
	 */
	public static class LandingPage extends BasePage {
		@RequestPath("say/*")
		public static HelloWorld sayReroute;
	}
	
	@Test
	@DisplayName("Static nested page redirection matches target route cleanly")
	public void test_withSimpleInterceptors() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new LandingPage()));
		String testUrl = "http://127.0.0.1:" + testPort + "/say/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
	}
	
	@Test
	@DisplayName("Renders clean 404 response when nested static page redirects to mismatched path")
	public void test_existRedirectButInvalidPath() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new LandingPage()));
		String testUrl = "http://127.0.0.1:" + testPort + "/say/invalid";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("<h1>404 Error</h1>\n" + "The requested resource is not avaliable Q.Q\n" + "\n"
			+ "Request URI : /say/invalid", response.toString().trim());
	}
	
	@Test
	@DisplayName("Renders clean 404 response when requested base path has no registered page mapping")
	public void test_nonExistenceRedirect() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new LandingPage()));
		String testUrl = "http://127.0.0.1:" + testPort + "/gone/invalid";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("<h1>404 Error</h1>\n" + "The requested resource is not avaliable Q.Q\n" + "\n"
			+ "Request URI : /gone/invalid", response.toString().trim());
	}
	
	public static class TestInternalWorld extends DStackPage {
		
		@RequestPath("last/moments")
		public void moments() {
			getPrintWriter().println("what");
		}
	}
	
	public static class RerouteWithMethod extends BasePage {
		
		@RequestPath("anything/*")
		public TestInternalWorld rerouting() {
			TestInternalWorld testInternalWorld = new TestInternalWorld();
			return testInternalWorld;
		}
	}
	
	@Test
	@DisplayName("Dispatches requests dynamically to class instances instantiated via routing methods")
	public void test_methodReroute() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new RerouteWithMethod()));
		String testUrl = "http://127.0.0.1:" + testPort + "/anything/last/moments";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("what", response.toString().trim());
	}
	
}
