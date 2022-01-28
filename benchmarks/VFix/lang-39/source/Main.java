import org.apache.commons.lang3.StringUtils;

public class Main {
	public static boolean pass = true;
	
	public static void main(String arg[]) {
		testReplace_StringStringArrayStringArray();
	}
	
    public static void testReplace_StringStringArrayStringArray() {

    	if(StringUtils.replaceEach(null, new String[]{"a"}, new String[]{"b"})==null)
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    		      
    	if("".equals(StringUtils.replaceEach("", new String[]{"a"}, new String[]{"b"})))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("aba".equals(StringUtils.replaceEach("aba", null, null)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("aba".equals(StringUtils.replaceEach("aba", new String[0], null)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("aba".equals(StringUtils.replaceEach("aba", null, new String[0])))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("aba".equals(StringUtils.replaceEach("aba", new String[]{"a"}, null)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("b".equals(StringUtils.replaceEach("aba", new String[]{"a"}, new String[]{""})))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("aba".equals(StringUtils.replaceEach("aba", new String[]{null}, new String[]{"a"})))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
        if("wcte".equals(StringUtils.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"w", "t"})))
        	System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
        
        if("dcte".equals(StringUtils.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "t"})))
        	System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
        
        if("bcc".equals(StringUtils.replaceEach("abc", new String[]{"a", "b"}, new String[]{"b", "c"})))
        	System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
        
        if("q651.506bera".equals( StringUtils.replaceEach("d216.102oren",
            new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", 
                "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", 
                "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", 
                "U", "V", "W", "X", "Y", "Z", "1", "2", "3", "4", "5", "6", "7", "8", "9"},
            new String[]{"n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "a", 
                "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "N", "O", "P", "Q", 
                "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "A", "B", "C", "D", "E", "F", "G", 
                "H", "I", "J", "K", "L", "M", "5", "6", "7", "8", "9", "1", "2", "3", "4"})))
        	System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
        
        if("aba".equals(StringUtils.replaceEach("aba", new String[]{"a"}, new String[]{null})))
        	System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}

        if("cbc".equals(StringUtils.replaceEach("aba", new String[]{"a", "b"}, new String[]{"c", null})))
        	System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
        
    }
}
