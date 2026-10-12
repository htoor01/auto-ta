// Sample submission: multiply never terminates for negative b (tests the grading timeout).
public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }

    public int multiply(int a, int b) {
        int result = 0;
        for (int i = 0; i != b; i++) {
            result += a;
        }
        return result;
    }

    public int divide(int a, int b) {
        return a / b;
    }
}
