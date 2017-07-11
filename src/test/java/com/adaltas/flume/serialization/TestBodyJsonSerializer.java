package com.adaltas.flume.serialization;

import com.google.common.base.Charsets;
import org.apache.flume.Context;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.serialization.EventSerializer;
import org.apache.flume.serialization.EventSerializerFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by root on 7/11/17.
 */
public class TestBodyJsonSerializer {
    private ByteArrayInputStream storedOutput;

    public void serializeWithContext(Context context, boolean withNewline, int numHeaders, String body) throws IOException {
        ByteArrayOutputStream serializedOutput = new ByteArrayOutputStream();

        Map<String, String> headers = new HashMap<String, String>();
        for(int i = 1; i < numHeaders + 1; i++) {
            headers.put("header" + i, "value" + i);
        }

        EventSerializer serializer =
                EventSerializerFactory.getInstance(
                        "com.adaltas.flume.serialization.HeaderAndBodyTextEventSerializer$Builder",
                        context,
                        serializedOutput
                );
        serializer.afterCreate();

        if(body != null) {
            serializer.write(EventBuilder.withBody(body, Charsets.UTF_8, headers));
        }
        serializer.flush();
        serializer.beforeClose();
        serializedOutput.flush();
        serializedOutput.close();

        storedOutput = new ByteArrayInputStream(serializedOutput.toByteArray());
    }

    @Test
    public void testCSVAndColumns() throws FileNotFoundException, IOException {
        Context context = new Context();
        context.put("format", "CSV");
        context.put("columns", "header3 header2 id value");
        context.put("jsonBody", "true");
        context.put("delimiter", ",");

        String body = "{\"id\": \"1\", \"value\": \"value1\"}";
        serializeWithContext(context, false, 3, body);

        BufferedReader reader = new BufferedReader(new InputStreamReader(storedOutput));
        Assert.assertEquals("\"value3\",\"value2\",\"1\",\"value1\"", reader.readLine());
        Assert.assertNull(reader.readLine());
        reader.close();
    }
}
