import java.util.*;
public class Main {
	public static void main(String[] args){
		Scanner sc = new Scanner(System.in);
		int a = sc.nextInt();
		int b = sc.nextInt();

		// 出力
		System.out.println( a % 2 == 1 && b % 2 == 1 ? "Odd" : "Even");
	}
}