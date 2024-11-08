import java.util.Random;

class Buffer {

    private int buffer;

    public synchronized int incr() {
        return ++buffer;
    }

    public synchronized int read() {
        return buffer;
    }
}

class Reader extends Thread {

    private Random random = new Random();

    private String name;

    private Buffer buffer;

    private int reads;

    public Reader(String name, Buffer buffer, int reads) {
        this.setPriority(Thread.MIN_PRIORITY);
        this.setName(name);
        this.buffer = buffer;
        this.reads = reads;
    }

    public void run() {
        while (reads-- > 0) {
            System.out.println("Reader " + this.getName() + ": " + buffer.read());
            try {
                Thread.sleep(random.nextInt(1000));
            } catch (InterruptedException e) {
            }
        }
    }
}

class Writer extends Thread {

    private Random random = new Random();

    private String name;

    private Buffer buffer;

    private int writes;

    public Writer(String name, Buffer buffer, int writes) {
        this.setPriority(Thread.MAX_PRIORITY);
        this.setName(name);
        this.buffer = buffer;
        this.writes = writes;
    }

    public void run() {
        while (writes-- > 0) {
            System.out.println("Writer " + this.getName() + ": " + buffer.incr());
            try {
                Thread.sleep(random.nextInt(1000));
            } catch (InterruptedException e) {
            }
        }
    }
}

public class RW {

    public static int nicify(String a, int low, int high) {
        return (Math.min(Math.max(Integer.parseInt(a), low), high));
    }

    public static void main(String[] args) {
        int nReaders = nicify(args[0], 1, 5);
        int nWriters = nicify(args[1], 1, 5);
        int nAccesses = Integer.parseInt(args[2]);
        String letters[] = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" };
        System.out.println("Starting RW with:\n" + nReaders + " readers,\n" + nWriters + " writers,\n" + nAccesses + " accesses.");
        Buffer buffer = new Buffer();
        for (int i = 0; i < nReaders; i++) {
            new Reader(letters[i], buffer, nAccesses).start();
        }
        for (int i = 0; i < nWriters; i++) {
            new Writer(letters[i + 5], buffer, nAccesses).start();
        }
    }
}
