package picoded.servlet;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletResponse;

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
	protected void transferParams(CorePage ori) {
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
		// Get the current class map
		BasePageClassMap classMap = BasePageClassMap.setupAndCache(this);
		classMap.handleRequest(this, requestWildcardUriArray());


		// route(requestWildcardUriArray());
	}

}