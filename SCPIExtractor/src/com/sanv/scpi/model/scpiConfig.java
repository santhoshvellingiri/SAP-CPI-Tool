package com.sanv.scpi.model;

public class scpiConfig {

	private String user;
	private String password;
	private String scpiURL;

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSCPIHost() {
		return this.scpiURL;
	}

	public void setSCPIHost(String apiUrl) {
		this.scpiURL = apiUrl;
	}

}
