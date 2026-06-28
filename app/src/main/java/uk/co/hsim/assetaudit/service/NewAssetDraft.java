package uk.co.hsim.assetaudit.service;

public final class NewAssetDraft {
    private final String sessionId;
    private final String assetTagId;
    private final String department;
    private final String description;
    private final String status;
    private final String site;
    private final String location;
    private final String category;
    private final String subCategory;
    private final String owner;
    private final String primaryUser;
    private final String notes;

    public NewAssetDraft(String sessionId, String assetTagId, String department, String description,
                         String status, String site, String location, String category, String subCategory,
                         String owner, String primaryUser, String notes) {
        this.sessionId = clean(sessionId);
        this.assetTagId = clean(assetTagId);
        this.department = clean(department);
        this.description = clean(description);
        this.status = clean(status);
        this.site = clean(site);
        this.location = clean(location);
        this.category = clean(category);
        this.subCategory = clean(subCategory);
        this.owner = clean(owner);
        this.primaryUser = clean(primaryUser);
        this.notes = clean(notes);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAssetTagId() {
        return assetTagId;
    }

    public String getDepartment() {
        return department;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getSite() {
        return site;
    }

    public String getLocation() {
        return location;
    }

    public String getCategory() {
        return category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public String getOwner() {
        return owner;
    }

    public String getPrimaryUser() {
        return primaryUser;
    }

    public String getNotes() {
        return notes;
    }
}
