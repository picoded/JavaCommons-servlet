package picoded.servlet;


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
	}

	// /**
	//  * Clone constructor, this is used to copy over all values from original instance
	//  */
	// public BasePage(BasePage ori) {
	// 	super(ori);
	// }

	///////////////////////////////////////////////////////
	//
	// Constructor extending
	//
	///////////////////////////////////////////////////////
	

}