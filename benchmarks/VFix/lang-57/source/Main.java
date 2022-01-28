import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;


public class Main {

	    public static void main(String arg[]) {
	        setUp();
//	      testConstructor();
	    }
	    
	    public static void setUp() {
	        LocaleUtils.isAvailableLocale(Locale.getDefault());
	    }
}
