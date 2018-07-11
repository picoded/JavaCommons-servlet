package picoded.servlet.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
	 */
	protected void route(BasePage page, String[] routePath) {

		// @TODO : RequestPath / ApiPath method fetching (only 1 valid result)
		// @TODO : RequestPath class reroute fetching (only 1 valid result)

		// @TODO : 404 exception handaling

		// @TODO : RequestBefore handling + execution
		
		// @TODO : method execution

		// @TODO : RequestAfter handling + execution

		// throw new RuntimeException( "404 error should occur here" ) ?
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