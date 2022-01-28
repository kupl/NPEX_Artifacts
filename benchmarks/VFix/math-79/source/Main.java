import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.EuclideanIntegerPoint;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;

public class Main {

	public static boolean pass = true;
	public static void main(String arg[]) {
		testPerformClusterAnalysisDegenerate();
	}
	
	public static void testPerformClusterAnalysisDegenerate() {
        KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer = new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(
                new Random(1746432956321l));
        EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {
                new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
                new EuclideanIntegerPoint(new int[] { 1960, 373200 }), };
        List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(Arrays.asList(points), 1, 1);
        
        if(clusters.size()==1){
        	System.out.println("PASS");
        }else{
        	pass = false;
        	System.out.println("FAIL");
        }
        
        if((clusters.get(0).getPoints().size())==2){
        	System.out.println("PASS");
        }else{
        	pass = false;
        	System.out.println("FAIL");
        }
        	
        EuclideanIntegerPoint pt1 = new EuclideanIntegerPoint(new int[] { 1959, 325100 });
        EuclideanIntegerPoint pt2 = new EuclideanIntegerPoint(new int[] { 1960, 373200 });
        
        if(clusters.get(0).getPoints().contains(pt1))
        	System.out.println("PASS");
        else{
        	pass= false;
        	System.out.println("FAIL");
        }
        
        if(clusters.get(0).getPoints().contains(pt2))
        	System.out.println("PASS");
        else{
        	pass = false;
        	System.out.println("FAIL");
        }

    } 
	
}
