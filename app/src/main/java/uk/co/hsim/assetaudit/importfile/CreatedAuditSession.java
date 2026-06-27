package uk.co.hsim.assetaudit.importfile;

public final class CreatedAuditSession {
    private final String sessionId;
    private final int assetCount;
    private final int departmentCount;

    public CreatedAuditSession(String sessionId, int assetCount, int departmentCount) {
        this.sessionId = sessionId;
        this.assetCount = assetCount;
        this.departmentCount = departmentCount;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getAssetCount() {
        return assetCount;
    }

    public int getDepartmentCount() {
        return departmentCount;
    }
}
