package picoded.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import picoded.servlet.util.EmbeddedServlet;
import picoded.core.web.RequestHttp;
import picoded.core.web.ResponseHttp;
import picoded.core.conv.ConvertJSON;
import picoded.servlet.annotation.*;

/**
 * This test is to verify that the parameters when passed through several layers of the endpoints,
 * will still get received by the intended endpoint at the very end
 */
public class BasePage_requestBefore_test {
	
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
	 * FirstLayer test class
	 */
	public static class FirstLayer extends BasePage {
		
		@RequestBefore("/info/*")
		public void halt() {
			throw new HaltException("SECOND LAYER HALT");
		}
		
		@RequestPath("/info/*")
		public static SecondLayer secondLayer;
	}
	
	/**
	 * 
	 */
	public static class SecondLayer extends BasePage {
		@ApiPath("/get")
		public String useParameterFunction(ServletRequestMap req) {
			return "iF it returns this, it has error";
		}
	}
	
	/**
	 * Hello world with interceptors
	 */
	public static class LandingPage extends BasePage {
		@RequestBefore("/account/*")
		public void haltEverything() {
			throw new HaltException("FIRST LAYER HALT");
		}
		
		@RequestPath("/account/verify/:accountID/*")
		public static FirstLayer firstLayer;
		
		@RequestPath("/project/*")
		public static FirstLayer firstLayer2;
		
		@Override
		public void handleHaltException(HaltException e) {
			// This is an example of if you want halt exception to print something out
			if (e.getMessage() != null) {
				getPrintWriter().println(e.getMessage());
			}
		}
	}
	
	@Test
	@DisplayName("Stops execution context when first layer interceptor throws HaltException")
	public void test_firstLayerHaltException() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new LandingPage()));
		String testUrl = "http://127.0.0.1:" + testPort + "/account/verify/RANDOMID/info/get";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		
		assertEquals("FIRST LAYER HALT", response.toString().trim());
	}
	
	@Test
	@DisplayName("Stops execution context when nested second layer interceptor throws HaltException")
	public void test_secondLayerHaltException() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new LandingPage()));
		String testUrl = "http://127.0.0.1:" + testPort + "/project/info/get";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		
		assertEquals("SECOND LAYER HALT", response.toString().trim());
	}
	
	@Test
	@DisplayName("Renders clean 404 response when nested route structures mismatch completely")
	public void test_noRouteFound() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new LandingPage()));
		String testUrl = "http://127.0.0.1:" + testPort + "/project/info/unknown";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		
		assertTrue(response.toString().trim().contains("The requested resource is not avaliable Q.Q"));
	}
	
}
