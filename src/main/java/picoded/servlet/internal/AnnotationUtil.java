package picoded.servlet.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import picoded.core.exception.ExceptionMessage;

/**
 * Static utility class used to faciliated annotation processing
 */
public class AnnotationUtil {

	/**
	 * Static class annotation exception
	 */
	AnnotationUtil() {
		throw new RuntimeException(ExceptionMessage.staticClassConstructor);
	}

	/**
	 * Fetch a list of Methods found in the given class with the annotation
	 * 
	 * @param  inClass object to scan for methods
	 * @param  annotationClass to scan methods for
	 * 
	 * @return  list of Method with valid annotation
	 */
	static <A,B extends Annotation> List<Method> fetchMethodsWithAnnotation(Class<A> inClass, Class<B>annotationClass) {
		// Result to return
		List<Method> result = new ArrayList<>();

		// Iterate the class methods, and append to result those with valid annotation
		Method[] methodArray = inClass.getMethods();
		for(Method methodObj : methodArray) {
			B annotation = methodObj.getAnnotation(annotationClass);
			if(annotation != null) {
				result.add(methodObj);
			}
		}

		// Return result
		return result;
	}


}