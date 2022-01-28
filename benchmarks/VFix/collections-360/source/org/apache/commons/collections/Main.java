package org.apache.commons.collections;

import java.util.Collection;

import org.apache.commons.collections.iterators.FilterListIterator;
import org.apache.commons.collections.list.GrowthList;
import org.junit.Assert;

public class Main {
	public static void main(String[] args) {
        Collection<Predicate<Object>> var7 = new GrowthList<Predicate<Object>>();
        Predicate<Object> var9 = PredicateUtils.anyPredicate(var7);
        FilterListIterator<Object> var13 = new FilterListIterator<Object>(var9);
        FilterListIterator<Object> var14 = new FilterListIterator<Object>(var9);
        var14.hasPrevious();
	}
}
