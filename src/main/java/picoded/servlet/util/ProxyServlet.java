package picoded.servlet.util;

import picoded.servlet.*;
import picoded.servlet.annotation.*;
import picoded.core.web.*;
import picoded.core.conv.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;

/**
 * Utility class which proxies a request 
 * it recieves onto a target server.
 * 
 * This class should either be initialied and use by another BasePage
 * Or be extended and modified, with the `protected String _serverTarget` overwritten
 * 
 * Example usage
 * 
 * ```
 * public customServlet extends BasePage {
 * 	// Request path to redirect
 * 	@RequestPath("*")
 * 	@RequestType({"GET", "POST", "DELETE", "PUT"})
 * 	public void relay_endpoint() {
 * 		(new ProxyServlet(this,"http://targetServer:port")).relayRequest();
 * 	}
 * }
 * ```
 */
public class ProxyServlet extends BasePage {
	
	/**
	 *  Target server to relay request onto, that is configured by the constructor
	 */
	protected String _serverTarget = null;
	
	/**
	 * Setup the class with the server target, and BasePage (to initialize from)
	 * 
	 * @param page    - original BasePage which recieved the request
	 * @param target  - target server endpoint, example - http://localhost:5555
	 */
	public ProxyServlet(BasePage page, String target) {
		transferParamsProcess(page);
		_serverTarget = target;
	}
	
	/**
	 * Setup the class with the server target
	 * 
	 * @param target  - target server endpoint, example - http://localhost:5555
	 */
	public ProxyServlet(String target) {
		_serverTarget = target;
	}
	
	/**
	 * Setup the class with default server target (http://localhost:5555)
	 */
	public ProxyServlet() {
		_serverTarget = "http://localhost:5555";
	}
	
	/**
	 * Server target to relay request onto
	 * This function can be overwritten to change the target behaviour
	 */
	public String serverTarget() {
		return _serverTarget;
	}
	
	/**
	 * Given the current request parameters and serverTarget, 
	 * computes the target endpoint to make the request by using the servlet requestURI
	 * 
	 * @return target endpoint to make the request against
	 */
	public String targetEndpoint() {
		// Get the server target
		String server = serverTarget();
		
		// Trims off ending "/" from server URL 
		while (server.endsWith("/")) {
			server = server.substring(0, server.length() - 1);
		}
		
		// Return the target server, with request URI
		return server + requestURI();
	}
	
	//--------------------------------------------------------
	// Static client library support
	//--------------------------------------------------------
	
	// Local static client variable
	private static volatile RequestHttpClient httpClientObj = null;
	
	/**
	 * RequestHttpClient, which by defaults uses the cached RequestHttp
	 * 
	 * @return  RequestHttpClient used in relay
	 */
	public static RequestHttpClient httpClient() {
		// Thread safe get
		if (httpClientObj != null) {
			return httpClientObj;
		}
		
		// Client object not yet initialized, try to get one
		// with a syncronized lock, initializing if needed
		synchronized (RequestHttp.class) {
			// This is to resolve race conditions,
			// where a seperate thread setup the client obj
			if (httpClientObj != null) {
				return httpClientObj;
			}
			
			// Initialize config map
			Map<String, Object> httpClientConfig = new HashMap<>();
			httpClientConfig.put("followRedirects", false);
			httpClientConfig.put("followSslRedirects", false);
			
			// Initialize the client object without redirect
			httpClientObj = new RequestHttpClient(httpClientConfig);
			return httpClientObj;
		}
	}
	
	/**
	 * [Utility function] Convert Map<String, Object> into Map<String, String[]>
	 *
	 * @param mapToConvert of type Map<String, Object>
	 * @return Map<String, String[]>
	 */
	protected Map<String, String[]> convertMapObjectToStringArray(Map<String, Object> mapToConvert) {
		// Null safety check
		if (mapToConvert == null) {
			return null;
		}
		Map<String, String[]> reformedParamMap = new HashMap<String, String[]>();
		for (String key : mapToConvert.keySet()) {
			Object value = mapToConvert.get(key);
			if (value instanceof String) { // Convert to array of size 1
				reformedParamMap.put(key, new String[] { value.toString() });
			} else if (value instanceof String[]) { // Put the array back as it is
				reformedParamMap.put(key, (String[]) value);
			} else { // Convert using ConvertJSON as a string and put to array of size 1
				String convertedString = ConvertJSON.fromObject(value);
				reformedParamMap.put(key, new String[] { convertedString });
			}
		}
		
		// Return the reformed map
		return reformedParamMap;
	}
	
	/**
	 * Calls the targetEndpoint, and relay the request
	 */
	@SuppressWarnings("unchecked")
	public void relayRequest() {
		// Compute the target endpoint
		String target = targetEndpoint();
		RequestHttpClient client = httpClient();
		
		// The header and cookie map
		Map<String, String[]> headerMap = requestHeaderMap();
		Map<String, String[]> cookieMap = requestCookieMap();
		
		// The servlet request parameters
		String contentType = getHttpServletRequest().getContentType();
		String requestType = requestTypeString().toUpperCase();
		
		// Servlet request parameters
		ServletRequestMap req = requestParameterMap();
		
		// The exepcted response
		//
		// For clarification, for the rest of this function,
		// response here refer to the result returned by the target server
		ResponseHttp response;
		
		// protocall selection
		if (isGET()) {
			// Standard get request
			response = client.get(target, req, (Map<String, Object>) (Object) headerMap,
				(Map<String, Object>) (Object) cookieMap);
		} else if (isDELETE()) {
			// Standard delete request
			response = client.delete(target, req, (Map<String, Object>) (Object) headerMap,
				(Map<String, Object>) (Object) cookieMap);
		} else if (contentType.contains("application/json") || contentType.contains("text/plain")) {
			// Does specific processing for application/json
			response = client.executeJsonRequest( //
				requestType, //
				target, //
				req, //
				cookieMap, //
				headerMap //
				);
		} else if (contentType.contains("application/x-www-form-urlencoded")) {
			// Does specific processing for application/json
			response = client.executeFormRequest( //
				requestType, //
				target, //
				convertMapObjectToStringArray(req), //
				cookieMap, //
				headerMap //
				);
		} else {
			throw new RuntimeException(requestType + " - " + contentType
				+ " request type not supported");
		}
		
		// get the servlet object (to return result with)
		HttpServletResponse servletResponse = getHttpServletResponse();
		
		// Set the status code
		servletResponse.setStatus(response.statusCode());
		
		// Pass over headers
		Map<String, String[]> responseHeaderMap = response.headersMap();
		for (String headerKey : responseHeaderMap.keySet()) {
			String[] multipleValues = responseHeaderMap.get(headerKey);
			for (String singleValue : multipleValues) {
				servletResponse.addHeader(headerKey, singleValue);
			}
		}
		
		// Pass over proxy target header
		// servletResponse.addHeader("__target_endpoint", target);
		
		// Pass over byte stream
		try {
			InputStream responseInputStream = response.inputStream();
			OutputStream servletOutputStream = getOutputStream();
			IOUtils.copy(responseInputStream, servletOutputStream);
			
			// Just to be safe, close response (not really needed)
			responseInputStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Wildcard request path
	 * to relay all request 
	 */
	@RequestPath("*")
	@RequestType({ "GET", "POST", "DELETE", "PUT" })
	public void relay_endpoint() {
		relayRequest();
	}
	
}