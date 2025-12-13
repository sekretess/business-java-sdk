package io.sekretess.client.response;


public record ConsumerKeysResponse(String username,
                                   String ik,
                                   String opk,
                                   String spkSignature,
                                   String spk,
                                   String spkID,
                                   String pqSpk,
                                   String pqSpkID,
                                   String pqSpkSignature,
                                   int regID) {


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
