import java.util.*;
public class Main {

    public static void main(String[] args) {
        Scanner scan=new Scanner(System.in);

        String s=scan.next();
        boolean app[]=new boolean[26];
        for(int i=0;i<s.length();i++){
            app[s.charAt(i)-'a']=true;
        }
        char ans;

        for(int i=0;i<26;i++){
            if(!app[i]){
                System.out.println((char)(i+'a'));
                break;

            }
            if(i==25){
                System.out.println("None");
            }
        }

    }
}