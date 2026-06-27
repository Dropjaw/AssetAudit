package uk.co.hsim.assetaudit.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import uk.co.hsim.assetaudit.domain.enums.AuditStatus;

@Entity(
        tableName = "asset",
        foreignKeys = @ForeignKey(
                entity = AuditSessionEntity.class,
                parentColumns = "session_id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"session_id", "asset_tag_id"}, unique = true),
                @Index(value = {"session_id", "department", "audit_status"})
        }
)
public class AssetEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "asset_id")
    public long assetId;

    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId;

    @NonNull
    @ColumnInfo(name = "asset_tag_id")
    public String assetTagId;

    @ColumnInfo(name = "department")
    public String department;

    @ColumnInfo(name = "previous_department")
    public String previousDepartment;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "brand")
    public String brand;

    @ColumnInfo(name = "model")
    public String model;

    @ColumnInfo(name = "site")
    public String site;

    @ColumnInfo(name = "location")
    public String location;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "sub_category")
    public String subCategory;

    @ColumnInfo(name = "owner")
    public String owner;

    @ColumnInfo(name = "primary_user")
    public String primaryUser;

    @ColumnInfo(name = "audit_details")
    public String auditDetails;

    @ColumnInfo(name = "source_row_number")
    public Integer sourceRowNumber;

    @NonNull
    @ColumnInfo(name = "audit_status")
    public AuditStatus auditStatus = AuditStatus.NOT_AUDITED;

    @ColumnInfo(name = "created_during_audit")
    public boolean createdDuringAudit;

    @ColumnInfo(name = "updated_at_utc")
    public long updatedAtUtc;

    @ColumnInfo(name = "imported_at_utc")
    public Long importedAtUtc;

    public AssetEntity(@NonNull String sessionId, @NonNull String assetTagId, String department,
                       String previousDepartment, String description, String status, String site,
                       String location, String category, String subCategory, String owner,
                       String primaryUser, @NonNull AuditStatus auditStatus,
                       boolean createdDuringAudit, long updatedAtUtc) {
        this.sessionId = sessionId;
        this.assetTagId = assetTagId;
        this.department = department;
        this.previousDepartment = previousDepartment;
        this.description = description;
        this.status = status;
        this.brand = "";
        this.model = "";
        this.site = site;
        this.location = location;
        this.category = category;
        this.subCategory = subCategory;
        this.owner = owner;
        this.primaryUser = primaryUser;
        this.auditDetails = "";
        this.sourceRowNumber = null;
        this.auditStatus = auditStatus;
        this.createdDuringAudit = createdDuringAudit;
        this.updatedAtUtc = updatedAtUtc;
        this.importedAtUtc = null;
    }
}
