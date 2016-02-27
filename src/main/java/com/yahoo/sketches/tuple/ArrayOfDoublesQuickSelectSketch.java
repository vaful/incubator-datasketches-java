/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.tuple;

import com.yahoo.sketches.QuickSelect;

import static com.yahoo.sketches.Util.RESIZE_THRESHOLD;
import static com.yahoo.sketches.Util.REBUILD_THRESHOLD;
import static com.yahoo.sketches.Util.ceilingPowerOf2;

/**
 * Top level class for hash table based implementation, which uses quick select algorithm
 * when the time comes to rebuild the hash table and throw away some entries.
 */
abstract class ArrayOfDoublesQuickSelectSketch extends ArrayOfDoublesUpdatableSketch {

  static final byte serialVersionUID = 1;

  static final int LG_NOM_ENTRIES_BYTE = 16;
  static final int LG_CUR_CAPACITY_BYTE = 17;
  static final int LG_RESIZE_FACTOR_BYTE = 18;
  // 1 byte of padding for alignment
  static final int SAMPLING_P_FLOAT = 20;
  static final int RETAINED_ENTRIES_INT = 24;
  // 4 bytes of padding for alignment
  static final int ENTRIES_START = 32;

  static final int MIN_NOM_ENTRIES = 32;
  static final int DEFAULT_LG_RESIZE_FACTOR = 3;

  // these can be derived from other things, but are kept here for performance
  int rebuildThreshold_;
  int mask_;
  int lgCurrentCapacity_;

  ArrayOfDoublesQuickSelectSketch(int numValues, long seed) {
    super(numValues, seed);
  }

  abstract void updateValues(int index, double[] values);
  abstract void setNotEmpty();
  abstract void setIsEmpty(boolean isEmpty);
  abstract boolean isInSamplingMode();
  abstract int getResizeFactor();
  abstract int getCurrentCapacity();
  abstract void rebuild(int newCapacity);
  abstract long getKey(int index);
  abstract void setKey(int index, long key);
  abstract void setValues(int index, double[] values, boolean isCopyRequired);
  abstract void incrementCount();
  abstract void setThetaLong(long theta);
  abstract int insertKey(long key);
  abstract int findOrInsertKey(long key);
  abstract double[] find(long key);

  @Override
  public void trim() {
    if (getRetainedEntries() > getNominalEntries()) {
      updateTheta();
      rebuild();
    }
  }

  /**
   * @param nomEntries Nominal number of entries. Forced to the nearest power of 2 greater than given value.
   * @param numValues Number of double values to keep for each key
   * @return maximum required storage bytes given nomEntries and numValues
   */
  static int getMaxBytes(int nomEntries, int numValues) {
    return ENTRIES_START + (SIZE_OF_KEY_BYTES + SIZE_OF_VALUE_BYTES * numValues) * ceilingPowerOf2(nomEntries) * 2;
  }

  // non-public methods below

  // this is a special back door insert for merging
  // not sufficient by itself without keeping track of theta of another sketch
  void merge(long key, double[] values) {
    setNotEmpty();
    if (key < theta_) {
      int index = findOrInsertKey(key);
      if (index < 0) {
        incrementCount();
        setValues(~index, values, true);
      } else {
        updateValues(index, values);
      }
      rebuildIfNeeded();
    }
  }

  void rebuildIfNeeded() {
    if (getRetainedEntries() < rebuildThreshold_) return;
    if (getCurrentCapacity() > getNominalEntries()) {
      updateTheta();
      rebuild();
    } else {
      rebuild(getCurrentCapacity() * getResizeFactor());
    }
  }
  
  void rebuild() {
    rebuild(getCurrentCapacity());
  }

  void insert(long key, double[] values) {
    int index = insertKey(key);
    setValues(index, values, false);
    incrementCount();
  }

  void setRebuildThreshold() {
    if (getCurrentCapacity() > getNominalEntries()) {
      rebuildThreshold_ = (int) (getCurrentCapacity() * REBUILD_THRESHOLD);
    } else {
      rebuildThreshold_ = (int) (getCurrentCapacity() * RESIZE_THRESHOLD);
    }
  }

  @Override
  void insertOrIgnore(long key, double[] values) {
    if (values.length != getNumValues()) throw new IllegalArgumentException("input array of values must have " + getNumValues() + " elements, but has " + values.length);
    setNotEmpty();
    if (key == 0 || key >= theta_) return;
    int index = findOrInsertKey(key);
    if (index < 0) {
      incrementCount();
      setValues(~index, values, true);
    } else {
      updateValues(index, values);
    }
    rebuildIfNeeded();
  }

  void updateTheta() {
    long[] keys = new long[getRetainedEntries()];
    int i = 0;
    for (int j = 0; j < getCurrentCapacity(); j++) {
      long key = getKey(j); 
      if (key != 0) keys[i++] = key;
    }
    setThetaLong(QuickSelect.select(keys, 0, getRetainedEntries() - 1, getNominalEntries()));
  }

}