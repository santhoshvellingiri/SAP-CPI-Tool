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
import com.sanv.scpi.model.scpiIFLOW;
import com.sanv.scpi.model.scpiPackage;
import com.sanv.scpi.model.scpiPackageWithIFlow;
import com.sanv.scpi.model.tenantConfiguration;

public class Design {

	public static List<scpiPackageWithIFlow> getPackagewithIFlow(tenantConfiguration tenant) throws Exception {

		System.out.println("EVENT : Connecting to Host " + tenant.getTMNHost() + " with user " + tenant.getUser());

		List<scpiPackageWithIFlow> pkge = new ArrayList<scpiPackageWithIFlow>();
		List<scpiPackage> pkgLst = getPackageList(tenant);

		pkgLst.parallelStream().forEach(aPkg -> {

			try {
				System.out.println("EVENT : Extracting Package " + aPkg.getDisplayName());
				scpiPackageWithIFlow pkg = new scpiPackageWithIFlow();
				pkg.setDisplayName(aPkg.getDisplayName());
				pkg.setTechnicalName(aPkg.getTechnicalName());
				pkg.setCreatedBy(aPkg.getCreatedBy());
				pkg.setCreatAt(aPkg.getCreatedAt());
				pkg.setscpiIFLOW(getIflow(aPkg.getTechnicalName(), tenant));
				pkge.add(pkg);

			} catch (Exception e) {
				e.printStackTrace();
			}

		});

		System.out.println("Result : Extracted " + pkge.size() + " Packages");

//		String apiUri = "";
//		String completeURL = "";
//
//		apiUri = "/itspaces/odata/1.0/workspace.svc/ContentEntities.ContentPackages/?$format=json&$select=TechnicalName,DisplayName,CreatedBy,CreatedAt,ModifiedBy,ModifiedAt&$orderby=DisplayName";
//		completeURL = "https://" + tenant.getTMNHost() + apiUri;
//
//		HttpResponse packageInfo = Utility.doGet(completeURL, tenant);
//		String packageInfoBody = EntityUtils.toString(packageInfo.getEntity(), "UTF-8");
//
//		JsonArray packArr = JsonParser.parseString(packageInfoBody).getAsJsonObject().get("d").getAsJsonObject()
//				.get("results").getAsJsonArray();
//
//
//
//		System.out.println("EVENT : Found " + packArr.size() + " Packages");
//		
//		for (JsonElement packageJE : packArr) {
//
//			JsonObject packageJOB = packageJE.getAsJsonObject();
//
//			System.out.println("EVENT : Extracting Package " + packageJOB.get("DisplayName").getAsString());
//
//			scpiPackageWithIFlow pkg = new scpiPackageWithIFlow();
//
//			pkg.setDisplayName(packageJOB.get("DisplayName").getAsString());
//			pkg.setTechnicalName(packageJOB.get("TechnicalName").getAsString());
//			pkg.setCreatedBy(packageJOB.get("CreatedBy").getAsString());
//			pkg.setCreatedAt(packageJOB.get("CreatedAt").getAsString());
//			pkg.setscpiIFLOW(getIflow(packageJOB.get("TechnicalName").getAsString(), tenant));
//
//			pkge.add(pkg);
//
//		}

		Collections.sort(pkge);
		return pkge;

	}

	public static List<scpiPackage> getPackageList(tenantConfiguration tenant) throws Exception {

//		System.out.println("EVENT : Connecting to Host " + tenant.getTMNHost() + " with user " + tenant.getUser());

		String apiUri = "";
		String completeURL = "";

		// apiUri =
		// "/itspaces/odata/1.0/workspace.svc/ContentEntities.ContentPackages/?$format=json&$select=TechnicalName,DisplayName,CreatedBy,CreatedAt,ModifiedBy,ModifiedAt&$orderby=DisplayName";
		apiUri = "/api/v1/IntegrationPackages?$format=json&$orderby=Name";
		completeURL = "https://" + tenant.getTMNHost() + apiUri;

		HttpResponse packageInfo = Utility.doGet(completeURL, tenant);
		String packageInfoBody = EntityUtils.toString(packageInfo.getEntity(), "UTF-8");

		JsonArray packArr = JsonParser.parseString(packageInfoBody).getAsJsonObject().get("d").getAsJsonObject()
				.get("results").getAsJsonArray();

		System.out.println("EVENT : Found " + packArr.size() + " Packages");

		List<scpiPackage> pkge = new ArrayList<scpiPackage>();

		for (JsonElement packageJE : packArr) {

			JsonObject packageJOB = packageJE.getAsJsonObject();

			scpiPackage pkg = new scpiPackage();

			pkg.setDisplayName(packageJOB.get("Name").getAsString());
			pkg.setTechnicalName(packageJOB.get("Id").getAsString());
			pkg.setCreatedBy(packageJOB.get("CreatedBy").getAsString());
			pkg.setCreatedAt(packageJOB.get("CreationDate").getAsString());
			pkge.add(pkg);
		}

		return pkge;

	}

	public static List<scpiIFLOW> getIflow(String packageID, tenantConfiguration tenant) throws Exception {

//		String apiUri = String.format(
//				"/itspaces/odata/1.0/workspace.svc/ContentPackages('%s')/Artifacts?$orderby=DisplayName", packageID);
		String apiUri = String.format(
				"/api/v1/IntegrationPackages('%s')/IntegrationDesigntimeArtifacts?$format=json&$orderby=Name",
				packageID);
		String completeURL = "https://" + tenant.getTMNHost() + apiUri;

		HttpResponse iflowInfo = Utility.doGet(completeURL, tenant);
		String iflowInfoBody = EntityUtils.toString(iflowInfo.getEntity(), "UTF-8");

		JsonArray iflowArr = JsonParser.parseString(iflowInfoBody).getAsJsonObject().get("d").getAsJsonObject()
				.get("results").getAsJsonArray();

		List<scpiIFLOW> iflow = new ArrayList<scpiIFLOW>();

		for (JsonElement iflowJE : iflowArr) {

			JsonObject iflowJOB = iflowJE.getAsJsonObject();

			scpiIFLOW iFlw = new scpiIFLOW();

			iFlw.setDisplayName(iflowJOB.get("Name").getAsString());
			iFlw.setName(iflowJOB.get("Id").getAsString());
			iFlw.setType("IFlow");
			iFlw.setVersion(iflowJOB.get("Version").getAsString());
			iFlw.setCreatedBy(null);
			iFlw.setCreatedAt(null);
			iFlw.setModifiedBy(null);
			iFlw.setModifiedAt(null);

			iflow.add(iFlw);
		}

		Collections.sort(iflow);
		return iflow;
	}

}
