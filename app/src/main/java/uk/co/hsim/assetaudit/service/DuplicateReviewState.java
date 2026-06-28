package uk.co.hsim.assetaudit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.co.hsim.assetaudit.data.entity.AssetEntity;
import uk.co.hsim.assetaudit.data.entity.AuditEventEntity;

public final class DuplicateReviewState {
    private final AssetEntity asset;
    private final List<AuditEventEntity> recentEvents;

    public DuplicateReviewState(AssetEntity asset, List<AuditEventEntity> recentEvents) {
        this.asset = asset;
        this.recentEvents = Collections.unmodifiableList(new ArrayList<>(recentEvents));
    }

    public AssetEntity getAsset() {
        return asset;
    }

    public List<AuditEventEntity> getRecentEvents() {
        return recentEvents;
    }
}
