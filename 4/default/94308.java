import java.util.*;
import org.blobstreaming.www.*;
import org.blobstreaming.*;
import java.io.*;
import java.text.*;
import java.lang.Thread;

public class TestMyBS {

    static int ITERATIONS = 100000;

    static void printTime(long start, long stop, int iterations) {
        long diff = stop - start;
        DecimalFormat f = new DecimalFormat("#0.0##");
        double ops = (double) iterations / (double) diff * (double) 1000;
        StringBuffer sb = f.format(ops, new StringBuffer(), new FieldPosition(NumberFormat.INTEGER_FIELD));
        System.out.println("Time: " + diff + " ms, " + sb.toString() + " ops/s");
    }

    static void delete_files() {
        File f;
        int i = 0;
        boolean notFound;
        try {
            long start = System.currentTimeMillis();
            for (i = 0; i < 2000000; i++) {
                notFound = true;
                f = new File("tmp/blobs/blob" + i);
                if (f.exists()) {
                    notFound = false;
                    f.delete();
                }
                f = new File("tmp/blobs/dir" + (i % 100) + "/blob" + i);
                if (f.exists()) {
                    notFound = false;
                    f.delete();
                }
                if (notFound) break;
            }
            for (int j = 0; j < 100; j++) {
                f = new File("tmp/blobs/dir" + j + "/");
                if (f.exists()) f.delete();
            }
            long stop = System.currentTimeMillis();
            printTime(start, stop, i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void write_file_test(boolean displayTime, boolean withSubDirs, boolean createDirs, int first, int iterations) {
        File f;
        FileOutputStream s;
        int i = 0, num;
        try {
            if (createDirs) {
                f = new File("tmp");
                if (!f.exists()) f.mkdir();
                f = new File("tmp/blobs");
                if (!f.exists()) f.mkdir();
                if (withSubDirs) {
                    for (i = 0; i < 100; i++) {
                        f = new File("tmp/blobs/dir" + i + "/");
                        if (!f.exists()) f.mkdir();
                    }
                }
            }
            long start = System.currentTimeMillis();
            for (i = 0; i < iterations; i++) {
                num = first + i;
                if (withSubDirs) f = new File("tmp/blobs/dir" + (num % 100) + "/blob" + num); else f = new File("tmp/blobs/blob" + num);
                f.createNewFile();
                s = new FileOutputStream(f);
                s.write("This is an example of an uploaded BLOB".getBytes());
                s.close();
            }
            long stop = System.currentTimeMillis();
            if (displayTime) printTime(start, stop, iterations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class MultiWriteThread extends Thread {

        boolean withSubDirs;

        int start;

        int iterations;

        public MultiWriteThread(boolean w, int s, int i) {
            withSubDirs = w;
            start = s;
            iterations = i;
        }

        public void run() {
            write_file_test(false, withSubDirs, false, start, iterations);
        }
    }

    static void multi_write_test(boolean withSubDirs, int threads, int total_iterations) {
        MultiWriteThread t[] = new MultiWriteThread[threads];
        int iterations;
        File f;
        for (int i = 0; i < 100; i++) {
            f = new File("tmp/blobs/dir" + i + "/");
            if (!f.exists()) f.mkdir();
        }
        iterations = (total_iterations + threads - 1) / threads;
        for (int i = 0; i < threads; i++) t[i] = new MultiWriteThread(withSubDirs, i * iterations, iterations);
        try {
            f = new File("tmp");
            if (!f.exists()) f.mkdir();
            f = new File("tmp/blobs");
            if (!f.exists()) f.mkdir();
            if (withSubDirs) {
                for (int i = 0; i < 100; i++) {
                    f = new File("tmp/blobs/dir" + i + "/");
                    if (!f.exists()) f.mkdir();
                }
            }
            long start = System.currentTimeMillis();
            for (int i = 0; i < threads; i++) t[i].start();
            for (int i = 0; i < threads; i++) t[i].join();
            long stop = System.currentTimeMillis();
            printTime(start, stop, iterations * threads);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void read_file_test(boolean displayTime, boolean withSubDirs, int first, int iterations) {
        File f;
        FileInputStream s;
        int i = 0, num;
        byte buffer[] = new byte[500];
        String str;
        try {
            long start = System.currentTimeMillis();
            for (i = 0; i < iterations; i++) {
                num = first + i;
                if (withSubDirs) f = new File("tmp/blobs/dir" + (num % 100) + "/blob" + num); else f = new File("tmp/blobs/blob" + num);
                s = new FileInputStream(f);
                s.read(buffer);
                str = new String(buffer);
                s.close();
            }
            long stop = System.currentTimeMillis();
            if (displayTime) printTime(start, stop, iterations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class MultiReadThread extends Thread {

        boolean withSubDirs;

        int start;

        int iterations;

        public MultiReadThread(boolean w, int s, int i) {
            withSubDirs = w;
            start = s;
            iterations = i;
        }

        public void run() {
            read_file_test(false, withSubDirs, start, iterations);
        }
    }

    static void multi_read_test(boolean withSubDirs, int threads, int total_iterations) {
        MultiReadThread t[] = new MultiReadThread[threads];
        int iterations;
        iterations = (total_iterations + threads - 1) / threads;
        for (int i = 0; i < threads; i++) t[i] = new MultiReadThread(withSubDirs, i * iterations, iterations);
        try {
            long start = System.currentTimeMillis();
            for (int i = 0; i < threads; i++) t[i].start();
            for (int i = 0; i < threads; i++) t[i].join();
            long stop = System.currentTimeMillis();
            printTime(start, stop, iterations * threads);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void upload_keep_alive_test(byte blobs[][], boolean displayTime, int first, int iterations) {
        try {
            Connection conn = new Connection("localhost:8080", true);
            long start = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                blobs[first + i] = conn.upload("/test", "html/text", "This is an example of an uploaded BLOB");
            }
            long stop = System.currentTimeMillis();
            conn.close();
            if (displayTime) printTime(start, stop, iterations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class MultiUploadThread extends Thread {

        byte blobs[][];

        int start;

        int iterations;

        public MultiUploadThread(byte b[][], int s, int i) {
            blobs = b;
            start = s;
            iterations = i;
        }

        public void run() {
            upload_keep_alive_test(blobs, false, start, iterations);
        }
    }

    static byte[][] multi_upload_test(int threads, int total_iterations) {
        MultiUploadThread t[] = new MultiUploadThread[threads];
        byte blobs[][];
        int iterations;
        iterations = (total_iterations + threads - 1) / threads;
        blobs = new byte[iterations * threads][];
        for (int i = 0; i < threads; i++) t[i] = new MultiUploadThread(blobs, i * iterations, iterations);
        try {
            long start = System.currentTimeMillis();
            for (int i = 0; i < threads; i++) t[i].start();
            for (int i = 0; i < threads; i++) t[i].join();
            long stop = System.currentTimeMillis();
            printTime(start, stop, iterations * threads);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blobs;
    }

    static void download_keep_alive_test(byte blobs[][], boolean displayTime, int first, int iterations) {
        try {
            Connection conn = new Connection("localhost:8080", true);
            String result;
            BlobStream blob;
            long start = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                blob = conn.download(new String(blobs[first + i]));
                result = blob.getStringData();
            }
            long stop = System.currentTimeMillis();
            conn.close();
            if (displayTime) printTime(start, stop, iterations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class MultiDownloadThread extends Thread {

        byte blobs[][];

        int start;

        int iterations;

        public MultiDownloadThread(byte b[][], int s, int i) {
            blobs = b;
            start = s;
            iterations = i;
        }

        public void run() {
            download_keep_alive_test(blobs, false, start, iterations);
        }
    }

    static void multi_download_test(byte blobs[][], int threads, int total_iterations) {
        MultiDownloadThread t[] = new MultiDownloadThread[threads];
        int iterations;
        iterations = (total_iterations + threads - 1) / threads;
        for (int i = 0; i < threads; i++) t[i] = new MultiDownloadThread(blobs, i * iterations, iterations);
        try {
            long start = System.currentTimeMillis();
            for (int i = 0; i < threads; i++) t[i].start();
            for (int i = 0; i < threads; i++) t[i].join();
            long stop = System.currentTimeMillis();
            printTime(start, stop, iterations * threads);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        byte blobs[][];
        System.out.println("MyBS Repository Test");
        System.out.println("java.class.path: " + System.getProperty("java.class.path"));
        System.out.println("TEST ITERATIONS: " + ITERATIONS);
        try {
            ContentHandler.register();
            FTP.register();
            HTTP.register();
            int threads = 1;
            for (int i = 0; i < 6; i++) {
                System.out.println("REPOSITORY, THREADS " + threads);
                System.out.print("Upload ");
                blobs = multi_upload_test(threads, ITERATIONS);
                System.out.print("Download ");
                multi_download_test(blobs, threads, ITERATIONS);
                threads *= 2;
            }
            threads = 1;
            for (int i = 0; i < 6; i++) {
                System.out.println("FILE SYSTEM, THREADS " + threads);
                System.out.print("Delete ");
                delete_files();
                System.out.print("Write ");
                multi_write_test(true, threads, ITERATIONS);
                System.out.print("Read ");
                multi_read_test(true, threads, ITERATIONS);
                threads *= 2;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
