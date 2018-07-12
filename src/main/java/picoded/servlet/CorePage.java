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
import picoded.core.file.FileUtil;
import picoded.core.common.EmptyArray;
import picoded.core.struct.ArrayListMap;
import picoded.servlet.util.FileServlet;
import picoded.servlet.internal.ServletStringUtil;

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
 * doGet ------------+--> spawnInstance().setupInstance(...).processChain(...)
 *                   |            |
 * doPost -----------+       doSharedSetup
 *                   |     && doRequestSetup
 * doPut ------------+            |
 *                   |        doRequest
 * doDelete ---------+            |
 *                   |     doRequestTearDown
 * #doOption --------+    && doSharedTeardown
 *                   |
 * #doHead ----------/
 *
 * #doOption and #doHead is not yet supported
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
	 * Clone constructor, this is used to copy over all values from original instance
	 * 
	 * @param  ori original CorePage to copy from
	 */
	public CorePage(CorePage ori) {
		super();
		this.transferParams(ori);
	}
	
	/**
	 * Import CorePage instance parameters over to another instance
	 * 
	 * @param  ori original CorePage to copy from
	 */
	protected void transferParams(CorePage ori) {
		// Skip transfer step, if null is passed
		if (ori == null) {
			return;
		}
		// Import the values respectively
		this._contextPath = ori._contextPath;
		this._contextURI = ori._contextURI;
		this._requestCookieMap = ori._requestCookieMap;
		this._requestHeaderMap = ori._requestHeaderMap;
		this._servletContextEvent = ori._servletContextEvent;
		this._httpRequest = ori._httpRequest;
		this._httpResponse = ori._httpResponse;
		this._requestMap = ori._requestMap;
		this._requestType = ori._requestType;
		this._responseOutputStream = ori._responseOutputStream;
		this._printWriter = ori._printWriter;
	}
	
	/**
	 * Spawn an instance of the current class
	 **/
	protected CorePage spawnInstance() throws ServletException { //, OutputStream outStream
		try {
			// Get new instance of page (via its extended class)
			Class<? extends CorePage> pageClass = this.getClass();
			CorePage ret = pageClass.newInstance();
			
			// Cast and apply servlet config
			ret = pageClass.cast(ret);
			ret.applyServletConfig(this.getServletConfig());
			
			// Return result
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
	// TheadLocal convinence copy
	//
	///////////////////////////////////////////////////////
	
	// Actual threadLocal storage
	private static ThreadLocal<CorePage> localCopy = new ThreadLocal<>();
	
	/**
	 * Setup the ThreadLocal storage internally, throws an error if existing value is found
	 */
	private void setupThreadLocal() {
		if (localCopy.get() != null) {
			throw new RuntimeException(
				"Existing CorePage instance found in current thread - multiple CorePage instances per thread is not supported");
		}
		localCopy.set(this);
	}
	
	/**
	 * Gets and return the thread local CorePage used in current servlet request
	 */
	public static CorePage getCorePage() {
		return localCopy.get();
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
	protected HttpRequestType _requestType = null;
	
	/**
	 * The actual output stream used
	 **/
	protected OutputStream _responseOutputStream = null;
	
	/**
	 * httpRequest used [modification of this value, is highly discouraged]
	 **/
	protected HttpServletRequest _httpRequest = null;
	
	/**
	 * httpResponse used [modification of this value, is highly discouraged]
	 **/
	protected HttpServletResponse _httpResponse = null;
	
	/**
	 * ServletRequestMap used for the current request
	 */
	protected ServletRequestMap _requestMap = null;
	
	/**
	 * Setup the instance, with http request and response
	 **/
	protected CorePage setupInstance( //
		HttpRequestType inRequestType, HttpServletRequest req, //
		HttpServletResponse res //
	) throws ServletException {
		
		// Setup the local instance properties
		_requestType = inRequestType;
		_httpRequest = req;
		_httpResponse = res;
		
		try {
			// UTF-8 enforcement
			_httpRequest.setCharacterEncoding("UTF-8");
			
			// @TODO: To use IOUtils.buffer for inputstream of httpRequest / parameterMap
			// THIS IS CRITICAL, for the POST request in proxyServlet to work
			_requestMap = new ServletRequestMap(_httpRequest);
			
			// Response output stream 
			_responseOutputStream = _httpResponse.getOutputStream();
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		// Return instance 
		return this;
	}
	
	///////////////////////////////////////////////////////
	//
	// setupInstance direct variables access
	//
	///////////////////////////////////////////////////////
	
	/**
	 * @return the native http servlet request
	 **/
	public HttpServletRequest getHttpServletRequest() {
		return _httpRequest;
	}
	
	/**
	 * @return the native http servlet response
	 **/
	public HttpServletResponse getHttpServletResponse() {
		return _httpResponse;
	}
	
	/**
	 * @return the request parameters as a map
	 **/
	public ServletRequestMap requestParameterMap() {
		return _requestMap;
	}
	
	///////////////////////////////////////////////////////
	//
	// Header and cookie map handling
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
		if (_httpRequest == null) {
			return null;
		}
		
		// Creates the _requestHeaderMap from httpRequest
		ArrayListMap<String, String> mapList = new ArrayListMap<String, String>();
		
		// Get an Enumeration of all of the header names sent by the client
		Enumeration<String> headerNames = _httpRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			
			// As per the Java Servlet API 2.5 documentation:
			//        Some headers, such as Accept-Language can be sent by clients
			//        as several headers each with a different value rather than
			//        sending the header as a comma separated list.
			// Thus, we get an Enumeration of the header values sent by the client
			mapList.append(name, _httpRequest.getHeaders(name));
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
		if (_httpRequest == null || _httpRequest.getCookies() == null) {
			return null;
		}
		
		// Creates the _requestCookieMap from httpRequest
		ArrayListMap<String, String> mapList = new ArrayListMap<String, String>();
		for (Cookie oneCookie : _httpRequest.getCookies()) {
			mapList.append(oneCookie.getName(), oneCookie.getValue());
		}
		
		// Cache and return
		return _requestCookieMap = mapList.toMapArray(new String[0]);
	}
	
	///////////////////////////////////////////////////////
	//
	// Server / request information (convinence function)
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Gets the server name
	 */
	public String getServerName() {
		return _httpRequest.getServerName();
	}
	
	public int getServerPort() {
		return _httpRequest.getServerPort();
	}
	
	/**
	 * Gets the server requestURI
	 **/
	public String requestURI() {
		return _httpRequest.getRequestURI();
	}
	
	/**
	 * Gets the request servlet path
	 **/
	public String requestServletPath() {
		return _httpRequest.getServletPath();
	}
	
	/**
	 * Gets the server wildcard segment of the URI
	 * Note this does any URL decoding if needed, use _httpRequest.getPathInfo() for the raw wild card path
	 **/
	public String requestWildcardUri() {
		try {
			String path = _httpRequest.getPathInfo(); //no query values
			if (path == null || path.isEmpty()) {
				return null;
			}
			return FileUtil.normalize(URLDecoder.decode(path, "UTF-8").trim());
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Gets the server wildcard segment of the URI as a split array
	 */
	public String[] requestWildcardUriArray() {
		// Get the wildcardURI, return a quick empty array if applicable
		String raw = requestWildcardUri();
		if (raw == null || raw.isEmpty()) {
			return EmptyArray.STRING;
		}
		
		// Normalize and split the string representation
		return ServletStringUtil.splitUriString(raw);
	}
	
	/**
	 * Cached context path
	 **/
	protected String _contextPath = null;
	
	/**
	 * Gets and returns the context path / application folder path in absolute terms if possible
	 *
	 * This represents the FILE path in the native file system
	 **/
	public String getContextPath() {
		if (_contextPath != null) {
			return _contextPath;
		}
		
		if (_httpRequest != null && _httpRequest.getServletContext() != null) {
			return _contextPath = (_httpRequest.getServletContext()).getRealPath("/") + "/";
		}
		
		if (_servletContextEvent != null) {
			ServletContext sc = _servletContextEvent.getServletContext();
			return _contextPath = sc.getRealPath("/") + "/";
		}
		
		try {
			// Note this may fail for contextInitialized
			return _contextPath = getServletContext().getRealPath("/") + "/";
		} catch (Exception e) {
			return _contextPath = "./";
		}
	}
	
	/**
	 * Cached context path
	 **/
	protected String _contextURI = null;
	
	/**
	 * Returns the whole server application contextual path : needed for base URI for page redirects / etc
	 *
	 * For root this is "" blank, however for example
	 * if deployed under "edge", it would be "/edge"
	 *
	 * This represents part of the URL path used in HTTP
	 **/
	public String getContextURI() {
		if (_contextURI != null) {
			return _contextURI;
		}
		
		if (_httpRequest != null) {
			return _contextURI = _httpRequest.getContextPath();
		}
		
		if (_servletContextEvent != null) {
			ServletContext sc = _servletContextEvent.getServletContext();
			return _contextURI = sc.getContextPath() + "/";
		}
		
		try {
			return (URLDecoder.decode(this.getClass().getClassLoader().getResource("/").getPath(),
				"UTF-8")).split("/WEB-INF/classes/")[0];
		} catch (UnsupportedEncodingException | NullPointerException e) {
			return "../";
		}
	}
	
	/**
	 * Returns the servlet contextual path : needed for base URI for page redirects / etc
	 * Note that this refers specifically to the current servlet request
	 **/
	public String getServletContextURI() {
		if (_httpRequest != null) {
			return _httpRequest.getServletPath();
		}
		//return getServletPath();
		throw new RuntimeException(
			"Unable to process getServletContextURI, outside of servlet request");
	}
	
	/**
	 * gets a parameter value, from the _httpRequest.getParameter
	 **/
	public String getParameter(String paramName) {
		if (requestParameterMap() != null) {
			return requestParameterMap().getString(paramName);
		}
		return null;
	}
	
	///////////////////////////////////////////////////////
	//
	// Request type config getters (convinence function)
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Returns the request type
	 **/
	public HttpRequestType requestType() {
		return _requestType;
	}
	
	/**
	 * Returns the request type as a string
	 **/
	public String requestTypeString() {
		return _requestType.toString();
	}
	
	/**
	 * Returns if the request is GET
	 **/
	public boolean isGET() {
		return _requestType == HttpRequestType.GET;
	}
	
	/**
	 * Returns if the request is POST
	 **/
	public boolean isPOST() {
		return _requestType == HttpRequestType.POST;
	}
	
	/**
	 * Returns if the request is PUT
	 **/
	public boolean isPUT() {
		return _requestType == HttpRequestType.PUT;
	}
	
	/**
	 * Returns if the request is DELETE
	 **/
	public boolean isDELETE() {
		return _requestType == HttpRequestType.DELETE;
	}
	
	/**
	 * Returns if the request is OPTION
	 **/
	public boolean isOPTION() {
		return _requestType == HttpRequestType.OPTION;
	}
	
	///////////////////////////////////////////////////////
	//
	// Output stream / output writer / send redirect
	//
	///////////////////////////////////////////////////////
	
	/** Memoizer for printwriter */
	protected PrintWriter _printWriter = null;

	/**
	 * gets the PrintWriter, from the getOutputStream() object and returns it
	 **/
	public PrintWriter getPrintWriter() {
		if( _printWriter != null ) {
			return _printWriter;
		}
		try {
			// Important note: You will need to use "true" for auto flush.
			// "PrintWriter(Writer out, boolean autoFlush)", or it will NOT work.
			_printWriter =  new PrintWriter(new OutputStreamWriter(getOutputStream(), getHttpServletRequest()
				.getCharacterEncoding()), true);
			return _printWriter;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * gets the OutputStream, from the httpResponse.getOutputStream() object and returns it
	 * also surpresses IOException, as RuntimeException
	 **/
	public OutputStream getOutputStream() {
		return _responseOutputStream;
	}
	
	/**
	 * Proxies to httpResponse.sendRedirect,
	 **/
	public void sendRedirect(String uri) {
		if (_httpResponse != null) {
			try {
				_httpResponse.sendRedirect(uri);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return;
		}
		
		// Fallsback to responseHeaderMap.location, if httpResponse is null
		// if( responseHeaderMap == null ) {
		//	responseHeaderMap = new HashMap<String, String>();
		// }
		// responseHeaderMap.put("location", uri);
	}
	
	///////////////////////////////////////////////////////
	//
	// Process chains overwrites
	//
	///////////////////////////////////////////////////////
	
	/**
	 * [To be extended by sub class, if needed]
	 * Called once when initialized per request, and by the initializeContext thread.
	 *
	 * The distinction is important, as certain parameters (such as requesrt details),
	 * cannot be assumed to be avaliable in initializeContext, but is present for most requests
	 **/
	protected void doSharedSetup() throws Exception {
		// Does nothing (to override)
	}
	
	/**
	 * [To be extended by sub class, if needed]
	 * Called once when completed per request, regardless of request status, and by the destroyContext thread
	 *
	 * PS: This is rarely needed, just rely on java GC =)
	 *
	 * The distinction is important, as certain parameters (such as requesrt details),
	 * cannot be assumed to be avaliable in initializeContext, but is present for most requests
	 **/
	protected void doSharedTeardown() throws Exception {
		// Does nothing (to override)
	}
	
	/**
	 * [To be extended by sub class, if needed]
	 * Called once when initialized per request
	 **/
	protected void doRequestSetup() throws Exception {
		// Does nothing (to override)
	}
	
	/**
	 * [To be extended by sub class, if needed]
	 * Called once when completed per request, regardless of request status
	 * PS: This is rarely needed, just rely on java GC =)
	 **/
	protected void doRequestTearDown() throws Exception {
		// Does nothing (to override)
	}
	
	/**
	 * [To be extended by sub class, if needed]
	 * Handles setup and teardown exception
	 **/
	protected void handleRequestSetupTeardownException(Exception e) throws Exception {
		throw e;
	}
	
	///////////////////////////////////////////////////////
	//
	// Servlet Context handling
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Cached servlet context event
	 **/
	protected ServletContextEvent _servletContextEvent = null;
	
	/**
	 * [To be extended by sub class, if needed]
	 * Initialize context setup process
	 **/
	protected void initializeContext() throws Exception {
		// Does nothing (to override)
	}
	
	/**
	 * [To be extended by sub class, if needed]
	 * Initialize context destroy process
	 **/
	protected void destroyContext() throws Exception {
		// Does nothing (to override)
	}
	
	///////////////////////////////////////////////////////
	//
	// HTTP request handling
	//
	///////////////////////////////////////////////////////
	
	/**
	 * [To be extended by sub class, if needed]
	 * Does the output processing, this is after do(Post/Get/Put/Delete)Request
	 *
	 * Important note: when outputing textual data like HTML/JS/etc. and not raw data,
	 * somehow the protocol requires an ending new line for the output to work properly.
	 * If you are using print() extensively, you may simply do a final println()
	 * at the end to terminate the output correctly.
	 **/
	protected void doRequest(PrintWriter writer) throws Exception {
		// Does nothing (to override)
	}
	
	/**
	 * [To be extended by sub class, if needed]
	 * Handles all other exceptions that was not previously handled
	 **/
	protected void handleRequestException(Exception e) throws Exception {
		throw e;
	}
	
	//
	// CorePage, previously had request specific endpoints
	// this is deprecated in favour of annotation based end point declearation
	//
	
	// /**
	//  * [To be extended by sub class, if needed]
	//  * Does the required page GET processing, AFTER doRequest
	//  **/
	// public boolean doGetRequest() throws Exception {
	// 	return true;
	// }
	// /**
	//  * [To be extended by sub class, if needed]
	//  * Does the required page POST processing, AFTER doRequest
	//  **/
	// public boolean doPostRequest() throws Exception {
	// 	return true;
	// }
	// /**
	//  * [To be extended by sub class, if needed]
	//  * Does the required page PUT processing, AFTER doRequest
	//  **/
	// public boolean doPutRequest() throws Exception {
	// 	return true;
	// }
	// /**
	//  * [To be extended by sub class, if needed]
	//  * Does the required page DELETE processing, AFTER doRequest
	//  **/
	// public boolean doDeleteRequest() throws Exception {
	// 	return true;
	// }
	
	///////////////////////////////////////////////////////
	//
	// Final exception fallback
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Exception handler for the request stack
	 *
	 * note that this should return false, or throw a ServletException, UNLESS the exception was gracefully handled.
	 * which in most cases SHOULD NOT be handled here.
	 **/
	protected void handleException(Exception e) throws Exception {
		// Throws a runtime Exception, let the servlet manager handle the rest
		throw e;
	}
	
	///////////////////////////////////////////////////////
	//
	// Process Chain execution
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Triggers the process chain with the current setup
	 **/
	private void processChain() throws ServletException {
		try {
			try {
				// Store a ThreadLocal copy
				setupThreadLocal();
				
				// Does setup
				try {
					doSharedSetup();
					doRequestSetup();
				} catch (Exception e) {
					handleRequestSetupTeardownException(e);
				}
				
				// Process the request
				// Flush any data if exists
				try {
					doRequest(getPrintWriter());
					getPrintWriter().flush();
				} catch (Exception e) {
					handleRequestException(e);
				}
				
				// Does teardwon
				try {
					doSharedTeardown();
					doRequestTearDown();
				} catch (Exception e) {
					handleRequestSetupTeardownException(e);
				}
			} catch (Exception e) {
				// Final exception catcher
				handleException(e);
			}
		} catch (Exception e) {
			throw new ServletException(e);
		} finally {
			// Remove ThreadLocal copy
			localCopy.remove();
		}
	}
	
	///////////////////////////////////////////////////////
	//
	// Native Servlet do overwrites [Avoid overwriting]
	//
	///////////////////////////////////////////////////////
	
	/**
	 * [Do not extend] Diverts the native doX to spawnInstance().setupInstance(TYPE,Req,Res).processChain()
	 **/
	@Override
	public final void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException {
		spawnInstance().setupInstance(HttpRequestType.GET, request, response).processChain();
	}
	
	/**
	 * [Do not extend] Diverts the native doX to spawnInstance().setupInstance(TYPE,Req,Res).processChain()
	 **/
	@Override
	public final void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException {
		spawnInstance().setupInstance(HttpRequestType.POST, request, response).processChain();
	}
	
	/**
	 * [Do not extend] Diverts the native doX to spawnInstance().setupInstance(TYPE,Req,Res).processChain()
	 **/
	@Override
	public final void doPut(HttpServletRequest request, HttpServletResponse response)
		throws ServletException {
		spawnInstance().setupInstance(HttpRequestType.PUT, request, response).processChain();
	}
	
	/**
	 * [Do not extend] Diverts the native doX to spawnInstance().setupInstance(TYPE,Req,Res).processChain()
	 **/
	@Override
	public final void doDelete(HttpServletRequest request, HttpServletResponse response)
		throws ServletException {
		spawnInstance().setupInstance(HttpRequestType.DELETE, request, response).processChain();
	}
	
	// /**
	//  * [Do not extend] Diverts the native doX to spawnInstance().setupInstance(TYPE,Req,Res).processChain()
	//  **/
	// @Override
	// public final void doOptions(HttpServletRequest request, HttpServletResponse response)
	// 	throws ServletException {
	// 	spawnInstance().setupInstance(HttpRequestType.OPTION, request, response).processChain();
	// 	try {
	// 		super.doOptions(request, response);
	// 	} catch (Exception e) {
	// 		throw new ServletException(e);
	// 	}
	// }
	
	/**
	 * [Do not extend] Servlet context initializer handling.
	 **/
	public void contextInitialized(ServletContextEvent sce) {
		_servletContextEvent = sce;
		try {
			setupThreadLocal();
			doSharedSetup();
			initializeContext();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * [Do not extend] Servlet context destroyed handling
	 **/
	public void contextDestroyed(ServletContextEvent sce) {
		_servletContextEvent = sce;
		try {
			doSharedTeardown();
			destroyContext();
			localCopy.remove();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @TODO : HEAD SUPPORT, for integration with FileServlet
	 **/
	
}
