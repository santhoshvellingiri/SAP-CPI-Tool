package com.sanv.scpi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
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

	public static void writeFile(File file, Object obj) throws Exception {

		// create the GSON Object
		File parent = file.getParentFile();
		if (parent != null)
			parent.mkdirs();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		FileOutputStream outputStream = new FileOutputStream(file);
		byte[] strToBytes = gson.toJson(obj).getBytes();
		outputStream.write(strToBytes);
		outputStream.close();

	}

	public static void writeFile(ZipInputStream zipIn, String folderName) throws Exception {
		ZipEntry zipEntry = zipIn.getNextEntry();
		while (zipEntry != null) {
			String name = zipEntry.getName();
			File file = new File(folderName, name);
			if (name.endsWith("/")) {
				file.mkdirs();
				continue;
			}
			File parent = file.getParentFile();
			if (parent != null)
				parent.mkdirs();
			InputStream is = convertZipInputStreamToInputStream(zipIn);
			FileOutputStream fos = new FileOutputStream(file);
			byte[] bytes = new byte[1024];
			int length;
			while ((length = is.read(bytes)) >= 0)
				fos.write(bytes, 0, length);
			is.close();
			fos.close();
			zipIn.closeEntry();
			zipEntry = zipIn.getNextEntry();
		}
	}

	public static void writePackageFile(String env, String packageName, String packageID, HttpResponse packageZip)
			throws Exception {
		String folderName = String.valueOf(env) + "/Package/" + packageName + "/";
		File file = new File(folderName, String.valueOf(packageID) + ".zip");
		File parent = file.getParentFile();
		if (parent != null)
			parent.mkdirs();
		InputStream is = packageZip.getEntity().getContent();
		FileOutputStream fos = new FileOutputStream(file);
		byte[] bytes = new byte[1024];
		int length;
		while ((length = is.read(bytes)) >= 0)
			fos.write(bytes, 0, length);
		is.close();
		fos.close();
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
