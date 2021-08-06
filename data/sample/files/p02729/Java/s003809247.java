import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Built using CHelper plug-in
 * Actual solution is at the top
 *
 * @author Akash
 */
public class Main {
    public static void main(String[] args) {
        InputStream inputStream = System.in;
        OutputStream outputStream = System.out;
        Scanner in = new Scanner(inputStream);
        PrintWriter out = new PrintWriter(outputStream);
        ATheNumberOfEvenPairs solver = new ATheNumberOfEvenPairs();
        solver.solve(1, in, out);
        out.close();
    }

    static class ATheNumberOfEvenPairs {
        public void solve(int testNumber, Scanner in, PrintWriter out) {
            int n = in.nextInt();
            int m = in.nextInt();
            int result = (n * (n - 1) / 2) + (m * (m - 1) / 2);
            out.println(result);
        }

    }
}

