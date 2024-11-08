package net.tourbook.srtm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import net.tourbook.srtm.download.DownloadETOPO;
import net.tourbook.srtm.download.DownloadGLOBE;
import net.tourbook.srtm.download.DownloadSRTM3;

public class ElevationFile {

    private FileChannel fileChannel;

    private ShortBuffer shortBuffer;

    private boolean _exists = false;

    private boolean _isLocalFileError = false;

    public ElevationFile(final String fileName, final int elevationTyp) throws Exception {
        switch(elevationTyp) {
            case Constants.ELEVATION_TYPE_ETOPO:
                initETOPO(fileName);
                break;
            case Constants.ELEVATION_TYPE_GLOBE:
                initGLOBE(fileName);
                break;
            case Constants.ELEVATION_TYPE_SRTM3:
                initSRTM3(fileName);
                break;
            case Constants.ELEVATION_TYPE_SRTM1:
                initSRTM1(fileName);
                break;
        }
    }

    public void close() {
        if (fileChannel == null) {
            return;
        }
        try {
            fileChannel.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public short get(final int index) {
        if (!_exists) {
            return (-32767);
        }
        return shortBuffer.get(index);
    }

    private void handleError(final String fileName, final Exception e) {
        System.out.println("handleError: " + fileName + ": " + e.getMessage());
        if (e instanceof FileNotFoundException) {
        } else {
            e.printStackTrace();
        }
        _exists = false;
    }

    private void initETOPO(final String fileName) throws Exception {
        try {
            open(fileName);
        } catch (final FileNotFoundException e1) {
            try {
                final String localName = fileName;
                final String remoteName = localName.substring(localName.lastIndexOf(File.separator) + 1);
                DownloadETOPO.get(remoteName, localName);
                open(fileName);
            } catch (final Exception e2) {
                handleError(fileName, e2);
            }
        } catch (final Exception e1) {
            handleError(fileName, e1);
        }
    }

    private void initGLOBE(final String fileName) throws Exception {
        try {
            open(fileName);
        } catch (final FileNotFoundException e1) {
            try {
                final String localZipName = fileName + ".gz";
                final String remoteFileName = localZipName.substring(localZipName.lastIndexOf(File.separator) + 1);
                DownloadGLOBE.get(remoteFileName, localZipName);
                FileZip.gunzip(localZipName);
                final File zipArchive = new File(localZipName);
                zipArchive.delete();
                open(fileName);
            } catch (final Exception e2) {
                handleError(fileName, e2);
            }
        } catch (final Exception e1) {
            handleError(fileName, e1);
        }
    }

    private void initSRTM1(final String fileName) throws Exception {
        try {
            open(fileName);
        } catch (final Exception e) {
            handleError(fileName, e);
        }
    }

    private void initSRTM3(final String fileName) throws Exception {
        if (_isLocalFileError) {
            return;
        }
        try {
            open(fileName);
        } catch (final FileNotFoundException e1) {
            try {
                final String localZipName = fileName + ".zip";
                final File localFile = new File(localZipName);
                if (localFile.exists() && localFile.length() == 0) {
                    _isLocalFileError = true;
                    localFile.delete();
                    throw new Exception("local file is empty");
                }
                final String remoteFileName = localZipName.substring(localZipName.lastIndexOf(File.separator) + 1);
                DownloadSRTM3.get(remoteFileName, localZipName);
                FileZip.unzip(localZipName);
                final File zipArchive = new File(localZipName);
                zipArchive.delete();
                open(fileName);
            } catch (final Exception e2) {
                handleError(fileName, e2);
            }
        } catch (final Exception e1) {
            handleError(fileName, e1);
        }
    }

    private void open(final String fileName) throws Exception {
        try {
            fileChannel = new FileInputStream(new File(fileName)).getChannel();
            shortBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).asShortBuffer();
        } catch (final Exception e) {
            System.out.println(e.getMessage());
            throw (e);
        }
        System.out.println("open " + fileName);
        _exists = true;
    }
}
