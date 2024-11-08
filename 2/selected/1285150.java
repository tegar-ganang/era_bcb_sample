package org.weasis.dicom.explorer.wado;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.VRMap;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.explorer.ObservableEvent;
import org.weasis.core.api.gui.task.CircularProgressBar;
import org.weasis.core.api.gui.task.SeriesProgressMonitor;
import org.weasis.core.api.gui.util.AbstractProperties;
import org.weasis.core.api.gui.util.GuiExecutor;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.api.media.data.SeriesImporter;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.media.data.TagW.TagType;
import org.weasis.core.api.media.data.Thumbnail;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.api.util.FileUtil;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.editor.SeriesViewerFactory;
import org.weasis.core.ui.editor.ViewerPluginBuilder;
import org.weasis.core.ui.editor.image.ViewerPlugin;
import org.weasis.dicom.codec.DicomInstance;
import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.TransferSyntax;
import org.weasis.dicom.codec.wado.WadoParameters;
import org.weasis.dicom.codec.wado.WadoParameters.HttpTag;
import org.weasis.dicom.explorer.DicomExplorer;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.explorer.MimeSystemAppFactory;

public class LoadSeries extends SwingWorker<Boolean, Void> implements SeriesImporter {

    private static final Logger log = LoggerFactory.getLogger(LoadSeries.class);

    public static final String CODOWNLOAD_IMAGES_NB = "wado.codownload.images.nb";

    public static final int CODOWNLOAD_NUMBER = BundleTools.SYSTEM_PREFERENCES.getIntProperty(CODOWNLOAD_IMAGES_NB, 4);

    public static final File DICOM_EXPORT_DIR = new File(AbstractProperties.APP_TEMP_DIR, "dicom");

    public static final File DICOM_TMP_DIR = new File(AbstractProperties.APP_TEMP_DIR, "tmp");

    static {
        try {
            DICOM_TMP_DIR.mkdirs();
            DICOM_EXPORT_DIR.mkdir();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    public enum Status {

        Downloading, Paused, Complete, Cancelled, Error
    }

    ;

    private final DicomModel dicomModel;

    private final Series dicomSeries;

    private final JProgressBar progressBar;

    private DownloadPriority priority = null;

    public LoadSeries(Series dicomSeries, DicomModel dicomModel) {
        if (dicomModel == null || dicomSeries == null) {
            throw new IllegalArgumentException("null parameters");
        }
        this.dicomModel = dicomModel;
        this.dicomSeries = dicomSeries;
        final List<DicomInstance> sopList = (List<DicomInstance>) dicomSeries.getTagValue(TagW.WadoInstanceReferenceList);
        final CircularProgressBar[] bar = new CircularProgressBar[1];
        GuiExecutor.instance().invokeAndWait(new Runnable() {

            @Override
            public void run() {
                bar[0] = new CircularProgressBar(0, sopList.size());
            }
        });
        this.progressBar = bar[0];
        this.dicomSeries.setSeriesLoader(this);
    }

    public LoadSeries(Series dicomSeries, DicomModel dicomModel, JProgressBar progressBar) {
        if (dicomModel == null || dicomSeries == null || progressBar == null) {
            throw new IllegalArgumentException("null parameters");
        }
        this.dicomModel = dicomModel;
        this.dicomSeries = dicomSeries;
        this.progressBar = progressBar;
        this.dicomSeries.setSeriesLoader(this);
    }

    @Override
    protected Boolean doInBackground() {
        return startDownload();
    }

    @Override
    public JProgressBar getProgressBar() {
        return progressBar;
    }

    @Override
    public boolean isStopped() {
        return isCancelled();
    }

    @Override
    public boolean stop() {
        if (!isDone()) {
            boolean val = cancel(true);
            dicomSeries.setSeriesLoader(this);
            return val;
        }
        return true;
    }

    @Override
    public void resume() {
        if (isStopped()) {
            LoadSeries taskResume = new LoadSeries(dicomSeries, dicomModel, progressBar);
            DownloadPriority p = this.getPriority();
            p.setPriority(DownloadPriority.COUNTER.getAndDecrement());
            taskResume.setPriority(p);
            Thumbnail thumbnail = (Thumbnail) this.getDicomSeries().getTagValue(TagW.Thumbnail);
            if (thumbnail != null) {
                LoadSeries.removeAnonymousMouseAndKeyListener(thumbnail);
                thumbnail.addMouseListener(DicomExplorer.createThumbnailMouseAdapter(taskResume.getDicomSeries(), dicomModel, taskResume));
                thumbnail.addKeyListener(DicomExplorer.createThumbnailKeyListener(taskResume.getDicomSeries(), dicomModel));
            }
            LoadRemoteDicomManifest.loadingQueue.offer(taskResume);
            LoadRemoteDicomManifest.addLoadSeries(taskResume, dicomModel);
            LoadRemoteDicomManifest.removeLoadSeries(this, dicomModel);
        }
    }

    @Override
    protected void done() {
        if (!isStopped()) {
            LoadRemoteDicomManifest.removeLoadSeries(this, dicomModel);
            Thumbnail thumbnail = (Thumbnail) dicomSeries.getTagValue(TagW.Thumbnail);
            if (thumbnail != null) {
                thumbnail.setProgressBar(null);
                thumbnail.repaint();
            }
            Integer splitNb = (Integer) dicomSeries.getTagValue(TagW.SplitSeriesNumber);
            Object dicomObject = dicomSeries.getTagValue(TagW.DicomSpecialElement);
            if (splitNb != null || dicomObject != null) {
                dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.Update, dicomModel, null, dicomSeries));
            } else if (dicomSeries.size() == 0) {
                dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.Remove, dicomModel, null, dicomSeries));
            }
            this.dicomSeries.setSeriesLoader(null);
        }
    }

    private boolean isSOPInstanceUIDExist(MediaSeriesGroup study, Series dicomSeries, String sopUID) {
        if (dicomSeries.hasMediaContains(TagW.SOPInstanceUID, sopUID)) {
            return true;
        }
        String uid = (String) dicomSeries.getTagValue(TagW.SeriesInstanceUID);
        if (study != null && uid != null) {
            Collection<MediaSeriesGroup> seriesList = dicomModel.getChildren(study);
            for (Iterator<MediaSeriesGroup> it = seriesList.iterator(); it.hasNext(); ) {
                MediaSeriesGroup group = it.next();
                if (dicomSeries != group && group instanceof Series) {
                    Series s = (Series) group;
                    if (uid.equals(s.getTagValue(TagW.SeriesInstanceUID))) {
                        if (s.hasMediaContains(TagW.SOPInstanceUID, sopUID)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void incrementProgressBarValue() {
        GuiExecutor.instance().execute(new Runnable() {

            @Override
            public void run() {
                progressBar.setValue(progressBar.getValue() + 1);
            }
        });
    }

    private Boolean startDownload() {
        MediaSeriesGroup patient = dicomModel.getParent(dicomSeries, DicomModel.patient);
        MediaSeriesGroup study = dicomModel.getParent(dicomSeries, DicomModel.study);
        log.info("Downloading series of {} [{}]", patient, dicomSeries);
        final List<DicomInstance> sopList = (List<DicomInstance>) dicomSeries.getTagValue(TagW.WadoInstanceReferenceList);
        final WadoParameters wado = (WadoParameters) dicomSeries.getTagValue(TagW.WadoParameters);
        if (wado == null) {
            return false;
        }
        ExecutorService imageDownloader = Executors.newFixedThreadPool(CODOWNLOAD_NUMBER);
        ArrayList<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>(sopList.size());
        int[] dindex = generateDownladOrder(sopList.size());
        GuiExecutor.instance().execute(new Runnable() {

            @Override
            public void run() {
                progressBar.setValue(0);
            }
        });
        for (int k = 0; k < sopList.size(); k++) {
            DicomInstance instance = sopList.get(dindex[k]);
            if (isCancelled()) {
                return true;
            }
            if (isSOPInstanceUIDExist(study, dicomSeries, instance.getSopInstanceUID())) {
                incrementProgressBarValue();
                log.debug("DICOM instance {} already exists, skip.", instance.getSopInstanceUID());
                continue;
            }
            URL url = null;
            try {
                String studyUID = "";
                String seriesUID = "";
                if (!wado.isRequireOnlySOPInstanceUID()) {
                    studyUID = (String) study.getTagValue(TagW.StudyInstanceUID);
                    seriesUID = (String) dicomSeries.getTagValue(TagW.SeriesInstanceUID);
                }
                StringBuffer request = new StringBuffer(wado.getWadoURL());
                if (instance.getDirectDownloadFile() == null) {
                    request.append("?requestType=WADO&studyUID=");
                    request.append(studyUID);
                    request.append("&seriesUID=");
                    request.append(seriesUID);
                    request.append("&objectUID=");
                    request.append(instance.getSopInstanceUID());
                    request.append("&contentType=application%2Fdicom");
                    TransferSyntax transcoding = DicomManager.getInstance().getWadoTSUID();
                    if (transcoding.getTransferSyntaxUID() != null) {
                        dicomSeries.setTag(TagW.WadoTransferSyntaxUID, transcoding.getTransferSyntaxUID());
                    }
                    String wado_tsuid = (String) dicomSeries.getTagValue(TagW.WadoTransferSyntaxUID);
                    if (wado_tsuid != null && !wado_tsuid.equals("")) {
                        String osName = AbstractProperties.OPERATING_SYSTEM;
                        if (!(osName.startsWith("win") && "x86".equals(System.getProperty("os.arch"))) && !osName.startsWith("linux")) {
                            if (wado_tsuid.startsWith("1.2.840.10008.1.2.4.5") || wado_tsuid.startsWith("1.2.840.10008.1.2.4.7") || wado_tsuid.startsWith("1.2.840.10008.1.2.4.8")) {
                                wado_tsuid = TransferSyntax.EXPLICIT_VR_LE.getTransferSyntaxUID();
                            }
                        }
                        request.append("&transferSyntax=");
                        request.append(wado_tsuid);
                        if (transcoding.getTransferSyntaxUID() != null) {
                            dicomSeries.setTag(TagW.WadoCompressionRate, transcoding.getCompression());
                        }
                        Integer rate = (Integer) dicomSeries.getTagValue(TagW.WadoCompressionRate);
                        if (rate != null && rate > 0) {
                            request.append("&imageQuality=");
                            request.append(rate);
                        }
                    }
                } else {
                    request.append(instance.getDirectDownloadFile());
                }
                request.append(wado.getAdditionnalParameters());
                url = new URL(request.toString());
            } catch (MalformedURLException e1) {
                log.error(e1.getMessage(), e1.getCause());
                continue;
            }
            log.debug("Download DICOM instance {} index {}.", url, k);
            Download ref = new Download(url, wado);
            tasks.add(ref);
        }
        try {
            imageDownloader.invokeAll(tasks);
        } catch (InterruptedException e) {
        }
        imageDownloader.shutdown();
        return true;
    }

    public void startDownloadImageReference(final WadoParameters wadoParameters) {
        final List<DicomInstance> sopList = (List<DicomInstance>) dicomSeries.getTagValue(TagW.WadoInstanceReferenceList);
        if (sopList.size() > 0) {
            Collections.sort(sopList, new Comparator<DicomInstance>() {

                @Override
                public int compare(DicomInstance dcm1, DicomInstance dcm2) {
                    int nubmer1 = dcm1.getInstanceNumber();
                    int nubmer2 = dcm2.getInstanceNumber();
                    if (nubmer1 == -1 && nubmer2 == -1) {
                        String str1 = dcm1.getSopInstanceUID();
                        String str2 = dcm2.getSopInstanceUID();
                        int length1 = str1.length();
                        int length2 = str2.length();
                        if (length1 < length2) {
                            char[] c = new char[length2 - length1];
                            for (int i = 0; i < c.length; i++) {
                                c[i] = '0';
                            }
                            int index = str1.lastIndexOf(".") + 1;
                            str1 = str1.substring(0, index) + new String(c) + str1.substring(index);
                        } else if (length1 > length2) {
                            char[] c = new char[length1 - length2];
                            for (int i = 0; i < c.length; i++) {
                                c[i] = '0';
                            }
                            int index = str2.lastIndexOf(".") + 1;
                            str2 = str2.substring(0, index) + new String(c) + str2.substring(index);
                        }
                        return str1.compareTo(str2);
                    } else {
                        return (nubmer1 < nubmer2 ? -1 : (nubmer1 == nubmer2 ? 0 : 1));
                    }
                }
            });
            final DicomInstance instance = sopList.get(sopList.size() / 2);
            GuiExecutor.instance().execute(new Runnable() {

                @Override
                public void run() {
                    Thumbnail thumbnail = (Thumbnail) dicomSeries.getTagValue(TagW.Thumbnail);
                    if (thumbnail == null) {
                        thumbnail = new Thumbnail(dicomSeries, null, Thumbnail.DEFAULT_SIZE);
                    }
                    if (LoadSeries.this.isDone()) {
                        thumbnail.setProgressBar(null);
                        thumbnail.repaint();
                    } else {
                        thumbnail.setProgressBar(progressBar);
                    }
                    addListenerToThumbnail(thumbnail, LoadSeries.this, dicomModel);
                    thumbnail.registerListeners();
                    dicomSeries.setTag(TagW.Thumbnail, thumbnail);
                    dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.Add, dicomModel, null, dicomSeries));
                }
            });
            Runnable thumbnailLoader = new Runnable() {

                @Override
                public void run() {
                    String studyUID = "";
                    String seriesUID = "";
                    if (!wadoParameters.isRequireOnlySOPInstanceUID()) {
                        MediaSeriesGroup study = dicomModel.getParent(dicomSeries, DicomModel.study);
                        studyUID = (String) study.getTagValue(TagW.StudyInstanceUID);
                        seriesUID = (String) dicomSeries.getTagValue(TagW.SeriesInstanceUID);
                    }
                    File file = null;
                    if (instance.getDirectDownloadFile() == null) {
                        try {
                            file = getJPEGThumnails(wadoParameters, studyUID, seriesUID, instance.getSopInstanceUID());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        String thumURL = (String) dicomSeries.getTagValue(TagW.DirectDownloadThumbnail);
                        if (thumURL != null) {
                            try {
                                File outFile = File.createTempFile("tumb_", FileUtil.getExtension(thumURL), AbstractProperties.APP_TEMP_DIR);
                                int resp = FileUtil.writeFile(new URL(wadoParameters.getWadoURL() + thumURL), outFile);
                                if (resp == -1) {
                                    file = outFile;
                                }
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (file != null) {
                        final File finalfile = file;
                        GuiExecutor.instance().execute(new Runnable() {

                            @Override
                            public void run() {
                                Thumbnail thumbnail = (Thumbnail) dicomSeries.getTagValue(TagW.Thumbnail);
                                if (thumbnail != null) {
                                    thumbnail.reBuildThumbnail(finalfile, MediaSeries.MEDIA_POSITION.MIDDLE);
                                }
                            }
                        });
                    }
                }
            };
            executor.submit(thumbnailLoader);
        }
    }

    public static void removeAnonymousMouseAndKeyListener(Thumbnail tumbnail) {
        MouseListener[] listener = tumbnail.getMouseListeners();
        MouseMotionListener[] motionListeners = tumbnail.getMouseMotionListeners();
        KeyListener[] keyListeners = tumbnail.getKeyListeners();
        MouseWheelListener[] wheelListeners = tumbnail.getMouseWheelListeners();
        for (int i = 0; i < listener.length; i++) {
            if (listener[i].getClass().isAnonymousClass()) {
                tumbnail.removeMouseListener(listener[i]);
            }
        }
        for (int i = 0; i < motionListeners.length; i++) {
            if (motionListeners[i].getClass().isAnonymousClass()) {
                tumbnail.removeMouseMotionListener(motionListeners[i]);
            }
        }
        for (int i = 0; i < wheelListeners.length; i++) {
            if (wheelListeners[i].getClass().isAnonymousClass()) {
                tumbnail.removeMouseWheelListener(wheelListeners[i]);
            }
        }
        for (int i = 0; i < keyListeners.length; i++) {
            if (keyListeners[i].getClass().isAnonymousClass()) {
                tumbnail.removeKeyListener(keyListeners[i]);
            }
        }
    }

    private static void addListenerToThumbnail(final Thumbnail thumbnail, final LoadSeries loadSeries, final DicomModel dicomModel) {
        final Series series = loadSeries.getDicomSeries();
        thumbnail.addMouseListener(DicomExplorer.createThumbnailMouseAdapter(series, dicomModel, loadSeries));
        thumbnail.addKeyListener(DicomExplorer.createThumbnailKeyListener(series, dicomModel));
    }

    public static void openSequenceInPlugin(SeriesViewerFactory factory, List<MediaSeries> series, DicomModel dicomModel, boolean removeOldSeries) {
        if (factory == null) {
            return;
        }
        dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.Register, dicomModel, null, new ViewerPluginBuilder(factory, series, dicomModel, true, removeOldSeries)));
    }

    public static void openSequenceInDefaultPlugin(List<MediaSeries> series, DicomModel dicomModel) {
        ArrayList<String> mimes = new ArrayList<String>();
        for (MediaSeries s : series) {
            String mime = s.getMimeType();
            if (mime != null && !mimes.contains(mime)) {
                mimes.add(mime);
            }
        }
        for (String mime : mimes) {
            SeriesViewerFactory plugin = UIManager.getViewerFactory(mime);
            if (plugin != null) {
                ArrayList<MediaSeries> seriesList = new ArrayList<MediaSeries>();
                for (MediaSeries s : series) {
                    if (mime.equals(s.getMimeType())) {
                        seriesList.add(s);
                    }
                }
                openSequenceInPlugin(plugin, seriesList, dicomModel, true);
            }
        }
    }

    public Series getDicomSeries() {
        return dicomSeries;
    }

    public File getJPEGThumnails(WadoParameters wadoParameters, String StudyUID, String SeriesUID, String SOPInstanceUID) throws Exception {
        URL url = new URL(wadoParameters.getWadoURL() + "?requestType=WADO&studyUID=" + StudyUID + "&seriesUID=" + SeriesUID + "&objectUID=" + SOPInstanceUID + "&contentType=image/jpeg&imageQuality=70" + "&rows=" + Thumbnail.MAX_SIZE + "&columns=" + Thumbnail.MAX_SIZE + wadoParameters.getAdditionnalParameters());
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoInput(true);
        httpCon.setRequestMethod("GET");
        if (wadoParameters.getWebLogin() != null) {
            httpCon.setRequestProperty("Authorization", "Basic " + wadoParameters.getWebLogin());
        }
        if (wadoParameters.getHttpTaglist().size() > 0) {
            for (HttpTag tag : wadoParameters.getHttpTaglist()) {
                httpCon.setRequestProperty(tag.getKey(), tag.getValue());
            }
        }
        httpCon.connect();
        if (httpCon.getResponseCode() / 100 != 2) {
            return null;
        }
        OutputStream out = null;
        InputStream in = null;
        File outFile = File.createTempFile("tumb_", ".jpg", AbstractProperties.APP_TEMP_DIR);
        try {
            out = new BufferedOutputStream(new FileOutputStream(outFile));
            in = httpCon.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(in);
            FileUtil.safeClose(out);
        }
        return outFile;
    }

    private int[] generateDownladOrder(final int size) {
        int[] dindex = new int[size];
        if (size < 4) {
            for (int i = 0; i < dindex.length; i++) {
                dindex[i] = i;
            }
            return dindex;
        }
        boolean[] map = new boolean[size];
        int pos = 0;
        dindex[pos++] = 0;
        map[0] = true;
        dindex[pos++] = size - 1;
        map[size - 1] = true;
        int k = (size - 1) / 2;
        dindex[pos++] = k;
        map[k] = true;
        while (k > 0) {
            int i = 1;
            int start = 0;
            while (i < map.length) {
                if (map[i]) {
                    if (!map[i - 1]) {
                        int mid = start + (i - start) / 2;
                        map[mid] = true;
                        dindex[pos++] = mid;
                    }
                    start = i;
                }
                i++;
            }
            k /= 2;
        }
        return dindex;
    }

    class Download implements Callable<Boolean> {

        private final URL url;

        private int size;

        private final int downloaded;

        private Status status;

        private File tempFile;

        private final WadoParameters wadoParameters;

        public Download(URL url, final WadoParameters wadoParameters) {
            this.url = url;
            this.wadoParameters = wadoParameters;
            size = -1;
            downloaded = 0;
            status = Status.Downloading;
        }

        public File getTempFile() {
            return tempFile;
        }

        public String getUrl() {
            return url.toString();
        }

        public int getSize() {
            return size;
        }

        public float getProgress() {
            return ((float) downloaded / size) * 100;
        }

        public Status getStatus() {
            return status;
        }

        public void pause() {
            status = Status.Paused;
        }

        public void resume() {
            status = Status.Downloading;
        }

        public void cancel() {
            status = Status.Cancelled;
        }

        private void error() {
            status = Status.Error;
        }

        @Override
        public Boolean call() throws Exception {
            InputStream stream = null;
            URLConnection httpCon = null;
            try {
                httpCon = url.openConnection();
                if (wadoParameters.getWebLogin() != null) {
                    httpCon.setRequestProperty("Authorization", "Basic " + wadoParameters.getWebLogin());
                }
                if (wadoParameters.getHttpTaglist().size() > 0) {
                    for (HttpTag tag : wadoParameters.getHttpTaglist()) {
                        httpCon.setRequestProperty(tag.getKey(), tag.getValue());
                    }
                }
                httpCon.setRequestProperty("Range", "bytes=" + downloaded + "-");
                httpCon.connect();
            } catch (IOException e) {
                error();
                log.error("IOException for {}: {} ", url, e.getMessage());
                return false;
            }
            if (httpCon instanceof HttpURLConnection) {
                int responseCode = ((HttpURLConnection) httpCon).getResponseCode();
                if (responseCode / 100 != 2) {
                    error();
                    log.error("Http Response error {} for {}", responseCode, url);
                    return false;
                }
            }
            if (tempFile == null) {
                tempFile = File.createTempFile("image_", ".dcm", DICOM_TMP_DIR);
            }
            stream = httpCon.getInputStream();
            int contentLength = httpCon.getContentLength();
            contentLength = -1;
            if (contentLength == -1) {
                progressBar.setIndeterminate(progressBar.getMaximum() < 3);
            } else {
            }
            if (size == -1) {
                size = contentLength;
            }
            DicomMediaIO dicomReader = null;
            log.debug("Download DICOM instance {} to {}.", url, tempFile.getName());
            if (dicomSeries != null) {
                final WadoParameters wado = (WadoParameters) dicomSeries.getTagValue(TagW.WadoParameters);
                int[] overrideList = wado.getOverrideDicomTagIDList();
                int bytesTransferred = 0;
                if (overrideList == null && wado != null) {
                    bytesTransferred = FileUtil.writeFile(new SeriesProgressMonitor(dicomSeries, stream), new FileOutputStream(tempFile));
                } else if (wado != null) {
                    bytesTransferred = writFile(new SeriesProgressMonitor(dicomSeries, stream), new FileOutputStream(tempFile), overrideList);
                }
                if (bytesTransferred >= 0) {
                    log.warn("Download interruption {} ", url);
                    try {
                        tempFile.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }
                File renameFile = new File(DICOM_EXPORT_DIR, tempFile.getName());
                if (tempFile.renameTo(renameFile)) {
                    tempFile = renameFile;
                }
                dicomReader = new DicomMediaIO(tempFile);
                if (dicomReader.readMediaTags()) {
                    if (dicomSeries.size() == 0) {
                        MediaSeriesGroup patient = dicomModel.getParent(dicomSeries, DicomModel.patient);
                        dicomReader.writeMetaData(patient);
                        MediaSeriesGroup study = dicomModel.getParent(dicomSeries, DicomModel.study);
                        dicomReader.writeMetaData(study);
                        dicomReader.writeMetaData(dicomSeries);
                        GuiExecutor.instance().invokeAndWait(new Runnable() {

                            @Override
                            public void run() {
                                Thumbnail thumb = (Thumbnail) dicomSeries.getTagValue(TagW.Thumbnail);
                                if (thumb != null) {
                                    thumb.repaint();
                                }
                                dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.UpdateParent, dicomModel, null, dicomSeries));
                            }
                        });
                    }
                }
            }
            if (status == Status.Downloading) {
                status = Status.Complete;
                if (tempFile != null) {
                    if (dicomSeries != null && dicomReader.readMediaTags()) {
                        final DicomMediaIO reader = dicomReader;
                        GuiExecutor.instance().invokeAndWait(new Runnable() {

                            @Override
                            public void run() {
                                boolean firstImageToDisplay = false;
                                MediaElement[] medias = reader.getMediaElement();
                                if (medias != null) {
                                    firstImageToDisplay = dicomSeries.size() == 0;
                                    for (MediaElement media : medias) {
                                        dicomModel.applySplittingRules(dicomSeries, media);
                                    }
                                    if (firstImageToDisplay && dicomSeries.size() == 0) {
                                        firstImageToDisplay = false;
                                    }
                                }
                                reader.reset();
                                Thumbnail thumb = (Thumbnail) dicomSeries.getTagValue(TagW.Thumbnail);
                                if (thumb != null) {
                                    thumb.repaint();
                                }
                                if (firstImageToDisplay) {
                                    boolean openNewTab = true;
                                    MediaSeriesGroup entry1 = dicomModel.getParent(dicomSeries, DicomModel.patient);
                                    if (entry1 != null) {
                                        synchronized (UIManager.VIEWER_PLUGINS) {
                                            for (final ViewerPlugin p : UIManager.VIEWER_PLUGINS) {
                                                if (entry1.equals(p.getGroupID())) {
                                                    openNewTab = false;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (openNewTab) {
                                        SeriesViewerFactory plugin = UIManager.getViewerFactory(dicomSeries.getMimeType());
                                        if (plugin != null && !(plugin instanceof MimeSystemAppFactory)) {
                                            ArrayList<MediaSeries> list = new ArrayList<MediaSeries>(1);
                                            list.add(dicomSeries);
                                            LoadSeries.openSequenceInPlugin(plugin, list, dicomModel, true);
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            }
            incrementProgressBarValue();
            return true;
        }

        /**
         * @param inputStream
         * @param out
         * @param overrideList
         * @return bytes transferred. O = error, -1 = all bytes has been transferred, other = bytes transferred before
         *         interruption
         */
        public int writFile(InputStream inputStream, OutputStream out, int[] overrideList) {
            if (inputStream == null && out == null) {
                return 0;
            }
            DicomInputStream dis = null;
            DicomOutputStream dos = null;
            try {
                dis = new DicomInputStream(new BufferedInputStream(inputStream));
                DicomObject dcm = dis.readDicomObject();
                dos = new DicomOutputStream(new BufferedOutputStream(out));
                if (overrideList != null) {
                    MediaSeriesGroup study = dicomModel.getParent(dicomSeries, DicomModel.study);
                    MediaSeriesGroup patient = dicomModel.getParent(dicomSeries, DicomModel.patient);
                    VRMap vrMap = VRMap.getVRMap();
                    for (int tag : overrideList) {
                        TagW tagElement = patient.getTagElement(tag);
                        Object value = null;
                        if (tagElement == null) {
                            tagElement = study.getTagElement(tag);
                            value = study.getTagValue(tagElement);
                        } else {
                            value = patient.getTagValue(tagElement);
                        }
                        if (value != null) {
                            TagType type = tagElement.getType();
                            if (TagType.String.equals(type)) {
                                dcm.putString(tag, vrMap.vrOf(tag), value.toString());
                            } else if (TagType.Date.equals(type) || TagType.Time.equals(type)) {
                                dcm.putDate(tag, vrMap.vrOf(tag), (Date) value);
                            } else if (TagType.Integer.equals(type)) {
                                dcm.putInt(tag, vrMap.vrOf(tag), (Integer) value);
                            } else if (TagType.Float.equals(type)) {
                                dcm.putFloat(tag, vrMap.vrOf(tag), (Float) value);
                            }
                        }
                    }
                }
                dos.writeDicomFile(dcm);
                return -1;
            } catch (InterruptedIOException e) {
                return e.bytesTransferred;
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            } finally {
                FileUtil.safeClose(dos);
                FileUtil.safeClose(dis);
            }
        }
    }

    public synchronized DownloadPriority getPriority() {
        return priority;
    }

    public synchronized void setPriority(DownloadPriority priority) {
        this.priority = priority;
    }

    @Override
    public void setPriority() {
        DownloadPriority p = getPriority();
        if (p != null) {
            if (StateValue.PENDING.equals(getState())) {
                boolean change = LoadRemoteDicomManifest.loadingQueue.remove(this);
                if (change) {
                    p.setPriority(DownloadPriority.COUNTER.getAndDecrement());
                    LoadRemoteDicomManifest.loadingQueue.offer(this);
                    synchronized (LoadRemoteDicomManifest.currentTasks) {
                        for (LoadSeries s : LoadRemoteDicomManifest.currentTasks) {
                            if (s != this && StateValue.STARTED.equals(s.getState())) {
                                LoadSeries taskResume = new LoadSeries(s.getDicomSeries(), dicomModel, s.getProgressBar());
                                s.cancel(true);
                                taskResume.setPriority(s.getPriority());
                                Thumbnail thumbnail = (Thumbnail) s.getDicomSeries().getTagValue(TagW.Thumbnail);
                                if (thumbnail != null) {
                                    LoadSeries.removeAnonymousMouseAndKeyListener(thumbnail);
                                    addListenerToThumbnail(thumbnail, taskResume, dicomModel);
                                }
                                LoadRemoteDicomManifest.loadingQueue.offer(taskResume);
                                LoadRemoteDicomManifest.addLoadSeries(taskResume, dicomModel);
                                LoadRemoteDicomManifest.removeLoadSeries(s, dicomModel);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
