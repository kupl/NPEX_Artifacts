import org.jfree.data.category.DefaultIntervalCategoryDataset;

public class Main {
	
	public static boolean pass = true;
	
	public static void main(String arg[]) {
		testChart16A();
//		testChart16B();
	}

	public static void testChart16A() {//this.categoryKeys
		testGetCategoryIndex();
		testGetRowCount();
		testGetColumnIndex();
		testGetColumnCount();
	}
	
	public static void testChart16B() {//this.seriesKeys
		testGetSeriesIndex();
		testGetRowIndex();
	}
	
    /**
     * Some checks for the getCategoryIndex() method.
     * @throws CloneNotSupportedException 
     */
    public static void testGetCategoryIndex(){
    	
    	// check an empty dataset
    	DefaultIntervalCategoryDataset empty 
    	        = new DefaultIntervalCategoryDataset(new double[0][0], new double[0][0]);
    	if(-1 == empty.getCategoryIndex("ABC"))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    }
    
    /**
     * Some checks for the getSeriesIndex() method.
     */
    public static void testGetSeriesIndex() {
    	
    	// check an empty dataset
    	DefaultIntervalCategoryDataset empty 
    	        = new DefaultIntervalCategoryDataset(new double[0][0], new double[0][0]);
    	
    	if(-1 == empty.getSeriesIndex("ABC"))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    	
    }
    
    /**
     * Some checks for the getRowCount() method.
     */
    public static void testGetRowCount() {
    	// check an empty dataset
    	DefaultIntervalCategoryDataset empty 
    	        = new DefaultIntervalCategoryDataset(new double[0][0], new double[0][0]);

        if(empty.getColumnCount() == 0)
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    }
    
    /**
     * Some checks for the getRowIndex() method.
     */
    public static void testGetRowIndex() {
    	// check an empty dataset
    	DefaultIntervalCategoryDataset empty 
    	        = new DefaultIntervalCategoryDataset(new double[0][0], 
    	        		new double[0][0]);

        if(empty.getRowIndex("ABC") == -1)
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    }
    
    /**
     * Some checks for the getColumnCount() method.
     */
    public static void testGetColumnCount() {
    	// check an empty dataset
    	DefaultIntervalCategoryDataset empty 
    	        = new DefaultIntervalCategoryDataset(new double[0][0], 
    	        		new double[0][0]);

        if(empty.getColumnCount() == 0)
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    }
    
    /**
     * Some checks for the getColumnIndex() method.
     */
    public static void testGetColumnIndex() {
    	// check an empty dataset
    	DefaultIntervalCategoryDataset empty 
    	        = new DefaultIntervalCategoryDataset(new double[0][0], 
    	        		new double[0][0]);
        if(empty.getColumnIndex("ABC") == -1)
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    }
    
    /**
     * A check to ensure that an empty dataset can be cloned.
     */
    public static void testCloning2() {
    	DefaultIntervalCategoryDataset d1 
                = new DefaultIntervalCategoryDataset(new double[0][0], 
        		    new double[0][0]);
        DefaultIntervalCategoryDataset d2 = null;
        try {
            d2 = (DefaultIntervalCategoryDataset) d1.clone();
        }
        catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        
        if(d1 != d2)
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
        
        if(d1.getClass() == d2.getClass())
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
        
        if(d1.equals(d2))
    		System.out.println("PASS");
    	else{
    		pass = false;
    		System.out.println("FAIL");
    	}
    }
}
