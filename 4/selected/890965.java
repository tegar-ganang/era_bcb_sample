package co.edu.unal.ungrid.services.proxy;

import java.io.File;
import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Map;
import co.edu.unal.ungrid.core.DirUtil;
import co.edu.unal.ungrid.core.Synchronizable;
import co.edu.unal.ungrid.image.AbstractImage;
import co.edu.unal.ungrid.image.ByteImage;
import co.edu.unal.ungrid.image.GrayIntImage;
import co.edu.unal.ungrid.image.RgbImage;
import co.edu.unal.ungrid.image.util.ImageUtil;
import co.edu.unal.ungrid.services.client.applet.document.AbstractDocument;
import co.edu.unal.ungrid.services.client.applet.document.AbstractRemoteJob;
import co.edu.unal.ungrid.services.client.applet.document.DocumentEventListener.DocumentEvent;
import co.edu.unal.ungrid.services.client.service.ServiceApp;
import co.edu.unal.ungrid.services.client.service.ServiceFactory;
import co.edu.unal.ungrid.services.client.util.BaseDlg;
import co.edu.unal.ungrid.services.client.util.comm.AppletServerHelper;
import co.edu.unal.ungrid.services.client.util.comm.Command;
import co.edu.unal.ungrid.services.client.util.comm.CommandHelperThread;
import co.edu.unal.ungrid.services.client.util.comm.CommandListener;
import co.edu.unal.ungrid.services.proxy.ProxyEventListener.ProxyEvent;
import co.edu.unal.ungrid.services.proxy.SynchronizeListener.SynchronizeEvent;
import co.edu.unal.ungrid.services.server.util.JobListener.JobEvent;

public class ProxyServer implements CommandListener {

    private static enum SyncType {

        SYNC_OUT_DOC, SYNC_OUT_IMG, LOG_OUT, SYNC_IN_DOC, SYNC_IN_IMG
    }

    private static class SyncClass {

        public SyncClass(final SyncType syncType, final Synchronizable syncData) {
            this.syncType = syncType;
            this.syncData = syncData;
        }

        SyncType syncType;

        Synchronizable syncData;
    }

    private ProxyServer() {
        m_proxyListeners = new ArrayList<ProxyEventListener>();
        m_syncListeners = new ArrayList<SynchronizeListener>();
        m_syncOut = new ArrayList<SyncClass>();
        m_syncIn = new ArrayList<SyncClass>();
        m_bNetOnLine = false;
        m_bUsrOnline = true;
    }

    public static synchronized ProxyServer getInstance() {
        if (m_this == null) {
            m_this = new ProxyServer();
        }
        return m_this;
    }

    public void init() {
        ServiceApp app = ServiceFactory.getInstance();
        ProxyUtil.log("ProxyServer::init(): creating AppSvrHelper ...");
        m_svrHelper = new AppletServerHelper(app.getAppServletUrl());
        ProxyUtil.log("ProxyServer::init(): AppSvrHelper is valid : " + m_svrHelper.isValid());
        ProxyUtil.log("ProxyServer::initCommandHelper(): creating CommandHelper thread ...");
        String sUsrId = CommandHelperThread.createUserId(app.getUserAccount(), app.getUserSession());
        m_cmdHelper = new CommandHelperThread(app.getCmdServletUrl(), sUsrId, CommandHelperThread.DEF_SLEEP_TIME);
        ProxyUtil.log("ProxyServer::init(): CommandHelper is valid : " + m_cmdHelper.isValid());
        createSyncDirs();
        addCommandListener(this);
    }

    private void createSyncDirs() {
        String sUsrDir = ServiceFactory.getInstance().getUserHome();
        if (sUsrDir != null) {
            m_fSync = new File(sUsrDir + File.separator + USR_SYNC_DIR);
            if (m_fSync.isDirectory() == false) {
                if (DirUtil.createDirIfNeeded(m_fSync.getAbsolutePath())) {
                    ProxyUtil.log("ProxyServer::createSyncDir(): created sync directory " + m_fSync.getAbsolutePath());
                } else {
                    m_fSync = new File(sUsrDir);
                    ProxyUtil.log("*** ProxyServer::createSyncDir(): failed to create sync directory " + m_fSync.getAbsolutePath());
                }
            } else {
                ProxyUtil.log("ProxyServer::createSyncDir(): using sync directory " + m_fSync.getAbsolutePath());
            }
        } else {
            m_fSync = new File(".");
            ProxyUtil.log("*** ProxyServer::createSyncDir(): can't determine user home directory, using " + m_fSync.getAbsolutePath());
        }
        File fDoc = new File(getDocsTmpDir());
        if (fDoc.isDirectory() == false) {
            if (DirUtil.createDirIfNeeded(fDoc.getAbsolutePath())) {
                ProxyUtil.log("ProxyServer::createSyncDir(): created sync docs directory " + fDoc.getAbsolutePath());
            } else {
                ProxyUtil.log("*** ProxyServer::createSyncDir(): failed to create sync docs directory " + fDoc.getAbsolutePath());
            }
        }
        File fImg = new File(getImgsTmpDir());
        if (fImg.isDirectory() == false) {
            if (DirUtil.createDirIfNeeded(fImg.getAbsolutePath())) {
                ProxyUtil.log("ProxyServer::createSyncDir(): created sync imgs directory " + fImg.getAbsolutePath());
            } else {
                ProxyUtil.log("*** ProxyServer::createSyncDir(): failed to create sync imgs directory " + fImg.getAbsolutePath());
            }
        }
    }

    public void start() {
        if (m_cmdHelper.isValid()) {
            ProxyUtil.log("ProxyServer::start(): starting CommandHelperThread ...");
            m_cmdHelper.start();
            setOnLine(false);
            notifyProxyListeners(ProxyEvent.NET_LOST);
        } else {
            ServiceFactory.getInstance().showErrorDlg("ProxyServer::start(): wrong config for CommandHelperThread");
        }
    }

    public void servletHandshake() {
        Object obj = m_svrHelper.servletHandshake(ServiceFactory.getInstance().getUserAccount());
        if (obj instanceof String) {
            ServiceFactory.getInstance().setHandshakeMsg((String) obj);
        }
    }

    public void addCommandListener(final CommandListener cl) {
        if (cl != null) m_cmdHelper.addListener(cl);
    }

    public void saveTmpDoc(final AbstractDocument doc) {
        assert doc != null;
        String sOrig = doc.getLocalPath();
        doc.setLocalPath(ProxyUtil.getUserDocsDir() + File.separator + TMP_DOC_FN);
        doc.setRemotePath(TMP_DOC_FN);
        ProxyUtil.log("ProxyServer::saveTmpDoc(): saving local doc " + doc.getLocalPath());
        ProxyUtil.saveLocalSyncObj(new SyncResource(doc.getLocalPath(), doc.getRemotePath(), doc));
        doc.setLocalPath(sOrig);
    }

    public boolean logLogOut() {
        return m_svrHelper.logLogOut(ServiceFactory.getInstance().getUserAccount());
    }

    public String selectDocName(final String sTitle, final String sDir) {
        if (isOnLine()) {
            if (m_syncThread != null) {
                m_syncThread.setPause(true);
            }
            ServiceApp app = ServiceFactory.getInstance();
            ArrayList<String> lst = m_svrHelper.listDocs(app.getUserAccount(), app.getDocType());
            if (lst != null && lst.size() > 0) {
                replicateRemoteDocNames(lst);
            }
        }
        String fn = ProxyUtil.showOpenDocFileChooser(sTitle);
        if (isOnLine()) {
            syncDownload();
            if (m_syncThread != null) {
                m_syncThread.setPause(false);
            }
        }
        return fn;
    }

    public int saveDocumentAs(final AbstractDocument doc) {
        int r = BaseDlg.OK;
        String sFileName = ProxyUtil.showSaveDocFileChooser(ProxyUtil.getUserDocsDir(), doc.getLocalPath());
        if (sFileName == null) {
            r = BaseDlg.CANCEL;
        } else {
            r = saveDocument(doc, sFileName);
        }
        return r;
    }

    public int saveDocument(final AbstractDocument doc, String sFileName) {
        assert doc != null;
        int r = BaseDlg.OK;
        final ServiceApp app = ServiceFactory.getInstance();
        String sCurName = doc.getLocalPath();
        if (sCurName.equals(sFileName) == false) {
            File f = new File(sFileName);
            if (f.exists()) {
                r = app.readUsrYesNoCancel(sFileName + " already exists.\n\nOverwrite existing file?");
                if (r == BaseDlg.CANCEL) {
                } else if (r == BaseDlg.NO) {
                    sFileName = ProxyUtil.showSaveDocFileChooser(ProxyUtil.getUserDocsDir(), sFileName);
                    if (sFileName != null) {
                        r = BaseDlg.OK;
                    } else {
                    }
                } else {
                    r = BaseDlg.OK;
                }
            }
        }
        if (r == BaseDlg.OK) {
            doc.setLocalPath(sFileName);
            doc.setRemotePath(ProxyUtil.getRemotePath(sFileName));
            app.setWaitCursor();
            r = ProxyUtil.saveLocalSyncObj(new SyncResource(doc.getLocalPath(), doc.getRemotePath(), doc));
            if (r == BaseDlg.OK) {
                doc.setModified(false);
            }
            app.setDefCursor();
            pushToUpload(new SyncClass(SyncType.SYNC_OUT_DOC, doc));
            syncUpload();
        }
        return r;
    }

    public AbstractDocument loadDocument(final String sFilePath) {
        File f = new File(sFilePath);
        if (f.length() == 0) {
            String sFileName = sFilePath;
            int n = sFilePath.lastIndexOf(File.separatorChar);
            if (n > 0) {
                sFileName = sFilePath.substring(n + 1);
            }
            AbstractDocument doc = loadServerDoc(sFileName);
            if (doc != null) {
                doc.setLocalPath(sFilePath);
                doc.setRemotePath(ProxyUtil.getRemotePath(sFilePath));
                ProxyUtil.saveLocalSyncObj(new SyncResource(doc.getLocalPath(), doc.getRemotePath(), doc));
            }
        }
        return ProxyUtil.loadLocalDoc(sFilePath);
    }

    public File selectOpenImageName(final String sTitle) {
        if (isOnLine()) {
            if (m_syncThread != null) {
                m_syncThread.setPause(true);
            }
            final ServiceApp app = ServiceFactory.getInstance();
            ArrayList<File> lst = m_svrHelper.listImages(app.getDocString().toLowerCase(), app.getUserAccount());
            if (lst != null && lst.size() > 0) {
                replicateRemoteImageNames(lst);
            }
        }
        File f = ProxyUtil.showOpenImageFileChooser(ProxyUtil.getUserImgsDir(), sTitle);
        if (isOnLine()) {
            syncDownload();
            if (m_syncThread != null) {
                m_syncThread.setPause(false);
            }
        }
        return f;
    }

    public void stop() {
        stopCommandHelper();
        stopThread(m_uploadDocObjThread);
        stopThread(m_uploadImgObjThread);
        stopThread(m_syncThread);
    }

    public String selectImageNameSave(final String sTitle, final String sCurDir) {
        if (m_sLastSaveImgName == null) {
            m_sLastSaveImgName = "";
        }
        String sFileName = ProxyUtil.selectImageNameSave(sTitle, sCurDir, m_sLastSaveImgName);
        if (sFileName != null) {
            m_sLastSaveImgName = sFileName;
        }
        return sFileName;
    }

    public int saveImage(final AbstractImage<?> img) {
        assert img != null;
        assert img.getLocalPath() != null;
        img.setRemotePath(ProxyUtil.getRemotePath(img.getLocalPath()));
        ServiceFactory.getInstance().setWaitCursor();
        int r = ProxyUtil.saveLocalSyncObj(new SyncResource(img.getLocalPath(), img.getRemotePath(), img));
        ServiceFactory.getInstance().setDefCursor();
        if (r == BaseDlg.OK) {
            pushToUpload(new SyncClass(SyncType.SYNC_OUT_IMG, img));
            syncUpload();
        }
        return r;
    }

    public RgbImage loadRgbImage(final String sFileName) {
        boolean b = downloadImageIf(sFileName, ProxyUtil.getRemoteURL(sFileName));
        return (b ? ImageUtil.loadAsRgbImage(sFileName) : null);
    }

    public GrayIntImage loadGrayImage(final String sFileName) {
        RgbImage rgb = loadRgbImage(sFileName);
        return (rgb != null ? ImageUtil.rgbToGrayScale(rgb) : null);
    }

    public ByteImage loadByteImage(final String sFileName) {
        boolean b = downloadImageIf(sFileName, ProxyUtil.getRemoteURL(sFileName));
        return (b ? ImageUtil.loadAsByteImage(sFileName) : null);
    }

    public ArrayList<GrayIntImage> loadTestImages(final String sService) {
        ArrayList<GrayIntImage> lst = m_svrHelper.loadTestImages(sService);
        return lst;
    }

    public boolean isUsrOnLine() {
        return m_bUsrOnline;
    }

    public void toggleOnLine() {
        setUsrOnLine(!isUsrOnLine());
        notifyProxyListeners(isUsrOnLine() ? ProxyEvent.USER_ON_LINE : ProxyEvent.USER_OFF_LINE);
    }

    public void addProxyListener(final ProxyEventListener pel) {
        assert pel != null;
        if (!m_proxyListeners.contains(pel)) {
            m_proxyListeners.add(pel);
        }
    }

    public void removeProxyListener(final ProxyEventListener pel) {
        assert pel != null;
        m_proxyListeners.remove(pel);
    }

    protected void notifyProxyListeners(final ProxyEvent pe) {
        for (final ProxyEventListener pel : m_proxyListeners) {
            pel.processProxyEvent(pe);
        }
    }

    public void addSyncListener(final SynchronizeListener sl) {
        assert sl != null;
        if (!m_syncListeners.contains(sl)) {
            m_syncListeners.add(sl);
        }
    }

    public void removeSyncListener(final SynchronizeListener sl) {
        assert sl != null;
        m_proxyListeners.remove(sl);
    }

    protected void notifySyncListeners(final SynchronizeEvent se) {
        for (final SynchronizeListener sl : m_syncListeners) {
            sl.processSynchronizeEvent(se);
        }
    }

    public void submit(final AbstractRemoteJob job) {
        assert job != null;
        assert job.isValid();
        Command res = null;
        if (job != null) {
            if (isOnLine() && isUsrOnLine()) {
                res = sendJobToServer(job);
            } else {
                job.addJobListener(ServiceFactory.getInstance());
                res = job.execute(AbstractRemoteJob.EXEC_LOCAL);
            }
        }
        m_cmdHelper.notifyListeners(res);
    }

    public void processSvrCmd(final Command cmd) {
        if (cmd != null) {
            switch(cmd.getType()) {
                case Command.COM_ERR:
                    {
                    }
                    break;
                case Command.COM_LST:
                case Command.LOG_OUT:
                    {
                        setOnLine(false);
                        stopSynchronize();
                    }
                    break;
                case Command.COM_OK:
                case Command.COM_REC:
                    {
                        if (!isOnLine()) {
                            setOnLine(true);
                            startSynchronize();
                        }
                    }
                    break;
                case Command.EXE_JOB:
                    {
                        Serializable data = cmd.getData();
                        if (data instanceof JobEvent) {
                            JobEvent je = (JobEvent) data;
                            switch(je) {
                                case SUBMITTED:
                                    ServiceFactory.getInstance().notifyDocumentListeners(DocumentEvent.EXEC_SUBMITTED, null);
                                    break;
                                case NO_CONFIG:
                                case BAD_CONFIG:
                                case BAD_IMAGE:
                                case NO_EXECUTOR:
                                case BAD_RESULT:
                                case CANCELLED:
                                case FAILED:
                                    ServiceFactory.getInstance().notifyDocumentListeners(DocumentEvent.EXEC_FAILED, null);
                                    break;
                                case STARTED:
                                    ServiceFactory.getInstance().notifyDocumentListeners(DocumentEvent.EXEC_STARTED, null);
                                    break;
                                case EXECUTED:
                                    ServiceFactory.getInstance().notifyDocumentListeners(DocumentEvent.EXEC_OK, null);
                                    break;
                            }
                        }
                    }
                    break;
                default:
                    {
                    }
            }
        }
    }

    public synchronized boolean isOnLine() {
        return m_bNetOnLine;
    }

    protected String getTmpDocSavePath() {
        return getTmpDocFileName(DOC_TMP_FN);
    }

    protected String getTmpImgSavePath() {
        return getTmpImgFileName(IMG_TMP_FN);
    }

    protected Command uploadFile(final AbstractDocument doc) {
        assert doc != null;
        assert doc.getRemotePath() != null;
        return m_svrHelper.saveFile(ServiceFactory.getInstance().getUserAccount(), doc);
    }

    protected Command uploadImageObj(final AbstractImage<?> img) {
        assert img != null;
        assert img.getRemotePath() != null;
        ServiceApp app = ServiceFactory.getInstance();
        return m_svrHelper.putImageObj(app.getUserAccount(), app.getDocString().toLowerCase(), img);
    }

    protected Command uploadImageFile(final SyncResource sr) {
        assert sr != null;
        assert sr.getRemotePath() != null;
        assert sr.getData() instanceof byte[];
        ServiceApp app = ServiceFactory.getInstance();
        return m_svrHelper.putImageFile(app.getUserAccount(), app.getDocString().toLowerCase(), sr.getRemotePath(), sr.getData());
    }

    protected ArrayList<File> listServerFiles() {
        ServiceApp app = ServiceFactory.getInstance();
        return m_svrHelper.listImages(app.getDocString().toLowerCase(), app.getUserAccount());
    }

    protected boolean serverFileExists(final String sRemotePath) {
        assert sRemotePath != null;
        ServiceApp app = ServiceFactory.getInstance();
        return m_svrHelper.fileExists(app.getDocString().toLowerCase(), app.getUserAccount(), sRemotePath);
    }

    protected SyncResource downloadDoc(final Synchronizable pair) {
        ServiceApp app = ServiceFactory.getInstance();
        AbstractDocument doc = m_svrHelper.getDocument(app.getUserAccount(), pair.getRemotePath(), app.getDocType());
        return (doc != null ? new SyncResource(pair.getLocalPath(), pair.getRemotePath(), doc) : null);
    }

    protected SyncResource downloadImg(final Synchronizable pair) {
        ServiceApp app = ServiceFactory.getInstance();
        byte[] ba = m_svrHelper.getImage(app.getUserAccount(), pair.getRemotePath());
        return (ba != null ? new SyncResource(pair.getLocalPath(), pair.getRemotePath(), ba) : null);
    }

    private void stopThread(final StoppableDaemonThread t) {
        if (t != null) {
            t.doStop();
        }
    }

    private String getDocsTmpDir() {
        return (m_fSync.getAbsolutePath() + File.separator + DOC_SYNC_DIR);
    }

    private String getImgsTmpDir() {
        return (m_fSync.getAbsolutePath() + File.separator + IMG_SYNC_DIR);
    }

    private String getTmpDocFileName(final String sFileName) {
        return (getDocsTmpDir() + File.separator + sFileName + "-" + ProxyUtil.getDateAndTime());
    }

    private String getTmpImgFileName(final String sFileName) {
        return (getImgsTmpDir() + File.separator + sFileName + "-" + ProxyUtil.getDateAndTime());
    }

    private void replicateRemoteDocNames(final ArrayList<String> lst) {
        String sBaseDir = ProxyUtil.getUserDocsDir();
        for (String str : lst) {
            replicateFileName(SyncType.SYNC_IN_DOC, sBaseDir, str);
        }
    }

    private void stopCommandHelper() {
        m_cmdHelper.doStop();
    }

    private void startUploadDocThread(final AbstractDocument doc) {
        stopThread(m_uploadDocObjThread);
        m_uploadDocObjThread = new UploadDocObjThread(this, doc);
        m_uploadDocObjThread.start();
    }

    private void startUploadImgThread(final AbstractImage<?> img) {
        stopThread(m_uploadImgObjThread);
        m_uploadImgObjThread = new UploadImgObjThread(this, img);
        m_uploadImgObjThread.start();
    }

    private void syncUpload() {
        SyncClass sc = popToUpload();
        if (sc != null) {
            switch(sc.syncType) {
                case SYNC_OUT_DOC:
                    {
                        assert (sc.syncData instanceof AbstractDocument);
                        startUploadDocThread((AbstractDocument) sc.syncData);
                    }
                    break;
                case SYNC_OUT_IMG:
                    {
                        assert (sc.syncData instanceof AbstractImage);
                        startUploadImgThread((AbstractImage<?>) sc.syncData);
                    }
                    break;
                case LOG_OUT:
                    {
                    }
                    break;
            }
        }
    }

    private void startDownloadDocThread(final Synchronizable doc) {
        assert (doc != null);
        DownloadSyncThread t = new DownloadDocThread(this, doc);
        t.start();
    }

    private void startDownloadImgThread(final Synchronizable img) {
        assert (img != null);
        DownloadSyncThread t = new DownloadImgThread(this, img);
        t.start();
    }

    private void syncDownload() {
        SyncClass sc = popToDownload();
        if (sc != null) {
            switch(sc.syncType) {
                case SYNC_IN_DOC:
                    startDownloadDocThread(sc.syncData);
                    break;
                case SYNC_IN_IMG:
                    startDownloadImgThread(sc.syncData);
                    break;
            }
        }
    }

    private AbstractDocument loadServerDoc(final String sFileName) {
        final ServiceApp app = ServiceFactory.getInstance();
        AbstractDocument doc = m_svrHelper.getDocument(app.getUserAccount(), sFileName, app.getDocType());
        if (doc == null) {
            ProxyUtil.log("*** ProxyServer::loadServerDoc(): error reading remote file " + sFileName);
            app.showErrorDlg("Error opening remote " + app.getDocString().toLowerCase() + ":\n\n" + sFileName);
        }
        return doc;
    }

    private void replicateDocFile(final String sBaseDir, final String sFileName) {
        File f = new File(sBaseDir + File.separator + sFileName);
        if (f.exists()) {
        } else {
            if (ProxyUtil.createEmptyFile(f.getAbsolutePath())) {
                pushToDownload(new SyncClass(SyncType.SYNC_IN_DOC, new SyncResource(f.getAbsolutePath(), sFileName)));
            }
        }
    }

    private void replicateImgFile(final String sBaseDir, final String sFileName) {
        String sAccName = ServiceFactory.getInstance().getUserAccount();
        int n = sFileName.indexOf(sAccName);
        if (n > 0) {
            n += sAccName.length() + 1;
            File f = new File(sBaseDir + File.separator + sFileName.substring(n));
            if (f.exists()) {
            } else {
                if (ProxyUtil.createEmptyFile(f.getAbsolutePath())) {
                    pushToDownload(new SyncClass(SyncType.SYNC_IN_IMG, new SyncResource(f.getAbsolutePath(), sFileName)));
                }
            }
        }
    }

    private void replicateFileName(final SyncType st, final String sBaseDir, final String sFileName) {
        switch(st) {
            case SYNC_IN_DOC:
                replicateDocFile(sBaseDir, sFileName);
                break;
            case SYNC_IN_IMG:
                replicateImgFile(sBaseDir, sFileName);
                break;
        }
    }

    private void replicateRemoteImageNames(final ArrayList<File> lst) {
        String sBaseDir = ProxyUtil.getUserImgsDir();
        for (File f : lst) {
            if (f.isDirectory()) {
                ProxyUtil.replicateDirectory(sBaseDir, f);
            }
        }
        for (File f : lst) {
            if (f.isFile()) {
                replicateFileName(SyncType.SYNC_IN_IMG, sBaseDir, f.getAbsolutePath());
            }
        }
    }

    private boolean downloadImageIf(final String sFileLocal, final String sFileRemote) {
        boolean b = true;
        File f = new File(sFileLocal);
        if (f.length() == 0) {
            b = ProxyUtil.downloadImage(sFileLocal, sFileRemote);
        }
        return b;
    }

    protected ArrayList<File> docsToSyncOut() {
        return ProxyUtil.scanDir(getDocsTmpDir());
    }

    protected ArrayList<File> imgsToSyncOut() {
        return ProxyUtil.scanDir(getImgsTmpDir());
    }

    protected ArrayList<File> docsToSyncIn() {
        ArrayList<File> fLst = null;
        ServiceApp app = ServiceFactory.getInstance();
        Map<String, Serializable> map = m_svrHelper.docsToSync(app.getUserAccount(), app.getDocType());
        if (map != null && map.size() > 0) {
            fLst = new ArrayList<File>();
            for (String sName : map.keySet()) {
                File f = new File(ProxyUtil.getUserDocsDir() + File.separator + sName);
                if (f.exists()) {
                    Serializable date = map.get(sName);
                    if (date instanceof Date) {
                        long lclTime = f.lastModified();
                        long svrTime = ((Date) date).getTime();
                        if (svrTime > lclTime) {
                            fLst.add(f);
                        }
                    }
                } else {
                    fLst.add(f);
                }
            }
        }
        return fLst;
    }

    protected ArrayList<File> imgsToSyncIn() {
        ArrayList<File> lclLst = null;
        ServiceApp app = ServiceFactory.getInstance();
        ArrayList<File> svrLst = m_svrHelper.imgsToSync(app.getDocString().toLowerCase(), app.getUserAccount());
        if (svrLst != null && svrLst.size() > 0) {
            lclLst = new ArrayList<File>();
            for (File fSvr : svrLst) {
                if (fSvr.isDirectory()) {
                    File fLcl = new File(ProxyUtil.getUserImgsDir() + File.separator + ProxyUtil.getLocalPath(fSvr.getAbsolutePath()));
                    if (fLcl.exists()) {
                    } else {
                        if (DirUtil.createDirIfNeeded(fLcl.getAbsolutePath()) == false) {
                            ProxyUtil.log("*** ProxyServer::imgsToSyncIn(): error creating local dir " + fLcl.getAbsolutePath());
                        }
                    }
                } else {
                    File fLcl = new File(ProxyUtil.getUserImgsDir() + File.separator + ProxyUtil.getLocalPath(fSvr.getAbsolutePath()));
                    if (fLcl.exists()) {
                        long srcTime = fSvr.lastModified();
                        long dstTime = fLcl.lastModified();
                        if (srcTime > dstTime) {
                            lclLst.add(fSvr);
                        }
                    } else {
                        lclLst.add(fSvr);
                    }
                }
            }
        }
        return lclLst;
    }

    private void startSynchronize() {
        if (m_syncThread == null) {
            m_syncThread = new SynchronizingThread(this);
            m_syncThread.start();
        }
    }

    private void stopSynchronize() {
        if (m_syncThread != null) {
            m_syncThread.doStop();
            m_syncThread = null;
        }
    }

    private Command sendJobToServer(final AbstractRemoteJob job) {
        assert job != null;
        assert job.isValid();
        assert isOnLine();
        return m_cmdHelper.exec(new Command(ServiceFactory.getInstance().getUserAccount(), Command.EXE_JOB, job));
    }

    private synchronized void setOnLine(boolean bNewStatus) {
        if (bNewStatus != m_bNetOnLine) {
            m_bNetOnLine = bNewStatus;
            notifyProxyListeners(m_bNetOnLine ? ProxyEvent.NET_RECOVERED : ProxyEvent.NET_LOST);
            ProxyUtil.log("ProxyServer::setOnLine(): *** GOING " + (m_bNetOnLine ? "ON-LINE" : "OFF-LINE"));
        }
    }

    private void setUsrOnLine(boolean usrOnline) {
        m_bUsrOnline = usrOnline;
        if (m_syncThread != null) {
            m_syncThread.setPause(!m_bUsrOnline);
        }
    }

    private synchronized void pushToUpload(final SyncClass sc) {
        m_syncOut.add(sc);
    }

    private synchronized SyncClass popToUpload() {
        return (m_syncOut.isEmpty() ? null : m_syncOut.remove(0));
    }

    private synchronized void pushToDownload(final SyncClass sc) {
        m_syncIn.add(sc);
    }

    private synchronized SyncClass popToDownload() {
        return (m_syncIn.isEmpty() ? null : m_syncOut.remove(0));
    }

    private static ProxyServer m_this;

    private AppletServerHelper m_svrHelper;

    private CommandHelperThread m_cmdHelper;

    private UploadDocObjThread m_uploadDocObjThread;

    private UploadImgObjThread m_uploadImgObjThread;

    private SynchronizingThread m_syncThread;

    private ArrayList<ProxyEventListener> m_proxyListeners;

    private ArrayList<SyncClass> m_syncOut;

    private ArrayList<SyncClass> m_syncIn;

    private ArrayList<SynchronizeListener> m_syncListeners;

    private File m_fSync;

    private String m_sLastSaveImgName;

    private boolean m_bNetOnLine;

    private boolean m_bUsrOnline;

    public static final String TMP_DOC_FN = "backup.ser";

    public static final String DOC_TMP_FN = "tmp-doc";

    public static final String IMG_TMP_FN = "tmp-img";

    public static final String USR_SYNC_DIR = ".sync";

    public static final String DOC_SYNC_DIR = "doc";

    public static final String IMG_SYNC_DIR = "img";
}
