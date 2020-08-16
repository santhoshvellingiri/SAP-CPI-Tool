package com.sanv.scpi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sanv.scpi.model.tenantConfiguration;

public class Utility {

	public static HttpResponse doGet(String url, tenantConfiguration tenant) throws Exception {
		CloseableHttpResponse closeableHttpResponse = null;

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("ACCEPT", "application/json");

		try {
			CloseableHttpClient client = HttpClientBuilder.create().build();
			String encoding = Base64.getEncoder()
					.encodeToString((tenant.getUser() + ":" + tenant.getPassword()).getBytes("UTF-8"));

			RequestBuilder requestbuilder = RequestBuilder.get().setUri(url).setHeader("Authorization",
					"Basic " + encoding);

			for (Map.Entry<String, String> header : headers.entrySet()) {
				requestbuilder.setHeader((String) header.getKey(), (String) header.getValue());
			}
			HttpUriRequest request = requestbuilder.build();
			closeableHttpResponse = client.execute(request);

			if (closeableHttpResponse.getStatusLine().getStatusCode() == 401) {
				System.out.println("ERROR : Cannot Connect");
				System.out.println("INFO  : Check Host / User Credential / User Authorization");
				throw new Exception("UNABLE_TO_CONNECT");
			}

		} catch (IOException e) {
			throw e;
		}
		return closeableHttpResponse;
	}

	public static void writeFile(String fileName, Object obj) throws Exception {

		// create the gson object
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		FileOutputStream outputStream = new FileOutputStream(fileName);
		byte[] strToBytes = gson.toJson(obj).getBytes();
		outputStream.write(strToBytes);
		outputStream.close();

	}

	public static InputStream convertZipInputStreamToInputStream(ZipInputStream in) throws IOException {
		final int BUFFER = 1024;
		int count = 0;
		byte data[] = new byte[BUFFER];

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		while ((count = in.read(data, 0, BUFFER)) != -1) {
			out.write(data, 0, count);
		}
		InputStream is = new ByteArrayInputStream(out.toByteArray());
		out.close();
		return is;
	}
	
	public static Document processxml(InputStream inpStream) throws Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(inpStream);
		return document;
	}

}
