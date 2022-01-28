
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.*;
import org.apache.commons.math.analysis.solvers.BisectionSolver;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolver;



public class Main {
	public static boolean pass = false;
	public static void main(String[] args) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException{
		
		UnivariateRealFunction f = new SinFunction();
		UnivariateRealSolver solver = new BisectionSolver();
    
		if(Math.abs(Math.PI-solver.solve(f, 3.0, 3.2, 3.1))<=solver.getAbsoluteAccuracy()){
			pass = true;
			System.out.println("pass");
		}
	}
}
