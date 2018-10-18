package org.opentripplanner.routing.core;

/**
 * Builder to allow editing of package protected fields in StateData by classes outside package.
 *
 * This is dirty but necessary to achieve temporary fix for joining paths without to much interference in existing code.
 *
 */
public class StateDataBuilder {

    private StateData stateData;

    public StateDataBuilder(StateData template) {
        this.stateData = template.clone();
    }

    public StateDataBuilder withEverBoarded(boolean everBoarded) {
        stateData.everBoarded= everBoarded;
        return this;
    }

    public StateData build() {
        return stateData;
    }
}
