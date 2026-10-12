// Sample submission: compiles, but subtract and divide are wrong (partial credit).
public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return b - a;
    }

    public int multiply(int a, int b) {
        return a * b;
    }

    public int divide(int a, int b) {
        return b == 0 ? 0 : a / b;
    }
}
