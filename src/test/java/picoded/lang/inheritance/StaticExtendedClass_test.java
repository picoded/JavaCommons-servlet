package picoded.lang.inheritance;

// Junit includes
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

// Base class to build on
public class StaticExtendedClass_test {
	
	@Test
	@DisplayName("Static base class hello method inheritance")
	public void hello() {
		assertEquals("world", StaticExtendedClass.hello());
	}
	
	@Test
	@DisplayName("Static base class toExtend method extension inheritance")
	public void extended() {
		assertEquals("base plus", StaticExtendedClass.toExtend());
	}
}