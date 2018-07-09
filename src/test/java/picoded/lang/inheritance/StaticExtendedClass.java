package picoded.lang.inheritance;

// Base class to build on
public class StaticExtendedClass extends StaticBaseClass {
	
	// Static function to overwrite?
	public static String toExtend() {
		return StaticBaseClass.toExtend() + " plus";
	}
}