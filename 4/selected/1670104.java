package study.io2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

public class A {

    public static void main(String[] args) throws IOException {
        File fileIn = new File("D:\\zz_c\\study2\\src\\study\\io\\A.java");
        InputStream fin = new FileInputStream(fileIn);
        PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream();
        pout.connect(pin);
        IoRead i = new IoRead();
        i.setIn(pin);
        File fileOU1 = new File("D:\\zz_c\\study2\\src\\study\\io\\A1.java");
        File fileOU2 = new File("D:\\zz_c\\study2\\src\\study\\io\\A2.java");
        File fileOU3 = new File("D:\\zz_c\\study2\\src\\study\\io\\A3.java");
        i.addOut(new BufferedOutputStream(new FileOutputStream(fileOU1)));
        i.addOut(new BufferedOutputStream(new FileOutputStream(fileOU2)));
        i.addOut(new BufferedOutputStream(new FileOutputStream(fileOU3)));
        PipedInputStream pin2 = new PipedInputStream();
        PipedOutputStream pout2 = new PipedOutputStream();
        i.addOut(pout2);
        pout2.connect(pin2);
        i.start();
        int read;
        try {
            read = fin.read();
            while (read != -1) {
                pout.write(read);
                read = fin.read();
            }
            fin.close();
            pout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int c = pin2.read();
        while (c != -1) {
            System.out.print((char) c);
            c = pin2.read();
        }
        pin2.close();
    }
}

class IoRead extends Thread {

    protected List<BufferedOutputStream> lout;

    PipedInputStream in;

    public IoRead() {
    }

    public void run() {
        try {
            int c;
            BufferedOutputStream[] outS = new BufferedOutputStream[lout.size()];
            outS = lout.toArray(outS);
            while ((c = in.read()) != -1) {
                for (int i = 0; i < outS.length; ++i) {
                    if (outS[i] != null) outS[i].write(c);
                }
            }
            for (BufferedOutputStream b : lout) {
                b.close();
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<BufferedOutputStream> getLout() {
        return lout;
    }

    public void setLout(List<BufferedOutputStream> lout) {
        this.lout = lout;
    }

    public void addOut(BufferedOutputStream out) {
        if (this.lout == null) lout = new ArrayList<BufferedOutputStream>();
        lout.add(out);
    }

    public void addOut(OutputStream outBin) {
        if (this.lout == null) lout = new ArrayList<BufferedOutputStream>();
        BufferedOutputStream out = new BufferedOutputStream(outBin);
        lout.add(out);
    }

    public PipedInputStream getIn() {
        return in;
    }

    public void setIn(PipedInputStream in) {
        this.in = in;
    }
}
