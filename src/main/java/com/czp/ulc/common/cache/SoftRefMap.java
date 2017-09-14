package com.czp.ulc.common.cache;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * 内存自适应map实现,实质上是Delegate模式,当内存充足时,该类会尽可能多的缓存数据<br>
 * 当内存不足时,GC会回收该类缓存的数据,不会出现OOME,能有效避免LRU逐个淘汰问题,<br>
 * 如: LRUCache cache = new LRUCache(10000);<br>
 * LRUCache总是每次只淘汰1个冷数据,当内存紧张时可能会导致频繁FullGC<br>
 * 你可以这样测试,不会OOME.jvm: -Xmx128m
 * public static void main(String[] args) throws IOException {
		SoftRefMap<Integer, String> map = new SoftRefMap<>();
		for (int i = 0; i < 10000000; i++) {
			map.put(i, "this is test" + i);
		}
		System.out.println(map.get(1000));
		System.in.read();
	}
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年8月11日 下午1:45:53</li>
 * 
 * @version 0.0.1
 */

public class SoftRefMap<K, V> implements Map<K, V> {

	/** 默认rehash比例 {@link ConcurrentHashMap.LOAD_FACTOR} */
	private static float DEFAULT_FACTOR = 0.75f;

	/** 默认初始化大小 {@link ConcurrentHashMap.DEFAULT_CAPACITY} */
	private static int DEFAULT_CAPACITY = 16;

	/** 是否开启debug模式,如果是当map被GC时会打印消息 */
	private boolean debugModel = false;

	private float factor = DEFAULT_FACTOR;

	private int capacity = DEFAULT_CAPACITY;

	/** map弱引用,内存不足时会自动回收,DCL检测,声明为 volatile */
	private volatile SoftReference<ConcurrentHashMap<K, V>> holder;

	public SoftRefMap() {
		this(DEFAULT_CAPACITY, DEFAULT_FACTOR, false);
	}

	public SoftRefMap(int initialCapacity, float loadFactor, boolean debug) {
		this.debugModel = debug;
		this.factor = loadFactor;
		this.capacity = initialCapacity;
		this.holder = new SoftReference<ConcurrentHashMap<K, V>>(
				new ConcurrentHashMap<K, V>(initialCapacity, loadFactor));
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

	/***
	 * <li>对map操作必须检查map是否被GC回收</li>
	 * <li>标记final期待JVM内联该方法</li>
	 * 
	 * @return
	 */
	private final ConcurrentHashMap<K, V> map() {
		ConcurrentHashMap<K, V> map = holder.get();
		if (map == null) {
			// map maybe remove by GC thread
			if (debugModel) {
				System.out.println("current map has been deleted by GC");
			}
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
