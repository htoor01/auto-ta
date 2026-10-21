import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Instructor tests for the Calculator assignment. Each test is worth the same share of the grade. */
class CalculatorTest {

    private final Calculator calculator = new Calculator();

    @Test
    @DisplayName("add: positive numbers")
    void addsPositiveNumbers() {
        assertEquals(5, calculator.add(2, 3));
    }

    @Test
    @DisplayName("add: negative numbers")
    void addsNegativeNumbers() {
        assertEquals(-5, calculator.add(-2, -3));
    }

    @Test
    @DisplayName("subtract: order matters")
    void subtractsInOrder() {
        assertEquals(6, calculator.subtract(10, 4));
    }

    @Test
    @DisplayName("multiply: positive numbers")
    void multipliesPositiveNumbers() {
        assertEquals(12, calculator.multiply(3, 4));
    }

    @Test
    @DisplayName("multiply: negative multiplier")
    void multipliesByNegativeNumber() {
        assertEquals(-12, calculator.multiply(3, -4));
    }

    @Test
    @DisplayName("divide: whole result")
    void dividesEvenly() {
        assertEquals(4, calculator.divide(12, 3));
    }

    @Test
    @DisplayName("divide: by zero throws ArithmeticException")
    void divideByZeroThrows() {
        assertThrows(ArithmeticException.class, () -> calculator.divide(1, 0));
    }
}
