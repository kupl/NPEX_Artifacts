import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

public class Main {

	public static boolean pass = true;
	
	public static void main(String arg[]) {
		
		testToClass_object();
	}
	
    public static void testToClass_object() {
    	
    	if(ClassUtils.toClass(null)==null)
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    		    	
    	if(ArrayUtils.EMPTY_CLASS_ARRAY.equals(ClassUtils.toClass(ArrayUtils.EMPTY_OBJECT_ARRAY)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
  
        if(Arrays.equals(new Class[] { String.class, Integer.class, Double.class },ClassUtils.toClass(new Object[] { "Test", 1, 99d })))
        	System.out.println("PASS");
        else{
    		pass = false;
    		System.out.println("FAIL");
    	}

        if(Arrays.equals(new Class[] { String.class, null, Double.class },ClassUtils.toClass(new Object[] { "Test", null, 99d })))
        	System.out.println("PASS");
        else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    }
    
}
