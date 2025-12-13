package io.sekretess.client.response;


public class ConsumerKeysResponse {
    private String username;
    private String ik;
    private String opk;
    private int regID;
    private String spkSignature;
    private String spk;
    private String spkID;
    private String pqSpk;
    private String pqSpkID;
    private String pqSpkSignature;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIk() {
        return ik;
    }

    public void setIk(String ik) {
        this.ik = ik;
    }

    public String getOpk() {
        return opk;
    }

    public void setOpk(String opk) {
        this.opk = opk;
    }

    public int getRegID() {
        return regID;
    }

    public void setRegID(int regID) {
        this.regID = regID;
    }

    public String getSpkSignature() {
        return spkSignature;
    }

    public void setSpkSignature(String spkSignature) {
        this.spkSignature = spkSignature;
    }

    public String getSpk() {
        return spk;
    }

    public void setSpk(String spk) {
        this.spk = spk;
    }

    public String getSpkID() {
        return spkID;
    }

    public void setSpkID(String spkID) {
        this.spkID = spkID;
    }

    public String getPqSpk() {
        return pqSpk;
    }

    public void setPqSpk(String pqSpk) {
        this.pqSpk = pqSpk;
    }

    public String getPqSpkID() {
        return pqSpkID;
    }

    public void setPqSpkID(String pqSpkID) {
        this.pqSpkID = pqSpkID;
    }

    public String getPqSpkSignature() {
        return pqSpkSignature;
    }

    public void setPqSpkSignature(String pqSpkSignature) {
        this.pqSpkSignature = pqSpkSignature;
    }

    @Override
    public String toString() {
        return "ConsumerKeysResponse{" +
                "username='" + username + '\'' +
                ", ik='" + ik + '\'' +
                ", opk='" + opk + '\'' +
                ", regId=" + regID +
                ", spkSignature='" + spkSignature + '\'' +
                ", spk='" + spk + '\'' +
                ", spkId='" + spkID + '\'' +
                ", pqspk='" + pqSpk + '\'' +
                ", pqspkID='" + pqSpkID + '\'' +
                ", pqSpkSignature='" + pqSpkSignature + '\'' +
                '}';
    }
}
