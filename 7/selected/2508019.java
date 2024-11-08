package chapter1;

import edu.princeton.cs.algs4.BinarySearch;
import edu.princeton.cs.stdlib.StdArrayIO;
import edu.princeton.cs.stdlib.StdOut;
import edu.princeton.cs.stdlib.StdRandom;
import edu.princeton.cs.stdlib.Stopwatch;

/***
 * Tests number of pairs in an array with distinct integers
 * Compares the method developed by me and the method described 
 * in the book by Sedgewick and Wayne  			
 * 
 * @author Andreas Bok Andersen
 *
 */
public class ThreeSumFaster {

    private static int[] a = null;

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("N\tThreeSum \ttriples\t\t\tThreeSumFast\ttriples\n");
        a = StdArrayIO.readInt1D();
        int size = a.length;
        Stopwatch w = new Stopwatch();
        int triples1 = threeSum(a);
        double t1 = w.elapsedTime();
        int triples2 = threeSumFast(a);
        double t2 = w.elapsedTime();
        sb.append(String.format("%s;\t%s;\t\t%s;\t\t\t%s;\t\t%s\n", size, t1, triples1, t2, triples2));
        StdOut.println("");
        StdOut.println(sb.toString());
    }

    private static void generateRandom(int size) {
        double bounds = size;
        bounds = size;
        a = new int[size];
        for (int j = 0; j < size; j++) {
            a[j] = (int) StdRandom.uniform(-bounds, bounds);
        }
        a = removeDuplicates(a);
    }

    /***
	 * 
	 * @param a int[] in which to search for zero sum pairs   
	 * @return number of pairs 
	 */
    private static int twoSum(int[] a) {
        int floor = 0;
        int ceil = a.length - 1;
        int pairs = 0;
        if (a[0] > 0 | a[ceil] < 0) return pairs;
        int delta;
        while (floor < ceil) {
            delta = a[floor] + a[ceil];
            if (delta == 0) {
                pairs++;
                floor++;
                ceil--;
            } else if (delta > 0) {
                ceil--;
            } else {
                floor++;
            }
        }
        return pairs;
    }

    /***
	 * 
	 * @param a int[] in which to search for zero sum triples 
	 * @return number of zero sum triples  
	 */
    private static int threeSum(int[] a) {
        int pairs = 0;
        for (int i = 0; i < a.length; i++) {
            int j = i + 1;
            int k = a.length - 1;
            int delta;
            while (j < k) {
                delta = a[i] + a[j] + a[k];
                if (delta == 0) {
                    pairs++;
                    j++;
                    k--;
                } else if (delta > 0) {
                    k--;
                } else {
                    j++;
                }
            }
        }
        return pairs;
    }

    /***
	 * The algorithm from Sedgewick and Wayne using BinarySearch.rank 
	 * @param a int[] in which to search for zero sum triples 
	 * @return number of zero sum triples  
	 */
    private static int threeSumFast(int[] a) {
        int N = a.length;
        int cnt = 0;
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                if (BinarySearch.rank(-a[i] - a[j], a) > j) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    /***
	 * 
	 * @param a int[] in which to remove duplicates
	 * @return an int[] with distinct elements 
	 */
    private static int[] removeDuplicates(int[] a) {
        int[] tmp = new int[a.length];
        int count = 0;
        tmp[count] = a[count];
        for (int i = 0; i < a.length - 1; i++) {
            if (tmp[count] != a[i + 1]) {
                count++;
                tmp[count] = a[i + 1];
            }
        }
        count++;
        int[] tmp2 = new int[count];
        for (int i = 0; i < tmp2.length; i++) {
            tmp2[i] = tmp[i];
        }
        return tmp2;
    }
}
