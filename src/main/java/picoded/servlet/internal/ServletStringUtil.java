package picoded.servlet.internal;

import picoded.core.exception.ExceptionMessage;

/**
 * Several reused string manipulation utils, used repeatingly in
 * the servlet project
 */
public class ServletStringUtil {

	/**
	 * Static class annotation exception
	 */
	ServletStringUtil() {
		throw new RuntimeException(ExceptionMessage.staticClassConstructor);
	}

	/**
	 * Normalize a URI string path, by removing
	 * duplicative "/" slash pathings
	 * 
	 * ```
	 * normalizeUriString("/hello//world/"); // "hello/world"
	 * ```
	 * 
	 * @param   raw string to parse
	 * 
	 * @return  parsed string
	 */
	public static String normalizeUriString(String raw) {
		// Remove "/" prefix and suffix
		if (raw.startsWith("/") || raw.startsWith("\\")) {
			raw = raw.substring(1);
		}
		if (raw.endsWith("/") || raw.endsWith("\\")) {
			raw = raw.substring(0, raw.length() - 1);
		}

		// Does duplicative "//" remove
		while( raw.indexOf("//") >= 0 ) {
			raw = raw.replaceAll("[\\\\/\\\\/]", "/");
		}
		
		// Does the actual splitting
		return raw;
	}

	/**
	 * Split a uri string (after normalizing it)
	 * 
	 * @param   raw string to parse
	 * 
	 * @return  split string as an array
	 */
	public static String[] splitUriString(String raw) {
		return normalizeUriString(raw).split("[\\\\/]");
	}
}