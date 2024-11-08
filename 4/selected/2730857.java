package de.schwarzrot.jobs.processing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.schwarzrot.app.errors.ApplicationException;
import de.schwarzrot.jobs.processing.support.AbstractXProcessStep;
import de.schwarzrot.rec.domain.Recording;
import de.schwarzrot.system.support.FileUtils;

public class MoveStep extends AbstractXProcessStep {

    private static final String PREFIX = "MOVE";

    private static Pattern STREAM_PAT = Pattern.compile("^Stream:\\s+'([^']+)'$");

    protected MoveStep(ProcessDefinition parent, String workRoot) {
        super(parent, workRoot);
        inputFiles = new TreeSet<File>();
    }

    @Override
    protected void cleanup() {
        List<Recording> recs2Process = getProcessDefinition().getSubjects();
        for (int i = 0; i < recs2Process.size(); ++i) {
            File workDir = new File(getWorkRoot(), String.format(RECDIR_MASK, i));
            FileUtils.removeDirectory(workDir);
        }
    }

    @Override
    protected String getPrefix() {
        return PREFIX;
    }

    @Override
    protected boolean postCheck(int recNum) {
        Recording rec = getProcessDefinition().getSubjects().get(recNum);
        String format = "%03d.vdr";
        File targetDir = getCutPath(rec, getJob().getTarget());
        File target = null;
        if ("TS".compareTo(getJob().getTarget()) == 0) format = "%05d.ts";
        if (inputFiles.size() < 1) processLog(recNum);
        if (inputFiles.size() < 1) return false;
        for (int i = 0; i < inputFiles.size(); ) {
            target = new File(targetDir, String.format(format, ++i));
            if (!target.exists() || target.length() == 0) return false;
        }
        return true;
    }

    @Override
    protected boolean preCheck(int recNum) {
        long sizeTotal = 0l;
        if (inputBytes == 0 || inputFiles.size() < 1) processLog(recNum);
        for (File cur : inputFiles) sizeTotal += cur.length();
        boolean rv = sizeTotal > 0 && sizeTotal >= inputBytes;
        if (!rv) log("Oups, sum of processed filed did not match: was " + sizeTotal + " - should be " + inputBytes);
        return rv;
    }

    @Override
    protected void prepare() {
        super.prepare();
        List<Recording> recs2Process = getProcessDefinition().getSubjects();
        for (int i = 0; i < recs2Process.size(); ++i) {
            Recording rec = recs2Process.get(i);
            File target = getCutPath(rec, getJob().getTarget());
            if (target.exists()) throw new ApplicationException("Oups, cutted recording does already exists!");
            log("target of cutted recording is: " + target.getAbsolutePath());
            target.mkdirs();
            sysInfo.syncFS();
            if (!(target.exists() && target.isDirectory())) throw new ApplicationException("Failed to create path for cutted recording!");
        }
    }

    protected void processLog(int recNum) {
        File workDir = new File(getWorkRoot(), String.format(RECDIR_MASK, recNum));
        NumberFormat nf = NumberFormat.getInstance();
        File logFile = null;
        for (File any : workDir.listFiles()) {
            if (any.getName().endsWith("_log.txt")) {
                logFile = any;
                break;
            }
        }
        if (logFile != null) {
            inputFiles.clear();
            inputBytes = 0l;
            String line = null;
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(logFile));
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("Stream:")) {
                        Matcher m = STREAM_PAT.matcher(line);
                        if (m.matches()) {
                            inputFiles.add(new File(m.group(1)));
                            line = br.readLine();
                            if (line.startsWith("=>")) {
                                String[] parts = line.split("\\s+");
                                Number tmp = nf.parse(parts[1]);
                                inputBytes = tmp.longValue();
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                log("failed to process logfile", t);
            }
        }
    }

    @Override
    protected void workOut(int recNum) {
        Recording rec = getProcessDefinition().getSubjects().get(recNum);
        String format = "%03d.vdr";
        File targetDir = getCutPath(rec, getJob().getTarget());
        File target = null;
        int i = 0;
        if ("TS".compareTo(getJob().getTarget()) == 0) format = "%05d.ts";
        for (File source : inputFiles) {
            target = new File(targetDir, String.format(format, ++i));
            log("should move file ]" + source + "[ => ]" + target + "[");
            FileUtils.moveOrRename(target, source);
        }
        for (File source : rec.getPath().listFiles()) {
            if ("info.vdr".compareTo(source.getName()) == 0 || "info".compareTo(source.getName()) == 0) {
                if ("TS".compareTo(getJob().getTarget()) == 0) target = new File(targetDir, "info"); else target = new File(targetDir, "info.vdr");
                log("should copy file ]" + source + "[ => ]" + target + "[");
                FileUtils.copyFile(target, source);
            } else if (source.getName().startsWith("info.")) {
                target = new File(targetDir, source.getName());
                log("should copy file ]" + source + "[ => ]" + target + "[");
                FileUtils.copyFile(target, source);
            }
        }
    }

    private Collection<File> inputFiles;

    private long inputBytes;
}
