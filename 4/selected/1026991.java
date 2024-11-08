package de.grogra.tools;

import java.io.*;

public class HGTTools {

    private static void skip(InputStream in, long n) throws IOException {
        while (n > 0) {
            n -= in.skip(n);
        }
    }

    private static void scaleHGT(String file, int width, int w, int h) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        int height = 0;
        while (true) {
            in.mark(1);
            if (in.read() < 0) {
                break;
            }
            in.reset();
            for (int x = 0; x < width; x++) {
                if (x % w == 0) {
                    System.out.write(in.read());
                    System.out.write(in.read());
                } else {
                    skip(in, 2);
                }
            }
            height++;
            skip(in, width * 2 * (h - 1));
        }
        in.close();
        System.out.flush();
        System.err.println("Output size is " + (width / w) + 'x' + height);
    }

    private static void extractHGT(String file, int width, int w, int h, int x, int y) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        byte[] buf = new byte[w * 2];
        skip(in, y * width * 2);
        while (--h >= 0) {
            skip(in, x * 2);
            in.read(buf);
            System.out.write(buf);
            skip(in, (width - x - w) * 2);
        }
        in.close();
        System.out.flush();
    }

    private static void combineHGT(String[] files, int begin, int end, int width, int weCount) throws IOException {
        InputStream[] in = new InputStream[weCount];
        int height = 0;
        while (begin < end) {
            for (int i = 0; i < weCount; i++) {
                in[i] = (begin >= end) ? null : new BufferedInputStream(new FileInputStream(files[begin++]));
            }
            boolean firstRow = height > 0;
            while (true) {
                in[0].mark(1);
                if (in[0].read() < 0) {
                    break;
                }
                in[0].reset();
                for (int i = 0; i < weCount; i++) {
                    for (int x = 0; x < width; x++) {
                        int a;
                        if ((in[i] == null) || ((a = in[i].read()) < 0)) {
                            a = Short.MIN_VALUE;
                        } else {
                            int b = in[i].read();
                            a = (b < 0) ? Short.MIN_VALUE : (a << 8) | b;
                        }
                        if (!firstRow && ((x > 0) || (i == 0))) {
                            System.out.write(a >> 8);
                            System.out.write(a);
                        }
                    }
                }
                if (firstRow) {
                    firstRow = false;
                } else {
                    height++;
                }
            }
            for (int i = 0; i < weCount; i++) {
                if (in[i] != null) {
                    in[i].close();
                }
            }
        }
        System.out.flush();
        System.err.println("Output size is " + (weCount * (width - 1) + 1) + 'x' + height);
    }

    public static void main(String[] args) throws IOException {
        parseArgs: if (args.length > 2) {
            if (!(args[0].equals("-width"))) {
                break parseArgs;
            }
            int width;
            try {
                width = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                break parseArgs;
            }
            if (args[2].equals("-combine")) {
                if (args.length <= 4) {
                    break parseArgs;
                }
                int weCount;
                try {
                    weCount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    break parseArgs;
                }
                combineHGT(args, 4, args.length, width, weCount);
                return;
            } else if (args[2].equals("-extract")) {
                if (args.length != 5) {
                    break parseArgs;
                }
                int w, h, x, y;
                try {
                    String d = args[3];
                    h = d.indexOf('x');
                    if (h <= 0) {
                        break parseArgs;
                    }
                    w = Integer.parseInt(d.substring(0, h));
                    x = d.indexOf('+', h);
                    if (x < h + 2) {
                        break parseArgs;
                    }
                    h = Integer.parseInt(d.substring(h + 1, x));
                    y = d.indexOf('+', x + 1);
                    if (y < x + 2) {
                        break parseArgs;
                    }
                    x = Integer.parseInt(d.substring(x + 1, y));
                    if (y >= d.length() - 1) {
                        break parseArgs;
                    }
                    y = Integer.parseInt(d.substring(y + 1));
                } catch (NumberFormatException e) {
                    break parseArgs;
                }
                extractHGT(args[4], width, w, h, x, y);
                return;
            } else if (args[2].equals("-scale")) {
                if (args.length != 5) {
                    break parseArgs;
                }
                int w, h;
                try {
                    String d = args[3];
                    h = d.indexOf('x');
                    if (h <= 0) {
                        break parseArgs;
                    }
                    w = Integer.parseInt(d.substring(0, h));
                    if (h >= d.length() - 1) {
                        break parseArgs;
                    }
                    h = Integer.parseInt(d.substring(h + 1));
                } catch (NumberFormatException e) {
                    break parseArgs;
                }
                scaleHGT(args[4], width, w, h);
                return;
            }
        }
        System.err.println("java -jar hgttools.jar -width w -combine count file1 file2 ...");
        System.err.println("java -jar hgttools.jar -width w -extract WxH+X+Y file");
        System.err.println("java -jar hgttools.jar -width w -scale WxH file");
    }
}
