import org.apache.commons.lang3.text.StrBuilder;

public class Main {

	public static boolean pass = true;
	
	public static void main(String arg[]) {
		
		testLang412Right();//A
//		testLang412Left();//B
	}
	
    public static void testLang412Right() {
        StrBuilder sb = new StrBuilder();
        sb.appendFixedWidthPadRight(null, 10, '*');
        
        if("**********".equals(sb.toString()))
        	System.out.println("PASS! Failed to invoke appendFixedWidthPadRight correctly");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }
    }

    public static void testLang412Left() {
        StrBuilder sb = new StrBuilder();
        sb.appendFixedWidthPadLeft(null, 10, '*');
        
        if("**********".equals(sb.toString()))
        	System.out.println("PASS! Failed to invoke appendFixedWidthPadLeft correctly");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }

    }
}