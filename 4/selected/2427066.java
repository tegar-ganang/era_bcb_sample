package edu.tsinghua.keg.distributedlda;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;
import edu.tsinghua.keg.distributedlda.gibbs.Document;
import edu.tsinghua.keg.distributedlda.gibbs.SamplingManager;
import edu.tsinghua.keg.distributedlda.nio.SocketEventListener;
import edu.tsinghua.keg.distributedlda.nio.SocketListenerThread;
import edu.tsinghua.keg.distributedlda.protocol.MessageTypes;

public class SamplingService implements SocketEventListener {

    String serverAddr;

    int serverPort;

    int localPort;

    SocketListenerThread slt;

    Hashtable<String, SamplingManager> jobs = new Hashtable<String, SamplingManager>();

    public SamplingService(String serverAddr, int serverPort, int localPort) {
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.localPort = localPort;
    }

    public void start() throws IOException {
        slt = new SocketListenerThread(localPort, this);
        slt.start();
        Socket sock = new Socket(serverAddr, serverPort);
        DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
        dos.writeInt(MessageTypes.ClientRegister);
        dos.writeInt(localPort);
        dos.flush();
        dos.close();
        sock.close();
    }

    public void close() {
        slt.close();
    }

    @Override
    public void message(String msg) {
        System.out.println(msg);
    }

    @Override
    public void newConnection(Socket socket) {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            int msgType = dis.readInt();
            SamplingManager job = null;
            StringBuffer sb = null;
            int jobNameLength;
            switch(msgType) {
                case MessageTypes.StatusQuery:
                    dos.writeInt(MessageTypes.ServiceAvailable);
                    dos.flush();
                    break;
                case MessageTypes.PrepareDeliverNewJob:
                    jobNameLength = dis.readInt();
                    sb = new StringBuffer();
                    for (int i = 0; i < jobNameLength; i++) {
                        char c = dis.readChar();
                        sb.append(c);
                    }
                    boolean docInMem = dis.readBoolean();
                    boolean reportImm = dis.readBoolean();
                    job = new SamplingManager(sb.toString(), this, docInMem, reportImm);
                    jobs.put(sb.toString(), job);
                    job.prepareReceiveNewJob();
                    dos.writeInt(MessageTypes.ReceiveNewJobReady);
                    int mark = dis.readInt();
                    while (mark == MessageTypes.NewDocument) {
                        int length = dis.readInt();
                        byte[] bytes = new byte[length];
                        int readBytes = 0;
                        while (readBytes < length) {
                            readBytes += dis.read(bytes, readBytes, length - readBytes);
                        }
                        Document doc = Document.fromCompressedBytes(bytes);
                        job.receiveNewDoc(doc);
                        mark = dis.readInt();
                    }
                    if (mark == MessageTypes.JobDeliverFinished) {
                        job.newJobFinished();
                        dos.writeInt(job.receivedDocs);
                        System.out.println("received " + job.receivedDocs + " documents for Job(" + sb.toString() + ")");
                    }
                    dos.flush();
                    break;
                case MessageTypes.InitializeSampling:
                    jobNameLength = dis.readInt();
                    sb = new StringBuffer();
                    for (int i = 0; i < jobNameLength; i++) {
                        char c = dis.readChar();
                        sb.append(c);
                    }
                    double alpha = dis.readDouble();
                    double beta = dis.readDouble();
                    int num_topics = dis.readInt();
                    int num_words = dis.readInt();
                    job = jobs.get(sb.toString());
                    job.initialize(alpha, beta, num_topics, num_words);
                    break;
                case MessageTypes.NextIteration:
                    jobNameLength = dis.readInt();
                    sb = new StringBuffer();
                    for (int i = 0; i < jobNameLength; i++) {
                        char c = dis.readChar();
                        sb.append(c);
                    }
                    boolean toSave = dis.readBoolean();
                    int iter = dis.readInt();
                    job = jobs.get(sb.toString());
                    int chainLen = dis.readInt();
                    boolean next = false;
                    DataOutputStream nextdos = null;
                    if (chainLen > 0) {
                        next = true;
                        StringBuffer chainB = new StringBuffer();
                        for (int i = 0; i < chainLen; i++) {
                            char c = dis.readChar();
                            chainB.append(c);
                        }
                        int firstPart = chainB.indexOf("\t");
                        String nextChain = "";
                        String[] tks;
                        if (firstPart == -1) {
                            tks = chainB.toString().split(":");
                        } else {
                            nextChain = chainB.substring(firstPart + 1);
                            tks = chainB.substring(0, firstPart).split(":");
                        }
                        Socket nextsock = new Socket(tks[0], Integer.parseInt(tks[1]));
                        nextdos = new DataOutputStream(nextsock.getOutputStream());
                        nextdos.writeInt(MessageTypes.NextIteration);
                        nextdos.writeInt(sb.length());
                        nextdos.writeChars(sb.toString());
                        nextdos.writeBoolean(toSave);
                        nextdos.writeInt(iter);
                        nextdos.writeInt(nextChain.length());
                        if (nextChain.length() > 0) nextdos.writeChars(nextChain);
                    }
                    int length = dis.readInt();
                    if (next) {
                        nextdos.writeInt(length);
                        nextdos.flush();
                    }
                    byte[] bytes = new byte[length];
                    int readBytes = 0;
                    while (readBytes < length) {
                        int read = dis.read(bytes, readBytes, length - readBytes);
                        if (next) {
                            nextdos.write(bytes, readBytes, read);
                            nextdos.flush();
                        }
                        readBytes += read;
                    }
                    if (next) {
                        nextdos.close();
                    }
                    ByteArrayInputStream bytein = new ByteArrayInputStream(bytes);
                    ZInputStream zIn = new ZInputStream(bytein);
                    DataInputStream nwIn = new DataInputStream(zIn);
                    int[][] newNw = new int[job.sampler.num_words][job.sampler.num_topics];
                    for (int i = 0; i < job.sampler.num_words; i++) {
                        for (int j = 0; j < job.sampler.num_topics; j++) {
                            newNw[i][j] = nwIn.readInt();
                        }
                    }
                    nwIn.close();
                    job.nextIteration(newNw, toSave, iter);
                    break;
                case MessageTypes.ReportTassign:
                    jobNameLength = dis.readInt();
                    sb = new StringBuffer();
                    for (int i = 0; i < jobNameLength; i++) {
                        char c = dis.readChar();
                        sb.append(c);
                    }
                    job = jobs.get(sb.toString());
                    job.prepareToReportTassign();
                    Document doc = null;
                    int reported = 0;
                    while ((doc = job.reportNextTassignDocument()) != null) {
                        dos.writeInt(MessageTypes.NewDocument);
                        byte[] bytesRep = doc.compressToBytes();
                        dos.writeInt(bytesRep.length);
                        dos.write(bytesRep);
                        dos.flush();
                        reported++;
                    }
                    dos.writeInt(MessageTypes.TassignReportFinished);
                    System.out.println("Finished reporting tassign of " + reported + " documents.\tAt:" + new Date(System.currentTimeMillis()));
                    dos.flush();
                    break;
                case MessageTypes.ClearJob:
                    jobNameLength = dis.readInt();
                    sb = new StringBuffer();
                    for (int i = 0; i < jobNameLength; i++) {
                        char c = dis.readChar();
                        sb.append(c);
                    }
                    job = jobs.remove(sb.toString());
                    if (job != null) job.clear();
                    System.out.println("Job(" + sb.toString() + ") is removed.");
                    break;
                case MessageTypes.PrepareContinue:
                    jobNameLength = dis.readInt();
                    sb = new StringBuffer();
                    for (int i = 0; i < jobNameLength; i++) {
                        char c = dis.readChar();
                        sb.append(c);
                    }
                    int fromIter = dis.readInt();
                    boolean docInMem2 = dis.readBoolean();
                    boolean reportImm2 = dis.readBoolean();
                    if (jobs.containsKey(sb.toString())) {
                        dos.writeInt(-1);
                    }
                    job = new SamplingManager(sb.toString(), this, docInMem2, reportImm2);
                    jobs.put(sb.toString(), job);
                    job.prepareContinue(fromIter);
                    break;
            }
            dis.close();
            dos.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @Override
    public void socketDown() {
        System.out.println("Listening on socket error.");
        slt.close();
    }

    @Override
    public void socketUp() {
        System.out.println("Start listen on socket.");
    }

    public void reportNewNw(int[][] newNw, String jobName, int iter) {
        Socket sock;
        try {
            sock = new Socket(serverAddr, serverPort);
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
            dos.writeInt(MessageTypes.ReportNewNw);
            dos.writeInt(jobName.length());
            dos.writeChars(jobName);
            ByteArrayOutputStream nwout = new ByteArrayOutputStream();
            ZOutputStream zOut = new ZOutputStream(nwout, JZlib.Z_BEST_COMPRESSION);
            DataOutputStream nwdout = new DataOutputStream(zOut);
            for (int i = 0; i < newNw.length; i++) {
                for (int j = 0; j < newNw[i].length; j++) {
                    nwdout.writeInt(newNw[i][j]);
                }
            }
            nwdout.close();
            byte[] bytes = nwout.toByteArray();
            dos.writeInt(bytes.length);
            dos.flush();
            dos.write(bytes);
            dos.flush();
            dos.close();
            sock.close();
        } catch (UnknownHostException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public void reportPrepareContinue(String jobName, int docNum) {
        Socket sock;
        try {
            sock = new Socket(serverAddr, serverPort);
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
            dos.writeInt(MessageTypes.ReportPrepareContinue);
            dos.writeInt(jobName.length());
            dos.writeChars(jobName);
            dos.writeInt(docNum);
            dos.close();
            sock.close();
        } catch (UnknownHostException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        if (docNum == -1) {
            jobs.remove(jobName);
        }
    }

    public void reportNwChange(String jobName, ArrayList<int[]> toReport) {
        Socket sock;
        try {
            sock = new Socket(serverAddr, serverPort);
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
            dos.writeInt(MessageTypes.ReportNwChange);
            dos.writeInt(jobName.length());
            dos.writeChars(jobName);
            ByteArrayOutputStream nwout = new ByteArrayOutputStream();
            ZOutputStream zOut = new ZOutputStream(nwout, JZlib.Z_BEST_COMPRESSION);
            DataOutputStream nwdout = new DataOutputStream(zOut);
            nwdout.writeInt(toReport.size());
            for (int[] rec : toReport) {
                nwdout.writeInt(rec[0]);
                nwdout.writeInt(rec[1]);
                nwdout.writeInt(rec[2]);
            }
            nwdout.close();
            byte[] bytes = nwout.toByteArray();
            dos.writeInt(bytes.length);
            dos.flush();
            dos.write(bytes);
            dos.flush();
            dos.close();
            sock.close();
        } catch (UnknownHostException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public void finishReportNwChange(String jobName, ArrayList<int[]> toReport) {
        Socket sock;
        try {
            sock = new Socket(serverAddr, serverPort);
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
            dos.writeInt(MessageTypes.FinishReportNwChange);
            dos.writeInt(jobName.length());
            dos.writeChars(jobName);
            if (toReport != null && toReport.size() > 0) {
                ByteArrayOutputStream nwout = new ByteArrayOutputStream();
                ZOutputStream zOut = new ZOutputStream(nwout, JZlib.Z_BEST_COMPRESSION);
                DataOutputStream nwdout = new DataOutputStream(zOut);
                nwdout.writeInt(toReport.size());
                for (int[] rec : toReport) {
                    nwdout.writeInt(rec[0]);
                    nwdout.writeInt(rec[1]);
                    nwdout.writeInt(rec[2]);
                }
                nwdout.close();
                byte[] bytes = nwout.toByteArray();
                dos.writeInt(bytes.length);
                dos.flush();
                dos.write(bytes);
                dos.flush();
                dos.close();
            } else {
                dos.writeInt(0);
                dos.close();
            }
            sock.close();
        } catch (UnknownHostException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
