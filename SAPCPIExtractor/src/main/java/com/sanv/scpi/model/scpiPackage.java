package com.sanv.scpi.model;

public class scpiPackage {

	private String DisplayName;
	private String TechnicalName;
	private String CreatedBy;
	private String CreatedAt;

	// Getter Methods

	public String getDisplayName() {
		return DisplayName;
	}

	public String getTechnicalName() {
		return TechnicalName;
	}

	public String getCreatedBy() {
		return CreatedBy;
	}

	public String getCreatedAt() {
		return CreatedAt;
	}	


	public void setDisplayName(String inpVal) {
		this.DisplayName = inpVal;
	}

	public void setTechnicalName(String inpVal) {
		this.TechnicalName = inpVal;
	}

	public void setCreatedBy(String inpVal) {
		this.CreatedBy = inpVal;
	}

	public void setCreatedAt(String inpVal) {
		String epochString = inpVal; //.substring(inpVal.indexOf('(') + 1, inpVal.indexOf(')'));
		long epoch = Long.parseLong(epochString);
		this.CreatedAt = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date(epoch));
		// this.CreatedAt = epochString;
	}
	
	public void setCreatAt(String inpVal) {
		 this.CreatedAt = inpVal;
	}

}
