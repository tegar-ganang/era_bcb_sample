package net.tourbook.srtm.download;

import java.io.File;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.srtm.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPMessageCollector;
import com.enterprisedt.net.ftp.FTPProgressMonitor;
import com.enterprisedt.net.ftp.FTPTransferType;
import de.byteholder.geoclipse.map.event.TileEventId;
import de.byteholder.geoclipse.tileinfo.TileInfoManager;

public final class FTPDownloader {

    private String host;

    private String user;

    private String password;

    public FTPDownloader(final String user, final String password) {
        this.user = user;
        this.password = password;
    }

    public void get(final String remoteFilePath, final String remoteFileName, final String localName) {
        final FTPClient ftp = new FTPClient();
        final FTPMessageCollector listener = new FTPMessageCollector();
        try {
            final String localDirName = localName.substring(0, localName.lastIndexOf(File.separator));
            System.out.println("ftp:");
            System.out.println("   remoteDir " + remoteFilePath);
            System.out.println("   localDir " + localDirName);
            final File localDir = new File(localDirName);
            if (!localDir.exists()) {
                System.out.println("   create Dir " + localDirName);
                localDir.mkdir();
            }
            ftp.setTimeout(10000);
            ftp.setRemoteHost(host);
            ftp.setMessageListener(listener);
        } catch (final UnknownHostException e) {
            showConnectError();
            return;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        final TileInfoManager tileInfoMgr = TileInfoManager.getInstance();
        final Job downloadJob = new Job(Messages.job_name_ftpDownload) {

            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                try {
                    showTileInfo(remoteFileName, -1);
                    System.out.println("   connect " + host);
                    ftp.connect();
                    showTileInfo(remoteFileName, -2);
                    System.out.println("   login " + user + " " + password);
                    ftp.login(user, password);
                    System.out.println("   set passive mode");
                    ftp.setConnectMode(FTPConnectMode.PASV);
                    System.out.println("   set type binary");
                    ftp.setType(FTPTransferType.BINARY);
                    showTileInfo(remoteFileName, -3);
                    System.out.println("   chdir " + remoteFilePath);
                    ftp.chdir(remoteFilePath);
                    ftp.setProgressMonitor(new FTPProgressMonitor() {

                        public void bytesTransferred(final long count) {
                            tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_LOADING_MONITOR, remoteFileName, count);
                        }
                    });
                    showTileInfo(remoteFileName, -4);
                    System.out.println("   get " + remoteFileName + " -> " + localName + " ...");
                    ftp.get(localName, remoteFileName);
                    System.out.println("   quit");
                    ftp.quit();
                } catch (final UnknownHostException e) {
                    return new Status(IStatus.ERROR, TourbookPlugin.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.error_message_cannotConnectToServer, host), e);
                } catch (final SocketTimeoutException e) {
                    return new Status(IStatus.ERROR, TourbookPlugin.PLUGIN_ID, IStatus.ERROR, NLS.bind(Messages.error_message_timeoutWhenConnectingToServer, host), e);
                } catch (final Exception e) {
                    e.printStackTrace();
                    tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_ERROR_LOADING, remoteFileName, 0);
                } finally {
                    tileInfoMgr.updateSRTMTileInfo(TileEventId.SRTM_DATA_END_LOADING, remoteFileName, 0);
                }
                return Status.OK_STATUS;
            }
        };
        downloadJob.schedule();
        try {
            downloadJob.join();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setHost(final String host) {
        this.host = host;
    }

    private void showConnectError() {
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.srtm_transfer_error_title, NLS.bind(Messages.srtm_transfer_error_message, host));
            }
        });
    }

    private void showTileInfo(final String remoteName, final int status) {
        TileInfoManager.getInstance().updateSRTMTileInfo(TileEventId.SRTM_DATA_START_LOADING, remoteName, status);
    }
}
