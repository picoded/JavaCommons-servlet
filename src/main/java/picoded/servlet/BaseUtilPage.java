package picoded.servlet;

import java.util.*;
import java.util.logging.Logger;
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
		BaseUtilPage oriPage = (BaseUtilPage) ori;
		
		// Does additional transfer for BaseUtilPage
		this._webInfPath = oriPage._webInfPath;
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
	private static GenericConvertMap<String, Object> _unmodifiableConfigFileSet = null;
	
	/**
	 * Get the configuration file set from the WEB-INF/config/ folder
	 * And returns its unmodifiableMap object.
	 * 
	 * PS : You do not need to optimize or cache this object, its already done.
	 * 
	 * @return  config file set map
	 */
	public GenericConvertMap<String, Object> configFileSet() {
		// If static variable is initialized : use it
		if (_unmodifiableConfigFileSet != null) {
			return _unmodifiableConfigFileSet;
		}
		
		// Performing a syncronized lock on the static class
		// before initializing the BaseUtilPage
		synchronized (BaseUtilPage.class) {
			
			// Return the config file set if it was
			// intitialized in a race condition
			if (_unmodifiableConfigFileSet != null) {
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
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Reusable output logger
	//
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Servlet logging interface
	 *
	 * This is not a static class, so that the this object inherits
	 * any extensions if needed
	 **/
	public Logger log() {
		if (logObj != null) {
			return logObj;
		}
		logObj = Logger.getLogger(this.getClass().getName());
		return logObj;
	}
	
	// Memoizer for log() function
	protected Logger logObj = null;
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	//
	// background threading : public
	//
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	// The running background thread
	private Thread backgroundThread = null;
	
	/**
	 * This should only be called within "backgroundProcess"
	 * @return true, if the process is a background thread
	 */
	public boolean isBackgroundThread() {
		return backgroundThread != null && backgroundThread.getId() == Thread.currentThread().getId();
	}
	
	/**
	 * This is to be called only within "backgroundProcess"
	 * @return true, if the process is a background thread, and not interrupted
	 */
	public boolean isBackgroundThreadAlive() {
		return (backgroundThread != null && !Thread.interrupted());
	}
	
	/**
	 * [To be extended by sub class, if needed]
	 * The background process to execute per tick.
	 */
	public void backgroundProcess() {
		// Does nothing, for now
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	//
	// background threading : internal
	// [ NOT OFFICIALLY SUPPORTED FOR EXTENSION ]
	//
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * The background thread handler, isolated as a runnable.
	 *
	 * This allows a clean isolation of the background thread,
	 * From the initializeContext thread. Especially for 'sleep' calls
	 */
	private Runnable backgroundThreadHandler = () -> {
		// The config to use : or default value
		GenericConvertMap<String, Object> bgConfig = configFileSet().getGenericConvertStringMap(
			"sys.background", "{}");
		
		// Get the current background thread mode
		// Expect "between" (default) or "start" mode 
		String threadmode = bgConfig.getString("mode", "between");
		long configInterval = bgConfig.getLong("interval", 10000);
		
		// The invcoation timestamp in previous call
		long previousStartTimestamp = 0;
		
		// Start of background thread loop
		while (isBackgroundThreadAlive()) {
			// Get the new start timestamp
			long startTimestamp = System.currentTimeMillis();
			
			// Does the background process
			try {
				backgroundProcess();
			} catch (Exception e) {
				log()
					.warning(
						"WARNING - Uncaught 'backgroundProcess' exception : "
							+ e.getMessage()
							+ "\n          Note that the 'backgroundProcess' should be designed to never throw an exception,"
							+ "\n          As it will simply be ignored and diverted into the logs (with this message)"
							+ "\n" + picoded.core.exception.ExceptionUtils.getStackTrace(e) //
					);
			}
			
			// Does the appropriate interval delay, takes interruptException as termination
			try {
				if (!isBackgroundThreadAlive()) {
					// Background thread was interrupted, time to break the loop
					break;
				} else if (threadmode.equalsIgnoreCase("start")) {
					// Does the calculations between the timestamp now, the previous start run
					long runtimeLength = System.currentTimeMillis() - startTimestamp;
					// Get the time needed to "wait"
					long sleepRequired = configInterval - runtimeLength;
					// Induce the sleep ONLY if its required
					if (sleepRequired > 0) {
						Thread.sleep(sleepRequired);
					}
				} else if (threadmode.equalsIgnoreCase("between")) {
					// Default mode is "between"
					// Note if an interrupt is called here, it is 'skipped'
					Thread.sleep(configInterval);
				} else {
					throw new RuntimeException("Invalid 'sys.background.mode' thread mode ("
						+ threadmode + ") - use either 'between' or 'start'");
				}
			} catch (InterruptedException e) {
				// Log the InterruptedException
				if (isBackgroundThreadAlive()) {
					log()
						.info(
							"backgroundThreadHandler - caught InterruptedException (possible termination event)");
				} else {
					log()
						.warning(
							"backgroundThreadHandler - caught Unexpected InterruptedException (outside termination event)");
				}
			}
			
			// Update the previous start timestamp
			previousStartTimestamp = startTimestamp;
		}
	};
	
	/**
	 * Loads the configuration and start the background thread
	 */
	private void backgroundThreadHandler_start() {
		if (configFileSet().getBoolean("sys.background.enable", true)) {
			// Start up the background thread start process, only if its enabled
			backgroundThread = new Thread(backgroundThreadHandler);
			// And start it up
			backgroundThread.start();
		}
	}
	
	/**
	 * Loads the configuration and stop the background thread
	 * Either gracefully, or forcefully.
	 */
	@SuppressWarnings("deprecation")
	private void backgroundThreadHandler_stop() {
		// Checks if there is relevent background thread first
		if (backgroundThread != null) {
			// Set the interupption flag
			backgroundThread.interrupt();
			// Attempts to perform a join first
			try {
				backgroundThread.join(configFileSet().getLong(
					"sys.background.contextDestroyJoinTimeout", 10000));
			} catch (InterruptedException e) {
				log().warning(
					"backgroundThreadHandler - Unexpected InterruptedException on Thread.join : "
						+ e.getMessage());
				log().warning(picoded.core.exception.ExceptionUtils.getStackTrace(e));
			}
			
			// Does the actual termination if needed
			if (backgroundThread.isAlive()) {
				backgroundThread.stop();
			}
		}
	}
	
	/**
	 * [To be extended by sub class, if needed]
	 * Initialize context setup process, with background thread
	 **/
	@Override
	protected void initializeContext() throws Exception {
		super.initializeContext();
		backgroundThreadHandler_start();
	}
	
	/**
	 * [To be extended by sub class, if needed]
	 * Initialize context destroy process, with background thread
	 **/
	@Override
	protected void destroyContext() throws Exception {
		backgroundThreadHandler_stop();
		super.destroyContext();
	}
	
}