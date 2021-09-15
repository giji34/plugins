package com.github.giji34.spigot;

import java.util.HashMap;
import java.util.concurrent.Callable;

class DefaultHashMap<K, V> {
    final HashMap<K, V> store = new HashMap<>();
    final Callable factory;

    DefaultHashMap(Callable<V> defaultFactory) {
        this.factory = defaultFactory;
    }

    V get(K key) {
        V v = this.store.get(key);
        if (v == null) {
            try {
                v = (V)this.factory.call();
                this.store.put(key, v);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        return v;
    }

    void put(K key, V value) {
        this.store.put(key, value);
    }

    void remove(K key) {
        this.store.remove(key);
    }
}
