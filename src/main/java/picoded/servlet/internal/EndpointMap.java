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
		// Get some of the endpointPathArr numbers
		int endpointPathArr_length = endpointPathArr.length;
		int endpointPathArr_lastIndex = endpointPathArr_length - 1;
		int requestPathArr_length = requestPathArr.length;

		// If endpoint is longer then request path, it will always fail
		if(endpointPathArr_length > requestPathArr_length) {
			return false;
		}

		//
		// Iterate each endpoint path array part
		// So for example, with the following requestPath of "hello/good/world/and/beyond",
		// and endpointPath of "hello/:test/*"
		//
		// The following endpointPart / requestPart, is compared with the respective index i
		//
		// | i | endpointPart | requestPart |
		// |---|--------------|-------------|
		// | 0 | hello        | hello       |
		// | 1 | :test        | good        |
		// | 2 | *            | world       |
		//
		// Iteration ends at i=2, and returns true
		//
		for(int i=0; i<endpointPathArr.length; ++i) {
			
			// Get the part to compare
			String endpointPart = endpointPathArr[i];
			String requestPart = (requestPathArr_length <= i)? null : requestPathArr[i];

			// Ending wildcard check
			// If this triggers, its is presumed that every part segment 
			// before was validated correctly
			if( i == endpointPathArr_lastIndex && endpointPart.equalsIgnoreCase("*") ) {
				return true;
			}

			// In between wildcard checks, or named paraemter checks
			if( endpointPart.equalsIgnoreCase("*") || endpointPart.startsWith(":") ) {
				// go to next part
				continue;
			}

			// Finally this would be textual part to part comparision
			// if this fails, the endpoint mapping is invalid
			if( endpointPart.equalsIgnoreCase(requestPart) ) {
				return false;
			}
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