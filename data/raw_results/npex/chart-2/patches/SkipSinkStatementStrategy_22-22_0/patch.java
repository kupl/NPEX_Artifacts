import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;

public class Main {
	private static final double EPSILON = 0.0000000001;
	
	public static boolean pass = true;
	
	public static void main(String arg[]) {
		testBug2849731_2();//Chart-2A
	}
	
public static void testBug2849731_2() {
    org.jfree.data.xy.XYIntervalSeriesCollection d = new org.jfree.data.xy.XYIntervalSeriesCollection();
    org.jfree.data.xy.XYIntervalSeries s = new org.jfree.data.xy.XYIntervalSeries("S1");
    s.add(1.0, java.lang.Double.NaN, java.lang.Double.NaN, java.lang.Double.NaN, 1.5, java.lang.Double.NaN);
    d.addSeries(s);
    org.jfree.data.Range r = org.jfree.data.general.DatasetUtilities.iterateDomainBounds(d);
    /* NPEX_PATCH_BEGINS */
    if (r != null) {
        if (Main.eql(1.0, r.getLowerBound())) {
            java.lang.System.out.println("PASS");
        } else {
            Main.pass = false;
            java.lang.System.out.println("FAIL");
        }
    }
    if (Main.eql(1.0, r.getUpperBound())) {
        java.lang.System.out.println("PASS");
    } else {
        Main.pass = false;
        java.lang.System.out.println("FAIL");
    }
    s.add(1.0, 1.5, java.lang.Double.NaN, java.lang.Double.NaN, 1.5, java.lang.Double.NaN);
    r = org.jfree.data.general.DatasetUtilities.iterateDomainBounds(d);
    if (Main.eql(1.0, r.getLowerBound())) {
        java.lang.System.out.println("PASS");
    } else {
        Main.pass = false;
        java.lang.System.out.println("FAIL");
    }
    if (Main.eql(1.5, r.getUpperBound())) {
        java.lang.System.out.println("PASS");
    } else {
        Main.pass = false;
        java.lang.System.out.println("FAIL");
    }
    s.add(1.0, java.lang.Double.NaN, 0.5, java.lang.Double.NaN, 1.5, java.lang.Double.NaN);
    r = org.jfree.data.general.DatasetUtilities.iterateDomainBounds(d);
    if (Main.eql(0.5, r.getLowerBound())) {
        java.lang.System.out.println("PASS");
    } else {
        Main.pass = false;
        java.lang.System.out.println("FAIL");
    }
    if (Main.eql(1.5, r.getUpperBound())) {
        java.lang.System.out.println("PASS");
    } else {
        Main.pass = false;
        java.lang.System.out.println("FAIL");
    }
}
	public static boolean eql(double a,double b) {
		
		if(Math.abs(a-b)<=EPSILON)
			return true;
		else
			return false;
		
	}
}
