package com.sanv.scpi.enabler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanv.scpi.Utility;
import com.sanv.scpi.model.IflowExConfig;
import com.sanv.scpi.model.scpiIFLOW;
import com.sanv.scpi.model.scpiPackage;
import com.sanv.scpi.model.tenantConfiguration;

public class ExternalConfiguration {

	public static List<IflowExConfig> downloadAllExConf(tenantConfiguration tenant) throws Exception {

		System.out.println("EVENT : Connecting to Host " + tenant.getTMNHost() + " with user " + tenant.getUser());

		List<scpiPackage> pkgLst = Design.getPackageList(tenant);

		List<IflowExConfig> ifExConfigList = new ArrayList<IflowExConfig>();

		pkgLst.parallelStream().forEach(aPkg -> {
			try {
				System.out.println("EVENT : Extracting Package " + aPkg.getDisplayName());
				String packageName = aPkg.getDisplayName();
				List<scpiIFLOW> iflst = Design.getIflow(aPkg.getTechnicalName(), tenant);

				iflst.parallelStream().forEach(aIfl -> {

					IflowExConfig ifxConfig = new IflowExConfig();
					ifxConfig.setPackageName(packageName);
					ifxConfig.setIflowName(aIfl.getDisplayName());
					TreeMap<String, String> XConf = null;
					try {
						XConf = getConfiguration(aIfl.getName(), tenant);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (!XConf.isEmpty()) {
						ifxConfig.setExternalConfiguration(XConf);
						ifExConfigList.add(ifxConfig);
					}
				});

			} catch (Exception e) {
				e.printStackTrace();
			}

		});

		// System.out.println("Result : Extracted " + pkge.size() + " Packages");

		Collections.sort(ifExConfigList);
		return ifExConfigList;

	}

	public static TreeMap<String, String> getConfiguration(String iFlowID, tenantConfiguration tenant)
			throws Exception {
		String apiUri = "";
		String completeURL = "";
		apiUri = String.format("/api/v1/IntegrationDesigntimeArtifacts(Id='%s',Version='active')/Configurations",
				iFlowID);
		completeURL = "https://" + tenant.getTMNHost() + apiUri;
		HttpResponse configValue = Utility.doGet(completeURL, tenant);
		String configBody = EntityUtils.toString(configValue.getEntity(), "UTF-8");
		JsonArray confArr = JsonParser.parseString(configBody).getAsJsonObject().get("d").getAsJsonObject()
				.get("results").getAsJsonArray();
		TreeMap<String, String> XConf = new TreeMap<String, String>();
		for (JsonElement confJE : confArr) {
			JsonObject confJOB = confJE.getAsJsonObject();
			XConf.put(confJOB.get("ParameterKey").getAsString(), confJOB.get("ParameterValue").getAsString());
		}
		return XConf;
	}
}
