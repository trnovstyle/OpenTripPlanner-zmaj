package org.opentripplanner.netex.loader.support;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Set;

public class HierarchicalMultimap<K,V> extends AbstractHierarchicalMap<K, Collection<V>> {
    private Multimap<K,V> map  = ArrayListMultimap.create();

    public HierarchicalMultimap() {
        super(null);
    }

    public HierarchicalMultimap(HierarchicalMultimap<K, V> parent) {
        super(parent);
    }

    public void add(K key, V value) {
        map.put(key, value);
    }

    public Set<K> keys() {
        return map.keySet();
    }

    @Override
    protected boolean valueExist(Collection<V> value) {
        return value != null && !value.isEmpty();
    }

    @Override
    protected Collection<V> localGet(K key) {
        return map.get(key);
    }

    @Override
    protected boolean localContainsKey(K key) {
        return map.containsKey(key);
    }
}
