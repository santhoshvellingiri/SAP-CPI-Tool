package com.sanv.scpi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sanv.scpi.model.scpiConfig;
import com.sanv.scpi.model.scpiIFLOW;
import com.sanv.scpi.model.scpiPackage;
import com.sanv.scpi.model.scpiResult;

public class scpiArtifactExtractor {

	public static void main(String[] args) {

		DefaultParser defaultParser = new DefaultParser();

		Options option = new Options();
		option.addOption("SCPI_Host", true, "SAP CPI Host");
		option.addOption("user", true, "Username");
		option.addOption("password", true, "Password");
		option.addOption("filename", true, "Output File Name");

		try {

			CommandLine comlin = defaultParser.parse(option, args);

			String host = comlin.getOptionValue("SCPI_Host");
			String user = comlin.getOptionValue("user");
			String pass = comlin.getOptionValue("password");
			String fileName = comlin.getOptionValue("filename");

			if (pass == null) {
				java.io.Console console = System.console();
				pass = new String(console.readPassword("Enter Password of User " + user + " : "));
			}

			if (isNullOrEmptyCheck(host)) {
				throw new MissingArgumentException("Please provide SCPI_Host arguments to execute the operation");
			}

			if (isNullOrEmptyCheck(user)) {
				throw new MissingArgumentException("Please provide user arguments to execute the operation");
			}

			if (isNullOrEmptyCheck(pass)) {
				throw new MissingArgumentException("Please provide password arguments to execute the operation");
			}

			scpiConfig ctx = new scpiConfig();
			ctx.setSCPIHost(host);
			ctx.setUser(user);
			ctx.setPassword(pass);

			Map<String, String> header = new HashMap<String, String>();
			header.put("ACCEPT", "application/json");
	
			System.out.println("START : Extraction");
			System.out.println("EVENT : Connecting to Host " + host + " with user " + user);
			
			String apiUri= "";
			String completeURL = "";
			// 1-->Get API Proxy Count in the tenant			
			//apiUri="/api/v1/IntegrationPackages/";
			apiUri="/itspaces/odata/1.0/workspace.svc/ContentEntities.ContentPackages/?$format=json&$select=TechnicalName,DisplayName,CreatedBy,CreatedAt,ModifiedBy,ModifiedAt&$orderby=DisplayName";
			completeURL = "https://" + host + apiUri;
					
			HttpResponse packageInfo = doGet(completeURL, header, ctx);
			String packageInfoBody = EntityUtils.toString(packageInfo.getEntity(), "UTF-8");
			
			JsonArray packArr = (new JsonParser()).parse(packageInfoBody).getAsJsonObject().get("d").getAsJsonObject()
					.get("results").getAsJsonArray();
			
			int i = 0;
			
			System.out.println("EVENT : Found " + packArr.size() + " Packages");
			System.out.println("START : Fetching Package Information ");
			scpiPackage[] pkge = new scpiPackage[packArr.size()];
			scpiResult res = new scpiResult();

			// 6--> Loop at number of Proxy
			for (JsonElement packageJE : packArr) {

				// 7--> Get the single API Proxy result
				JsonObject packageJOB = packageJE.getAsJsonObject();
				
//				System.out.println(job.get("__metadata").getAsJsonObject().get("id").getAsString());

				// 8--> Set the Name, ID and base Path
				pkge[i] = new scpiPackage();
				pkge[i].setDisplayName(packageJOB.get("DisplayName").getAsString());
				pkge[i].setTechnicalName(packageJOB.get("TechnicalName").getAsString());
				pkge[i].setCreatedBy(packageJOB.get("CreatedBy").getAsString());
				pkge[i].setCreatedAt(packageJOB.get("CreatedAt").getAsString());
				
				
				apiUri = packageJOB.get("__metadata").getAsJsonObject().get("id").getAsString() + "/Artifacts?$orderby=DisplayName";
				//completeURL = "https://" + host + apiUri;
				
				HttpResponse iflowInfo = doGet(apiUri, header, ctx);
				String iflowInfoBody = EntityUtils.toString(iflowInfo.getEntity(), "UTF-8");
				
				JsonArray iflowArr = (new JsonParser()).parse(iflowInfoBody).getAsJsonObject().get("d").getAsJsonObject()
						.get("results").getAsJsonArray();
				
				scpiIFLOW[] iflow = new scpiIFLOW[iflowArr.size()];
				
				int j = 0;
				
				for (JsonElement iflowJE : iflowArr) {
					
					JsonObject iflowJOB = iflowJE.getAsJsonObject();
					
					iflow[j] = new scpiIFLOW();
					
					iflow[j].setDisplayName(iflowJOB.get("DisplayName").getAsString());
					iflow[j].setName(iflowJOB.get("Name").getAsString());
					iflow[j].setType(iflowJOB.get("Type").getAsString());
					iflow[j].setVersion(iflowJOB.get("Version").getAsString());
					iflow[j].setCreatedBy(iflowJOB.get("CreatedBy").getAsString());
					iflow[j].setCreatedAt(iflowJOB.get("CreatedAt").getAsString());
					iflow[j].setModifiedBy(iflowJOB.get("ModifiedBy").getAsString());
					iflow[j].setModifiedAt(iflowJOB.get("ModifiedAt").getAsString());
					
					j++;
				}
				
				pkge[i].setscpiIFLOW(iflow);
				
				i++;
			}

			System.out.println("END   : Fetching Package Information");

			res.setResult(pkge);

			// create the gson object
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			FileOutputStream outputStream = new FileOutputStream(fileName);
			byte[] strToBytes = gson.toJson(res).getBytes();
			outputStream.write(strToBytes);
			outputStream.close();

//			System.out.println(gson.toJson(res));

			System.out.println("END   : Extraction");

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
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

	public static HttpResponse doGet(String url, Map<String, String> headers, scpiConfig ctx)
			throws IOException {
		CloseableHttpResponse closeableHttpResponse = null;
		try {
			CloseableHttpClient client = HttpClientBuilder.create().build();
			String encoding = Base64.getEncoder()
					.encodeToString((ctx.getUser() + ":" + ctx.getPassword()).getBytes("UTF-8"));

			RequestBuilder requestbuilder = RequestBuilder.get().setUri(url).setHeader("Authorization",
					"Basic " + encoding);

			for (Map.Entry<String, String> header : headers.entrySet()) {
				requestbuilder.setHeader((String) header.getKey(), (String) header.getValue());
			}
			HttpUriRequest request = requestbuilder.build();
			closeableHttpResponse = client.execute(request);
		} catch (IOException e) {
			throw e;
		}
		return closeableHttpResponse;
	}
}
