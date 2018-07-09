package picoded.lang.inheritance;

// Base class to build on
public class StaticBaseClass {
	// Hello world static method, to be inherited
	public static String hello() {
		return "world";
	}
	
	// Static function to overwrite?
	public static String toExtend() {
		return "base";
	}
}