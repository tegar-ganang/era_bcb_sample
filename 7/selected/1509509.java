package net.sourceforge.scrollrack;

import java.math.BigInteger;

public class Enccard {

    public static final int NUM_BYTES = 32;

    private byte[] data;

    public Enccard() {
    }

    /**
 * Copy the data from another card to this card.
 */
    public void set(Enccard that) {
        this.data = new byte[NUM_BYTES];
        for (int i = 0; i < NUM_BYTES; i++) this.data[i] = that.data[i];
    }

    /**
 * Write a description of the given card to this card.
 */
    public void set(CardBase cardbase, byte salt, int globalid) {
        CardInfo info = cardbase.get(globalid);
        if (info == null) {
            System.out.println("" + globalid + ": globalid not found");
            return;
        }
        byte[] srcBytes = info.name.getBytes();
        this.data = new byte[NUM_BYTES];
        this.data[0] = salt;
        this.data[1] = (byte) srcBytes.length;
        int src = 0;
        int dst = 2;
        while ((dst < NUM_BYTES) && (src < srcBytes.length)) {
            this.data[dst++] = srcBytes[src++];
        }
        while (dst < NUM_BYTES) {
            this.data[dst++] = (byte) DevRandom.random_number(256);
        }
    }

    /**
 * Translate an encoded card to a salt and globalid.
 */
    public int[] get(CardBase cardbase) {
        int[] res = new int[2];
        res[0] = data[0];
        int len = data[1];
        if (len <= NUM_BYTES - 2) {
            String name = new String(data, 2, len);
            res[1] = cardbase.find_globalid(name);
        } else {
            String name = new String(data, 2, NUM_BYTES - 2);
            res[1] = cardbase.find_prefix(name, len);
        }
        return (res);
    }

    /**
 * Translate a base64 encoding to an Enccard.
 */
    public void set(String text) {
        byte[] result = Base64.decode(text);
        if (result.length != NUM_BYTES) {
            System.out.println("Enccard: bad base64 encoding");
            return;
        }
        this.data = result;
    }

    /**
 * Translate an Enccard to base64.
 */
    public String get_base64() {
        return (new String(Base64.encode(data)));
    }

    /**
 * Translate a positive BigInteger to an Enccard.
 */
    public void set(BigInteger integer) {
        byte[] result = integer.toByteArray();
        if ((result.length == NUM_BYTES + 1) && (result[0] == 0)) {
            data = new byte[NUM_BYTES];
            for (int i = 0; i < NUM_BYTES; i++) data[i] = result[i + 1];
        } else if (result.length == NUM_BYTES) {
            data = result;
        } else if (result.length < NUM_BYTES) {
            data = new byte[NUM_BYTES];
            for (int i = 0; i < result.length; i++) data[i + NUM_BYTES - result.length] = result[i];
        } else {
            System.out.println("Enccard.set: result.length=" + result.length);
        }
    }

    /**
 * Translate an Enccard to a positive BigInteger.
 */
    public BigInteger get_integer() {
        return new BigInteger(+1, data);
    }
}
