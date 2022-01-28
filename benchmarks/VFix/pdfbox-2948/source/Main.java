import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDStream;

public class Main {
	public static boolean pass = true;
	public static void main(String[] args) throws Exception {
		testPDFBOX2948();
	}
	    
	public static void testPDFBOX2948() throws Exception{
	    testCreateInputStreamNullFilters();
	    testCreateInputStreamEmptyFilters();
	    testCreateInputStreamNullStopFilters();
	}
	    
	public static void testCreateInputStreamNullFilters() throws Exception{
	    
		PDDocument doc = new PDDocument();
	    InputStream is = new ByteArrayInputStream(new byte[] { 12, 34, 56, 78 });
	    PDStream pdStream = new PDStream(doc, is, (COSArray) null);
	        
	    if(pdStream.getFilters()==null)
	        System.out.println("PASS");
	    else{
	    	pass = false;
	        System.out.println("FAIL");
	    }
	        
	    List<String> stopFilters = new ArrayList<String>();
	    stopFilters.add(COSName.DCT_DECODE.toString());
	    stopFilters.add(COSName.DCT_DECODE_ABBREVIATION.toString());
	        
	    is = pdStream.createInputStream(stopFilters);
	        
	    if(12==is.read())
	        System.out.println("PASS");
	    else{
	    	pass = false;
	        System.out.println("FAIL");
	    }
	        
	    if(34==is.read())
	        System.out.println("PASS");
	    else{
	    	pass = false;
	        System.out.println("FAIL");
	    }
	        
	    if(56==is.read())
	        System.out.println("PASS");
	    else{
	    	pass = false;
	        System.out.println("FAIL");
	    }
	        
	    if(78==is.read())
	        System.out.println("PASS");
	    else{
	    	pass = false;
	        System.out.println("FAIL");
	    }
	        
	    if(-1==is.read())
	        System.out.println("PASS");
	    else{
	    	pass = false;
	        System.out.println("FAIL");
	    }
	        
	    doc.close();
} 
	    
	    /**
	     * Test for empty filter list
	     */
	    public static void testCreateInputStreamEmptyFilters() throws Exception
	    {
	        PDDocument doc = new PDDocument();
	        InputStream is = new ByteArrayInputStream(new byte[] { 12, 34, 56, 78 });
	        PDStream pdStream = new PDStream(doc, is, new COSArray());
	        
	        if(pdStream.getFilters().size()==0)
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        List<String> stopFilters = new ArrayList<String>();
	        stopFilters.add(COSName.DCT_DECODE.toString());
	        stopFilters.add(COSName.DCT_DECODE_ABBREVIATION.toString());
	        
	        is = pdStream.createInputStream(stopFilters);
	        
	        if(12==is.read())
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        if(34==is.read())
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        if(56==is.read())
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        if(78==is.read())
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        if(-1==is.read())
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        doc.close();
	    }
	    
	    /**
	     * Test for null stop filters
	     */
	    public static void testCreateInputStreamNullStopFilters() throws Exception
	    {
	        PDDocument doc = new PDDocument();
	        InputStream is = new ByteArrayInputStream(new byte[] { 12, 34, 56, 78 });
	        PDStream pdStream = new PDStream(doc, is, new COSArray());
	        
	        if(pdStream.getFilters().size()==0)
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        is = pdStream.createInputStream(null);
	        
	        if(12==is.read())
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        if(34==is.read())
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        if(56==is.read())
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        if(78==is.read())
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        if(-1==is.read())
	        	System.out.println("PASS");
	        else{
		    	pass = false;
		        System.out.println("FAIL");
		    }
	        
	        doc.close();
	    }
}
	