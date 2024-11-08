package gg.arkheion.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.logging.InternalLoggerFactory;
import ch.qos.logback.classic.Level;
import gg.arkehion.configuration.Configuration;
import gg.arkehion.exceptions.ArEndTransferException;
import gg.arkehion.exceptions.ArFileException;
import gg.arkehion.exceptions.ArFileWormException;
import gg.arkehion.exceptions.ArUnvalidIndexException;
import gg.arkehion.store.ArkDirConstants;
import gg.arkehion.store.ArkDualCasFileInterface;
import gg.arkehion.store.ArkLegacyInterface;
import gg.arkehion.store.ArkStoreInterface;
import gg.arkehion.store.abstimpl.ArkAbstractDeleteDualDocQueue;
import gg.arkehion.store.abstimpl.ArkAbstractDirFunction;
import gg.arkehion.store.abstimpl.ArkAbstractUpdateTimeQueue;
import gg.arkehion.store.abstimpl.ArkCrypto;
import gg.arkehion.store.fileimpl.ArkFsLegacy;
import gg.arkehion.store.hadoopimpl.ArkHadoopLegacy;
import gg.arkehion.store.hadoopimpl.ArkHadoopStore;
import gg.arkehion.utils.ArConstants;
import goldengate.common.crypto.KeyObject;
import goldengate.common.file.DataBlock;
import goldengate.common.logging.GgSlf4JLoggerFactory;

/**
 * @author Frederic Bregier
 * 
 */
public class TestStoreAndHttpThread implements Runnable {

    static final boolean DEBUG = false;

    static final int NB = 100;

    static long START = 0;

    public static boolean FSMODE = true;

    public static String baseroot = "J:/GG/ARK";

    public static String root = baseroot + "/Legacy1/";

    public static String rootH = "/Legacy1/";

    public static String out = baseroot + "/Out/";

    public static String root2 = baseroot + "/Legacy2/";

    public static String root2H = "/Legacy2/";

    public static String out2 = baseroot + "/Out/";

    static String filesrc = "J:/GG/ARK/todo.txt";

    static String dKey = "a8e48a9ef33033322ae3f7dc7610869a9c2d8738dadbfd4d19a6539366102e4f364f89ca946e7dfef38a1d6706c79cc54d366cbfc4574b60aecd4c859214b411";

    static String metadata = "<?xml version='1.0' encoding='ISO-8859-1'?>\n<!-- ======================================================== -->\n<!-- =====                                              ===== -->\n<!-- =====              ArchiveDelivery                 ===== -->\n<!-- =====                                              ===== -->\n<!-- ======================================================== -->\n<!--\n\n  Last change: 8. January 2010\n  Previews change: 16. February 2006\n\n  ArchiveDelivery Messages, standard d'echange 0.2\n  Prefix=ADYAES\n\n-->\n\n<xsd:schema version='1.1'\n\n            xmlns:xsd='http://www.w3.org/2001/XMLSchema'\n\n            xmlns:ccts='urn:un:unece:uncefact:documentation:standard:CoreComponentsTechnicalSpecification:2'\n            xmlns:udt='urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6'\n            xmlns:qdt='fr:gouv:ae:archive:draft:standard_echange_v0.2:QualifiedDataType:1'\n\n            xmlns='fr:gouv:ae:archive:draft:standard_echange_v0.2'\n\n            targetNamespace='fr:gouv:ae:archive:draft:standard_echange_v0.2'\n \n            elementFormDefault='qualified' attributeFormDefault='unqualified'>\n\n<!-- ======================================================== -->\n<!-- ====                Includes                        ==== -->\n<!-- ======================================================== -->\n</xsd:schema>";

    static ArkAbstractDirFunction arDir = null;

    public static int torun = 0;

    public int whoami = 0;

    public TestStoreAndHttpThread(int i) {
        whoami = i;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(new GgSlf4JLoggerFactory(Level.DEBUG));
        if (args.length > 0) {
            START = Long.parseLong(args[0]);
        }
        testMain();
    }

    public static void testMain() throws Exception {
        List<Date> dates = new LinkedList<Date>();
        List<String> names = new LinkedList<String>();
        ArkAbstractUpdateTimeQueue.startGlobal();
        ArkAbstractDeleteDualDocQueue.startGlobal();
        ArkAbstractUpdateTimeQueue.startGlobal();
        ArkAbstractDeleteDualDocQueue.startGlobal();
        ArkAbstractUpdateTimeQueue.startGlobal();
        ArkAbstractDeleteDualDocQueue.startGlobal();
        init();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        TestStoreAndHttpThread t1 = new TestStoreAndHttpThread(1);
        TestStoreAndHttpThread t2 = new TestStoreAndHttpThread(2);
        TestStoreAndHttpThread t3 = new TestStoreAndHttpThread(3);
        dates.add(new Date());
        names.add("start");
        torun = 1;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        pool = Executors.newFixedThreadPool(2);
        torun = 21;
        names.add("FullDelete");
        pool.execute(t1);
        pool.execute(t2);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        System.err.println("DelFromList " + ArkAbstractDeleteDualDocQueue.waitingFor());
        ArkAbstractDeleteDualDocQueue.waitFor();
        dates.add(new Date());
        pool = Executors.newFixedThreadPool(3);
        torun = 2;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        names.add("list");
        dates.add(new Date());
        names.add("create");
        pool = Executors.newFixedThreadPool(3);
        torun = 3;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("list");
        pool = Executors.newFixedThreadPool(3);
        torun = 4;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("move");
        pool = Executors.newFixedThreadPool(3);
        torun = 5;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("list");
        pool = Executors.newFixedThreadPool(3);
        torun = 6;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("copy");
        pool = Executors.newFixedThreadPool(3);
        torun = 7;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("list");
        pool = Executors.newFixedThreadPool(3);
        torun = 8;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("delete");
        pool = Executors.newFixedThreadPool(3);
        torun = 9;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("list");
        pool = Executors.newFixedThreadPool(3);
        torun = 10;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("store");
        pool = Executors.newFixedThreadPool(3);
        torun = 11;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("list");
        pool = Executors.newFixedThreadPool(3);
        torun = 12;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("delete");
        pool = Executors.newFixedThreadPool(3);
        torun = 13;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("storeFC");
        pool = Executors.newFixedThreadPool(3);
        torun = 14;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("retrieve");
        pool = Executors.newFixedThreadPool(3);
        torun = 15;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("retrieveDB");
        pool = Executors.newFixedThreadPool(3);
        torun = 16;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("retrieveMD");
        pool = Executors.newFixedThreadPool(3);
        torun = 17;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("info");
        pool = Executors.newFixedThreadPool(3);
        torun = 18;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("storeCrypted");
        pool = Executors.newFixedThreadPool(3);
        torun = 19;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("copyFromCrypted");
        pool = Executors.newFixedThreadPool(3);
        torun = 20;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        dates.add(new Date());
        names.add("waitForHttpRequest");
        torun = 0;
        System.err.println("Start HTTP");
        pool = Executors.newFixedThreadPool(1);
        TestStoreAndHttpThread t0 = new TestStoreAndHttpThread(0);
        pool.execute(t0);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        System.err.println("Stop HTTP");
        dates.add(new Date());
        names.add("delete");
        pool = Executors.newFixedThreadPool(3);
        torun = 13;
        pool.execute(t1);
        pool.execute(t2);
        pool.execute(t3);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        System.err.println("Del " + ArkAbstractDeleteDualDocQueue.waitingFor());
        ArkAbstractDeleteDualDocQueue.waitFor();
        dates.add(new Date());
        pool = Executors.newFixedThreadPool(2);
        torun = 21;
        names.add("FullDelete");
        pool.execute(t1);
        pool.execute(t2);
        pool.shutdown();
        pool.awaitTermination(100000, TimeUnit.SECONDS);
        System.err.println("DelFromList " + ArkAbstractDeleteDualDocQueue.waitingFor());
        ArkAbstractDeleteDualDocQueue.waitFor();
        dates.add(new Date());
        Iterator<Date> iterator = dates.iterator();
        Date date0 = iterator.next();
        Iterator<String> iterator2 = names.iterator();
        Date date1 = null;
        int i = 0;
        while (iterator.hasNext()) {
            date1 = iterator.next();
            String name = iterator2.next();
            double t = (date1.getTime() - date0.getTime());
            if (t == 0.0) {
                t = 1.0;
            }
            System.out.println(" T " + name + ": " + t + " : " + ((NB * 3000) / t));
            date0 = date1;
            i++;
        }
        ArkAbstractUpdateTimeQueue.stopGlobal();
        ArkAbstractDeleteDualDocQueue.stopGlobal();
        Configuration.stopConnection();
    }

    @Override
    public void run() {
        switch(torun) {
            case 0:
                runHttpClient();
                break;
            case 1:
                try {
                    start();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                listContents();
                break;
            case 3:
                try {
                    storeFrom();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                }
                break;
            case 4:
                listContents();
                break;
            case 5:
                try {
                    move();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                }
                break;
            case 6:
                listContents();
                break;
            case 7:
                try {
                    copy();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                }
                break;
            case 8:
                listContents();
                break;
            case 9:
                try {
                    delete();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                }
                break;
            case 10:
                listContents();
                break;
            case 11:
                try {
                    store();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case 12:
                listContents();
                break;
            case 13:
                try {
                    delete();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                }
                break;
            case 14:
                try {
                    storeFC();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case 15:
                try {
                    retrieve();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case 16:
                try {
                    retrieveDataBlock();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case 17:
                try {
                    retrieveMetadata();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case 18:
                try {
                    info();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case 19:
                try {
                    storeToCryptedLSD();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case 20:
                try {
                    copyFromCryptedLSD();
                } catch (ArUnvalidIndexException e) {
                    e.printStackTrace();
                } catch (ArFileException e) {
                    e.printStackTrace();
                } catch (ArFileWormException e) {
                    e.printStackTrace();
                }
                break;
            case 21:
                deleteAllContents();
                break;
        }
    }

    public static ArkLegacyInterface legacy1T1 = null;

    static ArkLegacyInterface legacy1T2 = null;

    static ArkLegacyInterface legacy1T3 = null;

    public static ArkStoreInterface store1a = null;

    public static ArkStoreInterface store1b = null;

    static ArkStoreInterface store1aT2 = null;

    static ArkStoreInterface store1bT2 = null;

    static ArkStoreInterface store1aT3 = null;

    static ArkStoreInterface store1bT3 = null;

    public static ArkLegacyInterface legacy2 = null;

    public static ArkStoreInterface store2a = null;

    static void init() throws Exception {
        KeyObject key = ArkCrypto.createNewKeyObject();
        key.generateKey();
        if (FSMODE) {
            arDir = ArkAbstractDirFunction.fsDirFunction;
            legacy1T1 = new ArkFsLegacy("Legacy1", 111, false, null, root, out, 10000000000L);
            legacy2 = new ArkFsLegacy("Legacy2", 112, true, key.getSecretKeyInBytes(), root2, out2, 10000000000L);
        } else {
            arDir = ArkAbstractDirFunction.hadoopDirFunction;
            legacy1T1 = new ArkHadoopLegacy("Legacy1", 111, false, null, rootH, out, 10000000000L, "");
            legacy2 = new ArkHadoopLegacy("Legacy2", 112, true, key.getSecretKeyInBytes(), root2H, out2, 10000000000L, "");
        }
        legacy1T3 = legacy1T1;
        legacy1T2 = legacy1T1;
        legacy1T1.create();
        store1a = legacy1T1.getStore(-9151313343288442623L);
        store1aT2 = legacy1T2.getStore(-9151313343288442623L);
        store1aT3 = legacy1T3.getStore(-9151313343288442623L);
        if (!FSMODE) System.out.println("Del: " + ((ArkHadoopLegacy) legacy1T1).getFileSystem().delete(((ArkHadoopStore) store1a).path, true));
        store1a.create();
        store1b = legacy1T1.getStore(-9151313343288442624L);
        store1bT2 = legacy1T2.getStore(-9151313343288442624L);
        store1bT3 = legacy1T3.getStore(-9151313343288442624L);
        if (!FSMODE) System.out.println("Del: " + ((ArkHadoopLegacy) legacy1T1).getFileSystem().delete(((ArkHadoopStore) store1b).path, true));
        store1b.create();
        legacy2.create();
        store2a = legacy2.getStore(-9151313343288442623L);
        if (!FSMODE) System.out.println("Del: " + ((ArkHadoopLegacy) legacy2).getFileSystem().delete(((ArkHadoopStore) store2a).path, true));
        store2a.create();
        Configuration.computeNbThreads();
        Configuration.startConnection();
    }

    void runHttpClient() {
        String[] args = { "http://localhost:8099/", filesrc };
        HttpClient.main(args);
    }

    void _start(long i, String[] sresult) throws ArUnvalidIndexException {
        if (DEBUG) System.out.println("getIndex():" + (i) + ":(0) " + sresult[0] + ":(1) " + sresult[1] + ":(2) " + sresult[2] + ":(3) " + sresult[3]);
        long[] gp = arDir.pathToIdUnique(sresult[ArkDirConstants.globalPathRank]);
        if (DEBUG) System.out.println(gp.length + ":" + gp[0]);
        gp = arDir.pathToIdUnique(sresult[ArkDirConstants.abstractPathRank]);
        if (DEBUG) System.out.println(gp.length + ":" + gp[0]);
    }

    void start() throws ArUnvalidIndexException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        String[] sresult = null;
        System.out.println("Index--------------------------------------------------");
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                sresult = arDir.idUniqueToStrings(rank + i);
                _start(rank + i, sresult);
            }
            System.out.println("Index--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                sresult = arDir.idUniqueToStrings(i - START);
                _start(i - START, sresult);
            }
            System.out.println("Index---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                sresult = arDir.idUniqueToStrings(rank - i);
                _start(rank - i, sresult);
            }
            System.out.println("Index----------------------------------------------------" + rank);
        }
        System.out.println(arDir.idUniqueTosubPath(-9151313343288442623L));
        System.out.println(arDir.idUniqueTosubPath(-9151313343288442501L));
        System.out.println("----------------------------------------------------");
    }

    void _storeFrom(ArkDualCasFileInterface doc) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        dKey = doc.storeFromLocalFile(filesrc, metadata);
        if (DEBUG) System.out.println("getIndex():" + doc.getIndex() + ":(0) " + doc.getGlobalPath() + ":(1) " + store1a.getObjectGlobalPath() + ":(2) " + doc.exists() + ":(3) " + dKey);
        doc.clear();
    }

    void storeFrom() throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("storeFrom--------------------------------------------------");
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store1a.getDoc(rank + i);
                _storeFrom(doc);
            }
            System.out.println("storeFrom--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store1aT2.getDoc(i - START);
                _storeFrom(doc);
            }
            System.out.println("storeFrom---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store1aT3.getDoc(rank - i);
                _storeFrom(doc);
            }
            System.out.println("storeFrom----------------------------------------------------");
        }
    }

    void listContents() {
        File file = new File("J:/GG/ARK/Legacy1.listing");
        if (whoami == 1) {
            System.out.println("List:" + arDir.getListOfFiles(legacy1T1, file, 1000, "/001", 0));
        } else if (whoami == 2) {
            file = new File("J:/GG/ARK/Legacy1.listingMark");
            System.out.println("ListMark:" + arDir.getListOfFilesMark(legacy1T2, file, 1000, "/001", 0));
        }
    }

    void _move(ArkDualCasFileInterface doc) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        ArkDualCasFileInterface doc2 = doc.move(store1b.getIndex(), doc.getIndex(), dKey);
        if (DEBUG) System.out.println("getIndex():" + doc2.getIndex() + ":(0) " + doc2.getGlobalPath() + ":(1) " + store1b.getObjectGlobalPath() + ":(2) " + doc2.exists());
        doc2.clear();
    }

    void move() throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("Move--------------------------------------------------");
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store1a.getDoc(rank + i);
                _move(doc);
            }
            System.out.println("Move--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store1aT2.getDoc(i - START);
                _move(doc);
            }
            System.out.println("Move---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store1aT3.getDoc(rank - i);
                _move(doc);
            }
            System.out.println("Move----------------------------------------------------");
        }
        ArkAbstractDeleteDualDocQueue.waitFor();
        System.out.println("DeleteWaitFor----------------------------------------------------");
    }

    void _delete(long i) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        ArkDualCasFileInterface doc;
        doc = store1a.getDoc(i);
        doc.delete(dKey);
        doc = store1b.getDoc(i);
        doc.delete(dKey);
        doc = store2a.getDoc(i);
        doc.delete(dKey);
    }

    void delete() throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("Delete--------------------------------------------------");
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                _delete(rank + i);
            }
            System.out.println("Delete--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                _delete(i - START);
            }
            System.out.println("Delete---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                _delete(rank - i);
            }
            System.out.println("Delete----------------------------------------------------");
        }
        ArkAbstractDeleteDualDocQueue.waitFor();
        System.out.println("DeleteWaitFor----------------------------------------------------");
    }

    void _copy(ArkDualCasFileInterface doc) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        ArkDualCasFileInterface doc2 = doc.copy(store1a.getIndex(), doc.getIndex());
        if (DEBUG) System.out.println("getIndex():" + doc2.getIndex() + ":(0) " + doc2.getGlobalPath() + ":(1) " + store1b.getObjectGlobalPath() + ":(2) " + doc2.exists());
        doc.clear();
        doc2.clear();
    }

    void copy() throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("Copy--------------------------------------------------");
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store1b.getDoc(rank + i);
                _copy(doc);
            }
            System.out.println("Copy--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store1bT2.getDoc(i - START);
                _copy(doc);
            }
            System.out.println("Copy---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store1bT3.getDoc(rank - i);
                _copy(doc);
            }
            System.out.println("Copy----------------------------------------------------");
        }
    }

    void _store(ArkDualCasFileInterface doc, File file) throws ArUnvalidIndexException, ArFileException, ArFileWormException, FileNotFoundException {
        doc.store(ArConstants.BUFFERSIZEDEFAULT, metadata);
        FileInputStream inputStream = new FileInputStream(file);
        dKey = doc.write(inputStream);
        if (DEBUG) System.out.println("getIndex():" + doc.getIndex() + ":(0) " + doc.getGlobalPath() + ":(1) " + store1a.getObjectGlobalPath() + ":(2) " + doc.exists() + ":(3) " + dKey);
        doc.clear();
    }

    void store() throws ArUnvalidIndexException, ArFileException, ArFileWormException, FileNotFoundException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("store--------------------------------------------------");
        File file = new File(filesrc);
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store1a.getDoc(rank + i);
                _store(doc, file);
            }
            System.out.println("store--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store1aT2.getDoc(i - START);
                _store(doc, file);
            }
            System.out.println("store---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store1aT3.getDoc(rank - i);
                _store(doc, file);
            }
            System.out.println("store----------------------------------------------------");
        }
    }

    void _storeFC(ArkDualCasFileInterface doc, File file) throws ArUnvalidIndexException, ArFileException, ArFileWormException, FileNotFoundException {
        doc.store(ArConstants.BUFFERSIZEDEFAULT, metadata);
        FileInputStream inputStream = new FileInputStream(file);
        FileChannel fileChannelIn = inputStream.getChannel();
        dKey = doc.write(fileChannelIn);
        if (DEBUG) System.out.println("getIndex():" + doc.getIndex() + ":(0) " + doc.getGlobalPath() + ":(1) " + store1a.getObjectGlobalPath() + ":(2) " + doc.exists() + ":(3) " + dKey);
        doc.clear();
    }

    void storeFC() throws ArUnvalidIndexException, ArFileException, ArFileWormException, FileNotFoundException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("storeFC--------------------------------------------------");
        File file = new File(filesrc);
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store1a.getDoc(rank + i);
                _storeFC(doc, file);
            }
            System.out.println("storeFC--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store1aT2.getDoc(i - START);
                _storeFC(doc, file);
            }
            System.out.println("storeFC---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store1aT3.getDoc(rank - i);
                _storeFC(doc, file);
            }
            System.out.println("storeFC----------------------------------------------------");
        }
    }

    /**
     * Writes to nowhere
     * 
     **/
    public static class NullOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
        }

        @Override
        public void write(byte[] b) throws IOException {
        }
    }

    void _retrieve(ArkDualCasFileInterface doc, NullOutputStream nullOutputStream) throws ArUnvalidIndexException, ArFileException {
        doc.retrieve(ArConstants.BUFFERSIZEDEFAULT);
        doc.get(nullOutputStream);
        if (DEBUG) System.out.println("getIndex():" + doc.getIndex() + ":(0) " + doc.getGlobalPath() + ":(1) " + store1a.getObjectGlobalPath() + ":(2) " + doc.exists() + ":(3) " + dKey);
        doc.clear();
    }

    void retrieve() throws ArUnvalidIndexException, ArFileException, ArFileWormException, FileNotFoundException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("retrieve--------------------------------------------------");
        NullOutputStream nullOutputStream = new NullOutputStream();
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store1a.getDoc(rank + i);
                _retrieve(doc, nullOutputStream);
            }
            System.out.println("retrieve--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store1aT2.getDoc(i - START);
                _retrieve(doc, nullOutputStream);
            }
            System.out.println("retrieve---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store1aT3.getDoc(rank - i);
                _retrieve(doc, nullOutputStream);
            }
            System.out.println("retrieve----------------------------------------------------");
        }
    }

    void _retrieveDataBlock(ArkDualCasFileInterface doc) throws ArUnvalidIndexException, ArFileException {
        doc.retrieve(ArConstants.BUFFERSIZEDEFAULT);
        DataBlock block = null;
        while (true) {
            try {
                block = doc.readDataBlock();
            } catch (ArEndTransferException e) {
                break;
            }
            if (block.isEOF()) {
                break;
            }
            block.clear();
        }
        if (block != null) block.clear();
        if (DEBUG) System.out.println("getIndex():" + doc.getIndex() + ":(0) " + doc.getGlobalPath() + ":(1) " + store1a.getObjectGlobalPath() + ":(2) " + doc.exists() + ":(3) " + dKey);
        doc.clear();
    }

    void retrieveDataBlock() throws ArUnvalidIndexException, ArFileException, ArFileWormException, FileNotFoundException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("retrieveDB--------------------------------------------------");
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store1a.getDoc(rank + i);
                _retrieveDataBlock(doc);
            }
            System.out.println("retrieveDB--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store1aT2.getDoc(i - START);
                _retrieveDataBlock(doc);
            }
            System.out.println("retrieveDB---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store1aT3.getDoc(rank - i);
                _retrieveDataBlock(doc);
            }
            System.out.println("retrieveDB----------------------------------------------------");
        }
    }

    void _retrieveMetadata(ArkDualCasFileInterface doc) throws ArUnvalidIndexException, ArFileException {
        doc.readMetadata();
        if (DEBUG) System.out.println("getIndex():" + doc.getIndex() + ":(0) " + doc.getGlobalPath() + ":(1) " + store1a.getObjectGlobalPath() + ":(2) " + doc.exists() + ":(3) " + dKey);
        doc.clear();
    }

    void retrieveMetadata() throws ArUnvalidIndexException, ArFileException, ArFileWormException, FileNotFoundException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("retrieveMD--------------------------------------------------");
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store1a.getDoc(rank + i);
                _retrieveMetadata(doc);
            }
            System.out.println("retrieveMD--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store1aT2.getDoc(i - START);
                _retrieveMetadata(doc);
            }
            System.out.println("retrieveMD---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store1aT3.getDoc(rank - i);
                _retrieveMetadata(doc);
            }
            System.out.println("retrieveMD----------------------------------------------------");
        }
    }

    void _info(ArkDualCasFileInterface doc) {
        boolean canRead = false;
        boolean exists = false;
        long time = 0;
        boolean error = false;
        try {
            canRead = doc.canRead();
        } catch (ArUnvalidIndexException e) {
            error = true;
        }
        try {
            exists = doc.exists();
        } catch (ArUnvalidIndexException e) {
            error = true;
        }
        try {
            time = doc.getTime();
        } catch (ArUnvalidIndexException e) {
            error = true;
        } catch (ArFileException e) {
            error = true;
        }
        if (DEBUG) System.out.println("getIndex():" + doc.getIndex() + ":(0) " + doc.getGlobalPath() + ":(1) " + store1a.getObjectGlobalPath() + ":(2) " + canRead + ":" + exists + ":" + time + "(" + error + ")");
        doc.clear();
    }

    void info() throws ArUnvalidIndexException, ArFileException, ArFileWormException, FileNotFoundException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("info--------------------------------------------------");
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store1a.getDoc(rank + i);
                _info(doc);
            }
            System.out.println("info--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store1aT2.getDoc(i - START);
                _info(doc);
            }
            System.out.println("info---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store1aT3.getDoc(rank - i);
                _info(doc);
            }
            System.out.println("info----------------------------------------------------");
        }
    }

    void storeToCryptedLSD() throws ArUnvalidIndexException, ArFileException, ArFileWormException, FileNotFoundException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        File file = new File(filesrc);
        System.out.println("storeCrypted--------------------------------------------------");
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store2a.getDoc(rank + i);
                _store(doc, file);
            }
            System.out.println("storeCrypted--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store2a.getDoc(i - START);
                _store(doc, file);
            }
            System.out.println("storeCrypted---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store2a.getDoc(rank - i);
                _store(doc, file);
            }
            System.out.println("storeCrypted----------------------------------------------------");
        }
    }

    void _copyFromCryptedLSD(ArkDualCasFileInterface doc) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        ArkDualCasFileInterface doc2 = store1b.getDoc(doc.getIndex());
        doc.copy(doc2);
        if (DEBUG) System.out.println("getIndex():" + doc2.getIndex() + ":(0) " + doc2.getGlobalPath() + ":(1) " + store1b.getObjectGlobalPath() + ":(2) " + doc2.exists());
        doc.clear();
        doc2.clear();
    }

    void copyFromCryptedLSD() throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        long rank = ArkDirConstants.minimal_idx + START;
        long i;
        System.out.println("CopyLSD--------------------------------------------------");
        if (whoami == 1) {
            for (i = 0; i < NB; i++) {
                ArkDualCasFileInterface doc = store2a.getDoc(rank + i);
                _copyFromCryptedLSD(doc);
            }
            System.out.println("CopyLSD--------------------------------------------------");
        } else if (whoami == 2) {
            for (i = -NB / 2; i < NB / 2; i++) {
                ArkDualCasFileInterface doc = store2a.getDoc(i - START);
                _copyFromCryptedLSD(doc);
            }
            System.out.println("CopyLSD---------------------------------------------------");
        } else {
            rank = ArkDirConstants.maximal_idx - START;
            for (i = NB; i >= 0; i--) {
                ArkDualCasFileInterface doc = store2a.getDoc(rank - i);
                _copyFromCryptedLSD(doc);
            }
            System.out.println("CopyLSD----------------------------------------------------");
        }
    }

    void deleteAllContents() {
        if (whoami == 1) {
            File fileIn = new File("J:/GG/ARK/Legacy1.listing");
            File fileOut = new File("J:/GG/ARK/LegacyOut1.listing");
            long nbIn = arDir.getListOfFilesMark(legacy1T1, fileIn, 1000, "/001", 0);
            System.out.println("CheckList: " + nbIn + ":" + arDir.checkFilesMark(legacy1T1, fileIn, nbIn, fileOut, 1000, "/001", 0));
            System.out.println("DeleteList: " + nbIn + ":" + arDir.deleteFilesMark(legacy1T1, fileIn, nbIn, fileOut, 1000, "/001", 0));
        } else if (whoami == 2) {
            File fileIn = new File("J:/GG/ARK/Legacy2.listing");
            File fileOut = new File("J:/GG/ARK/LegacyOut2.listing");
            long nbIn = arDir.getListOfFilesMark(legacy2, fileIn, 1000, "/001", 0);
            System.out.println("CheckList: " + nbIn + ":" + arDir.checkFilesMark(legacy2, fileIn, nbIn, fileOut, 1000, "/001", 0));
            System.out.println("DeleteList: " + nbIn + ":" + arDir.deleteFilesMark(legacy2, fileIn, nbIn, fileOut, 1000, "/001", 0));
        }
    }
}
