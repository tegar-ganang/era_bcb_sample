package com.vinny.xacml;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import com.vinny.xacml.event.TwoPhaseTransactionEvent;
import com.vinny.xacml.graph.DirectedGraphImpl;

public class MainClass {

    static boolean[] truetrue = { true, true };

    static boolean[] truefalse = { true, false };

    static boolean[] falsetrue = { false, true };

    static boolean[] falsefalse = { false, false };

    /**
	 * http://vmgump.apache.org/gump/public-jars/jakarta-commons-dormant/jars/
	 * http://svn.apache.org/viewvc/jakarta/commons/dormant/ graph2
	 * http://svn.apache.org/repos/asf/jakarta/commons/dormant/graph2/trunk/
	 * svn checkout http://svn.apache.org/repos/asf/spamassassin/trunk spamassassin
	 */
    public static void main(String[] args) {
        try {
            out = new PrintStream(new FileOutputStream(logfile, true), true);
        } catch (FileNotFoundException e) {
            out.println(e.getMessage());
        }
        if (args.length <= 1) {
            for (int i = 2; i < 10; i++) {
                try {
                    String[] cmdarray = new String[2];
                    cmdarray[0] = "java";
                    cmdarray[1] = "-version";
                    Process proc = Runtime.getRuntime().exec(cmdarray, null);
                } catch (IOException e) {
                    out.println(e.getMessage());
                }
            }
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            out.print("TEST  nodes: 	" + args[0] + " depth: 	" + args[1] + " conflicts: 	" + args[2] + " 	");
            runConstructTreeSize(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        }
    }

    public static void printToFile(String data) {
        out.println(data);
    }

    private static File logfile = new File("test-results.txt");

    private static PrintStream out;

    public static int NETWORK_LATENCY_MILLISEC = 200;

    public static int NETWORK_RADOMNESS_MILLISEC = 0;

    public static int QUEUE_LATENCY_MILLISEC = 0;

    public static int QUEUE_RADOMNESS_MILLISEC = 0;

    private static List<PDP> pdpList = new ArrayList<PDP>();

    private static volatile int nodeid = 0;

    private static Hashtable<String, PDP> pdpTable = new Hashtable<String, PDP>();

    private static List<String> pdpOrder = new ArrayList<String>();

    public static void runConstructTreeSize(int noOfNodes, int desiredDepth, int conflictsBetweenNodes) {
        int connectivity = findConnectivity(noOfNodes, desiredDepth);
        System.out.println("Max no of child nodes: " + connectivity);
        TransactionResourceManager trm = TransactionResourceManager.getInstance();
        recursivelyConstructPDPs(noOfNodes, desiredDepth, conflictsBetweenNodes, connectivity, null);
        List<String> reversePdpOrder = new ArrayList<String>();
        for (String pdp : pdpOrder) {
            reversePdpOrder.add(pdp);
        }
        trm.setPdpOrder(reversePdpOrder);
        trm.setPdps(pdpTable);
        ThreadGroup group = new ThreadGroup("test");
        Thread trmThread = new Thread(group, trm);
        trmThread.start();
        for (PDP pdp : pdpList) {
            Thread pdpThread = new Thread(group, pdp);
            pdpThread.start();
        }
        TwoPhaseTransactionEvent e = new TwoPhaseTransactionEvent(new Object(), null, "TransactionID1", TwoPhaseTransactionEvent.P1_QUERY_TO_COMMIT);
        pdpList.get(0).enqueEvent(e);
    }

    private static PDP recursivelyConstructPDPs(int noOfNodes, int currentDepth, int conflictsBetweenNodes, int connectivity, PDP parentPdp) {
        String pdpId = "PDP-" + nodeid;
        int myid = nodeid;
        nodeid++;
        PDP pdp = new PDP(pdpId, parentPdp);
        pdpList.add(pdp);
        pdpTable.put(pdp.getPDPId(), pdp);
        pdpOrder.add(pdp.getPDPId());
        currentDepth--;
        if (currentDepth > 0) {
            for (int i = 0; i < connectivity; i++) {
                List<PDP> subPdpList = null;
                if (pdp.getSubPDPs() != null) {
                    subPdpList = pdp.getSubPDPs();
                } else {
                    subPdpList = new ArrayList<PDP>();
                }
                if ((nodeid) < noOfNodes) {
                    System.out.println(pdpId + ": adding child (" + (subPdpList.size() + 1) + "): " + (nodeid));
                    PDP child = recursivelyConstructPDPs(noOfNodes, currentDepth, conflictsBetweenNodes, connectivity, pdp);
                    subPdpList.add(child);
                    pdp.setSubPDPs(subPdpList);
                } else {
                    if (subPdpList.size() == 0) {
                        System.out.println(pdpId + ": end PDP");
                    } else {
                        System.out.println(pdpId + ": done adding (" + subPdpList.size() + ") children");
                    }
                    break;
                }
            }
        } else {
            System.out.println(pdpId + ": end PDP");
        }
        Transaction t = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> LPwritelist = new ArrayList<String>();
            List<String> LDwritelist = new ArrayList<String>();
            List<String> GPwritelist = new ArrayList<String>();
            List<String> GDwritelist = new ArrayList<String>();
            int READSIZE = 1;
            for (int i = 0; i < READSIZE; i++) {
                readlist.add("ID-" + (myid + conflictsBetweenNodes + 1));
            }
            if ((myid + 1) >= (noOfNodes - conflictsBetweenNodes)) {
                for (int i = 0; i < conflictsBetweenNodes; i++) {
                    readlist.add("ID-" + (i + 1));
                    GPwritelist.add("ID-" + (i + 1));
                    LDwritelist.add("ID-" + (i + 1));
                }
            }
            t = new Transaction("TransactionID1", readlist, LPwritelist, LDwritelist, GPwritelist, GDwritelist, pdpId, Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        pdp.addTransaction(t);
        return pdp;
    }

    private static int findConnectivity(int noOfNodes, int desiredDepth) {
        if (noOfNodes > 1 && desiredDepth <= 1) {
            System.out.println("not possible");
            System.exit(1);
        }
        int conn = 0;
        while (true) {
            double count = 0;
            for (int i = 0; i < desiredDepth; i++) {
                count = count + Math.pow(conn, i);
            }
            if (count >= noOfNodes) {
                return conn;
            }
            conn++;
        }
    }

    public static void runSimulationTest2() {
        TransactionResourceManager trm = TransactionResourceManager.getInstance();
        PDP mainPDP = new PDP("PDP1", null);
        PDP sub1PDP = new PDP("PDP1.1", mainPDP);
        PDP sub2PDP = new PDP("PDP1.2", mainPDP);
        PDP sub11PDP = new PDP("PDP1.1.1", sub1PDP);
        PDP sub12PDP = new PDP("PDP1.1.2", sub1PDP);
        PDP sub21PDP = new PDP("PDP1.2.1", sub2PDP);
        {
            ArrayList<PDP> subPdpList = new ArrayList<PDP>();
            subPdpList.add(sub1PDP);
            subPdpList.add(sub2PDP);
            mainPDP.setSubPDPs(subPdpList);
        }
        {
            ArrayList<PDP> subPdpList = new ArrayList<PDP>();
            subPdpList.add(sub11PDP);
            subPdpList.add(sub12PDP);
            sub1PDP.setSubPDPs(subPdpList);
        }
        {
            ArrayList<PDP> subPdpList = new ArrayList<PDP>();
            subPdpList.add(sub21PDP);
            sub2PDP.setSubPDPs(subPdpList);
        }
        String transactionID = "TID-1";
        Transaction t1 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> LPwritelist = new ArrayList<String>();
            List<String> LDwritelist = new ArrayList<String>();
            List<String> GPwritelist = new ArrayList<String>();
            List<String> GDwritelist = new ArrayList<String>();
            readlist.add("ID-1");
            readlist.add("ID-2");
            readlist.add("ID-3");
            GPwritelist.add("ID-2");
            LDwritelist.add("ID-3");
            t1 = new Transaction(transactionID, readlist, LPwritelist, LDwritelist, GPwritelist, GDwritelist, "PDP1", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        mainPDP.addTransaction(t1);
        Transaction t1_1 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> LPwritelist = new ArrayList<String>();
            List<String> LDwritelist = new ArrayList<String>();
            List<String> GPwritelist = new ArrayList<String>();
            List<String> GDwritelist = new ArrayList<String>();
            readlist.add("ID-11");
            readlist.add("ID-12");
            readlist.add("ID-13");
            GPwritelist.add("ID-12");
            LDwritelist.add("ID-13");
            t1_1 = new Transaction(transactionID, readlist, LPwritelist, LDwritelist, GPwritelist, GDwritelist, "PDP1.1", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        sub1PDP.addTransaction(t1_1);
        Transaction t1_2 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> LPwritelist = new ArrayList<String>();
            List<String> LDwritelist = new ArrayList<String>();
            List<String> GPwritelist = new ArrayList<String>();
            List<String> GDwritelist = new ArrayList<String>();
            readlist.add("ID-21");
            readlist.add("ID-22");
            readlist.add("ID-23");
            GPwritelist.add("ID-22");
            LDwritelist.add("ID-23");
            t1_2 = new Transaction(transactionID, readlist, LPwritelist, LDwritelist, GPwritelist, GDwritelist, "PDP1.2", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        sub2PDP.addTransaction(t1_2);
        Transaction t1_1_1 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> LPwritelist = new ArrayList<String>();
            List<String> LDwritelist = new ArrayList<String>();
            List<String> GPwritelist = new ArrayList<String>();
            List<String> GDwritelist = new ArrayList<String>();
            readlist.add("ID-31");
            readlist.add("ID-32");
            readlist.add("ID-33");
            GPwritelist.add("ID-32");
            LDwritelist.add("ID-33");
            t1_1_1 = new Transaction(transactionID, readlist, LPwritelist, LDwritelist, GPwritelist, GDwritelist, "PDP1.1.1", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        sub11PDP.addTransaction(t1_1_1);
        Transaction t1_1_2 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> LPwritelist = new ArrayList<String>();
            List<String> LDwritelist = new ArrayList<String>();
            List<String> GPwritelist = new ArrayList<String>();
            List<String> GDwritelist = new ArrayList<String>();
            readlist.add("ID-41");
            readlist.add("ID-42");
            readlist.add("ID-43");
            GPwritelist.add("ID-42");
            LDwritelist.add("ID-43");
            t1_1_2 = new Transaction(transactionID, readlist, LPwritelist, LDwritelist, GPwritelist, GDwritelist, "PDP1.1.2", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        sub12PDP.addTransaction(t1_1_2);
        Transaction t1_2_1 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> LPwritelist = new ArrayList<String>();
            List<String> LDwritelist = new ArrayList<String>();
            List<String> GPwritelist = new ArrayList<String>();
            List<String> GDwritelist = new ArrayList<String>();
            readlist.add("ID-2");
            readlist.add("ID-52");
            readlist.add("ID-53");
            GPwritelist.add("ID-52");
            LDwritelist.add("ID-53");
            t1_2_1 = new Transaction(transactionID, readlist, LPwritelist, LDwritelist, GPwritelist, GDwritelist, "PDP1.2.1", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, falsefalse);
        }
        sub21PDP.addTransaction(t1_2_1);
        {
            Hashtable<String, PDP> pdpTable = new Hashtable<String, PDP>();
            pdpTable.put(mainPDP.getPDPId(), mainPDP);
            pdpTable.put(sub1PDP.getPDPId(), sub1PDP);
            pdpTable.put(sub2PDP.getPDPId(), sub2PDP);
            pdpTable.put(sub11PDP.getPDPId(), sub11PDP);
            pdpTable.put(sub12PDP.getPDPId(), sub12PDP);
            pdpTable.put(sub21PDP.getPDPId(), sub21PDP);
            List<String> pdpOrder = new ArrayList<String>();
            pdpOrder.add(mainPDP.getPDPId());
            pdpOrder.add(sub1PDP.getPDPId());
            pdpOrder.add(sub2PDP.getPDPId());
            pdpOrder.add(sub11PDP.getPDPId());
            pdpOrder.add(sub12PDP.getPDPId());
            pdpOrder.add(sub21PDP.getPDPId());
            trm.setPdpOrder(pdpOrder);
            trm.setPdps(pdpTable);
        }
        Thread trmThread = new Thread(trm);
        trmThread.start();
        {
            Thread mainPDPThread = new Thread(mainPDP);
            mainPDPThread.start();
            Thread sub1PDPThread = new Thread(sub1PDP);
            sub1PDPThread.start();
            Thread sub2PDPThread = new Thread(sub2PDP);
            sub2PDPThread.start();
            Thread sub11PDPThread = new Thread(sub11PDP);
            sub11PDPThread.start();
            Thread sub12PDPThread = new Thread(sub12PDP);
            sub12PDPThread.start();
            Thread sub21PDPThread = new Thread(sub21PDP);
            sub21PDPThread.start();
        }
        TwoPhaseTransactionEvent e = new TwoPhaseTransactionEvent(new Object(), null, transactionID, TwoPhaseTransactionEvent.P1_QUERY_TO_COMMIT);
        mainPDP.enqueEvent(e);
    }

    public static void runSimulationAllSuccess() {
        TransactionResourceManager trm = TransactionResourceManager.getInstance();
        boolean[] truetrue = { true, true };
        boolean[] truefalse = { true, false };
        boolean[] falsetrue = { false, true };
        boolean[] falsefalse = { false, false };
        PDP mainPDP = new PDP("PDP1", null);
        PDP sub1PDP = new PDP("PDP1.1", mainPDP);
        PDP sub2PDP = new PDP("PDP1.2", mainPDP);
        PDP sub11PDP = new PDP("PDP1.1.1", sub1PDP);
        PDP sub12PDP = new PDP("PDP1.1.2", sub1PDP);
        PDP sub21PDP = new PDP("PDP1.2.1", sub2PDP);
        {
            ArrayList<PDP> subPdpList = new ArrayList<PDP>();
            subPdpList.add(sub1PDP);
            subPdpList.add(sub2PDP);
            mainPDP.setSubPDPs(subPdpList);
        }
        {
            ArrayList<PDP> subPdpList = new ArrayList<PDP>();
            subPdpList.add(sub11PDP);
            subPdpList.add(sub12PDP);
            sub1PDP.setSubPDPs(subPdpList);
        }
        {
            ArrayList<PDP> subPdpList = new ArrayList<PDP>();
            subPdpList.add(sub21PDP);
            sub2PDP.setSubPDPs(subPdpList);
        }
        String transactionID = "TID-1";
        Transaction t1 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> writelist = new ArrayList<String>();
            readlist.add("ID-1");
            readlist.add("ID-2");
            readlist.add("ID-3");
            writelist.add("ID-2");
            writelist.add("ID-3");
            t1 = new Transaction(transactionID, readlist, writelist, writelist, writelist, writelist, "PDP1", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        mainPDP.addTransaction(t1);
        Transaction t1_1 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> writelist = new ArrayList<String>();
            readlist.add("ID-11");
            readlist.add("ID-12");
            readlist.add("ID-13");
            writelist.add("ID-12");
            writelist.add("ID-13");
            t1_1 = new Transaction(transactionID, readlist, writelist, writelist, writelist, writelist, "PDP1.1", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        sub1PDP.addTransaction(t1_1);
        Transaction t1_2 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> writelist = new ArrayList<String>();
            readlist.add("ID-2");
            readlist.add("ID-22");
            readlist.add("ID-23");
            writelist.add("ID-22");
            writelist.add("ID-23");
            t1_2 = new Transaction(transactionID, readlist, writelist, writelist, writelist, writelist, "PDP1.2", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        sub2PDP.addTransaction(t1_2);
        Transaction t1_1_1 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> writelist = new ArrayList<String>();
            readlist.add("ID-31");
            readlist.add("ID-2");
            readlist.add("ID-33");
            writelist.add("ID-2");
            writelist.add("ID-33");
            t1_1_1 = new Transaction(transactionID, readlist, writelist, writelist, writelist, writelist, "PDP1.1.1", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        sub11PDP.addTransaction(t1_1_1);
        Transaction t1_1_2 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> writelist = new ArrayList<String>();
            readlist.add("ID-41");
            readlist.add("ID-2");
            readlist.add("ID-43");
            writelist.add("ID-41");
            writelist.add("ID-43");
            t1_1_2 = new Transaction(transactionID, readlist, writelist, writelist, writelist, writelist, "PDP1.1.2", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        sub12PDP.addTransaction(t1_1_2);
        Transaction t1_2_1 = null;
        {
            List<String> readlist = new ArrayList<String>();
            List<String> writelist = new ArrayList<String>();
            readlist.add("ID-2");
            readlist.add("ID-52");
            readlist.add("ID-3");
            writelist.add("ID-52");
            writelist.add("ID-3");
            t1_2_1 = new Transaction(transactionID, readlist, writelist, writelist, writelist, writelist, "PDP1.2.1", Transaction.COMBINING_ALGORITHM_DENY_OVERRIDES, truetrue);
        }
        sub21PDP.addTransaction(t1_2_1);
        {
            Hashtable<String, PDP> pdpTable = new Hashtable<String, PDP>();
            pdpTable.put(mainPDP.getPDPId(), mainPDP);
            pdpTable.put(sub1PDP.getPDPId(), sub1PDP);
            pdpTable.put(sub2PDP.getPDPId(), sub2PDP);
            pdpTable.put(sub11PDP.getPDPId(), sub11PDP);
            pdpTable.put(sub12PDP.getPDPId(), sub12PDP);
            pdpTable.put(sub21PDP.getPDPId(), sub21PDP);
            List<String> pdpOrder = new ArrayList<String>();
            pdpOrder.add(mainPDP.getPDPId());
            pdpOrder.add(sub1PDP.getPDPId());
            pdpOrder.add(sub2PDP.getPDPId());
            pdpOrder.add(sub11PDP.getPDPId());
            pdpOrder.add(sub12PDP.getPDPId());
            pdpOrder.add(sub21PDP.getPDPId());
            trm.setPdpOrder(pdpOrder);
            trm.setPdps(pdpTable);
        }
        Thread trmThread = new Thread(trm);
        trmThread.start();
        {
            Thread mainPDPThread = new Thread(mainPDP);
            mainPDPThread.start();
            Thread sub1PDPThread = new Thread(sub1PDP);
            sub1PDPThread.start();
            Thread sub2PDPThread = new Thread(sub2PDP);
            sub2PDPThread.start();
            Thread sub11PDPThread = new Thread(sub11PDP);
            sub11PDPThread.start();
            Thread sub12PDPThread = new Thread(sub12PDP);
            sub12PDPThread.start();
            Thread sub21PDPThread = new Thread(sub21PDP);
            sub21PDPThread.start();
        }
        TwoPhaseTransactionEvent e = new TwoPhaseTransactionEvent(new Object(), null, transactionID, TwoPhaseTransactionEvent.P1_QUERY_TO_COMMIT);
        mainPDP.enqueEvent(e);
    }

    public static void graphTest() {
        DirectedGraphImpl d = new DirectedGraphImpl(20);
        d.addVertex("Serialized");
        d.addVertex("PDP1");
        d.addVertex("PDP2");
        d.addVertex("PDP3");
        d.addEdge("PDP1", "PDP3", 1);
        d.addEdge("PDP1", "PDP2", 1);
        d.addEdge("PDP1", "PDP1", 1);
        {
            Object[] a1 = d.topologicalSort();
            if (a1 != null) {
                System.out.println("Topological array of graph " + d.hashCode() + ":");
                for (int i = 0; i < a1.length; i++) {
                    System.out.println("\t\t" + (i + 1) + ": " + a1[i]);
                }
            } else {
                System.out.println("Topological sort not possible.");
            }
        }
    }
}
