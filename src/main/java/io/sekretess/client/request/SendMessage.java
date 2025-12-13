package io.sekretess.client.request;

public class SendMessage {
    private String text;
    private String consumerExchange;
    private String type;

    public SendMessage(String text, String consumerExchange) {
        this.text = text;
        this.consumerExchange = consumerExchange;
    }

    public SendMessage(String text, String consumerExchange,String type) {
        this.text = text;
        this.consumerExchange = consumerExchange;
        this.type=type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getConsumerExchange() {
        return consumerExchange;
    }

    public void setConsumerExchange(String consumerExchange) {
        this.consumerExchange = consumerExchange;
    }
}
