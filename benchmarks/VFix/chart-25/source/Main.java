import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

public class Main {

		public static void main(String[] args) {
			
			 testDrawWithNullMeanVertical();//Chart-25A
//			 testDrawWithNullDeviationVertical();//Chart-25B
//			 testDrawWithNullMeanHorizontal();//Chart-25C
//			 testDrawWithNullDeviationHorizontal();//Chart-25D
		}
		
		public static void testDrawWithNullMeanVertical() {

	            DefaultStatisticalCategoryDataset dataset 
	                    = new DefaultStatisticalCategoryDataset();
	            dataset.add(1.0, 2.0, "S1", "C1");
	            dataset.add(null, new Double(4.0), "S1", "C2");
	            CategoryPlot plot = new CategoryPlot(dataset, 
	                    new CategoryAxis("Category"), new NumberAxis("Value"), 
	                    new StatisticalBarRenderer());
	            JFreeChart chart = new JFreeChart(plot);
	            /* BufferedImage image = */ chart.createBufferedImage(300, 200, 
	                    null);
	    }
		
	    public static void testDrawWithNullDeviationVertical() {
	            DefaultStatisticalCategoryDataset dataset 
	                    = new DefaultStatisticalCategoryDataset();
	            dataset.add(1.0, 2.0, "S1", "C1");
	            dataset.add(new Double(4.0), null, "S1", "C2");
	            CategoryPlot plot = new CategoryPlot(dataset, 
	                    new CategoryAxis("Category"), new NumberAxis("Value"), 
	                    new StatisticalBarRenderer());
	            JFreeChart chart = new JFreeChart(plot);
	            /* BufferedImage image = */ chart.createBufferedImage(300, 200, 
	                    null);
	    }
	    
	    public static void testDrawWithNullMeanHorizontal() {
	            DefaultStatisticalCategoryDataset dataset 
	                    = new DefaultStatisticalCategoryDataset();
	            dataset.add(1.0, 2.0, "S1", "C1");
	            dataset.add(null, new Double(4.0), "S1", "C2");
	            CategoryPlot plot = new CategoryPlot(dataset, 
	                    new CategoryAxis("Category"), new NumberAxis("Value"), 
	                    new StatisticalBarRenderer());
	            plot.setOrientation(PlotOrientation.HORIZONTAL);
	            JFreeChart chart = new JFreeChart(plot);
	            /* BufferedImage image = */ chart.createBufferedImage(300, 200, 
	                    null);
	    }
	    
	    public static void testDrawWithNullDeviationHorizontal() {
	            DefaultStatisticalCategoryDataset dataset 
	                    = new DefaultStatisticalCategoryDataset();
	            dataset.add(1.0, 2.0, "S1", "C1");
	            dataset.add(new Double(4.0), null, "S1", "C2");
	            CategoryPlot plot = new CategoryPlot(dataset, 
	                    new CategoryAxis("Category"), new NumberAxis("Value"), 
	                    new StatisticalBarRenderer());
	            plot.setOrientation(PlotOrientation.HORIZONTAL);
	            JFreeChart chart = new JFreeChart(plot);
	            /* BufferedImage image = */ chart.createBufferedImage(300, 200, 
	                    null);
	    }

}
