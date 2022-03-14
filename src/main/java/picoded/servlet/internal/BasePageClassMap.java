package picoded.servlet.internal;

import java.io.PrintWriter;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import picoded.servlet.*;
import picoded.servlet.annotation.*;

import javax.servlet.http.HttpServletRequest;

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
		this.registerClass(AnnotationUtil.extractClass(page));
	}
	
	/**
	 * Consturctor with class object
	 *
	 * @param  classObj  BasePage class object
	 */
	public BasePageClassMap(Class<?> classObj) {
		super();
		this.registerClass(classObj);
	}
	
	///////////////////////////////////////////////////////
	//
	// Static caching of initialized BasePageClassMap
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Using a local static concurrent hash map, for caching of path tree data
	 */
	private static final Map<Class<?>, BasePageClassMap> instanceCache = new ConcurrentHashMap<>();
	
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
		if (ret != null) {
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
	protected Class<?> pageClass = null;
	/** List of methods used in before filters */
	protected EndpointMap<Method> beforeMap = new EndpointMap<>();
	
	/** List of methods used for pathing execution */
	protected EndpointMap<Method> pathMap = new EndpointMap<>();
	/** List of methods used for API execution */
	protected EndpointMap<Method> apiMap = new EndpointMap<>();
	/** List of fields used for path rerouting */
	protected EndpointMap<Field> rerouteFieldMap = new EndpointMap<>();
	/** List of methods used for path rerouting */
	protected EndpointMap<Method> rerouteMethodMap = new EndpointMap<>();
	
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
	 *
	 * @param  classObj to scan and import for methods
	 */
	protected void registerClassMethods(Class<?> classObj) {
		// Lets get the list of methods
		List<Method> methodList = AnnotationUtil.fetchMethodList(classObj);
		
		// Map the method list with annotation class
		for (Method methodObj : methodList) {
			// Register the method accordingly
			registerMethod(classObj, methodObj);
		}
	}
	
	/**
	 * Scans a single method for valid enpoint registrations
	 *
	 * @param classObj
	 * @param methodObj
	 */
	protected void registerMethod(Class<?> classObj, Method methodObj) {
		// Get and process each type of annotation we currently support for methods
		//
		// Minor note : Because annotation is not extendable, we cant fully refactor
		// the duplicative loop into a generic function, that is reusable.
		for (RequestBefore pathObj : methodObj.getAnnotationsByType(RequestBefore.class)) {
			beforeMap.registerEndpointPath(pathObj.value(), methodObj);
		}
		for (RequestAfter pathObj : methodObj.getAnnotationsByType(RequestAfter.class)) {
			afterMap.registerEndpointPath(pathObj.value(), methodObj);
		}
		
		//
		// Checks for a return type,
		// if its extended from BasePage - assuming its a redirect method
		//
		
		Class<?> retMethod = methodObj.getReturnType();
		
		//
		// if its extended from BasePage - assuming its a redirect method
		// and ends the process
		//
		if (retMethod != null && BasePage.class.isAssignableFrom(retMethod)) {
			for (RequestPath pathObj : methodObj.getAnnotationsByType(RequestPath.class)) {
				rerouteMethodMap.registerEndpointPath(pathObj.value(), methodObj);
			}
			for (ApiPath pathObj : methodObj.getAnnotationsByType(ApiPath.class)) {
				rerouteMethodMap.registerEndpointPath(pathObj.value(), methodObj);
			}
			return;
		}
		
		//
		// Assumes its a standard request method from here onwards
		//
		for (RequestPath pathObj : methodObj.getAnnotationsByType(RequestPath.class)) {
			pathMap.registerEndpointPath(pathObj.value(), methodObj);
		}
		for (ApiPath pathObj : methodObj.getAnnotationsByType(ApiPath.class)) {
			apiMap.registerEndpointPath(pathObj.value(), methodObj);
		}
	}
	
	/**
	 * Import and scan the given class object for relevent,
	 * annotations and map its fields accordingly internally.
	 */
	protected void registerClassFields(Class<?> classObj) {
		// Get the list of fields (for potential rerouting), filter it, and map it
		List<Field> fieldList = AnnotationUtil.fetchFieldList(classObj);
		
		// Map the field list with relevent annotation class
		for (Field field : fieldList) {
			// Get and process each type of annotation we currently support for fields
			for (RequestPath path : field.getAnnotationsByType(RequestPath.class)) {
				rerouteFieldMap.registerEndpointPath(path.value(), field);
			}
		}
	}
	
	///////////////////////////////////////////////////////
	//
	// Endpoint getter functions
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Retrieves the api endpoints of this class
	 * @return
	 */
	public EndpointMap<Method> apiEndpoints() {
		return apiMap;
	}
	
	/**
	 * Retrieves the rerouting fields of this class
	 * @return
	 */
	public EndpointMap<Field> reroutePaths() {
		return rerouteFieldMap;
	}
	
	/**
	 * From the given class, performs a recursive loop to generate the list of endpoints that appends the rerouting
	 * path to the internal class's @ApiPath and store it into the Map<String, Method>.
	 *
	 * Do note that due to object referencing, we do not need to perform returning values, instead the endpoints object
	 * refers to the same object throughout
	 *
	 * @param currentPath the current rerouting path String required to be append, the beginning path would usually be ""
	 * @param clazz       the Class to retrieve the methods and its reroute pathings, the initial Class would be the parent itself
	 * @param endpoints   the map that contains the entire rerouting paths and api paths
	 */
	public void getApiEndpointsFromClass(String currentPath, Class<?> clazz,
		Map<String, Method> endpoints) {
		// Remove the asterisk so that the path can be appended to the inner class's methods api endpoints
		if (currentPath.endsWith("/*")) {
			currentPath = currentPath.replaceAll("\\*$", "");
		}
		
		// Obtain the ClassMap so that we can retrieve the api endpoints as well as its rerouting paths
		BasePageClassMap basePageClassMap = BasePageClassMap.setupAndCache(clazz);
		
		EndpointMap<Method> apis = basePageClassMap.apiEndpoints();
		for (String key : apis.keySet()) {
			// NOTE: The first implementation will be taken! Future implementation will be ignored
			// This is so that only one endpoint exist at a time
			// @TODO: Need to do a logic in cases where methods are extended
			// Example: Account login method, and there is another class that extends this method
			endpoints.putIfAbsent(currentPath + key, apis.get(key));
			endpoints.putIfAbsent(key, apis.get(key));
		}
		
		// Recursively call through the reroute fields to retrieve the other api endpoints
		EndpointMap<Field> reroutePaths = basePageClassMap.reroutePaths();
		for (String key : reroutePaths.keySet()) {
			getApiEndpointsFromClass(currentPath + key, getRerouteClass(reroutePaths.get(key)),
				endpoints);
			getApiEndpointsFromClass(key, getRerouteClass(reroutePaths.get(key)), endpoints);
		}
	}
	
	///////////////////////////////////////////////////////
	//
	// route handling
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Given a field object, extract the BasePage class object
	 * for reroute detection
	 *
	 * @param  field to extract from
	 *
	 * @return extracted field class
	 */
	protected Class<?> getRerouteClass(Field field) {
		Class<?> ret = field.getType();
		if (!BasePage.class.isAssignableFrom(ret)) {
			throw new RuntimeException("Expected a BasePage class type, found " + ret.getName()
				+ " instead");
		}
		return ret;
	}
	
	/**
	 * Intiailize reroute BasePage class instance
	 *
	 * @param classObj to create an instance of
	 * @param page to transfer existing settings from
	 * @param rerouteField to check for existing instance
	 */
	protected BasePage setupRerouteClassInstance(Class<?> classObj, BasePage page, Field rerouteField) {
		// // Lets try getting the reroute field value itself (if possible)
		// try {
		// 	BasePage ret = (BasePage) (Object) rerouteField.get(page);
		// 	ret.transferParams(page);
		// 	return ret;
		// } catch (Exception e) {
		// 	// ignore, and does the fallback
		// }
		
		// Initialize a new instance if able
		try {
			BasePage ret = (BasePage) (Object) classObj.newInstance();
			ret.transferParams(page);
			return ret;
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Extract out the reroute subpath from current requestPath
	 * given the reroute declared endpoint.
	 *
	 * Note this does not validate if the routePath components is valid for requestPath,
	 * and performs the needed calculation, assuming it is.
	 *
	 * @param  requestPath to extract subpath
	 * @param  routePath to remove from requestPath
	 *
	 * @return reroute path
	 */
	protected String[] reroutePath(String[] requestPath, String routePath) {
		// Get the routePath parts count
		String[] splitRoutePath = rerouteFieldMap.splitUriString(routePath);
		int partsCount = splitRoutePath.length;
		
		// Remove the trailing '/*' in the parts count
		if (routePath.endsWith("/*")) {
			--partsCount;
		}
		
		// Get the reroute path
		return Arrays.copyOfRange(requestPath, partsCount, requestPath.length);
	}
	
	/**
	 * Checks and return if the given routePath is supported
	 *
	 * @param  requestPath to route path using
	 *
	 * @return true if route is found
	 */
	public boolean supportsRequestPath(String[] requestPath) {
		// Quick validation for path map, or api path map
		if (pathMap.findValidKeys(requestPath).size() > 0
			|| apiMap.findValidKeys(requestPath).size() > 0) {
			return true;
		}
		// Get list a reroute path
		List<String> pathList = rerouteFieldMap.findValidKeys(requestPath);
		
		// Return false (if no endpoint found)
		if (pathList == null || pathList.size() <= 0) {
			return false;
		}
		
		// Get a possibly valid endpoint reroute class
		String endpoint = pathList.get(0);
		Field fieldObj = rerouteFieldMap.get(endpoint);
		Class<?> routeClass = getRerouteClass(fieldObj);
		
		// Lets get the routeClass BasePageClassMap
		BasePageClassMap routeClassMap = BasePageClassMap.setupAndCache(routeClass);
		
		// And finally check if the route class can support the endpoint
		return routeClassMap.supportsRequestPath(reroutePath(requestPath, endpoint));
	}
	
	///////////////////////////////////////////////////////
	//
	// request handling
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
		if (request_api(page, routePath)) {
			return;
		}
		if (request_path(page, routePath)) {
			return;
		}
		if (request_methodReroute(page, routePath)) {
			return;
		}
		if (request_fieldReroute(page, routePath)) {
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
	protected boolean request_api(BasePage page, String[] requestPath) {
		try {
			// Get list of valid paths
			List<String> pathList = apiMap.findValidKeys(requestPath);
			
			// Return false (if no endpoint found)
			if (pathList == null || pathList.size() <= 0) {
				return false;
			}
			
			// Return the Method associated with a valid endpoint
			String annotationPath = pathList.get(0);
			Method toExecute = apiMap.get(annotationPath);
			
			// RequestBefore execution
			executeMethodMap(beforeMap, page, requestPath);
			
			// Execute the method
			executeMethod(page, toExecute, apiMap.splitUriString(annotationPath), requestPath);
			
			// RequestAfter execution
			executeMethodMap(afterMap, page, requestPath);
		} catch (ApiException ae) {
			throw ae;
		} catch (Exception e) {
			// Any exception will be thrown back to BasePage for handling
			Throwable cause = e;
			if (e.getCause() != null) {
				cause = e.getCause();
			}
			
			if (cause instanceof HaltException) {
				HaltException he = (HaltException) cause;
				throw he;
			}
			
			throw new ApiException(e);
		}
		
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
	protected boolean request_path(BasePage page, String[] requestPath) {
		// Get list of valid paths
		List<String> pathList = pathMap.findValidKeys(requestPath, page.requestType());
		
		// Return false (if no endpoint found)
		if (pathList == null || pathList.size() <= 0) {
			return false;
		}
		
		// Return the Method associated with a valid endpoint
		String annotationPath = pathList.get(0);
		Method toExecute = pathMap.get(annotationPath);
		
		// RequestBefore execution
		executeMethodMap(beforeMap, page, requestPath);
		
		// Execute the method
		executeMethod(page, toExecute, pathMap.splitUriString(annotationPath), requestPath);
		
		// RequestAfter execution
		executeMethodMap(afterMap, page, requestPath);
		
		// Assume valid execution
		return true;
	}
	
	/**
	 * Attempts to route a request with a valid ApiPath if found.
	 *
	 * @param  page to execute from
	 * @param  requestPath to route path using
	 *
	 * @return true if valid execution occurs
	 */
	protected boolean request_fieldReroute(BasePage page, String[] requestPath) {
		// Get list of valid paths
		List<String> pathList = rerouteFieldMap.findValidKeys(requestPath);
		
		// Return false (if no endpoint found)
		if (pathList == null || pathList.size() <= 0) {
			return false;
		}
		
		// Get the valid endpoint
		String endpoint = pathList.get(0);
		
		// Validate reroute endpoint ends with /*
		if (!endpoint.endsWith("/*")) {
			throw new RuntimeException("Reroute paths are suppose to end with '/*'");
		}
		
		// Return the Field associated with a valid endpoint
		Field rerouteField = rerouteFieldMap.get(endpoint);
		
		// Get the reroute target
		Class<?> routeClass = getRerouteClass(rerouteField);
		BasePageClassMap routeClassMap = BasePageClassMap.setupAndCache(routeClass);
		
		// To process the name parameters before it is removed
		processNameParameters(page.requestParameterMap(), apiMap.splitUriString(endpoint),
			requestPath);
		
		// Check if it supports rerouting
		String[] reroutePathArr = reroutePath(requestPath, endpoint);
		if (!routeClassMap.supportsRequestPath(reroutePathArr)) {
			return false;
		}
		
		// RequestBefore execution
		executeMethodMap(beforeMap, page, requestPath);
		
		// Execute the reroute, with the routing class
		// @TODO: handle name parameters in routePath (e.g. :user)
		BasePage routeClassObj = setupRerouteClassInstance(routeClass, page, rerouteField);
		routeClassMap.handleRequest(routeClassObj, reroutePathArr);
		
		// RequestAfter execution
		executeMethodMap(afterMap, routeClassObj, requestPath);
		
		// Assume valid execution
		return true;
	}
	
	/**
	 * Attempts to route a request with a valid ApiPath if found.
	 *
	 * @param  page to execute from
	 * @param  requestPath to route path using
	 *
	 * @return true if valid execution occurs
	 */
	protected boolean request_methodReroute(BasePage page, String[] requestPath) {
		// Get list of valid paths
		List<String> pathList = rerouteMethodMap.findValidKeys(requestPath);
		
		// Return false (if no endpoint found)
		if (pathList == null || pathList.size() <= 0) {
			return false;
		}
		
		// Get the valid endpoint
		String endpoint = pathList.get(0);
		
		// Validate reroute endpoint ends with /*
		if (!endpoint.endsWith("/*")) {
			throw new RuntimeException("Reroute paths are suppose to end with '/*'");
		}
		
		// Return the method associated with a valid endpoint
		Method rerouteMethod = rerouteMethodMap.get(endpoint);
		if (rerouteMethod == null) {
			return false;
		}
		
		// RequestBefore execution
		executeMethodMap(beforeMap, page, requestPath);
		
		//
		// I should instead, execute the method, get its expected object
		// which will be extended from BasePage
		//
		// I will then check the return object, throws equivalent of 404 if null
		// else I will subsequently get its class, and perform the "setupAndCache"
		// for the route mapping
		//
		// Then using the return object, I will do a `transferParams` on the instance
		// followed by the appropriate `handleRequest` sub call
		//
		
		try {
			
			Object returnObj = rerouteMethod.invoke(page, new Object[0]);
			if (returnObj == null) {
				page.handleMissingRouteFailure();
			}
			if (returnObj instanceof BasePage) {
				
				BasePage nextPage = (BasePage) returnObj;
				
				// Setup the route mapping
				BasePageClassMap routeClassMap = BasePageClassMap.setupAndCache(nextPage);
				
				// Return false if the nextPage does not contain the path for this request
				String[] reroutePathArr = reroutePath(requestPath, endpoint);
				if (!routeClassMap.supportsRequestPath(reroutePathArr)) {
					return false;
				}
				
				// Transfer page's settings to the nextPage
				nextPage.transferParams(page);
				routeClassMap.handleRequest(nextPage, reroutePathArr);
				
				executeMethodMap(afterMap, page, requestPath);
				
			} else {
				page.handleMissingRouteFailure();
			}
			
		} catch (IllegalAccessException iae) {
			throw new RuntimeException(iae);
		} catch (InvocationTargetException ite) {
			throw new RuntimeException(ite);
		}
		// // Get the reroute target
		// Class<?> routeClass = getRerouteClass(rerouteField);
		// BasePageClassMap routeClassMap = BasePageClassMap.setupAndCache(routeClass);
		
		// // Check if it supports rerouting
		// String[] reroutePathArr = reroutePath(requestPath, endpoint);
		// if (!routeClassMap.supportsRequestPath(reroutePathArr)) {
		// 	return false;
		// }
		
		// // Execute the reroute, with the routing class
		// // @TODO: handle name parameters in routePath (e.g. :user)
		// BasePage routeClassObj = setupRerouteClassInstance(routeClass, page, rerouteField);
		// routeClassMap.handleRequest(routeClassObj, reroutePathArr);
		
		// RequestAfter execution
		//		executeMethodMap(afterMap, routeClassObj, requestPath);
		
		// Assume valid execution
		return true;
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
		for (String annotationPath : pathList) {
			Method toExecute = methodMap.get(annotationPath);
			executeMethod(page, toExecute, methodMap.splitUriString(annotationPath), routePath);
		}
	}
	
	/**
	 * From the annotationPath, extract any name parameters that contains ":" and grab the exact value from
	 * requestPath and put them into the paramMap with parameter as key and the requestPath's value as value
	 */
	protected void processNameParameters(ServletRequestMap paramMap, String[] annotationPath,
		String[] requestPath) {
		for (int index = 0; index < annotationPath.length; index++) {
			String subpath = annotationPath[index];
			// Identify the name parameters in the annotationPath and put the exact value from requestPath into paramMap
			if (subpath.startsWith(":")) {
				paramMap.put(subpath.substring(1), requestPath[index]);
			}
		}
	}
	
	/**
	 * Execute the given method in the context of the current class object (this)
	 * Adapting the given parameters according to the expected parameter types.
	 *
	 * Also does the relevent output processing based on the output types
	 * 
	 * However, this method will not handle the output for exceptions and it will be thrown to
	 * the caller of this method. Basically, at the very end, the BasePage will be the one that is handling
	 * the output for such exception
	 *
	 * Map / List - JSON output
	 * String / StringBuilder - println output
	 * File - binary file output
	 * byte[] - binary output
	 * void - does nothing
	 *
	 * Similarly, it passes in the following values, according to the parameter type
	 *
	 * StringBuilder - previously StringBuilder output
	 *
	 * PrintWriter / OutputStream - respective output objects
	 * HttpServletRequest / Response - respective request / response specific objects
	 * ServletRequestMap / Map - ServletRequestMap
	 *
	 * ```
	 * @RequestPath("hello")
	 * public void hello(PrintWriter writer) {
	 * 	writer.println("hello");
	 * }
	 *
	 * @RequestPath("hello/:name")
	 * public void hello(PrintWriter writer, ServletRequestMap param) {
	 * 	writer.println("hello "+param.getString("name", "stranger"));
	 * }
	 * ```
	 *
	 * @param  page to execute from
	 * @param  toExecute method to execute
	 */
	protected void executeMethod(BasePage page, Method toExecute, String[] annotationPath,
		String[] requestPath) {
		
		// Using the list of parameters that the method requires, detect the appropriate
		// variables to pass to the method
		Class<?>[] parameterTypes = toExecute.getParameterTypes();
		List<Object> arguments = new ArrayList<>();
		
		// Process name parameters in annotationPath
		processNameParameters(page.requestParameterMap(), annotationPath, requestPath);
		
		//
		// Input handling
		//
		
		try {
			for (Class<?> type : parameterTypes) {
				// How isAssignableFrom works:
				// Map.class.isAssignableFrom(ServletRequestMap) translate to
				// Map<String, Object> map = new ServletRequestMap(page.getHttpServletRequest());
				// Map is the parent class and ServletRequestMap is the child class
				
				if (PrintWriter.class.isAssignableFrom(type)) {
					arguments.add(page.getPrintWriter());
					
				} else if (ServletRequestMap.class.isAssignableFrom(type)) {
					arguments.add(page.requestParameterMap());
					
				} else if (ApiResponseMap.class.isAssignableFrom(type)) {
					arguments.add(page.getApiResponseMap());
					
				} else if (Map.class.isAssignableFrom(type)) {
					arguments.add(page.requestParameterMap());
					
				} else if (HttpServletRequest.class.isAssignableFrom(type)) {
					arguments.add(page.getHttpServletRequest());
					
				} else if (StringBuilder.class.isAssignableFrom(type)) {
					arguments.add(page.getResponseStringBuilder());
					
				} else {
					throw new RuntimeException("Unsupported type in method " + toExecute.getName()
						+ " for parameter type " + type.getSimpleName());
				}
			}
		} catch (RuntimeException e) {
			ApiException ae = new ApiException(e);
			page.handleApiException(ae);
			return;
		}
		
		//
		// Page type safety handling
		//
		Class<?> pageClass = toExecute.getDeclaringClass();
		if (!pageClass.isInstance(page)) {
			/*
			// Get the original page
			BasePage oriPage = page;

			// Initialize the replacement page
			// @TODO - to potentially cache this instance
			try {
				Constructor<?> constructor = pageClass.getConstructor();
				page = (BasePage)(constructor.newInstance());
			} catch(Exception e) {
				throw new RuntimeException(e);
			}

			// Perform the transfer of params
			page.transferParams(oriPage);
			 */
			
			// @TODO - resolve doAfter issue for multiple nested classes
			return;
		}
		
		//
		// Execution handling
		//
		
		Object executionResponse = null;
		try {
			// Invoke the method
			executionResponse = toExecute.invoke(page, arguments.toArray());
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			// Any exception will be thrown back to BasePage for handling
			Throwable cause = e;
			if (e.getCause() != null) {
				cause = e.getCause();
			}
			if (cause instanceof ApiException) {
				ApiException ae = (ApiException) cause;
				throw ae;
			}
			
			if (cause instanceof HaltException) {
				HaltException he = (HaltException) cause;
				// Throw the HaltException upwards
				// Main idea is that as long as one of the methods throw a HaltException
				// it will just stop processing
				throw he;
			}
			
			throw new RuntimeException(cause);
		}
		
		//
		// Output handling
		//
		
		// Get the return type
		Class<?> returnType = toExecute.getReturnType();
		
		// Does File based processing
		if (returnType == File.class) {
			if (executionResponse != null) {
				// This will handle multipart handling
				page.sendFile((File) executionResponse);
			} else {
				// This will automatically do 404
				page.sendFile(null);
			}
			return;
		}
		
		if (executionResponse == null) {
			return;
		}
		
		if (executionResponse instanceof BasePage) {
			// @TODO : relay request
		}
		
		if (executionResponse instanceof Map) {
			Map<String, Object> executionMap = (Map<String, Object>) executionResponse;
			if (!executionMap.equals(page.getApiResponseMap())) {
				// @TODO: Recursive merge instead of replacing the entire variable
				page.getApiResponseMap().putAll(executionMap);
			}
			return;
		}
		
		if (executionResponse instanceof StringBuilder) {
			StringBuilder executionStringBuilder = (StringBuilder) executionResponse;
			if (!executionStringBuilder.equals(page.getResponseStringBuilder())) {
				page.getResponseStringBuilder().append(executionStringBuilder);
			}
			return;
		}
		
		if (executionResponse instanceof String) {
			page.getResponseStringBuilder().append(executionResponse.toString());
			return;
		}
		
		// @TODO - consider output failure for unknown non null value ??
	}
	
}
