package org.opentripplanner.model;

import java.io.Serializable;

public class KeyValue implements Serializable {

    private final String key;

    private final String typeOfKey;

    private final String value;

    public KeyValue(String key, String typeOfKey, String value) {
        this.key = key;
        this.typeOfKey = typeOfKey;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getTypeOfKey() {
        return typeOfKey;
    }

    public String getValue() {
        return value;
    }
}
