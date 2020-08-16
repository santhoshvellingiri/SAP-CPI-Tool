package com.sanv.scpi.model;

public class scpiRuntimeIflow implements Comparable<scpiRuntimeIflow>{

	private String id;
	private String packageName;
	private String symbolicName;
	private String name;
	private String version;
	private String deployedBy;
	private String deployedOn;
	private String type;
	private String queueURI;
	private String endpointUri;
	private String logLevel;

	// Getter Methods
	
	public scpiRuntimeIflow() {
		this.id = "";
		this.packageName = "";
		this.symbolicName = "";
		this.name = "";
		this.version = "";
		this.deployedBy = "";
		this.deployedOn = "";
		this.type = "";
		this.queueURI = "";
		this.endpointUri = "";
		this.logLevel = "";
	}

	public String getId() {
		return id;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getDeployedBy() {
		return deployedBy;
	}

	// Setter Methods

	public String getDeployedOn() {
		return deployedOn;
	}

	public String getType() {
		return type;
	}

	public String getQueueURI() {
		return queueURI;
	}

	public String getEndpointUri() {
		return endpointUri;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setpackageName(String inpVal) {
		this.packageName = inpVal;
	}

	public void setsymbolicName(String inpVal) {
		this.symbolicName = inpVal;
	}

	public void setname(String inpVal) {
		this.name = inpVal;
	}

	public void setversion(String inpVal) {
		this.version = inpVal;
	}

	public void setdeployedBy(String inpVal) {
		this.deployedBy = inpVal;
	}

	public void setdeployedOn(String inpVal) {
		this.deployedOn = inpVal;
	}

	public void settype(String inpVal) {
		this.type = inpVal;
	}

	public void setqueueURI(String inpVal) {
		this.queueURI = inpVal;
	}

	public void setendpointUri(String inpVal) {
		this.endpointUri = inpVal;
	}

	public void setlogLevel(String inpVal) {
		this.logLevel = inpVal;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int compareTo(scpiRuntimeIflow o) {
		// TODO Auto-generated method stub
		int res = this.getPackageName().compareToIgnoreCase(o.getPackageName());
		if (res == 0 )
			res = this.getName().compareToIgnoreCase(o.getName());		
		return res;
	}
}
