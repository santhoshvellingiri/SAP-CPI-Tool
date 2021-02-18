package com.sanv.scpi.enabler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanv.scpi.Utility;
import com.sanv.scpi.model.scpiPackage;
import com.sanv.scpi.model.scpiRuntimeIflow;
import com.sanv.scpi.model.tenantConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

public class Downloader {
	
	public static void downloadDeployedIflows(tenantConfiguration tenant) throws Exception {

		List<scpiRuntimeIflow> runIFo = Runtime.getRuntimeIFlows(tenant);

		int totalIflow = runIFo.size();

		System.out.println("EVENT : Found " + totalIflow + " IFlows");
		System.out.println("START : Downloading Deployed IFlows ");
		
		runIFo.parallelStream().forEach(aIflo -> {
			try {
				if (aIflo.getType().equals("VALUE_MAPPING"))
					return;
				System.out.format("Event : Downloading IFlow %s %n", aIflo.getName());
				
				String apiUri = "";
				String completeURL = "";

				apiUri = String.format("/api/v1/IntegrationDesigntimeArtifacts(Id='%s',Version='active')/$value",
						aIflo.getSymbolicName());
				completeURL = "https://" + tenant.getTMNHost() + apiUri;
				
				HttpResponse iflowZip;
				iflowZip = Utility.doGet(completeURL, tenant);
				if (iflowZip.getStatusLine().getStatusCode() == 404) {
					System.out.format("Event : Integration design time artifact not found%n");
					return;
				}

				if (iflowZip.getStatusLine().getStatusCode() == 400) {
					System.out.format("Event : Cannot download the artifact from a configure only package.%n");
					return;
				}
				
				ZipInputStream zipIn = new ZipInputStream(iflowZip.getEntity().getContent());
				Utility.writeFile(zipIn,
						String.valueOf(tenant.getName()) + "/DeployedIFlow/" + aIflo.getSymbolicName());
				
				
			}catch (Exception e) {
				e.printStackTrace();
			}
			
		});

		/*
		String apiUri = "";
		String completeURL = "";
		apiUri = "/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentsListCommand";
		completeURL = "https://" + tenant.getTMNHost() + apiUri;

		HttpResponse packageInfo = Utility.doGet(completeURL, tenant);
		String packageInfoBody = EntityUtils.toString(packageInfo.getEntity(), "UTF-8");
		JsonArray iflowIDArr = JsonParser.parseString(packageInfoBody).getAsJsonObject().get("artifactInformations")
				.getAsJsonArray();
		int i = 0;
		ArrayList<TreeMap<String, String>> al = new ArrayList<>();
		TreeMap<String, String> colmName = new TreeMap<>();
		colmName.put("IFlow", "Example");
		al.add(colmName);

		for (JsonElement iflowIDJE : iflowIDArr) {
			JsonObject iflowIDJOB = iflowIDJE.getAsJsonObject();
			String symbolicName = iflowIDJOB.get("symbolicName").getAsString();
			String iflowName = iflowIDJOB.get("name").getAsString();
			String packageName = "";
			System.out.format("IFLOW %03d/%03d : Downloading IFlow %s%n",
					new Object[] { Integer.valueOf(i + 1), Integer.valueOf(totalIflow), iflowName });
			JsonArray tagArr = iflowIDJOB.get("tags").getAsJsonArray();
			for (int j = tagArr.size() - 1; j >= 0; j--) {
				JsonObject tagJOB = tagArr.get(j).getAsJsonObject();
				if (tagJOB.get("name").getAsString().equals("artifact.package.name")) {
					packageName = tagJOB.get("value").getAsString();
					break;
				}
			}
			apiUri = String.format("/api/v1/IntegrationDesigntimeArtifacts(Id='%s',Version='active')/$value",
					new Object[] { symbolicName });
			completeURL = "https://" + tenant.getTMNHost() + apiUri;
			HttpResponse iflowZip = Utility.doGet(completeURL, tenant);
			if (iflowZip.getStatusLine().getStatusCode() == 404) {
				System.out.format("IFLOW %03d/%03d : Integration design time artifact not found%n",
						new Object[] { Integer.valueOf(i + 1), Integer.valueOf(totalIflow) });
				continue;
			}
			if (iflowZip.getStatusLine().getStatusCode() == 400) {
				System.out.format("IFLOW %03d/%03d : Cannot download the artifact from a configure only package.%n",
						new Object[] { Integer.valueOf(i + 1), Integer.valueOf(totalIflow) });
				continue;
			}
			ZipInputStream zipIn = new ZipInputStream(iflowZip.getEntity().getContent());
			Utility.writeFile(zipIn,
					String.valueOf(tenant.getName()) + "/DeployedIFlow/" + packageName + "/" + symbolicName);
			i++;
		}*/
	}

	public static void downloadAllPackage(tenantConfiguration tenant) throws Exception {
		List<scpiPackage> packgArr = Design.getPackageList(tenant);
		int totalPackage = packgArr.size();
		System.out.println("EVENT : Found " + totalPackage + " Packages");
		System.out.println("START : Downloading Custom Packages");
		int i = 0;
		for (scpiPackage pckJE : packgArr) {
			String packageID = pckJE.getTechnicalName();
			String packageName = pckJE.getDisplayName();
			System.out.format("Package %02d/%d : Downloading Package - %s%n",
					new Object[] { Integer.valueOf(i + 1), Integer.valueOf(totalPackage), packageName });
			HttpResponse packageZip = fetchPackage(packageID, tenant);
			if (packageZip.getStatusLine().getStatusCode() == 404) {
				System.out.format("               Package %s Integration design time artifact not found.%n",
						new Object[] { packageName });
				i++;
				continue;
			}
			if (packageZip.getStatusLine().getStatusCode() == 400) {
				System.out.format("               Package %s Cannot download - Configure only package.%n",
						new Object[] { packageName });
				i++;
				continue;
			}
			if (packageZip.getStatusLine().getStatusCode() == 500) {
				System.out.format("                Downloading Failed  - %s Contains Artifacts in Draft Status.%n",
						new Object[] { packageName });
				i++;
				continue;
			}
			Utility.writePackageFile(tenant.getName(), packageName, packageID, packageZip);
			i++;
		}
	}

	public static void downloadPackage(String packageID, tenantConfiguration tenant) throws Exception {
		String apiUri = String.format("/api/v1/IntegrationPackages('%s')", new Object[] { packageID });
		String completeURL = "https://" + tenant.getTMNHost() + apiUri;
		HttpResponse packageInfo = Utility.doGet(completeURL, tenant);
		String packageInfoBody = EntityUtils.toString(packageInfo.getEntity(), "UTF-8");
		JsonObject packgData = JsonParser.parseString(packageInfoBody).getAsJsonObject().get("d").getAsJsonObject();
		String packageName = packgData.get("Name").getAsString();
		System.out.println("START : Downloading Custom Packages :" + packageName);
		HttpResponse packageZip = fetchPackage(packageID, tenant);
		Utility.writePackageFile(tenant.getName(), packageName, packageID, packageZip);
		System.out.println("END : Downloading Custom Packages :" + packageName);
	}

	public static HttpResponse fetchPackage(String packageID, tenantConfiguration tenant) throws Exception {
		String apiUri = String.format("/api/v1/IntegrationPackages('%s')/$value", new Object[] { packageID });
		String completeURL = "https://" + tenant.getTMNHost() + apiUri;
		HttpResponse packageZip = Utility.doGet(completeURL, tenant);
		return packageZip;
	}
}
