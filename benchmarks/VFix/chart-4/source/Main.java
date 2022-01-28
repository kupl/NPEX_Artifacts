import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Main {

	public static boolean pass = true;
	
	public static void main(String arg[]) {
		testXYAutoRange1();
	}
	
	public static void testXYAutoRange1() {
        XYSeries series = new XYSeries("Series 1");
        series.add(1.0, 1.0);
        series.add(2.0, 2.0);
        series.add(3.0, 3.0);
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createScatterPlot(
            "Test",
            "X",
            "Y",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            false,
            false
        );
        XYPlot plot = (XYPlot) chart.getPlot();
        LogAxis axis = new LogAxis("Log(Y)");
        plot.setRangeAxis(axis);
 
        if(Math.abs(axis.getUpperBound() - 3.1694019256486126)<0.0001)
        	System.out.println("axis's UpperBound is correct!!!");
        else{
        	System.out.println("axis's UpperBound is wrong!!! bias:"+(axis.getUpperBound() - 3.1694019256486126));
        	pass = false;
        }
    }
}