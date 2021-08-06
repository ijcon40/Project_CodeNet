import java.util.*;

public class Main {
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int height = sc.nextInt();
    int width = sc.nextInt();

    int area = calculateArea(height, width);
    int circumference = calculateCircumference(height, width);
    System.out.println(String.format("%d %d", area, circumference));
  }

  /**
  * ??¢???????±???????????????????
  * @param height ??????
  * @param width ???
  * @return ??¢???
  */
  private static int calculateArea(int height, int width) {
    return height * width;
  }

  /**
  * ??¨?????????????±???????????????????
  * @param height ??????
  * @param width ???
  * @return ??¨?????????
  */
  private static int calculateCircumference(int height, int width) {
    return (height + width) * 2;
  }
}