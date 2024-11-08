package de.fzj.roctopus.implementation.unicore5;

import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unicore.ajo.AbstractJob;
import org.unicore.ajo.ChangePermissions;
import org.unicore.ajo.CreateDirectory;
import org.unicore.ajo.DeleteFile;
import org.unicore.ajo.FileCheck;
import org.unicore.ajo.ListDirectory;
import org.unicore.outcome.AbstractActionStatus;
import org.unicore.outcome.ChangePermissions_Outcome;
import org.unicore.outcome.CreateDirectory_Outcome;
import org.unicore.outcome.DeleteFile_Outcome;
import org.unicore.outcome.FileCheck_Outcome;
import org.unicore.outcome.ListDirectory_Outcome;
import org.unicore.outcome.Outcome;
import org.unicore.outcome.XDirectory;
import org.unicore.outcome.XFile;
import org.unicore.resources.Home;
import org.unicore.resources.Root;
import org.unicore.sets.ResourceSet;
import org.unicore.sets.XFileEnumeration;
import com.fujitsu.arcon.servlet.VsiteTh;
import de.fzj.roctopus.File;
import de.fzj.roctopus.Locatable;
import de.fzj.roctopus.Location;
import de.fzj.roctopus.Site;
import de.fzj.roctopus.Storage;
import de.fzj.roctopus.Task;
import de.fzj.roctopus.exceptions.RoctopusException;
import de.fzj.roctopus.implementation.unicore5.tasks.UnicoreExportTask;
import de.fzj.roctopus.implementation.unicore5.tasks.UnicoreImportTask;

public class UnicoreFile extends BaseLocatable implements File {

    private static final Logger log = LoggerFactory.getLogger(UnicoreFile.class);

    private UnicoreStorage unicorestorage;

    private boolean cached = false, exists, isdirectory;

    private Location location;

    private String path;

    private Date lastmodified;

    private long filesize;

    private boolean canexecute;

    private boolean canread;

    private boolean canwrite;

    protected UnicoreFile(UnicoreStorage us, String path) throws RoctopusException {
        this.unicorestorage = us;
        this.path = path;
        this.location = (Location) us.getLocation().getChildLocation(path);
    }

    @Override
    public boolean isFile() {
        return true;
    }

    public void refresh() throws RoctopusException {
        try {
            cache();
        } catch (Exception e) {
            throw new RoctopusException("xxx", e);
        }
    }

    private void cache() throws Exception {
        log.info("caching information about a UnicoreFile ..." + this.getLocation());
        FileCheck_Outcome fco;
        try {
            VsiteTh vsth = getUnicoreStorage().getUnicoreSite().getVSiteThForThisUnicoreSite();
            FileCheck filecheck = new FileCheck("filecheck");
            filecheck.addResource(this.unicorestorage.getUnicoreStorageResource());
            String path = getPath();
            if (path.length() == 0) path = ".";
            filecheck.setFileToCheck(path);
            filecheck.setFileExists(true);
            fco = (FileCheck_Outcome) executeAction(filecheck, vsth);
        } catch (Exception e) {
            log.error("cannot details");
            throw new RoctopusException("cannot cache", e);
        }
        if (fco.getStatus().isEquivalent(AbstractActionStatus.NOT_SUCCESSFUL)) {
            log.error("cannot cache details");
            RoctopusException ex = new RoctopusException("cannot cache details");
            ex.setLog(fco.getLog().toString());
            throw ex;
        }
        this.exists = fco.getDecision().equals(FileCheck_Outcome.SATISFIED);
        if (fco.getFile() != null) {
            this.isdirectory = (fco.getFile() instanceof XDirectory);
            this.lastmodified = fco.getFile().getModifyDate();
            this.filesize = fco.getFile().getSize();
            this.canexecute = fco.getFile().canExecute();
            this.canread = fco.getFile().canRead();
            this.canwrite = fco.getFile().canWrite();
        } else {
            this.isdirectory = false;
            this.lastmodified = null;
            this.filesize = 0;
        }
        this.cached = true;
    }

    public Task importLocalFile(java.io.File localfile, boolean overwrite) throws RoctopusException {
        log.info("importing localfile ...." + localfile.toString() + " to " + this.getLocation());
        if (exists() && !isDirectory() && !overwrite) {
            throw new RoctopusException("The remote file already exists, but shouldn't be overwritten!");
        }
        Task impt = new UnicoreImportTask(localfile, this);
        impt.setAutomaticallyAdvanceToCleanup(true);
        impt.startSync(localfile);
        return impt;
    }

    public Task exportToLocalFile(java.io.File localfile, boolean overwrite) throws RoctopusException {
        if (!exists()) {
            throw new RoctopusException("The file " + getPath() + " doesn't exist!");
        }
        if (localfile.exists() && !localfile.isDirectory() && !overwrite) throw new RoctopusException("A file" + localfile.getAbsolutePath() + " already exists and should not be overwritten!");
        if (localfile.isDirectory() && !overwrite) {
            java.io.File f = new java.io.File(localfile.getAbsolutePath() + java.io.File.separator + getName());
            if (f.exists()) throw new RoctopusException("A file" + localfile.getAbsolutePath() + " already exists and should not be overwritten!");
        }
        Task expt = new UnicoreExportTask(this, localfile);
        expt.setAutomaticallyAdvanceToCleanup(true);
        expt.startSync();
        return expt;
    }

    public Storage getStorage() {
        return this.unicorestorage;
    }

    protected UnicoreStorage getUnicoreStorage() {
        return this.unicorestorage;
    }

    public List<File> ls() throws RoctopusException {
        Location sibling;
        List<File> ufiles = new ArrayList<File>();
        try {
            VsiteTh vsth = getUnicoreStorage().getUnicoreSite().getVSiteThForThisUnicoreSite();
            ListDirectory ld = new ListDirectory("ls");
            ld.setTarget(getPath());
            ld.addResource(this.getUnicoreStorage().getUnicoreStorageResource());
            ListDirectory_Outcome ldo = (ListDirectory_Outcome) executeAction(ld, vsth);
            if (!ldo.getStatus().equals(AbstractActionStatus.SUCCESSFUL)) throw new RoctopusException("cannot ls ...");
            XFileEnumeration xfe = ldo.getListing().elements();
            while (xfe.hasMoreElements()) {
                XFile xfile = xfe.nextElement();
                try {
                    if (!this.isDirectory()) {
                        sibling = new Location(getLocation().toString());
                    } else {
                        sibling = (Location) getLocation().getChildLocation(xfile.getName().substring(xfile.getName().lastIndexOf("/") + 1));
                    }
                    UnicoreFile ff = new UnicoreFile(this.getUnicoreStorage(), sibling.getFilePath());
                    ff.exists = true;
                    ff.isdirectory = xfile instanceof XDirectory;
                    ff.lastmodified = xfile.getModifyDate();
                    ff.filesize = xfile.getSize();
                    ff.cached = true;
                    ff.canexecute = xfile.canExecute();
                    ff.canread = xfile.canRead();
                    ff.canwrite = xfile.canWrite();
                    ufiles.add(ff);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return ufiles;
        } catch (Exception e) {
            throw new RoctopusException("cannot ls, " + e.getMessage(), e);
        }
    }

    public boolean isRoot() throws RoctopusException {
        return getPath().equals("");
    }

    public boolean isDirectory() throws RoctopusException {
        try {
            if (this.cached == false) cache();
            return this.isdirectory;
        } catch (Exception e) {
            throw new RoctopusException("cannot isDirectory", e);
        }
    }

    public boolean exists() throws RoctopusException {
        try {
            if (this.cached == false) cache();
            return this.exists;
        } catch (Exception e) {
            log.error("", e);
            throw new RoctopusException("problems running exist()", e);
        }
    }

    public boolean mkdir() throws RoctopusException {
        try {
            VsiteTh vsth = getUnicoreStorage().getUnicoreSite().getVSiteThForThisUnicoreSite();
            CreateDirectory cd = new CreateDirectory("createdirectory");
            cd.setTarget(getPath());
            cd.addResource(this.unicorestorage.getUnicoreStorageResource());
            CreateDirectory_Outcome cdo = (CreateDirectory_Outcome) executeAction(cd, vsth);
            if (cdo.getStatus().isEquivalent(AbstractActionStatus.NOT_SUCCESSFUL)) {
                RoctopusException e = new RoctopusException("cannot mkdir");
                e.setLog(cdo.getLog().toString());
                throw e;
            }
        } catch (Exception e) {
            throw new RoctopusException("cannot mkdir", e);
        }
        try {
            cache();
        } catch (Exception e) {
            throw new RoctopusException("Couldn't collect information about " + "the file " + getPath());
        }
        return true;
    }

    public boolean chmod(boolean read, boolean write, boolean execute) throws RoctopusException {
        if (!exists()) {
            throw new RoctopusException("A file " + getPath() + " doesn't exist!");
        }
        try {
            VsiteTh vsth = getUnicoreStorage().getUnicoreSite().getVSiteThForThisUnicoreSite();
            ChangePermissions cp = new ChangePermissions("chmod", new ResourceSet(this.getUnicoreStorage().getUnicoreStorageResource()), getPath(), read, write, execute);
            if (cp == null) throw new RoctopusException("The required storage is unknown!");
            ChangePermissions_Outcome cpo = (ChangePermissions_Outcome) executeAction(cp, vsth);
            log.info("the status of the chmod for the file " + getPath() + " is " + cpo.getStatus());
            if (!cpo.getStatus().isEquivalent(AbstractActionStatus.SUCCESSFUL)) {
                log.error("couldn't chmod file " + getPath());
                return false;
            }
        } catch (Exception e) {
            throw new RoctopusException("cannot chmod a file!", e);
        }
        try {
            cache();
        } catch (Exception e) {
            throw new RoctopusException("Couldn't collect information about the file " + getPath() + "!");
        }
        return true;
    }

    public boolean delete() throws RoctopusException {
        if (!exists()) {
            throw new RoctopusException("A file " + getPath() + " doesn't exist!");
        }
        try {
            VsiteTh vsth = getUnicoreStorage().getUnicoreSite().getVSiteThForThisUnicoreSite();
            DeleteFile df = new DeleteFile("DeleteFile", new ResourceSet(this.getUnicoreStorage().getUnicoreStorageResource()), getPath());
            if (df == null) throw new RoctopusException("The required storage is unknown!");
            DeleteFile_Outcome dfo = (DeleteFile_Outcome) executeAction(df, vsth);
            log.info("The status of the delete-action for the file " + getPath() + " is " + dfo.getStatus());
            if (!dfo.getStatus().isEquivalent(AbstractActionStatus.SUCCESSFUL)) {
                log.error("Couldn't delete file " + getPath());
                return false;
            }
        } catch (Exception e) {
            throw new RoctopusException("cannot delete a file!", e);
        }
        try {
            cache();
        } catch (Exception e) {
            throw new RoctopusException("Couldn't collect information about the file " + getPath() + "!");
        }
        return true;
    }

    public File getParentFile() throws RoctopusException {
        String newpath = ((Location) getLocation().getParent()).getFilePath();
        return new UnicoreFile(getUnicoreStorage(), newpath);
    }

    public File getChildFile(String child) throws RoctopusException {
        String newpath;
        newpath = ((Location) ((Location) getLocation()).getChildLocation(child)).getFilePath();
        return new UnicoreFile(getUnicoreStorage(), newpath);
    }

    private void checkTargetForFileOperations(File target, boolean overwrite) throws RoctopusException {
        if (!isDirectory() && target.exists() && !target.isDirectory() && !overwrite) {
            log.error("The target file already exists and " + "shouldn't be overwritten!");
            throw new RoctopusException("The target file already exists and " + "shouldn't be overwritten!");
        }
        if (isDirectory() && target.exists() && !target.isDirectory()) {
            log.error("The source file is a directory, but the target " + "file is not!");
            throw new RoctopusException("The source file is a directory, but the target " + "file is not!");
        }
        if (!target.isDirectory() && target.getParentFile() != null & !target.getParentFile().exists()) {
            log.error("The target directory " + target.getParentFile().getPath() + " doesn't exist!");
            throw new RoctopusException("The target directory " + target.getParentFile().getPath() + " doesn't exist!");
        }
        if (target.isDirectory()) {
            try {
                if (!overwrite && ((UnicoreFile) target).containsFile(this)) {
                    log.error("The target file already exists and " + "shouldn't be overwritten!");
                    throw new RoctopusException("The target file already exists and " + "shouldn't be overwritten!");
                }
            } catch (RoctopusException e) {
                throw e;
            }
        }
    }

    public void copyTo(File to, boolean overwrite) throws RoctopusException {
        log.info("Copying " + getPath() + " to " + to.getPath());
        checkTargetForFileOperations(to, overwrite);
        AbstractJob ajo = getAJOForCopyTo(this, to, overwrite);
        VsiteTh vsth = getUnicoreStorage().getUnicoreSite().getVSiteThForThisUnicoreSite();
        ajo.setVsite(vsth.getVsite());
        Outcome outcome;
        try {
            outcome = executeAction(ajo, vsth);
        } catch (Exception e) {
            throw new RoctopusException("", e);
        }
        if (!outcome.getStatus().isEquivalent(AbstractActionStatus.SUCCESSFUL)) {
            throw new RoctopusException("copy task has failed");
        }
    }

    public void moveTo(File to, boolean overwrite) throws RoctopusException {
        log.info("moving file " + getPath() + " to " + to.getPath());
        copyTo(to, overwrite);
        delete();
    }

    /**
   * transfers a file to the file remoteTo.
   */
    public Task transfer(File remoteto, boolean overwrite) throws RoctopusException {
        if (this.getSite().equals(remoteto.getSite())) {
            log.info("The remote file is on the same Vsite, calling " + "copyTo-method...");
            copyTo(remoteto, overwrite);
        }
        checkTargetForFileOperations(remoteto, overwrite);
        AbstractJob ajo;
        try {
            ajo = getTransferAJO(this, remoteto, overwrite);
        } catch (Exception e) {
            throw new RoctopusException("Error: ", e);
        }
        Task transfer = getStorage().getSite().submit(ajo);
        transfer.setAutomaticallyAdvanceToCleanup(true);
        transfer.startASync();
        return transfer;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        try {
            if (this.exists) {
                formatter.format(LONG_FORMAT, this.isDirectory() ? "d" : "-", this.canRead() ? "r" : "-", this.canWrite() ? "w" : "-", this.canExecute() ? "x" : "-", this.size(), this.lastModified(), this.getLocation().toString());
            } else {
                formatter.format(LONG_FORMAT_NONEXISTANT, "?", "?", "?", "?", this.getLocation().toString());
            }
        } catch (RoctopusException e) {
            return "troubles toString()ing ... ";
        }
        return sb.toString();
    }

    public Location getLocation() {
        return this.location;
    }

    public String getPath() {
        if (this.path.equals("/")) return ""; else return this.path;
    }

    private static String[] splitPath(String path) {
        ArrayList<String> l = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(path, "/");
        while (tok.hasMoreTokens()) {
            l.add(tok.nextToken());
        }
        return (String[]) l.toArray(new String[l.size()]);
    }

    public Date lastModified() throws RoctopusException {
        return this.lastmodified;
    }

    public long size() {
        return this.filesize;
    }

    public boolean canExecute() {
        return this.canexecute;
    }

    public boolean canRead() {
        return this.canread;
    }

    public boolean canWrite() {
        return this.canwrite;
    }

    protected boolean containsFile(UnicoreFile file) throws RoctopusException {
        if (!exists() || !isDirectory()) return false;
        List<File> files = ls();
        for (File f : files) {
            if (f.getName().equals(file.getName())) return true;
        }
        return false;
    }

    public Site getSite() {
        return this.getStorage().getSite();
    }

    public String getName() {
        return this.location.getName();
    }

    public List<? extends Locatable> getLocatableChildren() throws RoctopusException {
        return this.ls();
    }

    public Locatable getLocatableParent() throws RoctopusException {
        if (isRoot()) return getStorage(); else return getParentFile();
    }

    public Locatable getLocatableChild(String c) throws RoctopusException {
        return getChildFile(c);
    }
}
