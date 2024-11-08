package org.sac.browse.client.job;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.sac.browse.client.util.BrowseHelper;
import org.sac.browse.client.util.DataHolder;
import org.sac.browse.client.util.MyProgressListener;
import org.sac.browse.client.util.PerformanceLogger;
import org.sac.crosspather.common.exception.CrossPatherException;
import org.sac.crosspather.common.exception.ExceptionFactory;
import org.sac.crosspather.common.listener.WorkListener;
import org.sac.crosspather.common.log.AppLogger;
import org.sac.crosspather.common.util.ClientProperties;
import org.sac.crosspather.common.util.Compressor;
import org.sac.crosspather.common.util.Constants;
import org.sac.crosspather.common.util.CustomRetryHandler;
import org.sac.crosspather.common.util.HTTPHelper;
import org.sac.crosspather.common.util.Constants.CLIENT_SERVER_STATUS;
import org.sac.crosspather.common.util.Constants.ERROR_CODES;
import org.sac.crosspather.common.util.Constants.PROPERTY_KEYS;
import org.sac.custom.httpclient.MonitoredFilePart;

public class ServerPing implements Job {

    private static final int FAILURE_NOTICE_LIMIT = 500;

    private static final int FATAL_FAILURE_NOTICE_LIMIT = 2;

    private static final int MAX_POST_BYTES_SIZE = 5000000;

    private static final int READ_BUFFER_SIZE = 1000000;

    private static final int ACTIVE_ATTEMPT_COUNT = 10;

    private static final int ONE_MINUTES_IN_SEC = 60;

    private static HttpClient readHttpClient;

    private static HttpClient writeHttpClient;

    private static long lastClientReadPropertiesVersion;

    private static long lastClientWritePropertiesVersion;

    private static AppLogger logger = new AppLogger(ServerPing.class.getName());

    private static final ClientProperties props = ClientProperties.getInstance();

    private static Throwable lastException;

    private static Throwable lastFileSendException;

    private static WorkListener fileServerListener;

    private static boolean isFirstRun = true;

    private static int rescheduledCount = -1;

    private static int multiFailSleepTime = 0;

    /**
	 * Static modifier will cause current "mode" logic to fail if multiple
	 * threads execute ServerPing at the same time.
	 */
    private static int noMessageCount = 0;

    /**
	 * Static modifier will cause failure attempt logic to fail if multiple
	 * threads execute ServerPing at the same time.
	 */
    private static int failure_count = 0;

    private static int fileSendFailureCount = 0;

    private void init(JobExecutionContext context) {
        fileServerListener = (WorkListener) context.getJobDetail().getJobDataMap().get("JOB_LISTENER");
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            if (rescheduledCount != (Integer) context.getJobDetail().getJobDataMap().get("RESCHEDULED_COUNT")) {
                init(context);
                isFirstRun = true;
                rescheduledCount = (Integer) context.getJobDetail().getJobDataMap().get("RESCHEDULED_COUNT");
            }
            String response = readFromRemoteServer();
            selectMode(response, context);
            if (isFirstRun && (response != null)) {
                isFirstRun = false;
                logger.doLog(AppLogger.INFO, "First Hit to Server. Message (first 500 chars):\n" + (response.length() < 500 ? response : response.substring(0, 500)), null);
                fileServerListener.updateServerStatus(CLIENT_SERVER_STATUS.ONLINE);
            }
        } catch (IOException e) {
            errorHandler(false, e, null, true);
        } catch (CrossPatherException e) {
            errorHandler(false, e, null, true);
        }
    }

    private void selectMode(String response, JobExecutionContext context) {
        if (response == null || Constants.REQUEST_RESPONSE_KEYS.NONE.equals(response)) {
            if (noMessageCount == ACTIVE_ATTEMPT_COUNT) {
                fileServerListener.changeMode(CLIENT_SERVER_STATUS.PASSIVE);
            }
            noMessageCount++;
        } else {
            if (noMessageCount >= ACTIVE_ATTEMPT_COUNT) {
                fileServerListener.changeMode(CLIENT_SERVER_STATUS.ACTIVE);
            }
            noMessageCount = 0;
        }
    }

    private void updateInterval(JobExecutionContext context, int interval) {
        Scheduler scheduler = context.getScheduler();
        JobDetail detail = context.getJobDetail();
        String oldTriggerName = context.getTrigger().getName();
        String oldTriggerGroup = context.getTrigger().getGroup();
        SimpleTrigger trigger = (SimpleTrigger) context.getTrigger();
        trigger.setRepeatInterval(interval * 1000);
        try {
            scheduler.rescheduleJob(oldTriggerName, oldTriggerGroup, trigger);
        } catch (SchedulerException e) {
            errorHandler(false, e, null, true);
        }
    }

    private void writeToRemoteServer(String responseBody) throws HttpException, IOException, CrossPatherException {
        Map<String, String> responseMap = populateResponseIntoMap(responseBody);
        String key = responseMap.get(Constants.REQUEST_RESPONSE_KEYS.KEY);
        String path = responseMap.get(Constants.REQUEST_RESPONSE_KEYS.PATH);
        String zipFlag = responseMap.get(Constants.REQUEST_RESPONSE_KEYS.ZIP_FLAG);
        String requesterCode = responseMap.get(Constants.REQUEST_RESPONSE_KEYS.REQUESTER_ID);
        String action = responseMap.get(Constants.REQUEST_RESPONSE_KEYS.SUB_ACTION);
        if (requesterCode != null) {
            fileServerListener.updateRequester(requesterCode);
        }
        String invalidMsg = null;
        int statusCode = -1;
        if (Constants.ACTION_VALUES.GET_DIR_SIZE.equals(action)) {
            fileServerListener.updateMainStatus("Received request for size of directory: " + path);
            logger.doLog(AppLogger.FINEST, "Received request for size of directory: " + path, null);
            BrowseHelper browseHelper = new BrowseHelper(fileServerListener);
            long fileSize = browseHelper.getDirSize(new File(path));
            HttpClient client = prepareWriteHttpClient();
            statusCode = doSimpleHttpWrite(key, String.valueOf(fileSize), client);
        } else if (Constants.ACTION_VALUES.START_SLEEP_MODE.equals(action)) {
            fileServerListener.updateMainStatus("Received request with Generic Message: " + action);
            logger.doLog(AppLogger.FINEST, "Received request with Generic Message: " + action, null);
            String sleepDuration = responseMap.get(Constants.REQUEST_RESPONSE_KEYS.PARAMETER + "0");
            try {
                long sleepTime = Long.parseLong(sleepDuration);
                fileServerListener.activateSleepMode(sleepTime);
            } catch (NumberFormatException e) {
                errorHandler(false, e, null, true);
            }
            HttpClient client = prepareWriteHttpClient();
            statusCode = doSimpleHttpWrite(key, "OK", client);
        } else if (path.equals(null)) {
            invalidMsg = Constants.REQUEST_RESPONSE_KEYS.PATH + " not found in: " + responseBody;
        } else {
            DataHolder holder = null;
            if (zipFlag != null && Constants.REQUEST_RESPONSE_KEYS.ZIP_FLAG.equals(zipFlag)) {
                holder = new DataHolder(path);
                holder.setZip(true);
            } else {
                try {
                    BrowseHelper browseHelper = new BrowseHelper(fileServerListener);
                    holder = browseHelper.getContents(path, false);
                } catch (Exception e) {
                    holder = new DataHolder(e.toString());
                    errorHandler(false, e, null, true);
                }
            }
            if (holder == null) {
                invalidMsg = "Received request for invalid path: " + path;
            } else if (holder.isFile()) {
                fileServerListener.updateMainStatus("Received request for file" + path);
                logger.doLog(AppLogger.FINEST, "Received request for file" + path, null);
                if (!holder.getFile().canRead()) {
                    invalidMsg = "Processing request for reading restricted file:" + path;
                } else {
                    InputStream is = new FileInputStream(holder.getFile());
                    statusCode = writeStream(key, is, holder.getFile().getName());
                }
            } else if (holder.isZip()) {
                PipedInputStream in = new PipedInputStream();
                final PipedOutputStream out = new PipedOutputStream(in);
                final String sourcePath = holder.getString();
                fileServerListener.updateMainStatus("Processing request for directory: " + sourcePath);
                logger.doLog(AppLogger.FINEST, "Processing request for directory: " + sourcePath, null);
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            Compressor compressor = new Compressor(fileServerListener);
                            compressor.zip(sourcePath, out);
                            out.flush();
                            out.close();
                        } catch (FileNotFoundException e) {
                            errorHandler(false, e, null, true);
                        } catch (IOException e) {
                            errorHandler(false, e, null, true);
                        }
                    }
                }).start();
                statusCode = writeStream(key, in, new File(sourcePath).getName());
            } else {
                fileServerListener.updateMainStatus("Processing request for content-list of directory: " + holder.getString());
                logger.doLog(AppLogger.FINEST, "Processing request for content-list of directory: " + holder.getString(), null);
                HttpClient client = prepareWriteHttpClient();
                statusCode = doSimpleHttpWrite(key, holder.getString(), client);
            }
        }
        if (invalidMsg != null) {
            logger.doLog(AppLogger.WARN, invalidMsg, null);
        }
        if (statusCode != HttpStatus.SC_OK) {
            logger.doLog(AppLogger.WARN, "Unable to write to server with key:" + key + " at:" + extractDate(key), null);
            fileServerListener.updateSubStatus("Unable to write to server with key:" + key + " at:" + extractDate(key));
        } else {
            logger.doLog(AppLogger.INFO, "Wrote to server with key:" + key + " at:" + extractDate(key), null);
            fileServerListener.updateMainStatus("Request processed successfully.");
            fileServerListener.updateSubStatus("Wrote to server with key:" + key + " at:" + extractDate(key));
        }
    }

    private int writeStream(String key, InputStream sourceStream, String sourceName) throws CrossPatherException {
        int statusCode = -1;
        HttpClient client = prepareWriteHttpClient();
        PostMethod method = null;
        PerformanceLogger perf = new PerformanceLogger();
        perf.start();
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        int readbytes = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int currentReadBytes = 0;
        int currentBatchNo = 0;
        try {
            String sendFileProgress = props.getProperty(PROPERTY_KEYS.SEND_FILE_PROGRESS);
            MyProgressListener myProgressListener;
            if (sendFileProgress != null && "ON".equals(sendFileProgress)) {
                myProgressListener = new MyProgressListener(key, true);
            } else {
                myProgressListener = new MyProgressListener(key, false);
            }
            do {
                currentReadBytes = 0;
                bos.reset();
                while (currentReadBytes < MAX_POST_BYTES_SIZE) {
                    try {
                        readbytes = sourceStream.read(buffer);
                    } catch (IOException e) {
                        boolean isCritical = fileSendErrorHandler(false, e, null, true, key);
                        if (isCritical) {
                            try {
                                sourceStream.close();
                            } catch (IOException e1) {
                                logger.doLog(AppLogger.WARN, "Error while closing stream", e1);
                            }
                            return -1;
                        }
                    }
                    currentReadBytes += readbytes;
                    if (readbytes == -1) {
                        break;
                    }
                    bos.write(buffer, 0, readbytes);
                    buffer = new byte[(currentReadBytes + READ_BUFFER_SIZE > MAX_POST_BYTES_SIZE) ? (MAX_POST_BYTES_SIZE - currentReadBytes) : READ_BUFFER_SIZE];
                }
                method = createMethodForStreaming(key, sourceName, readbytes, bos, currentReadBytes, currentBatchNo, myProgressListener);
                logger.doLog(AppLogger.DEBUG, "Sending New Batch: " + currentBatchNo + ", batchSize" + currentReadBytes + ", final:" + (readbytes == -1 ? true : false), null);
                fileServerListener.updateSubStatus("Sending New Batch: " + currentBatchNo + ", batchSize" + currentReadBytes + ", final:" + (readbytes == -1 ? true : false));
                statusCode = -1;
                boolean hasFailed = false;
                Throwable failureCause = null;
                boolean isCritical = false;
                int alreadySentBytes = myProgressListener.getCurrent();
                try {
                    perf.split("Before client.executeMethod()");
                    statusCode = client.executeMethod(method);
                    perf.split("After client.executeMethod()");
                    if (statusCode != HttpStatus.SC_OK) {
                        hasFailed = true;
                    }
                } catch (IOException e) {
                    hasFailed = true;
                    failureCause = e;
                }
                int retryCount = 0;
                while (hasFailed) {
                    client = prepareWriteHttpClient();
                    method = createMethodForStreaming(key, sourceName, readbytes, bos, currentReadBytes, currentBatchNo, myProgressListener);
                    if (statusCode != HttpStatus.SC_GATEWAY_TIMEOUT) {
                        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
                        params.setConnectionTimeout(30000);
                        params.setSoTimeout(30000);
                        client.getHttpConnectionManager().setParams(params);
                    }
                    if (failureCause == null) {
                        failureCause = ExceptionFactory.createException(ERROR_CODES.CONNECTION_ERROR, statusCode + "");
                    }
                    isCritical = fileSendErrorHandler(false, failureCause, null, false, key);
                    hasFailed = false;
                    failureCause = null;
                    if (!isCritical) {
                        myProgressListener.resetCurrentSize(alreadySentBytes);
                        try {
                            perf.split("Before client.executeMethod() - retry " + (++retryCount));
                            statusCode = client.executeMethod(method);
                            perf.split("After client.executeMethod() - retry " + retryCount);
                            if (statusCode != HttpStatus.SC_OK) {
                                hasFailed = true;
                            }
                        } catch (IOException e) {
                            hasFailed = true;
                            failureCause = e;
                        }
                    } else {
                        break;
                    }
                }
                if (isCritical) {
                    break;
                } else {
                    logger.doLog(AppLogger.DEBUG, "Successfully sent Batch: " + currentBatchNo + ", batchSize" + currentReadBytes + ", final:" + (readbytes == -1 ? true : false), null);
                    fileServerListener.updateSubStatus("Successfully sent Batch: " + currentBatchNo + ", batchSize" + currentReadBytes + ", final:" + (readbytes == -1 ? true : false));
                }
                currentBatchNo++;
            } while (readbytes != -1);
            perf.split("After Full File Upload  - Uploaded Bytes: " + myProgressListener.getCurrent());
        } finally {
            if (method != null) {
                method.releaseConnection();
                perf.split("After method.releaseConnection()");
            }
        }
        perf.stop();
        logger.doLog(AppLogger.DEBUG, "\n" + perf.getStats(), null);
        return statusCode;
    }

    private PostMethod createMethodForStreaming(String key, String sourceName, int readbytes, ByteArrayOutputStream bos, int currentReadBytes, int currentBatchNo, MyProgressListener myProgressListener) {
        PostMethod method;
        method = new PostMethod(props.getProperty(PROPERTY_KEYS.REMOTE_SERVER_URL) + "FileServer");
        method.getParams().setParameter(Constants.REQUEST_RESPONSE_KEYS.ACTION, Constants.ACTION_VALUES.CLIENT_WRITE);
        method.getParams().setParameter(Constants.REQUEST_RESPONSE_KEYS.KEY, key);
        method.getParams().setParameter("TYPE", "FILE");
        if (readbytes == -1) {
            Part[] parts = { new StringPart(Constants.REQUEST_RESPONSE_KEYS.ACTION, Constants.ACTION_VALUES.CLIENT_WRITE), new StringPart(Constants.REQUEST_RESPONSE_KEYS.KEY, key), new StringPart("TYPE", "FILE"), new StringPart("BATCH_NO", String.valueOf(currentBatchNo)), new StringPart("BATCH_SIZE", String.valueOf(currentReadBytes)), new StringPart("MAX_BATCH_SIZE", String.valueOf(MAX_POST_BYTES_SIZE)), new StringPart("IS_FINAL_BATCH", "IS_FINAL_BATCH"), new MonitoredFilePart(sourceName, bos.toByteArray(), myProgressListener) };
            method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));
        } else {
            Part[] parts = { new StringPart(Constants.REQUEST_RESPONSE_KEYS.ACTION, Constants.ACTION_VALUES.CLIENT_WRITE), new StringPart(Constants.REQUEST_RESPONSE_KEYS.KEY, key), new StringPart("TYPE", "FILE"), new StringPart("BATCH_NO", String.valueOf(currentBatchNo)), new StringPart("BATCH_SIZE", String.valueOf(currentReadBytes)), new StringPart("MAX_BATCH_SIZE", String.valueOf(MAX_POST_BYTES_SIZE)), new MonitoredFilePart(sourceName, bos.toByteArray(), myProgressListener) };
            method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));
        }
        return method;
    }

    /**
	 *  Move this to a seperate Helper class in future.
	 * @return HttpClient
	 */
    private HttpClient prepareReadHttpClient() {
        if (readHttpClient == null || props.getPropertiesVersion() != lastClientReadPropertiesVersion) {
            lastClientReadPropertiesVersion = props.getPropertiesVersion();
            readHttpClient = HTTPHelper.prepareHttpClient();
        }
        return readHttpClient;
    }

    /**
	 *  Move this to a seperate Helper class in future.
	 * @return HttpClient
	 */
    private HttpClient prepareWriteHttpClient() {
        if (writeHttpClient == null || props.getPropertiesVersion() != lastClientWritePropertiesVersion) {
            lastClientWritePropertiesVersion = props.getPropertiesVersion();
            writeHttpClient = HTTPHelper.prepareHttpClient();
        }
        return writeHttpClient;
    }

    /** This is static since it is a helper method used by other classes too.
	 *  Move this to a seperate Helper class in future.
	 */
    public static int doSimpleHttpWrite(String key, String msg, HttpClient client) throws IOException, HttpException {
        int statusCode;
        StatusLine statusLine;
        Map<String, Object> returnValues = new HashMap<String, Object>();
        PostMethod method = new PostMethod(props.getProperty(PROPERTY_KEYS.REMOTE_SERVER_URL) + "FileServer");
        NameValuePair listPair1 = new NameValuePair(Constants.REQUEST_RESPONSE_KEYS.ACTION, Constants.ACTION_VALUES.CLIENT_WRITE);
        NameValuePair listPair2 = new NameValuePair(Constants.REQUEST_RESPONSE_KEYS.KEY, key);
        NameValuePair listPair3 = new NameValuePair("TYPE", "STRING");
        NameValuePair listPair4 = new NameValuePair(Constants.REQUEST_RESPONSE_KEYS.MSG, msg);
        method.setRequestBody(new NameValuePair[] { listPair1, listPair2, listPair3, listPair4 });
        statusCode = client.executeMethod(method);
        statusLine = method.getStatusLine();
        method.releaseConnection();
        returnValues.put("statusCode", statusCode);
        returnValues.put("statusLine", statusLine);
        return statusCode;
    }

    private Date extractDate(String key) {
        return new Date(Long.parseLong(key.split("_")[1]));
    }

    private String readFromRemoteServer() throws IOException, CrossPatherException {
        boolean msgPresent = false;
        HttpClient client = prepareReadHttpClient();
        PostMethod method = new PostMethod(props.getProperty(PROPERTY_KEYS.REMOTE_SERVER_URL) + "FileServer");
        NameValuePair listPair1 = new NameValuePair(Constants.REQUEST_RESPONSE_KEYS.ACTION, Constants.ACTION_VALUES.CLIENT_READ);
        String myCode = props.getProperty(PROPERTY_KEYS.MY_CODE);
        NameValuePair listPair2 = new NameValuePair(Constants.REQUEST_RESPONSE_KEYS.CLIENT_ID, myCode);
        HttpMethodRetryHandler retryHandler = new CustomRetryHandler(1);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);
        method.setRequestBody(new NameValuePair[] { listPair1, listPair2 });
        int statusCode = -1;
        String responseBody = null;
        StatusLine statusLine = null;
        try {
            statusCode = client.executeMethod(method);
            statusLine = method.getStatusLine();
            responseBody = method.getResponseBodyAsString();
            if (statusCode == HttpStatus.SC_OK) {
                if (!Constants.REQUEST_RESPONSE_KEYS.NONE.equals(responseBody)) {
                    if (authenticateResponse(responseBody)) {
                        msgPresent = true;
                        writeToRemoteServer(responseBody);
                    } else {
                        String brief;
                        if (responseBody.length() > 15) {
                            brief = responseBody.substring(0, 10) + "..." + "(" + responseBody.length() + " chars)";
                        } else {
                            brief = responseBody;
                        }
                        throw ExceptionFactory.createException(ERROR_CODES.UNAUTHORIZED_REQUEST, brief);
                    }
                }
            } else if (statusCode == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                throw ExceptionFactory.createException(ERROR_CODES.PROXY_AUTHENTICATION_REQUIRED);
            } else {
                throw ExceptionFactory.createException(ERROR_CODES.CONNECTION_ERROR, statusCode + " " + statusLine);
            }
        } finally {
            method.releaseConnection();
        }
        return responseBody;
    }

    private boolean authenticateResponse(String responseBody) {
        String key = extractKey(responseBody);
        String myCode = props.getProperty(Constants.PROPERTY_KEYS.MY_CODE);
        String[] splits = key.split("_");
        if (splits.length > 0 && splits[0].equals(myCode)) {
            return true;
        }
        logger.doLog(AppLogger.WARN, "Not autenticated response", null);
        return false;
    }

    private String extractKey(String response) {
        String[] split = response.split(Constants.REQUEST_RESPONSE_KEYS.KEY_SEPERATOR, 2);
        String tmp = split[0].split("\\" + Constants.REQUEST_RESPONSE_KEYS.VALUE_SEPERATOR)[1];
        return tmp;
    }

    private String extractPath(String response) {
        String[] split = response.split(Constants.REQUEST_RESPONSE_KEYS.KEY_SEPERATOR);
        if (split.length >= 2) {
            String tmp[] = split[1].split("\\" + Constants.REQUEST_RESPONSE_KEYS.VALUE_SEPERATOR, 2);
            if (tmp.length == 2 && Constants.REQUEST_RESPONSE_KEYS.PATH.equals(tmp[0])) {
                return tmp[1];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String extractZipFlag(String response) {
        String[] split = response.split(Constants.REQUEST_RESPONSE_KEYS.KEY_SEPERATOR);
        if (split.length >= 3) {
            String tmp[] = split[2].split("\\" + Constants.REQUEST_RESPONSE_KEYS.VALUE_SEPERATOR, 2);
            if (tmp.length == 2 && Constants.REQUEST_RESPONSE_KEYS.ZIP_FLAG.equals(tmp[0])) {
                return tmp[1];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String extractRequesterCode(String response) {
        String[] split = response.split(Constants.REQUEST_RESPONSE_KEYS.KEY_SEPERATOR);
        if (split.length >= 4) {
            String tmp[] = split[3].split("\\" + Constants.REQUEST_RESPONSE_KEYS.VALUE_SEPERATOR, 2);
            if (tmp.length == 2 && Constants.REQUEST_RESPONSE_KEYS.REQUESTER_ID.equals(tmp[0])) {
                return tmp[1];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
	 * Returns the SUB_ACTION value in response
	 * @param response
	 * @return
	 */
    private String extractRequest(String response) {
        String[] split = response.split(Constants.REQUEST_RESPONSE_KEYS.KEY_SEPERATOR);
        if (split.length >= 5) {
            String tmp[] = split[4].split("\\" + Constants.REQUEST_RESPONSE_KEYS.VALUE_SEPERATOR, 2);
            if (tmp.length == 2 && Constants.REQUEST_RESPONSE_KEYS.SUB_ACTION.equals(tmp[0])) {
                return tmp[1];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean errorHandler(boolean isFatal, Throwable t, String msg, boolean showStackTrace) {
        boolean isCritical = false;
        if (showStackTrace) {
        }
        if (t instanceof UnknownHostException) {
            isFatal = true;
        } else if (t instanceof CrossPatherException) {
            if (ERROR_CODES.PROXY_AUTHENTICATION_REQUIRED.equals(t.getMessage())) {
                isFatal = true;
            }
        }
        failure_count++;
        if (lastException == null || !lastException.toString().equals(t.toString())) {
            logger.doLog(AppLogger.WARN, "First failure due to: " + t + "\nDetail:" + msg, t);
            lastException = t;
        } else if (isFatal && failure_count >= FATAL_FAILURE_NOTICE_LIMIT) {
            logger.doLog(AppLogger.ERROR, "Failed " + FATAL_FAILURE_NOTICE_LIMIT + " consecutive times due to fatal issue " + t, t);
            failure_count = 0;
            lastException = null;
            fileServerListener.jobFailed(t);
            isCritical = true;
        } else if (failure_count >= FAILURE_NOTICE_LIMIT) {
            logger.doLog(AppLogger.ERROR, "Failed " + FAILURE_NOTICE_LIMIT + " consecutive times due to " + t, t);
            failure_count = 0;
            lastException = null;
            fileServerListener.jobFailed(t);
            isCritical = true;
        } else if (failure_count % 20 == 0) {
            logger.doLog(AppLogger.DEBUG, "Rotating proxy.", null);
            props.nextProxy();
            readHttpClient = HTTPHelper.prepareHttpClient();
            writeHttpClient = HTTPHelper.prepareHttpClient();
        } else if (failure_count % 10 == 0) {
            logger.doLog(AppLogger.DEBUG, "Sleeping for " + multiFailSleepTime + " secs after " + failure_count + " failures.", null);
            multiFailSleepTime += ONE_MINUTES_IN_SEC;
            if (multiFailSleepTime > (ONE_MINUTES_IN_SEC * 10)) {
                multiFailSleepTime -= ONE_MINUTES_IN_SEC;
            }
            fileServerListener.activateSleepMode(multiFailSleepTime);
        }
        if (failure_count == 0) {
            multiFailSleepTime = 0;
        }
        return isCritical;
    }

    private boolean fileSendErrorHandler(boolean isFatal, Throwable t, String msg, boolean showStackTrace, String key) {
        boolean isCritical = false;
        if (showStackTrace) {
        }
        fileSendFailureCount++;
        if (lastFileSendException == null || !lastFileSendException.toString().equals(t.toString())) {
            logger.doLog(AppLogger.WARN, "First failure due to: " + t + "\nDetail:" + msg, t);
            lastFileSendException = t;
            fileServerListener.updateSubStatus("First failure due to " + t.getMessage());
        } else if (isFatal && fileSendFailureCount >= FATAL_FAILURE_NOTICE_LIMIT) {
            fileSendFailureCount = 0;
            lastFileSendException = null;
            isCritical = true;
            logger.doLog(AppLogger.ERROR, "Aborting current operation due to " + FATAL_FAILURE_NOTICE_LIMIT + " consecutive errors - " + t.getMessage(), t);
            fileServerListener.updateSubStatus("Aborting current operation due to " + FATAL_FAILURE_NOTICE_LIMIT + " consecutive errors - " + t.getMessage());
        } else if (fileSendFailureCount >= FAILURE_NOTICE_LIMIT) {
            fileSendFailureCount = 0;
            lastFileSendException = null;
            isCritical = true;
            logger.doLog(AppLogger.ERROR, "Aborting current operation due to " + FAILURE_NOTICE_LIMIT + " consecutive errors - " + t.getMessage(), t);
            fileServerListener.updateSubStatus("Aborting current operation due to " + FAILURE_NOTICE_LIMIT + " consecutive errors - " + t.getMessage());
        } else {
            fileServerListener.updateSubStatus("Consecutive failure no:" + fileSendFailureCount + " due to " + t.getMessage());
        }
        if (isCritical && key != null && t != null) {
            try {
                int statusCode = doSimpleHttpWrite(key, "ERROR: Failed due to:" + t.getMessage(), prepareWriteHttpClient());
                if (statusCode != HttpStatus.SC_OK) {
                    logger.doLog(AppLogger.INFO, "Successfully updated server with failure reason", null);
                } else {
                    logger.doLog(AppLogger.WARN, "Error while sending failure reason to server, HTTPCode: " + statusCode, null);
                }
            } catch (IOException e) {
                logger.doLog(AppLogger.WARN, "Error while sending failure reason to server", e);
            }
        }
        return isCritical;
    }

    private Map<String, String> populateResponseIntoMap(String response) {
        Map<String, String> responseMap = new HashMap<String, String>();
        String[] split = response.split(Constants.REQUEST_RESPONSE_KEYS.KEY_SEPERATOR);
        for (String properties : split) {
            String[] property = properties.split("\\" + Constants.REQUEST_RESPONSE_KEYS.VALUE_SEPERATOR);
            if (Constants.REQUEST_RESPONSE_KEYS.SUB_ACTION.equals(property[0])) {
                if (property.length > 1) {
                    String[] params = property[1].split("\\" + Constants.REQUEST_RESPONSE_KEYS.PARAM_SEPERATOR);
                    responseMap.put(property[0], params[0]);
                    for (int i = 1; i < params.length; i++) {
                        responseMap.put(Constants.REQUEST_RESPONSE_KEYS.PARAMETER + (i - 1), params[i]);
                    }
                }
            } else if (property.length > 1) {
                responseMap.put(property[0], property[1]);
            }
        }
        return responseMap;
    }
}
