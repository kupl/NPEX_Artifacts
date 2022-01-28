import org.apache.felix.scrplugin.Project;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.helper.ClassScanner;

public class Main {

	public static void main(String[] args) throws SCRDescriptorFailureException, SCRDescriptorException {
		testFelix5464();
	}
    public static void testFelix5464() throws SCRDescriptorFailureException, SCRDescriptorException {
    	
    	Project project = new Project();
    	project.setClassLoader(new com.sun.org.apache.bcel.internal.util.ClassLoader());
    	ClassScanner cs = new ClassScanner(null, null, null, null);
    	Object obj = new Object();
    	cs.processClass(obj.getClass(),null);
    }
}
