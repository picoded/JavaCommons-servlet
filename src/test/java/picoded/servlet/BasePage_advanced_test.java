package picoded.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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

public class BasePage_advanced_test {

	
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
	 * Hello world with interceptors
	 */
	public static class HelloWorld_withComplexInterceptors extends BasePage {
		@RequestBefore("map/*")
		public Map<String,Object> before() {
			Map<String, Object> map = new HashMap<>();
			map.put("Before", "value");
			return map;
		}

		@RequestBefore("map/hello")
		public Map<String,Object> before2() {
			Map<String, Object> map = new HashMap<>();
			map.put("Before2", "value");
			return map;
		}

		@RequestPath("map/hello")
		public Map<String, Object> helloWorld() {
			Map<String, Object> map = new HashMap<>();
			map.put("Execution", "map");
			return map;
		}

		@RequestAfter("map/*")
		public Map<String,Object> after() {
			Map<String, Object> map = new HashMap<>();
			map.put("After", "value");
			return map;
		}

		@RequestAfter("map/hello")
		public Map<String,Object> after2() {
			Map<String, Object> map = new HashMap<>();
			map.put("After2", "value");
			return map;
		}

		//
		// StringBuilder Tests
		//

		@RequestBefore("sb/*")
		public StringBuilder sb_before() {
			return new StringBuilder("sb_before ");
		}

		@RequestBefore("sb/new")
		public StringBuilder sb_new_before() {
			return new StringBuilder("sb_new_before ");
		}

		@RequestPath("sb/new")
		public StringBuilder execution_sb(){
			return new StringBuilder("execution_sb ");
		}

		@RequestAfter("sb/*")
		public StringBuilder sb_after() {
			return new StringBuilder("sb_after ");
		}

		@RequestAfter("sb/new")
		public StringBuilder sb_new_after() {
			return new StringBuilder("sb_new_after ");
		}

		
	}

	@Test
	public void test_complexInterceptors_map(){
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld_withComplexInterceptors()));
		String testUrl = "http://127.0.0.1:"+testPort+"/map/hello";
		Map<String, Object> expectedResult = new HashMap<>();
		expectedResult.put("Before", "value");
		expectedResult.put("Before2", "value");
		expectedResult.put("Execution", "map");
		expectedResult.put("After", "value");
		expectedResult.put("After2", "value");

		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		Map<String, Object> responseMap = response.toMap();
		for(String key : expectedResult.keySet()) {
			if(responseMap != null){
				assertEquals(expectedResult.get(key).toString(), responseMap.get(key).toString());
				responseMap.remove(key);
			}
		}
		assertEquals(0, responseMap.size());
	
	}

	@Test
	public void test_complexInterceptors_sb(){
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld_withComplexInterceptors()));
		String testUrl = "http://127.0.0.1:"+testPort+"/sb/new";
		List<String> expectedResult = new ArrayList();
		expectedResult.add("sb_before");
		expectedResult.add("sb_new_before");
		expectedResult.add("execution_sb");
		expectedResult.add("sb_after");
		expectedResult.add("sb_new_after");

		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		String responseString = response.toString().trim();
		for(String value : expectedResult) {
			assertTrue(responseString.contains(value));
		}
	
	}
}