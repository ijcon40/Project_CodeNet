import java.util.*;

class Main{
    public static void main(String args[]) {
        Scanner sc = new Scanner(System.in);
        int a1 = sc.nextInt();
        int a2 = sc.nextInt();
        int a3 = sc.nextInt();
        int sum = a1 + a2 + a3;
        String res = "win";
        if(sum>21){
            res = "bust";
        }
        System.out.println(res);
    }
}
