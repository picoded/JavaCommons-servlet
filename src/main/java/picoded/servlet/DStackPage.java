package picoded.servlet;

import picoded.core.struct.GenericConvertMap;
import picoded.dstack.DStack;
import picoded.servlet.internal.*;

/**
 * Extends of BaseUtilPage, with preconfigured Dstack module
 */
public class DStackPage extends BaseUtilPage {
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Parameters transfer handling
	//
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Import CorePage/BasePage instance parameters over to another instance
	 * 
	 * @param  ori original CorePage to copy from
	 */
	public void transferParamsProcess(CorePage ori) {
		// Does original transfer
		super.transferParamsProcess(ori);
		
		// Abort if instance is not extended from BasePage
		if (!(ori instanceof DStackPage)) {
			return;
		}
		
		// Get the BasePage instance
		DStackPage oriPage = (DStackPage) ori;
		
		// Does additional transfer for BaseUtilPage
		this._dstackObj = oriPage._dstackObj;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	//
	// DStack handling
	//
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	/// The internal DStack object, that was initialized
	protected DStack _dstackObj = null;
	
	/**
	 * Get and return the DStack config from the configuration file / folder
	 * if applicable, returns null if no valid config found.
	 * 
	 * @return  the dstack config map, to initialize the stack object
	 */
	public GenericConvertMap<String, Object> dstackConfig() {
		return configFileSet().getGenericConvertStringMap("sys.dstack", null);
	}
	
}