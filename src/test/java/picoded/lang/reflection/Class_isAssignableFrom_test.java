package picoded.lang.reflection;

// Junit includes
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import picoded.core.struct.GenericConvertHashMap;
import picoded.core.struct.GenericConvertMap;

// Base class to build on
public class Class_isAssignableFrom_test {
	
	@Test
	@DisplayName("Reflection check for Map interface assignment capabilities on subclass conversion classes")
	public void assignableTest() {
		Class<Map> mapClass = Map.class;
		Class<GenericConvertMap> genericMapClass = GenericConvertMap.class;
		Class<GenericConvertHashMap> genericHashMapClass = GenericConvertHashMap.class;
		
		// Example
		//
		// Map a = new Map();
		// Map a = new GenericConvertMap();
		// Map a = new GenericConvertHashMap();
		assertTrue(mapClass.isAssignableFrom(mapClass));
		assertTrue(mapClass.isAssignableFrom(genericMapClass));
		assertTrue(mapClass.isAssignableFrom(genericHashMapClass));
		
		// Example Failure
		//
		// GenericConvertMap a = new Map();
		// GenericConvertHashMap a = new Map();
		assertFalse(genericMapClass.isAssignableFrom(mapClass));
		assertFalse(genericHashMapClass.isAssignableFrom(mapClass));
	}
}