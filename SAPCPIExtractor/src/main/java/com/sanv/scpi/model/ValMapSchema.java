package com.sanv.scpi.model;

public class ValMapSchema {
    private String srcAgency;
    private String srcId;
    private String tgtAgency;
    private String tgtId;

    public String getSrcAgency() {
        return srcAgency;
    }

    public void setSrcAgency(String srcAgency) {
        this.srcAgency = srcAgency;
    }

    public String getSrcId() {
        return srcId;
    }

    public void setSrcId(String srcId) {
        this.srcId = srcId;
    }

    public String getTgtAgency() {
        return tgtAgency;
    }

    public void setTgtAgency(String tgtAgency) {
        this.tgtAgency = tgtAgency;
    }

    public String getTgtId() {
        return tgtId;
    }

    public void setTgtId(String tgtId) {
        this.tgtId = tgtId;
    }

}
