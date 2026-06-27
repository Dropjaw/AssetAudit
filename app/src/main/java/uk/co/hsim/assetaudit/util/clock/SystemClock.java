package uk.co.hsim.assetaudit.util.clock;

public final class SystemClock implements Clock {
    @Override
    public long nowUtcMillis() {
        return java.lang.System.currentTimeMillis();
    }
}
