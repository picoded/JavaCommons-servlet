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
	
	/**
	 * DStack instance object, used to represent the backend data structure
	 * 
	 * @return  dstack object, initialized using the dstackConfig if valid
	 */
	public DStack dstack() {
		if (_dstackObj != null) {
			return _dstackObj;
		}
		return dstack_forceInitialize();
	}
	
	/**
	 * DStack instance object, used to represent the backend data structure
	 * 
	 * This forcefully reinitialize the DStack object, so that L0-1 cache
	 * (eg. stackSimple) could be resetted between background background cycles.
	 * 
	 * @return  dstack object, initialized using the dstackConfig if valid
	 */
	protected DStack dstack_forceInitialize() {
		GenericConvertMap<String, Object> configObj = dstackConfig();
		if (configObj != null) {
			_dstackObj = new DStack(configObj);
		} else {
			throw new IllegalArgumentException("Missing valid configuration : sys.dstack = null");
		}
		return _dstackObj;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	//
	// DStack shared setup
	//
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * [To be extended by sub class, if needed]
	 * Called once when initialized per request, and by the initializeContext thread.
	 *
	 * The distinction is important, as certain parameters (such as requesrt details),
	 * cannot be assumed to be avaliable in initializeContext, but is present for most requests
	 **/
	@Override
	protected void doSharedSetup() throws Exception {
		// Super call
		super.doSharedSetup();
		
		// DStack initialization
		dstack();
	}
	
	/**
	 * [To be extended by sub class, if needed]
	 * Initialize context setup process
	 **/
	@Override
	protected void initializeContext() throws Exception {
		// Super call
		super.initializeContext();
		
		// DStack systemSetup
		//
		// as this is done AFTER doSharedSetup,
		// any objects initialized in there will 
		// have its own systemSetup() call performed.
		dstack().systemSetup();
	}
	
}