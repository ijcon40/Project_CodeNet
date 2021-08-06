import java.util.*;

public class Main 
{
  	public static void main(String[] args) 
    {
		Scanner sc = new Scanner(System.in);

		int n = sc.nextInt();
		HashSet<String> set = new HashSet<String>();

		String prev = sc.next();
		set.add(prev);
		for(int i = 1; i < n; i++){
			String tmp = sc.next();

			if(set.contains(tmp) || prev.charAt(prev.length() - 1) != tmp.charAt(0)){
				System.out.println("No");
				return;
			}
			set.add(tmp);
			prev = tmp;
		}
	System.out.println("Yes");
	}
}
