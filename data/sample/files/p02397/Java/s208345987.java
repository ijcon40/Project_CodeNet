import java.util.Scanner;
	class Main{

		public static void main(String[] args){
	Scanner sc=new Scanner(System.in);
int x;
int y;
int hantei =9;
for(;hantei!=0;){
	x=sc.nextInt();
	y=sc.nextInt();
	
	if(x==0){
		if(y==0){
		System.exit(0);	
		}
	}
	if(x<=y){
		System.out.println(x + " "+y);
	}else if(y<x){
		System.out.println(y + " "+x);
	}
	
	
}
	
	
	
	}
	}