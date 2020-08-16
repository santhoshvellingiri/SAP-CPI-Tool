package com.sanv.scpi;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
import org.w3c.dom.Document;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanv.scpi.model.scpiConfig;

public class DataStoreCleaner {
	static scpiConfig ctx;
	static CookieStore cookieStore = null;
	static Header[] xcsrf = null;

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {

		long startTime = System.nanoTime();

		DefaultParser defaultParser = new DefaultParser();

		Options option = new Options();
		option.addOption("SCPI_Host", true, "SAP CPI Host");
		option.addOption("user", true, "Username");
		option.addOption("password", true, "Password");
		option.addOption("DataStoreName", true, "Name of the DataStore");
		option.addOption("iFlowName", true, "Extraction Mode");
		option.addOption("dateC", true, "Resolve Extraction Variables Y/N");

		try {

			CommandLine comlin = defaultParser.parse(option, args);

			String host = comlin.getOptionValue("SCPI_Host");
			String user = comlin.getOptionValue("user");
			String pass = comlin.getOptionValue("password");
			String dsName = comlin.getOptionValue("DataStoreName");
			String ifName = comlin.getOptionValue("iFlowName");
			String dateCheck = comlin.getOptionValue("dateC");

			if (pass == null) {
				java.io.Console console = System.console();
				pass = new String(console.readPassword("Enter Password of User " + user + " : "));
			}

			if (isNullOrEmptyCheck(host)) {
				throw new MissingArgumentException("Provide SCPI_Host arguments to execute the operation");
			}

			if (isNullOrEmptyCheck(user)) {
				throw new MissingArgumentException("Provide user arguments to execute the operation");
			}

			if (isNullOrEmptyCheck(pass)) {
				throw new MissingArgumentException("Provide password arguments to execute the operation");
			}

			if (isNullOrEmptyCheck(dsName)) {
				throw new MissingArgumentException("Provide fileName arguments to execute the operation");
			}
			if (isNullOrEmptyCheck(dateCheck)) {
				dateCheck = "01/01/1990";
			}

			ctx = new scpiConfig();
			ctx.setSCPIHost(host);
			ctx.setUser(user);
			ctx.setPassword(pass);

			System.out.println("START : Deletion");

			boolean hasmoreRecord = false;
			
			int i=0;
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
				completeURL = "https://" + ctx.getSCPIHost() + apiUri;

				HttpResponse dsEntries = doGet(completeURL);

				if(xcsrf == null)
					xcsrf = dsEntries.getHeaders("X-CSRF-Token");

				String dsEntryBody = EntityUtils.toString(dsEntries.getEntity(), "UTF-8");
				JsonObject result = (new JsonParser()).parse(dsEntryBody).getAsJsonObject();
				JsonArray entryArr = result.get("entries").getAsJsonArray();
				hasmoreRecord = result.get("moreData").getAsBoolean();

				if (entryArr.size()>0) {
					ArrayList<String> ids = new ArrayList<String>();
					for (JsonElement packageJE : entryArr) {						
						JsonObject packageJOB = packageJE.getAsJsonObject();
						String overdueDate = packageJOB.get("overdueDate").getAsString();						
					    Date parsedDate = dateFormat.parse(overdueDate);
					    Date tocheck = formatter.parse(dateCheck);
					    if(parsedDate.compareTo(tocheck) < 0) {
					    	ids.add(packageJOB.get("id").getAsString());
					    	i++;
					    }						
					}

					dsObj ds = new dsObj();
					ds.setStoreName(dsName);
					ds.setIDs(ids);
					
					if (!isNullOrEmptyCheck(ifName)) {
						ds.setqualifier(ifName);
					}
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					
					System.out.println("Log   : Fetched "+ ids.size() + " Entries ");

					if(ids.size()>0) {
						
						apiUri = "/itspaces/Operations/com.sap.esb.monitoring.datastore.access.command.DeleteDataStoreEntryCommand";
						completeURL = "https://" + ctx.getSCPIHost() + apiUri;
						
						HttpClientContext context = new HttpClientContext();
						CloseableHttpClient client = HttpClients.createDefault();
						HttpPost httpPost = new HttpPost(completeURL);
						context.setCookieStore(cookieStore);
						httpPost.setHeaders(xcsrf);
						String JSON_STRING = gson.toJson(ds);
						HttpEntity stringEntity = new StringEntity(JSON_STRING, ContentType.APPLICATION_JSON);
						httpPost.setEntity(stringEntity);
						CloseableHttpResponse response = client.execute(httpPost, context);
						
						//System.out.println(EntityUtils.toString(response.getEntity(), "UTF-8"));
						
						if (response.getStatusLine().getStatusCode() != 200) {
							System.out.println("ERROR : Delete Error");
							break;
						}
						
						System.out.println("Log   : Deleted "+ ids.size() + " Entries - Total : " + i);						
					}
					else {
						break;
					}
					
				}
				

			} while (hasmoreRecord);

			System.out.println("END   : Deletion");

			long endTime = System.nanoTime();
			long totalTime = endTime - startTime;
			StringBuilder executionInfo = new StringBuilder();
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

	public static boolean isNullOrEmptyCheck(String parameter) {

		// Function Return true if the parameter is null of empty else false
		if (parameter != null && !parameter.isEmpty()) {
			return false;
		}

		return true;
	}

	public static HttpResponse doGet(String url) throws Exception {
		CloseableHttpResponse closeableHttpResponse = null;

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("ACCEPT", "application/json");
		if (xcsrf == null) {
			headers.put("x-csrf-token", "fetch");
		}

		try {
			CloseableHttpClient client = HttpClientBuilder.create().build();
			String encoding = Base64.getEncoder()
					.encodeToString((ctx.getUser() + ":" + ctx.getPassword()).getBytes("UTF-8"));

			RequestBuilder requestbuilder = RequestBuilder.get().setUri(url).setHeader("Authorization",
					"Basic " + encoding);

			for (Map.Entry<String, String> header : headers.entrySet()) {
				requestbuilder.setHeader((String) header.getKey(), (String) header.getValue());
			}
			HttpClientContext context = new HttpClientContext();
			HttpUriRequest request = requestbuilder.build();
			closeableHttpResponse = client.execute(request, context);
			if(cookieStore == null)
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

	public static Document processxml(InputStream inpStream) throws Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(inpStream);
		return document;
	}

}