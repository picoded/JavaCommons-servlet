package picoded.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.*;

// Exceptions used
import java.io.IOException;

// Objects used
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;

// Apache library used
import org.apache.commons.io.FilenameUtils;

// JavaCommons library used
import picoded.core.conv.ConvertJSON;
import picoded.core.common.EmptyArray;
import picoded.core.struct.ArrayListMap;
import picoded.servlet.util.FileServlet;

import picoded.core.common.HttpRequestType;

/**
 * JavaCommons.servlet page core system, in which all additional page are extended from.
 * In addition, this is intentionally structured to be "usable" even without the understanding / importing of
 * the various HttpServlet functionalities. Though doing so is still highly recommended.
 * 
 * JavaCommons.servlet are all designed to be re initiated for each thread request, ensuring class instance
 * isolation between various request by default.
 *
 * Note that internally, doPost, doGet creates a new class instance for each call/request it recieves.
 * As such, all subclass built can consider all servlet instances are fresh instances on process request.
 *
 * ---------------------------------------------------------------------------------------------------------
 *
 * ## Process flow
 * <pre>
 * {@code
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * [CorePage request process flow]
 *
 * doOption ---------+--> spawnInstance().setupInstance(...).processChain(...)
 *                   |         |
 * doPost -----------+     doSharedSetup
 *                   |     && doRequestSetup
 * doGet ------------+         |
 *                   |     doRequest --> do_X_Request --> outputRequest
 * doDelete ---------+                                          |
 *                   |                                    doRequestTearDown
 * doPut ------------+                                 && doSharedTeardown
 *                   |
 * doHead -----------/
 *
 * [CorePage lifecycle process flow]
 *
 * contextInitialized --> doSharedSetup -----> initializeContext
 * contextDestroyed ----> doSharedTeardown --> destroyContext
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * }
 * </pre>
 * ---------------------------------------------------------------------------------------------------------
 *
 * ## [TODO]
 * + Websocket support?
 **/
public class CorePage extends javax.servlet.http.HttpServlet implements ServletContextListener {
	
	// Java object serilization number
	private static final long serialVersionUID = 1L;
	
	///////////////////////////////////////////////////////
	//
	// Constructor and spawnInstance
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Blank constructor, used for template building, unit testing, etc
	 **/
	public CorePage() {
		super();
	}
	
	/**
	 * Spawn an instance of the current class
	 **/
	public CorePage spawnInstance() throws ServletException { //, OutputStream outStream
		try {
			Class<? extends CorePage> pageClass = this.getClass();
			CorePage ret = pageClass.newInstance();
			ret = pageClass.cast(ret);
			ret.applyServletConfig(this.getServletConfig());
			return ret;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Copy servlet config from the orginal instance, to a new instance
	 * 
	 * @param  servletConfig  servlet config to apply from the original page
	 **/
	protected void applyServletConfig(ServletConfig servletConfig) {
		try {
			if (servletConfig != null) {
				init(servletConfig);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	///////////////////////////////////////////////////////
	//
	// setupInstance with its respective variables
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Request type indicator
	 **/
	//protected byte requestType = 0;
	protected HttpRequestType requestType = null;
	
	/**
	 * The actual output stream used
	 **/
	protected OutputStream responseOutputStream = null;
	
	/**
	 * httpRequest used [modification of this value, is highly discouraged]
	 **/
	protected HttpServletRequest httpRequest = null;
	
	/**
	 * httpResponse used [modification of this value, is highly discouraged]
	 **/
	protected HttpServletResponse httpResponse = null;
	
	/**
	 * Setup the instance, with http request and response
	 **/
	protected CorePage setupInstance( //
		HttpRequestType inRequestType, HttpServletRequest req, //
		HttpServletResponse res //
	) throws ServletException {

		// Setup the local instance properties
		requestType = inRequestType;
		httpRequest = req;
		httpResponse = res;

		try {
			// UTF-8 enforcement
			httpRequest.setCharacterEncoding("UTF-8");
			
			// @TODO: To use IOUtils.buffer for inputstream of httpRequest / parameterMap
			// THIS IS CRITICAL, for the POST request in proxyServlet to work
			// requestParameters = RequestMap.fromStringArrayValueMap( httpRequest.getParameterMap() );
			
			responseOutputStream = httpResponse.getOutputStream();
		} catch (Exception e) {
			throw new ServletException(e);
		}

		// Return instance 
		return this;
	}
	
	/**
	 * Get the native http servlet request
	 **/
	public HttpServletRequest getHttpServletRequest() {
		return httpRequest;
	}
	
	/**
	 * Get the native http servlet response
	 **/
	public HttpServletResponse getHttpServletResponse() {
		return httpResponse;
	}
	
	///////////////////////////////////////////////////////
	//
	// Header and cookie map
	//
	///////////////////////////////////////////////////////
	
	/**
	 * The requested headers map, either set at startup or extracted from httpRequest
	 **/
	protected Map<String, String[]> _requestHeaderMap = null;
	
	/**
	 * Gets and returns the requestHeaderMap
	 **/
	public Map<String, String[]> requestHeaderMap() {
		// gets the constructor set cookies / cached cookies
		if (_requestHeaderMap != null) {
			return _requestHeaderMap;
		}
		
		// if the cached copy not previously set, and request is null, nothing can be done
		if (httpRequest == null) {
			return null;
		}
		
		// Creates the _requestHeaderMap from httpRequest
		ArrayListMap<String, String> mapList = new ArrayListMap<String, String>();
		
		// Get an Enumeration of all of the header names sent by the client
		Enumeration<String> headerNames = httpRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			
			// As per the Java Servlet API 2.5 documentation:
			//        Some headers, such as Accept-Language can be sent by clients
			//        as several headers each with a different value rather than
			//        sending the header as a comma separated list.
			// Thus, we get an Enumeration of the header values sent by the client
			mapList.append(name, httpRequest.getHeaders(name));
		}
		
		return _requestHeaderMap = mapList.toMapArray(new String[0]);
	}
	
	/**
	 * The requested cookie map, either set at startup or extracted from httpRequest
	 **/
	protected Map<String, String[]> _requestCookieMap = null;
	
	/**
	 * Gets and returns the requestCookieMap
	 **/
	public Map<String, String[]> requestCookieMap() {
		// gets the constructor set cookies / cached cookies
		if (_requestCookieMap != null) {
			return _requestCookieMap;
		}
		
		// if the cached copy not previously set, and request is null, nothing can be done
		if (httpRequest == null || httpRequest.getCookies() == null) {
			return null;
		}
		
		// Creates the _requestCookieMap from httpRequest
		ArrayListMap<String, String> mapList = new ArrayListMap<String, String>();
		for (Cookie oneCookie : httpRequest.getCookies()) {
			mapList.append(oneCookie.getName(), oneCookie.getValue());
		}
		
		// Cache and return
		return _requestCookieMap = mapList.toMapArray(new String[0]);
	}
	
	// ///////////////////////////////////////////////////////
	// //
	// // Static variables
	// //
	// ///////////////////////////////////////////////////////
	
	// /**
	//  * parameter map, either initialized from httpRequest, or directly
	//  **/
	// protected RequestMap requestParameters = null;
	
	// // Various request variables
	// //-------------------------------------------
	
	// ///////////////////////////////////////////////////////
	// //
	// // Instance config
	// //
	// ///////////////////////////////////////////////////////
	
	// // HTTP Servlet convinence functions
	// //-------------------------------------------
	
	// /**
	//  * Gets the server name
	//  */
	// public String getServerName() {
	// 	return httpRequest.getServerName();
	// }
	
	// public int getServerPort() {
	// 	return httpRequest.getServerPort();
	// }
	
	// /**
	//  * Gets the server requestURI
	//  **/
	// public String requestURI() {
	// 	return httpRequest.getRequestURI();
	// }
	
	// /**
	//  * Gets the request servlet path
	//  **/
	// public String requestServletPath() {
	// 	return httpRequest.getServletPath();
	// }
	
	// /**
	//  * Gets the serer wildcard segment of the URI
	//  * Note this does any URL decoding if needed, use httpRequest.getPathInfo() for the raw wild card path
	//  **/
	// public String requestWildcardUri() {
	// 	try {
	// 		String path = httpRequest.getPathInfo(); //no query values
	// 		if (path == null || path.isEmpty()) {
	// 			return null;
	// 		}
	// 		return FileUtil.normalize(URLDecoder.decode(path, "UTF-8").trim());
	// 	} catch (Exception e) {
	// 		return null;
	// 	}
	// }
	
	// public String[] requestWildcardUriArray() {
	// 	String raw = requestWildcardUri();
		
	// 	if (raw == null || raw.isEmpty()) {
	// 		return EmptyArray.STRING;
	// 	}
		
	// 	if (raw.startsWith("/") || raw.startsWith("\\")) {
	// 		raw = raw.substring(1);
	// 	}
		
	// 	if (raw.endsWith("/") || raw.endsWith("\\")) {
	// 		raw = raw.substring(0, raw.length() - 1);
	// 	}
		
	// 	return raw.split("[\\\\/]");
	// }
	
// 	//-------------------------------------------
// 	// Request type config getters
// 	//-------------------------------------------
	
// 	/**
// 	 * Returns the request type
// 	 **/
// 	public HttpRequestType requestType() {
// 		return requestType;
// 	}
	
// 	/**
// 	 * Returns the request parameters
// 	 **/
// 	public RequestMap requestParameters() {
// 		if (requestParameters != null) {
// 			return requestParameters;
// 		}
		
// 		requestParameters = new RequestMap(httpRequest);
		
// 		return requestParameters;
// 	}
	
// 	/**
// 	 * Returns if the request is GET
// 	 **/
// 	public boolean isGET() {
// 		return requestType == HttpRequestType.GET;
// 	}
	
// 	/**
// 	 * Returns if the request is POST
// 	 **/
// 	public boolean isPOST() {
// 		return requestType == HttpRequestType.POST;
// 	}
	
// 	/**
// 	 * Returns if the request is PUT
// 	 **/
// 	public boolean isPUT() {
// 		return requestType == HttpRequestType.PUT;
// 	}
	
// 	/**
// 	 * Returns if the request is DELETE
// 	 **/
// 	public boolean isDELETE() {
// 		return requestType == HttpRequestType.DELETE;
// 	}
	
// 	/**
// 	 * Returns if the request is OPTION
// 	 **/
// 	public boolean isOPTION() {
// 		return requestType == HttpRequestType.OPTION;
// 	}
	
// 	/**
// 	 * Setup the instance, with the request parameter, and
// 	 **/
// 	protected CorePage setupInstance(HttpRequestType inRequestType, Map<String, String[]> reqParam)
// 		throws ServletException {
// 		requestType = inRequestType;
// 		//requestParameters = new RequestMap( reqParam );
// 		return this;
// 	}
	
// 	/**
// 	 * Setup the instance, with the request parameter, and cookie map
// 	 **/
// 	protected CorePage setupInstance(HttpRequestType inRequestType, Map<String, String[]> reqParam,
// 		Map<String, Cookie[]> reqCookieMap) throws ServletException {
// 		requestType = inRequestType;
// 		//requestParameters = new RequestMap( reqParam );
// 		//requestCookieMap = reqCookieMap;
// 		return this;
// 	}
	
// 	///////////////////////////////////////////////////////
// 	//
// 	// Convinence functions
// 	//
// 	///////////////////////////////////////////////////////
	
// 	/**
// 	 * gets the PrintWriter, from the getOutputStream() object and returns it
// 	 **/
// 	public PrintWriter getWriter() {
// 		try {
// 			return new PrintWriter(new OutputStreamWriter(getOutputStream(), getHttpServletRequest()
// 				.getCharacterEncoding()), true);
// 		} catch (UnsupportedEncodingException e) {
// 			throw new RuntimeException(e);
// 		}
		
// 		// Important note: You will need to use "true" for auto flush.
// 		// "PrintWriter(Writer out, boolean autoFlush)", or it will NOT work.
// 		// return new PrintWriter(getOutputStream(), true);
// 	}
	
// 	/**
// 	 * gets the OutputStream, from the httpResponse.getOutputStream() object and returns it
// 	 * also surpresses IOException, as RuntimeException
// 	 **/
// 	public OutputStream getOutputStream() {
// 		return responseOutputStream;
// 	}
	
// 	/**
// 	 * Cached context path
// 	 **/
// 	protected String _contextPath = null;
	
// 	/**
// 	 * Gets and returns the context path / application folder path in absolute terms if possible
// 	 *
// 	 * This represents the FILE path in the native file system
// 	 **/
// 	public String getContextPath() {
// 		if (_contextPath != null) {
// 			return _contextPath;
// 		}
		
// 		if (httpRequest != null && httpRequest.getServletContext() != null) {
// 			return _contextPath = (httpRequest.getServletContext()).getRealPath("/") + "/";
// 		}
		
// 		if (_servletContextEvent != null) {
// 			ServletContext sc = _servletContextEvent.getServletContext();
// 			return _contextPath = sc.getRealPath("/") + "/";
// 		}
		
// 		try {
// 			// Note this may fail for contextInitialized
// 			return _contextPath = getServletContext().getRealPath("/") + "/";
// 		} catch (Exception e) {
// 			return _contextPath = "./";
// 		}
// 	}
	
// 	/**
// 	 * Cached context path
// 	 **/
// 	protected String _contextURI = null;
	
// 	/**
// 	 * Returns the whole server application contextual path : needed for base URI for page redirects / etc
// 	 *
// 	 * For root this is "" blank, however for example
// 	 * if deployed under "edge", it would be "/edge"
// 	 *
// 	 * This represents part of the URL path used in HTTP
// 	 **/
// 	public String getContextURI() {
// 		if (_contextURI != null) {
// 			return _contextURI;
// 		}
		
// 		if (httpRequest != null) {
// 			return _contextURI = httpRequest.getContextPath();
// 		}
		
// 		if (_servletContextEvent != null) {
// 			ServletContext sc = _servletContextEvent.getServletContext();
// 			return _contextURI = sc.getContextPath() + "/";
// 		}
		
// 		try {
// 			return (URLDecoder.decode(this.getClass().getClassLoader().getResource("/").getPath(),
// 				"UTF-8")).split("/WEB-INF/classes/")[0];
// 		} catch (UnsupportedEncodingException | NullPointerException e) {
// 			return "../";
// 		}
// 	}
	
// 	/**
// 	 * Returns the servlet contextual path : needed for base URI for page redirects / etc
// 	 * Note that this refers specifically to the current servlet request
// 	 **/
// 	public String getServletContextURI() {
// 		if (httpRequest != null) {
// 			return httpRequest.getServletPath();
// 		}
// 		//return getServletPath();
// 		throw new RuntimeException(
// 			"Unable to process getServletContextURI, outside of servlet request");
// 	}
	
// 	/**
// 	 * gets a parameter value, from the httpRequest.getParameter
// 	 **/
// 	public String getParameter(String paramName) {
// 		if (requestParameters() != null) {
// 			return requestParameters().getString(paramName);
// 		}
// 		return null;
// 	}
	
// 	/**
// 	 * Proxies to httpResponse.sendRedirect,
// 	 **/
// 	public void sendRedirect(String uri) {
// 		if (httpResponse != null) {
// 			try {
// 				httpResponse.sendRedirect(uri);
// 			} catch (IOException e) {
// 				throw new RuntimeException(e);
// 			}
// 			return;
// 		}
		
// 		// Fallsback to responseHeaderMap.location, if httpResponse is null
// 		//
// 		// if( responseHeaderMap == null ) {
// 		//	responseHeaderMap = new HashMap<String, String>();
// 		// }
// 		// responseHeaderMap.put("location", uri);
// 	}
	
// 	///////////////////////////////////////////////////////
// 	//
// 	// Native FileServlet and path handling
// 	//
// 	///////////////////////////////////////////////////////
	
// 	/**
// 	 * Cached FileServlet
// 	 **/
// 	protected FileServlet _outputFileServlet = null;
	
// 	/**
// 	 * Returns the File servlet
// 	 **/
// 	public FileServlet outputFileServlet() {
// 		if (_outputFileServlet != null) {
// 			return _outputFileServlet;
// 		}
// 		return (_outputFileServlet = new FileServlet(getContextPath()));
// 	}
	
// 	/**
// 	 * Checks and forces a redirection for closing slash on index page requests.
// 	 * If needed (returns false, on validation failure)
// 	 *
// 	 * For example : https://picoded.com/JavaCommons , will redirect to https://picoded.com/JavaCommons/
// 	 *
// 	 * This is a rather complicated topic. Regarding the ambiguity of the HTML
// 	 * redirection handling (T605 on phabricator)
// 	 *
// 	 * But basically take the following as example. On how a redirect is handled
// 	 * for a relative "./index.html" within a webpage.
// 	 *
// 	 * | Current URL              | Redirects to             |
// 	 * |--------------------------|--------------------------|
// 	 * | host/subpath             | host/index.html          |
// 	 * | host/subpath/            | host/subpath/index.html  |
// 	 * | host/subpath/index.html  | host/subpath/index.html  |
// 	 *
// 	 * As a result of the ambiguity in redirect for html index pages loaded
// 	 * in "host/subpath". This function was created, so that when called.
// 	 * will do any redirect if needed if the request was found to be.
// 	 *
// 	 * The reason for standardising to "host/subpath/" is that this will be consistent
// 	 * offline page loads (such as through cordova). Where the index.html will be loaded
// 	 * in full file path instead.
// 	 *
// 	 * 1) A request path withoug the "/" ending
// 	 *
// 	 * 2) Not a file request, a file request is assumed if there was a "." in the last name
// 	 *    Example: host/subpath/file.js
// 	 *
// 	 * 3) Not an API request with the "api" keyword. Example: host/subpath/api
// 	 *
// 	 * This will also safely handle the forwarding of all GET request parameters.
// 	 * For example: "host/subpath?abc=xyz" will be redirected to "host/subpath/?abc=xyz"
// 	 *
// 	 * Note: THIS will silently pass as true, if a httpRequest is not found. This is to facilitate
// 	 *       possible function calls done on servlet setup. Without breaking them
// 	 *
// 	 * Now that was ALOT of explaination for one simple function wasnt it >_>
// 	 * Well its one of the lesser understood "gotchas" in the HTTP specifications.
// 	 * Made more unknown by the JavaCommons user due to the common usage of ${PageRootURI}
// 	 * which basically resolves this issue. Unless its in relative path mode. Required for app exports.
// 	 **/
// 	protected boolean enforceProperRequestPathEnding() throws IOException {
// 		if (httpRequest != null) {
// 			String fullURI = httpRequest.getRequestURI();
			
// 			// This does not validate blank / root requests
// 			//
// 			// Should we? : To fix if this is required (as of now no)
// 			if (fullURI == null || fullURI.equalsIgnoreCase("/")) {
// 				return true;
// 			}
			
// 			//
// 			// Already ends with a "/" ? : If so its considered valid
// 			//
// 			if (fullURI.endsWith("/")) {
// 				return true;
// 			}
			
// 			//
// 			// Checks if its a file request. Ends check if it is
// 			//
// 			String name = FilenameUtils.getName(fullURI);
// 			if (FilenameUtils.getExtension(name).length() > 0) {
// 				// There is a file extension. so we shall assume it is a file
// 				return true; // And end it
// 			}
			
// 			//
// 			// Get the query string to append (if needed)
// 			//
// 			String queryString = httpRequest.getQueryString();
// 			if (queryString == null) {
// 				queryString = "";
// 			} else if (!queryString.startsWith("?")) {
// 				queryString = "?" + queryString;
// 			}
			
// 			//
// 			// Enforce proper URL handling
// 			//
// 			httpResponse.sendRedirect(fullURI + "/" + queryString);
// 			return false;
// 		}
		
// 		// Validation is valid.
// 		return true;
// 	}
	
// 	///////////////////////////////////////////////////////
// 	//
// 	// CORS Handling
// 	//
// 	///////////////////////////////////////////////////////
	
// 	/**
// 	 * Does a check if CORS should be provided, by default this uses `isJsonRequest`
// 	 *
// 	 * @return True / False if CORS should be enabled
// 	 */
// 	public boolean isCorsRequest() {
// 		return isJsonRequest();
// 	}
	
// 	/**
// 	 * Does the CORS validation headers.
// 	 * This is automatically done when `isCorsRequest()` is true
// 	 */
// 	public void processCors() {
// 		// If httpResponse isnt set, there is nothing to CORS
// 		if (httpResponse == null) {
// 			return;
// 		}
		
// 		// Get origin server
// 		String originServer = httpRequest.getHeader("Referer");
// 		if (originServer == null || originServer.isEmpty()) {
// 			// Unable to process CORS as no referer was sent
// 			httpResponse.addHeader("Access-Control-Warning",
// 				"Missing Referer header, Unable to process CORS");
// 			return;
// 		}
// 		// @TODO : Validate originServer against accepted list?
		
// 		// Sanatize origin server to be strictly
// 		// http(s)://originServer.com, without additional "/" nor URI path
// 		boolean refererHttps = false;
// 		if (originServer.startsWith("https://")) {
// 			refererHttps = true;
// 			originServer = "https://" + originServer.substring("https://".length()).split("/")[0];
// 		} else {
// 			originServer = "http://" + originServer.substring("http://".length()).split("/")[0];
// 		}
		
// 		// @TODO : Validate originServer against accepted list?
		
// 		// By default CORS is enabled for all API requests
// 		httpResponse.addHeader("Access-Control-Allow-Origin", originServer);
// 		httpResponse.addHeader("Access-Control-Allow-Credentials", "true");
// 		httpResponse.addHeader("Access-Control-Allow-Methods",
// 			"POST, GET, OPTIONS, PUT, DELETE, HEAD");
// 	}
	
// 	///////////////////////////////////////////////////////
// 	//
// 	// Process Chain execution
// 	//
// 	///////////////////////////////////////////////////////
	
// 	/**
// 	 * Triggers the process chain with the current setup, and indicates failure / success
// 	 **/
// 	public boolean processChain() throws ServletException {
// 		try {
// 			try {
// 				boolean ret = true;
				
// 				// Does setup
// 				doSharedSetup();
// 				doRequestSetup();
				
// 				// Does CORS processing
// 				if (isCorsRequest()) {
// 					processCors();
// 				}
				
// 				// is JSON request?
// 				if (isJsonRequest()) {
// 					ret = processChainJSON();
// 				} else { // or as per normal
// 					ret = processChainRequest();
// 				}
				
// 				// Flush any data if exists
// 				getWriter().flush();
				
// 				// Does teardwon
// 				doSharedTeardown();
// 				doRequestTearDown();
				
// 				// Returns success or failure
// 				return ret;
// 			} catch (Exception e) {
// 				doException(e);
// 				return false;
// 			}
// 		} catch (Exception e) {
// 			throw new ServletException(e);
// 		}
// 	}
	
// 	/**
// 	 * The process chain part specific to a normal request
// 	 **/
// 	@SuppressWarnings("incomplete-switch")
// 	private boolean processChainRequest() throws Exception {
// 		try {
// 			// PathEnding enforcement
// 			// https://stackoverflow.com/questions/4836858/is-response-redirect-always-an-http-get-response
// 			// To explain why its only used for GET requests
// 			if (requestType == HttpRequestType.GET && !enforceProperRequestPathEnding()) {
// 				return false;
// 			}
			
// 			// Does authentication check
// 			if (!doAuth(templateDataObj)) {
// 				return false;
// 			}
			
// 			// Does for all requests
// 			if (!doRequest(templateDataObj)) {
// 				return false;
// 			}
// 			boolean ret = true;
			
// 			// Switch is used over if,else for slight compiler optimization
// 			// http://stackoverflow.com/questions/6705955/why-switch-is-faster-than-if
// 			//
// 			// HttpRequestType reqTypeAsEnum = HttpRequestType(requestType);
// 			switch (requestType) {
// 			case GET:
// 				ret = doGetRequest(templateDataObj);
// 				break;
// 			case POST:
// 				ret = doPostRequest(templateDataObj);
// 				break;
// 			case PUT:
// 				ret = doPutRequest(templateDataObj);
// 				break;
// 			case DELETE:
// 				ret = doDeleteRequest(templateDataObj);
// 				break;
// 			}
			
// 			if (ret) {
// 				outputRequest(templateDataObj, getWriter());
// 			}
			
// 			// // Flush the output stream
// 			// getWriter().flush();
// 			// getOutputStream().flush();
			
// 			return ret;
// 		} catch (Exception e) {
// 			return outputRequestException(templateDataObj, getWriter(), e);
// 		}
// 	}
	
// 	/**
// 	 * The process chain part specific to JSON request
// 	 **/
// 	@SuppressWarnings("incomplete-switch")
// 	private boolean processChainJSON() throws Exception {
// 		try {
// 			// Does authentication check
// 			if (!doAuth(templateDataObj)) {
// 				return false;
// 			}
			
// 			// Does for all JSON
// 			if (!doJSON(jsonDataObj, templateDataObj)) {
// 				return false;
// 			}
			
// 			boolean ret = true;
			
// 			// Switch is used over if,else for slight compiler optimization
// 			// http://stackoverflow.com/questions/6705955/why-switch-is-faster-than-if
// 			//
// 			switch (requestType) {
// 			case GET:
// 				ret = doGetJSON(jsonDataObj, templateDataObj);
// 				break;
// 			case POST:
// 				ret = doPostJSON(jsonDataObj, templateDataObj);
// 				break;
// 			case PUT:
// 				ret = doPutJSON(jsonDataObj, templateDataObj);
// 				break;
// 			case DELETE:
// 				ret = doDeleteJSON(jsonDataObj, templateDataObj);
// 				break;
// 			}
			
// 			if (ret) {
// 				outputJSON(jsonDataObj, templateDataObj, getWriter());
// 			}
			
// 			return ret;
// 		} catch (Exception e) {
// 			return outputJSONException(jsonDataObj, templateDataObj, getWriter(), e);
// 		}
// 	}
	
// 	///////////////////////////////////////////////////////
// 	//
// 	// Process chains overwrites
// 	//
// 	///////////////////////////////////////////////////////
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Called once when initialized per request, and by the initializeContext thread.
// 	 *
// 	 * The distinction is important, as certain parameters (such as requesrt details),
// 	 * cannot be assumed to be avaliable in initializeContext, but is present for most requests
// 	 **/
// 	public void doSharedSetup() throws Exception {
// 		// Does nothing (to override)
// 	}
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Called once when completed per request, regardless of request status, and by the destroyContext thread
// 	 *
// 	 * PS: This is rarely needed, just rely on java GC =)
// 	 *
// 	 * The distinction is important, as certain parameters (such as requesrt details),
// 	 * cannot be assumed to be avaliable in initializeContext, but is present for most requests
// 	 **/
// 	public void doSharedTeardown() throws Exception {
// 		// Does nothing (to override)
// 	}
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Called once when initialized per request
// 	 **/
// 	public void doRequestSetup() throws Exception {
// 		// Does nothing (to override)
// 	}
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Called once when completed per request, regardless of request status
// 	 * PS: This is rarely needed, just rely on java GC =)
// 	 **/
// 	public void doRequestTearDown() throws Exception {
// 		// Does nothing (to override)
// 	}
	
// 	/**
// 	 * Handles setup and teardown exception
// 	 **/
// 	public void doException(Exception e) throws Exception {
// 		throw e;
// 	}
	
// 	//-------------------------------------------
// 	// HTTP request handling
// 	//-------------------------------------------
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Does the needed page request authentication, page redirects (if needed), and so forth. Should not do any actual,
// 	 * output processing. Returns true to continue process chian (default) or false to terminate the process chain.
// 	 **/
// 	public boolean doAuth(Map<String, Object> templateData) throws Exception {
// 		return true;
// 	}
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Does the required page request processing, this is used if both post / get behaviour is consistent
// 	 **/
// 	public boolean doRequest(Map<String, Object> templateData) throws Exception {
// 		return true;
// 	}
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Does the required page GET processing, AFTER doRequest
// 	 **/
// 	public boolean doGetRequest(Map<String, Object> templateData) throws Exception {
// 		return true;
// 	}
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Does the required page POST processing, AFTER doRequest
// 	 **/
// 	public boolean doPostRequest(Map<String, Object> templateData) throws Exception {
// 		return true;
// 	}
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Does the required page PUT processing, AFTER doRequest
// 	 **/
// 	public boolean doPutRequest(Map<String, Object> templateData) throws Exception {
// 		return true;
// 	}
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Does the required page DELETE processing, AFTER doRequest
// 	 **/
// 	public boolean doDeleteRequest(Map<String, Object> templateData) throws Exception {
// 		return true;
// 	}
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Does the output processing, this is after do(Post/Get/Put/Delete)Request
// 	 *
// 	 * Important note: when output testual data like HTML/JS/etc. and not raw data,
// 	 * somehow the protocol requires an ending new line for the output to work.
// 	 * If you are using print() extensively, you may simply do a final println()
// 	 * at the end to terminate the output correctly.
// 	 **/
// 	public boolean outputRequest(Map<String, Object> templateData, PrintWriter output)
// 		throws Exception {
		
// 		/**
// 		 * Does string output if parameter is set
// 		 **/
// 		Object outputString = templateData.get("OutputString");
// 		if (outputString != null) {
// 			output.println(outputString.toString());
// 			return true;
// 		}
		
// 		/**
// 		 * Does standard file output - if file exists
// 		 **/
// 		outputFileServlet().processRequest( //
// 			getHttpServletRequest(), //
// 			getHttpServletResponse(), //
// 			requestType() == HttpRequestType.HEAD, //
// 			requestWildcardUri());
		
// 		/**
// 		 * Completes and return
// 		 **/
// 		return true;
// 	}
	
// 	/**
// 	 * Exception handler for the request stack
// 	 *
// 	 * note that this should return false, or throw a ServletException, UNLESS the exception was gracefully handled.
// 	 * which in most cases SHOULD NOT be handled here.
// 	 **/
// 	public boolean outputRequestException(Map<String, Object> templateData, PrintWriter output,
// 		Exception e) throws Exception {
// 		// Throws a runtime Exception, let the servlet manager handle the rest
// 		throw e;
// 		//return false;
// 	}
	
// 	///////////////////////////////////////////////////////
// 	//
// 	// Servlet Context handling
// 	//
// 	///////////////////////////////////////////////////////
	
// 	/**
// 	 * Cached servlet context event
// 	 **/
// 	protected ServletContextEvent _servletContextEvent = null;
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Initialize context setup process
// 	 **/
// 	public void initializeContext() throws Exception {
// 		// does nothing
// 	}
	
// 	/**
// 	 * [To be extended by sub class, if needed]
// 	 * Initialize context destroy process
// 	 **/
// 	public void destroyContext() throws Exception {
// 		// does nothing
// 	}
	
// 	///////////////////////////////////////////////////////
// 	//
// 	// Native Servlet do overwrites [Avoid overwriting]
// 	//
// 	///////////////////////////////////////////////////////
	
// 	/**
// 	 * [Do not extend] Diverts the native doX to spawnInstance().setupInstance(TYPE,Req,Res).processChain()
// 	 **/
// 	@Override
// 	public final void doGet(HttpServletRequest request, HttpServletResponse response)
// 		throws ServletException {
// 		spawnInstance().setupInstance(HttpRequestType.GET, request, response).processChain();
// 	}
	
// 	/**
// 	 * [Do not extend] Diverts the native doX to spawnInstance().setupInstance(TYPE,Req,Res).processChain()
// 	 **/
// 	@Override
// 	public final void doPost(HttpServletRequest request, HttpServletResponse response)
// 		throws ServletException {
// 		spawnInstance().setupInstance(HttpRequestType.POST, request, response).processChain();
// 	}
	
// 	/**
// 	 * [Do not extend] Diverts the native doX to spawnInstance().setupInstance(TYPE,Req,Res).processChain()
// 	 **/
// 	@Override
// 	public final void doPut(HttpServletRequest request, HttpServletResponse response)
// 		throws ServletException {
// 		spawnInstance().setupInstance(HttpRequestType.PUT, request, response).processChain();
// 	}
	
// 	/**
// 	 * [Do not extend] Diverts the native doX to spawnInstance().setupInstance(TYPE,Req,Res).processChain()
// 	 **/
// 	@Override
// 	public final void doDelete(HttpServletRequest request, HttpServletResponse response)
// 		throws ServletException {
// 		spawnInstance().setupInstance(HttpRequestType.DELETE, request, response).processChain();
// 	}
	
// 	/**
// 	 * [Do not extend] Diverts the native doX to spawnInstance().setupInstance(TYPE,Req,Res).processChain()
// 	 **/
// 	@Override
// 	public final void doOptions(HttpServletRequest request, HttpServletResponse response)
// 		throws ServletException {
// 		spawnInstance().setupInstance(HttpRequestType.OPTION, request, response).processChain();
// 		try {
// 			super.doOptions(request, response);
// 		} catch (Exception e) {
// 			throw new ServletException(e);
// 		}
// 	}
	
// 	/**
// 	 * [Do not extend] Servlet context initializer handling.
// 	 **/
// 	public void contextInitialized(ServletContextEvent sce) {
// 		_servletContextEvent = sce;
// 		try {
// 			doSharedSetup();
// 			initializeContext();
// 		} catch (Exception e) {
// 			throw new RuntimeException(e);
// 		}
// 	}
	
// 	/**
// 	 * [Do not extend] Servlet context destroyed handling
// 	 **/
// 	public void contextDestroyed(ServletContextEvent sce) {
// 		_servletContextEvent = sce;
// 		try {
// 			doSharedTeardown();
// 			destroyContext();
// 		} catch (Exception e) {
// 			throw new RuntimeException(e);
// 		}
// 	}
	
// 	/**
// 	 * @TODO : HEAD SUPPORT, for integration with FileServlet
// 	 **/
	
}
