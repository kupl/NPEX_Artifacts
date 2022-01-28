import org.apache.pdfbox.preflight.Validator_A1b;
import org.apache.xmpbox.xml.DomXmpParser;

public class Main {
	public static void main(String[] args) throws Exception {
		String[] arg = {System.getProperty("user.dir")+"/input/pdfbox2477.pdf"};
		DomXmpParser p = new DomXmpParser();
		p.getClass();
		Validator_A1b.main(arg);
	}
}
