import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;

public class Main {
	private static JFreeChart chart;
	public static void main(String[] args) {
		
		testDrawWithNullInfo();
	}
	
    public static void testDrawWithNullInfo() {
    		
    	chart = createAreaChart();
        BufferedImage image = new BufferedImage(200 , 100, 
                    BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        chart.draw(g2, new Rectangle2D.Double(0, 0, 200, 100), null, 
                   null);
        g2.dispose();
    }
    
    private static JFreeChart createAreaChart() {
        Number[][] data = new Integer[][]
            {{new Integer(-3), new Integer(-2)},
             {new Integer(-1), new Integer(1)},
             {new Integer(2), new Integer(3)}};
        CategoryDataset dataset = DatasetUtilities.createCategoryDataset("S", 
                "C", data);
        return ChartFactory.createAreaChart("Area Chart", "Domain", "Range",
                dataset, PlotOrientation.HORIZONTAL, true, true, true);

    }
}
