package common;

public class Debugger {

	public static void log(Object o){
		if(Constants.DEBUGGING) {
			System.out.println(o.toString());
		}           
	}
	
}
