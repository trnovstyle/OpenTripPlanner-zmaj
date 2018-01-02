package org.opentripplanner.netex.loader.support;

import org.rutebanken.netex.model.EntityInVersionStructure;

import java.util.Comparator;

public class HierarchicalMultimapById<V extends EntityInVersionStructure> extends HierarchicalMultimap<String, V> {

    public HierarchicalMultimapById() {
    }

    public HierarchicalMultimapById(HierarchicalMultimapById<V> parent) {
        super(parent);
    }


    public V lookupLastVersionById(String id) {
        return lookup(id).stream()
                .max(Comparator.comparingInt(o2 -> Integer.parseInt(o2.getVersion())))
                .orElse(null);
    }

    /**
     * Add an entity and use its Key to index it.
     */
    public void add(V entity) {
        super.add(entity.getId(), entity);
    }

    @Override
    public void add(String key, V value) {
        throw new IllegalArgumentException("Use the add method with just one argument instead of this method.");
    }
}
