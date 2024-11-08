package de.schwarzrot.recmgr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import de.schwarzrot.app.errors.ApplicationException;
import de.schwarzrot.app.errors.InvalidSplitOperationException;
import de.schwarzrot.app.support.ApplicationServiceProvider;
import de.schwarzrot.concurrent.ProgressPublisher;
import de.schwarzrot.data.access.support.EqualConditionElement;
import de.schwarzrot.data.transaction.TORead;
import de.schwarzrot.data.transaction.TORemove;
import de.schwarzrot.data.transaction.TOSave;
import de.schwarzrot.data.transaction.TOSetProperty;
import de.schwarzrot.data.transaction.Transaction;
import de.schwarzrot.data.transaction.TransactionStatus;
import de.schwarzrot.data.transaction.support.TransactionFactory;
import de.schwarzrot.rec.domain.BonusItem;
import de.schwarzrot.rec.domain.CutMark;
import de.schwarzrot.rec.domain.RecInfo;
import de.schwarzrot.rec.domain.Recording;
import de.schwarzrot.rec.domain.Stream;
import de.schwarzrot.rec.domain.VideoStream;
import de.schwarzrot.recmgr.domain.AudioSelection;
import de.schwarzrot.recmgr.domain.RMConfig;
import de.schwarzrot.recmgr.support.AbstractRecordingHandler;
import de.schwarzrot.system.support.FileUtils;

/**
 * handler to manipulate vdr-recordings and their database counterparts. Used to
 * rename recordings at harddisk or join/split recordings.
 * <p>
 * Will be used by frontend {@code de.schwarzrot.recmgr.app.RecordingManager}
 * and backend {@code de.schwarzrot.recmgr.service.RecordingManager}
 * 
 * @author <a href="mailto:rmantey@users.sourceforge.net">Reinhard Mantey</a>
 */
public class LocalRecordingHandler extends AbstractRecordingHandler {

    /**
     * caluculate an estimated cutsize based on cutmarks
     * 
     * @param r
     *            - the Recording that should get calculated
     */
    @Override
    public void determineCutSize(Recording r) {
        r.setCutSize(r.getSize());
        if (r.getCutSize() > 0 && r.getCutMarks() != null && r.getCutMarks().size() > 0 && r.getStreams() != null && r.getStreams().size() > 0) {
            double fps = 0;
            long cutFrames = 0;
            for (Stream s : r.getStreams()) {
                if (s instanceof VideoStream) {
                    fps = ((VideoStream) s).getFps();
                    break;
                }
            }
            if (fps == 0) return;
            if (r.getFrameSize() == 0) r.setFrameSize(determineFrameSize(r));
            for (int i = 0; i < r.getCutMarks().size(); i++) {
                getLogger().info("cut at frames: " + r.getCutMarks().get(i).getFrameNum());
                if (i % 2 == 0) cutFrames += (-1) * r.getCutMarks().get(i).getFrameNum(); else cutFrames += r.getCutMarks().get(i).getFrameNum();
            }
            if (cutFrames > 0) r.setCutSize(cutFrames * r.getFrameSize());
            getLogger().info("new cutsize is: " + r.getCutSize());
        }
    }

    /**
     * creates an array with the sizes of each file, part of the recording. The
     * first entry is the size of the index file, so the second entry holds the
     * size of the first recording file. That way get(1) returns the size of
     * file 001.vdr
     * 
     * @param r
     *            - the Recording to get the file sizes from
     * @return an array of {@code Long} file sizes
     */
    @Override
    public List<Long> determineFileSizes(Recording r) {
        List<Long> rv = new ArrayList<Long>();
        long total = 0;
        File cur = null;
        cur = new File(r.getPath(), r.isPesRecording() ? "index.vdr" : "index");
        rv.add(cur.length());
        for (int i = 1; i < 999; i++) {
            cur = r.getVideoFile(i);
            if (!cur.exists()) break;
            rv.add(cur.length());
            total += cur.length();
        }
        if (total != 0) r.setSize(total);
        return rv;
    }

    /**
     * calculates an estimated size of a frame
     * 
     * @param r
     *            - the Recording to calculate
     * @return - the estimated size of a frame
     */
    public int determineFrameSize(Recording r) {
        int rv = 0;
        List<Long> sizes = determineFileSizes(r);
        if (sizes != null && sizes.size() > 0) {
            long total = r.getSize();
            long frames = sizes.get(0) / 8;
            rv = (int) (total / frames);
        }
        return rv;
    }

    @Override
    public TransactionStatus doDelete(BonusItem item) {
        if (item == null) return TransactionStatus.STATUS_NO_TRANSACTION;
        getLogger().info("execute >doDelete< for item: " + item);
        if (item.getType().compareTo(BonusItem.VARIANT_BONUS) == 0) {
            File image = item.getImagePath();
            File bonus = item.getPath();
            if (bonus.exists()) bonus.delete();
            if (image != null && image.exists()) image.delete();
        } else {
            File dir2Delete = item.getPath();
            if (dir2Delete.exists()) {
                FileUtils.removeDirectory(dir2Delete);
                dir2Delete = dir2Delete.getParentFile();
                dir2Delete.delete();
            }
        }
        Transaction ta = taFactory.createTransaction();
        item.setDeleted(true);
        ta.add(new TOSetProperty<BonusItem>(item, new String[] { BonusItem.FLD_DELETED }, new EqualConditionElement("id", item)));
        ta.execute();
        return ta.getStatus();
    }

    @Override
    public void doJoin(Recording item, List<BonusItem> others) {
        getLogger().info("execute command >doJoin< with itme: " + item);
    }

    @Override
    public void doMarkDeleted(BonusItem item) {
        if (item == null) return;
        getLogger().info("execute >doDelMark< for item: " + item);
        if (item instanceof Recording) {
            File recDir = item.getPath();
            if (recDir.exists() && recDir.isDirectory()) {
                item.setDeleted(!item.isDeleted());
                if (item.isDeleted()) {
                    if (recDir.getName().endsWith(".rec")) {
                        File deleted = new File(recDir.getParent(), recDir.getName().replace(".rec", ".del"));
                        recDir.renameTo(deleted);
                    }
                } else {
                    if (recDir.getName().endsWith(".del")) {
                        File undeleted = new File(recDir.getParent(), recDir.getName().replace(".del", ".rec"));
                        recDir.renameTo(undeleted);
                    }
                }
            } else getLogger().warn("Huh?!? - invalid recording, or may be drive not mounted?");
        } else {
            getLogger().warn("item is not a recording - so no way to mark it for vdr!");
        }
    }

    @Override
    public void doMarkDeleted(List<BonusItem> selected) {
        getLogger().info("execute >doDelMark< for item-list");
        if (selected != null && selected.size() > 0) {
            for (BonusItem item : selected) doMarkDeleted(item);
        }
    }

    @Override
    public TransactionStatus doMoveOrRename(BonusItem src, File newDest) {
        File source = src.getPath();
        File dest;
        if (source.listFiles() == null || source.listFiles().length < 2) {
            throw new ApplicationException(source + " is not a recording!");
        }
        if (newDest.mkdirs()) {
            for (File cur : source.listFiles()) {
                getLogger().info("found recording subfile: " + cur.getAbsolutePath());
                dest = new File(newDest, cur.getName());
                if (FileUtils.moveOrRename(dest, cur)) {
                    getLogger().info("Yeah, renaming was successful!");
                } else {
                    getLogger().warn("Oups, renaming was not successful!");
                }
            }
            while (source.delete()) source = source.getParentFile();
            Transaction ta = taFactory.createTransaction();
            src.setPath(newDest);
            ta.add(new TOSave<BonusItem>(src));
            ta.execute();
            return ta.getStatus();
        }
        throw new ApplicationException("could not create [" + newDest + "] - please check directory permissions.");
    }

    @Override
    public void doNewCutMarks(Recording rec, File newCutmarkFile) {
        if (rec != null && newCutmarkFile != null && newCutmarkFile.exists() && newCutmarkFile.canRead()) {
            Transaction t = taFactory.createTransaction();
            if (rec.getStreams() == null || rec.getStreams().size() < 2) {
                TORead<Recording> tor = new TORead<Recording>(rec);
                t.add(tor);
                t.setRollbackOnly();
                t.execute();
                if (tor.getResult() != null && tor.getResult().size() > 0) rec = tor.getResult().get(0);
            }
            processCutMarkFile(rec, newCutmarkFile);
            t = taFactory.createTransaction();
            t.add(new TORemove<CutMark>(rec, CutMark.class));
            t.add(new TOSave<Recording>(rec));
            t.execute();
        }
    }

    @Override
    public void doRecode(Recording rec, AudioSelection as, String profile) {
        throw new UnsupportedOperationException("recoding is not a frontend action!");
    }

    @Override
    public void doReIndex(Recording rec) {
        File indexFile = rec.getKeyFile();
        indexFile.delete();
        String genindex = config.getGenindexPesPath().getAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder(new String[] { genindex, "-q" });
        pb.redirectErrorStream(true);
        pb.directory(rec.getPath());
        try {
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                getLogger().info("old: " + line);
            }
        } catch (Throwable t) {
            throw new ApplicationException("failed to reindex [" + rec.getPath() + "]");
        }
    }

    @Override
    public TransactionStatus doRemoveFromList(BonusItem item) {
        getLogger().info("remove item >" + item.getPath() + "< from database.");
        Transaction ta = taFactory.createTransaction();
        ta.add(new TORemove<BonusItem>(item));
        ta.execute();
        return ta.getStatus();
    }

    @Override
    public void doSaveCutMarks(List<BonusItem> selected) {
        for (BonusItem item : selected) {
            if (item instanceof Recording) {
                Recording rec = (Recording) item;
                writeVdrCutmarks(rec);
                touch(rec);
            }
        }
    }

    @Override
    public void doScan(File scanRoot) {
        doScan(scanRoot, 0);
    }

    @Override
    public void doScan(File scanRoot, int autobackup) {
        List<BonusItem> scannedItems = null;
        Date start = new Date();
        int saved = 0;
        if (scanRoot != null) {
            try {
                config.setRecordingRoot(scanRoot.getCanonicalFile());
            } catch (Throwable t) {
                getLogger().fatal("invalid recording root specified!", t);
            }
        }
        scanner.run();
        scannedItems = scanner.getScannedItems();
        getLogger().info("scanned " + scannedItems.size() + " new/updated items - lets save em");
        for (BonusItem cur : scannedItems) {
            Transaction ta = taFactory.createTransaction();
            if (!cur.isDirty()) {
                getLogger().info("==> skip non-dirty item " + cur.getName());
                continue;
            }
            if (cur instanceof Recording) {
                Recording rec = (Recording) cur;
                if (!rec.isDVD() && (rec.getCutMarks() == null || rec.getCutMarks().size() < 1)) {
                    getLogger().fatal("==> skip incomplete/failed scan of recording (no cutmarks): " + cur.getPath());
                    continue;
                }
                if (rec.getStreams() == null || rec.getStreams().size() < 2) {
                    getLogger().fatal("==> skip incomplete/failed scan of recording (no streams): " + cur.getPath());
                    continue;
                }
                if (((Recording) cur).getCutMarks().size() % 2 > 0) {
                    getLogger().fatal(">>> " + cur.getName() + " has odd cutmarks!!!");
                    continue;
                }
            }
            try {
                if (cur.getId() != null && cur.getId() > 0 && cur instanceof Recording) {
                    ta.add(new TORemove<CutMark>(cur, CutMark.class));
                }
                ta.add(new TOSave<BonusItem>(cur));
                ta.execute();
                saved++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Date end = new Date();
        getLogger().warn("scanning of " + scannedItems.size() + " items took " + ((double) (end.getTime() - start.getTime()) / 1000));
        getLogger().warn("saved " + saved + " out of " + scannedItems.size());
    }

    @Override
    public void doSplit(ProgressPublisher pp, Recording oldRec, Recording newRec, CutMark splitPoint) {
        if (oldRec == null || newRec == null || oldRec.getPath().compareTo(newRec.getPath()) == 0) {
            getLogger().info("new recording has same path as old one, so no split at all!");
            return;
        }
        if (oldRec.getType().compareTo(Recording.VARIANT_PES_REC) != 0) {
            getLogger().error("Sorry, no split operation on non-Pes recordings!");
            return;
        }
        if (pp != null && !pp.isActive()) pp.start();
        File oldRecDir = oldRec.getPath();
        File newRecDir = newRec.getPath();
        List<Long> sizes = determineFileSizes(oldRec);
        int progress = 0;
        getLogger().info("performSplit at: " + splitPoint.getByteOffset() + " (FO:" + splitPoint.getFileOffset() + "), starting at file #" + splitPoint.getFileNumber());
        if (pp != null) pp.configure(0, 10);
        if (pp != null) pp.publishProgress(progress++, "create directory");
        try {
            newRecDir.mkdirs();
            if (newRecDir.exists() && newRecDir.isDirectory()) {
                int newIndex = 1;
                if (pp != null) pp.publishProgress(progress++, "copy file with splitpoint");
                File destFile = newRec.getVideoFile(newIndex);
                File srcFile = oldRec.getVideoFile(splitPoint.getFileNumber());
                FileUtils.copyFile(destFile, srcFile);
                if (pp != null) pp.publishProgress(progress++, "move rest of movie files");
                for (int i = splitPoint.getFileNumber() + 1; i < sizes.size(); i++) {
                    destFile = newRec.getVideoFile(++newIndex);
                    srcFile = oldRec.getVideoFile(i);
                    FileUtils.moveOrRename(destFile, srcFile);
                }
                if (pp != null) pp.publishProgress(progress++, "reindex old movie");
                doReIndex(oldRec);
                if (pp != null) pp.publishProgress(progress++, "reindex new movie");
                doReIndex(newRec);
                File indexFile = newRec.getKeyFile();
                if (indexFile.exists() && indexFile.length() > 20) {
                    if (pp != null) pp.publishProgress(progress++, "calculate new cutmarks for both recordings");
                    List<CutMark>[] res = splitCutmarks(newRec, oldRec.getCutMarks(), splitPoint);
                    oldRec.setCutMarks(res[0]);
                    newRec.setCutMarks(res[1]);
                    if (pp != null) pp.publishProgress(progress++, "save new recordings to persistence");
                    TransactionFactory taf = (TransactionFactory) ApplicationServiceProvider.getService(TransactionFactory.class);
                    Transaction ta = taf.createTransaction();
                    ta.add(new TOSave<Recording>(oldRec));
                    ta.add(new TOSave<Recording>(newRec));
                    ta.execute();
                    if (pp != null) pp.publishProgress(progress++, "create info.vdr for new movie");
                    File infoSrc = new File(oldRecDir, oldRec.isPesRecording() ? "info.vdr" : "info");
                    File infoDest = new File(newRecDir, infoSrc.getName());
                    FileUtils.copyFile(infoDest, infoSrc);
                    updateInfo(oldRec);
                    updateInfo(newRec);
                    File cmx = new File(oldRec.getPath(), "marks.pjx");
                    if (cmx.exists()) writePjxCutmarks(oldRec, cmx); else writeVdrCutmarks(oldRec);
                    writeVdrCutmarks(newRec);
                }
                if (pp != null) pp.publishProgress(progress++, "done");
            }
        } catch (InvalidSplitOperationException iso) {
            getLogger().error("What the ...", iso);
            throw iso;
        } catch (Exception e) {
            getLogger().error("Oups", e);
        } finally {
            if (pp != null && pp.isActive()) pp.end();
        }
    }

    @Override
    public void doTag(List<BonusItem> selected) {
        if (selected != null && selected.size() > 0) {
            for (BonusItem item : selected) touch(item);
        }
    }

    @Override
    public boolean isAlreadyCut(Recording rec) {
        boolean rv = true;
        if (rec != null) {
            File check = rec.getPath().getParentFile();
            if (!check.getName().startsWith("%")) {
                List<CutMark> curMarks = rec.getCutMarks();
                List<Long> fileSizes = determineFileSizes(rec);
                List<CutMark> initialMarks = CutMark.createDefaults(rec, fileSizes);
                if (curMarks != null && curMarks.size() == initialMarks.size()) {
                    for (int i = 0; i < initialMarks.size(); i++) {
                        if (curMarks.get(i).compareTo(initialMarks.get(i)) != 0) {
                            rv = false;
                            break;
                        }
                    }
                } else rv = false;
            }
        }
        return rv;
    }

    public void processCutMarkFile(Recording r, File cmf) {
        List<Long> fileSizes = determineFileSizes(r);
        processCutMarkFile(r, cmf, fileSizes);
    }

    @Override
    public void processCutMarkFile(Recording r, File cmf, List<Long> fileSizes) {
        List<CutMark> newCutMarks = null;
        try {
            if (cmf == null) newCutMarks = CutMark.createDefaults(r, fileSizes); else newCutMarks = scanCutMarks(r, cmf, fileSizes);
        } catch (Exception e) {
            getLogger().error("failed to read cutmarks from " + cmf, e);
        }
        if (newCutMarks != null && newCutMarks.size() > 0) r.setCutMarks(newCutMarks);
        determineCutSize(r);
    }

    public List<CutMark> scanCutMarks(Recording r, File cmf, List<Long> fileSizes) {
        List<CutMark> newCutMarks = new ArrayList<CutMark>();
        BufferedReader inputStream = null;
        String line;
        try {
            inputStream = new BufferedReader(new FileReader(cmf.getAbsolutePath()));
            while ((line = inputStream.readLine()) != null) {
                if (line.startsWith(CutMark.PJX_INTRO)) continue;
                if (line.contains(":")) {
                    CutMark cm = CutMark.valueOf(r, line, fileSizes);
                    if (cm != null) newCutMarks.add(cm);
                } else {
                    String parts[] = line.split("\\s+");
                    CutMark cm = CutMark.valueOf(r, Long.valueOf(parts[0]), fileSizes);
                    if (cm != null) newCutMarks.add(cm);
                }
            }
        } catch (Exception e) {
            getLogger().fatal("failed to determine cutmarks: ", e);
        } finally {
            if (inputStream != null) try {
                inputStream.close();
                inputStream = null;
            } catch (IOException e) {
            }
            System.gc();
        }
        if (newCutMarks.size() % 2 != 0) {
            CutMark cm = CutMark.createLast(r, fileSizes);
            if (cm != null) newCutMarks.add(cm);
        }
        return newCutMarks;
    }

    @Override
    public void setup() {
        taFactory = (TransactionFactory) ApplicationServiceProvider.getService(TransactionFactory.class);
        scanner = (RecordingScanner) ApplicationServiceProvider.getService(RecordingScanner.class);
        Transaction ta = taFactory.createTransaction();
        config = new RMConfig();
        ta.add(new TORead<RMConfig>(config));
        ta.setRollbackOnly();
        ta.execute();
        scanner.setRecHandler(this);
        scanner.setConfig(config);
    }

    public void touch(BonusItem bi) {
        File keyFile = new File(bi.getPath().getAbsolutePath());
        keyFile.setLastModified(new Date().getTime());
    }

    @Override
    public void triggerBackup(Recording rec, AudioSelection as, String profile, Transaction ta) {
        doRecode(rec, as, profile, ta);
    }

    @Override
    public void updateInfo(Recording rec) {
        if (rec != null && rec.getId() != null && rec.getPath().exists()) {
            getLogger().info("update info on recording: " + rec);
            RecInfo ri = new RecInfo(rec);
            try {
                ri.read();
                ri.setTitle(rec.getSection().replace("\n", "|"));
                ri.setSubtitle(rec.getTitle().replace("\n", "|"));
                ri.setDescription(rec.getDescription().replace("\n", "|"));
                ri.write();
            } catch (Exception e) {
                getLogger().info("info update failed with", e);
            }
        } else {
            getLogger().info("update info: got invalid recording! Skipped.");
        }
    }

    @Override
    public String writePjxCutmarks(Recording r, File destDir) {
        String rv = null;
        PrintWriter cm = null;
        try {
            File cutMarkFile = new File(destDir, "marks.pjx");
            cm = new PrintWriter(new FileWriter(cutMarkFile.getAbsolutePath()));
            cm.println("CollectionPanel.CutMode=0");
            for (CutMark mark : r.getCutMarks()) cm.println(mark.getByteOffset());
            if (cutMarkFile.exists()) rv = cutMarkFile.getAbsolutePath();
        } catch (Exception e) {
            getLogger().fatal("failed to write pjx cutmarks ", e);
        } finally {
            if (cm != null) cm.close();
        }
        return rv;
    }

    public String writeVdrCutmarks(Recording r) {
        String rv = null;
        PrintWriter cm = null;
        try {
            File cutMarkFile = new File(r.getPath(), r.isPesRecording() ? "marks.vdr" : "marks");
            cm = new PrintWriter(new FileWriter(cutMarkFile.getAbsolutePath()));
            for (CutMark mark : r.getCutMarks()) cm.println(mark.getTimeCode());
            if (cutMarkFile.exists()) rv = cutMarkFile.getAbsolutePath();
        } catch (Exception e) {
            getLogger().fatal("failed to write vdr cutmarks ", e);
        } finally {
            if (cm != null) cm.close();
        }
        return rv;
    }

    protected void dumpCutmark(int n, CutMark cm) {
        getLogger().info(n + ") cutmark: " + cm.getFileNumber() + " / " + cm.getFileOffset() + " - " + cm.getByteOffset());
    }

    @SuppressWarnings("unchecked")
    protected List<CutMark>[] splitCutmarks(Recording splittedRec, List<CutMark> source, CutMark splitPoint) {
        List<CutMark>[] rv = new List[2];
        List<Long> sizes = determineFileSizes(splittedRec);
        rv[0] = new ArrayList<CutMark>();
        rv[1] = new ArrayList<CutMark>();
        CutMark any;
        boolean inside = false;
        int i = 0;
        for (any = source.get(i); any.getByteOffset() < splitPoint.getByteOffset(); any = source.get(++i)) {
            rv[0].add(any);
            inside = !inside;
        }
        if (inside) {
            rv[0].add(splitPoint);
            if (any.getByteOffset().equals(splitPoint.getByteOffset())) {
                any = source.get(++i);
                inside = false;
            }
        }
        int fileCorr = splitPoint.getFileNumber() - 1;
        long byteOff = 0;
        long bytePosition = 0;
        CutMark oldCM;
        if (inside) rv[1].add(CutMark.valueOf(splittedRec, splitPoint.getFileOffset(), sizes));
        for (; i < source.size(); i++) {
            oldCM = source.get(i);
            byteOff = 0;
            for (int n = fileCorr + 1; n < oldCM.getFileNumber(); n++) {
                byteOff += sizes.get(n - fileCorr);
            }
            bytePosition = byteOff + oldCM.getFileOffset();
            any = CutMark.valueOf(splittedRec, bytePosition, sizes);
            rv[1].add(any);
        }
        return rv;
    }

    private RMConfig config;

    private RecordingScanner scanner;
}
