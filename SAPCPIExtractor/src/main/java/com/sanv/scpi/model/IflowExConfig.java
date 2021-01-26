package com.sanv.scpi.model;

import java.util.HashMap;
import java.util.TreeMap;

public class IflowExConfig implements Comparable<IflowExConfig>{

	private String packageName;
	private String iflowName;
	private TreeMap<String, String> ExternalConfigurationObject;
	//ExConfig ExternalConfigurationObject;

	// Getter Methods

	public String getPackageName() {
		return packageName;
	}

	public String getIflowName() {
		return iflowName;
	}
	
	public TreeMap<String, String> getExternalConfigurationObject() {
		return ExternalConfigurationObject;
	}

	// Setter Methods

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public void setIflowName(String iflowName) {
		this.iflowName = iflowName;
	}

	public void setExternalConfiguration(TreeMap<String,String> mval) {
		this.ExternalConfigurationObject = mval;
	}
	
	@Override
	public int compareTo(IflowExConfig o) {
		// TODO Auto-generated method stub
		int res = this.getPackageName().compareToIgnoreCase(o.getPackageName());
		if (res == 0 )
			res = this.getIflowName().compareToIgnoreCase(o.getIflowName());		
		return res;
	}
	
}
