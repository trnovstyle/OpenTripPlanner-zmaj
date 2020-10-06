package org.opentripplanner.netex.loader.support;



public class DelegateToParentHierarchicalMap<K,V> extends HierarchicalMap<K, V> {
    private final HierarchicalMap<K, V> parent;

    public DelegateToParentHierarchicalMap(HierarchicalMap<K, V> parent) {
        super(parent);
        this.parent = parent;
    }

    public void add(K key, V value) {
        parent.add(key, value);
    }

    @Override
    protected V localGet(K key) {
        return null; // parent.localGet(key);
    }

    @Override
    protected boolean localContainsKey(K key) {
        return false; // parent.localContainsKey(key);
    }

}
