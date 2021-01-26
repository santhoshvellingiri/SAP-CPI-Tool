package com.sanv.scpi.model;

import java.util.List;

public class dsObj {
	
	private String storeName;
	private List<String> ids;
	private String qualifier;
	
	public void setStoreName(String name) {
		this.storeName = name;
	}
	
	public void setIDs(List<String> inp) {
		this.ids = inp;
	}
	
	public void setqualifier(String inp) {
		this.qualifier = inp;
	}

	public String getstoreName() {
		return this.storeName;
	}
	
	public List<String> getIDs() {
		return this.ids;
	}
	
	public String getqualifier() {
		return this.qualifier;
	}
}
