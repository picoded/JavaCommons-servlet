package picoded.servlet.internal;

// Junit includes

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import picoded.servlet.annotation.*;

// Base class to build on
public class EndpointMap_test {
	
	EndpointMap<String> endpoints = null;
	
	@BeforeEach
	public void setUp() {
		endpoints = new EndpointMap<>();
	}
	
	@Test
	@DisplayName("Precise full valid static path route matching")
	public void fullValidPathMatch() {
		assertEquals(0, endpoints.findValidKeys("hello/good/world").size());
		endpoints.registerEndpointPath("hello/good/world", "Awesome world");
		assertEquals(1, endpoints.findValidKeys("hello/good/world").size());
	}
	
	@Test
	@DisplayName("Rejects nested sub-paths exceeding registered static full path length")
	public void invalidRegisteredFullPathMatch() {
		assertEquals(0, endpoints.findValidKeys("hello/good/world/others").size());
		endpoints.registerEndpointPath("hello/good/world", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("hello/good/world/others").size());
	}
	
	@Test
	@DisplayName("Rejects parent path calls lacking complete registered child path elements")
	public void invalidRequestFullPathMatch() {
		assertEquals(0, endpoints.findValidKeys("hello/good/world").size());
		endpoints.registerEndpointPath("hello/good/world/others", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("hello/good/world").size());
	}
	
	@Test
	@DisplayName("Matches trailing wildcard routes and orders multiple matching keys")
	public void wildCardPathMatch() {
		assertEquals(0, endpoints.findValidKeys("hello/good/world").size());
		endpoints.registerEndpointPath("hello/good/*", "Awesome world");
		assertEquals(1, endpoints.findValidKeys("hello/good/world").size());
		
		endpoints.registerEndpointPath("hello/*", "Awesome world");
		assertEquals(2, endpoints.findValidKeys("hello/good/world").size());
	}
	
	@Test
	@DisplayName("Rejects requests failing to match wildcard segments")
	public void badRequestPath() {
		assertEquals(0, endpoints.findValidKeys("hello/bad/world").size());
		endpoints.registerEndpointPath("hello/good/*", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("hello/bad/world").size());
	}
	
	@Test
	@DisplayName("Resolves root empty path routes and global wildcard matching")
	public void emptyRequestPath() {
		assertEquals(0, endpoints.findValidKeys("").size());
		endpoints.registerEndpointPath("hello/good/*", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("").size());
		endpoints.registerEndpointPath("*", "Awesome world");
		assertEquals(1, endpoints.findValidKeys("").size());
	}
	
	@Test
	@DisplayName("Resolves dynamic path variables inside registered routes")
	public void endpointPathVariableMatch() {
		assertEquals(0, endpoints.findValidKeys("hello/test/world").size());
		endpoints.registerEndpointPath("hello/:variable/world", "Awesome world");
		assertEquals(1, endpoints.findValidKeys("hello/test/world").size());
		assertEquals(1, endpoints.findValidKeys("hello/anything/world").size());
	}
	
	@Test
	@DisplayName("Rejects dynamic path variables when adjacent static structures mismatch")
	public void endpointPathVariableFailMatch() {
		assertEquals(0, endpoints.findValidKeys("hello/test/notworld").size());
		endpoints.registerEndpointPath("hello/:variable/world", "Awesome world");
		assertEquals(0, endpoints.findValidKeys("hello/test/notworld").size());
		assertEquals(0, endpoints.findValidKeys("hello/anything/notworld").size());
	}
	
	@Test
	@DisplayName("Orders endpoints prioritising concrete configurations over wildcards")
	public void sortEndpointListing() {
		String[] sample = new String[] { "*", "session", "a/b" };
		List<String> sampleList = new ArrayList<>(Arrays.asList(sample));
		
		endpoints.sortEndpointList(sampleList);
		assertEquals("a/b", sampleList.get(0));
		assertEquals("session", sampleList.get(1));
		assertEquals("*", sampleList.get(2));
	}
}
