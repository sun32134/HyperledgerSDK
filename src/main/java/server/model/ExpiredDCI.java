package server.model;

public class ExpiredDCI {
    private final String Hash;

    public ExpiredDCI(String hash) {
        Hash = hash;
    }

    public String getHash() {
        return Hash;
    }
}
