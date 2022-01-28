import org.apache.sling.scripting.thymeleaf.internal.SlingResourceTemplateResolver;

public class Main {
	public static void main(String[] args) {
		
		SlingResourceTemplateResolver srtr=new SlingResourceTemplateResolver();
		srtr.resolveTemplate(null,null,null,null);
	}
	
}
