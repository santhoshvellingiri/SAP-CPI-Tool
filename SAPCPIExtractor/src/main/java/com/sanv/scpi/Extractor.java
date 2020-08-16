package com.sanv.scpi;

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
import com.sanv.scpi.enabler.Runtime;
import com.sanv.scpi.model.scpiPackageWithIFlow;
import com.sanv.scpi.model.scpiRuntimeIflow;
import com.sanv.scpi.model.tenantConfiguration;

public class Extractor {

	static tenantConfiguration src;
	static tenantConfiguration trg;

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
		option.addOption("to", true, "Extraction Mode");

		try {

			CommandLine comlin = defaultParser.parse(option, args);

			String mode = comlin.getOptionValue("mode");
			String from = comlin.getOptionValue("from");
//			String to = comlin.getOptionValue("to");

			verifyMode(mode);

			src = getTenant(from, tc);
//			trg = getTenant(to, tc);

			if (src == null)
				throw new Exception("Incorrect Source or Target ID");

			String fileName = null;
			// String exVar = comlin.getOptionValue("resolveExtVar");

			switch (mode) {
			case "designtime":
				List<scpiPackageWithIFlow> res = Design.getPackagewithIFlow(src);
				fileName = src.getId() + "_design.json";
				Utility.writeFile(fileName, res);
				break;
			case "runtime":
				List<scpiRuntimeIflow> runifl = Runtime.getRuntimeData(src);
				fileName = src.getId() + "_runtime.json";
				Utility.writeFile(fileName, runifl);
				break;
			case "configuration":
				Object iFlConfig = Config.getAllConfigData("Y", src);
				fileName = src.getId() + "_conf.json";
				Utility.writeFile(fileName, iFlConfig);
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
		allowedMode = Arrays.asList("designtime", "runtime", "configuration");
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
