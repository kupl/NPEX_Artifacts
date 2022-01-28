import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.geometry.euclidean.threed.SubLine;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Main {

	public static boolean pass = true;
	
	public static void main(String arg[]) {
		testIntersectionNotIntersecting();
	}
	
    public static void testIntersectionNotIntersecting() throws MathIllegalArgumentException {
        SubLine sub1 = new SubLine(new Vector3D(1, 1, 1), new Vector3D(1.5, 1, 1));
        SubLine sub2 = new SubLine(new Vector3D(2, 3, 0), new Vector3D(2, 3, 0.5));
        if(sub1.intersection(sub2, true)==null)
        	System.out.println("PASS");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }
        
        if(sub1.intersection(sub2, false)==null)
        	System.out.println("PASS");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }
    }
}
