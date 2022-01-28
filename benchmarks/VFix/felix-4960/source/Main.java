import java.util.Enumeration;
import org.apache.felix.framework.BundleRevisionImpl;

public class Main{
    public static boolean pass=true;
    public static void main(String arg[]) {
        testGetResourcesLocalNullContentPath();
    }
    
    public static void testGetResourcesLocalNullContentPath()
    {
        BundleRevisionImpl bri = new BundleRevisionImpl(null, null);
        
        Enumeration<?> en = bri.getResourcesLocal("foo");
        if(en.hasMoreElements()==false)
            System.out.println("PASS");
        else{
        	pass = false;
            System.out.println("FAIL");
        }
    }
}
