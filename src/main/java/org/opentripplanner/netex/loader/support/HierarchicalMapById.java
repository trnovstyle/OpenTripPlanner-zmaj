package org.opentripplanner.netex.loader.support;

import org.rutebanken.netex.model.EntityStructure;

import java.util.Collection;

public class HierarchicalMapById<V extends EntityStructure> extends HierarchicalMap<String, V> {

    public HierarchicalMapById() {
    }

    public HierarchicalMapById(HierarchicalMap<String, V> parent) {
        super(parent);
    }

    /**
     * Add an entity and use its Key to index it.
     */
    public void add(V entity) {
        super.add(entity.getId(), entity);
    }

    public void addAll(Collection<V> entities) {
        for (V v : entities) {  add(v); }
    }

    @Override
    public void add(String key, V value) {
        throw new IllegalArgumentException("Use the add method with just one argument instead of this method.");
    }
}
