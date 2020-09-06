package com.sanv.scpi.model;

import java.util.ArrayList;
import java.util.List;

public class tenantConfiguration {

	private String Id;
	private String Name;
	private String TMNHost;
	private String IFLHost;
	private String User;
	private String password;
	private List <String> Connection = new ArrayList < String > ();

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
	
	public List<String> getConnection() {
		return Connection;
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
	
	public void setConnection(List<String> Connection) {
		this.Connection = Connection;
	}

}
