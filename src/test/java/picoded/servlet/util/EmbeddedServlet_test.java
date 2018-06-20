package picoded.servlet.util;

import static org.junit.Assert.*;
import org.junit.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

import javax.servlet.*;
import javax.servlet.http.*;

import picoded.TestConfig;
import picoded.core.conv.*;
import picoded.core.common.*;
import picoded.web.*;

///
/// Test the EmbeddedServlet implmentation
///
public class EmbeddedServlet_test {
	
	//
	// The test folders to use
	//
	File testCollection = new File("./test/files/servlet/util/EmbeddedServlet/");
	File helloWorldHtml = new File(testCollection, "HelloWorldHtml");
	File helloWorldJava = new File(testCollection, "HelloWorldJava");
	File helloWorldJWar = new File(testCollection, "HelloWorldJWar/test.war");
	
	//
	// The test vars to use
	//
	int testPort = 0; //Test port to use
	EmbeddedServlet testServlet = null; //Test servlet to use
	
	//
	// Standard setup and teardown
	//
	@Before
	public void setUp() {
		// Issue a possible port to use
		testPort = TestConfig.issuePortNumber();
		testServlet = null;
	}
	
	@After
	public void tearDown() throws Exception {
		if (testServlet != null) {
			testServlet.close();
			testServlet = null;
		}
	}
	
	// Sanity check
	@Test
	public void fileChecks() {
		assertTrue(testCollection.isDirectory());
		assertTrue(helloWorldHtml.isDirectory());
		assertTrue(helloWorldJava.isDirectory());
		assertTrue(helloWorldJWar.isFile());
		
	}
	
	//
	// Testing various servlet packages deployment
	//
	@Test
	public void helloWorldHtml() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, helloWorldHtml));
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/index.html").toString().trim());
	}
	
	@Test
	public void helloWorldJava() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, helloWorldJava));
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/test-html.html").toString().trim());
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/test-java").toString().trim());
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/test-jsp.jsp").toString().trim());
	}
	
	@Test
	public void helloWorldJWar() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, helloWorldJWar));
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/test-html.html").toString().trim());
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/test-java").toString().trim());
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/test-jsp.jsp").toString().trim());
	}
	
	@Test
	public void helloWorldJWar_contextName() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, "ctest", helloWorldJWar));
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/ctest/test-html.html").toString()
				.trim());
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/ctest/test-java").toString().trim());
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/ctest/test-jsp.jsp").toString().trim());
	}
	
	//
	// Testing deployment using a single servlet class
	// This is useful for Junit testing
	//
	
	// The hello world class to test
	public class HelloWorld extends HttpServlet {
		private String message;
		
		public void init() throws ServletException {
			// Do required initialization
			message = "Hello World";
		}
		
		public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
			// Set response content type
			response.setContentType("text/html");
			
			// Actual logic goes here.
			PrintWriter out = response.getWriter();
			out.println("<h1>" + message + "</h1>");
		}
	}
	
	@Test
	public void helloWorldServlet() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld()));
		assertEquals("<h1>Hello World</h1>", RequestHttp
			.get("http://localhost:" + testPort + "/test").toString().trim());
	}
	
	@Test
	public void helloWorldServlet_fixedPath() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new HelloWorld(), "/fixed"));
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/fixed").toString().trim());
		assertNotEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/rand").toString().trim());
	}
	
	@Test
	public void helloWorldServlet_contextName() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, "ctest", new HelloWorld(), null));
		assertEquals("<h1>Hello World</h1>",
			RequestHttp.get("http://localhost:" + testPort + "/ctest/test").toString().trim());
	}
}
