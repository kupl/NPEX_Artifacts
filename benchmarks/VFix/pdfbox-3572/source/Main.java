import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

public class Main {

	
	  public static void main(String[] args) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
	    	testPDFBOX3572();
	    }
	  public static void testPDFBOX3572() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
		        
		  String password = "pw";    
		  String cipherString = "AES/CBC/PKCS5Padding";
		  String testFilename = "test.pdf";

		  PDDocument document = new PDDocument();
		  AccessPermission ap = new AccessPermission();
		  ap.setReadOnly();

		  StandardProtectionPolicy policy = new StandardProtectionPolicy(password, password, ap);
		  policy.setEncryptionKeyLength(128);
		  policy.setPreferAES(true);
		  document.protect(policy);
		  document.getDocumentInformation().setAuthor("author");
		  document.save(testFilename);
		  document.close();
		  Cipher cipher;
		  // Decryption with SunJCE works
		  cipher = Cipher.getInstance(cipherString);
		  document = PDDocument.load(new File(testFilename), password);
		  Security.removeProvider("SunJCE");

		  // Decryption with BouncyCastle fails with NPE
		  cipher = Cipher.getInstance(cipherString);
		  document = PDDocument.load(new File(testFilename), password);

	      System.out.println("PASS");
		  
	  }
	  
}
	