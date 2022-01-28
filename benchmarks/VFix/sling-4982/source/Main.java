import junitx.util.PrivateAccessor;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.security.impl.ContentDispositionFilter;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;


public class Main {
	private static ContentDispositionFilter contentDispositionFilter;
    private final static Mockery context = new JUnit4Mockery();
        
    private static final String JCR_CONTENT_LEAF = "jcr:content";
    public static boolean pass = true;
    
    public static void main(String arg[]) throws Throwable {
    	
    	contentDispositionFilter = new ContentDispositionFilter();
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);       
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        final Resource resource = context.mock(Resource.class);
        
        context.checking(new Expectations() {
            {
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(null));
                allowing(resource).getChild(JCR_CONTENT_LEAF);
                will(returnValue(null));
            }
        });     
        
        Boolean result = (Boolean) PrivateAccessor.invoke(rewriterResponse,"isJcrData",  new Class[]{Resource.class},new Object[]{resource});
        
        if(result == false){
        	System.out.println("PASS");
        }else{
        	pass = false;
        	System.out.println("FAIL");
        }
        
    }
}
