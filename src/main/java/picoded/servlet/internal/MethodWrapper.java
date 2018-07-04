package picoded.servlet.internal;

import java.lang.reflect.Method;

/**
 * Wrapper class for Method, so that we can extend in the future
 * for more functionality, and / or meta data information.
 */
public class MethodWrapper {

	/**
	 * The core Method object
	 */
	public Method method = null;

	/**
	 * Constructor with method initialization object
	 */
	public MethodWrapper(Method in) {
		this.method = in;
	}
}