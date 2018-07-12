package picoded.servlet.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	public String[] splitUriString(String path) {
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
		if (endpointPathArr_length > requestPathArr_length) {
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
		for (int index = 0; index < endpointPathArr_length; ++index) {

			// Get the part to compare
			String endpointPart = endpointPathArr[index];
			String requestPart = (requestPathArr_length <= index) ? null : requestPathArr[index];

			// Ending wildcard check
			// If this triggers, its is presumed that every part segment 
			// before was validated correctly
			if (index == endpointPathArr_lastIndex && endpointPart.equalsIgnoreCase("*")) {
				return true;
			}

			if (endpointPart.equalsIgnoreCase("*") || endpointPart.startsWith(":")) {
				// go to next part
				continue;
			}

			// EndpointPath is longer than requestPath
			if (index >= requestPathArr.length) {
				return false;
			}

			// Finally this would be textual part to part comparision
			// if this fails, the endpoint mapping is invalid
			if (!endpointPart.equalsIgnoreCase(requestPart)) {
				return false;
			}
		}

		// the endpointPath matches everything but requestPath is longer
		// endpointPath: hello/good/world
		// requestPath:  hello/good/world/others
		if (requestPathArr.length > endpointPathArr.length) {
			return false;
		}

		// Final return true
		return true;
	}

	/**
	 * Sort a list of endpoints, in order of 
	 * 
	 * + Longest path match
	 * + String comparision (final)
	 * 
	 * @param  list of string paths, to sort
	 */
	protected void sortEndpointList(List<String> list) {
		Collections.sort(list, (a,b) -> {
			String[] a_arr = splitUriString(a);
			String[] b_arr = splitUriString(b);

			// Longest path match sorting
			if( a_arr.length > b_arr.length ) {
				return 1;
			} else if( b_arr.length > a_arr.length ) {
				return -1;
			}
 
			// @TODO "least wildcard match" is implemented
			// this is so that more exact matches (starting from left)
			// takes priority over least exact matches

			// String comparision
			return a.compareTo(b);
		});
	}

	/**
	 * Given an endpoint path, search and find all relevent
	 * endpoint paths and return its list of relevent "keys"
	 * 
	 * @TODO : annotatation type varient (when needed)
	 *
	 * @param  requestPathArr of the method endpoint
	 * 
	 * @return  list for valid keys found, null if no keys found
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

		// no valid key found check, skip and return empty list
		if( ret.size() <= 0 ) {
			return ret;
		}

		// Sort the result
		sortEndpointList(ret);

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