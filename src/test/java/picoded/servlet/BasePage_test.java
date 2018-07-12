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

public class BasePage_test {
	
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
		String testUrl = "http://127.0.0.1:"+testPort+"/hello";
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
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld_withSimpleInterceptors()));
		String testUrl = "http://127.0.0.1:"+testPort+"/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("good morning : time to sleep", response.toString().trim());
	}

	/**
	 * Hello world with interceptors
	 */
	public static class HelloWorld_withMethodParameters extends BasePage{

		@RequestPath("hello")
		public void helloWorld(PrintWriter pw) {
			pw.print("morning :");
		}

		@RequestPath("hello/request")
		public void helloRequest(PrintWriter pw, GenericConvertMap<String, Object> map){
			pw.print(ConvertJSON.fromObject(map));
		}

		@RequestPath("hello/request/sub")
		public void helloRequest(ServletRequestMap servletRequestMap, GenericConvertMap<String, Object> map){
			getPrintWriter().print(ConvertJSON.fromObject(map)+" ");
			getPrintWriter().print(ConvertJSON.fromObject(servletRequestMap));

		}

		@RequestPath("hello/request/mixed")
		public void helloRequest(ServletRequestMap servletRequestMap, String sentence, GenericConvertMap<String, Object> map){
			getPrintWriter().print(ConvertJSON.fromObject(servletRequestMap));
			getPrintWriter().print(sentence);
			getPrintWriter().print(ConvertJSON.fromObject(map)+" ");

		}

		@RequestPath("hello/request/unknown")
		public void helloRequest(String sentence, int age){
			getPrintWriter().print(sentence+ " " + age);

		}

	}
	@Test
	public void test_parameter(){
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld_withMethodParameters()));
		String testUrl = "http://127.0.0.1:"+testPort+"/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("morning :", response.toString().trim());
	}

	@Test
	public void test_multipleParams(){
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld_withMethodParameters()));
		String testUrl = "http://127.0.0.1:"+testPort+"/hello/request";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("{}", response.toString().trim());
	}

	@Test
	public void test_subClass(){
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld_withMethodParameters()));
		String testUrl = "http://127.0.0.1:"+testPort+"/hello/request/sub";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("{} {}", response.toString().trim());

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("first value", "first");
		response = RequestHttp.get(testUrl, params, null, null);
		assertEquals("{\"first value\":\"first\"} {\"first value\":\"first\"}", response.toString().trim());

	}

	@Test
	public void test_unknownDefaultParameters(){
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld_withMethodParameters()));
		String testUrl = "http://127.0.0.1:"+testPort+"/hello/request/unknown";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("0", response.toString().trim());
	}

	@Test
	public void test_parametersWithUnknownDefaultParameters(){
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld_withMethodParameters()));
		String testUrl = "http://127.0.0.1:"+testPort+"/hello/request/mixed";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("{}{}", response.toString().trim());
	}
	
}