import org.apache.commons.lang3.StringUtils;

public class Main {
	
	public static boolean pass = true;
	
	public static void main(String arg[]) {
		testJoin_Objectarray();
//		testJoin_ArrayChar();

	}
	
	static final String WHITESPACE;
    static final String NON_WHITESPACE;
    static final String TRIMMABLE;
    static final String NON_TRIMMABLE;
    static {
        String ws = "";
        String nws = "";
        String tr = "";
        String ntr = "";
        for (int i = 0; i < Character.MAX_VALUE; i++) {
            if (Character.isWhitespace((char) i)) {
                ws += String.valueOf((char) i);
                if (i > 32) {
                    ntr += String.valueOf((char) i);
                }
            } else if (i < 40) {
                nws += String.valueOf((char) i);
            }
        }
        for (int i = 0; i <= 32; i++) {
            tr += String.valueOf((char) i);
        }
        WHITESPACE = ws;
        NON_WHITESPACE = nws;
        TRIMMABLE = tr;
        NON_TRIMMABLE = ntr;
    }

    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = {null};
    private static final Object[] NULL_TO_STRING_LIST = {
    	new Object(){
    		@Override
    		public String toString() {
    			return null;
    		}
    	}
    };
    private static final String[] MIXED_ARRAY_LIST = {null, "", "foo"};
    private static final Object[] MIXED_TYPE_LIST = {"foo", Long.valueOf(2L)};

    private static final char   SEPARATOR_CHAR = ';';

    private static final String TEXT_LIST_CHAR = "foo;bar;baz";
    
    public static void testJoin_ArrayChar() {
    	
    	System.out.println("testJoin_ArrayChar");
    	
    	if(StringUtils.join((Object[]) null, ',') == null)
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if(TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if(";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}

    	if("/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	   
    	if("null".equals(StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	     
    	if("2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1)))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    }
    
    public static void testJoin_Objectarray() {
    	    	
    	if(StringUtils.join((Object[]) null)==null)
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
      
    	if("".equals(StringUtils.join()))
  		  	System.out.println("PASS");
  	  	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("".equals(StringUtils.join((Object) null)))
  		  	System.out.println("PASS");
  	  	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
      
    	if("".equals(StringUtils.join(EMPTY_ARRAY_LIST)))
  		  	System.out.println("PASS");
  	  	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("".equals(StringUtils.join(NULL_ARRAY_LIST)))
  		  	System.out.println("PASS");
  	  	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("null".equals(StringUtils.join(NULL_TO_STRING_LIST)))
  		  	System.out.println("PASS");
  	  	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("abc".equals(StringUtils.join(new String[] {"a", "b", "c"})))
  		  	System.out.println("PASS");
  	  	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
     
    	if("a".equals(StringUtils.join(new String[] {null, "a", ""})))
  		  	System.out.println("PASS");
  	  	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("foo".equals(StringUtils.join(MIXED_ARRAY_LIST)))
  		  	System.out.println("PASS");
  	  	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    	if("foo2".equals(StringUtils.join(MIXED_TYPE_LIST)))
  		  	System.out.println("PASS");
  	  	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
  }
}
