/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches;

import static com.yahoo.sketches.Util.zeroPad;

//import org.testng.annotations.Test;

import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;


/**
 * @author Lee Rhodes
 */
public class TestingUtil {
  
  static void printMem(Memory mem, String comment) {
    int bytes = (int) mem.getCapacity();
    int longs = bytes >>> 3;
    long[] longArr = new long[longs];
    mem.getLongArray(0, longArr, 0, longs);
    printArr(longArr, comment);
  }
  
  static void printArr(byte[] arr, String comment) {
    int longs = arr.length >>> 3;
    long[] longArr = new long[longs];
    Memory mem = new NativeMemory(arr);
    mem.getLongArray(0, longArr, 0, longs);
    printArr(longArr, comment);
  }
  
  static void printArr(long[] arr, String comment) {
    println("\n"+comment);
    for (int i=0; i< arr.length; i++) println(i+"\t"+arr[i]);
  }
  
  public static String longToHexBytes(long k) {
    long mask = 0XFFL;
    StringBuilder sb = new StringBuilder();
    for (int i = 8; i--> 0; ) {
      String s = Long.toHexString((k >>> i*8) & mask);
      sb.append(zeroPad(s, 2)).append(" ");
    }
    return sb.toString();
  }
  
  //@Test
  public void checklongToHexBytes() {
    long v = 0X1112131415161718L;
    println(longToHexBytes(v));
  }
  
  //@Test
  public void printlnTest() {
    println(this.getClass().getSimpleName());
  }
  
  /**
   * @param s value to print 
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }
}
