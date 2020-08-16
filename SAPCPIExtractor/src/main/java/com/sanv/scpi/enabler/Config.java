package com.sanv.scpi.enabler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.HttpResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sanv.scpi.Utility;
import com.sanv.scpi.model.scpiRuntimeIflow;
import com.sanv.scpi.model.tenantConfiguration;

public class Config {
	
	public static ArrayList<TreeMap<String, String>> getAllConfigData(String exVar, tenantConfiguration tenant)
			throws Exception {

		System.out.println("EVENT : Connecting to Host " + tenant.getTMNHost() + " with user " + tenant.getUser());

		List<scpiRuntimeIflow> runIFo = Runtime.getRuntimeIFlowID(tenant);

		int totalIflow = runIFo.size();

		System.out.println("EVENT : Found " + totalIflow + " IFlows");
		System.out.println("START : Fetching Runtime Information ");

		ArrayList<TreeMap<String, String>> al = new ArrayList<TreeMap<String, String>>();
		TreeMap<String, String> colmName = new TreeMap<String, String>();
		colmName.put("IFlow", "Example");
		al.add(colmName);
		
		runIFo.parallelStream().forEach(aIflo -> {
			try {

				if (aIflo.getType().equals("VALUE_MAPPING"))
					return;
				System.out.format("Event : Extrating Runtime Data of IFlow %s %n", aIflo.getName());
//				ArrayList<TreeMap<String,String>> ret = getIFlowConfigData(aIflo.getSymbolicName(), exVar, tenant);				
//				if (ret != null)
//					al.addAll(ret);
				
				String apiUri = "";
				String completeURL = "";

				apiUri = String.format("/api/v1/IntegrationDesigntimeArtifacts(Id='%s',Version='active')/$value", aIflo.getSymbolicName());
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
				InputStream propertyFile = null;
				InputStream iflowData = null;
				ZipEntry entry = zipIn.getNextEntry();

				while (entry != null) {
					String entryName = entry.getName();
					if (entryName.endsWith(".prop") && exVar.equalsIgnoreCase("y")) {
						propertyFile = Utility.convertZipInputStreamToInputStream(zipIn);
//						System.out.println(convertInputStreamToString(propertyFile));					
					} else if (entryName.endsWith(".iflw")) {
						iflowData = Utility.convertZipInputStreamToInputStream(zipIn);
//						System.out.println(convertInputStreamToString(iflowData));	
					}
					zipIn.closeEntry();
					entry = zipIn.getNextEntry();
				}

				Properties prop = new Properties();
				if (exVar.equalsIgnoreCase("y") && propertyFile != null) {
					prop.load(propertyFile);
				}

				// 7 Parse model xml
				if(iflowData == null)
					return;
				Document document = Utility.processxml(iflowData);
				document.getDocumentElement().normalize();
				Element root = document.getDocumentElement();
				Element collaboration = (Element) root
						.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "collaboration").item(0);
				NodeList messageFlows = collaboration.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL",
						"messageFlow");

				TreeMap<String, String> map = new TreeMap<String, String>();

				for (int tem1 = 0; tem1 < messageFlows.getLength(); tem1++) {

					Node messageFlow = messageFlows.item(tem1);

					if (messageFlow.getNodeType() == Node.ELEMENT_NODE) {

						Element mesgFlowElement = (Element) messageFlow;

						NodeList properties = mesgFlowElement.getElementsByTagNameNS("http:///com.sap.ifl.model/Ifl.xsd",
								"property");

						map = new TreeMap<String, String>();
						map.put("IFlow", aIflo.getName());

						for (int tem2 = 0; tem2 < properties.getLength(); tem2++) {

							Node property = properties.item(tem2);
							if (property.getNodeType() == Node.ELEMENT_NODE) {

								Element propertyElement = (Element) property;

								NodeList NLkey = propertyElement.getElementsByTagName("key");
								NodeList NLvalue = propertyElement.getElementsByTagName("value");

								String key = "";
								String value = "";

								if (NLkey.getLength() > 0)
									key = NLkey.item(0).getTextContent();
								if (NLvalue.getLength() > 0)
									value = NLvalue.item(0).getTextContent();

								if (!colmName.containsKey(key))
									colmName.put(key, "example");

								if (exVar.equalsIgnoreCase("y")) {

									final String regex = "\\{\\{[\\w -<>\\+]*\\}\\}";

									if (value.contains("{{")) {

										final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
										final Matcher matcher = pattern.matcher(value);

										while (matcher.find()) {

											String string1 = matcher.group(0);
//											System.out.println("Full match: " + string1);

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
				

			} catch (Exception e) {
				e.printStackTrace();
			}

		});
		
		
//		for (scpiRuntimeIflow aIflo : runIFo) {
//			if (aIflo.getType().equals("VALUE_MAPPING"))
//				continue;
//			System.out.format("Event : Extrating Runtime Data of IFlow %s %n", aIflo.getName());
//			ArrayList<TreeMap<String, String>> ret = getIFlowConfigData(aIflo.getSymbolicName(), exVar, tenant);
//			if (ret != null)
//				al.addAll(ret);			
//		}
		
		return al;
	}

	public static ArrayList<TreeMap<String,String>> getIFlowConfigData(String Iflowname, String exVar,
			tenantConfiguration tenant) throws Exception {

		String apiUri = "";
		String completeURL = "";

		ArrayList<TreeMap<String, String>> al = new ArrayList<TreeMap<String, String>>();
//		TreeMap<String, String> colmName = new TreeMap<String, String>();
//		colmName.put("IFlow", "Example");
//		al.add(colmName);

		apiUri = String.format("/api/v1/IntegrationDesigntimeArtifacts(Id='%s',Version='active')/$value", Iflowname);
		completeURL = "https://" + tenant.getTMNHost() + apiUri;

		HttpResponse iflowZip;
		iflowZip = Utility.doGet(completeURL, tenant);
		if (iflowZip.getStatusLine().getStatusCode() == 404) {
			System.out.format("Event : Integration design time artifact not found%n");
			return null;
		}

		if (iflowZip.getStatusLine().getStatusCode() == 400) {
			System.out.format("Event : Cannot download the artifact from a configure only package.%n");
			return null;
		}

		ZipInputStream zipIn = new ZipInputStream(iflowZip.getEntity().getContent());
		InputStream propertyFile = null;
		InputStream iflowData = null;
		ZipEntry entry = zipIn.getNextEntry();

		while (entry != null) {
			String entryName = entry.getName();
			if (entryName.endsWith(".prop") && exVar.equalsIgnoreCase("y")) {
				propertyFile = Utility.convertZipInputStreamToInputStream(zipIn);
//				System.out.println(convertInputStreamToString(propertyFile));					
			} else if (entryName.endsWith(".iflw")) {
				iflowData = Utility.convertZipInputStreamToInputStream(zipIn);
//				System.out.println(convertInputStreamToString(iflowData));	
			}
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}

		Properties prop = new Properties();
		if (exVar.equalsIgnoreCase("y") && propertyFile != null) {
			prop.load(propertyFile);
		}

		// 7 Parse model xml
		if(iflowData == null)
			return null;
		Document document = Utility.processxml(iflowData);
		document.getDocumentElement().normalize();
		Element root = document.getDocumentElement();
		Element collaboration = (Element) root
				.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "collaboration").item(0);
		NodeList messageFlows = collaboration.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL",
				"messageFlow");

		TreeMap<String, String> map = new TreeMap<String, String>();

		for (int tem1 = 0; tem1 < messageFlows.getLength(); tem1++) {

			Node messageFlow = messageFlows.item(tem1);

			if (messageFlow.getNodeType() == Node.ELEMENT_NODE) {

				Element mesgFlowElement = (Element) messageFlow;

				NodeList properties = mesgFlowElement.getElementsByTagNameNS("http:///com.sap.ifl.model/Ifl.xsd",
						"property");

				map = new TreeMap<String, String>();
				map.put("IFlow", Iflowname);

				for (int tem2 = 0; tem2 < properties.getLength(); tem2++) {

					Node property = properties.item(tem2);
					if (property.getNodeType() == Node.ELEMENT_NODE) {

						Element propertyElement = (Element) property;

						NodeList NLkey = propertyElement.getElementsByTagName("key");
						NodeList NLvalue = propertyElement.getElementsByTagName("value");

						String key = "";
						String value = "";

						if (NLkey.getLength() > 0)
							key = NLkey.item(0).getTextContent();
						if (NLvalue.getLength() > 0)
							value = NLvalue.item(0).getTextContent();

//						if (!colmName.containsKey(key))
//							colmName.put(key, "example");

						if (exVar.equalsIgnoreCase("y")) {

							final String regex = "\\{\\{[\\w -<>\\+]*\\}\\}";

							if (value.contains("{{")) {

								final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
								final Matcher matcher = pattern.matcher(value);

								while (matcher.find()) {

									String string1 = matcher.group(0);
//									System.out.println("Full match: " + string1);

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
		return al;

	}

}
