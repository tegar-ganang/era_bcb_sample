package net.emotivecloud.vrmm.vtm.disk;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import net.emotivecloud.utils.ovf.EmotiveOVF;
import net.emotivecloud.vrmm.vtm.DomainStore;
import net.emotivecloud.vrmm.vtm.config.ConfigManager;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Manages the creation of checkpoints.
 * @author goirix
 */
public class Checkpoint {

    private Log log = LogFactory.getLog(DiskManager.class);

    private DomainStore domains;

    private String poolPath;

    private String aplicPath;

    private String checkPath;

    private String remotePath;

    private String hdfsAddress;

    private List<String> historic;

    private short compressionMode = 1;

    private boolean wholeCheckpoint = false;

    private HashMap<String, CompressionStats> compression;

    public Checkpoint(DomainStore domains) {
        this.domains = domains;
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(ConfigManager.getVtMConfigurationPath());
            poolPath = config.getString("pool");
            aplicPath = config.getString("aplic.path");
            checkPath = config.getString("checkpoint.path");
            remotePath = config.getString("checkpoint.path.remote", "/user/root/checkpoint/");
            if (config.containsKey("checkpoint.compress")) compressionMode = config.getShort("checkpoint.compress");
            if (config.containsKey("checkpoint.whole")) wholeCheckpoint = config.getBoolean("checkpoint.whole");
            hdfsAddress = config.getString("hdfs.address");
            log.info("Environment variables already set from file: \"" + ConfigManager.getVtMConfigurationPath() + "\".");
            log.debug("    POOL       = " + poolPath);
            log.debug("    APLIC PATH = " + aplicPath);
            log.debug("    CHECKPOINT = " + checkPath);
            log.debug("    HDFS       = " + hdfsAddress);
        } catch (ConfigurationException e) {
            log.error("Error reading configuration file: \"" + ConfigManager.getVtMConfigurationPath() + "\".");
        }
        this.historic = new LinkedList<String>();
        this.compression = new HashMap<String, CompressionStats>();
    }

    /**
	 * Stores the status of the task making a checkpoint.
	 * @param task
	 * @throws IOException
	 */
    public void createCheckpoint(String task) throws IOException {
        DiskManager.lock();
        long ini = System.currentTimeMillis();
        EmotiveOVF ovfDomEmo = this.domains.getDomainByName(task);
        if (ovfDomEmo != null) {
            this.domains.pause(ovfDomEmo.getId());
        }
        this.storeMemoyCheckpoint(task);
        log.info("Memory checkpoint in " + (System.currentTimeMillis() - ini) + "ms");
        this.manageDiskCheckpoint(task);
        this.restoreMemoryCheckpoint(task);
        log.info("Checkpoint created. Stopped time: " + (System.currentTimeMillis() - ini) + "ms");
        if (ovfDomEmo != null) {
            this.domains.unpause(ovfDomEmo.getId());
        }
        long iniUpload = System.currentTimeMillis();
        this.uploadDiskCheckpoint(task);
        log.info("Disk checkpoint of task " + task + " uploaded in " + (System.currentTimeMillis() - iniUpload) + "ms");
        log.info("Checkpoint of task " + task + " finished in " + (System.currentTimeMillis() - ini) + "ms");
        DiskManager.unlock();
    }

    /**
	 * Recover the status of a task already checkpointed.
	 * @param name Name of the domain to be recovered.
	 * @throws IOException
	 */
    public boolean recoverCheckpoint(String name) throws IOException {
        boolean success = false;
        DiskManager.lock();
        try {
            long iniDownload = System.currentTimeMillis();
            success = this.downloadDiskCheckpoint(name);
            log.info("Disk images of task " + name + " recovered: " + (System.currentTimeMillis() - iniDownload) + "ms");
            if (success) {
                long iniFileManage = System.currentTimeMillis();
                this.uncompressDiskCheckpoint(name);
                log.info("Downloaded files manage: " + (System.currentTimeMillis() - iniFileManage) + "ms");
            }
        } catch (IOException e) {
            log.error("Recovering checkpoint: " + e.getMessage());
        }
        DiskManager.unlock();
        return success;
    }

    /**
	 * Generates a checkpoint of a task execution memory.
	 * @param name Name of the virtual machine to checkpoint.
	 */
    private void storeMemoyCheckpoint(String name) {
        try {
            new File(checkPath + "/" + name + "/" + name + "-mem.img").delete();
            new File(checkPath + "/" + name + "/").mkdirs();
            Process procCheckpoint = Runtime.getRuntime().exec("xm save " + name + " " + checkPath + "/" + name + "/" + name + "-mem.img");
            if (procCheckpoint.waitFor() != 0) {
                log.error("Making a checkpoint of memory of task \"" + name + "\".");
                log.error(IOUtils.toString(procCheckpoint.getErrorStream()));
            }
        } catch (IOException e) {
            log.error("Creating checkpoint: " + e.getMessage());
        } catch (InterruptedException e) {
            log.error("Creating checkpoint: " + e.getMessage());
        }
    }

    /**
	 * Compress all the writable disks in order to be uploaded to hadoop.
	 * @param task
	 */
    private void manageDiskCheckpoint(String task) {
        try {
            boolean compress = false;
            if (compressionMode == 2) {
                compress = true;
            } else if (compressionMode == 1) {
                CompressionStats prevStats = compression.get(task);
                if (prevStats == null || (prevStats != null && prevStats.compress)) {
                    compress = true;
                }
            }
            long time = System.currentTimeMillis();
            if (compress) {
                log.info("Compressing disks to upload.");
                new File(checkPath + "/" + task + "/" + task + "-home.img").delete();
                Compressor.compressFileGz(poolPath + "/" + task + "/home.img", checkPath + "/" + task + "/" + task + "-home.img.gz");
                long srcSize = new File(poolPath + "/" + task + "/home.img").length();
                long dstSize = new File(checkPath + "/" + task + "/" + task + "-home.img.gz").length();
                CompressionStats stats = new CompressionStats();
                stats.compressTime = System.currentTimeMillis() - time;
                stats.srcSize = srcSize;
                stats.dstSize = dstSize;
                stats.compress = true;
                compression.put(task, stats);
            } else {
                log.info("Copying home disks to upload.");
                new File(checkPath + "/" + task + "/" + task + "-home.img.gz").delete();
                FileUtils.copyFile(new File(poolPath + "/" + task + "/home.img"), new File(checkPath + "/" + task + "/" + task + "-home.img"));
            }
            if (wholeCheckpoint) {
                log.info("Copying the other disks");
                if (new File(checkPath + "/" + task + "/" + task + "-debianbase.img").exists()) new File(checkPath + "/" + task + "/" + task + "-debianbase.img").delete();
                FileUtils.copyFile(new File(poolPath + "/" + task + "/debianbase.img"), new File(checkPath + "/" + task + "/" + task + "-debianbase.img"));
                if (new File(poolPath + "/" + task + "/breinAplic.img").exists()) {
                    if (new File(checkPath + "/" + task + "/" + task + "-breinAplic.img").exists()) new File(checkPath + "/" + task + "/" + task + "-breinAplic.img").delete();
                    FileUtils.copyFile(new File(poolPath + "/" + task + "/breinAplic.img"), new File(checkPath + "/" + task + "/" + task + "-breinAplic.img"));
                }
            }
            time = System.currentTimeMillis() - time;
            log.info("Checkpoint file manage: " + time + "ms");
        } catch (IOException e) {
            log.error("Managing checkpoint disks of task " + task + ": " + e.getMessage());
        }
    }

    /**
	 * Resumes a virtual machine.
	 * @param task Name of the virtual machine.
	 */
    private void restoreMemoryCheckpoint(String task) {
        try {
            Process procCheckpoint = Runtime.getRuntime().exec("xm restore " + checkPath + "/" + task + "/" + task + "-mem.img");
            if (procCheckpoint.waitFor() != 0) {
                log.error("Restoring checkpoint of of task \"" + task + "\".");
                log.error(IOUtils.toString(procCheckpoint.getErrorStream()));
            }
        } catch (IOException e) {
            log.error("Resotoring checkpoint: " + e.getMessage());
        } catch (InterruptedException e) {
            log.error("Resotring checkpoint: " + e.getMessage());
        }
    }

    /**
	 * Upload the checkpoint of a VM in the Hadoop File System.
	 * @param name Name of the virtual machine to checkpoint.
	 * @throws IOException 
	 */
    private void uploadDiskCheckpoint(String name) throws IOException {
        try {
            Configuration conf = new Configuration();
            conf.addResource(new Path(ConfigManager.getHadoopConfigurationPath()));
            FileSystem fs = FileSystem.get(new URI(hdfsAddress), conf);
            String auxRemotePath = this.remotePath + "/" + name + "/";
            fs.mkdirs(new Path(auxRemotePath));
            uploadFile(fs, checkPath + "/" + name + "/" + name + "-mem.img", auxRemotePath + "memory.img");
            if (new File(checkPath + "/" + name + "/" + name + "-home.img.gz").exists()) {
                long ini = System.currentTimeMillis();
                uploadFile(fs, checkPath + "/" + name + "/" + name + "-home.img.gz", auxRemotePath + "home.img.gz");
                if (compression.get(name) != null) {
                    CompressionStats stats = compression.get(name);
                    stats.uploadTime = System.currentTimeMillis() - ini;
                    int diskspeed = DiskManager.getDiskSpeed();
                    System.out.println("Disk Speed   = " + diskspeed);
                    System.out.println("Total time   = " + stats.getTotalTime() + " = " + stats.compressTime + "+" + stats.uploadTime + " > " + ((stats.srcSize / 1000) / diskspeed) + "+" + (stats.uploadTime / stats.getRatio()));
                    if (stats.getTotalTime() > ((stats.srcSize / 1000) / diskspeed) + (stats.uploadTime / stats.getRatio())) {
                        compression.get(name).compress = false;
                        log.info("Longer compression and uploading than simple uploading -> copying directly");
                    }
                    if (fs.exists(new Path(auxRemotePath + "home.img"))) fs.delete(new Path(auxRemotePath + "home.img"), true);
                }
            } else {
                long time = System.currentTimeMillis();
                uploadFile(fs, checkPath + "/" + name + "/" + name + "-home.img", auxRemotePath + "home.img");
                time = System.currentTimeMillis() - time;
                log.info("Changes file uploading: " + time + "ms");
                if (fs.exists(new Path(auxRemotePath + "home.img.gz"))) fs.delete(new Path(auxRemotePath + "home.img.gz"), true);
            }
            if (wholeCheckpoint) {
                uploadFile(fs, checkPath + "/" + name + "/" + name + "-debianbase.img", auxRemotePath + "debianbase.img");
                if (new File(checkPath + "/" + name + "/" + name + "-breinAplic.img").exists()) uploadFile(fs, checkPath + "/" + name + "/" + name + "-breinAplic.img", auxRemotePath + "breinAplic.img");
            } else if (!historic.contains(name)) {
                uploadFile(fs, poolPath + "/" + name + "/debianbase.img", auxRemotePath + "debianbase.img");
                if (new File(poolPath + "/" + name + "/breinAplic.img").exists()) uploadFile(fs, poolPath + "/" + name + "/breinAplic.img", auxRemotePath + "breinAplic.img");
            }
            if (!historic.contains(name)) {
                uploadFile(fs, poolPath + "/" + name + "/" + name + ".cfg", auxRemotePath + name + ".cfg");
                if (new File(poolPath + "/" + name + "/initramfs.img").exists()) uploadFile(fs, poolPath + "/" + name + "/initramfs.img", auxRemotePath + "initramfs.img");
                if (new File(poolPath + "/" + name + "/vmlinuz").exists()) uploadFile(fs, poolPath + "/" + name + "/vmlinuz", auxRemotePath + "vmlinuz");
                historic.add(name);
            }
            fs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Retrieves disk images of a virtual machine.
	 * @param name
	 * @return If the disks images could be recovered.
	 * @throws IOException
	 */
    private boolean downloadDiskCheckpoint(String name) throws IOException {
        boolean success = false;
        try {
            Configuration conf = new Configuration();
            conf.addResource(ConfigManager.getHadoopConfigurationPath());
            FileSystem fs = FileSystem.get(new URI(hdfsAddress), conf);
            String auxRemotePath = this.remotePath + "/" + name + "/";
            if (fs.exists(new Path(auxRemotePath + "memory.img")) && fs.exists(new Path(auxRemotePath + "debianbase.img")) && (fs.exists(new Path(auxRemotePath + "home.img.gz")) || fs.exists(new Path(auxRemotePath + "home.img"))) && fs.exists(new Path(auxRemotePath + name + ".cfg"))) {
                fs.copyToLocalFile(new Path(auxRemotePath + name + ".cfg"), new Path(poolPath + "/" + name + "/" + name + ".cfg"));
                fs.copyToLocalFile(new Path(auxRemotePath + "memory.img"), new Path(poolPath + "/" + name + "/memory.img"));
                fs.copyToLocalFile(new Path(auxRemotePath + "debianbase.img"), new Path(poolPath + "/" + name + "/debianbase.img"));
                if (fs.exists(new Path(auxRemotePath + "home.img.gz"))) fs.copyToLocalFile(new Path(auxRemotePath + "home.img.gz"), new Path(poolPath + "/" + name + "/home.img.gz")); else fs.copyToLocalFile(new Path(auxRemotePath + "home.img"), new Path(poolPath + "/" + name + "/home.img"));
                if (fs.exists(new Path(auxRemotePath + "vmlinuz"))) fs.copyToLocalFile(new Path(auxRemotePath + "vmlinuz"), new Path(poolPath + "/" + name + "/vmlinuz"));
                if (fs.exists(new Path(auxRemotePath + "initramfs.img"))) fs.copyToLocalFile(new Path(auxRemotePath + "initramfs.img"), new Path(poolPath + "/" + name + "/initramfs.img"));
                if (fs.exists(new Path(auxRemotePath + "breinAplic.img"))) fs.copyToLocalFile(new Path(auxRemotePath + "breinAplic.img"), new Path(poolPath + "/" + name + "/breinAplic.img"));
                success = true;
                historic.add(name);
            } else {
                log.error("Some files on \"" + auxRemotePath + "\" do not exist.");
                log.error("   " + auxRemotePath + "memory.img     " + fs.exists(new Path(auxRemotePath + "memory.img")));
                log.error("   " + auxRemotePath + "debianbase.img " + fs.exists(new Path(auxRemotePath + "debianbase.img")));
                log.error("   " + auxRemotePath + "breinAplic.img " + fs.exists(new Path(auxRemotePath + "breinAplic.img")));
                log.error("   " + auxRemotePath + "initramfs.img  " + fs.exists(new Path(auxRemotePath + "initramfs.img")));
                log.error("   " + auxRemotePath + "vmlinuz        " + fs.exists(new Path(auxRemotePath + "vmlinuz")));
                log.error("   " + auxRemotePath + "home.img.gz    " + fs.exists(new Path(auxRemotePath + "home.img.gz")));
                log.error("   " + auxRemotePath + "home.img       " + fs.exists(new Path(auxRemotePath + "home.img")));
                log.error("   " + auxRemotePath + name + ".cfg      " + fs.exists(new Path(auxRemotePath + name + ".cfg")));
            }
            fs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    /**
	 * Uncompress all the writable disks in order to be uploaded to hadoop.
	 * @param task
	 */
    private void uncompressDiskCheckpoint(String task) {
        try {
            if (new File(poolPath + "/" + task + "/home.img.gz").exists()) Compressor.uncompressFileGz(poolPath + "/" + task + "/home.img.gz", poolPath + "/" + task + "/home.img");
        } catch (IOException e) {
            log.error("Unompressing disks: " + task + ". Error: " + e.getMessage());
        }
    }

    /**
	 * Uploads a file to a file system making an intermediate file.
	 * @param fs File system.
	 * @param src Local source file.
	 * @param dst Destination file name.
	 * @throws IOException
	 */
    private void uploadFile(FileSystem fs, String src, String dst) throws IOException {
        fs.copyFromLocalFile(new Path(src), new Path(dst + ".part"));
        if (fs.exists(new Path(dst))) fs.delete(new Path(dst), true);
        fs.rename(new Path(dst + ".part"), new Path(dst));
    }

    /**
	 * Store compression related information.
	 * @author goirix
	 */
    class CompressionStats {

        long compressTime;

        long uploadTime;

        long srcSize;

        long dstSize;

        boolean compress;

        public float getRatio() {
            return (float) dstSize / (float) srcSize;
        }

        public long getTotalTime() {
            return compressTime + uploadTime;
        }
    }
}
