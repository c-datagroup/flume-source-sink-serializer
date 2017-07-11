package com.adaltas.flume.serialization;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by root on 7/11/17.
 */
public class TestJsonToMap {
    @Test
    public void testJsonStringToMap(){
        final String data = "{\"id\": \"1\", \"value\": \"val\", \"key\": \"key1\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> result = new HashMap<String, String>();
        try {
            result = objectMapper.readValue(data, Map.class);
            Assert.assertEquals(result.size(), 3);
            Assert.assertEquals(result.get("id"), "1");
            Assert.assertEquals(result.get("value"), "val");
            Assert.assertEquals(result.get("key"), "key1");
        }
        catch(Exception exp){
            Assert.assertEquals(false, true);
        }
    }

    @Test
    public void testJsonBytesToMap(){
        final String data = "{\"id\": \"1\", \"value\": \"val\", \"key\": \"key1\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> result = new HashMap<String, String>();
        try {
            result = objectMapper.readValue(data.getBytes(), Map.class);
            Assert.assertEquals(result.size(), 3);
            Assert.assertEquals(result.get("id"), "1");
            Assert.assertEquals(result.get("value"), "val");
            Assert.assertEquals(result.get("key"), "key1");
        }
        catch(Exception exp){
            Assert.assertEquals(false, true);
        }
    }
}
