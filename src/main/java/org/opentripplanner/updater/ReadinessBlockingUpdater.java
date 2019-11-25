package org.opentripplanner.updater;

public class ReadinessBlockingUpdater {
    protected boolean blockReadinessUntilInitialized;
    protected boolean isInitialized;
    /**
     * The type name in the preferences
     */
    protected String type;

    public boolean isReady() {
        if (blockReadinessUntilInitialized) {
            return isInitialized;
        }
        return true;
    }

    public String getType() {
        return type;
    }
}
