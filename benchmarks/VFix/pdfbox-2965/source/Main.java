import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;


public class Main {
	public static boolean pass = true;
	
	private static PDDocument document;
	private static PDAcroForm acroForm;
	    
	private static final File OUT_DIR = new File("target/test-output");
	    
	    
	public static void main(String[] args){
		document = new PDDocument();
        acroForm = new PDAcroForm(document);
        document.getDocumentCatalog().setAcroForm(acroForm);
        
        if(acroForm.getFields()==null){
        	pass = false;
        	return;
        }
        	
        if(acroForm.getFields().size()!=0){
        	pass = false;
        	return;
        }
        
        if(acroForm.getField("foo")!=null){
        	pass = false;
        	return;
        }
        
        // remove the required entry which is the case for some
        // PDFs (see PDFBOX-2965)
        acroForm.getCOSObject().removeItem(COSName.FIELDS);
        
        // ensure there is always an empty collection returned
        if(acroForm.getFields()==null){
        	pass = false;
        	return;
        }
        
        if(acroForm.getFields().size()!=0){
        	pass = false;
        	return;
        }

        // there shouldn't be an exception if there is no such field
        if(acroForm.getField("foo")!=null){
        	pass = false;
        	return;
        }
	}
}
