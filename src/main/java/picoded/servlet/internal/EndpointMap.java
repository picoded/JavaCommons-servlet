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
	 * @param  obj to register
	 */
	public void registerEndpointPath(String path, V obj) {
		// And register the endpoint & cache its split path
		splitUriString(path);
		this.put(path, obj);
	}

	/**
	 * Validate endpoint path arrays
	 * 
	 */
	public boolean isValidEndpoint(String[] endpointPathArr, String[] requestPathArr) {
		//
		for(int i=0; i<endpointPathArr.length; ++i) {
			if(!endpointPathArr[i].equals(requestPathArr[i])){
				return false;
			}
		}
		return true;
	}

	/**
	 * Given an endpoint path, search and find all relevent
	 * endpoint paths and return its list of relevent "keys"
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
	 * @param  requestPathArr of the method endpoint
	 */
	public List<String> findValidKeys(String[] requestPathArr) {
		// Return list of results
		List<String> ret = new ArrayList<>();

		// Lets iterate each key and array
		for( String endpoint : this.keySet() ) {
			// Get the key array from the cache
			String[] endpointPathArr = splitUriString(endpoint);

			if(isValidEndpoint(endpointPathArr, requestPathArr)){
				ret.add(endpoint);
			}

		}

		// Return found result
		return ret;
	}


	public List<String> findValidKeys(String path) {
		return findValidKeys(ServletStringUtil.splitUriString(path));
	}

}