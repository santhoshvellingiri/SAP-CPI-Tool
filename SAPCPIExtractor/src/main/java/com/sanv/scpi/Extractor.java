package com.sanv.scpi;

import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import com.google.gson.Gson;
import com.sanv.scpi.enabler.Config;
import com.sanv.scpi.enabler.Design;
import com.sanv.scpi.enabler.Downloader;
import com.sanv.scpi.enabler.ExternalConfiguration;
import com.sanv.scpi.enabler.Runtime;
import com.sanv.scpi.model.IflowExConfig;
import com.sanv.scpi.model.scpiPackageWithIFlow;
import com.sanv.scpi.model.scpiRuntimeIflow;
import com.sanv.scpi.model.tenantConfiguration;

public class Extractor {

	static tenantConfiguration src;

	public static void main(String[] args) throws Exception {

		// TODO Auto-generated method stub

		System.out.println("INFO  : Tool Version " + Extractor.class.getPackage().getImplementationVersion());

		String cDir = System.getProperty("user.dir") + "/config.json";
		File currentDir = new File(cDir);
		FileReader fileReader = new FileReader(currentDir);
		Gson gson = new Gson();
		tenantConfiguration[] tc = (tenantConfiguration[]) gson.fromJson(fileReader, tenantConfiguration[].class);

		long startTime = System.nanoTime();

		DefaultParser defaultParser = new DefaultParser();

		Options option = new Options();
		option.addOption("mode", true, "Extraction Mode");
		option.addOption("from", true, "Extraction Mode");
		option.addOption("s_password", true, "Password");
		option.addOption("name", true, "Name");

		try {

			CommandLine comlin = defaultParser.parse(option, args);

			String mode = comlin.getOptionValue("mode");
			String from = comlin.getOptionValue("from");
			String s_pass = comlin.getOptionValue("s_password");
			String name = comlin.getOptionValue("name");
//			String to = comlin.getOptionValue("to");

			verifyMode(mode);

			src = getTenant(from, tc);
//			trg = getTenant(to, tc);

			if (src == null)
				throw new Exception("Incorrect Source or Target ID");

			if (src.getPassword() == null) {
				Console console = System.console();
				s_pass = new String(
						console.readPassword("Enter Password of User " + src.getUser() + " : ", new Object[0]));
				src.setPassword(s_pass);
			}

			String folderName = String.valueOf(src.getName()) + "/";
			String fileName = null;
			File file = null;
			// String exVar = comlin.getOptionValue("resolveExtVar");

			switch (mode) {
			case "designtime":
				List<scpiPackageWithIFlow> res = Design.getPackagewithIFlow(src);
				fileName = src.getId() + "_design.json";
				file = new File(folderName, fileName);
				Utility.writeFile(file, res);
				break;
			case "runtime":
				List<scpiRuntimeIflow> runifl = Runtime.getRuntimeData(src);
				fileName = src.getId() + "_runtime.json";
				file = new File(folderName, fileName);
				Utility.writeFile(file, runifl);
				break;
			case "configuration":
				Object iFlConfig = Config.getAllConfigData("Y", src);
				fileName = src.getId() + "_conf.json";
				file = new File(folderName, fileName);
				Utility.writeFile(file, iFlConfig);
				break;
			case "downloadAllPackage":
				Downloader.downloadAllPackage(src);
				break;
			case "downloadPackage":
				Downloader.downloadPackage(name, src);
				break;
			case "downloadDeployedIFlow":
				Downloader.downloadDeployedIflows(src);
				break;
			case "getConnections":
				Object iFlTarConfig = Config.getTargetConnection(src);
				fileName = src.getId() + "_ConnectionDetails.json";
				file = new File(folderName, fileName);
				Utility.writeFile(file, iFlTarConfig);
				break;
			case "downloadExtConfiguration":
				List<IflowExConfig> XConLs = ExternalConfiguration.downloadAllExConf(src);
				fileName = src.getId() + "_XConfiguration.json";
				file = new File(folderName, fileName);
				Utility.writeFile(file, XConLs);
				break;
			}

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

	public static void verifyMode(String mode) throws Exception {
		List<String> allowedMode = new ArrayList<String>();
		allowedMode = Arrays.asList("designtime", "runtime", "configuration","downloadAllPackage","downloadPackage","downloadDeployedIFlow","getConnections", "downloadExtConfiguration");
		if (!allowedMode.contains(mode))
			throw new Exception("Incorrect Mode");
	}

	public static tenantConfiguration getTenant(String name, tenantConfiguration[] tc) throws Exception {

		for (tenantConfiguration tc2 : tc) {
			if (tc2.getId().equals(name))
				return tc2;
		}
		return null;
	}
}
