package org.opentripplanner.netex.loader.support;

import org.rutebanken.netex.model.EntityStructure;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HierarchicalMap<K,V> extends AbstractHierarchicalMap<K, V> {
    private Map<K,V> map  = new HashMap<>();

    public HierarchicalMap() {
        super(null);
    }

    public HierarchicalMap(HierarchicalMap<K, V> parent) {
        super(parent);
    }

    public void add(K key, V value) {
        map.put(key, value);
    }

    public Set<K> keys() {
        return map.keySet();
    }

    public Collection<V> values() {
        return map.values();
    }

    @Override
    protected V localGet(K key) {
        return map.get(key);
    }

    @Override
    protected boolean localContainsKey(K key) {
        return map.containsKey(key);
    }


}
