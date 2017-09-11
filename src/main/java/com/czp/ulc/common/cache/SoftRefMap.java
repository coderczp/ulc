package com.czp.ulc.common.cache;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 弱引用map,内存自适应
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月11日 下午1:45:53</li>
 * 
 * @version 0.0.1
 */

public class SoftRefMap<K, V> implements Map<K, V> {

	private static int capacity = 16;

	private static float factor = 0.75f;

	private volatile SoftReference<ConcurrentHashMap<K, V>> holder;

	public SoftRefMap() {
		this(capacity, factor);
	}

	public SoftRefMap(int initialCapacity, float loadFactor) {
		factor = loadFactor;
		capacity = initialCapacity;
		ConcurrentHashMap<K, V> map = new ConcurrentHashMap<K, V>(initialCapacity, loadFactor);
		holder = new SoftReference<ConcurrentHashMap<K, V>>(map);
	}

	@Override
	public int size() {
		return map().size();
	}

	@Override
	public boolean isEmpty() {
		return map().isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map().containsValue(value);
	}

	@Override
	public V get(Object key) {
		return map().get(key);
	}

	@Override
	public V put(K key, V value) {
		return map().put(key, value);
	}

	@Override
	public V remove(Object key) {
		return map().remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		map().putAll(m);
	}

	@Override
	public void clear() {
		map().clear();
	}

	@Override
	public Set<K> keySet() {
		return map().keySet();
	}

	@Override
	public Collection<V> values() {
		return map().values();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map().entrySet();
	}

	private ConcurrentHashMap<K, V> map() {
		ConcurrentHashMap<K, V> map = holder.get();
		if (map == null) {
			// map maybe remove by GC thread
			synchronized (this) {
				if (map == null) {
					map = new ConcurrentHashMap<K, V>(capacity, factor);
					holder = new SoftReference<ConcurrentHashMap<K, V>>(map);
				}
			}
		}
		return map;
	}
}
