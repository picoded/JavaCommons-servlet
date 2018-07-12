package picoded.servlet.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
 * annotation routes of a given class object.
 * 
 * And perform the subsquent request/api call for it.
 * 
 * Note that this class map is designed for concurrent access by multiple threads
 **/
public class BasePageClassMap {
	
	///////////////////////////////////////////////////////
	//
	// Consturctor and method list init
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Blank constructor
	 */
	public BasePageClassMap() {
		super();
	}
	
	/**
	 * Consturctor with BasePage
	 * 
	 * @param  page  BasePage instance object
	 */
	public BasePageClassMap(BasePage page) {
		super();
		this.registerClass( AnnotationUtil.extractClass(page) );
	}

	/**
	 * Consturctor with class object
	 * 
	 * @param  classObj  BasePage class object
	 */
	public BasePageClassMap(Class<?> classObj) {
		super();
		this.registerClass( classObj );
	}

	///////////////////////////////////////////////////////
	//
	// Static caching of initialized BasePageClassMap
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Using a local static concurrent hash map, for caching of path tree data
	 */
	private static final Map<Class<?>,BasePageClassMap> instanceCache = new ConcurrentHashMap<>();

	/**
	 * Given a BasePage instance, extract out the relevent BasePageClassMap information,
	 * and cache it if possible.
	 * 
	 * @param  page  BasePage instance object
	 */
	public static BasePageClassMap setupAndCache(BasePage page) {
		return setupAndCache(page.getClass());
	}

	/**
	 * Given a BasePage class, extract out the relevent BasePageClassMap information,
	 * and cache it if possible.
	 * 
	 * @param  classObj  BasePage clas
	 */
	public static BasePageClassMap setupAndCache(Class<?> classObj) {
		// Get from the cache first if possible
		BasePageClassMap ret = instanceCache.get(classObj);
		if( ret != null ) {
			return ret;
		}

		// Cache not found, lets recreate this
		ret = new BasePageClassMap(classObj);

		// And store in cache, + return it
		instanceCache.put(classObj, ret);
		return ret;
	}

	///////////////////////////////////////////////////////
	//
	// Class mapping
	//
	///////////////////////////////////////////////////////
	
	/** Class object representation of BasePage instance used in BasePageClassMap */
	protected Class<?>  pageClass = null;
	/** List of methods used in before filters */
	protected EndpointMap<Method> beforeMap = new EndpointMap<>();

	/** List of methods used for pathing execution */
	protected EndpointMap<Method> pathMap = new EndpointMap<>();
	/** List of methods used for API execution */
	protected EndpointMap<Method> apiMap = new EndpointMap<>();
	/** Lisf of fields used for path rerouting */
	protected EndpointMap<Field> rerouteMap = new EndpointMap<>();

	/** List of methods used in after filters */
	protected EndpointMap<Method> afterMap = new EndpointMap<>();

	/**
	 * Import and scan the given class object for relevent 
	 * annotations and map it accordingly internally to this BasePageClassMap
	 **/
	protected void registerClass(Class<?> classObj) {
		// Setup the class object refrence
		pageClass = classObj;
		// Map the class methods and fields
		registerClassMethods(classObj);
		registerClassFields(classObj);
	}
	
	/**
	 * Import and scan the given class object for relevent 
	 * annotations and map its methods accordingly internally
	 */
	protected void registerClassMethods(Class<?> classObj) {
		// Lets get the list of methods
		List<Method> methodList = AnnotationUtil.fetchMethodList(classObj);
		
		// Map the method list with annotation class
		for (Method methodObj : methodList) {
			// Get and process each type of annotation we currently support for methods
			//
			// Minor note : Because annotation is not extendable, we cant fully refactor 
			// the duplicative loop into a generic function, that is reusable.
			for (RequestBefore pathObj : methodObj.getAnnotationsByType(RequestBefore.class)) {
				beforeMap.registerEndpointPath(pathObj.value(), methodObj);
			}
			for (RequestPath pathObj : methodObj.getAnnotationsByType(RequestPath.class)) {
				pathMap.registerEndpointPath(pathObj.value(), methodObj);
			}
			for (ApiPath pathObj : methodObj.getAnnotationsByType(ApiPath.class)) {
				apiMap.registerEndpointPath(pathObj.value(), methodObj);
			}
			for (RequestAfter pathObj : methodObj.getAnnotationsByType(RequestAfter.class)) {
				afterMap.registerEndpointPath(pathObj.value(), methodObj);
			}
		}

	}

	/**
	 * Import and scan the given class object for relevent 
	 * annotations and map its fields accordingly internally
	 */
	protected void registerClassFields(Class<?> classObj) {
		// Get the list of fields (for potential rerouting), filter it, and map it
		List<Field> fieldList = AnnotationUtil.fetchFieldList(classObj);
		
		// Map the field list with relevent annotation class
		for (Field fieldObj : fieldList) {
			// Get and process each type of annotation we currently support for fields
			for(RequestPath pathObj : fieldObj.getAnnotationsByType(RequestPath.class) ) {
				rerouteMap.registerEndpointPath(pathObj.value(), fieldObj);
			}
		}
	}

	///////////////////////////////////////////////////////
	//
	// route handling
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Takes the existing request, and perform routing logic on it respectively.
	 * 
	 * This is automatically called on any request, or alternatively when  
	 * forwarding to another instnce request.
	 * 
	 * @param  page to execute from
	 * @param  routePath to route path using
	 */
	public void handleRequest(BasePage page, String[] routePath) {
		// Try to use the various routing options
		if( request_api(page, routePath) ) {
			return;
		}
		if( request_path(page, routePath) ) {
			return;
		}
		if( request_reroute(page, routePath) ) {
			return;
		}

		// If none is found throws a 404 =(
		page.handleMissingRouteFailure();
	}

	/**
	 * Attempts to route a request with a valid ApiPath if found.
	 * 
	 * @param  page to execute from
	 * @param  routePath to route path using
	 * 
	 * @return true if valid execution occurs
	 */
	protected boolean request_api(BasePage page, String[] routePath) {
		return false;
	}

	/**
	 * Attempts to route a request with a valid ApiPath if found.
	 * 
	 * @param  page to execute from
	 * @param  routePath to route path using
	 * 
	 * @return true if valid execution occurs
	 */
	protected boolean request_path(BasePage page, String[] routePath) {
		// Get list of valid paths
		List<String> pathList = pathMap.findValidKeys(routePath);

		// Return false (if no endpoint found)
		if( pathList == null || pathList.size() <= 0 ) {
			return false;
		}

		// Return the Method associated with a valid endpoint
		Method toExecute = pathMap.get( pathList.get(0) );

		// RequestBefore execution
		executeMethodMap(beforeMap, page, routePath);
		
		// Execute the method
		executeMethod(page, toExecute);

		// RequestAfter execution
		executeMethodMap(afterMap, page, routePath);
		
		// Assume valid execution
		return true;
	}

	/**
	 * Attempts to route a request with a valid ApiPath if found.
	 * 
	 * @param  page to execute from
	 * @param  routePath to route path using
	 * 
	 * @return true if valid execution occurs
	 */
	protected boolean request_reroute(BasePage page, String[] routePath) {
		return false;
	}

	///////////////////////////////////////////////////////
	//
	// Method execution handling
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Execute all relevent request methods, found in an endpointMap for a routePath
	 * 
	 * @param  methodMap to execute from
	 * @param  page to execute from
	 * @param  routePath to route path using
	 */
	protected void executeMethodMap(EndpointMap<Method> methodMap, BasePage page, String[] routePath) {
		// Get list of valid paths
		List<String> pathList = methodMap.findValidKeys(routePath);

		// and execute all its relevent method
		for(String path : pathList) {
			Method toExecute = methodMap.get(path);
			executeMethod(page, toExecute);
		}
	}
	
	/**
	 * Execute the given method in the context of the current class object (this)
	 * Adapting the given parameters according to the expected parameter types.
	 * 
	 * Also does the relevent output processing based on the output types
	 * 
	 * Map / List - JSON output
	 * String / StringBuilder - println output
	 * File - binary file output
	 * byte[] - binary output
	 * void - does nothing
	 * 
	 * Similarly, it passes in the following values, according to the parameter type
	 * 
	 * PrintWriter / OutputStream - respective output objects
	 * HttpServletRequest / Response - respective request / response specific objects
	 * ServletRequestMap / Map - ServletRequestMap
	 * 
	 * @param  page to execute from
	 * @param  toExecute method to execute
	 */
	protected void executeMethod(BasePage page, Method toExecute) {
		// @TODO : Does method detection and parameter / output logic respectively

		// Invoke the method
		try {
			toExecute.invoke(page);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

}