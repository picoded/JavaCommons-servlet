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
import picoded.core.common.HttpRequestType;
import picoded.core.conv.ArrayConv;
import picoded.core.conv.ConvertJSON;
import picoded.core.exception.ExceptionMessage;
import picoded.core.struct.GenericConvertConcurrentHashMap;

/**
 * Internal utility class, used to mapped the relvent
 * annotated path routes to a given object
 *
 * And perform the subsquent storage and/or lookup for it
 **/
public class EndpointMap<V> extends ConcurrentHashMap<String, V> {
	
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
	protected ConcurrentHashMap<String, String[]> _splitUriString = new ConcurrentHashMap<>();
	
	/**
	 * Local memoizer copy of `ServletStringUtil.splitUriString`.
	 * USed this only internally within enpoint map class
	 */
	public String[] splitUriString(String path) {
		// Get and return the cached result
		String[] res = _splitUriString.get(path);
		if (res != null) {
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
	 * + More exact path match (from left)
	 * + String comparision (final)
	 * 
	 * @param  list of string paths, to sort
	 */
	public void sortEndpointList(List<String> list) {
		Collections.sort(list, (a, b) -> {
			String[] a_arr = splitUriString(a);
			String[] b_arr = splitUriString(b);
			
			// Longest path match sorting
			if (a_arr.length > b_arr.length) {
				return -1;
			} else if (b_arr.length > a_arr.length) {
				return 1;
			}
			
			// At this point both arays are considered the same length
			
			// "least wildcard match" implementation
			// this is so that more exact matches (starting from left)
			// takes priority over least exact matches
			for (int i = 0; i < a_arr.length; ++i) {
				
				// String part used for comparision
				String a_part = a_arr[i];
				String b_part = b_arr[i];
				
				//
				// Type flag to use
				//
				// 0 - string match
				// 1 - variable 
				// 2 - wildcard
				//
				int a_type = 0;
				int b_type = 0;
				
				// calculate the type flag
				if (a_part.equalsIgnoreCase("*")) {
					a_type = 2;
				} else if (a_part.startsWith(":")) {
					a_type = 1;
				}
				if (b_part.equalsIgnoreCase("*")) {
					b_type = 2;
				} else if (b_part.startsWith(":")) {
					b_type = 1;
				}
				
				// A has a more exact match then B 
				if (a_type < b_type) {
					return -1;
				}
				
				// B has a more exact match then A 
				if (a_type < b_type) {
					return 1;
				}
				
				// // If both parts are same type, skip to next match
				// if( a_type == b_type ) {
				// 	continue;
				// }
			}
			
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
		return findValidKeys(requestPathArr, null);
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
	public List<String> findValidKeys(String[] requestPathArr, HttpRequestType reqType) {
		// Return list of results
		List<String> ret = new ArrayList<>();
		
		// Lets iterate each key and array
		for (String endpoint : this.keySet()) {
			// Get the key array from the cache
			String[] endpointPathArr = splitUriString(endpoint);
			// Find the valid endpoints
			if (isValidEndpoint(endpointPathArr, requestPathArr)
				&& validateRequestType(endpoint, reqType)) {
				ret.add(endpoint);
			}
		}
		
		// no valid key found check, skip and return empty list
		if (ret.size() <= 0) {
			return ret;
		}
		
		// Sort the result
		sortEndpointList(ret);
		
		// Return found result
		return ret;
	}
	
	/**
	 * With the requestType of the request, it checks whether does the endpoint is a Method class,
	 * followed by grabbing the endpoint's RequestTypes to check that the requestType is defined in it.
	 * 
	 * By default, the function will pass it as true if the endpoint does not specify the RequestType
	 * 
	 * @param endpointName The method to be checked
	 * @param requestType  The method of the request (POST/GET/DELETE/etc)
	 * 
	 * @return true if it is allowed to execute, false otherwise
	 * 
	 */
	private boolean validateRequestType(String endpointName, HttpRequestType requestType) {
		
		// No specific method is give, treat as allowed
		if (requestType == null) {
			return true;
		}
		
		// If the endpoint is not a method, treats as valid
		Object endpoint = this.get(endpointName);
		if (!(endpoint instanceof Method)) {
			return true;
		}
		
		// Check through the RequestType annotation of the endpoint and validates if the requestType
		// is contained in it. If the endpoint does not have any RequestType set, treat as allowed
		Method endpointImplementation = (Method) endpoint;
		RequestType[] endpointRequestTypes = endpointImplementation
			.getAnnotationsByType(RequestType.class);
		if (endpointRequestTypes == null || endpointRequestTypes.length == 0) {
			return true;
		}
		
		for (RequestType endpointRequestType : endpointRequestTypes) {
			String[] types = endpointRequestType.value();
			if (ArrayConv.containsIgnoreCase(types, requestType.toString())) {
				return true;
			}
		}
		
		// At this point, the method of the request does not match any of the endpoint's RequestType
		// treat as false
		return false;
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