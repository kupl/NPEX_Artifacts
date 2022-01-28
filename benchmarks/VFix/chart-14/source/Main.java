import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;

public class Main {
	
	public static boolean pass = true;
	public static void main(String arg[]) {
		testRemoveDomainMarker();
	}
	
    public static void testRemoveDomainMarker() {
    	CategoryPlot plot = new CategoryPlot();
    	if(plot.removeDomainMarker(new CategoryMarker("Category 1"))==false)
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    }
}
