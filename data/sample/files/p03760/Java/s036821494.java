import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

	public static void main(String[] args) throws IOException {
		BufferedReader input =new BufferedReader (new InputStreamReader (System.in));
		String o = input.readLine( );
		String e = input.readLine( );
		String[] oList = o.split("");
		String[] eList = e.split("");
		int max = Math.min(o.length(), e.length());
		for(int i=0; i<max; i++){
			System.out.print(oList[i]);
			System.out.print(eList[i]);
		}
		if(o.length()>e.length()){
			System.out.println(oList[max]);
		}else{
			System.out.println();
		}
	}
}
