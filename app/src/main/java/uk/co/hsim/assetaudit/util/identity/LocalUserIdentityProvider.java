package uk.co.hsim.assetaudit.util.identity;

public final class LocalUserIdentityProvider implements UserIdentityProvider {
    @Override
    public String getDisplayName() {
        return "Local user";
    }
}
