package util;

import java.util.Map;

/**
 * @author: HuangSiBo
 * @Description: map，用来存储kv结构的数据
 * @Data: Created in 10:59 2022/3/21
 */
public class MyEntry<K, V> implements Map.Entry<K, V> {
    private K key;
    private V value;

    public MyEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V old = this.value;
        this.value = value;
        return old;
    }
}
