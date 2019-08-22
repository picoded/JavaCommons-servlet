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

import picoded.servlet.util.EmbeddedServlet;
import picoded.core.web.RequestHttp;
import picoded.core.web.ResponseHttp;
import picoded.servlet.annotation.*;

/**
 * This test is to verify that the parameters when passed through several layers of the endpoints,
 * will still get received by the intended endpoint at the very end
 */
public class BasePage_reroute_parameter_test {
	
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
	 * FirstLayer test class
	 */
	public static class FirstLayer extends BasePage {
		@RequestPath("/nested/*")
		public static SecondLayer secondLayer;
	}
	
	/**
	 * 
	 */
	public static class SecondLayer extends BasePage {
		@ApiPath("/param")
		public Map<String, Object> useParameterFunction(ServletRequestMap req) {
			String nameParam = req.getString("nameParam");
			Map<String, Object> res = new HashMap<>();
			res.put("nameParam", nameParam);
			res.put("getParam", req.getString("getParam"));
			res.put("postParam", req.getString("postParam"));
			return res;
		}
	}
	
	/**
	 * Hello world with interceptors
	 */
	public static class LandingPage extends BasePage {
		@RequestPath("/pass/:nameParam/internal/*")
		public static FirstLayer firstLayer;
	}
	
	@Test
	public void test_withNameParams() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new LandingPage()));
		String testUrl = "http://127.0.0.1:" + testPort + "/pass/testing/internal/nested/param";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		
		assertEquals("testing", response.toMap().getString("nameParam").trim());
	}
	
	@Test
	public void test_withGetParams() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new LandingPage()));
		String testUrl = "http://127.0.0.1:" + testPort + "/pass/testing/internal/nested/param?getParam=answer";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("answer", response.toMap().getString("getParam").trim());
	}
	
	@Test
	public void test_withPostParams() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new LandingPage()));
		String testUrl = "http://127.0.0.1:" + testPort + "/pass/testing/internal/nested/param";
		Map<String, Object> params = new HashMap<>();
		params.put("postParam", "answerForPost");
		ResponseHttp response = RequestHttp.post(testUrl, params, null, null);
		assertEquals("answerForPost", response.toMap().getString("postParam").trim());
	}
	
}
