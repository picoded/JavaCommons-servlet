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
	protected List<MethodWrapper> methodList = new ArrayList<>();

	///////////////////////////////////////////////////////
	//
	// Annotation path mapping
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Get annotation endpoint path
	 * 
	 * @param   path  to fetch annotation
	 * 
	 * @return  AnootationPathMap at the respective path
	 */
	public AnnotationPathMap fetchAnnotationPath(String path) {
		return fetchAnnotationPath( ServletStringUtil.splitUriString(path) );
	}

	/**
	 * Get annotation endpoint path
	 * 
	 * @param   splitPath  to fetch annotation
	 * 
	 * @return  AnootationPathMap at the respective path
	 */
	public AnnotationPathMap fetchAnnotationPath(String[] splitPath) {
		// Annotation path map to recusively fetch from
		AnnotationPathMap step = this;

		// For each annotation path part, 
		// find the nested path map 
		for(String part : splitPath) {
			// Skips processing if part is blank
			if(part.length() == 0) {
				continue;
			}

			// Get the nested part map
			// or initialize it
			if(step.get(part) == null) {
				step.put(part, new AnnotationPathMap());
			}
			step = step.get(part);
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
			for(RequestPath pathObj : methodObj.getAnnotationsByType(RequestPath.class)) {
				mapMethod(pathObj.value(), methodObj);
			}
			for(RequestBefore pathObj : methodObj.getAnnotationsByType(RequestBefore.class)) {
				mapMethod(pathObj.value(), methodObj);
			}
			for(RequestAfter pathObj : methodObj.getAnnotationsByType(RequestAfter.class)) {
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
		AnnotationPathMap pathObj = fetchAnnotationPath(path);
		pathObj.methodList.add( new MethodWrapper(methodObj) );
	}

}