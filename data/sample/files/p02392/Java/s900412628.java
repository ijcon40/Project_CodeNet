import java.util.*;

class Main {
	public static void main(String[] args) {
		Scanner stdIn = new Scanner(System.in);
		int a = stdIn.nextInt();
		int b = stdIn.nextInt();
		int c = stdIn.nextInt();
		if (a < b && b < c) {
			System.out.println("Yes");
		}else {
			System.out.println("No");
		}

	}
}