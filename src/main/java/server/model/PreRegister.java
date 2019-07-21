package server.model;

public class PreRegister {
    private final String requestHash;
    private final String CopyrightOwner;
    private final String Title;
    private final String Type;
    private final String RequestTime;
    private final String CreationCompleteTime;
    private final String FirstPublishTime;
    private final String RequestUserId;
    private final String Fingerprint;
    private final String DownloadUrl;
    private final String SigAlgorithm;


    public PreRegister(String requestHash, String copyrightOwner, String title, String type, String requestTime, String creationCompleteTime, String firstPublishTime, String requestUserId, String fingerprint, String downloadUrl, String sigAlgorithm){
        this.requestHash = requestHash;
        CopyrightOwner = copyrightOwner;
        Title = title;
        Type = type;
        RequestTime = requestTime;
        CreationCompleteTime = creationCompleteTime;
        FirstPublishTime = firstPublishTime;
        RequestUserId = requestUserId;
        Fingerprint = fingerprint;
        DownloadUrl = downloadUrl;
        SigAlgorithm = sigAlgorithm;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getCopyrightOwner() {
        return CopyrightOwner;
    }

    public String getTitle() {
        return Title;
    }

    public String getType() {
        return Type;
    }

    public String getRequestTime() {
        return RequestTime;
    }

    public String getCreationCompleteTime() {
        return CreationCompleteTime;
    }

    public String getFirstPublishTime() {
        return FirstPublishTime;
    }

    public String getRequestUserId() {
        return RequestUserId;
    }

    public String getFingerprint() {
        return Fingerprint;
    }

    public String getDownloadUrl() {
        return DownloadUrl;
    }

    public String getSigAlgorithm() {
        return SigAlgorithm;
    }
}
