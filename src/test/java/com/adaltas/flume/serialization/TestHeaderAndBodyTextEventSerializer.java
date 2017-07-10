/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.adaltas.flume.serialization;

import com.google.common.base.Charsets;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.flume.Context;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.serialization.EventSerializer;
import org.apache.flume.serialization.EventSerializerFactory;
import org.junit.Assert;
import org.junit.Test;

public class TestHeaderAndBodyTextEventSerializer {

	static File testFile = new File("src/test/resources/events.txt");
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
		serializer.write(EventBuilder.withBody("event 1" + (withNewline ? "\n" : ""), Charsets.UTF_8, headers));
		serializer.write(EventBuilder.withBody("event 2" + (withNewline ? "\n" : ""), Charsets.UTF_8, headers));
		serializer.write(EventBuilder.withBody("event 3" + (withNewline ? "\n" : ""), Charsets.UTF_8, headers));
		
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
	public void testWithNewline() throws FileNotFoundException, IOException {
		serializeWithContext(new Context(), false, 2, null);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(storedOutput));
		Assert.assertEquals("{header2=value2, header1=value1} event 1", reader.readLine());
		Assert.assertEquals("{header2=value2, header1=value1} event 2", reader.readLine());
		Assert.assertEquals("{header2=value2, header1=value1} event 3", reader.readLine());
		Assert.assertNull(reader.readLine());
		reader.close();
	}

	@Test
	public void testNoNewline() throws FileNotFoundException, IOException {
		Context context = new Context();
		context.put("appendNewline", "false");
		serializeWithContext(context, true, 2, null);
	
		BufferedReader reader = new BufferedReader(new InputStreamReader(storedOutput));
		Assert.assertEquals("{header2=value2, header1=value1} event 1", reader.readLine());
		Assert.assertEquals("{header2=value2, header1=value1} event 2", reader.readLine());
		Assert.assertEquals("{header2=value2, header1=value1} event 3", reader.readLine());
		Assert.assertNull(reader.readLine());
		reader.close();
	}

	@Test
	public void testCSV() throws FileNotFoundException, IOException {
		Context context = new Context();
		context.put("format", "CSV");
		serializeWithContext(context, false, 2, null);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(storedOutput));
		Assert.assertEquals("\"value2\",\"value1\",\"event 1\"", reader.readLine());
		Assert.assertEquals("\"value2\",\"value1\",\"event 2\"", reader.readLine());
		Assert.assertEquals("\"value2\",\"value1\",\"event 3\"", reader.readLine());
		Assert.assertNull(reader.readLine());
		reader.close();
	}

	@Test
	public void testCSVAndColumns() throws FileNotFoundException, IOException {
		Context context = new Context();
		context.put("format", "CSV");
		context.put("columns", "header3 header2");
		serializeWithContext(context, false, 3, null);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(storedOutput));
		Assert.assertEquals("\"value3\",\"value2\",\"event 1\"", reader.readLine());
		Assert.assertEquals("\"value3\",\"value2\",\"event 2\"", reader.readLine());
		Assert.assertEquals("\"value3\",\"value2\",\"event 3\"", reader.readLine());
		Assert.assertNull(reader.readLine());
		reader.close();
	}
	
	@Test
	public void testCSVAndColumnsThatDoNotExistInTheData() throws FileNotFoundException, IOException {
		Context context = new Context();
		context.put("format", "CSV");
		context.put("columns", "header3 header2 header75");
		serializeWithContext(context, false, 3, null);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(storedOutput));
		Assert.assertEquals("\"value3\",\"value2\",\"\",\"event 1\"", reader.readLine());
		Assert.assertEquals("\"value3\",\"value2\",\"\",\"event 2\"", reader.readLine());
		Assert.assertEquals("\"value3\",\"value2\",\"\",\"event 3\"", reader.readLine());
		Assert.assertNull(reader.readLine());
		reader.close();
	}

	@Test
	public void testCSVWithAlternativeDelimiter() throws FileNotFoundException, IOException {
		Context context = new Context();
		context.put("format", "CSV");
		context.put("delimiter", "\t");
		serializeWithContext(context, false, 2, null);
	
		BufferedReader reader = new BufferedReader(new InputStreamReader(storedOutput));
		Assert.assertEquals("\"value2\"\t\"value1\"\t\"event 1\"", reader.readLine());
		Assert.assertEquals("\"value2\"\t\"value1\"\t\"event 2\"", reader.readLine());
		Assert.assertEquals("\"value2\"\t\"value1\"\t\"event 3\"", reader.readLine());
		Assert.assertNull(reader.readLine());
		reader.close();
	}
	
	@Test
	public void testCSVEscapesQuotesInOutput() throws IOException, FileNotFoundException {
		Context context = new Context();
		context.put("format", "CSV");
		context.put("delimiter", "\t");
		serializeWithContext(context, false, 2, "\"yay\"");
	
		BufferedReader reader = new BufferedReader(new InputStreamReader(storedOutput));
		Assert.assertEquals("\"value2\"\t\"value1\"\t\"event 1\"", reader.readLine());
		Assert.assertEquals("\"value2\"\t\"value1\"\t\"event 2\"", reader.readLine());
		Assert.assertEquals("\"value2\"\t\"value1\"\t\"event 3\"", reader.readLine());
		Assert.assertEquals("\"value2\"\t\"value1\"\t\"\"\"yay\"\"\"", reader.readLine());
		Assert.assertNull(reader.readLine());
		reader.close();	
	}

}
