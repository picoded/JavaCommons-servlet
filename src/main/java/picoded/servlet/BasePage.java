package picoded.servlet;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import picoded.core.conv.ConvertJSON;
import picoded.core.exception.ExceptionUtils;
import picoded.core.struct.GenericConvertHashMap;
import picoded.core.struct.GenericConvertMap;
import picoded.servlet.internal.*;

/**
 * BasePage builds ontop of CorePage, and completely overwrites
 * doRequest behaviour to depend on annotations routing for paths
 */
public class BasePage extends CoreUtilPage {
	
	///////////////////////////////////////////////////////
	//
	// Constructor extending
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Blank constructor, used for template building, unit testing, etc
	 **/
	public BasePage() {
		super();
		// Automatically import any existing "basePage" where applicable
		// transferParams(CorePage.getCorePage());
	}
	
	/**
	 * Clone constructor, this is used to copy over
	 * all values from original instance
	 */
	public BasePage(CorePage ori) {
		super();
		transferParams(ori);
	}
	
	/**
	 * Import CorePage/BasePage instance parameters over to another instance
	 *
	 * @param  ori original CorePage to copy from
	 */
	public void transferParamsProcess(CorePage ori) {
		// Does original transfer
		super.transferParamsProcess(ori);
		
		// Abort if instance is not extended from BasePage
		if (!(ori instanceof BasePage)) {
			return;
		}
		
		// Get the BasePage instance
		BasePage oriPage = (BasePage) ori;
		
		// Does additional transfer for base page
		this.responseApiMap = oriPage.responseApiMap;
		this.responseStringBuilder = oriPage.responseStringBuilder;
	}
	
	///////////////////////////////////////////////////////
	//
	// Handle no route found exception
	//
	///////////////////////////////////////////////////////
	
	/**
	 * No route found failure condition.
	 * This typically happen no valid enpoints was found.
	 */
	public void handleMissingRouteFailure() {
		// Set 404 header
		getHttpServletResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
		
		// Print out the error
		PrintWriter print = getPrintWriter();
		print.println("<h1>404 Error</h1>");
		print.println("The requested resource is not avaliable Q.Q");
		print.println("");
		print.println("Request URI : " + requestURI());
	}
	
	///////////////////////////////////////////////////////
	//
	// Overwriting doRequest pipeline
	//
	///////////////////////////////////////////////////////
	
	@Override
	protected void doRequest(PrintWriter writer) throws Exception {
		// Response builder, to use within requests (if applicable)
		responseStringBuilder = new StringBuilder();
		responseApiMap = new GenericConvertHashMap<String, Object>();
		
		try {
			// Get the current class map
			BasePageClassMap classMap = BasePageClassMap.setupAndCache(this);
			classMap.handleRequest(this, requestWildcardUriArray());
			
			// Process the response objects, and output them
			doRequestOutput(writer);
		} catch (ApiException ae) {
			handleApiException(ae);
		} catch (HaltException he) {
			handleHaltException(he);
		}
	}
	
	protected void doRequestOutput(PrintWriter writer) throws Exception {
		// Assert that either response API map or stringbuilder can be safely used (not both)
		if (responseStringBuilder.length() > 0 && responseApiMap.size() > 0) {
			throw new RuntimeException(
				"ResponseApiMap and ResponseStringBuilder have content in them!");
		}
		
		if (responseStringBuilder.length() > 0) {
			// Does the string based response accordingly
			writer.println(responseStringBuilder.toString());
		} else if (responseApiMap.size() > 0) {
			// Setting the response to be JSON output
			if (getHttpServletResponse().getContentType() == null) {
				getHttpServletResponse().setContentType("application/javascript");
			}
			writer.println(ConvertJSON.fromObject(responseApiMap, true));
		}
	}
	
	/**
	 * Response map builder for api
	 * NOTE: Do not use this in conjuction with PrintWriter / responseStringBuilder
	 *
	 * @TODO : Refactor to protected _ equivalent with getter functions
	 */
	public GenericConvertMap<String, Object> responseApiMap = null;
	
	/**
	 * Response string builder, to use within requests (if applicable)
	 * NOTE: Do not use this in conjuction with PrintWriter / responseApiMap
	 *
	 * @TODO : Refactor to protected _ equivalent with getter functions
	 */
	public StringBuilder responseStringBuilder = null;
	
	///////////////////////////////////////////////////////
	//
	// Exception handling
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Throws a halt exception, to stop further processing of the request
	 */
	public void halt() {
		throw new HaltException();
	}
	
	/**
	 * Handles HALT exception
	 **/
	protected void handleHaltException(HaltException e) throws Exception {
		//intentionally does nothing
	}
	
	/**
	 * Handles API based exceptions
	 **/
	protected void handleApiException(ApiException e) throws Exception {
		
		responseApiMap.put("ERROR", e.getErrorMap());
		
		getHttpServletResponse().setContentType("application/javascript");
		getPrintWriter().println(ConvertJSON.fromObject(responseApiMap, true));
	}
}
