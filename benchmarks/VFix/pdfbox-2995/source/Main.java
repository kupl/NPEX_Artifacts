import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;


public class Main {
	
	public static boolean pass = true;
	
	public static void main(String arg[]) throws IOException {
		testPDFBOX2995();
	}
	
	private static PDDocument document;
	private static PDAcroForm acroForm;
	    	    
	public static void testPDFBOX2995() throws IOException {
		setUp();
		
		if(acroForm.getDefaultAppearance().isEmpty())
			System.out.println("PASS");
		else{
			pass = false;
			System.out.println("FAIL");
		}
		
		acroForm.setDefaultAppearance("/Helv 0 Tf 0 g");
		
		if("/Helv 0 Tf 0 g".equals(acroForm.getDefaultAppearance()))
			System.out.println("PASS");
		else{
			pass = false;
			System.out.println("FAIL");
		}
		
		tearDown();
	}

	public static void setUp()
	{
	    document = new PDDocument();
	    acroForm = new PDAcroForm(document);
	    document.getDocumentCatalog().setAcroForm(acroForm);
	 }

	 public static void tearDown() throws IOException
	 {
	    document.close();
	 }

}
