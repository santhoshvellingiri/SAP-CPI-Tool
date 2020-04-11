package com.sanv.scpi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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
import com.sanv.scpi.model.scpiRuntimeIflow;

public class scpiRuntimeExtractor {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

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

			String apiUri = "";
			String completeURL = "";
			// 1-->Get API Proxy Count in the tenant
			// apiUri="/api/v1/IntegrationPackages/";
			apiUri = "/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentsListCommand";
			completeURL = "https://" + host + apiUri;

			HttpResponse packageInfo = doGet(completeURL, header, ctx);
			String packageInfoBody = EntityUtils.toString(packageInfo.getEntity(), "UTF-8");

			JsonArray iflowIDArr = (new JsonParser()).parse(packageInfoBody).getAsJsonObject()
					.get("artifactInformations").getAsJsonArray();

			int i = 0;

			System.out.println("EVENT : Found " + iflowIDArr.size() + " Packages");
			System.out.println("START : Fetching Runtime Information ");
			scpiRuntimeIflow[] iflow = new scpiRuntimeIflow[iflowIDArr.size()];
			scpiResult res = new scpiResult();

			// 6--> Loop at number of Proxy
			for (JsonElement iflowIDJE : iflowIDArr) {

				iflow[i] = new scpiRuntimeIflow();

				// 7--> Get the single API Proxy result
				JsonObject iflowIDJOB = iflowIDJE.getAsJsonObject();

				String ID = iflowIDJOB.get("id").getAsString();

				JsonArray tagArr = iflowIDJOB.get("tags").getAsJsonArray();

				for (JsonElement tagJE : tagArr) {

					JsonObject tagJOB = tagJE.getAsJsonObject();

					if (tagJOB.get("name").getAsString().equals("artifact.package.name")) {
						iflow[i].setpackageName(tagJOB.get("value").getAsString());
						break;
					}

				}

				apiUri = "/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentDetailCommand?artifactId="
						+ ID;
				completeURL = "https://" + host + apiUri;

				HttpResponse iflowInfo = doGet(completeURL, header, ctx);
				String iflowInfoBody = EntityUtils.toString(iflowInfo.getEntity(), "UTF-8");

//				System.out.println(ID);

				JsonElement je = (new JsonParser()).parse(iflowInfoBody);

				JsonObject logConfiguration = je.getAsJsonObject().get("logConfiguration").getAsJsonObject();
				JsonObject artifactInformation = je.getAsJsonObject().get("artifactInformation").getAsJsonObject();
//				JsonArray componentInformations = je.getAsJsonObject().get("componentInformations").getAsJsonArray();

//				System.out.println(artifactInformation.get("symbolicName").getAsString());

				iflow[i].setname(artifactInformation.get("name").getAsString());
				iflow[i].setsymbolicName(artifactInformation.get("symbolicName").getAsString());
				iflow[i].settype(artifactInformation.get("type").getAsString());
				iflow[i].setversion(artifactInformation.get("version").getAsString());
				iflow[i].setdeployedBy(artifactInformation.get("deployedBy").getAsString());
				iflow[i].setdeployedOn(artifactInformation.get("deployedOn").getAsString());
				iflow[i].setlogLevel(logConfiguration.get("logLevel").getAsString());

				String endpoint = "";
				if (je.getAsJsonObject().get("endpoints") != null) {
					JsonArray endpoints = je.getAsJsonObject().get("endpoints").getAsJsonArray();
					for (JsonElement endpJE : endpoints) {
						endpoint += endpJE.getAsString() + "\n";
					}

				}
				iflow[i].setendpointUri(endpoint);

				String pollEP = "";

				if (je.getAsJsonObject().get("componentInformations") != null) {
					JsonArray compInfo = je.getAsJsonObject().get("componentInformations").getAsJsonArray();
					JsonElement adapterPollInfos = compInfo.get(0).getAsJsonObject().get("adapterPollInfos");
					if (adapterPollInfos != null) {
						JsonArray pollingEndpoints = adapterPollInfos.getAsJsonArray();
						for (JsonElement pollingEndJE : pollingEndpoints) {
							String temp = pollingEndJE.getAsJsonObject().get("endpointUri").getAsString();
							if (temp.startsWith("sftp"))
								temp = temp.substring(0, temp.indexOf("?") - 1);
							pollEP += temp + "\n";
						}

					}

				}
				iflow[i].setqueueURI(pollEP);

				i++;

//				if(i==10)
//					break;
			}

			System.out.println("END   : Fetching Package Information");

			// create the gson object
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			FileOutputStream outputStream = new FileOutputStream(fileName);
			byte[] strToBytes = gson.toJson(iflow).getBytes();
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

	public static HttpResponse doGet(String url, Map<String, String> headers, scpiConfig ctx) throws IOException {
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
