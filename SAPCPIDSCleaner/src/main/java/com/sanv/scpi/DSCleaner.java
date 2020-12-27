package com.sanv.scpi;

import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanv.scpi.model.dsObj;
import com.sanv.scpi.model.tenantConfiguration;

public class DSCleaner {

	static tenantConfiguration src;
	static CookieStore cookieStore = null;
	static Header[] xcsrf = null;

	public static void main(String[] args) throws Exception {

		long startTime = System.nanoTime();

		System.out.println("INFO  : Tool Version " + DSCleaner.class.getPackage().getImplementationVersion());

		String cDir = System.getProperty("user.dir") + "/config.json";
		File currentDir = new File(cDir);
		FileReader fileReader = new FileReader(currentDir);
		Gson gson = new Gson();
		tenantConfiguration[] tc = (tenantConfiguration[]) gson.fromJson(fileReader, tenantConfiguration[].class);

		DefaultParser defaultParser = new DefaultParser();

		Options option = new Options();
		option.addOption("from", true, "Extraction Mode");
		option.addOption("DataStoreName", true, "Name of the DataStore");
		option.addOption("iFlowName", true, "IFlow Name");
		option.addOption("dateC", true, "ON or Before Date");
		option.addOption("s_password", true, "Password");

		try {

			CommandLine comlin = defaultParser.parse(option, args);

			String from = comlin.getOptionValue("from");
			String dsName = comlin.getOptionValue("DataStoreName");
			String ifName = comlin.getOptionValue("iFlowName");
			String dateCheck = comlin.getOptionValue("dateC");
			String s_pass = comlin.getOptionValue("s_password");

			src = getTenant(from, tc);
			src.setPassword(s_pass);

			if (src == null)
				throw new Exception("Incorrect Source ID");
			if (src.getPassword() == null) {
				Console console = System.console();
				s_pass = new String(
						console.readPassword("Enter Password of User " + src.getUser() + " : ", new Object[0]));
				src.setPassword(s_pass);
			}

			if (isNullOrEmptyCheck(dsName)) {
				throw new MissingArgumentException("Provide fileName arguments to execute the operation");
			}
			if (isNullOrEmptyCheck(dateCheck)) {
				dateCheck = "01/01/1990";
			}

			System.out.println("START : Deletion");

			boolean hasmoreRecord = false;

			int i = 0;
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm:ss a");
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

			do {

				String apiUri = "";
				String completeURL = "";
				apiUri = "/itspaces/Operations/com.sap.esb.monitoring.datastore.access.command.ListDataStoreEntriesCommand?";
				apiUri += "storeName=" + dsName;
				apiUri += "&maxNum=100000&onlyOverdue=true";
				if (!isNullOrEmptyCheck(ifName)) {
					apiUri += "&qualifier=" + ifName;
				}
				completeURL = "https://" + src.getTMNHost() + apiUri;

				HttpResponse dsEntries = doGet(completeURL, src);

				if (xcsrf == null)
					xcsrf = dsEntries.getHeaders("X-CSRF-Token");

				String dsEntryBody = EntityUtils.toString(dsEntries.getEntity(), "UTF-8");
				JsonObject result = JsonParser.parseString(dsEntryBody).getAsJsonObject();
				JsonArray entryArr = result.get("entries").getAsJsonArray();
				hasmoreRecord = result.get("moreData").getAsBoolean();

				if (entryArr.size() > 0) {
					List<String> ids = new ArrayList<String>();
					for (JsonElement packageJE : entryArr) {
						JsonObject packageJOB = packageJE.getAsJsonObject();
						String writeDate = packageJOB.get("writeDate").getAsString();
						Date writeparsedDate = dateFormat.parse(writeDate);
						Date tocheck = formatter.parse(dateCheck);
						if (writeparsedDate.compareTo(tocheck) < 0) {
							ids.add(packageJOB.get("id").getAsString());
							i++;
						}
					}
					
					System.out.println("Log   : Fetched " + ids.size() + " Entries ");

					
					batches(ids, 1000).parallel().forEach(obj -> {
						
						dsObj ds = new dsObj();
						ds.setStoreName(dsName);
						ds.setIDs(obj);

						if (!isNullOrEmptyCheck(ifName)) {
							ds.setqualifier(ifName);
						}
						Gson gson1 = new Gson();
						gson1 = new GsonBuilder().setPrettyPrinting().create();
						
						String apiUri1 = "/itspaces/Operations/com.sap.esb.monitoring.datastore.access.command.DeleteDataStoreEntryCommand";
						String completeURL1 = "https://" + src.getTMNHost() + apiUri1;		
						
						
						if (obj.size() > 0) {							

							HttpClientContext context = new HttpClientContext();
							CloseableHttpClient client = HttpClients.createDefault();
							HttpPost httpPost = new HttpPost(completeURL1);
							context.setCookieStore(cookieStore);
							httpPost.setHeaders(xcsrf);
							String JSON_STRING = gson1.toJson(ds);
							HttpEntity stringEntity = new StringEntity(JSON_STRING, ContentType.APPLICATION_JSON);
							httpPost.setEntity(stringEntity);
							CloseableHttpResponse response = null;
							try {
								response = client.execute(httpPost, context);
							} catch (ClientProtocolException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}

							// System.out.println(EntityUtils.toString(response.getEntity(), "UTF-8"));

							if (response.getStatusLine().getStatusCode() != 200) {
								System.out.println("ERROR : Delete Error");
								return;
							}

							System.out.println("Log   : Deleted " + obj.size());
						} else {
							return;
						}
						
					});			 
					/*
					dsObj ds = new dsObj();
					ds.setStoreName(dsName);
					ds.setIDs(ids);

					if (!isNullOrEmptyCheck(ifName)) {
						ds.setqualifier(ifName);
					}
					Gson gson1 = new GsonBuilder().setPrettyPrinting().create();

					System.out.println("Log   : Fetched " + ids.size() + " Entries ");

					if (ids.size() > 0) {

						apiUri = "/itspaces/Operations/com.sap.esb.monitoring.datastore.access.command.DeleteDataStoreEntryCommand";
						completeURL = "https://" + src.getTMNHost() + apiUri;

						HttpClientContext context = new HttpClientContext();
						CloseableHttpClient client = HttpClients.createDefault();
						HttpPost httpPost = new HttpPost(completeURL);
						context.setCookieStore(cookieStore);
						httpPost.setHeaders(xcsrf);
						String JSON_STRING = gson1.toJson(ds);
						HttpEntity stringEntity = new StringEntity(JSON_STRING, ContentType.APPLICATION_JSON);
						httpPost.setEntity(stringEntity);
						CloseableHttpResponse response = client.execute(httpPost, context);

						// System.out.println(EntityUtils.toString(response.getEntity(), "UTF-8"));

						if (response.getStatusLine().getStatusCode() != 200) {
							System.out.println("ERROR : Delete Error");
							break;
						}
						System.out.println("Log   : Deleted " + ids.size() + " Entries - Total : " + i);
					} else {
						break;
					}*/
					System.out.println("Log   : Deleted " + ids.size() + " Entries - Total : " + i);
				}

			} while (hasmoreRecord);

			System.out.println("END   : Deletion");

			long endTime = System.nanoTime();
			long totalTime = endTime - startTime;
			StringBuilder executionInfo = new StringBuilder();
			System.out.println("INFO  : Extraction Time " + totalTime / 1000000 + " Milli Seconds");
			executionInfo.append("INFO  : Extraction Time ");
			executionInfo.append(totalTime / 1000000000);
			executionInfo.append(" Seconds");
			System.out.println(executionInfo.toString());
			System.out.println("Author : Santhosh Vellingiri");
			System.out.println("Follow : https://www.linkedin.com/in/santhoshkumarvellingiri/");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static tenantConfiguration getTenant(String name, tenantConfiguration[] tc) throws Exception {

		for (tenantConfiguration tc2 : tc) {
			if (tc2.getId().equals(name))
				return tc2;
		}
		return null;
	}

	public static boolean isNullOrEmptyCheck(String parameter) {

		// Function Return true if the parameter is null of empty else false
		if (parameter != null && !parameter.isEmpty()) {
			return false;
		}

		return true;
	}

	public static HttpResponse doGet(String url, tenantConfiguration tenant) throws Exception {
		CloseableHttpResponse closeableHttpResponse = null;

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("ACCEPT", "application/json");
		if (xcsrf == null) {
			headers.put("x-csrf-token", "fetch");
		}

		try {
			CloseableHttpClient client = HttpClientBuilder.create().build();
			String encoding = Base64.getEncoder()
					.encodeToString((tenant.getUser() + ":" + tenant.getPassword()).getBytes("UTF-8"));

			RequestBuilder requestbuilder = RequestBuilder.get().setUri(url).setHeader("Authorization",
					"Basic " + encoding);

			for (Map.Entry<String, String> header : headers.entrySet()) {
				requestbuilder.setHeader((String) header.getKey(), (String) header.getValue());
			}
			HttpClientContext context = new HttpClientContext();
			HttpUriRequest request = requestbuilder.build();
			closeableHttpResponse = client.execute(request, context);
			if (cookieStore == null)
				cookieStore = context.getCookieStore();

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

	public static <T> Stream<List<T>> batches(List<T> source, int length) {
		if (length <= 0)
			throw new IllegalArgumentException("length = " + length);
		int size = source.size();
		if (size <= 0)
			return Stream.empty();
		int fullChunks = (size - 1) / length;
		return IntStream.range(0, fullChunks + 1)
				.mapToObj(n -> source.subList(n * length, n == fullChunks ? size : (n + 1) * length));
	}
}
