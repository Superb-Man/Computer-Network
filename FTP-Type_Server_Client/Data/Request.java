package Data;

import java.io.Serializable;

public class Request implements Serializable {
    private String from;
    private String description;
    private int id = -1;

    public Request(String from, String description){
        this.from = from;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setId(int id) {
        this.id = id;
    }
}