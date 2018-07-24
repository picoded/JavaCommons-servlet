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

		@ApiPath("middle/*")
		public void middle(){

		}

		@RequestPath("to/*")
		public static SmallWorld rerouteToSmallWorld;
	}

	public static class SameWorld extends BasePage {
		@ApiPath("hello")
		public void helloWorld() {
			getPrintWriter().println("world");
		}

		@ApiPath("middle/*")
		public void middle(){

		}

		@RequestPath("to/*")
		public static SmallWorld rerouteToSmallWorld;

	}

	public static class RerouteWorld extends BasePage {
		@RequestPath("reroute/*")
		public static HelloWorld rerouteToHelloWorld;

		@RequestPath("reroute2/*")
		public static SameWorld rerouteToHelloWorld2;

		@Override
		protected void doSharedSetup() throws Exception {

			AxiosApiBuilder axiosApiBuilder = new AxiosApiBuilder(this);
			axiosApiBuilder.scanApiEndpoints();
			super.doSharedSetup();

		}

		@ApiPath("assorted/*")
		public void assortedPath(){

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
}
