package org.apache.hadoop.mapred;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.nio.*;
import java.nio.channels.*;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.DF;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSError;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.HadoopCacheFileSystem;
import org.apache.hadoop.fs.HadoopCacheFileChunk;
import org.apache.hadoop.fs.LocalDirAllocator;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.mapred.InterTrackerProtocol;
import org.apache.hadoop.mapred.JobClient.TaskStatusFilter;
import org.apache.hadoop.mapred.JobTracker;
import org.apache.hadoop.mapred.TaskStatus.Phase;
import org.apache.hadoop.mapred.pipes.Submitter;
import org.apache.hadoop.metrics.MetricsContext;
import org.apache.hadoop.metrics.MetricsException;
import org.apache.hadoop.metrics.MetricsRecord;
import org.apache.hadoop.metrics.MetricsUtil;
import org.apache.hadoop.metrics.Updater;
import org.apache.hadoop.metrics.jvm.JvmMetrics;
import org.apache.hadoop.net.DNS;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.util.DiskChecker;
import org.apache.hadoop.util.ProcfsBasedProcessTree;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.RunJar;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.util.VersionInfo;
import org.apache.hadoop.util.DiskChecker.DiskErrorException;
import org.apache.hadoop.util.Shell.ShellCommandExecutor;
import org.apache.log4j.LogManager;

public class HadoopCache implements Runnable, HadoopCacheProtocol {

    public static final Log LOG = LogFactory.getLog(HadoopCache.class);

    protected LinkedBlockingQueue<cachingQueueElement> fileBlockCachingQueue;

    protected HadoopCacheFileSystem fileBlockStorage;

    private int handlerCount;

    protected InterTrackerProtocol jobClient;

    protected String hostName;

    public HadoopCache(int numThread) {
        fileBlockCachingQueue = new LinkedBlockingQueue<cachingQueueElement>();
        JobConf conf = new JobConf();
        int cacheSize = conf.getInt("mapred.hadoopcache.size.mb", 100);
        cacheSize *= (1024 * 1024);
        fileBlockStorage = new HadoopCacheFileSystem(cacheSize);
        handlerCount = numThread;
        try {
            InetSocketAddress jobTrackAddr = JobTracker.getAddress(conf);
            this.jobClient = (InterTrackerProtocol) RPC.waitForProxy(InterTrackerProtocol.class, InterTrackerProtocol.versionID, jobTrackAddr, conf);
            InetAddress localMachine = InetAddress.getLocalHost();
            this.hostName = localMachine.getCanonicalHostName();
        } catch (UnknownHostException e) {
            LOG.error("Can't get host name!" + e);
            this.hostName = "";
        } catch (IOException e) {
            LOG.error("InterTrackerProtocol init failed!" + e);
        }
        for (int i = 0; i < handlerCount; i++) {
            new FileBlockCachingThread(fileBlockCachingQueue, fileBlockStorage, jobClient, hostName).start();
        }
        LOG.debug("HadoopCache - init!");
    }

    public void run() {
        try {
            JobConf conf = new JobConf();
            InetSocketAddress addr = getAddress(conf);
            Server HadoopCacheFrontServer = RPC.getServer(this, addr.getHostName(), addr.getPort(), 1, false, conf);
            HadoopCacheFrontServer.start();
            LOG.info("aopCommServer RUNNING");
            HadoopCacheFrontServer.join();
            LOG.info("Stopped aopCommServer");
        } catch (IOException e) {
            LOG.info("IOException occurred: " + e);
        } catch (InterruptedException e) {
            LOG.info("InterruptedException occurred: " + e);
        }
    }

    public static InetSocketAddress getAddress(Configuration conf) {
        String jobTrackerStr = conf.get("mapred.hadoopcache.address", "localhost:9650");
        return NetUtils.createSocketAddr(jobTrackerStr);
    }

    public long getProtocolVersion(String protocol, long clientVersion) throws IOException {
        if (protocol.equals(HadoopCacheProtocol.class.getName())) {
            return HadoopCacheProtocol.versionID;
        } else {
            throw new IOException("Unknown protocol to HadoopCache: " + protocol);
        }
    }

    public String open(Configuration job, FileSplit split) throws IOException {
        System.out.println("HadoopCache.open: split: " + split);
        try {
            long start = split.getStart();
            long end = start + split.getLength();
            final Path file = split.getPath();
            String filePath = file.toUri().getPath();
            HadoopCacheFileChunk hChunk = fileBlockStorage.getFileChunk(filePath, start, (end - start), false);
            if (hChunk == null) {
                System.out.println("HadoopCache.open: exp: " + job.get("exp.id", "none") + " fault " + split.getLength() + " filePath: " + split);
                start = Math.max(start - 4096, 0);
                end = end + (16 * 1024);
                hChunk = fileBlockStorage.reserveSpaceForNewFile(filePath, start, (end - start));
                fileBlockCachingQueue.put(new cachingQueueElement(job, split, hChunk));
            } else {
                System.out.println("HadoopCache.open: exp: " + job.get("exp.id", "none") + "  hit  " + split.getLength() + " filePath: " + split);
            }
            return hChunk.getCachePath();
        } catch (InterruptedException e) {
            LOG.error("Can't add new element in the fileBlockCachingQueue.\n" + e);
            return null;
        }
    }

    public String testFunc() throws IOException {
        return "testFunc";
    }

    public static void main(String argv[]) throws Exception {
        StringUtils.startupShutdownMessage(HadoopCache.class, argv, LOG);
        if (argv.length != 0) {
            System.out.println("usage: HadoopCache");
            System.exit(-1);
        }
        try {
            new HadoopCache(4).run();
        } catch (Throwable e) {
            LOG.error("Can not start HadoopCache because " + StringUtils.stringifyException(e));
            System.exit(-1);
        }
    }
}

/**
 *  FileBlockCahingQueue consumer
 */
class FileBlockCachingThread extends Thread {

    private static Log LOG = LogFactory.getLog(FileBlockCachingThread.class);

    private final int READ_BUFFER_SIZE = 64 * 1024;

    private LinkedBlockingQueue<cachingQueueElement> fileBlockCachingQueue;

    private HadoopCacheFileSystem fileBlockStorage;

    private InterTrackerProtocol jobClient;

    private String hostName;

    public FileBlockCachingThread(LinkedBlockingQueue<cachingQueueElement> queue, HadoopCacheFileSystem cache, InterTrackerProtocol jobClient, String hostName) {
        this.fileBlockCachingQueue = queue;
        this.fileBlockStorage = cache;
        this.jobClient = jobClient;
        this.hostName = hostName;
    }

    @Override
    public void run() {
        LOG.debug("Starting thread: " + this.getClass());
        while (true) {
            try {
                cachingQueueElement cachingObject = fileBlockCachingQueue.take();
                cacheFileBlock(cachingObject);
                System.gc();
            } catch (InterruptedException e) {
                LOG.error("FileBlockCachingThread.run\n" + e);
                e.printStackTrace();
                continue;
            }
        }
    }

    private void cacheFileBlock(cachingQueueElement cachingObject) {
        try {
            Configuration conf = cachingObject.getConf();
            FileSplit split = cachingObject.getSplit();
            HadoopCacheFileChunk hChunk = cachingObject.getCacheChunk();
            long start = hChunk.getOffset();
            long end = start + hChunk.getSize();
            final Path file = split.getPath();
            LOG.debug("caching file blocks: " + file + ", start: " + start + ", end: " + end);
            FileSystem fs = file.getFileSystem(conf);
            FSDataInputStream fileIn = fs.open(split.getPath());
            RandomAccessFile fout = new RandomAccessFile(hChunk.getCachePath(), "rw");
            FileChannel fcOut = fout.getChannel();
            MappedByteBuffer mbb = fcOut.map(FileChannel.MapMode.READ_WRITE, 0, HadoopCacheFileChunk.FRONT_HEADER_SIZE);
            int headerSize = mbb.getInt();
            long offset = mbb.getLong();
            long size = mbb.getLong();
            mbb = fcOut.map(FileChannel.MapMode.READ_WRITE, 0, (headerSize + size));
            mbb.position(headerSize);
            byte[] readBuf = new byte[READ_BUFFER_SIZE];
            long currPos = start;
            int desiredSize, readSize;
            fileIn.seek(start);
            while (currPos < end) {
                readSize = fileIn.read(readBuf);
                if (readSize > 0) {
                    desiredSize = Math.min(readSize, (int) (end - currPos));
                    mbb.put(readBuf, 0, desiredSize);
                    currPos += readSize;
                    hChunk.setValidSize((hChunk.getValidSize() + desiredSize));
                    mbb.putLong(HadoopCacheFileChunk.VALID_SIZE, hChunk.getValidSize());
                } else {
                    end = currPos;
                    size = end - start;
                    hChunk.resize(size);
                    mbb.putLong(HadoopCacheFileChunk.SIZE, size);
                    fcOut = fcOut.truncate(headerSize + size);
                    break;
                }
            }
            fileIn.close();
            fcOut.close();
            fout.close();
            mbb = null;
            fcOut = null;
            fout = null;
            jobClient.addNewCacheBlockLocation(hostName, hChunk.getPath(), start, end);
        } catch (IOException e) {
            LOG.error("FileBlockCachingThread.cacheFileBlock\n" + e);
            e.printStackTrace();
        }
    }
}

class cachingQueueElement {

    private Configuration conf;

    private FileSplit split;

    private HadoopCacheFileChunk hChunk;

    public cachingQueueElement(Configuration conf, FileSplit split, HadoopCacheFileChunk hChunk) {
        this.conf = conf;
        this.split = split;
        this.hChunk = hChunk;
    }

    public Configuration getConf() {
        return conf;
    }

    public FileSplit getSplit() {
        return split;
    }

    public HadoopCacheFileChunk getCacheChunk() {
        return hChunk;
    }
}
