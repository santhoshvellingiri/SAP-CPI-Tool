package com.sanv.scpi.enabler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanv.scpi.Utility;
import com.sanv.scpi.model.scpiRuntimeIflow;
import com.sanv.scpi.model.tenantConfiguration;

public class Runtime {

	public static List<scpiRuntimeIflow> getRuntimeData(tenantConfiguration tenant) throws Exception {

		System.out.println("EVENT : Connecting to Host " + tenant.getTMNHost() + " with user " + tenant.getUser());

		List<scpiRuntimeIflow> runIFo = getRuntimeIFlowID(tenant);
		List<scpiRuntimeIflow> runIFoRet = new ArrayList<scpiRuntimeIflow>();

		runIFo.parallelStream().forEach(aIfo -> {

			String apiUri = "";
			String completeURL = "";

			System.out.format("Event : Fetching Runtime Data of IFlow %s %n", aIfo.getName());

			apiUri = "/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentDetailCommand?artifactId="
					+ aIfo.getId();
			completeURL = "https://" + tenant.getTMNHost() + apiUri;

			HttpResponse iflowInfo;
			String iflowInfoBody;
			try {
				iflowInfo = Utility.doGet(completeURL, tenant);
				iflowInfoBody = EntityUtils.toString(iflowInfo.getEntity(), "UTF-8");
				JsonElement je = JsonParser.parseString(iflowInfoBody);

				JsonObject logConfiguration = je.getAsJsonObject().get("logConfiguration").getAsJsonObject();

				aIfo.setlogLevel(logConfiguration.get("logLevel").getAsString());

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
				aIfo.setendpointUri(endpoint);

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
				aIfo.setqueueURI(pollEP);
				runIFoRet.add(aIfo);
			} catch (Exception e) {
				e.printStackTrace();
			}

		});

		Collections.sort(runIFoRet);
		return runIFoRet;

//		int index = 0;
//		int totalIflow = runIFo.size();

//		for (scpiRuntimeIflow aIfo : runIFo) {
//			
//			System.out.format("IFLOW  %d/%d : Fetching %s from Runtime%n", index + 1, totalIflow, aIfo.getName());
//			
//			String apiUri = "";
//			String completeURL = "";			
//
//			apiUri = "/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentDetailCommand?artifactId="
//					+ aIfo.getId();
//			completeURL = "https://" + tenant.getTMNHost() + apiUri;
//
//			HttpResponse iflowInfo = Utility.doGet(completeURL, tenant);
//			String iflowInfoBody = EntityUtils.toString(iflowInfo.getEntity(), "UTF-8");
//
//			JsonElement je = JsonParser.parseString(iflowInfoBody);
//
//			JsonObject logConfiguration = je.getAsJsonObject().get("logConfiguration").getAsJsonObject();
//
//			aIfo.setlogLevel(logConfiguration.get("logLevel").getAsString());
//
//			String endpoint = "";
//			if (je.getAsJsonObject().get("endpoints") != null) {
//				JsonArray endpoints = je.getAsJsonObject().get("endpoints").getAsJsonArray();
//				int counter = 0;
//				for (JsonElement endpJE : endpoints) {
//					endpoint += endpJE.getAsString();
//					counter++;
//					if (counter != endpoints.size())
//						endpoint += "\n";
//				}
//			}
//			aIfo.setendpointUri(endpoint);
//
//			String pollEP = "";
//
//			if (je.getAsJsonObject().get("componentInformations") != null) {
//				JsonArray compInfo = je.getAsJsonObject().get("componentInformations").getAsJsonArray();
//				JsonElement adapterPollInfos = compInfo.get(0).getAsJsonObject().get("adapterPollInfos");
//				if (adapterPollInfos != null) {
//					int counter = 0;
//					JsonArray pollingEndpoints = adapterPollInfos.getAsJsonArray();
//					for (JsonElement pollingEndJE : pollingEndpoints) {
//						String temp = pollingEndJE.getAsJsonObject().get("endpointUri").getAsString();
//						if (temp.startsWith("sftp"))
//							temp = temp.substring(0, temp.indexOf("?") - 1);
//						pollEP += temp;
//						counter++;
//						if (counter != pollingEndpoints.size())
//							pollEP += "\n";
//					}
//
//				}
//
//			}
//			aIfo.setqueueURI(pollEP);			
//			
//			runIFo.set(index, aIfo);
//			
//			index++;
//
//			if (index == 10)
//				break;
//		}
//		return runIFo;
//
//		System.out.println("END   : Fetching IFlow Information");

	}

	public static List<scpiRuntimeIflow> getRuntimeIFlowID(tenantConfiguration tenant) throws Exception {

		String apiUri = "";
		String completeURL = "";

		apiUri = "/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.IntegrationComponentsListCommand";
		completeURL = "https://" + tenant.getTMNHost() + apiUri;

		HttpResponse packageInfo = Utility.doGet(completeURL, tenant);
		String packageInfoBody = EntityUtils.toString(packageInfo.getEntity(), "UTF-8");

		JsonArray iflowIDArr = JsonParser.parseString(packageInfoBody).getAsJsonObject().get("artifactInformations")
				.getAsJsonArray();

		List<scpiRuntimeIflow> runIfo = new ArrayList<scpiRuntimeIflow>();

//		Gson gson = new Gson();
//		scpiRuntimeIflow[] runIfoArr = (scpiRuntimeIflow[])gson.fromJson(iflowIDArr, scpiRuntimeIflow[].class); 
//		runIfo = Arrays.asList(runIfoArr);

		for (JsonElement iflowIDJE : iflowIDArr) {

			JsonObject iflowIDJOB = iflowIDJE.getAsJsonObject();

			scpiRuntimeIflow iFlo = new scpiRuntimeIflow();

			iFlo.setId(iflowIDJOB.get("id").getAsString());
			iFlo.setname(iflowIDJOB.get("name").getAsString());
			iFlo.setsymbolicName(iflowIDJOB.get("symbolicName").getAsString());
			iFlo.settype(iflowIDJOB.get("type").getAsString());
			iFlo.setversion(iflowIDJOB.get("version").getAsString());
			iFlo.setdeployedBy(iflowIDJOB.get("deployedBy").getAsString());
			iFlo.setdeployedOn(iflowIDJOB.get("deployedOn").getAsString());

			JsonArray tagArr = iflowIDJOB.get("tags").getAsJsonArray();
			for (int j = tagArr.size() - 1; j >= 0; j--) {
				JsonObject tagJOB = tagArr.get(j).getAsJsonObject();
				if (tagJOB.get("name").getAsString().equals("artifact.package.name")) {
					iFlo.setpackageName(tagJOB.get("value").getAsString());
					break;
				}
			}
			runIfo.add(iFlo);
		}

		return runIfo;

	}
	

}
