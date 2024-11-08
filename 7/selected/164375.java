package com.aricom.core;

import java.io.*;

public class Header {

    private int[] freq;

    private int[] cumfreq;

    private int c;

    private File file;

    public Header(File file, boolean fileCompressed, int c, int f) throws IOException, Exception {
        this.file = file;
        System.out.println("Obtaining probability table...");
        int car, freqtotal = (int) file.length() + 1;
        freq = new int[257];
        cumfreq = new int[258];
        FileInputStream in = new FileInputStream(file);
        DataInputStream din = new DataInputStream(in);
        if (fileCompressed) {
            int id = din.readInt();
            if (id / 256 == 0xAC05) {
                this.c = id % 256;
                for (car = 0; car < 256; car++) cumfreq[car] = din.readInt();
                cumfreq[256] = 1;
                cumfreq[257] = 0;
                for (car = 0; car < 256; car++) freq[car] = cumfreq[car] - cumfreq[car + 1];
            } else {
                Exception e = new Exception("This file is not aricom genuine ;)");
                throw e;
            }
        } else {
            int numshifts = 0;
            int aux;
            this.c = c;
            while (freqtotal >= (1 << f)) {
                freqtotal = freqtotal >>> 1;
                numshifts++;
            }
            for (int i = 0; i < 256; i++) freq[i] = 0;
            while ((car = in.read()) != -1) {
                freq[car]++;
            }
            freq[256] = 1;
            cumfreq[257] = 0;
            for (int i = 256; i >= 0; i--) {
                aux = freq[i] >>> numshifts;
                if (aux == 0 && freq[i] > 0) aux = 1;
                cumfreq[i] = cumfreq[i + 1] + aux;
            }
            if (cumfreq[0] >= (1 << f)) {
                numshifts++;
                for (int i = 256; i >= 0; i--) {
                    aux = freq[i] >>> numshifts;
                    if (aux == 0 && freq[i] > 0) aux = 1;
                    cumfreq[i] = cumfreq[i + 1] + aux;
                }
            }
            if (cumfreq[0] >= (1 << f)) {
                System.out.println("Sure it doesn't work cause frequences are too high");
            }
        }
    }

    public void writeHeader(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        DataOutputStream dos = new DataOutputStream(fos);
        dos.writeInt(1094922240 + c);
        for (int i = 0; i < 256; i++) {
            dos.writeInt(cumfreq[i]);
        }
        fos.close();
        System.out.println("Writen header");
    }

    public int cumFreq(int n) {
        return cumfreq[n];
    }

    public int readPi() {
        return this.c;
    }

    public int freq(int n) {
        return freq[n];
    }

    public int maxCumFreq() {
        return cumfreq[0];
    }

    public int realFileSize() {
        return (int) this.file.length();
    }

    public double entropy() {
        double ent = 0;
        double log2 = Math.log(2);
        for (int i = 0; i < 257; i++) if (freq[i] != 0) ent -= freq[i] * Math.log(freq[i] * (1. / (int) this.file.length())) / log2;
        return ent;
    }
}
