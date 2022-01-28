import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

public class Main {

	public static void main(String[] arg) {
		testDrawWithNullDataset();
	}
	public static void testDrawWithNullDataset() {
	        JFreeChart chart = ChartFactory.createPieChart3D("Test", null, true, 
	                false, false);
	            BufferedImage image = new BufferedImage(200 , 100, 
	                    BufferedImage.TYPE_INT_RGB);
	            Graphics2D g2 = image.createGraphics();
	            chart.draw(g2, new Rectangle2D.Double(0, 0, 200, 100), null, null);
	            g2.dispose();
	 }
}
