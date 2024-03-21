package edu.fspt.demo2224_cloudserver;

public class Setting {
    public String getName() {
        return name;
    }

    public String getSptag() {
        return sptag;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSptag(String sptag) {
        this.sptag = sptag;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public String getVal() {

        return val;
    }

    String name;
    String sptag;
    String val;

    public Setting(String name, String sptag, String val) {
        this.name = name;
        this.sptag = sptag;
        this.val = val;
    }

}
