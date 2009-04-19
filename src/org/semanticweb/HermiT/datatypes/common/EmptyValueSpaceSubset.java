// Copyright 2008 by Oxford University; see license.txt for details
package org.semanticweb.HermiT.datatypes.common;

import java.util.Collection;

import org.semanticweb.HermiT.datatypes.ValueSpaceSubset;

public class EmptyValueSpaceSubset implements ValueSpaceSubset {

    public EmptyValueSpaceSubset() {
    }
    public boolean hasCardinalityAtLeast(int number) {
        return number<=0;
    }
    public boolean containsDataValue(Object dataValue) {
        return false;
    }
    public void enumerateDataValues(Collection<Object> dataValues) {
    }
}
