package server.model;

public class DCI {
    private final String RequestHash;
    private final String DCI;
    private final String DCIHash;
    private final String DCISigAlgorithm;
    private final String DCIStatus;

    public DCI(String requestHash, String dci, String dciHash, String dciSigAlgorithm, String dciStatus) {
        RequestHash = requestHash;
        DCI = dci;
        DCIHash = dciHash;
        DCISigAlgorithm = dciSigAlgorithm;
        DCIStatus = dciStatus;
    }

    public String getRequestHash() {
        return RequestHash;
    }

    public String getDCI() {
        return DCI;
    }

    public String getDCIHash() {
        return DCIHash;
    }

    public String getDCISigAlgorithm() {
        return DCISigAlgorithm;
    }

    public String getDCIStatus() {
        return DCIStatus;
    }
}
