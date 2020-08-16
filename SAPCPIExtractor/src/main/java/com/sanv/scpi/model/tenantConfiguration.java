package com.sanv.scpi.model;

public class tenantConfiguration {

	private String Id;
	private String Name;
	private String TMNHost;
	private String IFLHost;
	private String User;
	private String password;

	// Getter Methods

	public String getId() {
		return Id;
	}

	public String getName() {
		return Name;
	}

	public String getTMNHost() {
		return TMNHost;
	}

	public String getIFLHost() {
		return IFLHost;
	}

	public String getUser() {
		return User;
	}

	public String getPassword() {
		return password;
	}

	// Setter Methods

	public void setId(String Id) {
		this.Id = Id;
	}

	public void setName(String Name) {
		this.Name = Name;
	}

	public void setTMNHost(String TMNHost) {
		this.TMNHost = TMNHost;
	}

	public void setIFLHost(String IFLHost) {
		this.IFLHost = IFLHost;
	}

	public void setUser(String User) {
		this.User = User;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
