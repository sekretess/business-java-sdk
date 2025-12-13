package io.sekretess.client.request;

public class SendMessage {
    private String text;
    private String consumerName;
    private String type;

    public SendMessage(String text, String consumerName, String type) {
        this.text = text;
        this.consumerName = consumerName;
        this.type = type;
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

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }
}
