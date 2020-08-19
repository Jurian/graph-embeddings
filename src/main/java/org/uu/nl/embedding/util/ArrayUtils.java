	package org.uu.nl.embedding.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import Jama.Matrix;

public class ArrayUtils {
	
	/**
	 * Code copied from:
	 * https://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
	 * @param <T>
	 * @param a
	 * @param b
	 * @return
	 */
	static public <T> T[] concatenate(final T[] array1, final T[] array2) {
	    int aLen = array1.length;
	    int bLen = array2.length;

	    @SuppressWarnings("unchecked")
	    T[] resArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), aLen + bLen);
	    System.arraycopy(array1, 0, resArray, 0, aLen);
	    System.arraycopy(array2, 0, resArray, aLen, bLen);

	    return resArray;
	}
	
	/**
	 * 
	 * @param array1
	 * @param array2
	 * @return
	 */
	static public boolean[] concatenate(final boolean[] array1, final boolean[] array2) {
	    int aLen = array1.length;
	    int bLen = array2.length;

	    @SuppressWarnings("unchecked")
	    boolean[] resArray = (boolean[]) Array.newInstance(array1.getClass().getComponentType(), aLen + bLen);
	    System.arraycopy(array1, 0, resArray, 0, aLen);
	    System.arraycopy(array2, 0, resArray, aLen, bLen);

	    return resArray;
	}

	/**
	 * 
	 * @param array
	 * @return
	 */
	static public boolean[] toArray(final ArrayList<Boolean> array, final boolean bool) {
	    int len = array.size();

	    @SuppressWarnings("unchecked")
	    boolean[] resArray = (boolean[]) Array.newInstance(array.getClass().getComponentType(), len);
	    System.arraycopy(array, 0, resArray, 0, len);

	    return resArray;
	}

	/**
	 * 
	 * @param array
	 * @return
	 */
	static public int[] toArray(final ArrayList<Integer> array, final int i) {
	    int len = array.size();

	    @SuppressWarnings("unchecked")
	    int[] resArray = (int[]) Array.newInstance(array.getClass().getComponentType(), len);
	    System.arraycopy(array, 0, resArray, 0, len);

	    return resArray;
	}
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @param range
	 * @return
	 */
	static public int[] rangeNext(final int start, final int end, final int range) {
		int next = start;
		int[] steps = new int[ (((end-start)/range)+1) ];
		
		int counter = 0;
		while (next < end) {
			steps[counter] = next;
			next += range;
			counter++;
		}
		return steps;
	}
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @param range
	 * @return
	 */
	static public ArrayList<int[]> rangeNextTuple(final int start, final int end, final int range) {

		int prev = start;
		int next = ((start + range) - 1);
		ArrayList<int[]> steps = new ArrayList<int[]>();
		
		while (next < end) {
			steps.add(new int[] {prev, next});
			prev += range;
			next += range;
		}
		steps.add(new int[] {prev, end});
		
		return steps;
	}
	
	/**
	 * 
	 * @param <T>
	 * @param input
	 * @param start
	 * @param end
	 * @param range
	 * @return
	 */
	static public <T> ArrayList<T[]> getRangeSteps(final T[] input, final int start, final int end, final int range) {
		int counter = start;
		int prev = start;
		int next = (start+range); // GAAT DIT GOED??????????
		
	    T[] tempAr = (T[]) Array.newInstance(input.getClass().getComponentType(), range);
		ArrayList<T[]> resultList = new ArrayList<T[]>();
		
		while (next < end) {
			for (int i = 0; i < range; i++) {
				tempAr[i] = input[counter];
				 counter++;
			}
			resultList.add(tempAr);
			prev = next;
			next = (prev+range);
		}
		
		// Add rest of the input as the final array.
		tempAr = (T[]) Array.newInstance(input.getClass().getComponentType(), (end-prev) );
		for (int i = 0; counter < end; i++, counter++) {
			tempAr[i] = input[counter];
		}
		resultList.add(tempAr);
		
		return resultList;
	}

	
	/**
	 * 
	 * @param <T>
	 * @param input
	 * @param start
	 * @param end
	 * @param range
	 * @return
	 */
	static public <T> ArrayList<ArrayList<T>> getRangeSteps(final ArrayList<T> input, final int start, final int end, final int range) {
		int counter = start;
		int prev = start;
		int next = (start+range); // GAAT DIT GOED??????????
		
	    ArrayList<T> list = (ArrayList<T>) new ArrayList<T>();
		ArrayList<ArrayList<T>> resultList = new ArrayList<ArrayList<T>>();
		
		while (counter < end) {
			for (int i = 0; i < range; i++) {
				list.add(input.get(counter));
				 counter++;
			}
			resultList.add(list);
			prev = next;
			next = (prev+range);
		}
		
		return resultList;
	}
}


