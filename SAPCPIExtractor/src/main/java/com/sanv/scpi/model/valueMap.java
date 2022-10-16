package com.sanv.scpi.model;
import java.util.List;

public class valueMap implements Comparable<valueMap>{
    private String valueMapName;
    private String valueMapID;
    private String packageId;
    private List<ValMapSchema> valMapSchema = null;

    public String getValueMapName() {
        return valueMapName;
    }

    public void setValueMapName(String valueMapName) {
        this.valueMapName = valueMapName;
    }

    public String getValueMapID() {
        return valueMapID;
    }

    public void setValueMapID(String valueMapID) {
        this.valueMapID = valueMapID;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public List<ValMapSchema> getValMapSchema() {
        return valMapSchema;
    }

    public void setValMapSchema(List<ValMapSchema> valMapSchema) {
        this.valMapSchema = valMapSchema;
    }

    @Override
    public int compareTo(valueMap o) {
        // TODO Auto-generated method stub
        return this.getValueMapName().toLowerCase().compareTo(o.getValueMapName().toLowerCase());
    }
}
