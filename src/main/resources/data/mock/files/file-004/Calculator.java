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
        int count = 0;
        while (count != b) {
            result += a;
            if (b > 0) {
                count++;
            }
        }
        return result;
    }

    public int divide(int a, int b) {
        return a / b;
    }
}
