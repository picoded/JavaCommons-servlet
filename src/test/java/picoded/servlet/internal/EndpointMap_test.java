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
	public void invalidFullPathMatch(){
		assertEquals(0, endpoints.findValidKeys("hello/good/world/others").size());
		endpoints.registerEndpointPath("hello/good/world", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("hello/good/world/others").size());
	}
}
	