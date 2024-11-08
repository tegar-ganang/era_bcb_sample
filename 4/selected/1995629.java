package in.raster.mayam.delegate;

import in.raster.mayam.context.ApplicationContext;
import in.raster.mayam.model.InputArgumentValues;
import in.raster.mayam.model.ServerModel;
import in.raster.mayam.param.WadoParam;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4che.util.DcmURL;

/**
 *
 * @author  BabuHussain
 * @version 0.7
 *
 */
public class WadoRetrieveDelegate extends Thread {

    private Vector<WadoParam> wadoUrls;

    private HttpURLConnection httpURLConnection;

    private String studyUID;

    private String serverName;

    private String patientID;

    private String destinationPath;

    private ServerModel serverModel = null;

    public WadoRetrieveDelegate() {
        this.wadoUrls = new Vector();
    }

    public void run() {
        getWadoURLList();
        doDownloadStudy();
    }

    public void retrieveStudy(String serverName, String patientID, String studyInstanceUID) {
        this.serverName = serverName;
        this.patientID = patientID;
        this.studyUID = studyInstanceUID;
        this.start();
    }

    public void retrieveStudy(ServerModel serverModel) {
        this.serverModel = serverModel;
        InputArgumentValues inputArgumentValues = null;
        if (serverModel.getAeTitle() != null) {
            inputArgumentValues = InputArgumentsParser.inputArgumentValues;
            EchoService echoService = new EchoService();
            DcmURL dcmurl = new DcmURL("dicom://" + inputArgumentValues.getAeTitle() + "@" + inputArgumentValues.getHostName() + ":" + inputArgumentValues.getPort());
            echoService.checkEcho(dcmurl);
            if (echoService.getStatus().equalsIgnoreCase("EchoSuccess")) {
                QueryService queryService = new QueryService();
                queryService.callFindWithQuery(inputArgumentValues.getPatientID(), inputArgumentValues.getPatientName(), null, inputArgumentValues.getSearchDate(), inputArgumentValues.getModality(), inputArgumentValues.getAccessionNumber(), inputArgumentValues.getStudyUID(), dcmurl);
                for (int dataSetCount = 0; dataSetCount < queryService.getDatasetVector().size(); dataSetCount++) {
                    try {
                        Dataset dataSet = (Dataset) queryService.getDatasetVector().elementAt(dataSetCount);
                        this.patientID = dataSet.getString(Tags.PatientID) != null ? dataSet.getString(Tags.PatientID) : "";
                        this.studyUID = dataSet.getString(Tags.StudyInstanceUID) != null ? dataSet.getString(Tags.StudyInstanceUID) : "";
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
                this.start();
            }
        }
    }

    private void getWadoURLList() {
        String seriesInstanceUID;
        String instanceUID = "";
        if (wadoUrls != null) {
            wadoUrls.clear();
        }
        if (serverModel == null) {
            serverModel = ApplicationContext.databaseRef.getServerModel(serverName);
        }
        DcmURL url = new DcmURL("dicom://" + serverModel.getAeTitle() + "@" + serverModel.getHostName() + ":" + serverModel.getPort());
        QuerySeriesService querySeriesService = new QuerySeriesService();
        if (patientID != null || studyUID != null) querySeriesService.callFindWithQuery(patientID, studyUID, url);
        for (int dataSetCount = 0; dataSetCount < querySeriesService.getDatasetVector().size(); dataSetCount++) {
            try {
                Dataset dataSet = (Dataset) querySeriesService.getDatasetVector().elementAt(dataSetCount);
                seriesInstanceUID = dataSet.getString(Tags.SeriesInstanceUID) != null ? dataSet.getString(Tags.SeriesInstanceUID) : "";
                QueryInstanceService queryInstanceService = new QueryInstanceService();
                queryInstanceService.callFindWithQuery(patientID, studyUID, seriesInstanceUID, url);
                for (int instanceCount = 0; instanceCount < queryInstanceService.getDatasetVector().size(); instanceCount++) {
                    Dataset instanceDataset = (Dataset) queryInstanceService.getDatasetVector().elementAt(instanceCount);
                    instanceUID = instanceDataset.getString(Tags.SOPInstanceUID) != null ? instanceDataset.getString(Tags.SOPInstanceUID) : "";
                    WadoParam wadoParam = getWadoParam(serverModel.getWadoProtocol(), serverModel.getAeTitle(), serverModel.getHostName(), serverModel.getWadoPort(), studyUID, seriesInstanceUID, instanceUID, serverModel.getRetrieveTransferSyntax());
                    wadoUrls.add(wadoParam);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private WadoParam getWadoParam(String wadoProtocol, String aeTitle, String hostName, int port, String studyUID, String seriesUID, String instanceUID, String retrieveTransferSyntax) {
        WadoParam wadoParam = new WadoParam();
        if (wadoProtocol.equalsIgnoreCase("https")) {
            wadoParam.setSecureQuery(true);
        } else {
            wadoParam.setSecureQuery(false);
        }
        wadoParam.setAeTitle(aeTitle);
        wadoParam.setRemoteHostName(hostName);
        wadoParam.setRemotePort(port);
        wadoParam.setStudy(studyUID);
        wadoParam.setSeries(seriesUID);
        wadoParam.setObject(instanceUID);
        wadoParam.setRetrieveTrasferSyntax(retrieveTransferSyntax);
        return wadoParam;
    }

    public void doDownloadStudy() {
        for (WadoParam wadoParam : wadoUrls) {
            String queryString = "";
            if (wadoParam != null) {
                queryString = wadoParam.getWadoUrl();
            }
            try {
                URL wadoUrl = new URL(queryString);
                httpURLConnection = (HttpURLConnection) wadoUrl.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setInstanceFollowRedirects(false);
                httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpURLConnection.setRequestProperty("Content-Type", "application/x-java-serialized-object");
                try {
                    httpURLConnection.connect();
                } catch (RuntimeException e) {
                    System.out.println("Error while querying " + e.getMessage());
                }
                if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    responseSuccess(wadoParam);
                } else {
                    System.out.println("Response Error:" + httpURLConnection.getResponseMessage());
                }
            } catch (Exception ex) {
                Logger.getLogger(WadoRetrieveDelegate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void responseSuccess(WadoParam wadoParam) {
        InputStream in = null;
        try {
            OutputStream out = null;
            in = httpURLConnection.getInputStream();
            Calendar today = Calendar.getInstance();
            setDestination();
            File struturedDestination = new File(destinationPath + File.separator + today.get(Calendar.YEAR) + File.separator + today.get(Calendar.MONTH) + File.separator + today.get(Calendar.DATE) + File.separator + studyUID);
            String child[] = struturedDestination.list();
            if (child == null) {
                struturedDestination.mkdirs();
            }
            File storeLocation = new File(struturedDestination, wadoParam.getObject());
            out = new FileOutputStream(storeLocation);
            copy(in, out);
            NetworkQueueUpdateDelegate networkQueueUpdateDelegate = new NetworkQueueUpdateDelegate();
            networkQueueUpdateDelegate.updateReceiveTable(storeLocation, wadoParam.getAeTitle());
        } catch (IOException ex) {
            Logger.getLogger(WadoRetrieveDelegate.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(WadoRetrieveDelegate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void setDestination() {
        destinationPath = ApplicationContext.databaseRef.getListenerDetails()[2];
        ;
    }

    private synchronized void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (out != null) {
                out.write(buffer, 0, read);
            }
        }
    }
}
