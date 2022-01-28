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
        XYIntervalSeriesCollection d = new XYIntervalSeriesCollection();
        XYIntervalSeries s = new XYIntervalSeries("S1");
        s.add(1.0, Double.NaN, Double.NaN, Double.NaN, 1.5, Double.NaN);
        d.addSeries(s);
        Range r = DatasetUtilities.iterateDomainBounds(d);
        
        if(eql(1.0, r.getLowerBound()))
        	System.out.println("PASS");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }
        	
        
        if(eql(1.0, r.getUpperBound()))
        	System.out.println("PASS");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }
        
        s.add(1.0, 1.5, Double.NaN, Double.NaN, 1.5, Double.NaN);
        
        r = DatasetUtilities.iterateDomainBounds(d);
        
        if(eql(1.0, r.getLowerBound()))
        	System.out.println("PASS");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }
        				
        if(eql(1.5, r.getUpperBound()))
        	System.out.println("PASS");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }
        
        s.add(1.0, Double.NaN, 0.5, Double.NaN, 1.5, Double.NaN);
        
        r = DatasetUtilities.iterateDomainBounds(d);
        
        if(eql(0.5, r.getLowerBound()))
        	System.out.println("PASS");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }
        	
        if(eql(1.5, r.getUpperBound()))
        	System.out.println("PASS");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }
    }
	public static boolean eql(double a,double b) {
		
		if(Math.abs(a-b)<=EPSILON)
			return true;
		else
			return false;
		
	}
}
