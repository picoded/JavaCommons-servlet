package picoded.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

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
		
		@ApiPath("api/single")
		@RequestType("POST")
		public Map<String, Object> apiSingle() {
			Map<String, Object> ret = new ApiResponseMap();
			ret.put("result", "pong");
			return ret;
		}
		
		@ApiPath("api/multiple")
		@RequestType({ "GET", "POST" })
		public Map<String, Object> apiMultiple() {
			Map<String, Object> ret = new ApiResponseMap();
			ret.put("result", "pong");
			return ret;
		}
		
		@ApiPath("api/none")
		public Map<String, Object> apiNone() {
			Map<String, Object> ret = new ApiResponseMap();
			ret.put("result", "pong");
			return ret;
		}
		
		// Duplicate path different verbs tests
		@RequestBefore("multi/verb")
		public void before_multi_verb() {
			getPrintWriter().print("[BEFORE] ");
		}

		@RequestPath("multi/verb")
		@RequestType("GET")
		public void get_multi_verb() {
			getPrintWriter().print("GET");
		}
		
		@RequestPath("multi/verb")
		@RequestType("POST")
		public void post_multi_verb() {
			getPrintWriter().print("POST");
		}
		
		@RequestPath("multi/verb")
		@RequestType("DELETE")
		public void delete_multi_verb() {
			getPrintWriter().print("DELETE");
		}

		@RequestAfter("multi/verb")
		public void after_multi_verb() {
			getPrintWriter().print(" [AFTER]");
		}

		@RequestBefore("api/multi/verb")
		public void before_api_multi_verb() {
			getApiResponseMap().put("before", "ok");
		}

		@ApiPath("api/multi/verb")
		@RequestType("GET")
		public Map<String, Object> get_api_multi_verb() {
			Map<String, Object> ret = new ApiResponseMap();
			ret.put("method", "GET");
			return ret;
		}

		@ApiPath("api/multi/verb")
		@RequestType("POST")
		public Map<String, Object> post_api_multi_verb() {
			Map<String, Object> ret = new ApiResponseMap();
			ret.put("method", "POST");
			return ret;
		}

		@ApiPath("api/multi/verb")
		@RequestType("DELETE")
		public Map<String, Object> delete_api_multi_verb() {
			Map<String, Object> ret = new ApiResponseMap();
			ret.put("method", "DELETE");
			return ret;
		}

		@RequestAfter("api/multi/verb")
		public void after_api_multi_verb() {
			getApiResponseMap().put("after", "ok");
		}
		
	}
	
	@Test
	@DisplayName("Standard GET routing with restricted @RequestType(\"GET\")")
	public void test_singleType() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/type/single";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
	}
	
	@Test
	@DisplayName("Standard multi-verb routing with restricted @RequestType({\"GET\", \"POST\"})")
	public void test_multipleType() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/type/multiple";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
	}
	
	@Test
	@DisplayName("Standard routing with unrestricted @RequestType (omitted defaults to all verbs)")
	public void test_noRequestType() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/type/multiple";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
	}
	
	@Test
	@DisplayName("Standard routing correctly rejects non-matching verbs with 404 for multi-verb definitions")
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
	@DisplayName("Standard routing correctly rejects non-matching verbs with 404 for single-verb definitions")
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
	
	@Test
	@DisplayName("API JSON routing with unrestricted @RequestType (omitted defaults to all verbs)")
	public void test_api_none() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/api/none";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("{\"result\":\"pong\"}", response.toString().replaceAll("\\s+", ""));
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("{\"result\":\"pong\"}", response.toString().replaceAll("\\s+", ""));
	}
	
	@Test
	@DisplayName("API JSON routing with restricted @RequestType(\"POST\")")
	public void test_api_single() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/api/single";
		ResponseHttp response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("{\"result\":\"pong\"}", response.toString().replaceAll("\\s+", ""));
	}
	
	@Test
	@DisplayName("API JSON routing correctly rejects non-matching verbs with 404 for single-verb definitions")
	public void test_api_single_invalid() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/api/single";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("<h1>404 Error</h1>\n" + "The requested resource is not avaliable Q.Q\n" + "\n"
			+ "Request URI : /api/single", response.toString().trim());
	}
	
	@Test
	@DisplayName("API JSON routing with restricted @RequestType({\"GET\", \"POST\"})")
	public void test_api_multiple() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/api/multiple";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("{\"result\":\"pong\"}", response.toString().replaceAll("\\s+", ""));
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("{\"result\":\"pong\"}", response.toString().replaceAll("\\s+", ""));
	}
	
	@Test
	@DisplayName("API JSON routing correctly rejects non-matching verbs with 404 for multi-verb definitions")
	public void test_api_multiple_invalid() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/api/multiple";
		ResponseHttp response = RequestHttp.put(testUrl, null, null, null);
		assertEquals("<h1>404 Error</h1>\n" + "The requested resource is not avaliable Q.Q\n" + "\n"
			+ "Request URI : /api/multiple", response.toString().trim());
	}
	
	@Test
	@DisplayName("Same-route standard page matching across GET, POST, and DELETE with universal interceptors")
	public void test_requestPath_multipleVerbs() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/multi/verb";
		
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("[BEFORE] GET [AFTER]", response.toString().trim());
		
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("[BEFORE] POST [AFTER]", response.toString().trim());
		
		response = RequestHttp.delete(testUrl, null, null, null);
		assertEquals("[BEFORE] DELETE [AFTER]", response.toString().trim());
		
		response = RequestHttp.put(testUrl, null, null, null);
		assertEquals("<h1>404 Error</h1>\n" + "The requested resource is not avaliable Q.Q\n" + "\n"
			+ "Request URI : /multi/verb", response.toString().trim());
	}

	@Test
	@DisplayName("Same-route API JSON matching across GET, POST, and DELETE with universal interceptors")
	public void test_apiPath_multipleVerbs() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/api/multi/verb";
		
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("{\"method\":\"GET\",\"before\":\"ok\",\"after\":\"ok\"}", response.toString().replaceAll("\\s+", ""));
		
		response = RequestHttp.post(testUrl, null, null, null);
		assertEquals("{\"method\":\"POST\",\"before\":\"ok\",\"after\":\"ok\"}", response.toString().replaceAll("\\s+", ""));
		
		response = RequestHttp.delete(testUrl, null, null, null);
		assertEquals("{\"method\":\"DELETE\",\"before\":\"ok\",\"after\":\"ok\"}", response.toString().replaceAll("\\s+", ""));
		
		response = RequestHttp.put(testUrl, null, null, null);
		assertEquals("<h1>404 Error</h1>\n" + "The requested resource is not avaliable Q.Q\n" + "\n"
			+ "Request URI : /api/multi/verb", response.toString().trim());
	}
	
}