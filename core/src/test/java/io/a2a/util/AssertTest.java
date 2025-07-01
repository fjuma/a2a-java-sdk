package io.a2a.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * Tests for Assert utility class
 */
class AssertTest {

    @Test
    void testCheckNotNullParam_WithValidValue() {
        // Test normal case
        String testValue = "test";
        String result = Assert.checkNotNullParam("testParam", testValue);
        assertEquals(testValue, result);
    }

    @Test
    void testCheckNotNullParam_WithNullValue() {
        // Test that null value throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            Assert.checkNotNullParam("testParam", null);
        });
    }

    @Test
    void testCheckNotNullParam_WithNullName() {
        // Test that null name parameter throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            Assert.checkNotNullParam(null, "testValue");
        });
    }

    @Test
    void testCheckNotNullParam_WithEmptyName() {
        // Test empty string name parameter
        String testValue = "test";
        String result = Assert.checkNotNullParam("", testValue);
        assertEquals(testValue, result);
    }

    @Test
    void testIsNullOrStringOrInteger_WithNull() {
        // Test null value
        assertDoesNotThrow(() -> {
            Assert.isNullOrStringOrInteger(null);
        });
    }

    @Test
    void testIsNullOrStringOrInteger_WithString() {
        // Test String type
        assertDoesNotThrow(() -> {
            Assert.isNullOrStringOrInteger("test");
        });
    }

    @Test
    void testIsNullOrStringOrInteger_WithInteger() {
        // Test Integer type
        assertDoesNotThrow(() -> {
            Assert.isNullOrStringOrInteger(123);
        });
    }

    @Test
    void testIsNullOrStringOrInteger_WithLong() {
        // Test that Long type throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            Assert.isNullOrStringOrInteger(123L);
        });
    }

    @Test
    void testIsNullOrStringOrInteger_WithDouble() {
        // Test that Double type throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            Assert.isNullOrStringOrInteger(123.45);
        });
    }

    @Test
    void testIsNullOrStringOrInteger_WithBoolean() {
        // Test that Boolean type throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            Assert.isNullOrStringOrInteger(true);
        });
    }

    @Test
    void testIsNullOrStringOrInteger_WithObject() {
        // Test that Object type throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            Assert.isNullOrStringOrInteger(new Object());
        });
    }
}
