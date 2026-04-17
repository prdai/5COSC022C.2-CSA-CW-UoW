package com.w2120198.csa.cw.dao;

import com.w2120198.csa.cw.model.BaseModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * List-backed generic store. Every method synchronises on the store,
 * because JAX-RS resources are instantiated per request and many such
 * instances may drive this store concurrently. This is the concurrency
 * remedy discussed in the Part 1.1 report answer.
 *
 * @param <T> entity type; must expose a string id via {@link BaseModel}
 */
public class GenericDAO<T extends BaseModel> {

    private final List<T> items;

    public GenericDAO(List<T> items) {
        this.items = items;
    }

    public List<T> getAll() {
        synchronized (items) {
            return new ArrayList<>(items);
        }
    }

    public T getById(String id) {
        if (id == null) {
            return null;
        }
        synchronized (items) {
            for (T item : items) {
                if (id.equals(item.getId())) {
                    return item;
                }
            }
            return null;
        }
    }

    public void add(T item) {
        synchronized (items) {
            items.add(item);
        }
    }

    public void update(T item) {
        synchronized (items) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(item.getId())) {
                    items.set(i, item);
                    return;
                }
            }
        }
    }

    public boolean delete(String id) {
        synchronized (items) {
            return items.removeIf(item -> item.getId().equals(id));
        }
    }

    /**
     * Exposed read-only for callers that need to iterate without the
     * list-copy allocation; callers must externally synchronise on the
     * returned reference if they mutate during iteration.
     */
    public List<T> unmodifiableView() {
        return Collections.unmodifiableList(items);
    }
}
