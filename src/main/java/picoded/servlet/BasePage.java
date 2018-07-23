package picoded.servlet;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletResponse;

import picoded.core.conv.ConvertJSON;
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
	public void transferParams(CorePage ori) {
		// Skip transfer step, if null is passed
		if (ori == null) {
			return;
		}
		
		// Does original transfer (if applicable)
		super.transferParams(ori);
		// Abort if instance is not extended from BasePage
		if (!(ori instanceof BasePage)) {
			return;
		}
		
		// Does additional transfer for base page
		
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
		print.println("Request URI : "+requestURI());
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

		// Get the current class map
		BasePageClassMap classMap = BasePageClassMap.setupAndCache(this);
		classMap.handleRequest(this, requestWildcardUriArray());

		// Process the response objects, and output them
		doRequestOutput(writer);
	}

	protected void doRequestOutput(PrintWriter writer) throws Exception {
		// Assert that either response API map or stringbuilder can be safely used (not both)
		if(responseStringBuilder.length() > 0 && responseApiMap.size() > 0) {
			throw new RuntimeException("ResponseApiMap and ResponseStringBuilder have content in them!");
		}

		if(responseStringBuilder.length() > 0) {
			// Does the string based response accordingly
			writer.println(responseStringBuilder.toString());
		} else if(responseApiMap.size() > 0) {
			// Setting the response to be JSON output 
			if( getHttpServletResponse().getContentType() == null) {
				getHttpServletResponse().setContentType("application/json");
			}
			writer.println(ConvertJSON.fromObject(responseApiMap, true));
		}
	}

	/**
	 * Response map builder for api
	 * NOTE: Do not use this in conjuction with PrintWriter / responseStringBuilder
	 */
	public GenericConvertMap<String,Object> responseApiMap = null;

	/**
	 * Response string builder, to use within requests (if applicable)
	 * NOTE: Do not use this in conjuction with PrintWriter / responseApiMap
	 */
	public StringBuilder responseStringBuilder = null;



}