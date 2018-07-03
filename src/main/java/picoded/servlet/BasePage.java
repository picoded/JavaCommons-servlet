package picoded.servlet;

import java.io.PrintWriter;

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
		transferParams(CorePage.getCorePage());
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
		if(ori == null) {
			return;
		}

		// Does original transfer (if applicable)
		super.transferParams(ori);
		// Abort if instance is not extended from BasePage
		if( !(ori instanceof BasePage) ) {
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
		route();
	}

	///////////////////////////////////////////////////////
	//
	// route handling
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Takes the existing request, and perform routing logic on it respectively.
	 * 
	 * This is automatically called on any request, or alternatively when forwarding 
	 * to another instnce request.
	 */
	public void route() {

	}


}