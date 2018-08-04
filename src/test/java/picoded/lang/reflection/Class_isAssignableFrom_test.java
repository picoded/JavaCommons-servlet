package picoded.lang.reflection;

// Junit includes
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import picoded.core.struct.GenericConvertHashMap;
import picoded.core.struct.GenericConvertMap;

// Base class to build on
public class Class_isAssignableFrom_test {
	
	@Test
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