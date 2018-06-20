package picoded.servlet.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Closeable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

import picoded.core.conv.GUID;
import picoded.core.conv.GenericConvert;
import picoded.core.file.FileUtil;

/**
 * EmbeddedServlet class, provides a means of self executing any of the JavaCommons packages
 * Without having the need of a parent tomcat : YAYS!
 *
 * On the flip side, this assumes only 1 context per deployment.
 * For more complex deployments, seriously use tomcat (or its many siblings)
 **/
public class EmbeddedServlet implements Closeable {
	
	///////////////////////////////////////////////////////
	//
	// Core instance variables
	//
	///////////////////////////////////////////////////////
	
	/**
	 * The tomcat instance, this does the actual implmentation
	 **/
	Tomcat tomcat = null;
	
	/**
	 * Context for the server, as mentioned above, EmbeddedServlet only support 1 context
	 **/
	Context context = null;
	
	/**
	 * Context name to be used, if not provided it defaults to "ROOT"
	 **/
	String contextName = "ROOT";
	
	/**
	 * The context path used, derived from context name
	 **/
	String contextPath = "";
	
	/**
	 * Temp path used, delete on close, if exists
	 **/
	Path tempBaseDir = null;
	Path tempContextDir = null;
	
	///////////////////////////////////////////////////////
	//
	// Constructor, and basic server setup+start
	//
	///////////////////////////////////////////////////////
	
	/**
	 * The simplest consturctor, just point to the web application folder
	 * and run it as "ROOT" on a specified port
	 *
	 * @param   Port to run the embedded servlet on, -1 defaults to 8080
	 * @param   File representing either the folder, or the war file to deploy
	 **/
	public EmbeddedServlet(int port, File webappPath) {
		this(port, "ROOT", webappPath);
	}
	
	/**
	 * Sometimes the app just will not work for "ROOT", so you may want to define
	 * a fixed contextName path instead.
	 *
	 * @param   Port to run the embedded servlet on, -1 defaults to 8080
	 * @param   String representing the context name and path, without "/", like "ROOT"
	 * @param   File representing either the folder, or the war file to deploy
	 **/
	public EmbeddedServlet(int port, String contextName, File webappPath) {
		initTomcatInstance("", port);
		addWebapp(contextName, webappPath);
		startup();
	}
	
	/**
	 * Implement just one class as a servlet for the whole context,
	 * mainly for a single role port services, or for unit testing.
	 *
	 * This defaults to a servlet path of "/*"
	 *
	 * @param   Port to run the embedded servlet on, -1 defaults to 8080
	 * @param   Servlet class to use
	 **/
	public EmbeddedServlet(int port, Servlet servletClass) {
		this(port, "ROOT", servletClass, null);
	}
	
	/**
	 * Implement just one class as a servlet for the whole context,
	 * mainly for a single role port services, or for unit testing.
	 *
	 * @param   Port to run the embedded servlet on, -1 defaults to 8080
	 * @param   Servlet class to use
	 * @param   Servlet path to assign class to, null defaults to "/*"
	 **/
	public EmbeddedServlet(int port, Servlet servletClass, String servletPath) {
		this(port, "ROOT", servletClass, servletPath);
	}
	
	/**
	 * Implement just one class as a servlet for the whole context,
	 * mainly for a single role port services, or for unit testing.
	 *
	 * @param   Port to run the embedded servlet on, -1 defaults to 8080
	 * @param   String representing the context name and path, without "/", like "ROOT"
	 * @param   Servlet class to use
	 * @param   Servlet path to assign class to, null defaults to "/*"
	 **/
	public EmbeddedServlet(int port, String contextName, Servlet servletClass, String servletPath) {
		initTomcatInstance("", port);
		addServlet(contextName, servletClass, servletPath);
		startup();
	}
	
	///////////////////////////////////////////////////////
	//
	// Server closure and destruction
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Destroy the tomcat instance and removes it, if it exists
	 **/
	public synchronized void close() {
		try {
			// Tomcat teardown / destruction
			if (tomcat != null) {
				tomcat.stop();
				tomcat.destroy();
				tomcat = null;
			}
			
			// Temp file clenup
			if (tempBaseDir != null) {
				FileUtil.deleteDirectory(tempBaseDir.toFile());
				tempBaseDir = null;
			}
			if (tempContextDir != null) {
				FileUtil.deleteDirectory(tempContextDir.toFile());
				tempContextDir = null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Finalize cleanup, does the tomcat destroy call if needed
	 *
	 * This is to ensure proper closure on "Garbage Collection"
	 **/
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
	
	///////////////////////////////////////////////////////
	//
	// Await thread handling
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Thread await for the server to keep running
	 **/
	public void await() {
		tomcat.getServer().await();
	}
	
	///////////////////////////////////////////////////////
	//
	// Initializing and starting, internal functions
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Does the basic default tomcat instance setup
	 *
	 * @param  Temp base directory to use
	 * @param  Port number to use
	 **/
	protected void initTomcatInstance(File tempPath, int port) {
		initTomcatInstance(tempPath.getAbsolutePath(), port);
	}
	
	/**
	 * Does the basic default tomcat instance setup
	 *
	 * @param  Temp base directory to use
	 * @param  Port number to use
	 **/
	protected void initTomcatInstance(String tempPath, int port) {
		try {
			// Setup tomcat instance
			tomcat = new Tomcat();
			
			// Port setup
			tomcat.setPort(port > 0 ? port : 8080);
			
			// Get default base dir to current executing directory "java_tmp/random-guid"
			// If needed of course
			//
			// It is important to note that in the Tomcat API documentation this is considered
			// a security risk. Mainly due to cross application attacks
			//
			// Files.createTempDirectory - is used in place of java.io.tmpdir
			// String mWorkingDir = System.getProperty("java.io.tmpdir");
			if (tempPath == null || tempPath.isEmpty()) {
				// Temp directory for context
				Path tPath = Files.createTempDirectory(GUID.base58()).toAbsolutePath();
				
				// Minor error prevention
				Path webappPath = tPath.resolve("webapps");
				Files.createDirectories(webappPath);
				assert Files.isWritable(webappPath);
				
				// Temp path directory
				tempBaseDir = tPath;
				tempPath = tPath.toString();
			}
			tomcat.setBaseDir(tempPath);
			
			// Possible things that may change in the future
			//--------------------------------------------------------------------------
			//tomcat.getHost().setAppBase(mWorkingDir);
			//tomcat.getHost().setAutoDeploy(true);
			//tomcat.getHost().setDeployOnStartup(true);
			//tomcat.setHostname("localhost");
			//tomcat.enableNaming();
			
			// Do not add ContextConfig at the server level, but at the webapp level
			// This somehow fails despite it being in accordance to some documentation
			//--------------------------------------------------------------------------
			// ContextConfig contextConfig = new ContextConfig() {
			// 	private boolean invoked = false;
			// 	@Override
			// 	public void lifecycleEvent(LifecycleEvent event) {
			// 		if (!invoked) {
			// 			StandardJarScanner scanner = new StandardJarScanner();
			// 			scanner.setScanBootstrapClassPath(true);
			// 			scanner.setScanClassPath(false);
			// 			scanner.setScanManifest(true);
			// 			((Context) event.getLifecycle()).setJarScanner(scanner);
			// 			invoked = true;
			// 		}
			// 		super.lifecycleEvent(event);
			// 	}
			// };
			//
			// StandardServer server = (StandardServer)tomcat.getServer();
			// server.addLifecycleListener(contextConfig);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Normalize the context name, and provides its actual context path
	 *
	 * @param In context name to use (will be converted to path)
	 *
	 * @return  Context path to use
	 **/
	protected String normalizeContextNameToPath(String inContextName) {
		//
		// Load context name and path
		//
		contextName = inContextName;
		if (contextName == null || contextName.isEmpty() || contextName.equalsIgnoreCase("ROOT")) {
			contextName = "ROOT";
			contextPath = "";
		} else {
			contextPath = "/" + contextName;
		}
		return contextPath;
	}
	
	/**
	 * Add a webapplication
	 *
	 * @param  In context name to use (will be converted to path)
	 * @param  Webapplication File path to use
	 **/
	protected void addWebapp(String inContextName, File webappPath) {
		addWebapp(inContextName, webappPath.getAbsolutePath());
	}
	
	/**
	 * Add a webapplication
	 *
	 * @param  In context name to use (will be converted to path)
	 * @param  Webapplication File path to use
	 **/
	protected void addWebapp(String inContextName, String webappPath) {
		try {
			// Normalize and store the context path
			normalizeContextNameToPath(inContextName);
			
			//
			// This helps disable the default parent class path scanning
			// And help reduce the classpathing warning errors.
			//
			// In the context JavaCommons webappllication, each deployment
			// will have more then enough Jars to provide on their own
			//
			LifecycleListener contextConfig = new ContextConfig() {
				private boolean invoked = false;
				
				@Override
				public void lifecycleEvent(LifecycleEvent event) {
					if (!invoked) {
						StandardJarScanner scanner = new StandardJarScanner();
						scanner.setScanBootstrapClassPath(true);
						scanner.setScanClassPath(false);
						scanner.setScanManifest(true);
						((Context) event.getLifecycle()).setJarScanner(scanner);
						invoked = true;
					}
					super.lifecycleEvent(event);
				}
			};
			
			//
			// Loads the application with the custom contextConfig
			//
			context = tomcat.addWebapp(tomcat.getHost(), contextPath, webappPath, contextConfig);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Adds a single servlet, and load it with a context.
	 * This is mainly useful for single use ports, or unit testing.
	 *
	 * @param  In context name to use (will be converted to path)
	 * @param  Servlet class to use
	 * @param  Servlet path to use (defaults to "/*")
	 **/
	protected void addServlet(String inContextName, Servlet serverClass, String serverPath) {
		try {
			// Normalize and store the context path
			normalizeContextNameToPath(inContextName);
			
			// Temp directory for context
			tempContextDir = Files.createTempDirectory(GUID.base58()).toAbsolutePath();
			
			// Setup context
			context = tomcat.addContext(contextPath, tempContextDir.toString());
			
			// Setup servlet class
			Tomcat.addServlet(context, "ServletApp", serverClass);
			
			// And link the path
			if (serverPath == null) {
				// http://stackoverflow.com/questions/4140448/difference-between-and-in-servlet-mapping-url-pattern
				serverPath = "/*";
			}
			
			// Add the servlet
			context.addServletMappingDecoded(serverPath, "ServletApp");
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Startup the tomcat instance, does any final initialization prior to startup if needed
	 **/
	protected void startup() {
		try {
			//tomcat.init();
			tomcat.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	///////////////////////////////////////////////////////
	//
	// CLI Utility runner
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Takes in the following parameters
	 *
	 * args[0] - servlet static (parent of WEB-INF) folder. Default ".."
	 * args[1] - context name. Default "ROOT"
	 * args[2] - port number. Default "8080"
	 **/
	public static void main(String[] args) {
		
		// Config settings to pass
		String servletFolder = "..";
		String contextName = "ROOT";
		String portNumber = "8080";
		
		// Arguments passing
		if (args.length > 0) {
			servletFolder = args[0];
		}
		if (args.length > 1) {
			contextName = args[1];
		}
		if (args.length > 2) {
			portNumber = args[2];
		}
		
		// Run the servlet
		EmbeddedServlet servlet = new EmbeddedServlet(GenericConvert.toInt(portNumber), contextName,
			new File(servletFolder));
		
		// Wait and continue
		servlet.await();
	}
}
