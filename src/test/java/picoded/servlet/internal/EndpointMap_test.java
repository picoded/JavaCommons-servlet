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
public class EndpointMap_test {

	EndpointMap<String> endpoints = null;

	@Before
	public void setUp() {
		endpoints = new EndpointMap<>();
	}
	
	@Test
	public void fullValidPathMatch(){
		assertEquals(0, endpoints.findValidKeys("hello/good/world").size());
		endpoints.registerEndpointPath("hello/good/world", "Awesome world");
		assertEquals(1, endpoints.findValidKeys("hello/good/world").size());
	}

	@Test
	public void invalidRegisteredFullPathMatch(){
		assertEquals(0, endpoints.findValidKeys("hello/good/world/others").size());
		endpoints.registerEndpointPath("hello/good/world", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("hello/good/world/others").size());
	}

	@Test
	public void invalidRequestFullPathMatch(){
		assertEquals(0, endpoints.findValidKeys("hello/good/world").size());
		endpoints.registerEndpointPath("hello/good/world/others", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("hello/good/world").size());
	}

	@Test
	public void wildCardPathMatch(){
		assertEquals(0, endpoints.findValidKeys("hello/good/world").size());
		endpoints.registerEndpointPath("hello/good/*", "Awesome world");
		assertEquals(1, endpoints.findValidKeys("hello/good/world").size());

		endpoints.registerEndpointPath("hello/*", "Awesome world");
		assertEquals(2, endpoints.findValidKeys("hello/good/world").size());
	}

	@Test
	public void badRequestPath(){
		assertEquals(0, endpoints.findValidKeys("hello/bad/world").size());
		endpoints.registerEndpointPath("hello/good/*", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("hello/bad/world").size());
	}

	@Test
	public void emptyRequestPath(){
		assertEquals(0, endpoints.findValidKeys("").size());
		endpoints.registerEndpointPath("hello/good/*", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("").size());
		endpoints.registerEndpointPath("*", "Awesome world");
		assertEquals(1, endpoints.findValidKeys("").size());
	}

	@Test
	public void endpointPathVariableMatch(){
		assertEquals(0, endpoints.findValidKeys("hello/test/world").size());
		endpoints.registerEndpointPath("hello/:variable/world", "Awesome world");
		assertEquals(1, endpoints.findValidKeys("hello/test/world").size());
		assertEquals(1, endpoints.findValidKeys("hello/anything/world").size());
	}

	@Test
	public void endpointPathVariableFailMatch(){
		assertEquals(0, endpoints.findValidKeys("hello/test/notworld").size());
		endpoints.registerEndpointPath("hello/:variable/world", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("hello/test/notworld").size());
		assertEquals(0, endpoints.findValidKeys("hello/anything/notworld").size());
	}
}
	