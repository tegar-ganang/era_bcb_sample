package edu.ds.p2p.daemons;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import rice.p2p.commonapi.NodeHandle;
import edu.ds.p2p.app.CycleSharingApp;
import edu.ds.p2p.app.MpiMessageSocketApp;
import edu.ds.p2p.app.NegotiatorApp;
import edu.ds.p2p.message.CommMessage;
import edu.ds.p2p.message.FileMessage;
import edu.ds.p2p.mpi.CommWorld;
import edu.ds.p2p.mpi.MpiFramework;
import edu.ds.p2p.util.JobRepo;

/**
 * Spwaned by the job monitor thread to handel a single remote job
 * @author Team falcon
 *
 */
public class RemoteJobThread implements Runnable {

    CommMessage comMsg;

    List<FileMessage> msg;

    int jobId;

    /**
	 * Constructor
	 * @param id
	 */
    public RemoteJobThread(int id) {
        this.jobId = id;
        this.comMsg = JobRepo.getCommMessage(id);
        this.msg = comMsg.getMsg();
    }

    @Override
    public void run() {
        PrintStream prt = null;
        try {
            NegotiatorApp nego = (NegotiatorApp) CycleSharingApp.getApp("negotiator");
            nego.startJob();
            MpiFramework frame = null;
            prt = System.out;
            MpiMessageSocketApp app = (MpiMessageSocketApp) CycleSharingApp.getApp("messageSock");
            NodeHandle handle = app.getNodeHandle();
            int rank = comMsg.getRank().getRank(handle);
            File out = new File(JobRepo.getRemoteJobProperty(jobId, "job.dir") + File.separator + "out-" + rank);
            out.createNewFile();
            JobRepo.addRemoteJobProperty(jobId, "job.out", out.getAbsolutePath());
            Class<?> main = null;
            ClassLoader cl = new URLClassLoader(new URL[] { new File(JobRepo.getRemoteJobProperty(jobId, "job.dir")).toURL() });
            for (FileMessage file : msg) {
                String fileName = file.getFileName();
                if (fileName.endsWith(".class")) {
                    Class<?> clazz = cl.loadClass(fileName.replace(".class", ""));
                    if (clazz.getName().equalsIgnoreCase(comMsg.getMainClass())) main = clazz;
                }
            }
            frame = (MpiFramework) main.newInstance();
            CommWorld world = comMsg.getWorld();
            world.setRank(rank);
            world.setSize();
            world.setOutDir(JobRepo.getRemoteJobProperty(jobId, "job.dir") + File.separator);
            frame.setWorld(comMsg.getWorld());
            System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(out))));
            frame.execute(comMsg.getArgs());
            System.out.close();
            System.setOut(prt);
            if (rank == 0) {
                Thread.sleep(2000);
                CommMessage msg = JobRepo.getCommMessage(jobId);
                FileMessage fMsg = new FileMessage("out.zip", false);
                fMsg.setData(compress(JobRepo.getRemoteJobProperty(jobId, "job.dir")));
                fMsg.setOut(true);
                fMsg.setJobId(msg.getJobId());
                fMsg.setFrom(app.getNodeHandle());
                fMsg.setHost(msg.getHost());
                app.setOutToHost(fMsg, msg.getHost());
            } else app.sendOutFiles(jobId);
            nego.endJob();
            frame = null;
        } catch (Throwable e) {
            if (prt != null) System.setOut(prt);
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }

    /**
	 * Performs compression of files located at specified location
	 * @param path
	 * @return
	 * @throws IOException
	 */
    private static byte[] compress(String path) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        BufferedOutputStream bf = new BufferedOutputStream(b);
        ZipOutputStream zip = new ZipOutputStream(bf);
        compress(path, "", zip);
        zip.close();
        bf.close();
        return b.toByteArray();
    }

    /**
	 * test method for  compression of the data
	 * @param path target path
	 * @param parent 
	 * @param zip
	 * @throws IOException
	 */
    private static void compress(String path, String parent, ZipOutputStream zip) throws IOException {
        File[] f = new File(path).listFiles();
        byte[] buffer = new byte[4096];
        int bytes_read;
        for (int i = 0; i < f.length; i++) {
            if (f[i].isFile()) {
                FileInputStream in = new FileInputStream(f[i]);
                ZipEntry entry = new ZipEntry(parent + f[i].getName());
                zip.putNextEntry(entry);
                while ((bytes_read = in.read(buffer)) != -1) zip.write(buffer, 0, bytes_read);
                in.close();
            } else if (f[i].isDirectory()) {
                compress(f[i].getAbsolutePath(), parent + f[i].getName() + File.separator, zip);
            }
        }
    }
}
