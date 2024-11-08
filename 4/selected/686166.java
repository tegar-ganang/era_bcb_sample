package tests.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.RIPEMD128Digest;
import org.bouncycastle.crypto.generators.ElGamalKeyPairGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.ElGamalKeyGenerationParameters;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.pfyshnet.bc_codec.PfyshLevel;
import org.pfyshnet.bc_codec.PfyshNodePrivateKeys;
import org.pfyshnet.bc_codec.PfyshNodePublicKeys;
import org.pfyshnet.core.DataStore;
import org.pfyshnet.core.GroupKey;
import org.pfyshnet.core.GroupKeyInfo;
import org.pfyshnet.core.Level;
import org.pfyshnet.core.LocalDataStore;
import org.pfyshnet.core.LocalSearchData;
import org.pfyshnet.core.LocalSearchSpecification;
import org.pfyshnet.core.MyNodeInfo;
import org.pfyshnet.core.NodeHello;
import org.pfyshnet.core.NodeInfo;
import org.pfyshnet.core.SearchData;
import org.pfyshnet.core.SearchSpecification;
import org.pfyshnet.pfysh_database.DataBase;
import org.pfyshnet.utils.DiffFiles;

public class DataBaseTester2 {

    public static void main(String args[]) {
        try {
            DataBaseTester2 t = new DataBaseTester2();
            t.RunTests();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long KeyGenTime;

    public void RunTests() throws IOException {
        long starttime = (new Date()).getTime();
        ResetDir("dbtest");
        SecureRandom sr = new SecureRandom();
        DataBase db = new DataBase("dbtest", sr, false);
        TestMyNodeInfo(db, sr);
        db = new DataBase("dbtest", sr, false);
        TestNodeInfo(db, sr);
        db.close();
        ResetDir("dbtest");
        db = new DataBase("dbtest", sr, false);
        TestGroupKeyInfo(db, sr);
        TestSearchSpecs(db, sr);
        TestData(db, sr);
        TestSearchData(db, sr);
        TestValues(db, sr);
        TestDeleteOld(db, sr);
        db.close();
        long endtime = (new Date()).getTime();
        long runtime = endtime - starttime;
        double keyp = (double) KeyGenTime / (double) runtime;
        System.out.println("Runtime: " + runtime + " KeyGen time: " + KeyGenTime + " Fract: " + keyp);
    }

    public void ResetDir(String dir) {
        ResetDir(new File(dir));
    }

    public void ResetDir(File df) {
        if (df.exists() && df.isDirectory()) {
            File[] ls = df.listFiles();
            for (int cnt = 0; cnt < ls.length; cnt++) {
                if (ls[cnt].isDirectory()) {
                    ResetDir(ls[cnt]);
                } else {
                    if (!ls[cnt].delete()) {
                        try {
                            RandomAccessFile raf = new RandomAccessFile(ls[cnt], "rw");
                            raf.getChannel().force(true);
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (!ls[cnt].delete()) {
                            throw new RuntimeException("Failed to delete: " + ls[cnt].getPath());
                        }
                    }
                }
            }
            if (!df.delete()) {
                throw new RuntimeException("Could not delete directory: " + df.getPath());
            }
        }
    }

    public void TestDeleteOld(DataBase db, SecureRandom sr) throws IOException {
        List<NodeInfo> lst = db.queryForRandomNodes(1000, false);
        db.deleteOldNodes(1015L, 10);
        List<NodeInfo> lst2 = db.queryForRandomNodes(1000, false);
        if ((lst2.size() + 10) != lst.size()) {
            throw new RuntimeException();
        }
        db.deleteOldNodes(1015L, 1000);
        lst = db.queryForRandomNodes(1000, false);
        if (lst.size() == 0) {
            throw new RuntimeException();
        }
        Iterator<NodeInfo> i = lst.iterator();
        while (i.hasNext()) {
            NodeInfo ni = i.next();
            if (ni.getReceivedTime() < 1015 && ni.isActive()) {
                throw new RuntimeException();
            }
        }
        LinkedList<LocalDataStore> dlist = new LinkedList<LocalDataStore>();
        for (int cnt = 0; cnt < PfyshLevel.MULTS.length; cnt++) {
            List<LocalDataStore> dl = db.queryForGroupData(cnt, Long.MIN_VALUE);
            dlist.addAll(dl);
        }
        db.deleteOldData(0x90L, 1000);
        LinkedList<LocalDataStore> dlist2 = new LinkedList<LocalDataStore>();
        for (int cnt = 0; cnt < PfyshLevel.MULTS.length; cnt++) {
            List<LocalDataStore> dl = db.queryForGroupData(cnt, Long.MIN_VALUE);
            dlist2.addAll(dl);
        }
        if (dlist2.size() == 0) {
            throw new RuntimeException();
        }
        if (dlist2.size() >= dlist.size()) {
            throw new RuntimeException();
        }
        Iterator<LocalDataStore> id = dlist2.iterator();
        while (id.hasNext()) {
            LocalDataStore lds = id.next();
            if (lds.getTimeStored() < 0x90) {
                throw new RuntimeException();
            }
        }
        LinkedList<LocalSearchSpecification> slist = new LinkedList<LocalSearchSpecification>();
        for (long time = 10; time < 1000; time += 10) {
            long id0 = sr.nextLong();
            id0 = id0 & PfyshLevel.MASKS[PfyshLevel.MASKS.length - 1];
            PfyshLevel pl0 = new PfyshLevel(id0, 5);
            LocalSearchSpecification l0 = createLocalSpec(sr, id0, pl0, time);
            db.storeSearchSpecification(l0);
            slist.add(l0);
        }
        db.deleteOldSearchSpecs(712L, 1000);
        List<LocalSearchSpecification> slist2 = db.queryRandomSpecifications(1000);
        if (slist2.size() == 0) {
            throw new RuntimeException();
        }
        Iterator<LocalSearchSpecification> si = slist2.iterator();
        while (si.hasNext()) {
            LocalSearchSpecification lss = si.next();
            if (lss.getTime() < 712L) {
                throw new RuntimeException();
            }
        }
        System.out.println("TestDeleteOld:  PASS");
    }

    public void TestValues(DataBase db, SecureRandom sr) throws IOException {
        long time = sr.nextLong();
        db.setLastPeriodicTime(time);
        long time2 = db.getLastPeriodicTime();
        if (time != time2) {
            throw new RuntimeException();
        }
        time = sr.nextLong();
        db.setLastPeriodicTime(time);
        time2 = db.getLastPeriodicTime();
        if (time != time2) {
            throw new RuntimeException();
        }
        long did0 = db.getNextDownloadID();
        long uid0 = db.getNextUploadID();
        long sid0 = db.getNextSearchID();
        long gid0 = db.getNextGroupKeyID();
        long did1 = db.getNextDownloadID();
        long uid1 = db.getNextUploadID();
        long sid1 = db.getNextSearchID();
        long gid1 = db.getNextGroupKeyID();
        if (did0 == did1 || uid0 == uid1 || sid0 == sid1 || gid0 == gid1) {
            throw new RuntimeException();
        }
        long did2 = db.getNextDownloadID();
        long uid2 = db.getNextUploadID();
        long sid2 = db.getNextSearchID();
        long gid2 = db.getNextGroupKeyID();
        if (did0 == did2 || uid0 == uid2 || sid0 == sid2 || gid0 == gid2) {
            throw new RuntimeException();
        }
        System.out.println("TestValues:  PASS");
    }

    public void TestSearchData(DataBase db, SecureRandom sr) throws IOException {
        File dir = new File("dbtest");
        long fullid = sr.nextLong() & PfyshLevel.MASKS[PfyshLevel.MASKS.length - 1];
        long tag = sr.nextLong() & PfyshLevel.MASKS[PfyshLevel.MASKS.length - 1];
        LocalSearchData lsd = createLocalSearchData(sr, dir, 5, fullid, tag, 0x80L);
        db.storeSearchData(lsd);
        List<LocalSearchData> ld = db.queryForSearchData(5, 0x50L);
        if (ld.size() != 1) {
            System.out.println("Found: " + ld.size());
            throw new RuntimeException();
        }
        Iterator<LocalSearchData> i = ld.iterator();
        while (i.hasNext()) {
            LocalSearchData ql = i.next();
            if (!LocalSearchDataEqual(ql, lsd)) {
                throw new RuntimeException();
            }
        }
        ld = db.queryForSearchData(4, 0x50L);
        if (ld.size() != 0) {
            throw new RuntimeException();
        }
        ld = db.queryForSearchData(6, 0x50L);
        if (ld.size() != 0) {
            throw new RuntimeException();
        }
        ld = db.queryForSearchData(5, 0x90L);
        if (ld.size() != 0) {
            throw new RuntimeException();
        }
        tag = sr.nextLong() & PfyshLevel.MASKS[PfyshLevel.MASKS.length - 1];
        LocalSearchData lsd2 = createLocalSearchData(sr, dir, 4, fullid, tag, 0x80L);
        db.storeSearchData(lsd2);
        ld = db.queryForSearchData(4, 0x50L);
        if (ld.size() != 1) {
            System.out.println("Found: " + ld.size());
            throw new RuntimeException();
        }
        i = ld.iterator();
        while (i.hasNext()) {
            LocalSearchData ql = i.next();
            if (!LocalSearchDataEqual(ql, lsd2)) {
                throw new RuntimeException();
            }
        }
        ld = db.queryForSearchData(3, 0x50L);
        if (ld.size() != 0) {
            throw new RuntimeException();
        }
        ld = db.queryForSearchData(4, 0x90L);
        if (ld.size() != 0) {
            throw new RuntimeException();
        }
        tag = sr.nextLong() & PfyshLevel.MASKS[PfyshLevel.MASKS.length - 1];
        long fullid2 = fullid + PfyshLevel.MULTS[5];
        fullid2 = fullid2 & PfyshLevel.MASKS[PfyshLevel.MASKS.length - 1];
        LocalSearchData lsd3 = createLocalSearchData(sr, dir, 4, fullid2, tag, 0x80L);
        db.storeSearchData(lsd3);
        ld = db.searchQuery(fullid);
        if (ld.size() != 2) {
            System.out.println("Size: " + ld.size());
            throw new RuntimeException();
        }
        i = ld.iterator();
        while (i.hasNext()) {
            LocalSearchData qd = i.next();
            if (!(LocalSearchDataEqual(qd, lsd) || LocalSearchDataEqual(qd, lsd2))) {
                throw new RuntimeException();
            }
        }
        System.out.println("SearchData:  PASS");
    }

    public void TestData(DataBase db, SecureRandom sr) throws IOException {
        File dir = new File("dbtest");
        PfyshLevel pl = new PfyshLevel(sr.nextLong(), 4);
        long tag = sr.nextLong();
        LocalDataStore lds = createLocalDataStore(sr, dir, pl, tag, 0x80L);
        File gf = (File) lds.getDataStore().getData();
        db.storeData(lds);
        LocalDataStore tds = db.queryData(tag);
        File f2 = (File) lds.getDataStore().getData();
        if (gf.equals(f2)) {
            System.out.println("F: " + gf.getPath() + "  F2: " + f2.getPath());
            throw new RuntimeException();
        }
        if (!LocalDataStoreEqual(tds, lds)) {
            throw new RuntimeException();
        }
        long tag2 = sr.nextLong();
        LocalDataStore lds2 = createLocalDataStore(sr, dir, pl, tag2, 0x180L);
        db.storeData(lds2);
        long tag3 = sr.nextLong();
        PfyshLevel pl2 = new PfyshLevel(sr.nextLong(), 4);
        LocalDataStore lds3 = createLocalDataStore(sr, dir, pl2, tag3, 0x180L);
        db.storeData(lds3);
        List<LocalDataStore> qd = db.queryForGroupData(4, 0x50L);
        if (qd.size() != 3) {
            System.out.println("qd: " + qd.size());
            throw new RuntimeException();
        }
        Iterator<LocalDataStore> i = qd.iterator();
        while (i.hasNext()) {
            LocalDataStore qlds = i.next();
            if (!(LocalDataStoreEqual(qlds, lds) || LocalDataStoreEqual(qlds, lds2) || LocalDataStoreEqual(qlds, lds3))) {
                throw new RuntimeException();
            }
        }
        qd = db.queryForGroupData(4, 0x90L);
        if (qd.size() != 2) {
            throw new RuntimeException();
        }
        i = qd.iterator();
        while (i.hasNext()) {
            LocalDataStore qlds = i.next();
            if (!(LocalDataStoreEqual(qlds, lds2) || LocalDataStoreEqual(qlds, lds3))) {
                throw new RuntimeException();
            }
            if (LocalDataStoreEqual(qlds, lds)) {
                throw new RuntimeException();
            }
        }
        qd = db.queryForGroupData(3, 0x50L);
        if (qd.size() != 0) {
            throw new RuntimeException();
        }
        qd = db.queryForGroupData(5, 0x50L);
        if (qd.size() != 0) {
            throw new RuntimeException();
        }
        System.out.println("TestData:   PASS");
    }

    public void TestSearchSpecs(DataBase db, SecureRandom sr) {
        long id0 = sr.nextLong();
        id0 = id0 & PfyshLevel.MASKS[PfyshLevel.MASKS.length - 1];
        PfyshLevel pl0 = new PfyshLevel(id0, 3);
        LocalSearchSpecification l0 = createLocalSpec(sr, id0, pl0, 0x100L);
        if (!db.storeSearchSpecification(l0)) {
            throw new RuntimeException();
        }
        if (db.storeSearchSpecification(l0)) {
            throw new RuntimeException();
        }
        List<LocalSearchSpecification> ql = db.queryRandomSpecifications(1000);
        if (ql.size() != 1) {
            throw new RuntimeException();
        }
        if (!LocalSearchSpecEqual(ql.get(0), l0)) {
            throw new RuntimeException();
        }
        PfyshLevel ql0 = new PfyshLevel(id0, 4);
        ql = db.querySearchSpecifications(ql0, 0x80L);
        if (ql.size() != 0) {
            throw new RuntimeException();
        }
        ql0 = new PfyshLevel(id0, 2);
        ql = db.querySearchSpecifications(ql0, 0x80L);
        if (ql.size() != 0) {
            throw new RuntimeException();
        }
        ql0 = new PfyshLevel(id0, 3);
        ql = db.querySearchSpecifications(ql0, 0x80L);
        if (ql.size() != 1) {
            throw new RuntimeException();
        }
        if (!LocalSearchSpecEqual(ql.get(0), l0)) {
            throw new RuntimeException();
        }
        ql0 = new PfyshLevel(id0, 3);
        ql = db.querySearchSpecifications(ql0, 0x110L);
        if (ql.size() != 0) {
            throw new RuntimeException();
        }
        ql0 = new PfyshLevel(~id0, 3);
        ql = db.querySearchSpecifications(ql0, 0x80L);
        if (ql.size() != 0) {
            throw new RuntimeException();
        }
        System.out.println("SearchSpec:   PASS");
    }

    public void TestGroupKeyInfo(DataBase db, SecureRandom sr) {
        List<NodeInfo> nodes = storeNodes(db, sr);
        List<NodeInfo> nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        NodeInfo ni = nodes.get(0);
        List<GroupKeyInfo> ls = db.getNodesGroupKeys(ni.getFullID().getID(), ni.getLevels()[3]);
        if (ls.size() != 0) {
            throw new RuntimeException();
        }
        GroupKeyInfo info = createGroupKeyInfo(sr, ni, ni.getLevels()[2], 3, 0x100L);
        db.storeGroupKey(info);
        ls = db.getNodesGroupKeys(ni.getFullID().getID(), ni.getLevels()[3]);
        if (ls.size() != 0) {
            throw new RuntimeException();
        }
        ls = db.getNodesGroupKeys(ni.getFullID().getID(), ni.getLevels()[1]);
        if (ls.size() != 0) {
            throw new RuntimeException();
        }
        ls = db.getNodesGroupKeys(ni.getFullID().getID(), ni.getLevels()[2]);
        if (ls.size() != 1) {
            System.out.println("size: " + ls.size());
            throw new RuntimeException();
        }
        GroupKeyInfo tinfo = ls.get(0);
        if (!GroupKeyInfoEqual(tinfo, info)) {
            throw new RuntimeException();
        }
        nodes2 = db.queryForNodesWithNoKeys(ni.getLevels()[2], 1000);
        System.out.println("nodes2: " + nodes2.size());
        if (nodes2.size() == 0) {
            throw new RuntimeException();
        }
        Iterator<NodeInfo> i = nodes2.iterator();
        while (i.hasNext()) {
            NodeInfo ni2 = i.next();
            System.out.println("ni2: " + Long.toHexString((Long) ni2.getFullID().getID()) + " ni: " + Long.toHexString((Long) ni.getFullID().getID()));
            if (NodeInfoEqual(ni, ni2)) {
                throw new RuntimeException();
            }
        }
        NodeInfo ni2 = nodes.get(1);
        GroupKeyInfo info2 = createGroupKeyInfo(sr, ni2, ni2.getLevels()[2], 1, 0x100L);
        db.storeGroupKey(info2);
        NodeInfo ni3 = nodes.get(2);
        GroupKeyInfo info3 = createGroupKeyInfo(sr, ni3, ni3.getLevels()[2], 3, 0x10L);
        db.storeGroupKey(info3);
        NodeInfo ni4 = nodes.get(3);
        GroupKeyInfo info4 = createGroupKeyInfo(sr, ni4, ni4.getLevels()[2], 4, 0x90L);
        db.storeGroupKey(info4);
        List<GroupKeyInfo> ql = db.queryForGroupKeys(2, 0x80, 2);
        if (ql.size() != 2) {
            throw new RuntimeException();
        }
        Iterator<GroupKeyInfo> qi = ql.iterator();
        while (qi.hasNext()) {
            GroupKeyInfo qk = qi.next();
            if (!(GroupKeyInfoEqual(qk, info) || GroupKeyInfoEqual(qk, info4))) {
                throw new RuntimeException();
            }
        }
        System.out.println("GroupKeyInfo: PASS");
    }

    public void TestNodeInfo(DataBase db, SecureRandom sr) {
        LinkedList<NodeInfo> nodes = storeNodes(db, sr);
        NodeInfo ni = nodes.get(0);
        List<NodeInfo> nl = db.queryForNodesWithNoKeys(ni.getFullID(), 10);
        if (nl.size() != 1) {
            System.out.println("nl: " + nl.size() + " id: " + Long.toHexString((Long) ni.getFullID().getID()));
            throw new RuntimeException();
        }
        NodeInfo ni0 = nl.get(0);
        if (!NodeInfoEqual(ni, ni0)) {
            throw new RuntimeException();
        }
        int total = 0;
        nl = db.queryForNodesWithNoKeys(new PfyshLevel(0L, 0), 10);
        total += nl.size();
        nl = db.queryForNodesWithNoKeys(new PfyshLevel(PfyshLevel.MULTS[0], 0), 10);
        total += nl.size();
        nl = db.queryForNodesWithNoKeys(new PfyshLevel(PfyshLevel.MULTS[0] * 0x2, 0), 10);
        total += nl.size();
        nl = db.queryForNodesWithNoKeys(new PfyshLevel(PfyshLevel.MULTS[0] * 0x3, 0), 10);
        total += nl.size();
        if (total != nodes.size()) {
            throw new RuntimeException();
        }
        GroupKeyInfo info = createGroupKeyInfo(sr, ni, ni.getFullID(), 2, 100L);
        db.storeGroupKey(info);
        nl = db.queryForNodesWithNoKeys(ni.getFullID(), 10);
        if (nl.size() != 0) {
            throw new RuntimeException();
        }
        Level l = ni.getLevels()[ni.getLevels().length - 3];
        nl = db.queryForNodesWithNoKeys(l, 10);
        if (nl.size() != 1) {
            System.out.println("NL len: " + nl.size());
            throw new RuntimeException();
        }
        info = createGroupKeyInfo(sr, ni, l, 2, 100L);
        db.storeGroupKey(info);
        db.storeNode(ni);
        l = ni.getLevels()[ni.getLevels().length - 2];
        nl = db.queryForNodesWithNoKeys(l, 10);
        if (nl.size() != 1) {
            throw new RuntimeException();
        }
        l = ni.getLevels()[ni.getLevels().length - 3];
        nl = db.queryForNodesWithNoKeys(l, 10);
        if (nl.size() != 0) {
            System.out.println("Size: " + nl.size());
            throw new RuntimeException();
        }
        nl = db.queryForRandomNodes(5, false);
        if (nl.size() != 5) {
            throw new RuntimeException();
        }
        Iterator<NodeInfo> i = nl.iterator();
        int numact = 0;
        NodeInfo deactnode = null;
        while (i.hasNext()) {
            ni = i.next();
            if (ni.isActive()) {
                numact++;
            } else {
                deactnode = ni;
            }
            if (!CheckForNode(ni, nodes)) {
                throw new RuntimeException();
            }
        }
        if (numact != 4) {
            throw new RuntimeException();
        }
        nl = db.queryForRandomNodes(5, false);
        NodeInfo deactnode2 = null;
        i = nl.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.isActive()) {
                deactnode2 = ni;
            }
        }
        if (NodeInfoEqual(deactnode, deactnode2)) {
            System.out.println("NOTE:  This will sometimes happen.. Run again.. It just shouldn't happen every time.");
        }
        nl = db.queryForRandomNodes(5, true);
        if (nl.size() != 4) {
            throw new RuntimeException();
        }
        i = nl.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.isActive()) {
                throw new RuntimeException();
            }
        }
        LinkedList<NodeInfo> nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        nl = db.queryForRandomNodes(8, true);
        List<NodeInfo> nl2 = db.queryForRandomNodes(8, false);
        if (nl.size() != 8 || nl2.size() != 8) {
            throw new RuntimeException();
        }
        i = nl.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!CheckForNode(ni, nl2)) {
                throw new RuntimeException();
            }
            if (!ni.isActive()) {
                throw new RuntimeException();
            }
        }
        nodes2 = storeNodes(db, sr);
        nodes.addAll(nodes2);
        PfyshLevel ql2 = new PfyshLevel(0L, 0);
        PfyshLevel ql3 = new PfyshLevel(PfyshLevel.MULTS[0], 0);
        PfyshLevel ql4 = new PfyshLevel(PfyshLevel.MULTS[0] * 0x2, 0);
        PfyshLevel ql5 = new PfyshLevel(PfyshLevel.MULTS[0] * 0x3, 0);
        LinkedList<NodeInfo> nql2 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql3 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql4 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql5 = new LinkedList<NodeInfo>();
        i = nodes.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (ni.getLevels()[0].equals(ql2)) {
                nql2.add(ni);
            }
            if (ni.getLevels()[0].equals(ql3)) {
                nql3.add(ni);
            }
            if (ni.getLevels()[0].equals(ql4)) {
                nql4.add(ni);
            }
            if (ni.getLevels()[0].equals(ql5)) {
                nql5.add(ni);
            }
        }
        nodes2.clear();
        List<NodeInfo> qlst2 = db.queryForRandomNodes(ql2, 100, false);
        if (qlst2.size() != nql2.size()) {
            throw new RuntimeException();
        }
        i = qlst2.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[0].equals(ql2)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst2);
        List<NodeInfo> qlst3 = db.queryForRandomNodes(ql3, 100, false);
        if (qlst3.size() != nql3.size()) {
            throw new RuntimeException();
        }
        i = qlst3.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[0].equals(ql3)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst3);
        List<NodeInfo> qlst4 = db.queryForRandomNodes(ql4, 100, false);
        if (qlst4.size() != nql4.size()) {
            throw new RuntimeException();
        }
        i = qlst4.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[0].equals(ql4)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst4);
        List<NodeInfo> qlst5 = db.queryForRandomNodes(ql5, 100, false);
        if (qlst5.size() != nql5.size()) {
            throw new RuntimeException();
        }
        i = qlst5.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[0].equals(ql5)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst5);
        if (nodes.size() != nodes2.size()) {
            System.out.println("Exp: " + nodes.size() + ", Act: " + nodes2.size());
            throw new RuntimeException();
        }
        i = nodes.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!CheckForNode(ni, nodes2)) {
                throw new RuntimeException();
            }
        }
        nodes2.clear();
        ql2 = new PfyshLevel(0L, 1);
        ql3 = new PfyshLevel(PfyshLevel.MULTS[1], 1);
        ql4 = new PfyshLevel(PfyshLevel.MULTS[1] * 0x2, 1);
        ql5 = new PfyshLevel(PfyshLevel.MULTS[1] * 0x3, 1);
        LinkedList<NodeInfo> nql02 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql03 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql04 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql05 = new LinkedList<NodeInfo>();
        i = nodes.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (ni.getLevels()[1].equals(ql2)) {
                nql02.add(ni);
            }
            if (ni.getLevels()[1].equals(ql3)) {
                nql03.add(ni);
            }
            if (ni.getLevels()[1].equals(ql4)) {
                nql04.add(ni);
            }
            if (ni.getLevels()[1].equals(ql5)) {
                nql05.add(ni);
            }
        }
        qlst2 = db.queryForRandomNodes(ql2, 100, false);
        if (qlst2.size() != nql02.size()) {
            throw new RuntimeException();
        }
        i = qlst2.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql2)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst2);
        qlst3 = db.queryForRandomNodes(ql3, 100, false);
        if (qlst3.size() != nql03.size()) {
            throw new RuntimeException();
        }
        i = qlst3.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql3)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst3);
        qlst4 = db.queryForRandomNodes(ql4, 100, false);
        if (qlst4.size() != nql04.size()) {
            throw new RuntimeException();
        }
        i = qlst4.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql4)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst4);
        qlst5 = db.queryForRandomNodes(ql5, 100, false);
        if (qlst5.size() != nql05.size()) {
            throw new RuntimeException();
        }
        i = qlst5.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql5)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst5);
        if (nodes2.size() != nql2.size()) {
            throw new RuntimeException();
        }
        i = nodes2.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!CheckForNode(ni, nql2)) {
                throw new RuntimeException();
            }
        }
        nodes2.clear();
        ql2 = new PfyshLevel(PfyshLevel.MULTS[0] + 0L, 1);
        ql3 = new PfyshLevel(PfyshLevel.MULTS[0] + PfyshLevel.MULTS[1], 1);
        ql4 = new PfyshLevel(PfyshLevel.MULTS[0] + PfyshLevel.MULTS[1] * 0x2, 1);
        ql5 = new PfyshLevel(PfyshLevel.MULTS[0] + PfyshLevel.MULTS[1] * 0x3, 1);
        LinkedList<NodeInfo> nql12 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql13 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql14 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql15 = new LinkedList<NodeInfo>();
        i = nodes.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (ni.getLevels()[1].equals(ql2)) {
                nql12.add(ni);
            }
            if (ni.getLevels()[1].equals(ql3)) {
                nql13.add(ni);
            }
            if (ni.getLevels()[1].equals(ql4)) {
                nql14.add(ni);
            }
            if (ni.getLevels()[1].equals(ql5)) {
                nql15.add(ni);
            }
        }
        qlst2 = db.queryForRandomNodes(ql2, 100, false);
        if (qlst2.size() != nql12.size()) {
            throw new RuntimeException();
        }
        i = qlst2.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql2)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst2);
        qlst3 = db.queryForRandomNodes(ql3, 100, false);
        if (qlst3.size() != nql13.size()) {
            throw new RuntimeException();
        }
        i = qlst3.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql3)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst3);
        qlst4 = db.queryForRandomNodes(ql4, 100, false);
        if (qlst4.size() != nql14.size()) {
            throw new RuntimeException();
        }
        i = qlst4.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql4)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst4);
        qlst5 = db.queryForRandomNodes(ql5, 100, false);
        if (qlst5.size() != nql15.size()) {
            throw new RuntimeException();
        }
        i = qlst5.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql5)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst5);
        if (nodes2.size() != nql3.size()) {
            throw new RuntimeException();
        }
        i = nodes2.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!CheckForNode(ni, nql3)) {
                throw new RuntimeException();
            }
        }
        nodes2.clear();
        ql2 = new PfyshLevel((PfyshLevel.MULTS[0] * 0x2) + 0L, 1);
        ql3 = new PfyshLevel((PfyshLevel.MULTS[0] * 0x2) + PfyshLevel.MULTS[1], 1);
        ql4 = new PfyshLevel((PfyshLevel.MULTS[0] * 0x2) + PfyshLevel.MULTS[1] * 0x2, 1);
        ql5 = new PfyshLevel((PfyshLevel.MULTS[0] * 0x2) + PfyshLevel.MULTS[1] * 0x3, 1);
        LinkedList<NodeInfo> nql22 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql23 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql24 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql25 = new LinkedList<NodeInfo>();
        i = nodes.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (ni.getLevels()[1].equals(ql2)) {
                nql22.add(ni);
            }
            if (ni.getLevels()[1].equals(ql3)) {
                nql23.add(ni);
            }
            if (ni.getLevels()[1].equals(ql4)) {
                nql24.add(ni);
            }
            if (ni.getLevels()[1].equals(ql5)) {
                nql25.add(ni);
            }
        }
        qlst2 = db.queryForRandomNodes(ql2, 100, false);
        if (qlst2.size() != nql22.size()) {
            throw new RuntimeException();
        }
        i = qlst2.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql2)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst2);
        qlst3 = db.queryForRandomNodes(ql3, 100, false);
        if (qlst3.size() != nql23.size()) {
            throw new RuntimeException();
        }
        i = qlst3.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql3)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst3);
        qlst4 = db.queryForRandomNodes(ql4, 100, false);
        if (qlst4.size() != nql24.size()) {
            throw new RuntimeException();
        }
        i = qlst4.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql4)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst4);
        qlst5 = db.queryForRandomNodes(ql5, 100, false);
        if (qlst5.size() != nql25.size()) {
            throw new RuntimeException();
        }
        i = qlst5.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql5)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst5);
        if (nodes2.size() != nql4.size()) {
            throw new RuntimeException();
        }
        i = nodes2.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!CheckForNode(ni, nql4)) {
                throw new RuntimeException();
            }
        }
        nodes2.clear();
        ql2 = new PfyshLevel((PfyshLevel.MULTS[0] * 0x3) + 0L, 1);
        ql3 = new PfyshLevel((PfyshLevel.MULTS[0] * 0x3) + PfyshLevel.MULTS[1], 1);
        ql4 = new PfyshLevel((PfyshLevel.MULTS[0] * 0x3) + PfyshLevel.MULTS[1] * 0x2, 1);
        ql5 = new PfyshLevel((PfyshLevel.MULTS[0] * 0x3) + PfyshLevel.MULTS[1] * 0x3, 1);
        LinkedList<NodeInfo> nql32 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql33 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql34 = new LinkedList<NodeInfo>();
        LinkedList<NodeInfo> nql35 = new LinkedList<NodeInfo>();
        i = nodes.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (ni.getLevels()[1].equals(ql2)) {
                nql32.add(ni);
            }
            if (ni.getLevels()[1].equals(ql3)) {
                nql33.add(ni);
            }
            if (ni.getLevels()[1].equals(ql4)) {
                nql34.add(ni);
            }
            if (ni.getLevels()[1].equals(ql5)) {
                nql35.add(ni);
            }
        }
        qlst2 = db.queryForRandomNodes(ql2, 100, false);
        if (qlst2.size() != nql32.size()) {
            throw new RuntimeException();
        }
        i = qlst2.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql2)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst2);
        qlst3 = db.queryForRandomNodes(ql3, 100, false);
        if (qlst3.size() != nql33.size()) {
            throw new RuntimeException();
        }
        i = qlst3.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql3)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst3);
        qlst4 = db.queryForRandomNodes(ql4, 100, false);
        if (qlst4.size() != nql34.size()) {
            throw new RuntimeException();
        }
        i = qlst4.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql4)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst4);
        qlst5 = db.queryForRandomNodes(ql5, 100, false);
        if (qlst5.size() != nql35.size()) {
            throw new RuntimeException();
        }
        i = qlst5.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!ni.getLevels()[1].equals(ql5)) {
                throw new RuntimeException();
            }
        }
        nodes2.addAll(qlst5);
        if (nodes2.size() != nql5.size()) {
            throw new RuntimeException();
        }
        i = nodes2.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (!CheckForNode(ni, nql5)) {
                throw new RuntimeException();
            }
        }
        List<NodeInfo> allactive = db.queryForRandomNodes(100, true);
        db.deactivateOldNodes(1015L, 100);
        List<NodeInfo> allactive2 = db.queryForRandomNodes(100, true);
        if (allactive2.size() >= allactive.size()) {
            throw new RuntimeException();
        }
        i = allactive2.iterator();
        while (i.hasNext()) {
            ni = i.next();
            if (ni.getReceivedTime() <= 1025L) {
                throw new RuntimeException();
            }
        }
        System.out.println("NodeInfo:   PASS");
    }

    public void TestMyNodeInfo(DataBase db, SecureRandom sr) throws IOException {
        MyNodeInfo m = createMyNodeInfo(10, sr);
        db.StoreMyData(m);
        db.close();
        db = new DataBase("dbtest", sr, false);
        MyNodeInfo m2 = db.getMyData();
        if (!m2.getFullID().equals(m.getFullID())) {
            throw new RuntimeException();
        }
        if (m.getLevels().length != m2.getLevels().length) {
            throw new RuntimeException();
        }
        for (int cnt = 0; cnt < m.getLevels().length && cnt < m2.getLevels().length; cnt++) {
            if (!m.getLevels()[cnt].equals(m2.getLevels()[cnt])) {
                throw new RuntimeException();
            }
        }
        PfyshNodePrivateKeys priv = (PfyshNodePrivateKeys) m.getPrivateKey();
        PfyshNodePrivateKeys priv2 = (PfyshNodePrivateKeys) m.getPrivateKey();
        if (!ElGamalPrivEqual(priv.getDecryptionKey(), priv2.getDecryptionKey())) {
            throw new RuntimeException();
        }
        if (!RSAPrivEqual(priv.getSignatureKey(), priv2.getSignatureKey())) {
            throw new RuntimeException();
        }
        NodeHello h = m.getNode();
        NodeHello h2 = m2.getNode();
        if (!NodeHelloEqual(h, h2)) {
            throw new RuntimeException();
        }
        db.close();
        System.out.println("MyNodeInfo: PASS");
    }

    public LinkedList<NodeInfo> storeNodes(DataBase db, SecureRandom sr) {
        LinkedList<NodeInfo> nodes = new LinkedList<NodeInfo>();
        NodeInfo i = createNodeInfo(10L, sr);
        i.setActive(true);
        i.setReceivedTime(1000L);
        db.storeNode(i);
        nodes.add(i);
        i = createNodeInfo(11L, sr);
        i.setActive(true);
        i.setReceivedTime(1010L);
        db.storeNode(i);
        nodes.add(i);
        i = createNodeInfo(12L, sr);
        i.setActive(true);
        i.setReceivedTime(1020L);
        db.storeNode(i);
        nodes.add(i);
        i = createNodeInfo(13L, sr);
        i.setActive(true);
        i.setReceivedTime(1030L);
        db.storeNode(i);
        nodes.add(i);
        i = createNodeInfo(10L, sr);
        i.setActive(false);
        i.setReceivedTime(1001L);
        db.storeNode(i);
        nodes.add(i);
        i = createNodeInfo(11L, sr);
        i.setActive(false);
        i.setReceivedTime(1011L);
        db.storeNode(i);
        nodes.add(i);
        i = createNodeInfo(12L, sr);
        i.setActive(false);
        i.setReceivedTime(1021L);
        db.storeNode(i);
        nodes.add(i);
        i = createNodeInfo(13L, sr);
        i.setActive(false);
        i.setReceivedTime(1031L);
        db.storeNode(i);
        nodes.add(i);
        return nodes;
    }

    public static boolean CheckForNode(NodeInfo n, List<NodeInfo> lst) {
        Iterator<NodeInfo> i = lst.iterator();
        while (i.hasNext()) {
            if (NodeInfoEqual(n, i.next())) {
                return true;
            }
        }
        return false;
    }

    public static boolean LocalSearchDataEqual(LocalSearchData s, LocalSearchData s2) throws IOException {
        if (s.getStoreTime() != s2.getStoreTime()) {
            return false;
        }
        if (!SearchDataEqual(s.getSearchData(), s2.getSearchData())) {
            return false;
        }
        return true;
    }

    public static boolean SearchDataEqual(SearchData s, SearchData s2) throws IOException {
        long id = (Long) s.getFullID();
        long id2 = (Long) s2.getFullID();
        if (id != id2) {
            return false;
        }
        if (s.getDepth() != s2.getDepth()) {
            return false;
        }
        if (s.getTag() != s2.getTag()) {
            return false;
        }
        File f = (File) s.getData();
        File f2 = (File) s2.getData();
        if (!DiffFiles.diffFiles(f, f2)) {
            return false;
        }
        return true;
    }

    public static boolean LocalDataStoreEqual(LocalDataStore l, LocalDataStore l2) throws IOException {
        if (!DataStoreEqual(l.getDataStore(), l2.getDataStore())) {
            return false;
        }
        if (l.getTimeStored() != l2.getTimeStored()) {
            return false;
        }
        return true;
    }

    public static boolean DataStoreEqual(DataStore d, DataStore d2) throws IOException {
        if (!d.getLevel().equals(d2.getLevel())) {
            return false;
        }
        if (d.getTag() != d2.getTag()) {
            return false;
        }
        File f = (File) d.getData();
        File f2 = (File) d2.getData();
        if (!DiffFiles.diffFiles(f, f2)) {
            return false;
        }
        return true;
    }

    public static boolean LocalSearchSpecEqual(LocalSearchSpecification s, LocalSearchSpecification s2) {
        if (!s.getLevel().equals(s2.getLevel())) {
            return false;
        }
        long id = (Long) s.getFullID();
        long id2 = (Long) s2.getFullID();
        if (id != id2) {
            return false;
        }
        if (s.getTime() != s2.getTime()) {
            return false;
        }
        if (!SearchSpecEqual(s.getSpec(), s2.getSpec())) {
            return false;
        }
        return true;
    }

    public static boolean SearchSpecEqual(SearchSpecification s, SearchSpecification s2) {
        List<Object> l = s.getGroupKeys();
        List<Object> l2 = s2.getGroupKeys();
        if (l.size() != l2.size()) {
            return false;
        }
        Iterator<Object> i = l.iterator();
        Iterator<Object> i2 = l2.iterator();
        while (i.hasNext()) {
            ElGamalPublicKeyParameters e = (ElGamalPublicKeyParameters) i.next();
            ElGamalPublicKeyParameters e2 = (ElGamalPublicKeyParameters) i2.next();
            if (!ElGamalPubEqual(e, e2)) {
                return false;
            }
        }
        return true;
    }

    public static boolean GroupKeyInfoEqual(GroupKeyInfo k, GroupKeyInfo k2) {
        if (k.getDataBaseID() != k2.getDataBaseID()) {
            return false;
        }
        if (k.getEncounters() != k2.getEncounters()) {
            return false;
        }
        if (k.getReceivedTime() != k2.getReceivedTime()) {
            return false;
        }
        long id = (Long) k.getSourceNodeID();
        long id2 = (Long) k2.getSourceNodeID();
        if (id != id2) {
            return false;
        }
        if (!GroupKeyEqual(k.getGroupKey(), k2.getGroupKey())) {
            return false;
        }
        return true;
    }

    public static boolean GroupKeyEqual(GroupKey k, GroupKey k2) {
        if (!k.getLevel().equals(k2.getLevel())) {
            return false;
        }
        ElGamalPrivateKeyParameters priv = (ElGamalPrivateKeyParameters) k.getPrivateKey();
        ElGamalPrivateKeyParameters priv2 = (ElGamalPrivateKeyParameters) k2.getPrivateKey();
        if (!ElGamalPrivEqual(priv, priv2)) {
            return false;
        }
        ElGamalPublicKeyParameters pub = (ElGamalPublicKeyParameters) k.getPublicKey();
        ElGamalPublicKeyParameters pub2 = (ElGamalPublicKeyParameters) k2.getPublicKey();
        if (!ElGamalPubEqual(pub, pub2)) {
            return false;
        }
        if (!NodeHelloEqual(k.getSourceNode(), k2.getSourceNode())) {
            return false;
        }
        byte[] b = (byte[]) k.getSignature();
        byte[] b2 = (byte[]) k2.getSignature();
        if (!Arrays.equals(b, b2)) {
            return false;
        }
        return true;
    }

    public static boolean NodeInfoEqual(NodeInfo n, NodeInfo n2) {
        if (!n.getFullID().equals(n2.getFullID())) {
            return false;
        }
        if (n.getLastHello() != n2.getLastHello()) {
            return false;
        }
        if (n.getReceivedTime() != n2.getReceivedTime()) {
            return false;
        }
        if (n.getLevels().length != n2.getLevels().length) {
            return false;
        }
        for (int cnt = 0; cnt < n.getLevels().length; cnt++) {
            if (!n.getLevels()[cnt].equals(n2.getLevels()[cnt])) {
                return false;
            }
        }
        if (!NodeHelloEqual(n.getHello(), n2.getHello())) {
            return false;
        }
        return true;
    }

    public static boolean NodeHelloEqual(NodeHello h, NodeHello h2) {
        if (!h.getConnectionLocation().equals(h2.getConnectionLocation())) {
            System.out.println("connection not equal");
            return false;
        }
        if (h.getHelloNumber() != h2.getHelloNumber()) {
            System.out.println("hello number not equal");
            return false;
        }
        byte[] s = (byte[]) h.getSignature();
        byte[] s2 = (byte[]) h2.getSignature();
        if (!Arrays.equals(s, s2)) {
            System.out.println("signature not equal  s: " + s.length + " s2: " + s2.length);
            return false;
        }
        PfyshNodePublicKeys pub = (PfyshNodePublicKeys) h.getPublicKey();
        PfyshNodePublicKeys pub2 = (PfyshNodePublicKeys) h2.getPublicKey();
        if (!RSAPubEqual(pub.getVerificationKey(), pub2.getVerificationKey())) {
            System.out.println("verification not equal");
            return false;
        }
        if (!ElGamalPubEqual((ElGamalPublicKeyParameters) pub.getEncryptionKey(), (ElGamalPublicKeyParameters) pub2.getEncryptionKey())) {
            System.out.println("encryption not equal");
            return false;
        }
        return true;
    }

    public static boolean RSAPubEqual(RSAKeyParameters m, RSAKeyParameters m2) {
        if (!m.getExponent().equals(m2.getExponent())) {
            return false;
        }
        if (!m.getModulus().equals(m2.getModulus())) {
            return false;
        }
        return true;
    }

    public static boolean ElGamalPubEqual(ElGamalPublicKeyParameters m, ElGamalPublicKeyParameters m2) {
        if (!m.getY().equals(m2.getY())) {
            return false;
        }
        return ElGamalEqual(m.getParameters(), m2.getParameters());
    }

    public static boolean ElGamalEqual(ElGamalParameters p, ElGamalParameters p2) {
        if (!p.getG().equals(p2.getG())) {
            return false;
        }
        if (p.getL() != p2.getL()) {
            return false;
        }
        if (!p.getP().equals(p2.getP())) {
            return false;
        }
        return true;
    }

    public static boolean RSAPrivEqual(RSAPrivateCrtKeyParameters m, RSAPrivateCrtKeyParameters m2) {
        if (!m.getDP().equals(m2.getDP())) {
            return false;
        }
        if (!m.getDQ().equals(m2.getDQ())) {
            return false;
        }
        if (!m.getExponent().equals(m2.getExponent())) {
            return false;
        }
        if (!m.getModulus().equals(m2.getModulus())) {
            return false;
        }
        if (!m.getP().equals(m2.getP())) {
            return false;
        }
        if (!m.getPublicExponent().equals(m2.getPublicExponent())) {
            return false;
        }
        if (!m.getQ().equals(m2.getQ())) {
            return false;
        }
        if (!m.getQInv().equals(m2.getQInv())) {
            return false;
        }
        return true;
    }

    public static boolean ElGamalPrivEqual(ElGamalPrivateKeyParameters m, ElGamalPrivateKeyParameters m2) {
        if (!m.getX().equals(m2.getX())) {
            throw new RuntimeException();
        }
        ElGamalParameters p = m.getParameters();
        ElGamalParameters p2 = m2.getParameters();
        return ElGamalEqual(p, p2);
    }

    public static byte[] long2byte(long val) {
        byte[] bs = new byte[Long.SIZE / Byte.SIZE];
        ByteBuffer buf = ByteBuffer.wrap(bs);
        buf.putLong(val);
        return bs;
    }

    public static LocalSearchData createLocalSearchData(SecureRandom sr, File dir, int depth, long fullid, long tag, long time) throws IOException {
        LocalSearchData lsd = new LocalSearchData();
        lsd.setSearchData(createSearchData(sr, dir, depth, fullid, tag));
        lsd.setStoreTime(time);
        return lsd;
    }

    public static SearchData createSearchData(SecureRandom sr, File dir, int depth, long fullid, long tag) throws IOException {
        SearchData sd = new SearchData();
        File f = File.createTempFile("testdata", ".dat", dir);
        byte[] buf = new byte[1024];
        FileOutputStream fos = new FileOutputStream(f);
        long flen = Math.max(1024L, (long) ((double) 10000 * sr.nextDouble()));
        for (; flen > 0; flen--) {
            sr.nextBytes(buf);
            fos.write(buf);
        }
        fos.close();
        sd.setData(f);
        sd.setDepth(depth);
        sd.setFullID(fullid & PfyshLevel.MASKS[PfyshLevel.MASKS.length - 1]);
        sd.setTag(tag & PfyshLevel.MASKS[PfyshLevel.MASKS.length - 1]);
        return sd;
    }

    public static LocalDataStore createLocalDataStore(SecureRandom sr, File dir, Level lev, long tag, long time) throws IOException {
        LocalDataStore lds = new LocalDataStore();
        lds.setDataStore(createDataStore(sr, dir, lev, tag));
        lds.setTimeStored(time);
        return lds;
    }

    public static DataStore createDataStore(SecureRandom sr, File dir, Level lev, long tag) throws IOException {
        DataStore ds = new DataStore();
        File f = File.createTempFile("testdata", ".dat", dir);
        byte[] buf = new byte[1024];
        FileOutputStream fos = new FileOutputStream(f);
        long flen = Math.max(1024L, (long) ((double) 10000 * sr.nextDouble()));
        for (; flen > 0; flen--) {
            sr.nextBytes(buf);
            fos.write(buf);
        }
        fos.close();
        ds.setData(f);
        ds.setTag(tag & PfyshLevel.MASKS[PfyshLevel.MASKS.length - 1]);
        ds.setLevel(lev);
        return ds;
    }

    public static GroupKeyInfo createGroupKeyInfo(SecureRandom rand, NodeInfo node, Level l, int enc, long rec) {
        GroupKey gk = createGroupKey(rand, l, node.getHello());
        GroupKeyInfo gki = new GroupKeyInfo(gk, node.getFullID().getID(), rand.nextLong());
        gki.setEncounters(enc);
        gki.setReceivedTime(rec);
        return gki;
    }

    public static GroupKey createGroupKey(SecureRandom rand, Level l, NodeHello node) {
        GroupKey gk = new GroupKey();
        gk.setLevel(l);
        gk.setSourceNode(node);
        KeyPairs kp = createKeys(rand);
        gk.setPrivateKey(kp.Priv.getDecryptionKey());
        gk.setPublicKey(kp.Pub.getEncryptionKey());
        byte[] sig = new byte[128];
        rand.nextBytes(sig);
        gk.setSignature(sig);
        return gk;
    }

    public static LocalSearchSpecification createLocalSpec(SecureRandom rand, long id, PfyshLevel level, long time) {
        LocalSearchSpecification l = new LocalSearchSpecification();
        l.setFullID(id);
        l.setLevel(level);
        l.setTime(time);
        l.setSpec(createSearchSpec(rand));
        return l;
    }

    public static SearchSpecification createSearchSpec(SecureRandom rand) {
        SearchSpecification spec = new SearchSpecification();
        LinkedList<Object> sl = new LinkedList<Object>();
        int numkeys = (int) ((double) 10 * rand.nextDouble());
        numkeys = Math.max(1, numkeys);
        for (int cnt = 0; cnt < numkeys; cnt++) {
            KeyPairs p = createKeys(rand);
            sl.add(p.Pub.getEncryptionKey());
        }
        spec.setGroupKeys(sl);
        return spec;
    }

    public static KeyPairs createKeys(SecureRandom rand) {
        long starttime = (new Date()).getTime();
        byte[] b = new byte[512];
        byte[] b2 = new byte[300];
        rand.nextBytes(b);
        BigInteger P = new BigInteger(b);
        P = P.abs();
        rand.nextBytes(b2);
        BigInteger G = new BigInteger(b2);
        G = G.abs();
        ElGamalParameters parms = new ElGamalParameters(P, G);
        ElGamalKeyGenerationParameters genparms = new ElGamalKeyGenerationParameters(rand, parms);
        ElGamalKeyPairGenerator keygen = new ElGamalKeyPairGenerator();
        keygen.init(genparms);
        AsymmetricCipherKeyPair enckeys = keygen.generateKeyPair();
        RSAKeyGenerationParameters rsaparms = new RSAKeyGenerationParameters(BigInteger.valueOf(311L), rand, 512, 2);
        RSAKeyPairGenerator rsagen = new RSAKeyPairGenerator();
        rsagen.init(rsaparms);
        AsymmetricCipherKeyPair valkeys = rsagen.generateKeyPair();
        PfyshNodePrivateKeys privkey = new PfyshNodePrivateKeys();
        PfyshNodePublicKeys pubkey = new PfyshNodePublicKeys();
        privkey.setDecryptionKey((ElGamalPrivateKeyParameters) enckeys.getPrivate());
        privkey.setSignatureKey((RSAPrivateCrtKeyParameters) valkeys.getPrivate());
        pubkey.setEncryptionKey((ElGamalPublicKeyParameters) enckeys.getPublic());
        pubkey.setVerificationKey((RSAKeyParameters) valkeys.getPublic());
        KeyPairs k = new KeyPairs();
        k.Priv = privkey;
        k.Pub = pubkey;
        long endtime = (new Date()).getTime();
        KeyGenTime += (endtime - starttime);
        return k;
    }

    public static Level[] CalculateLevels(PfyshNodePublicKeys pub) {
        RIPEMD128Digest dig = new RIPEMD128Digest();
        byte[] pbytes = ((ElGamalPublicKeyParameters) pub.getEncryptionKey()).getParameters().getP().toByteArray();
        dig.update(pbytes, 0, pbytes.length);
        byte[] hash128 = new byte[dig.getDigestSize()];
        dig.doFinal(hash128, 0);
        ByteBuffer buf = ByteBuffer.wrap(hash128);
        Level[] levels = new Level[31];
        long fullid = buf.getLong();
        for (int cnt = 0; cnt <= 30; cnt++) {
            levels[cnt] = new PfyshLevel(fullid, cnt);
        }
        return levels;
    }

    public static NodeInfo createNodeInfo(long val, SecureRandom rand) {
        NodeHello h = new NodeHello();
        h.setConnectionLocation("right here man! " + val);
        h.setHelloNumber(val);
        h.setSignature(long2byte(val));
        KeyPairs kp = createKeys(rand);
        h.setPublicKey(kp.Pub);
        NodeInfo ni = new NodeInfo();
        ni.setActive(rand.nextBoolean());
        ni.setHello(h);
        ni.setLastHello(val);
        ni.setReceivedTime(val);
        ni.setLevels(CalculateLevels((PfyshNodePublicKeys) h.getPublicKey()));
        return ni;
    }

    public static MyNodeInfo createMyNodeInfo(long val, SecureRandom rand) {
        NodeHello h = new NodeHello();
        h.setConnectionLocation("right here man! " + val);
        h.setHelloNumber(val);
        h.setSignature(long2byte(val));
        KeyPairs kp = createKeys(rand);
        h.setPublicKey(kp.Pub);
        MyNodeInfo mn = new MyNodeInfo();
        mn.setLevels(CalculateLevels((PfyshNodePublicKeys) h.getPublicKey()));
        mn.setNode(h);
        mn.setPrivateKey(kp.Priv);
        return mn;
    }
}
