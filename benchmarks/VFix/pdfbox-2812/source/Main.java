import java.awt.color.ColorSpace;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpaceFactory;
import org.apache.pdfbox.pdmodel.graphics.color.PDICCBased;


public class Main {

	public static boolean pass = true;
	public static void main(String arg[]) throws IOException {
		testPDFBOX2812();
	}

	public static void testPDFBOX2812() throws IOException{
        PDDocument doc = new PDDocument();
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        
        if(cs.isCS_sRGB()==false) {// this test doesn't work with CS_sRGB
        	System.out.println("PASS");
        }else {
        	System.out.println("FAIL");
        	pass= false;
        }

        PDICCBased iccBased = (PDICCBased) PDColorSpaceFactory.createColorSpace(doc, cs);
        
        if("ICCBased".equals(iccBased.getName())) {
        	System.out.println("PASS");
        }else {
        	System.out.println("FAIL");
        	pass = false;
        }
    }
}