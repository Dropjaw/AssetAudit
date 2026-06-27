package uk.co.hsim.assetaudit.importfile;

public final class AssetImportRow {
    private final int sourceRowNumber;
    private final String assetTagId;
    private final String department;
    private final String description;
    private final String status;
    private final String brand;
    private final String model;
    private final String site;
    private final String location;
    private final String category;
    private final String subCategory;
    private final String primaryUser;
    private final String owner;
    private final String auditDetails;

    public AssetImportRow(int sourceRowNumber, String assetTagId, String department, String description,
                          String status, String brand, String model, String site, String location,
                          String category, String subCategory, String primaryUser, String owner,
                          String auditDetails) {
        this.sourceRowNumber = sourceRowNumber;
        this.assetTagId = clean(assetTagId);
        this.department = clean(department);
        this.description = clean(description);
        this.status = clean(status);
        this.brand = clean(brand);
        this.model = clean(model);
        this.site = clean(site);
        this.location = clean(location);
        this.category = clean(category);
        this.subCategory = clean(subCategory);
        this.primaryUser = clean(primaryUser);
        this.owner = clean(owner);
        this.auditDetails = clean(auditDetails);
    }

    public int getSourceRowNumber() {
        return sourceRowNumber;
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

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
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

    public String getPrimaryUser() {
        return primaryUser;
    }

    public String getOwner() {
        return owner;
    }

    public String getAuditDetails() {
        return auditDetails;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
