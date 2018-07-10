package picoded.servlet;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
	// Overwriting doRequest pipeline
	//
	///////////////////////////////////////////////////////
	
	@Override
	protected void doRequest(PrintWriter writer) throws Exception {
		route(requestWildcardUriArray());
	}
	
	///////////////////////////////////////////////////////
	//
	// route handling
	//
	///////////////////////////////////////////////////////
	
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
	 * @param  toExecute - method to execute
	 */
	protected void executeMethod(Method toExecute) {
		// @TODO : Does method detection and parameter / output logic respectively

		// Invoke the method
		try {
			toExecute.invoke(this);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Takes the existing request, and perform routing logic on it respectively.
	 * 
	 * This is automatically called on any request, or alternatively when  
	 * forwarding to another instnce request.
	 */
	protected void route(String[] routePath) {

		// Get the current class path tree
		AnnotationPathTree pathTree = AnnotationPathTree.setupAndCachePagePathTree(this);

		// @TODO : RequestPath / ApiPath method fetching (only 1 valid result)
		// @TODO : RequestPath class reroute fetching (only 1 valid result)

		// @TODO : RequestBefore handling + execution
		
		// @TODO : method execution

		// @TODO : RequestAfter handling + execution

		AnnotationPathTree endpoint = pathTree.getAnnotationPath(routePath);
		if( endpoint.methodList.size() == 0 ) {
			throw new RuntimeException( "404 error should occur here" );
		}

		executeMethod(endpoint.methodList.get(0));
	}


}