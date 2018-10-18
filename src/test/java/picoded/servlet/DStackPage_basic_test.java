package picoded.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import picoded.core.conv.ConvertJSON;
import picoded.core.web.RequestHttp;
import picoded.core.web.ResponseHttp;
import picoded.servlet.util.EmbeddedServlet;
import picoded.servlet.annotation.*;
import picoded.dstack.DataObjectMap;
import picoded.dstack.DataObject;

public class DStackPage_basic_test {
	
	// Test config folder to use
	static File dstackPageConfigFile = new File("./test/DStackPage/config");
	
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
	
	public static class SimplePage extends DStackPage {
		
		@RequestPath("dstack")
		public void simpleDStack() {
			System.out.println(ConvertJSON.fromObject(this.dstack()));
			DataObjectMap dataObjectMap = this.dstack().dataObjectMap("simplepage");
			
			DataObject dataObject = dataObjectMap.newEntry();
			dataObject.saveAll();
			System.out.println(dataObject._oid());
			
		}
		
		@Override
		protected void initializeContext() throws Exception {
			this.dstack().dataObjectMap("simplepage").systemSetup();
			super.initializeContext();
		}
		
		/**
		 * @return config files path
		 **/
		public String getConfigPath() {
			System.out.println(dstackPageConfigFile.getAbsolutePath());
			return dstackPageConfigFile.getAbsolutePath();
		}
	}
	
	@Test
	public void DStack_simple() {
		assertNotNull(testServlet = new EmbeddedServlet(testPort, new SimplePage()));
		String testUrl = "http://127.0.0.1:" + testPort + "/dstack";
		ResponseHttp response = RequestHttp.get(testUrl, null, null, null);
		//		Map<String, Object> map = response.toMap();
		//		assertEquals()
	}
}
