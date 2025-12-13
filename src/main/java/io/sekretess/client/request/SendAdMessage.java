package io.sekretess.client.request;

public class SendAdMessage {
    private String text;
    private String businessExchange;

    public SendAdMessage(String text, String businessExchange) {
        this.text = text;
        this.businessExchange = businessExchange;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getBusinessExchange() {
        return businessExchange;
    }

    public void setBusinessExchange(String businessExchange) {
        this.businessExchange = businessExchange;
    }
}
