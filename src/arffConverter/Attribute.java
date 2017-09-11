package arffConverter;

public class Attribute {
	
	public static final String numericTag = "NUMERIC";
	public static final String stringTag = "NUMERIC";
	
	public static String getType (String data) {
		try {
			Float.parseFloat(data);
			// Didn't throw an error, must be numeric
			return numericTag;
		} catch (NumberFormatException nfe) {
			// Threw an error, must not be numeric
			return stringTag;
		}
	}
	
}
