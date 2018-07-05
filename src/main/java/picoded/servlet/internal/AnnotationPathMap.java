package picoded.servlet.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import picoded.servlet.annotation.*;
import picoded.core.exception.ExceptionMessage;
import picoded.core.struct.GenericConvertConcurrentHashMap;
/**
 * Internal utility class, used to mapped the relvent 
 * annotation routes of a given class object.
 * 
 * And perform the subsquent request/api call for it.
 **/
public class AnnotationPathMap extends GenericConvertConcurrentHashMap<String,AnnotationPathMap> {

	///////////////////////////////////////////////////////
	//
	// Consturctor and method list init
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Blank constructor
	 */
	public AnnotationPathMap() {
		// blank constructor
	}

	/**
	 * Relevent method list for this current endpoint
	 */
	protected List<Method> methodList = new ArrayList<>();

	///////////////////////////////////////////////////////
	//
	// Annotation path mapping
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Get annotation endpoint path, or initialize it if it does not exist
	 * 
	 * @param   path  to fetch annotation
	 * 
	 * @return  AnootationPathMap at the respective path
	 */
	public AnnotationPathMap initAnnotationPath(String path) {
		return fetchAnnotationPath( ServletStringUtil.splitUriString(path), true );
	}

	/**
	 * Get annotation endpoint path, return null if not found
	 * 
	 * @param   path  to fetch annotation
	 * 
	 * @return  AnootationPathMap at the respective path
	 */
	public AnnotationPathMap getAnnotationPath(String path) {
		return fetchAnnotationPath( ServletStringUtil.splitUriString(path), false );
	}

	/**
	 * Get annotation endpoint path
	 * 
	 * @param   splitPath  to fetch annotation
	 * @param   init      boolean flag to initialize endpoint if it does not exist
	 * 
	 * @return  AnootationPathMap at the respective path
	 */
	protected AnnotationPathMap fetchAnnotationPath(String[] splitPath, boolean init) {
		// Annotation path map to recusively fetch from
		AnnotationPathMap step = this;

		// For each annotation path part, 
		// find the nested path map 
		for(String part : splitPath) {
			// Skips processing if part is blank
			if(part.length() == 0) {
				continue;
			}

			if(init) {
				// Get the nested path map
				// And initialize it if it does not exist
				AnnotationPathMap nextStep = step.get(part);
				if(nextStep == null) {
					nextStep = new AnnotationPathMap();
					step.put(part, nextStep);
				}
				step = nextStep;
			} else {
				// Get the nested path map
				// and return null if its not initialized
				step = step.get(part);
				if(step == null) {
					return null;
				}
			}
		}

		// Return the annotation path mapping
		return step;
	}

	///////////////////////////////////////////////////////
	//
	// Class mapping
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Import and scan the given class object for relevent 
	 * annotations and map it accordingly internally
	 **/
	public void mapClass(Class<?> classObj) {
		
		// Map the class methods
		mapClassMethods(classObj);
	}

	/**
	 * Import and scan the given class object for relevent 
	 * annotations and map its methods accordingly internally
	 */
	public void mapClassMethods(Class<?> classObj) {
		// Lets get the list of methods
		List<Method> methodList = AnnotationUtil.fetchMethodList(classObj);

		// Map the method list with annotation class
		for(Method methodObj : methodList) {

			// Get and process each type of annotation we currently support for methods
			//
			// Minor note : Because annotation is not extendable, we cant fully refactor 
			// the duplicative loop into a generic function, that is reusable.
			for(RequestPath pathObj   : methodObj.getAnnotationsByType(RequestPath.class)) {
				mapMethod(pathObj.value(), methodObj);
			}
			for(RequestBefore pathObj : methodObj.getAnnotationsByType(RequestBefore.class)) {
				mapMethod(pathObj.value(), methodObj);
			}
			for(RequestAfter pathObj  : methodObj.getAnnotationsByType(RequestAfter.class)) {
				mapMethod(pathObj.value(), methodObj);
			}
		}
	}

	/**
	 * Register a method endpoint
	 * 
	 * @param  path of the method endpoint
	 * @param  methodObj to register
	 */
	protected void mapMethod(String path, Method methodObj) {
		AnnotationPathMap pathObj = initAnnotationPath(path);
		if( pathObj.methodList.indexOf(methodObj) < 0 ) {
			pathObj.methodList.add( methodObj );
		}
	}

}