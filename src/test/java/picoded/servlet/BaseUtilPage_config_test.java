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
import picoded.core.web.ResponseHttp;
import picoded.core.common.*;

public class BaseUtilPage_config_test {
	
	//
	// The test vars to use
	//
	int testPort = 0; //Test port to use
	EmbeddedServlet testServlet = null; //Test servlet to use
	
	// Test config folder to use
	static File testConfigFolder = new File("./test/BasePage/config");
	
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
	public static class BaseUtilPageConfig extends BaseUtilPage {
		
		/**
		 * @return config files path
		 **/
		public String getConfigPath() {
			return testConfigFolder.getAbsolutePath();
		}
		
		// Message to put
		@Override
		protected void doRequest(PrintWriter writer) throws Exception {
			writer.println("<h1>" + configFileSet().getString("app.msg", "sad") + "</h1>");
		}
	}
	
	@Test
	public void simpleHelloWorldTest() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new BaseUtilPageConfig()));
		
		String testUrl = "http://localhost:" + testPort + "/test/";
		String testString = "<h1>hello world</h1>";
		assertEquals(testString, RequestHttp.get(testUrl, null).toString().trim());
	}
	
}