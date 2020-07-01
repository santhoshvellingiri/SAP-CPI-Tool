package com.sanv.scpi;

import java.util.ArrayList;

public class dsObj {
	
	private String storeName;
	private ArrayList<String> ids;
	private String qualifier;
	
	public void setStoreName(String name) {
		this.storeName = name;
	}
	
	public void setIDs(ArrayList<String> inp) {
		this.ids = inp;
	}
	
	public void setqualifier(String inp) {
		this.qualifier = inp;
	}

}
