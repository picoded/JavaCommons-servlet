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

public class BasePage_basic_test {
	
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
	
	@Test
	public void testHelloPath() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:" + testPort + "/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
	}
	
	/**
	 * Hello world with interceptors
	 */
	public static class HelloWorld_withSimpleInterceptors extends BasePage {
		@RequestBefore("hello")
		public void before() {
			getPrintWriter().print("good ");
		}
		
		@RequestPath("hello")
		public void helloWorld() {
			getPrintWriter().print("morning : ");
		}
		
		@RequestAfter("hello")
		public void after() {
			getPrintWriter().print("time to sleep");
		}
	}
	
	@Test
	public void test_withSimpleInterceptors() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort,
			new HelloWorld_withSimpleInterceptors()));
		String testUrl = "http://127.0.0.1:" + testPort + "/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("good morning : time to sleep", response.toString().trim());
	}
	
	/**
	 * Hello world with interceptors
	 */
	public static class HelloWorld_withMethodParameters extends BasePage {
		
		@RequestPath("hello")
		public void helloWorld(PrintWriter pw) {
			pw.print("morning :");
		}
		
		@RequestPath("hello/request")
		public void helloRequest(PrintWriter pw, GenericConvertMap<String, Object> map) {
			pw.print(ConvertJSON.fromObject(map));
		}
		
		@RequestPath("hello/request/sub")
		public void helloRequest(ServletRequestMap servletRequestMap,
			GenericConvertMap<String, Object> map) {
			getPrintWriter().print(ConvertJSON.fromObject(map) + " ");
			getPrintWriter().print(ConvertJSON.fromObject(servletRequestMap));
			
		}
		
		@RequestPath("hello/request/mixed")
		public void helloRequest(ServletRequestMap servletRequestMap, String sentence,
			GenericConvertMap<String, Object> map) {
			getPrintWriter().print(ConvertJSON.fromObject(servletRequestMap));
			getPrintWriter().print(sentence);
			getPrintWriter().print(ConvertJSON.fromObject(map) + " ");
			
		}
		
		@RequestPath("hello/request/unknown")
		public void helloRequest(String sentence, int age) {
			getPrintWriter().print(sentence + " " + age);
			
		}
		
	}
	
	@Test
	public void test_parameter() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort,
			new HelloWorld_withMethodParameters()));
		String testUrl = "http://127.0.0.1:" + testPort + "/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("morning :", response.toString().trim());
	}
	
	@Test
	public void test_multipleParams() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort,
			new HelloWorld_withMethodParameters()));
		String testUrl = "http://127.0.0.1:" + testPort + "/hello/request";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("{}", response.toString().trim());
	}
	
	@Test
	public void test_subClass() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort,
			new HelloWorld_withMethodParameters()));
		String testUrl = "http://127.0.0.1:" + testPort + "/hello/request/sub";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("{} {}", response.toString().trim());
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("first value", "first");
		response = RequestHttp.get(testUrl, params, null, null);
		assertEquals("{\"first value\":\"first\"} {\"first value\":\"first\"}", response.toString()
			.trim());
		
	}
	
	@Test
	public void test_unknownDefaultParameters() {
		try {
			assertNotNull(testServlet = new EmbeddedServlet(testPort,
				new HelloWorld_withMethodParameters()));
			String testUrl = "http://127.0.0.1:" + testPort + "/hello/request/unknown";
			ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		} catch (RuntimeException e) {
			assertEquals("Unsupported type in method", e.getMessage());
		}
	}
	
	@Test
	public void test_parametersWithUnknownDefaultParameters() {
		try {
			assertNotNull(testServlet = new EmbeddedServlet(testPort,
				new HelloWorld_withMethodParameters()));
			String testUrl = "http://127.0.0.1:" + testPort + "/hello/request/mixed";
			ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		} catch (RuntimeException e) {
			assertEquals("Unsupported type in method", e.getMessage());
		}
	}
	
	/**
	 * Hello world responseStringBuilder and responseApiMap
	 */
	public static class HelloWorld_withStringBuilderAndApiMap extends BasePage {
		
		@RequestPath("different/response/stringbuilder")
		public StringBuilder differentResponseStringBuilder(ServletRequestMap map) {
			responseStringBuilder.append("first value ");
			return new StringBuilder("Return mee");
		}
		
		@RequestPath("different/response/map")
		public Map<String, Object> differentResponseMap(ServletRequestMap servletRequestMap) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("return", "money");
			return map;
			
		}
		
		@RequestPath("same/response/stringbuilder")
		public StringBuilder sameResponseStringBuilder(StringBuilder stringBuilder) {
			stringBuilder.append("added another");
			return stringBuilder;
		}
		
		/**
		 * KIV first, Still do not know whether we want to pass in basePage's responseApiMap into method as argument
		 */
		@RequestPath("same/response/map")
		public Map<String, Object> sameResponseMap(Map<String, Object> responseMap) {
			responseMap.put("addition", "value");
			return responseMap;
		}
		
	}
	
	@Test
	public void test_differentResponseStringBuilder() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort,
			new HelloWorld_withStringBuilderAndApiMap()));
		String testUrl = "http://127.0.0.1:" + testPort + "/different/response/stringBuilder";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("first value Return mee", response.toString().trim());
	}
	
	@Test
	public void test_differentResponseMap() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort,
			new HelloWorld_withStringBuilderAndApiMap()));
		String testUrl = "http://127.0.0.1:" + testPort + "/different/response/map";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("{\n\t\"return\" : \"money\"\n}", response.toString().trim());
	}
	
	@Test
	public void test_sameResponseStringBuilder() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort,
			new HelloWorld_withStringBuilderAndApiMap()));
		String testUrl = "http://127.0.0.1:" + testPort + "/same/response/stringBuilder";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("added another", response.toString().trim());
	}
	
	/**
	 * Servlet for initializing name parameters endpoints
	 */
	public static class NameParametersServlet extends BasePage {
		@RequestPath("name/:parameter")
		public String simpleNameParam(ServletRequestMap map) {
			return map.getString("parameter");
		}
		
		@RequestBefore("name/*/:before/*")
		public Map<String, Object> simpleNameParamBefore(ServletRequestMap map) {
			return map;
		}
		
		@RequestPath("name/:parameter/*")
		public Map<String, Object> nameParamExecution(ServletRequestMap map) {
			return map;
		}
		
		@RequestAfter("name/*/*/:after")
		public Map<String, Object> simpleNameParamAfter(ServletRequestMap map) {
			return map;
		}
	}
	
	@Test
	public void test_nameParameter() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new NameParametersServlet()));
		String testUrl = "http://127.0.0.1:" + testPort + "/name/testingUser";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("testingUser", response.toString().trim());
	}
	
	@Test
	public void test_nameParametersWithBeforeAndAfter() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new NameParametersServlet()));
		String testUrl = "http://127.0.0.1:" + testPort + "/name/testingUser/beforeValue/afterValue";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		Map<String, Object> map = response.toMap();
		assertEquals("beforeValue", map.get("before").toString());
		assertEquals("testingUser", map.get("parameter").toString());
		assertEquals("afterValue", map.get("after").toString());
	}
	
	/**
	 * Methods that explicitly throw ApiPathException
	 */
	public static class ApiPathExceptionServlet extends BasePage {
		@ApiPath("name/testing")
		public void simpleNameParam(Integer wrongParam) {
			// intentionally leave blank
		}
	}
	
	@Test
	public void test_apiPathException() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new ApiPathExceptionServlet()));
		String testUrl = "http://127.0.0.1:" + testPort + "/name/testing";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		Map<String, Object> map = response.toMap();
		
		assertEquals(
			"java.lang.RuntimeException: Unsupported type in method simpleNameParam for parameter type Integer",
			map.get("ERROR_MSG").toString());
	}
}