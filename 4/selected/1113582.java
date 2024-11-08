package org.netbeans.server.snapshots;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUCCTContainer;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.PrestimeCPUCCTNode;
import org.netbeans.modules.exceptions.entity.Logfile;
import org.netbeans.modules.exceptions.entity.SuspiciousMethodMapping;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.server.componentsmatch.Component;
import org.netbeans.server.componentsmatch.Matcher;
import org.netbeans.web.Persistable;
import org.netbeans.web.Persistable.TransactionResult;
import org.netbeans.web.Utils;

/**
 *
 * @author Jindrich Sedek
 */
public class SnapshotManager {

    private static final int PERCENTAGE_OF_SELF_NODE = 28;

    private static final int PERCENTAGE_OF_BIGGEST_NODE = 72;

    private static final int MAX_DUMP_DEPTH = 3000;

    private LoadedSnapshot loadedSnapshot;

    private CPUResultsSnapshot snapshot;

    private MethodItem suspiciousMethod;

    private byte[] originalSnapshot;

    private SnapshotManager() {
    }

    public static SnapshotManager loadSnapshot(Logfile lf) {
        try {
            File npsFile = lf.getNPSFile();
            if (!npsFile.exists()) {
                return null;
            }
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(npsFile));
            try {
                return SnapshotManager.loadSnapshot(bis);
            } finally {
                bis.close();
            }
        } catch (IOException ioe) {
            Logger.getLogger(SlownessChecker.class.getName()).log(Level.SEVERE, "slowness nps file not found", ioe);
        }
        return null;
    }

    public static SnapshotManager loadSnapshot(InputStream is) throws IOException {
        SnapshotManager sm = new SnapshotManager();
        DataInputStream dis = new DataInputStream(is);
        sm.load(dis);
        return sm;
    }

    public boolean AWTcontainsMethod(String methodName) {
        return containsMethod(getAWTRoot(), methodName);
    }

    private boolean containsMethod(PrestimeCPUCCTNode searchRoot, String methodName) {
        String nodeMethodName = getMethodName(searchRoot);
        CCTNode[] nodes = searchRoot.getChildren();
        if (nodeMethodName.startsWith(methodName)) {
            return true;
        }
        if ((nodes == null) || nodes.length == 0) {
            return false;
        }
        for (CCTNode cCTNode : nodes) {
            PrestimeCPUCCTNode pCCTNode = (PrestimeCPUCCTNode) cCTNode;
            if (pCCTNode.isSelfTimeNode()) {
                continue;
            } else if (containsMethod(pCCTNode, methodName)) {
                return true;
            }
        }
        return false;
    }

    public MethodItem getSuspiciousMethodItem(EntityManager em) {
        if (suspiciousMethod == null) {
            suspiciousMethod = getSuspiciousMethodItemImpl(em, false);
        }
        return suspiciousMethod;
    }

    public MethodItem getSuspiciousMethodItemForCC(EntityManager em) {
        if (suspiciousMethod == null) {
            suspiciousMethod = getSuspiciousMethodItemImpl(em, true);
        }
        return suspiciousMethod;
    }

    public MethodItem getSuspiciousMethodItemImpl(EntityManager em, boolean searchInCCThread) {
        MethodItem candidate;
        if (searchInCCThread) {
            candidate = getSuspiciousMethodItemFromRoot(em, getCCRoot());
        } else {
            candidate = getSuspiciousMethodItemFromRoot(em, getAWTRoot());
        }
        if (candidate == null) {
            return null;
        }
        SuspiciousMethodMapping mapping = SuspiciousMethodMapping.getMappingFor(em, candidate.getMethodName());
        if (mapping == null) {
            return candidate;
        }
        PrestimeCPUCCTNode maxOccurence = findLongestOccurenceOutsideAWT(mapping.getMappedMethodName());
        if (maxOccurence == null) {
            return candidate;
        }
        MethodItem newCandidate = getSuspiciousMethodItemFromRoot(em, maxOccurence);
        if (newCandidate == null) {
            return candidate;
        }
        return newCandidate;
    }

    private MethodItem getSuspiciousMethodItemFromRoot(EntityManager em, PrestimeCPUCCTNode searchRoot) {
        List<MethodItem> prefix = getSuspiciousStackPrefix(searchRoot);
        ListIterator<MethodItem> li = prefix.listIterator(prefix.size());
        Matcher mr = Matcher.getDefault();
        while (li.hasPrevious()) {
            MethodItem candidate = li.previous();
            Component comp = mr.matchMethod(em, candidate.getMethodName());
            if (comp != null) {
                return candidate;
            }
        }
        return null;
    }

    public Component getComponent(EntityManager em) {
        MethodItem mi = getSuspiciousMethodItem(em);
        if (mi == null) {
            return null;
        }
        Matcher mchr = Matcher.getDefault();
        return mchr.getRealComponent(em, mchr.matchMethod(em, mi.getMethodName()));
    }

    List<MethodItem> getSuspiciousStackPrefix() {
        return getSuspiciousStackPrefix(getAWTRoot());
    }

    private List<MethodItem> getSuspiciousStackPrefix(final PrestimeCPUCCTNode root) {
        final List<MethodItem> suspiciousStackPrefix = new ArrayList<MethodItem>();
        final Matcher matcher = Matcher.getDefault();
        Utils.processPersistable(new Persistable.Query() {

            public TransactionResult runQuery(EntityManager em) throws Exception {
                PrestimeCPUCCTNode node = getBiggestChild(root);
                Component componentCandidate = null;
                long selfTime;
                boolean canContinue = true;
                do {
                    selfTime = getSelfTime(node);
                    String methodName = getMethodName(node);
                    suspiciousStackPrefix.add(new MethodItem(methodName, node.getTotalTime0(), selfTime));
                    Component nodeComponent = matcher.matchMethod(em, node.getNodeName());
                    if (nodeComponent != null) {
                        componentCandidate = nodeComponent;
                    }
                    if (isMoreThan(selfTime, PERCENTAGE_OF_SELF_NODE, node.getTotalTime0())) {
                        if (nodeComponent != null) {
                            canContinue = false;
                        }
                    }
                    PrestimeCPUCCTNode child = getBiggestChild(node);
                    if (child == null) {
                        canContinue = false;
                    } else if (!isMoreThan(child.getTotalTime0(), PERCENTAGE_OF_BIGGEST_NODE, node.getTotalTime0())) {
                        if (componentCandidate != null) {
                            canContinue = false;
                        }
                    }
                    node = child;
                } while (canContinue);
                return TransactionResult.NONE;
            }
        });
        return suspiciousStackPrefix;
    }

    private String getMethodName(PrestimeCPUCCTNode node) {
        String methodName = node.getNodeName();
        if (methodName.endsWith("()")) {
            methodName = methodName.substring(0, methodName.length() - 2);
        }
        return methodName.replace("[native]", "");
    }

    private boolean isMoreThan(long count, int percentage, long all) {
        return count > all * percentage / 100;
    }

    private PrestimeCPUCCTNode getBiggestChild(PrestimeCPUCCTNode node) {
        CCTNode[] nodes = node.getChildren();
        if ((nodes == null) || nodes.length == 0) {
            return null;
        }
        PrestimeCPUCCTNode biggestChild = null;
        for (CCTNode cCTNode : nodes) {
            PrestimeCPUCCTNode pCCTNode = (PrestimeCPUCCTNode) cCTNode;
            if (pCCTNode.isSelfTimeNode()) {
                continue;
            } else if ((biggestChild == null) || (biggestChild.getTotalTime0() < pCCTNode.getTotalTime0())) {
                biggestChild = pCCTNode;
            }
        }
        return biggestChild;
    }

    private long getSelfTime(PrestimeCPUCCTNode node) {
        if (node.getChildren() == null) {
            return node.getTotalTime0();
        }
        assert node.getChildren().length > 1;
        PrestimeCPUCCTNode firstNode = (PrestimeCPUCCTNode) node.getChild(0);
        assert "Self time:".equals(firstNode.getNodeName());
        assert firstNode.isSelfTimeNode();
        return firstNode.getTotalTime0();
    }

    private PrestimeCPUCCTNode getRoot(String threadName) {
        for (int id : snapshot.getThreadIds()) {
            String name = snapshot.getThreadNameForId(id);
            if (!name.contains(threadName)) {
                continue;
            }
            CPUCCTContainer cpuct = snapshot.getContainerForThread(id, CPUResultsSnapshot.METHOD_LEVEL_VIEW);
            return cpuct.getRootNode();
        }
        return null;
    }

    private PrestimeCPUCCTNode getAWTRoot() {
        return getRoot("AWT-EventQueue");
    }

    private PrestimeCPUCCTNode getCCRoot() {
        return getRoot("Code Completion");
    }

    public void printAWTThread(PrintWriter printWriter) {
        printWriter.write("<b>Method name: total time(ms) / self time(ms)</b>\n");
        dumpCCTNode(printWriter, 0, getAWTRoot());
        printWriter.flush();
    }

    void printAllThreads(PrintWriter printWriter) {
        printWriter.write("<b>Method name: total time(ms) / self time(ms)</b>\n");
        for (int id : snapshot.getThreadIds()) {
            CPUCCTContainer cpuct = snapshot.getContainerForThread(id, CPUResultsSnapshot.METHOD_LEVEL_VIEW);
            dumpCCTNode(printWriter, 0, cpuct.getRootNode());
            printWriter.println();
        }
    }

    void dumpAWTThread(PrintWriter writer) {
        writer.println("time:" + (snapshot.getTimeTaken() - snapshot.getBeginTime()));
        writer.println("Threads:");
        dumpCCTNode(writer, 0, getAWTRoot());
    }

    private void dumpCCTNode(PrintWriter writer, int offset, PrestimeCPUCCTNode node) {
        dumpCCTNode(0, writer, offset, node);
    }

    private void dumpCCTNode(int depth, PrintWriter writer, int offset, PrestimeCPUCCTNode node) {
        if (depth > MAX_DUMP_DEPTH || node.isSelfTimeNode()) {
            return;
        }
        for (int i = 0; i < offset; i++) {
            writer.print("    ");
        }
        writer.println(node.getNodeName() + ": " + toMS(node.getTotalTime0()) + " / " + toMS(getSelfTime(node)));
        long parentTime = node.getTotalTime0();
        CCTNode[] children = node.getChildren();
        if (children != null) {
            for (CCTNode child : children) {
                PrestimeCPUCCTNode childNode = (PrestimeCPUCCTNode) child;
                if (parentTime != childNode.getTotalTime0()) {
                    dumpCCTNode(depth + 1, writer, offset + 1, childNode);
                } else {
                    dumpCCTNode(depth + 1, writer, offset, childNode);
                }
            }
        }
    }

    private int toMS(long l) {
        return new Long(l / 1000).intValue();
    }

    private PrestimeCPUCCTNode findLongestOccurenceOutsideAWT(String mappedMethodName) {
        PrestimeCPUCCTNode bestOccurence = null;
        for (int threadId : snapshot.getThreadIds()) {
            String name = snapshot.getThreadNameForId(threadId);
            if (name.contains("AWT-EventQueue")) {
                continue;
            }
            CPUCCTContainer cont = snapshot.getContainerForThread(threadId, CPUResultsSnapshot.METHOD_LEVEL_VIEW);
            PrestimeCPUCCTNode occurence = getMaxOccurence(cont.getRootNode(), mappedMethodName);
            if (bestOccurence == null) {
                bestOccurence = occurence;
            } else if (occurence != null) {
                bestOccurence = bestOccurence.getTotalTime0() > occurence.getTotalTime0() ? bestOccurence : occurence;
            }
        }
        return bestOccurence;
    }

    private PrestimeCPUCCTNode getMaxOccurence(PrestimeCPUCCTNode node, String methodName) {
        if (node.isSelfTimeNode()) {
            return null;
        }
        if (node.getNodeName().contains(methodName)) {
            return node;
        }
        PrestimeCPUCCTNode bestOccurence = null;
        CCTNode[] children = node.getChildren();
        if (children == null) {
            return null;
        }
        for (CCTNode child : children) {
            PrestimeCPUCCTNode occurence = getMaxOccurence((PrestimeCPUCCTNode) child, methodName);
            if (bestOccurence == null) {
                bestOccurence = occurence;
            } else if (occurence != null) {
                bestOccurence = bestOccurence.getTotalTime0() > occurence.getTotalTime0() ? bestOccurence : occurence;
            }
        }
        return bestOccurence;
    }

    private void load(DataInputStream dis) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int next = dis.read();
        while (next > -1) {
            buf.write(next);
            next = dis.read();
        }
        buf.flush();
        originalSnapshot = buf.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(originalSnapshot);
        DataInputStream dis2 = new DataInputStream(bis);
        loadedSnapshot = LoadedSnapshot.loadSnapshot(dis2);
        assert loadedSnapshot.getSnapshot() instanceof CPUResultsSnapshot;
        snapshot = (CPUResultsSnapshot) loadedSnapshot.getSnapshot();
    }

    public void save(DataOutputStream dos) throws IOException {
        if (originalSnapshot != null) {
            dos.write(originalSnapshot);
            dos.flush();
            if (dos != null) {
                dos.close();
            }
        } else {
            loadedSnapshot.save(dos);
        }
    }

    public boolean hasNpssContent() {
        boolean npss = false;
        if (originalSnapshot != null) {
            char c = (char) originalSnapshot[1];
            if (c == 'P') {
                npss = true;
            }
        }
        return npss;
    }
}
