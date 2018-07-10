package picoded.servlet.internal;

// Junit includes
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import picoded.servlet.annotation.*;

// Base class to build on
public class AnnotationPathTree_test {
	
	/**
	 * Hello world annotation detection
	 */
	public class HelloServlet {
		
		@RequestPath("hello/world")
		public void doesNothing() {
			// because im only checking annotation
		}
		
		@RequestPath("hello/world/:user/location")
		public void withParamInPath() {
			// because im only checking annotation
		}
	}
	
	/**
	 * This test the AnnotationPathTree, scanning for RequestPath annotations only
	 * and succesfully mapping them
	 */
	@Test
	public void requestPathMapping() {
		AnnotationPathTree pathMap = new AnnotationPathTree();
		pathMap.registerClass(HelloServlet.class);
		
		assertNull(pathMap.getAnnotationPath("does-not-exist"));
		assertEquals(0, pathMap.getAnnotationPath("/").methodList.size());
		assertEquals(0, pathMap.getAnnotationPath("hello").methodList.size());
		
		assertNull(pathMap.getAnnotationPath("hello/does-not-exist"));
		assertNotNull(pathMap.getAnnotationPath("hello/world"));
		assertEquals(1, pathMap.getAnnotationPath("hello/world").methodList.size());
		
		assertNotNull(pathMap.getAnnotationPath("hello/world/:user/location"));
	}
	
	/**
	 * 
	 */
	
	// /**
	//  * 
	//  */
	// public class HelloServlet_withForwarding {
	
	// 	@RequestForward("account")
	// 	@RequestForwardClass(HelloServlet.class)
	// 	private static void pointlessLineForTheSakeOfMakingThisCompile;
	
	// 	@RequestPath("account/list")
	// 	public void something() {
	
	// 		return redirect ("old_acount/v3/list");
	
	// 	}
	// 	//@RequestPath("account")
	// 	//static BasePage nextServletAlt = new HelloServlet();
	
	// }
}