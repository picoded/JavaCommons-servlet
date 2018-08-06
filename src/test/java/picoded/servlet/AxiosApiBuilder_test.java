package picoded.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequest;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import picoded.core.conv.ConvertJSON;
import picoded.core.conv.GenericConvert;
import picoded.core.struct.GenericConvertMap;
import picoded.core.struct.GenericConvertHashMap;
import picoded.servlet.util.EmbeddedServlet;
import picoded.servlet.ServletRequestMap;
import picoded.servlet.internal.*;
import picoded.servlet.annotation.*;
import picoded.core.web.RequestHttp;
import picoded.core.web.ResponseHttp;


public class AxiosApiBuilder_test {

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

	public static class SmallWorld extends BasePage {
		@ApiPath("smallWorld")
		@RequestType({"POST"})
		@RequiredVariables({"name", "id"})
		public void smallWorld(){

		}
	}

	/**
	 * Hello world test class
	 */
	public static class HelloWorld extends BasePage {
		@ApiPath("hello")
		public void helloWorld() {
			getPrintWriter().println("world");
		}

		@RequestPath("middle/*")
		public void middle(){

		}

		@RequestPath("to/*")
		public static SmallWorld rerouteToSmallWorld;

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			super.contextInitialized(sce);
			System.out.println("RAMRARANRNAN");
		}
	}

	public static class SameWorld extends BasePage {
		@ApiPath("hello")
		public void helloWorld() {
			getPrintWriter().println("world");
		}

		@RequestPath("middle/*")
		public void middle(){

		}

		@RequestPath("to/*")
		public static SmallWorld rerouteToSmallWorld;

	}

	public static class RerouteWorld extends BasePage {

		AxiosApiBuilder axiosApiBuilder;

		@RequestPath("reroute/*")
		public static HelloWorld rerouteToHelloWorld;

		@RequestPath("reroute2/*")
		public static SameWorld rerouteToHelloWorld2;

		@Override
		protected void doSharedSetup() throws Exception {
			// @TODO: Take note that this should be called inside initializeContext, but it is not working
			// so for now we are using doSharedSetup
			axiosApiBuilder = new AxiosApiBuilder(this);
			axiosApiBuilder.scanApiEndpoints();
			super.doSharedSetup();

		}

		@ApiPath("paths/*")
		public Map<String, Method> assortedPath(){
			return axiosApiBuilder.scanApiEndpoints();
		}
	}

	@Test
	public void testHelloPath() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		String testUrl = "http://127.0.0.1:"+testPort+"/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
	}

	/*
	Effectively the legit endpoints are
	hello, Method -> helloWorld
	to/smallWorld -> smallWorld

	reroute/hello -> helloWorld
	reroute/to/smallWorld -> smallWorld

	smallWorld -> smallWorld
	 */
	@Test
	public void testRerouteWorld() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new RerouteWorld()));
		String testUrl = "http://127.0.0.1:"+testPort+"/reroute/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("world", response.toString().trim());
	}

	@Test
	public void rerouteScannedApiPaths() throws Exception {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new RerouteWorld()));
		String testUrl = "http://127.0.0.1:"+testPort+"/paths/hello";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		Map<String, Object> responseMap = response.toMap();
		List<String> expectedResult = new ArrayList<>();
		expectedResult.add("to/smallWorld");
		expectedResult.add("paths/*");
		expectedResult.add("reroute/hello");
		expectedResult.add("reroute/to/smallWorld");
		expectedResult.add("reroute2/hello");
		expectedResult.add("reroute2/to/smallWorld");
		expectedResult.add("smallWorld");
		expectedResult.add("hello");
		for(String key : expectedResult){
			if (responseMap.get(key) != null){
				responseMap.remove(key);
			}
		}

		assertTrue(responseMap.size() == 0);
	}


	public static class EndpointMapGenerator extends BasePage {

		AxiosApiBuilder axiosApiBuilder;

		@ApiPath("endpoint")
		public String endpoint() {
			Map<String, Object> endpointMaps = axiosApiBuilder.generateEndpointMap();
			GenericConvertMap<String, Object> maps = GenericConvert.toGenericConvertStringMap(endpointMaps);
			GenericConvertMap<String, Object> endpoint = maps.getGenericConvertStringMap("endpoint");
			return endpoint.getString("methods");
		}

		@ApiPath("endpoint/string")
		public String endpointString(){
			Map<String, Object> endpointMaps = axiosApiBuilder.generateEndpointMap();
			return axiosApiBuilder.endpointMapInString();
		}

		@Override
		protected void doSharedSetup() throws Exception {
			// @TODO: Take note that this should be called inside initializeContext, but it is not working
			// so for now we are using doSharedSetup
			axiosApiBuilder = new AxiosApiBuilder(this);
			axiosApiBuilder.scanApiEndpoints();
			super.doSharedSetup();
		}
	}

	@Test
	public void test_endpointMapGeneration(){
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new EndpointMapGenerator()));
		String testUrl = "http://127.0.0.1:"+testPort+"/endpoint";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("[\"GET\",\"POST\"]", response.toString().trim());
	}

	@Test
	public void test_endpointMapInString(){
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new EndpointMapGenerator()));
		String testUrl = "http://127.0.0.1:"+testPort+"/endpoint/string";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals("\"endpoint\" : {\"methods\":[\"GET\",\"POST\"],\"optional\":[],\"required\":[]}," +
				"\n\t\t\"endpoint/string\" : {\"methods\":[\"GET\",\"POST\"],\"optional\":[],\"required\":[]}", response.toString().trim());
	}




	public static class EndpointLoad extends BasePage {

		AxiosApiBuilder axiosApiBuilder;

		@ApiPath("template/load")
		public String loadTemplate() {
			return axiosApiBuilder.grabAxiosApiTemplate();
		}

		@RequestPath("reroute/*")
		public static SmallWorld rerouteToIt;

		@Override
		protected void doSharedSetup() throws Exception {
			// @TODO: Take note that this should be called inside initializeContext, but it is not working
			// so for now we are using doSharedSetup
			axiosApiBuilder = new AxiosApiBuilder(this);
			axiosApiBuilder.load();
			super.doSharedSetup();
		}
	}

	@Test
	public void test_loadingTemplate(){
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new EndpointLoad()));
		String testUrl = "http://127.0.0.1:"+testPort+"/template/load";
		String expectedAxioJS = obtainExpectedAxioJS();
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		assertEquals(expectedAxioJS, response.toString().trim());
	}

	private String obtainExpectedAxioJS() {
		String result;
		try {
			// https://stackoverflow.com/questions/24499692/access-resources-in-unit-tests
			URI uri = AxiosApiBuilder_test.class.getClassLoader().getResource("expectedAxioJS.js").toURI();
			result = new String(Files.readAllBytes(Paths.get(uri)), "utf-8");
			result = result.replace("REPLACE_PORT_NUMBER", Integer.toString(testPort));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}
}
