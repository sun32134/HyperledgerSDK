package server.model;

public class Greeting {
    private final String name;
    private final long id;
    public Greeting(long id, String name){
        this.id = id;
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }
}
