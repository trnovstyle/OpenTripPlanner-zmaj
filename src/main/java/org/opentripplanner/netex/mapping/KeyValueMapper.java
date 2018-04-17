package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.KeyValue;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;

import java.util.List;
import java.util.stream.Collectors;

public class KeyValueMapper {

    public List<KeyValue> mapKeyValues(KeyListStructure keyListStructure) {
        if (keyListStructure == null || keyListStructure.getKeyValue() == null) {
            return null;
        }
        return keyListStructure.getKeyValue().stream().map(kv -> mapKeyValue(kv)).collect(Collectors.toList());
    }

    public KeyValue mapKeyValue(KeyValueStructure netexKeyValue) {
        return new KeyValue(netexKeyValue.getKey(), netexKeyValue.getTypeOfKey(), netexKeyValue.getValue());
    }
}
