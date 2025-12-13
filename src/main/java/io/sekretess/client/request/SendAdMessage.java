package io.sekretess.client.request;

public class SendAdMessage {
    private String text;
    private String businessName;

    public SendAdMessage(String text, String businessName) {
        this.text = text;
        this.businessName = businessName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
}
