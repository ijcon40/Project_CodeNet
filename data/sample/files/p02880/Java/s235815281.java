import java.io.*; 
import java.math.*;
import java.util.*;
 
public class Main {
	//static final long MOD = 998244353;
	static final long MOD = 1000000007;
	static final int INF = 1000000007;
	public static void main(String[] args) {
		FastScanner sc = new FastScanner();
		int N = sc.ni();
		for (int a = 1; a < 10; a++) {
			for (int b = 1; b < 10; b++) {
				if (a*b==N) {
					System.out.println("Yes");
					return;
				}
			}
		}
		System.out.println("No");
	}
    
    public static long power(long x, long y, long p) { 
        // Initialize result
        long res = 1;
         
        // Update x if it is more   
        // than or equal to p 
        x = x % p;  
      
        while (y > 0) 
        { 
            // If y is odd, multiply x 
            // with result 
            if((y & 1)==1) 
                res = (res * x) % p; 
      
            // y must be even now 
            // y = y / 2 
            y = y >> 1;  
            x = (x * x) % p;  
        } 
        return res; 
    }
    
    public static long dist(int[] point, int[] point2) {
    	return (long)(Math.pow((point2[1]-point[1]),2)+Math.pow((point2[0]-point[0]),2));
    }
    
    public static long gcd(long a, long b) {
    	if (b == 0)
    		return a;
    	else
    		return gcd(b,a%b);
    }
    
    public static int[][] sort(int[][] array) {
    	//Sort an array (immune to quicksort TLE)
		Random rgen = new Random();
		for (int i = 0; i < array.length; i++) {
		    int randomPosition = rgen.nextInt(array.length);
		    int[] temp = array[i];
		    array[i] = array[randomPosition];
		    array[randomPosition] = temp;
		}
		Arrays.sort(array, new Comparator<int[]>() {
			  @Override
        	  public int compare(int[] arr1, int[] arr2) {
				  return arr2[1]-arr1[1];
	          }
		});
		return array;
	}
    
    static class FastScanner { 
        BufferedReader br; 
        StringTokenizer st; 
  
        public FastScanner() { 
            br = new BufferedReader(new InputStreamReader(System.in)); 
        } 
  
        String next() { 
            while (st == null || !st.hasMoreElements()) { 
                try { 
                    st = new StringTokenizer(br.readLine());
                } catch (IOException  e) { 
                    e.printStackTrace(); 
                } 
            } 
            return st.nextToken(); 
        } 
  
        int ni() { 
            return Integer.parseInt(next()); 
        } 
  
        long nl() { 
            return Long.parseLong(next()); 
        } 
  
        double nd() { 
            return Double.parseDouble(next()); 
        } 
  
        String nextLine() { 
            String str = ""; 
            try { 
                str = br.readLine(); 
            } catch (IOException e) {
                e.printStackTrace(); 
            } 
            return str; 
        }
    }
}