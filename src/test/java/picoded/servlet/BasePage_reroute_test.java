package picoded.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
		public static HelloWorld sayPath;

		@RequestPath("say/*")
		public static Class<HelloWorld> sayClass = HelloWorld.class;
	}
	
	@Test
	public void test_withSimpleInterceptors() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new LandingPage()));
		String testUrl = "http://127.0.0.1:"+testPort+"/say/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("good morning : time to sleep", response.toString().trim());
	}
	
}