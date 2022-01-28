import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.util.Matrix;

public class Main {

	public static boolean pass = true;
	
	public static void main(String arg[]) throws Exception {
		testPDFBOX2951();
	}
	
	public static void testPDFBOX2951() throws Exception {
		testLayerImport();
	}
	
	private final static File testResultsDir = new File("target/test-output");

    protected static void setUp() throws Exception
    {
        testResultsDir.mkdirs();
    }

    /**
     * Tests layer import.
     * @throws Exception if an error occurs
     */
    public static void testLayerImport() throws Exception
    {
        File mainPDF = createMainPDF();
        File overlay1 = createOverlay1();
        File targetFile = new File(testResultsDir, "text-with-form-overlay.pdf");

        PDDocument targetDoc = PDDocument.load(mainPDF);
        PDDocument overlay1Doc = PDDocument.load(overlay1);
        try
        {
            LayerUtility layerUtil = new LayerUtility(targetDoc);
            PDFormXObject form = layerUtil.importPageAsForm(overlay1Doc, 0);
            PDPage targetPage = targetDoc.getPage(0);
            layerUtil.wrapInSaveRestore(targetPage);
            AffineTransform at = new AffineTransform();
            layerUtil.appendFormAsLayer(targetPage, form, at, "overlay");

            targetDoc.save(targetFile.getAbsolutePath());
        }
        finally
        {
            targetDoc.close();
            overlay1Doc.close();
        }

        PDDocument doc = PDDocument.load(targetFile);
        try
        {
            PDDocumentCatalog catalog = doc.getDocumentCatalog();

            //OCGs require PDF 1.5 or later
            if(doc.getVersion()==1.5f)
            	System.out.println("PASS");
            else{
            	pass = false;
            	System.out.println("FAIL");
            }
            
            PDPage page = doc.getPage(0);
            PDOptionalContentGroup ocg = (PDOptionalContentGroup)page.getResources()
                    .getProperties(COSName.getPDFName("oc1"));
            
            if(ocg!=null)
            	System.out.println("PASS");
            else{
            	pass = false;
            	System.out.println("FAIL");
            }
            
            if("overlay".equals(ocg.getName()))
            	System.out.println("PASS");
            else{
            	pass = false;
            	System.out.println("FAIL");
            }
            
            PDOptionalContentProperties ocgs = catalog.getOCProperties();
            PDOptionalContentGroup overlay = ocgs.getGroup("overlay");
            
            if(ocg.getName().equals(overlay.getName()))
            	System.out.println("PASS");
            else{
            	pass = false;
            	System.out.println("FAIL");
            }

        }
        finally
        {
            doc.close();
        }
    }

    private static File createMainPDF() throws IOException
    {
        File targetFile = new File(testResultsDir, "text-doc.pdf");
        PDDocument doc = new PDDocument();
        try
        {
            //Create new page
            PDPage page = new PDPage();
            doc.addPage(page);
            PDResources resources = page.getResources();
            if( resources == null )
            {
                resources = new PDResources();
                page.setResources( resources );
            }

            final String[] text = new String[] {
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer fermentum lacus in eros",
                    "condimentum eget tristique risus viverra. Sed ac sem et lectus ultrices placerat. Nam",
                    "fringilla tincidunt nulla id euismod. Vivamus eget mauris dui. Mauris luctus ullamcorper",
                    "leo, et laoreet diam suscipit et. Nulla viverra commodo sagittis. Integer vitae rhoncus velit.",
                    "Mauris porttitor ipsum in est sagittis non luctus purus molestie. Sed placerat aliquet",
                    "vulputate."
            };

            //Setup page content stream and paint background/title
            PDPageContentStream contentStream = new PDPageContentStream(doc, page, AppendMode.OVERWRITE, false);
            PDFont font = PDType1Font.HELVETICA_BOLD;
            contentStream.beginText();
            contentStream.newLineAtOffset(50, 720);
            contentStream.setFont(font, 14);
            contentStream.showText("Simple test document with text.");
            contentStream.endText();
            font = PDType1Font.HELVETICA;
            contentStream.beginText();
            int fontSize = 12;
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(50, 700);
            for (String line : text)
            {
                contentStream.newLineAtOffset(0, -fontSize * 1.2f);
                contentStream.showText(line);
            }
            contentStream.endText();
            contentStream.close();

            doc.save(targetFile.getAbsolutePath());
        }
        finally
        {
            doc.close();
        }
        return targetFile;
    }

    private static File createOverlay1() throws IOException
    {
        File targetFile = new File(testResultsDir, "overlay1.pdf");
        PDDocument doc = new PDDocument();
        try
        {
            //Create new page
            PDPage page = new PDPage();
            doc.addPage(page);
            PDResources resources = page.getResources();
            if( resources == null )
            {
                resources = new PDResources();
                page.setResources( resources );
            }

            //Setup page content stream and paint background/title
            PDPageContentStream contentStream = new PDPageContentStream(doc, page, AppendMode.OVERWRITE, false);
            PDFont font = PDType1Font.HELVETICA_BOLD;
            contentStream.setNonStrokingColor(Color.LIGHT_GRAY);
            contentStream.beginText();
            float fontSize = 96;
            contentStream.setFont(font, fontSize);
            String text = "OVERLAY";
            //float sw = font.getStringWidth(text);
            //Too bad, base 14 fonts don't return character metrics.
            PDRectangle crop = page.getCropBox();
            float cx = crop.getWidth() / 2f;
            float cy = crop.getHeight() / 2f;
            Matrix transform = new Matrix();
            transform.translate(cx, cy);
            transform.rotate(Math.toRadians(45));
            transform.translate(-190 /* sw/2 */, 0);
            contentStream.setTextMatrix(transform);
            contentStream.showText(text);
            contentStream.endText();
            contentStream.close();

            doc.save(targetFile.getAbsolutePath());
        }
        finally
        {
            doc.close();
        }
        return targetFile;
    }

}
