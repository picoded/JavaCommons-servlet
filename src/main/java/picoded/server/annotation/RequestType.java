package picoded.server.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import picoded.core.common.HttpRequestType;

/**
 * Annotation for request path, used to detect endpoint to handle request at the appropriate path
 * 
 * ```
 * @RequestPath("hello")
 * @RequestType("GET")
 * public void loadWorld() {
 * 	getPrintWriter().println("world");
 * }
 * ```
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestType {
	public String[] value();
}
