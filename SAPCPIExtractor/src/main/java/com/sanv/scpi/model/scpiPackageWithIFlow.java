package com.sanv.scpi.model;

import java.util.ArrayList;
import java.util.List;

public class scpiPackageWithIFlow extends scpiPackage implements Comparable<scpiPackageWithIFlow> {
	
	private List<scpiIFLOW> scpiIFLOW;
	
	public scpiPackageWithIFlow() {
		scpiIFLOW = new ArrayList<scpiIFLOW>();
	}	
	
	public List<scpiIFLOW> getscpiIFLOW() {
		return scpiIFLOW;
	}
	
	public void addcpiIFLOW(scpiIFLOW res) {
		this.scpiIFLOW.add(res);
	}

	public void setscpiIFLOW(List<scpiIFLOW> iflow) {
		// TODO Auto-generated method stub
		scpiIFLOW = iflow;
	}

	@Override
	public int compareTo(scpiPackageWithIFlow o) {
		// TODO Auto-generated method stub
		return this.getDisplayName().toLowerCase().compareTo(o.getDisplayName().toLowerCase());
	}	

}
