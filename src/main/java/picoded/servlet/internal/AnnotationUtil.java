package picoded.servlet.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import picoded.core.exception.ExceptionMessage;
import picoded.servlet.*;

/**
 * Static utility class used to faciliated annotation processing
 * This is structured and designed in a way for users not too familiar with 
 * Java "Class", "Annotation" types and java reflections
 */
public class AnnotationUtil {
	
	/**
	 * Static class annotation exception
	 */
	AnnotationUtil() {
		throw new RuntimeException(ExceptionMessage.staticClassConstructor);
	}
	
	//--------------------------------------------------------------------------------------
	//
	//  Generic annotation utils, for nearly any annotation types
	//
	//--------------------------------------------------------------------------------------
	
	/**
	 * Extract out the generic class object of the given object instance
	 * 
	 * @param  inObj to extract the class
	 * 
	 * @return Class object of the given inObj
	 */
	static Class<?> extractClass(Object inObj) {
		return inObj.getClass();
	}
	
	/**
	 * Extract out array list of methods in a class
	 * 
	 * @param  classObj to extract methods from
	 * 
	 * @return List of methods
	 */
	static List<Method> fetchMethodList(Class<?> classObj) {
		return new ArrayList<>(Arrays.asList(classObj.getMethods()));
	}
	
	/**
	 * Filter method list given the annotation class
	 * 
	 * @param  inList  to filter and return
	 * @param  annotationClass to filter the list from
	 * 
	 * @return  List of methods filtered
	 */
	static <A extends Annotation> List<Method> filterMethodListWithAnnotationClass(
		List<Method> inList, Class<A> annotationClass) {
		// Result to return
		List<Method> result = new ArrayList<>();
		
		// Iterate the class methods, and append to result those with valid annotation
		for (Method methodObj : inList) {
			A annotation = methodObj.getAnnotation(annotationClass);
			if (annotation != null) {
				result.add(methodObj);
			}
		}
		
		// Return result
		return result;
	}
	
	/**
	 * Extract out array list of methods in a class
	 * 
	 * @param  classObj to extract methods from
	 * 
	 * @return List of methods
	 */
	static List<Field> fetchFieldList(Class<?> classObj) {
		return new ArrayList<>(Arrays.asList(classObj.getFields()));
	}
	
	/**
	 * Filter field list given the annotation class
	 * 
	 * @param  inList  to filter and return
	 * @param  annotationClass to filter the list from
	 * 
	 * @return  List of methods filtered
	 */
	static <A extends Annotation> List<Field> filterFieldListWithAnnotationClass(
		List<Field> inList, Class<A> annotationClass) {
		// Result to return
		List<Field> result = new ArrayList<>();
		
		// Iterate the class methods, and append to result those with valid annotation
		for (Field fieldObj : inList) {
			A annotation = fieldObj.getAnnotation(annotationClass);
			if (annotation != null) {
				result.add(fieldObj);
			}
		}
		
		// Return result
		return result;
	}
	
	//--------------------------------------------------------------------------------------
	//
	//  AnnotationPathTree utility
	//
	//--------------------------------------------------------------------------------------
	
}