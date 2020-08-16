package com.sanv.scpi.model;

public class scpiIFLOW {
	
	private String DisplayName;
	private String Name;
	private String Version;
	private String CreatedBy;
	private String CreatedAt;
	private String ModifiedBy;
	private String ModifiedAt;
	private String Type;
	
	public void setDisplayName(String inpVal) {
		this.DisplayName = inpVal;
	}

	public void setName(String inpVal) {
		this.Name = inpVal;
	}
	
	public void setVersion(String inpVal) {
		this.Version = inpVal;
	}
	public void setCreatedBy(String inpVal) {
		this.CreatedBy = inpVal;
	}
	public void setCreatedAt(String inpVal) {		
		String epochString = inpVal.substring(inpVal.indexOf('(')+1, inpVal.indexOf(')'));
		long epoch = Long.parseLong( epochString );		
		this.CreatedAt = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (epoch));
	}
	public void setModifiedBy(String inpVal) {
		
		this.ModifiedBy = inpVal;
	}
	public void setModifiedAt(String inpVal) {
		String epochString = inpVal.substring(inpVal.indexOf('(')+1, inpVal.indexOf(')'));
		long epoch = Long.parseLong( epochString );		
		this.ModifiedAt = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (epoch));
	}
	public void setType(String inpVal) {
		this.Type = inpVal;
	}
}
