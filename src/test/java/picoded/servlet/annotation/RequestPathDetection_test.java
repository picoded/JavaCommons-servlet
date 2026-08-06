package picoded.servlet.annotation;

// Junit includes
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

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
	@DisplayName("Reflective inspection of @RequestPath runtime method annotation")
	public void hello() throws Exception {
		//assertEquals("world", StaticExtendedClass.hello());
		// Get the example class
		Object exampleObject = new Example();
		
		// Get the exaple class annotations
		RequestPath[] annotations = exampleObject.getClass().getMethod("hello")
			.getAnnotationsByType(RequestPath.class);
		
		// Check for only 1 annotation, that is "hello"
		assertEquals(1, annotations.length);
		
		// Get the value in a string array
		assertNotNull(annotations[0].value());
		String[] valArr = (String[]) (annotations[0].value());
		
		// And validate it
		assertEquals("hello", valArr[0]);
	}
	
}