package example.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class Message {

    @JsonProperty
    private String qName;

    @JsonProperty
    private String message;

    public Message() {
    }

    public Message(String qName, String message) {
        this.qName = qName;
        this.message = message;
    }

    public String getqName() {
        return qName;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("qName", qName)
            .add("message", message)
            .toString();
    }
}
