package netxrv.jnlp.tardiff;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipFile;
import netxrv.jnlp.DiffPatcher;
import netxrv.jnlp.jardiff.JarDiffPatcher;
import netxrv.jnlp.jardiff.Patcher;
import netxrv.jnlp.jardiff.Patcher.PatchDelegate;
import netxrv.jnlp.util.InputStreamUtil;
import netxrv.jnlp.util.StringUtil;

public class TarDiffPatcher extends DiffPatcher {

    private static Logger logger = Logger.getLogger(TarDiffPatcher.class.getName());

    public TarDiffPatcher() {
    }

    private void writeEntry(TarOutputStream tos, TarEntry entry, InputStream data) throws IOException {
        tos.putNextEntry(entry);
        byte[] newBytes = InputStreamUtil.createReadBuffer();
        for (int size = data.read(newBytes); size != -1; size = data.read(newBytes)) tos.write(newBytes, 0, size);
        data.close();
        tos.closeEntry();
    }

    public synchronized void applyRecursiveJarDiffPatch(TarFile oldTar, TarFile tarDiff, TarEntry entry, netxrv.jnlp.jardiff.Patcher.PatchDelegate delegate, TarOutputStream tos, String jarName) throws Exception {
        TarEntry oldEntry = oldTar.getEntryByName(jarName);
        File oldEntryTempFile = InputStreamUtil.writeToFile(oldTar.getInputStream(oldEntry), null, ".jar", null);
        File entryJarDiff = InputStreamUtil.writeToFile(tarDiff.getInputStream(entry), null, ".jar", null);
        logger.info("oldEntryTempFile:" + oldEntryTempFile.getAbsolutePath() + " entryJarDiff:" + entryJarDiff.getAbsolutePath());
        ZipFile oldEntryJar = new ZipFile(oldEntryTempFile);
        File tempEntryFile = File.createTempFile("jdp", ".jar");
        tempEntryFile.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(tempEntryFile);
        BufferedOutputStream entryOutputStream = new BufferedOutputStream(fileOutputStream);
        (new JarDiffPatcher()).applyPatch(delegate, oldEntryTempFile.getAbsolutePath(), entryJarDiff.getAbsolutePath(), entryOutputStream);
        entryOutputStream.close();
        FileInputStream entryInputStream = new FileInputStream(tempEntryFile);
        TarEntry tarEntry = createTarEntry(entry, jarName, tempEntryFile);
        writeEntry(tos, tarEntry, entryInputStream);
        entryInputStream.close();
        tempEntryFile.delete();
        oldEntryJar.close();
        oldEntryTempFile.delete();
        entryJarDiff.delete();
    }

    private TarEntry createTarEntry(TarEntry entry, String entryName, File tempEntryFile) {
        TarEntry tarEntry = new TarEntry(entryName);
        tarEntry.setFileMode(entry.getFileMode());
        tarEntry.setFileSize((new Long(tempEntryFile.length())).intValue());
        tarEntry.setGroupUserId(entry.getGroupUserId());
        tarEntry.setLastModificationTime(entry.getLastModificationTime());
        tarEntry.setLinkIndicator(entry.getLinkIndicator());
        tarEntry.setNameOfLinkedFile(entry.getNameOfLinkedFile());
        tarEntry.setOffset(entry.getOffset());
        tarEntry.setOwnerUserId(entry.getOwnerUserId());
        return tarEntry;
    }

    public synchronized void applyRecursiveTarDiffPatch(TarFile oldTar, TarFile tarDiff, TarEntry entry, netxrv.jnlp.jardiff.Patcher.PatchDelegate delegate, TarOutputStream tos, String tarName) throws Exception {
        TarEntry oldEntry = oldTar.getEntryByName(tarName);
        String tarExt = null;
        if (tarName.endsWith(".tar.gz")) tarExt = ".tar.gz"; else tarExt = ".tar";
        File oldEntryTempFile = InputStreamUtil.writeToFile(oldTar.getInputStream(oldEntry), null, tarExt, null);
        File entryTarDiff = InputStreamUtil.writeToFile(tarDiff.getInputStream(entry), null, tarExt, null);
        File tempEntryFile = File.createTempFile("jdp", (new StringBuilder()).append(".").append(tarExt).toString());
        tempEntryFile.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(tempEntryFile);
        BufferedOutputStream entryOutputStream = new BufferedOutputStream(fileOutputStream);
        applyPatch(delegate, oldEntryTempFile.getAbsolutePath(), entryTarDiff.getAbsolutePath(), entryOutputStream);
        entryOutputStream.close();
        FileInputStream entryInputStream = new FileInputStream(tempEntryFile);
        TarEntry tarEntry = createTarEntry(entry, tarName, tempEntryFile);
        writeEntry(tos, tarEntry, entryInputStream);
        entryInputStream.close();
        tempEntryFile.delete();
        oldEntryTempFile.delete();
        entryTarDiff.delete();
    }

    private TarEntry createNewEntry(String newName, TarEntry oldEntry) {
        TarEntry newEntry = new TarEntry(newName);
        newEntry.setFileMode(oldEntry.getFileMode());
        newEntry.setOwnerUserId(oldEntry.getOwnerUserId());
        newEntry.setFileSize(oldEntry.getFileSize());
        newEntry.setLastModificationTime(oldEntry.getLastModificationTime());
        newEntry.setCheckSumForHeaderBlock(oldEntry.getCheckSumForHeaderBlock());
        newEntry.setLinkIndicator(oldEntry.getLinkIndicator());
        newEntry.setNameOfLinkedFile(oldEntry.getNameOfLinkedFile());
        return newEntry;
    }

    public synchronized void applyPatch(netxrv.jnlp.jardiff.Patcher.PatchDelegate delegate, String oldTarPath, String tarDiffPath, OutputStream result) throws Exception {
        logger.info("tarDiffPath [" + tarDiffPath + "] open tar file [" + oldTarPath + "]");
        TarFile tarDiff = new TarFile(tarDiffPath);
        Set ignoreSet = new HashSet();
        Map renameMap = new HashMap();
        determineNameMapping(tarDiff, ignoreSet, renameMap);
        Set<String> oldtarNames = new HashSet();
        TarFile oldTar = new TarFile(oldTarPath);
        Collection oldEntries = oldTar.getTarEntries();
        TarEntry tarEntry;
        for (Iterator i$ = oldEntries.iterator(); i$.hasNext(); oldtarNames.add(tarEntry.getFileName())) tarEntry = (TarEntry) i$.next();
        Object keys[] = renameMap.keySet().toArray();
        double size = oldtarNames.size() + keys.length + tarDiff.getTarEntries().size();
        double currentEntry = 0.0D;
        oldtarNames.removeAll(ignoreSet);
        size -= ignoreSet.size();
        Collection entries = tarDiff.getTarEntries();
        TarOutputStream tos = null;
        if (result instanceof TarOutputStream) tos = (TarOutputStream) result; else if (oldTarPath.endsWith(".gz")) tos = new TarOutputStream(new GZIPOutputStream(result)); else tos = new TarOutputStream(result);
        int jarDiffExtLength = ".jardiff".length();
        Iterator itr = entries.iterator();
        do {
            if (!itr.hasNext()) break;
            TarEntry entry = (TarEntry) itr.next();
            String entryName = entry.getFileName();
            if ("META-INF/INDEX.TD".equals(entryName)) size--; else if (entryName.endsWith(".jardiff")) {
                int entryNameLength = entryName.length();
                String frontPart = entryName.substring(0, entryNameLength - jarDiffExtLength);
                String jarName = (new StringBuilder()).append(frontPart).append(".jar").toString();
                applyRecursiveJarDiffPatch(oldTar, tarDiff, entry, delegate, tos, jarName);
                oldtarNames.remove(jarName);
                size--;
            } else if (entryName.endsWith(".tardiff.gz") || entryName.endsWith(".tardiff")) {
                int entryNameLength = entryName.length();
                String tarDiffExt = null;
                String tarExt = null;
                if (entryName.endsWith(".tardiff.gz")) {
                    tarDiffExt = ".tardiff.gz";
                    tarExt = ".tar.gz";
                } else {
                    tarDiffExt = ".tardiff";
                    tarExt = ".tar";
                }
                String frontPart = entryName.substring(0, entryNameLength - tarDiffExt.length());
                String tarName = (new StringBuilder()).append(frontPart).append(tarExt).toString();
                applyRecursiveTarDiffPatch(oldTar, tarDiff, entry, delegate, tos, tarName);
                oldtarNames.remove(tarName);
                size--;
            } else {
                updateDelegate(delegate, currentEntry, size);
                currentEntry++;
                writeEntry(tos, entry, tarDiff.getInputStream(entry));
                boolean wasInOld = oldtarNames.remove(entryName);
                if (wasInOld) size--;
            }
        } while (true);
        for (int j = 0; j < keys.length; j++) {
            String newName = (String) keys[j];
            String oldName = (String) renameMap.get(newName);
            TarEntry oldEntry = oldTar.getEntryByName(oldName);
            if (oldEntry == null) {
                String moveCmd = (new StringBuilder()).append("move").append(oldName).append(" ").append(newName).toString();
                handleException("jardiff.error.badmove", moveCmd);
            }
            TarEntry newEntry = createNewEntry(newName, oldEntry);
            updateDelegate(delegate, currentEntry, size);
            currentEntry++;
            writeEntry(tos, newEntry, oldTar.getInputStream(oldEntry));
            boolean wasInOld = oldtarNames.remove(oldName);
            if (wasInOld) size--;
        }
        for (String name : oldtarNames) {
            TarEntry oldEntry = oldTar.getEntryByName(name);
            updateDelegate(delegate, currentEntry, size);
            currentEntry++;
            writeEntry(tos, oldEntry, oldTar.getInputStream(oldEntry));
        }
        updateDelegate(delegate, currentEntry, size);
        tos.finish();
    }

    private static void determineNameMapping(TarFile tarDiff, Set ignoreSet, Map renameMap) throws IOException {
        InputStream is = tarDiff.getInputStream(tarDiff.getEntryByName("META-INF/INDEX.TD"));
        if (is == null) handleException("jardiff.error.noindex", null);
        LineNumberReader indexReader = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
        String line = indexReader.readLine();
        if (line == null || !line.equals("version 1.0")) handleException("jardiff.error.badheader", line);
        do {
            if ((line = indexReader.readLine()) == null) break;
            if (line.startsWith("remove")) {
                List sub = StringUtil.getSubpaths(line.substring("remove".length()));
                if (sub.size() != 1) handleException("jardiff.error.badremove", line);
                ignoreSet.add(sub.get(0));
            } else if (line.startsWith("move")) {
                List sub = StringUtil.getSubpaths(line.substring("move".length()));
                if (sub.size() != 2) handleException("jardiff.error.badmove", line);
                if (renameMap.put(sub.get(1), sub.get(0)) != null) handleException("jardiff.error.badmove", line);
            } else if (line.length() > 0) handleException("jardiff.error.badcommand", line);
        } while (true);
    }
}
