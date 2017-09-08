package com.czp.ulc.util;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年8月11日 上午9:55:09</li>
 * 
 * @version 0.0.1
 */

public class MiniHeap<E> {

	/** 最大值即topk的K */
	private int maxSize;

	/** 堆 */
	private ArrayList<E> data;

	/** 比较器 */
	private Comparator<E> cmp;

	/** 标记是否已经构建了堆 */
	private boolean isBuildHeap;

	public MiniHeap(int size, Comparator<E> cmp) {
		this.cmp = cmp;
		this.maxSize = size;
		this.data = new ArrayList<>(size);
	}

	public boolean add(E e) {
		if (data.size() < maxSize) {
			return data.add(e);
		}
		if (!isBuildHeap) {
			buildHeap();
		}
		boolean add = cmp.compare(e, getRoot()) > 0;
		if (add)
			setRoot(e);
		return add;
	}

	public int size() {
		return data.size();
	}

	private void buildHeap() {
		int len = data.size();
		for (int i = len / 2 - 1; i >= 0; i--) {
			heapify(i,len);
		}
	}

	private int right(int i) {
		return (i + 1) << 1;
	}

	private int left(int i) {
		return ((i + 1) << 1) - 1;
	}

	private void heapify(int i, int len) {

		int l = left(i);
		int r = right(i);
		int smallest = i;

		if (l < len && cmp.compare(data.get(l), data.get(i)) < 0)
			smallest = l;

		if (r < len && cmp.compare(data.get(r), data.get(smallest)) < 0)
			smallest = r;

		if (i == smallest)
			return;

		swap(i, smallest);
		heapify(smallest,len);
	}

	public E getRoot() {
		return data.get(0);
	}

	public void setRoot(E root) {
		data.set(0, root);
		heapify(0,data.size());
	}

	public E get(int i) {
		return data.get(i);
	}

	private void swap(int i, int j) {
		E tmp = data.get(i);
		data.set(i, data.get(j));
		data.set(j, tmp);
	}

	public void sort() {
		final int min = 0;
		final int total = data.size();
        //末尾与头交换，交换后调整最大堆
        for (int i = total - 1; i > min; i--) {
            swap(0, i);
            heapify(0, i);
        }
	}

	public static void main(String[] args) {
		int[] data = { 854124545,100, 2, -1, 89, 1024, 88888, 36, 22, 888 ,2};
		MiniHeap<Integer> maxFiles = new MiniHeap<>(5, new Comparator<Integer>() {

			@Override
			public int compare(Integer o1, Integer o2) {
				return o1.compareTo(o2);
			}

		});

		for (int num : data) {
			maxFiles.add(num);
		}
		maxFiles.sort();
		for (int i = 0; i < maxFiles.size(); i++) {
			System.out.println(maxFiles.get(i));
		}

	}
}
