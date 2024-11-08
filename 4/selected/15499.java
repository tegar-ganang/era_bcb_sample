package net.sf.semanticdebug.examples;

import java.math.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * @author DS
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BaseNContainer implements Container {

    public static void main(String[] args) throws Exception {
        int num = 10000;
        BaseNContainer cont = new BaseNContainer(num);
        for (int a = 0; a < num; a++) cont.add(a);
        int[] arr = cont.getAll();
        for (int a = 0; a < arr.length; a++) {
            System.out.println(arr[a]);
        }
    }

    BigInteger multiplier, currentResult, base;

    /**
	 * Maakt een baseN container.
	 * @param base base is het maximale getal + 1. Het grootste getal dient ook te onderscheiden zijn.
	 */
    public BaseNContainer(int base) {
        this.base = new BigInteger(base + "");
        multiplier = BigInteger.ONE;
        currentResult = BigInteger.ZERO;
    }

    /**
	 * Voegt een getal toe aan de container.
	 * @param num Nummer dat toegevoegd dient te worden.
	 * @throws Exception Als een getal toegevoegd wordt die niet onder de base ligt, probleem.
	 */
    public void add(int num) throws Exception {
        if (num >= base.intValue()) throw new Exception("Te grote waarde " + num);
        currentResult = currentResult.add(multiplier.multiply(new BigInteger(num + "")));
        multiplier = multiplier.multiply(base);
    }

    public String toString() {
        return currentResult.toString();
    }

    /**
	 * Haalt de volgende waarde uit de container en past het aan. 
	 * @return Volgende getal dat in de container zit.
	 */
    public int get() {
        int val = currentResult.mod(base).intValue();
        currentResult = currentResult.divide(base);
        return val;
    }

    public int[] getAll() {
        List val = new ArrayList();
        while (currentResult.compareTo(BigInteger.ZERO) != 0) {
            val.add(new Integer(get()));
        }
        int[] returns = new int[val.size()];
        Iterator it = val.iterator();
        int num = 0;
        while (it.hasNext()) returns[num++] = ((Integer) it.next()).intValue();
        return returns;
    }

    public void loadFromFile(RandomAccessFile file) {
        try {
            MappedByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            byte[] arr = new byte[(int) file.length()];
            buffer.get(arr);
            currentResult = new BigInteger(arr);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveToFile(RandomAccessFile file) {
        try {
            byte[] arr = currentResult.toByteArray();
            MappedByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, arr.length);
            buffer.put(arr);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
