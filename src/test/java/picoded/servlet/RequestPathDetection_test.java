package picoded.servlet;

// Junit includes
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// Base class to build on
public class RequestPathDetection_test {

	/**
	 * Example class with annotation
	 */
	public class Example {
		public Example() {
			// blank constructor
		}

		@RequestPath("hello")
		public void hello() {

		}
	}

	@Test
	public void hello() throws Exception {
		//assertEquals("world", StaticExtendedClass.hello());
		// Get the example class
		Object exampleObject = new Example();

		// Get the exaple class annotations
		RequestPath[] annotations = exampleObject.getClass().getMethod("hello").getAnnotationsByType( RequestPath.class );

		// Check for only 1 annotation, that is "hello"
		assertEquals(1, annotations.length);
		assertEquals("hello", annotations[0].value());
	}

}