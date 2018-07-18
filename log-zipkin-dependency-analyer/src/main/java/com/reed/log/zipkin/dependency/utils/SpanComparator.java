package com.reed.log.zipkin.dependency.utils;

import java.util.Comparator;

import zipkin2.Span;

/**
 * compare span timestamp
 * @author reed
 *
 */
public class SpanComparator implements Comparator<Span> {

	@Override
	public int compare(Span o1, Span o2) {
		int r = 0;
		if (o1 != null && o2 != null) {
			r = Long.valueOf(o1.timestampAsLong() - o2.timestampAsLong()).intValue();
		}
		return r;
	}

}
