package picoded.servlet.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for request path, used to detect endpoint to handle request at the appropriate path
 * 
 * ```
 * @ApiPath("api/hello")
 * public Map<String,Object> loadWorld() {
 * 	return ConvertJSON.toMap("{ hello : 'world' }");
 * }
 * ```
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiPath {
	public String value();
}