import java.util.Collection;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.iterators.FilterListIterator;
import org.apache.commons.collections.list.GrowthList;

public class Main {

	public static boolean pass = true;
	public static void main(String arg[])throws Throwable {
        Collection<Predicate<Object>> var7 = new GrowthList<Predicate<Object>>();
        Predicate<Object> var9 = PredicateUtils.anyPredicate(var7);
        FilterListIterator<Object> var13 = new FilterListIterator<Object>(var9);
        
        if(var13.hasNext()==false)
        	System.out.println("PASS");
        else{
        	System.out.println("FAIL");
        	pass = false;
        }
        	
	}
}
