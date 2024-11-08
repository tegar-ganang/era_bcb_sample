package algorithm.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>Provide pipe mechanism to transport whatever data bytes.</p>
 * For example:[code]
 * public static void main(String argv[]) {
 * 	copyFile();
 * 	integrateApacheHttpClient4();
 * }
 * 
 * static void copyFile() {
 * 	Pipe.transport(new File("C:/src.txt"), new File("C:/dest.txt"), 0x00000400<<0x0000000A);
 * }
 * 
 * static void integrateApacheHttpClient4() { // with HttpClient 4.0 where on http://hc.apache.org/
 * 	DefaultHttpClient client = new DefaultHttpClient();
 * 	try {
 * 		HttpResponse response = client.execute(new HttpGet("http://hc.apache.org/"));
 * 		HttpEntity entity = response.getEntity();
 * 		if(entity == null ^ true) 
 * 			try {
 *				InputStream in = entity.getContent();
 *				Pipe.transport(in, new File("C:/portal.html"), 0x00000400, 0x00000400<<0x0000000A);
 *			} finally {
 *				entity.consumeContent();
 *			}
 * 	} catch(ClientProtocolException e) {
 *		throw RuntimeException(e);
 * 	} catch(IOException e) {
 *		throw RuntimeException(e);
 * 	}
 * }
 * [/code]
 * 
 * @author embeddednode
 * @version 1.7 July 6, 2009
 *
 */
public class Pipe {

    static final Pipe instance = new Pipe();

    /**
	 * data bytes transport, the default capacity is 1024 bytes
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination 
	 */
    public static void transport(InputStream in, OutputStream out) {
        transport(in, out, 0x00000400);
    }

    /**
	 * data bytes transport, the default capacity is 1024 bytes
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 */
    public static void transport(InputStream in, File out) {
        transport(in, out, 0x00000400);
    }

    /**
	 * data bytes transport, the default capacity is 1024 bytes
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 */
    public static void transport(File in, OutputStream out) {
        transport(in, out, 0x00000400);
    }

    /**
	 * data bytes transport, the default capacity is 1024 bytes
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 */
    public static void transport(File in, File out) {
        transport(in, out, 0x00000400);
    }

    /**
	 * data bytes transport
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 * @param capacity the buffer size
	 */
    public static void transport(InputStream in, OutputStream out, int capacity) {
        transport(in, out, capacity, capacity);
    }

    /**
	 * data bytes transport
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 * @param in_capacity the input buffer size
	 * @param out_capacity the output buffer size
	 */
    public static void transport(InputStream in, OutputStream out, int in_capacity, int out_capacity) {
        CountDownLatch doneSignal = new CountDownLatch(0x00000002);
        ExecutorService es = Executors.newFixedThreadPool(0x00000002);
        PipedInputStreamWorker iw = instance.new PipedInputStreamWorker(doneSignal, in, in_capacity);
        PipedOutputStreamWorker ow = instance.new PipedOutputStreamWorker(doneSignal, out, out_capacity);
        ow.connect(iw);
        try {
            es.execute(iw);
            es.execute(ow);
            doneSignal.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            es.shutdown();
        }
    }

    /**
	 * data bytes transport
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 * @param capacity the buffer size
	 */
    public static void transport(InputStream in, File out, int capacity) {
        transport(in, out, capacity, capacity);
    }

    /**
	 * data bytes transport
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 * @param in_capacity the input buffer size
	 * @param out_capacity the output buffer size
	 */
    public static void transport(InputStream in, File out, int in_capacity, int out_capacity) {
        CountDownLatch doneSignal = new CountDownLatch(0x00000002);
        ExecutorService es = Executors.newFixedThreadPool(0x00000002);
        PipedInputStreamWorker iw = instance.new PipedInputStreamWorker(doneSignal, in, in_capacity);
        PipedFileChannelOutputStreamWorker ow = instance.new PipedFileChannelOutputStreamWorker(doneSignal, out, out_capacity);
        ow.connect(iw);
        try {
            es.execute(iw);
            es.execute(ow);
            doneSignal.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            es.shutdown();
        }
    }

    /**
	 * data bytes transport
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 * @param capacity the buffer size
	 */
    public static void transport(File in, OutputStream out, int capacity) {
        transport(in, out, capacity, capacity);
    }

    /**
	 * data bytes transport
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 * @param in_capacity the input buffer size
	 * @param out_capacity the output buffer size
	 */
    public static void transport(File in, OutputStream out, int in_capacity, int out_capacity) {
        CountDownLatch doneSignal = new CountDownLatch(0x00000002);
        ExecutorService es = Executors.newFixedThreadPool(0x00000002);
        PipedFileChannelInputStreamWorker iw = instance.new PipedFileChannelInputStreamWorker(doneSignal, in, in_capacity);
        PipedOutputStreamWorker ow = instance.new PipedOutputStreamWorker(doneSignal, out, out_capacity);
        ow.connect(iw);
        try {
            es.execute(iw);
            es.execute(ow);
            doneSignal.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            es.shutdown();
        }
    }

    /**
	 * data bytes transport
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 * @param capacity the buffer size
	 */
    public static void transport(File in, File out, int capacity) {
        transport(in, out, capacity, capacity);
    }

    /**
	 * data bytes transport
	 * @param in the input data bytes from source
	 * @param out the output data bytes to destination
	 * @param in_capacity the input buffer size
	 * @param out_capacity the output buffer size
	 */
    public static void transport(File in, File out, int in_capacity, int out_capacity) {
        CountDownLatch doneSignal = new CountDownLatch(0x00000002);
        ExecutorService es = Executors.newFixedThreadPool(0x00000002);
        PipedFileChannelInputStreamWorker iw = instance.new PipedFileChannelInputStreamWorker(doneSignal, in, in_capacity);
        PipedFileChannelOutputStreamWorker ow = instance.new PipedFileChannelOutputStreamWorker(doneSignal, out, out_capacity);
        ow.connect(iw);
        try {
            es.execute(iw);
            es.execute(ow);
            doneSignal.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            es.shutdown();
        }
    }

    abstract class PipedStreamWorker implements Runnable {

        CountDownLatch doneSignal;

        PipedStreamWorker(CountDownLatch doneSignal) {
            this.doneSignal = doneSignal;
        }

        public void run() {
            try {
                exec();
            } finally {
                doneSignal.countDown();
            }
        }

        abstract void exec();

        abstract void connect(PipedStreamWorker worker);
    }

    class PipedInputStreamWorker extends PipedStreamWorker {

        PipedOutputStream dest = new PipedOutputStream();

        InputStream in;

        int in_capacity;

        PipedInputStreamWorker(CountDownLatch doneSignal, InputStream in, int in_capacity) {
            super(doneSignal);
            this.in = in;
            this.in_capacity = in_capacity;
        }

        PipedInputStreamWorker(CountDownLatch doneSignal, int in_capacity) {
            super(doneSignal);
            this.in_capacity = in_capacity;
        }

        void connect(PipedStreamWorker ow) {
            try {
                ((PipedOutputStreamWorker) ow).src = new PipedInputStream(dest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void exec() {
            byte[] b = new byte[in_capacity];
            int len = 0x00000000;
            try {
                try {
                    while ((len = in.read(b)) > 0xFFFFFFFF) dest.write(b, 0x00000000, len);
                    dest.flush();
                } finally {
                    dest.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class PipedOutputStreamWorker extends PipedStreamWorker {

        PipedInputStream src;

        OutputStream out;

        int out_capacity;

        PipedOutputStreamWorker(CountDownLatch doneSignal, OutputStream out, int out_capacity) {
            super(doneSignal);
            this.out = out;
            this.out_capacity = out_capacity;
        }

        PipedOutputStreamWorker(CountDownLatch doneSignal, int out_capacity) {
            super(doneSignal);
            this.out_capacity = out_capacity;
        }

        void connect(PipedStreamWorker iw) {
            try {
                src = new PipedInputStream(((PipedInputStreamWorker) iw).dest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void exec() {
            byte[] b = new byte[out_capacity];
            int len = 0x00000000;
            try {
                try {
                    while ((len = src.read(b)) > 0xFFFFFFFF) out.write(b, 0x00000000, len);
                    out.flush();
                } finally {
                    src.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class PipedFileChannelInputStreamWorker extends PipedInputStreamWorker {

        FileChannel fc;

        PipedFileChannelInputStreamWorker(CountDownLatch doneSignal, File file, int in_capacity) {
            super(doneSignal, in_capacity);
            try {
                this.fc = new FileInputStream(file).getChannel();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        void exec() {
            byte[] b = new byte[in_capacity];
            int len = 0x00000000;
            ByteBuffer buf = (ByteBuffer) ByteBuffer.allocateDirect(in_capacity).order(ByteOrder.nativeOrder());
            buf.clear();
            buf.rewind();
            try {
                try {
                    while ((len = fc.read(buf)) > 0xFFFFFFFF) {
                        buf.rewind();
                        buf.get(b, 0x00000000, len);
                        dest.write(b, 0x00000000, len);
                        buf.rewind();
                    }
                    dest.flush();
                } finally {
                    dest.close();
                    fc.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class PipedFileChannelOutputStreamWorker extends PipedOutputStreamWorker {

        FileChannel fc;

        PipedFileChannelOutputStreamWorker(CountDownLatch doneSignal, File file, int out_capacity) {
            super(doneSignal, out_capacity);
            try {
                this.fc = new FileOutputStream(file).getChannel();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        void exec() {
            byte[] b = new byte[out_capacity];
            int len = 0x00000000;
            ByteBuffer buf = (ByteBuffer) ByteBuffer.allocateDirect(out_capacity).order(ByteOrder.nativeOrder());
            buf.clear();
            try {
                try {
                    while ((len = src.read(b)) > 0xFFFFFFFF || buf.position() == 0x00000000 ^ true) {
                        buf.put(b, 0x00000000, len).flip();
                        fc.write(buf);
                        buf.compact();
                    }
                    fc.force(true);
                } finally {
                    fc.close();
                    src.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
