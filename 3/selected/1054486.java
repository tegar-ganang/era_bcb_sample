package org.openthinclient.pkgmgr.connect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;
import com.levigo.util.preferences.PreferenceStoreHolder;

/**
 * 
 * @author tauschfn
 * 
 */
public class DownloadFiles {

    private static final Logger logger = Logger.getLogger(DownloadFiles.class);

    private final DPKGPackageManager pkgmgr;

    public DownloadFiles(DPKGPackageManager pkgmgr) {
        this.pkgmgr = pkgmgr;
    }

    /**
	 * get an ArrayList and starting the download and MD5sum check for the
	 * different files
	 * 
	 * @param packages
	 * @throws PackageManagerException 
	 * @throws Throwable
	 */
    public boolean downloadAndMD5sumCheck(ArrayList<Package> packages) throws PackageManagerException {
        final String archivesDir = PreferenceStoreHolder.getPreferenceStoreByName("tempPackageManager").getPreferenceAsString("archivesDir", null);
        ;
        final String partialDir = PreferenceStoreHolder.getPreferenceStoreByName("tempPackageManager").getPreferenceAsString("partialDir", null);
        ;
        boolean ret = true;
        for (int i = 0; i < packages.size(); i++) {
            final Package myPackage = packages.get(i);
            final String packageFileName = myPackage.getFilename();
            final String serverPath = myPackage.getServerPath();
            final String[] archiveFile = new FileName().getLocationsForDownload(packageFileName, serverPath, archivesDir);
            final String[] partialFile = new FileName().getLocationsForDownload(packageFileName, serverPath, partialDir);
            final File fileToInstall = new File(partialFile[1]);
            final File alreadyDownloadedFile = new File(archiveFile[1]);
            if (alreadyDownloadedFile.isFile() && alreadyDownloadedFile.renameTo(fileToInstall)) {
                if (!checksum(fileToInstall, myPackage)) ret = false;
            } else try {
                final InputStream in = new ConnectToServer(pkgmgr).getInputStream(partialFile[0]);
                final FileOutputStream out = new FileOutputStream(partialFile[1]);
                final int buflength = 4096;
                final double maxsize = pkgmgr.getMaxVolumeinByte();
                final int maxProgress = new Double(pkgmgr.getMaxProgress() * 0.6).intValue();
                final byte[] buf = new byte[buflength];
                int len;
                int anzahl = 0;
                final int beforeStarting = pkgmgr.getActprogress();
                double leneee = 0;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                    anzahl++;
                    leneee += len;
                    if (anzahl % 25 == 0) pkgmgr.setActprogressPlusX((beforeStarting + new Double(leneee / maxsize * maxProgress).intValue()), new Double(leneee / 1024).intValue(), new Double(myPackage.getSize() / 1024).intValue(), myPackage.getName());
                }
                in.close();
                out.close();
                pkgmgr.setActprogressPlusX((beforeStarting + new Double(leneee / maxsize * maxProgress).intValue()), new Double(leneee / 1024).intValue(), new Double(myPackage.getSize() / 1024).intValue(), myPackage.getName());
                if (!checksum(new File(partialFile[1]), myPackage)) ret = false;
            } catch (final MalformedURLException e) {
                e.printStackTrace();
                String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName("Screen").getPreferenceAsString("DownloadFiles.downloadAndMD5sumCheck.MalformedURL", "No entry found for DownloadFiles.downloadAndMD5sumCheck.MalformedURL");
                logger.error(errorMessage);
                pkgmgr.addWarning(errorMessage);
            } catch (final IOException e) {
                String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName("Screen").getPreferenceAsString("DownloadFiles.downloadAndMD5sumCheck.IOException", "No entry found for DownloadFiles.downloadAndMD5sumCheck.IOException");
                e.printStackTrace();
                logger.error(errorMessage);
                pkgmgr.addWarning(errorMessage);
            }
        }
        return ret;
    }

    public boolean checksum(File file, Package myPackage) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            final FileInputStream in = new FileInputStream(file);
            int len;
            final byte[] buf = new byte[4096];
            while ((len = in.read(buf)) > 0) md.update(buf, 0, len);
            in.close();
            final byte[] fileMD5sum = md.digest();
            md.reset();
            if (myPackage.getMD5sum().equalsIgnoreCase(byteArrayToHexString(fileMD5sum))) {
                File parentFile = file.getParentFile();
                if (parentFile != null && parentFile.exists()) parentFile = parentFile.getParentFile();
                final String testFileName = parentFile.getPath() + File.separator + file.getName();
                if (!file.renameTo(new File(testFileName))) {
                    String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName("Screen").getPreferenceAsString("DownloadFiles.checksum.md5different", "No entry found for DownloadFiles.checksum.md5different");
                    logger.error(errorMessage);
                    pkgmgr.addWarning(errorMessage);
                    return false;
                }
            } else {
                String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName("Screen").getPreferenceAsString("DownloadFiles.checksum.md5different", "No entry found for DownloadFiles.checksum.md5different");
                logger.error(errorMessage);
                pkgmgr.addWarning(errorMessage);
                file.delete();
                return false;
            }
        } catch (final NoSuchAlgorithmException e1) {
            String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName("Screen").getPreferenceAsString("DownloadFiles.checksum.NoSuchAlgorithmException", "No entry found for DownloadFiles.checksum.NoSuchAlgorithmException");
            e1.printStackTrace();
            logger.error(errorMessage);
            pkgmgr.addWarning(errorMessage);
        } catch (final IOException e) {
            String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName("Screen").getPreferenceAsString("DownloadFiles.checksum.IOException", "No entry found for DownloadFiles.checksum.IOException");
            e.printStackTrace();
            logger.error(errorMessage);
            pkgmgr.addWarning(errorMessage);
        }
        return true;
    }

    public static String byteArrayToHexString(byte[] b) {
        final StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            final int v = b[i] & 0xff;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }
}
