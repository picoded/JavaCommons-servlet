package picoded.lang.inheritance;

// Junit includes
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// Base class to build on
public class StaticExtendedClass_test {

	@Test
	public void hello() {
		assertEquals("world", StaticExtendedClass.hello());
	}

	@Test
	public void extended() {
		assertEquals("base plus", StaticExtendedClass.toExtend());
	}
}