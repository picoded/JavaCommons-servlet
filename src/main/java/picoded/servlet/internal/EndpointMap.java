package picoded.servlet.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import picoded.servlet.*;
import picoded.servlet.annotation.*;
import picoded.core.exception.ExceptionMessage;
import picoded.core.struct.GenericConvertConcurrentHashMap;

/**
 * Internal utility class, used to mapped the relvent 
 * annotated path routes to a given object
 * 
 * And perform the subsquent storage and/or lookup for it
 **/
public class EndpointMap<V> extends ConcurrentHashMap<String,V> {

	///////////////////////////////////////////////////////
	//
	// Consturctor setup
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Blank constructor
	 */
	public EndpointMap() {
		super();
	}
	
	///////////////////////////////////////////////////////
	//
	// local Split URI handling & caching
	//
	///////////////////////////////////////////////////////
	
	/** Memoizer for splitUriString */
	protected ConcurrentHashMap<String,String[]> _splitUriString = new ConcurrentHashMap<>();

	/**
	 * Local memoizer copy of `ServletStringUtil.splitUriString`.
	 * USed this only internally within enpoint map class
	 */
	protected String[] splitUriString(String path) {
		// Get and return the cached result
		String[] res = _splitUriString.get(path);
		if( res != null ) {
			return res;
		}

		// Process the path, and cache the result
		res = ServletStringUtil.splitUriString(path);
		_splitUriString.put(path, res);

		// And return it
		return res;
	}

	///////////////////////////////////////////////////////
	//
	// Path handling / lookup
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Register a method endpoint
	 * 
	 * @param  path of the method endpoint
	 * @param  obj  to register
	 */
	public void registerEndpointPath(String path, V obj) {
		// And register the endpoint & cache its split path
		splitUriString(path);
		this.put(path, obj);
	}

	/**
	 * Validate endpoint path arrays
	 * 
	 * For example the following request path "hello/good/world" 
	 * will match the following endpoint path
	 * 
	 * ```
	 * hello/*
	 * :start/*
	 * hello/:ohno/*
	 * hello/good/*
	 * hello/good/world
	 * hello/:test/world
	 * ``` 
	 * 
	 * And ignore the following
	 * 
	 * ```
	 * hello
	 * hello/bad/world
	 * hello/bad/*
	 * hello/:test/notaworld
	 * hello/good/world/others
	 * ```
	 * 
	 */
	public boolean isValidEndpoint(String[] endpointPathArr, String[] requestPathArr) {
		//
		for (int index = 0; index < endpointPathArr.length; ++index) {

			// Check for asterisks
			if (endpointPathArr[index].equals("*")) {
				return true;
			}

			// EndpointPath is longer than requestPath
			if (index >= requestPathArr.length) {
				return false;
			}

			// Path variable matching check
			if (endpointPathArr[index].startsWith(":")) {
				continue;
			}

			// the endpoint sub path does not match the request's sub path
			if (!endpointPathArr[index].equals(requestPathArr[index])) {
				return false;
			}
		}

		if (requestPathArr.length > endpointPathArr.length) {
			return false;
		}

		return true;
	}

	/**
	 * Given an endpoint path, search and find all relevent
	 * endpoint paths and return its list of relevent "keys"
	 * 
	 * @param  requestPathArr of the method endpoint
	 */
	public List<String> findValidKeys(String[] requestPathArr) {
		// Return list of results
		List<String> ret = new ArrayList<>();

		// Lets iterate each key and array
		for( String endpoint : this.keySet() ) {
			// Get the key array from the cache
			String[] endpointPathArr = splitUriString(endpoint);
			// Find the valid endpoints
			if(isValidEndpoint(endpointPathArr, requestPathArr)){
				ret.add(endpoint);
			}
		}
		// Return found result
		return ret;
	}

	/**
	 * Given an endpoint path, search and find all relevent
	 * endpoint paths and return its list of relevent "keys"
	 * 
	 * @param  reuqestPath of the method endpoint
	 */
	public List<String> findValidKeys(String reuqestPath) {
		return findValidKeys(ServletStringUtil.splitUriString(reuqestPath));
	}

}