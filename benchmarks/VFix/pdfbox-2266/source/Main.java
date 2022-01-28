import java.io.IOException;

import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;

public class Main {
	public static void main(String[] args) throws IOException{
		PDPattern pd = new PDPattern(null);
		float[] a = {0,0,0};
		PDColor pc = new PDColor(a, null);
		pd.toPaint(null, pc, 0);
	}
}
