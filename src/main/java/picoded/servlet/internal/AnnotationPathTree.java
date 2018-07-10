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
 * annotation routes of a given class object.
 * 
 * And perform the subsquent request/api call for it.
 **/
public class AnnotationPathTree {
	
	///////////////////////////////////////////////////////
	//
	// Consturctor and method list init
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Blank constructor
	 */
	public AnnotationPathTree() {
		super();
	}
	
	/**
	 * Consturctor with BasePage
	 * 
	 * @param  page  BasePage instance object
	 */
	public AnnotationPathTree(BasePage page) {
		super();
		this.registerClass( AnnotationUtil.extractClass(page) );
	}

	/**
	 * Relevent method list for this current endpoint
	 */
	public List<Method> methodList = new ArrayList<>();

	/**
	 * Nested path tree digram, for full path matching
	 */
	public Map<String, AnnotationPathTree> pathTree = new HashMap<>();
	
	///////////////////////////////////////////////////////
	//
	// Static caching of class annotation paths
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Using a local static concurrent hash map, for caching of path tree data
	 */
	private static final Map<Class<?>,AnnotationPathTree> pathTreeCache = new ConcurrentHashMap<>();

	/**
	 * Given a BasePage instance, extract out the relevent AnnotationPathTree information,
	 * and cache it if possible.
	 * 
	 * @param  page  BasePage instance object
	 */
	public static AnnotationPathTree setupAndCachePagePathTree(BasePage page) {
		// Get the class object representation
		Class<?> classObj = page.getClass();

		// Get from the cache first if possible
		AnnotationPathTree ret = pathTreeCache.get(classObj);
		if( ret != null ) {
			return ret;
		}

		// Cache not found, lets recreate this
		ret = new AnnotationPathTree(page);

		// And store in cache, + return it
		pathTreeCache.put(classObj, ret);
		return ret;
	}

	///////////////////////////////////////////////////////
	//
	// Annotation path mapping
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Get annotation endpoint path, or initialize it if it does not exist
	 * 
	 * @param   path  to fetch annotation
	 * 
	 * @return  AnootationPathMap at the respective path
	 */
	public AnnotationPathTree initAnnotationPath(String path) {
		return fetchAnnotationPath(ServletStringUtil.splitUriString(path), true);
	}
	
	/**
	 * Get annotation endpoint path, return null if not found
	 * 
	 * @param   path  to fetch annotation
	 * 
	 * @return  AnootationPathMap at the respective path
	 */
	public AnnotationPathTree getAnnotationPath(String path) {
		return fetchAnnotationPath(ServletStringUtil.splitUriString(path), false);
	}
	
	/**
	 * Get annotation endpoint path, return null if not found
	 * 
	 * @param   path  to fetch annotation
	 * 
	 * @return  AnootationPathMap at the respective path
	 */
	public AnnotationPathTree getAnnotationPath(String[] path) {
		return fetchAnnotationPath(path, false);
	}
	
	/**
	 * Get annotation endpoint path
	 * 
	 * @param   splitPath  to fetch annotation
	 * @param   init      boolean flag to initialize endpoint if it does not exist
	 * 
	 * @return  AnootationPathMap at the respective path
	 */
	protected AnnotationPathTree fetchAnnotationPath(String[] splitPath, boolean init) {
		// Annotation path map to recusively fetch from
		AnnotationPathTree step = this;
		
		// For each annotation path part, 
		// find the nested path map 
		for (String part : splitPath) {
			// Skips processing if part is blank
			if (part.length() == 0) {
				continue;
			}
			
			if (init) {
				// Get the nested path map
				// And initialize it if it does not exist
				AnnotationPathTree nextStep = step.pathTree.get(part);
				if (nextStep == null) {
					nextStep = new AnnotationPathTree();
					step.pathTree.put(part, nextStep);
				}
				step = nextStep;
			} else {
				// Get the nested path map
				// and return null if its not initialized
				step = step.pathTree.get(part);
				if (step == null) {
					return null;
				}
			}
		}
		
		// Return the annotation path mapping
		return step;
	}
	
	///////////////////////////////////////////////////////
	//
	// Class mapping
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Import and scan the given class object for relevent 
	 * annotations and map it accordingly internally
	 **/
	public void registerClass(Class<?> classObj) {
		// Map the class methods
		registerClassMethods(classObj);
	}
	
	/**
	 * Import and scan the given class object for relevent 
	 * annotations and map its methods accordingly internally
	 */
	public void registerClassMethods(Class<?> classObj) {
		// Lets get the list of methods
		List<Method> methodList = AnnotationUtil.fetchMethodList(classObj);
		
		// Map the method list with annotation class
		for (Method methodObj : methodList) {
			// Get and process each type of annotation we currently support for methods
			//
			// Minor note : Because annotation is not extendable, we cant fully refactor 
			// the duplicative loop into a generic function, that is reusable.
			for (RequestPath pathObj : methodObj.getAnnotationsByType(RequestPath.class)) {
				registerMethod(pathObj.value(), methodObj);
			}
			for (RequestBefore pathObj : methodObj.getAnnotationsByType(RequestBefore.class)) {
				registerMethod(pathObj.value(), methodObj);
			}
			for (RequestAfter pathObj : methodObj.getAnnotationsByType(RequestAfter.class)) {
				registerMethod(pathObj.value(), methodObj);
			}
		}
	}
	
	/**
	 * Register a method endpoint
	 * 
	 * @param  path of the method endpoint
	 * @param  methodObj to register
	 */
	protected void registerMethod(String path, Method methodObj) {
		AnnotationPathTree pathObj = initAnnotationPath(path);
		if (pathObj.methodList.indexOf(methodObj) < 0) {
			pathObj.methodList.add(methodObj);
		}
	}
	
	///////////////////////////////////////////////////////
	//
	// Valid path searching
	//
	///////////////////////////////////////////////////////
	
	// /**
	//  * Get and returns all possible valid paths that is applicable
	//  * for the given the request URI
	//  */
	// public String[] listApplicablePaths(String path) {
	// 	String[] splitPath = ServletStringUtil.splitUriString(path);

	// 	// Temporary code : over simplified fetching 

	// }
}