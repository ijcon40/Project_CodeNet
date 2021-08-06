abstract class Lcounter {
    
    abstract int inc();
    
    static void count(counter c) {
        if (c.inc() < 5)
            count(c);
    }
    
    public static void main(String... args) {
        counter c = new counter() {
                private int total = 0;
                
                int inc() {
                  return total++;
                }
        };
        
        count(c);
    }
}
