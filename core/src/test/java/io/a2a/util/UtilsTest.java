package io.a2a.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Tests for Utils utility class
 */
class UtilsTest {

    @Test
    void testDefaultIfNull_WithNullValue() {
        // Test that null value returns default value
        String result = Utils.defaultIfNull(null, "default");
        assertEquals("default", result);
    }

    @Test
    void testDefaultIfNull_WithNonNullValue() {
        // Test that non-null value returns original value
        String value = "test";
        String result = Utils.defaultIfNull(value, "default");
        assertEquals(value, result);
    }

    @Test
    void testDefaultIfNull_WithNullDefault() {
        // Test case where default value is null
        String value = "test";
        String result = Utils.defaultIfNull(value, null);
        assertEquals(value, result);
    }

    @Test
    void testDefaultIfNull_WithBothNull() {
        // Test case where both value and default are null
        String result = Utils.defaultIfNull(null, null);
        assertNull(result);
    }

    @Test
    void testDefaultIfNull_WithDifferentTypes() {
        // Test with different types
        Integer intValue = 123;
        Integer result = Utils.defaultIfNull(intValue, 456);
        assertEquals(intValue, result);
    }

    @Test
    void testUnmarshalFrom_WithValidJson() throws Exception {
        // Test with valid JSON string
        String json = "{\"name\":\"test\",\"value\":123}";
        TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
        
        Map<String, Object> result = Utils.unmarshalFrom(json, typeRef);
        
        assertNotNull(result);
        assertEquals("test", result.get("name"));
        assertEquals(123, result.get("value"));
    }

    @Test
    void testUnmarshalFrom_WithArrayJson() throws Exception {
        // Test with array JSON
        String json = "[\"item1\",\"item2\",\"item3\"]";
        TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {};
        
        List<String> result = Utils.unmarshalFrom(json, typeRef);
        
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("item1", result.get(0));
        assertEquals("item2", result.get(1));
        assertEquals("item3", result.get(2));
    }

    @Test
    void testUnmarshalFrom_WithInvalidJson() {
        // Test with invalid JSON string
        String invalidJson = "{invalid json}";
        TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
        
        assertThrows(Exception.class, () -> {
            Utils.unmarshalFrom(invalidJson, typeRef);
        });
    }

    @Test
    void testUnmarshalFrom_WithEmptyJson() throws Exception {
        // Test with empty JSON object
        String json = "{}";
        TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
        
        Map<String, Object> result = Utils.unmarshalFrom(json, typeRef);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRethrow_WithRuntimeException() {
        // Test rethrowing RuntimeException
        RuntimeException originalException = new RuntimeException("test exception");
        
        assertThrows(RuntimeException.class, () -> {
            Utils.rethrow(originalException);
        });
    }

    @Test
    void testRethrow_WithCheckedException() {
        // Test rethrowing checked exception
        Exception originalException = new Exception("test checked exception");
        
        assertThrows(Exception.class, () -> {
            Utils.rethrow(originalException);
        });
    }

    @Test
    void testRethrow_WithCustomException() {
        // Test rethrowing custom exception
        IllegalArgumentException originalException = new IllegalArgumentException("test illegal argument");
        
        assertThrows(IllegalArgumentException.class, () -> {
            Utils.rethrow(originalException);
        });
    }

    @Test
    void testObjectMapper_IsNotNull() {
        // Test that OBJECT_MAPPER is not null
        assertNotNull(Utils.OBJECT_MAPPER);
    }

    @Test
    void testObjectMapper_CanSerializeAndDeserialize() throws Exception {
        // Test that OBJECT_MAPPER can serialize and deserialize properly
        TestObject original = new TestObject("test", 123);
        
        String json = Utils.OBJECT_MAPPER.writeValueAsString(original);
        TestObject deserialized = Utils.OBJECT_MAPPER.readValue(json, TestObject.class);
        
        assertEquals(original.name, deserialized.name);
        assertEquals(original.value, deserialized.value);
    }

    // Simple class for testing
    static class TestObject {
        public String name;
        public int value;

        public TestObject() {}

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
