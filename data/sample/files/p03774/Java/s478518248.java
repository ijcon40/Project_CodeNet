
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		new Main().solve();
	}
	
	int N,M;
	
	void solve(){
		Scanner sc=new Scanner(System.in);
		N=sc.nextInt();
		M=sc.nextInt();
		
		int[]a=new int[N];
		int[]b=new int[N];
		int[]c=new int[M];
		int[]d=new int[M];
		
		for(int i=0;i<N;i++){
			a[i]=sc.nextInt();
			b[i]=sc.nextInt();
		}
		for(int i=0;i<M;i++){
			c[i]=sc.nextInt();
			d[i]=sc.nextInt();
		}
		
		for(int i=0;i<N;i++){
			int dis=Integer.MAX_VALUE/2;
			int min=0;
			
			for(int j=0;j<M;j++){
				int man_dis=Math.abs(a[i]-c[j])+Math.abs(b[i]-d[j]);
				if(man_dis<dis){
					dis=man_dis;
					min=j;
				}
			}
			System.out.println(min+1);
		}
	}
}