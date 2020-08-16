package com.sanv.scpi.model;

public class scpiIFLOW implements Comparable<scpiIFLOW>{

	private String DisplayName;
	private String Name;
	private String Version;
	private String CreatedBy;
	private String CreatedAt;
	private String ModifiedBy;
	private String ModifiedAt;
	private String Type;

	// Getter Methods

	public String getDisplayName() {
		return DisplayName;
	}

	public String getName() {
		return Name;
	}

	public String getVersion() {
		return Version;
	}

	public String getCreatedBy() {
		return CreatedBy;
	}

	public String getCreatedAt() {
		return CreatedAt;
	}

	public String getModifiedBy() {
		return ModifiedBy;
	}

	public String getModifiedAt() {
		return ModifiedAt;
	}

	public String getType() {
		return Type;
	}

	// Setter Methods

	public void setDisplayName(String DisplayName) {
		this.DisplayName = DisplayName;
	}

	public void setName(String Name) {
		this.Name = Name;
	}

	public void setVersion(String Version) {
		this.Version = Version;
	}

	public void setCreatedBy(String CreatedBy) {
		this.CreatedBy = CreatedBy;
	}

	public void setCreatedAt(String CreatedAt) {
		String epochString = CreatedAt.substring(CreatedAt.indexOf('(') + 1, CreatedAt.indexOf(')'));
		long epoch = Long.parseLong(epochString);
		this.CreatedAt = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date(epoch));
	}

	public void setModifiedBy(String ModifiedBy) {
		this.ModifiedBy = ModifiedBy;
	}

	public void setModifiedAt(String ModifiedAt) {
		String epochString = ModifiedAt.substring(ModifiedAt.indexOf('(')+1, ModifiedAt.indexOf(')'));
		long epoch = Long.parseLong( epochString );		
		this.ModifiedAt = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (epoch));
	}

	public void setType(String Type) {
		this.Type = Type;
	}

	@Override
	public int compareTo(scpiIFLOW o) {
		return this.getDisplayName().compareToIgnoreCase(o.getDisplayName());
	}

}
