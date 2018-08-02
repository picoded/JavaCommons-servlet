package picoded.servlet;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletResponse;

import picoded.core.conv.ConvertJSON;
import picoded.core.file.ConfigFileSet;
import picoded.core.struct.GenericConvertHashMap;
import picoded.core.struct.GenericConvertMap;
import picoded.servlet.internal.*;

/**
 * Extension of BasePage, with common preconfigured components
 * 
 * BaseUtilPage onwards assume a standard servlet deployment setup, with WEB-INF folder
 * for config files, and various other components.
 */
public class BaseUtilPage extends BasePage {

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
		if (!(ori instanceof BaseUtilPage)) {
			return;
		}
		
		// Get the BasePage instance
		BaseUtilPage oriPage = (BaseUtilPage)ori;

		// Does additional transfer for BaseUtilPage
		this._webInfPath  = oriPage._webInfPath;
		this._classesPath = oriPage._classesPath;
		this._libraryPath = oriPage._libraryPath;
		this._configsPath = oriPage._configsPath;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Path handling
	//
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	protected String _webInfPath = null;
	protected String _classesPath = null;
	protected String _libraryPath = null;
	protected String _configsPath = null;
	
	/**
	 * @return WEB-INF folder path
	 **/
	public String getWebInfPath() {
		return (_webInfPath != null) ? _webInfPath : (_webInfPath = getContextPath() + "WEB-INF/");
	}
	
	/**
	 * @return classes folder path
	 **/
	public String getClassesPath() {
		return (_classesPath != null) ? _classesPath : (_classesPath = getWebInfPath() + "classes/");
	}
	
	/**
	 * @return library folder path
	 **/
	public String getLibraryPath() {
		return (_libraryPath != null) ? _libraryPath : (_libraryPath = getWebInfPath() + "lib/");
	}
	
	/**
	 * @return config files path
	 **/
	public String getConfigPath() {
		return (_configsPath != null) ? _configsPath : (_configsPath = getWebInfPath() + "config/");
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Configuration handling
	//
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	// Internal static representation of the config file
	private static ConfigFileSet _configSet = null;
	private static GenericConvertMap<String,Object> _unmodifiableConfigFileSet = null;

	/**
	 * Get the configuration file set from the WEB-INF/config/ folder
	 * And returns its unmodifiableMap object.
	 * 
	 * PS : You do not need to optimize or cache this object, its already done.
	 * 
	 * @return  config file set map
	 */
	public GenericConvertMap<String,Object> configFileSet() {
		// If static variable is initialized : use it
		if( _unmodifiableConfigFileSet != null ) {
			return _unmodifiableConfigFileSet;
		}

		// Performing a syncronized lock on the static class
		// before initializing the BaseUtilPage
		synchronized( BaseUtilPage.class ) {

			// Return the config file set if it was
			// intitialized in a race condition
			if( _unmodifiableConfigFileSet != null ) {
				return _unmodifiableConfigFileSet;
			}

			// Load the config file set
			_configSet = new ConfigFileSet();
			_configSet.addConfigSet(getConfigPath());

			// Get and return the unmodifiable copy
			// and return it
			_unmodifiableConfigFileSet = _configSet.unmodifiableMap();
			return _unmodifiableConfigFileSet;
		}
	}
	
}