package picoded.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.*;

// Exceptions used
import java.io.IOException;

// Objects used
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import java.io.File;
import java.io.PrintWriter;
import java.io.OutputStream;
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
import picoded.core.common.HttpRequestType;
import picoded.core.struct.ArrayListMap;
import picoded.servlet.util.FileServlet;

import picoded.core.common.HttpRequestType;

/**
 * Extension of CorePage with various utility functionalities
 */
public class CoreUtilPage extends CorePage {

	///////////////////////////////////////////////////////
	//
	// Constructor extending
	//
	///////////////////////////////////////////////////////

	/**
	 * Blank constructor, used for template building, unit testing, etc
	 **/
	public CoreUtilPage() {
		super();
	}

	/**
	 * Clone constructor, this is used to copy over all values from original instance
	 */
	public CoreUtilPage(CorePage ori) {
		super(ori);
	}

	// /**
	//  * Gets and return the thread local CorePage used in current servlet request
	//  */
	// public static CoreUtilPage getCoreUtilPage() {
	// 	CorePage ret = localCopy.get();
	// 	if(ret instanceof CoreUtilPage) {
	// 		return ret;
	// 	} else if(ret != null) {
	// 		return new CoreUtilPage(ret);
	// 	}
	// 	return null;
	// }

	///////////////////////////////////////////////////////
	//
	// [Utility] Native FileServlet and path handling
	//
	///////////////////////////////////////////////////////

	/**
	 * Cached FileServlet (for reuse)
	 **/
	protected FileServlet _outputFileServlet = null;

	/**
	 * Returns the File servlet
	 **/
	protected FileServlet outputFileServlet() {
		if (_outputFileServlet != null) {
			return _outputFileServlet;
		}
		return (_outputFileServlet = new FileServlet(getContextPath()));
	}

	/**
	 * Send file as an output - use this to automatically provide optimized file transfers.
	 * Note that for this to work, no additional output should be done after this command (or before) it
	 *
	 * @param file data to send
	 */
	public void sendFile(File data) {
		try {
			outputFileServlet().processRequest(getHttpServletRequest(), getHttpServletResponse(),
					false, data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	///////////////////////////////////////////////////////
	//
	// [Utility] Webpath rerouting
	//
	///////////////////////////////////////////////////////

	/**
	 * Checks and forces a redirection with closing slash on index page requests.
	 * If needed (returns false, on validation failure)
	 * <p>
	 * For example : https://picoded.com/JavaCommons , will redirect to https://picoded.com/JavaCommons/
	 * <p>
	 * This is a rather complicated topic. Regarding the ambiguity of the HTML
	 * redirection handling (T605 on phabricator)
	 * <p>
	 * But basically take the following as example. On how a redirect is handled
	 * for a relative "./index.html" within a webpage.
	 * <p>
	 * | Current URL              | Redirects to             |
	 * |--------------------------|--------------------------|
	 * | host/subpath             | host/index.html          |
	 * | host/subpath/            | host/subpath/index.html  |
	 * | host/subpath/index.html  | host/subpath/index.html  |
	 * <p>
	 * As a result of the ambiguity in redirect for html index pages loaded
	 * in "host/subpath". This function was created, so that the index page is
	 * enforced to be loaded with proper file or "/" index page loading
	 * <p>
	 * The reason for standardising to "host/subpath/" is that this will be consistent with
	 * offline page loads (such as through cordova). Where the index.html will be loaded
	 * in full file path instead.
	 * <p>
	 * The redirection only triggers with the following conditions
	 * <p>
	 * 1) Request is a GET request
	 * <p>
	 * 2) A request path without the "/" ending
	 * <p>
	 * 3) Is not a file request, a file request is assumed if there was a "." in the last name
	 * Example: host/subpath/file.js
	 * <p>
	 * This will also safely handle the forwarding of all GET request parameters.
	 * For example: "host/subpath?abc=xyz" will be redirected to "host/subpath/?abc=xyz"
	 * <p>
	 * Note: THIS will silently pass as true, if a _httpRequest is not found. This is to facilitate
	 * possible function calls done on servlet setup. Without breaking them
	 * <p>
	 * Now that was ALOT of explaination for one simple function wasnt it >_>
	 * Well its one of the lesser understood "gotchas" in the HTTP specifications.
	 * Made more unknown by the JavaCommons user due to the common usage of ${PageRootURI}
	 * which basically resolves this issue. Unless its in relative path mode. Required for app exports.
	 **/
	protected boolean enforceProperRequestPathEnding() throws IOException {
		if (_httpRequest != null) {
			String fullURI = _httpRequest.getRequestURI();

			// Check request type, ignore if not a get request
			if (!isGET()) {
				return true;
			}

			// This does not validate blank / root requests
			//
			// Should we? : To fix if this is required (as of now no)
			if (fullURI == null || fullURI.equalsIgnoreCase("/")) {
				return true;
			}

			//
			// Already ends with a "/" ? : If so its considered valid
			//
			if (fullURI.endsWith("/")) {
				return true;
			}

			//
			// Checks if its a file request. Ends check if it is
			//
			String name = FilenameUtils.getName(fullURI);
			if (FilenameUtils.getExtension(name).length() > 0) {
				// There is a file extension. so we shall assume it is a file
				return true; // And end it
			}

			//
			// Get the query string to append (if needed)
			//
			String queryString = _httpRequest.getQueryString();
			if (queryString == null) {
				queryString = "";
			} else if (!queryString.startsWith("?")) {
				queryString = "?" + queryString;
			}

			//
			// Enforce proper URL handling
			//
			_httpResponse.sendRedirect(fullURI + "/" + queryString);
			return false;
		}

		// Validation is valid.
		return true;
	}

	///////////////////////////////////////////////////////
	//
	// [Utility] CORS Handling
	//
	///////////////////////////////////////////////////////

	/**
	 * Add the necessery headers to allow the current request to be processed with CORS
	 */
	protected void enableCORS() {
		CoreUtilPage.enableCORS(this._httpRequest, this._httpResponse);
	}

	/**
	 * Add the necessery headers to allow the current request to be processed with CORS
	 */
	static public void enableCORS(HttpServletRequest req, HttpServletResponse res) {
		// If _httpResponse isnt set, there is nothing to CORS
		if (res == null) {
			return;
		}

		// Check if CORS is already configured, if so skip
		String existingCors = res.getHeader("Access-Control-Allow-Origin");
		if (existingCors != null && existingCors.length() > 0) {
			return;
		}

		// Get origin server, from either the referer, or origin itself
		String originServer = req.getHeader("Referer");
		if (originServer == null || originServer.isEmpty()) {
			originServer = req.getHeader("Origin");
		}
		if (originServer == null || originServer.isEmpty()) {
			String protocall = req.getHeader("x-forwarded-proto");
			if (protocall == null || protocall.isEmpty()) {
				protocall = req.getScheme();
			}
			originServer = protocall + "://" + req.getServerName();
		}

		// Perform a warning if cors origin cannot be detirmined
		if (originServer == null || originServer.isEmpty()) {
			// Unable to process CORS as no referer was sent
			res.setHeader("Access-Control-Warning",
					"Missing Referer/Origin header, Unable to process CORS accurately");
		}

		// Normalize the origin server
		if (originServer == null || originServer.isEmpty()) {
			// Handle requests, which lacked the origin server source, and respond with "*"
			originServer = "*";
		} else {
			// Sanatize origin server to be strictly
			// http(s)://originServer.com, without additional "/" nor URI path
			URI uri = null;
			try {
				uri = new URI(originServer);
				originServer = uri.getScheme() + "://" + uri.getAuthority();
			} catch (URISyntaxException e) {
				res.setHeader("Access-Control-Warning", "Unable to process CORS accurately because origin header is invalid: " + e);
			}
		}

		// @TODO : Validate originServer against accepted list?

		// By default CORS is enabled for all API requests
		res.setHeader("Access-Control-Allow-Origin", originServer);

		res.setHeader("Access-Control-Allow-Headers", "Accept, Accept-Language, Content-Language, Content-Type, Content-Encoding, Range");
		res.setHeader("Access-Control-Allow-Credentials", "true");
		res.setHeader("Access-Control-Allow-Methods",
				"POST, GET, OPTIONS, PUT, DELETE, HEAD");
	}

	///////////////////////////////////////////////////////
	//
	// doRequest extension
	//
	///////////////////////////////////////////////////////

	/**
	 * Extends `doRequest` to perform `enforceProperRequestPathEnding` and `outputFileServlet`
	 * only when processing GET requests
	 */
	protected void doRequest(PrintWriter writer) throws Exception {
		// Extends original behaviour (if any)
		super.doRequest(writer);

		// Only does file serving with GET request
		// and valid file path handling
		if (isGET() && enforceProperRequestPathEnding()) {
			/**
			 * Does standard file output - if file exists
			 **/
			outputFileServlet().processRequest( //
					getHttpServletRequest(), //
					getHttpServletResponse(), //
					requestType() == HttpRequestType.HEAD, //
					requestWildcardUri());
		}
	}

	///////////////////////////////////////////////////////
	//
	// Some legacy stuff
	//
	///////////////////////////////////////////////////////

	// /**
	//  * The process chain part specific to a normal request
	//  **/
	// @SuppressWarnings("incomplete-switch")
	// private boolean processRequest() throws Exception {
	// 	try {
	// 		// PathEnding enforcement
	// 		// https://stackoverflow.com/questions/4836858/is-response-redirect-always-an-http-get-response
	// 		// To explain why its only used for GET requests
	// 		if (requestType == _httpRequestType.GET && !enforceProperRequestPathEnding()) {
	// 			return false;
	// 		}

	// 		// Does authentication check
	// 		if (!doAuth(templateDataObj)) {
	// 			return false;
	// 		}

	// 		// Does for all requests
	// 		if (!doRequest(templateDataObj)) {
	// 			return false;
	// 		}
	// 		boolean ret = true;

	// 		// Switch is used over if,else for slight compiler optimization
	// 		// http://stackoverflow.com/questions/6705955/why-switch-is-faster-than-if
	// 		//
	// 		// _httpRequestType reqTypeAsEnum = _httpRequestType(requestType);
	// 		switch (requestType) {
	// 		case GET:
	// 			ret = doGetRequest(templateDataObj);
	// 			break;
	// 		case POST:
	// 			ret = doPostRequest(templateDataObj);
	// 			break;
	// 		case PUT:
	// 			ret = doPutRequest(templateDataObj);
	// 			break;
	// 		case DELETE:
	// 			ret = doDeleteRequest(templateDataObj);
	// 			break;
	// 		}

	// 		if (ret) {
	// 			outputRequest(templateDataObj, getWriter());
	// 		}

	// 		// // Flush the output stream
	// 		// getWriter().flush();
	// 		// getOutputStream().flush();

	// 		return ret;
	// 	} catch (Exception e) {
	// 		return outputRequestException(templateDataObj, getWriter(), e);
	// 	}
	// }

	// /**
	//  * The process chain part specific to JSON request
	//  **/
	// @SuppressWarnings("incomplete-switch")
	// private boolean processChainJSON() throws Exception {
	// 	try {
	// 		// Does authentication check
	// 		if (!doAuth(templateDataObj)) {
	// 			return false;
	// 		}

	// 		// Does for all JSON
	// 		if (!doJSON(jsonDataObj, templateDataObj)) {
	// 			return false;
	// 		}

	// 		boolean ret = true;

	// 		// Switch is used over if,else for slight compiler optimization
	// 		// http://stackoverflow.com/questions/6705955/why-switch-is-faster-than-if
	// 		//
	// 		switch (requestType) {
	// 		case GET:
	// 			ret = doGetJSON(jsonDataObj, templateDataObj);
	// 			break;
	// 		case POST:
	// 			ret = doPostJSON(jsonDataObj, templateDataObj);
	// 			break;
	// 		case PUT:
	// 			ret = doPutJSON(jsonDataObj, templateDataObj);
	// 			break;
	// 		case DELETE:
	// 			ret = doDeleteJSON(jsonDataObj, templateDataObj);
	// 			break;
	// 		}

	// 		if (ret) {
	// 			outputJSON(jsonDataObj, templateDataObj, getWriter());
	// 		}

	// 		return ret;
	// 	} catch (Exception e) {
	// 		return outputJSONException(jsonDataObj, templateDataObj, getWriter(), e);
	// 	}
	// }

}
