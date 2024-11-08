package co.edu.unal.ungrid.services.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import co.edu.unal.ungrid.core.Synchronizable;
import co.edu.unal.ungrid.services.client.util.comm.Command;
import co.edu.unal.ungrid.services.proxy.SynchronizeListener.SynchronizeEvent;

public abstract class UploadSyncThread extends SynchronizableThread {

    protected abstract String getTempSavePath();

    protected abstract Command doUpload(final Synchronizable sync);

    public UploadSyncThread(final ProxyServer proxy, final Synchronizable sync) {
        super(proxy, sync);
    }

    public void run() {
        try {
            while (nonStopped()) {
                if (m_proxy.isOnLine()) {
                    if (upload()) {
                        doStop();
                    } else {
                        sleep(10000);
                    }
                } else {
                    setTempPath(getTempSavePath());
                    if (writeTemporal()) {
                        sleep(10000);
                    } else {
                        sleep(5000);
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected boolean upload() {
        boolean r = false;
        m_proxy.notifySyncListeners(SynchronizeEvent.STARTED);
        Synchronizable sync = getData();
        if (sync == null) {
            setData(loadLocalSync());
        }
        sync = getData();
        if (sync != null) {
            if (stopped()) {
                r = true;
            } else {
                Command cmd = doUpload(sync);
                if (cmd.getType() == Command.COM_OK) {
                    r = true;
                    ProxyUtil.log("UploadSyncThread::upload(): file " + sync.getRemotePath() + " has been written");
                } else {
                    ProxyUtil.log("*** UploadSyncThread::upload(): error writing remote file " + sync.getRemotePath());
                }
            }
        }
        if (m_sTempPath != null) {
            deleteTemporal();
            setData(null);
        }
        m_proxy.notifySyncListeners(SynchronizeEvent.FINISHED);
        return r;
    }

    protected boolean writeTemporal() {
        boolean r = true;
        Synchronizable sync = getData();
        if (sync != null) {
            if (saveLocalSync(sync)) {
                setData(null);
                ProxyUtil.log("UploadSyncThread::writeTemporal(): file " + m_sTempPath + " has been written");
            } else {
                r = false;
            }
        }
        return r;
    }

    protected void deleteTemporal() {
        if (m_sTempPath != null) {
            File f = new File(m_sTempPath);
            if (f.canRead()) {
                if (f.delete() == false) {
                    ProxyUtil.log("*** UploadSyncThread::deleteTemporal(): Error deleting temp file " + m_sTempPath);
                } else {
                    ProxyUtil.log("UploadSyncThread::deleteTemporal(): file " + m_sTempPath + " has been deleted");
                }
            }
        }
    }

    @Override
    public void doStop() {
        super.doStop();
        deleteTemporal();
    }

    private boolean saveLocalSync(final Synchronizable sync) {
        assert sync != null;
        boolean r = false;
        if (m_sTempPath != null) {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(m_sTempPath));
                oos.writeObject(sync);
                oos.close();
                r = true;
            } catch (Exception exc) {
                ProxyUtil.log("*** UploadSyncThread::saveLocalSync(): writing temp file '" + m_sTempPath + "' failed: " + exc);
            }
        } else {
            ProxyUtil.log("*** UploadSyncThread::saveLocalSync(): null temp path!");
        }
        return r;
    }

    private Synchronizable loadLocalSync() {
        Synchronizable sync = null;
        if (m_sTempPath != null) {
            sync = ProxyUtil.loadLocalSyncObj(m_sTempPath);
        } else {
            ProxyUtil.log("*** UploadSyncThread::loadLocalSync(): null temp path!");
        }
        return sync;
    }

    protected String getTempPath() {
        return m_sTempPath;
    }

    protected void setTempPath(final String sTempPath) {
        assert sTempPath != null;
        m_sTempPath = sTempPath;
    }

    protected String m_sTempPath;
}
