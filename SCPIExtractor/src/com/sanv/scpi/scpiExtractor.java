package com.sanv.scpi;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanv.scpi.model.scpiConfig;
import com.sanv.scpi.model.scpiIFLOW;
import com.sanv.scpi.model.scpiPackage;
import com.sanv.scpi.model.scpiRuntimeIflow;

public class scpiExtractor {

	static scpiConfig ctx;

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {

		long startTime = System.nanoTime();

		DefaultParser defaultParser = new DefaultParser();

		Options option = new Options();
		option.addOption("SCPI_Host", true, "SAP CPI Host");
		option.addOption("user", true, "Username");
		option.addOption("password", true, "Password");
		option.addOption("filename", true, "Output File Name");
		option.addOption("mode", true, "Extraction Mode");
		option.addOption("resolveExtVar", true, "Resolve Extraction Variables Y/N");

		try {

			CommandLine comlin = defaultParser.parse(option, args);

			String host = comlin.getOptionValue("SCPI_Host");
			String user = comlin.getOptionValue("user");
			String pass = comlin.getOptionValue("password");
			String fileName = comlin.getOptionValue("filename");
			String mode = comlin.getOptionValue("mode");
			String exVar = comlin.getOptionValue("resolveExtVar");

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

			if (isNullOrEmptyCheck(fileName)) {
				throw new MissingArgumentException("Provide fileName arguments to execute the operation");
			}

			if (isNullOrEmptyCheck(mode)) {
				throw new MissingArgumentException("Provide mode arguments to execute the operation");
			}
			if (isNullOrEmptyCheck(exVar)) {
				exVar = "N";
			}

			ctx = new scpiConfig();
			ctx.setSCPIHost(host);
			ctx.setUser(user);
			ctx.setPassword(pass);

			if (mode.equalsIgnoreCase("designtime")) {

				System.out.println("START : Extraction");
				scpiPackage[] res = getDesigntimedata();
				writeFile(fileName, res);
				System.out.println("END   : Extraction");

			} else if (mode.equalsIgnoreCase("runtime")) {
				System.out.println("START : Extraction");
				scpiRuntimeIflow[] res = getRuntimeData();
				writeFile(fileName, res);
				System.out.println("END   : Extraction");
			} else if (mode.equalsIgnoreCase("configuration")) {
				System.out.println("START : Extraction");
				ArrayList<TreeMap<String, String>> cnfData = getConfigData(exVar);
				writeFile(fileName, cnfData);
				System.out.println("END   : Extraction");
			} else {
				System.out.println("Incorrect Mode value! Accepted value : designtime / runtime");
			}

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

			if (closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
				System.out.println("ERROR : Cannot Connect");
				System.out.println("INFO  : Check Host / User Credential / User Authorization");
				throw new Exception("UNABLE_TO_CONNECT");
			}

		} catch (IOException e) {
			throw e;
		}
		return closeableHttpResponse;
	}

	public static scpiPackage[] getDesigntimedata() throws Exception {

		System.out.println("START : Fetching Package Information ");
		scpiPackage[] res = getPackage();

		System.out.println("END   : Fetching Package Information");

		return res;

	}

	public static scpiPackage[] getPackage() throws Exception {

		System.out.println("EVENT : Connecting to Host " + ctx.getSCPIHost() + " with user " + ctx.getUser());

		String apiUri = "";
		String completeURL = "";
		// 1-->Get API Proxy Count in the tenant
		// apiUri="/api/v1/IntegrationPackages/";
		apiUri = "/itspaces/odata/1.0/workspace.svc/ContentEntities.ContentPackages/?$format=json&$select=TechnicalName,DisplayName,CreatedBy,CreatedAt,ModifiedBy,ModifiedAt&$orderby=DisplayName";
		completeURL = "https://" + ctx.getSCPIHost() + apiUri;

		HttpResponse packageInfo = doGet(completeURL);
		String packageInfoBody = EntityUtils.toString(packageInfo.getEntity(), "UTF-8");

		JsonArray packArr = (new JsonParser()).parse(packageInfoBody).getAsJsonObject().get("d").getAsJsonObject()
				.get("results").getAsJsonArray();

		System.out.println("EVENT : Found " + packArr.size() + " Packages");

		scpiPackage[] pkge = new scpiPackage[packArr.size()];
		int i = 0;

		Map<String, String> header = new HashMap<String, String>();
		header.put("ACCEPT", "application/json");

		// 6--> Loop at number of Proxy
		for (JsonElement packageJE : packArr) {

			// 7--> Get the single API Proxy result
			JsonObject packageJOB = packageJE.getAsJsonObject();

			System.out.println("EVENT : Extracting Package " + packageJOB.get("DisplayName").getAsString());

			// 8--> Set the Name, ID and base Path
			pkge[i] = new scpiPackage();
			pkge[i].setDisplayName(packageJOB.get("DisplayName").getAsString());
			pkge[i].setTechnicalName(packageJOB.get("TechnicalName").getAsString());
			pkge[i].setCreatedBy(packageJOB.get("CreatedBy").getAsString());
			pkge[i].setCreatedAt(packageJOB.get("CreatedAt").getAsString());

			// 9 --> Get IFlow based on Package Name
			pkge[i].setscpiIFLOW(getIflow(packageJOB.get("TechnicalName").getAsString()));
			i++;
		}

		return pkge;

	}

	public static scpiIFLOW[] getIflow(String packageID) throws Exception {

		String apiUri = String.format(
				"/itspaces/odata/1.0/workspace.svc/ContentPackages('%s')/Artifacts?$orderby=DisplayName", packageID);
		String completeURL = "https://" + ctx.getSCPIHost() + apiUri;

		HttpResponse iflowInfo = doGet(completeURL);
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
		return iflow;
	}

	public static void writeFile(String fileName, Object obj) throws Exception {

		// create the gson object
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		FileOutputStream outputStream = new FileOutputStream(fileName);
		byte[] strToBytes = gson.toJson(obj).getBytes();
		outputStream.write(strToBytes);
		outputStream.close();

	}

	public static scpiRuntimeIflow[] getRuntimeData() throws Exception {

		System.out.println("EVENT : Connecting to Host " + ctx.getSCPIHost() + " with user " + ctx.getUser());

		String apiUri = "";
		String completeURL = "";
		// 1-->Get API Proxy Count in the tenant
		// apiUri="/api/v1/IntegrationPackages/";
		apiUri = "/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentsListCommand";
		completeURL = "https://" + ctx.getSCPIHost() + apiUri;

		HttpResponse packageInfo = doGet(completeURL);
		String packageInfoBody = EntityUtils.toString(packageInfo.getEntity(), "UTF-8");

		JsonArray iflowIDArr = (new JsonParser()).parse(packageInfoBody).getAsJsonObject().get("artifactInformations")
				.getAsJsonArray();

		int i = 0;

		System.out.println("EVENT : Found " + iflowIDArr.size() + " IFlows");
		System.out.println("START : Fetching Runtime Information ");
		scpiRuntimeIflow[] iflow = new scpiRuntimeIflow[iflowIDArr.size()];

		// 2--> Loop at each IFlow
		for (JsonElement iflowIDJE : iflowIDArr) {

			System.out.format("EVENT : Fetching Iflow %d from Runtime%n", i + 1);

			iflow[i] = new scpiRuntimeIflow();

			// 3--> Get IFlow ID
			JsonObject iflowIDJOB = iflowIDJE.getAsJsonObject();
			String ID = iflowIDJOB.get("id").getAsString();

			// 4 --> Read tags and find package name
			JsonArray tagArr = iflowIDJOB.get("tags").getAsJsonArray();
			for (int j = tagArr.size() - 1; j >= 0; j--) {
				JsonObject tagJOB = tagArr.get(j).getAsJsonObject();
				if (tagJOB.get("name").getAsString().equals("artifact.package.name")) {
					iflow[i].setpackageName(tagJOB.get("value").getAsString());
					break;
				}
			}

			apiUri = "/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentDetailCommand?artifactId="
					+ ID;
			completeURL = "https://" + ctx.getSCPIHost() + apiUri;

			HttpResponse iflowInfo = doGet(completeURL);
			String iflowInfoBody = EntityUtils.toString(iflowInfo.getEntity(), "UTF-8");

//			System.out.println(ID);

			JsonElement je = (new JsonParser()).parse(iflowInfoBody);

			JsonObject logConfiguration = je.getAsJsonObject().get("logConfiguration").getAsJsonObject();
			JsonObject artifactInformation = je.getAsJsonObject().get("artifactInformation").getAsJsonObject();
//			JsonArray componentInformations = je.getAsJsonObject().get("componentInformations").getAsJsonArray();

//			System.out.println(artifactInformation.get("symbolicName").getAsString());

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
				int counter = 0;
				for (JsonElement endpJE : endpoints) {
					endpoint += endpJE.getAsString();
					counter++;
					if (counter != endpoints.size())
						endpoint += "\n";

				}

			}
			iflow[i].setendpointUri(endpoint);

			String pollEP = "";

			if (je.getAsJsonObject().get("componentInformations") != null) {
				JsonArray compInfo = je.getAsJsonObject().get("componentInformations").getAsJsonArray();
				JsonElement adapterPollInfos = compInfo.get(0).getAsJsonObject().get("adapterPollInfos");
				if (adapterPollInfos != null) {
					int counter = 0;
					JsonArray pollingEndpoints = adapterPollInfos.getAsJsonArray();
					for (JsonElement pollingEndJE : pollingEndpoints) {
						String temp = pollingEndJE.getAsJsonObject().get("endpointUri").getAsString();
						if (temp.startsWith("sftp"))
							temp = temp.substring(0, temp.indexOf("?") - 1);
						pollEP += temp;
						counter++;
						if (counter != pollingEndpoints.size())
							pollEP += "\n";
					}

				}

			}
			iflow[i].setqueueURI(pollEP);

			i++;

//			if (i == 10)
//				break;
		}

		System.out.println("END   : Fetching IFlow Information");

		return iflow;
	}

	public static ArrayList<TreeMap<String, String>> getConfigData(String exVar) throws Exception {

		System.out.println("EVENT : Connecting to Host " + ctx.getSCPIHost() + " with user " + ctx.getUser());

		String apiUri = "";
		String completeURL = "";
		// 1-->Get deployed IFLow
		apiUri = "/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentsListCommand";
		completeURL = "https://" + ctx.getSCPIHost() + apiUri;

		HttpResponse packageInfo = doGet(completeURL);
		String packageInfoBody = EntityUtils.toString(packageInfo.getEntity(), "UTF-8");

		JsonArray iflowIDArr = (new JsonParser()).parse(packageInfoBody).getAsJsonObject().get("artifactInformations")
				.getAsJsonArray();

		int i = 0;

		System.out.println("EVENT : Found " + iflowIDArr.size() + " IFlows");
		System.out.println("START : Fetching Runtime Information ");

		ArrayList<TreeMap<String, String>> al = new ArrayList<TreeMap<String, String>>();
		TreeMap<String, String> colmName = new TreeMap<String, String>();
		colmName.put("IFlow", "Example");
		al.add(colmName);

		// 2--> Loop Each Iflow
		for (JsonElement iflowIDJE : iflowIDArr) {

//	
			// 3--> Get IFLow Name and ID
			JsonObject iflowIDJOB = iflowIDJE.getAsJsonObject();

			String symbolicName = iflowIDJOB.get("symbolicName").getAsString();
			// String version = iflowIDJOB.get("version").getAsString();
			String iflowName = iflowIDJOB.get("name").getAsString();
			String type = iflowIDJOB.get("type").getAsString();

			if (type.equals("VALUE_MAPPING"))
				continue;

//			if(!iflowName.equals("Notify Service Ticket of Follow Up Document from SAP Business Suite_"))
//				continue;

			System.out.println("EVENT : Extraction Config of IFlow " + iflowName);

			// 4 Get IFLow ZIP
			apiUri = String.format("/api/v1/IntegrationDesigntimeArtifacts(Id='%s',Version='active')/$value",
					symbolicName);
			completeURL = "https://" + ctx.getSCPIHost() + apiUri;

			HttpResponse iflowZip = doGet(completeURL);

//			byte [] byteArray = EntityUtils.toByteArray(iflowZip.getEntity());
//			FileOutputStream outputStream = new FileOutputStream("temp.zip");
//			outputStream.write(byteArray);
//			outputStream.close();

			// 5 Unzip and read only property and model file
			ZipInputStream zipIn = new ZipInputStream(iflowZip.getEntity().getContent());
			InputStream propertyFile = null;
			InputStream iflowData = null;
			ZipEntry entry = zipIn.getNextEntry();
			while (entry != null) {
				String entryName = entry.getName();
				if (entryName.endsWith(".prop") && exVar.equalsIgnoreCase("y")) {
					propertyFile = convertZipInputStreamToInputStream(zipIn);
//					System.out.println(convertInputStreamToString(propertyFile));					
				} else if (entryName.endsWith(".iflw")) {
					iflowData = convertZipInputStreamToInputStream(zipIn);
//					System.out.println(convertInputStreamToString(iflowData));	
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}

			// 6 Load properties
			Properties prop = new Properties();
			if (exVar.equalsIgnoreCase("y")) {				
				prop.load(propertyFile);
			}

			// 7 Parse model xml
			Document document = processxml(iflowData);
			document.getDocumentElement().normalize();
			Element root = document.getDocumentElement();
			Element collaboration = (Element) root
					.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "collaboration").item(0);
			NodeList messageFlows = collaboration.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL",
					"messageFlow");

			TreeMap<String, String> map;

			for (int tem1 = 0; tem1 < messageFlows.getLength(); tem1++) {

				Node messageFlow = messageFlows.item(tem1);

				if (messageFlow.getNodeType() == Node.ELEMENT_NODE) {

					Element mesgFlowElement = (Element) messageFlow;

					NodeList properties = mesgFlowElement.getElementsByTagNameNS("http:///com.sap.ifl.model/Ifl.xsd",
							"property");

					map = new TreeMap<String, String>();
					map.put("IFlow", iflowName);

					for (int tem2 = 0; tem2 < properties.getLength(); tem2++) {

						Node property = properties.item(tem2);
						if (property.getNodeType() == Node.ELEMENT_NODE) {

							Element propertyElement = (Element) property;
							String key = propertyElement.getElementsByTagName("key").item(0).getTextContent();
							String value = propertyElement.getElementsByTagName("value").item(0).getTextContent();

							if (!colmName.containsKey(key))
								colmName.put(key, "example");

							if (exVar.equalsIgnoreCase("y")) {

								final String regex = "\\{\\{[\\w -<>\\+]*\\}\\}";

								if (value.contains("{{")) {

									final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
									final Matcher matcher = pattern.matcher(value);

									while (matcher.find()) {

										String string1 = matcher.group(0);
//										System.out.println("Full match: " + string1);

										String onlyProp = string1.replace("{{", "").replace("}}", "");

										String propvalue = prop.get(onlyProp).toString();

										if (propvalue != null && !propvalue.isEmpty()) {
											propvalue = "[" + propvalue + "]";
											value = value.replace(string1, propvalue);
										}
									}
								}
							}

							map.put(key, value);
						}
					}
					al.add(map);
				}
			}
			i++;
//			if (i == 5)
//				break;
		}

		return al;

	}

	private static InputStream convertZipInputStreamToInputStream(ZipInputStream in) throws IOException {
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