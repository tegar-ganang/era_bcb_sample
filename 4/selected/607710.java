package com.apachetune.core.errorreportsystem;

import com.apachetune.core.AppManager;
import com.apachetune.core.AppVersion;
import com.apachetune.core.ResourceManager;
import com.apachetune.core.impl.AppManagerImpl;
import com.apachetune.core.preferences.Preferences;
import com.apachetune.core.preferences.PreferencesManager;
import com.apachetune.core.preferences.impl.PreferencesManagerImpl;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.mutable.MutableObject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jdesktop.swingx.JXHyperlink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.prefs.BackingStoreException;
import static com.apachetune.core.Constants.*;
import static com.apachetune.core.utils.Utils.createRuntimeException;
import static com.apachetune.core.utils.Utils.gzip;
import static com.apachetune.main.MainModule.REMOTE_REPORTING_SERVICE;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static javax.swing.JOptionPane.showInputDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.apache.commons.httpclient.params.HttpMethodParams.RETRY_HANDLER;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notNull;
import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;
import static org.apache.velocity.runtime.RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS;

/**
 * FIXDOC
 */
public class ErrorReportManager {

    private static final Logger logger = LoggerFactory.getLogger(ErrorReportManager.class);

    private static final ErrorReportManager INSTANCE = new ErrorReportManager();

    private static final String LOGS_PATH = "logs";

    private static final String LOG_BASE_NAME = "apachetune.log";

    private static final String SEND_ERROR_INFO_REPORT_ACTION = "send-error-info";

    private static final String SEND_APP_LOG_ACTION = "send-app-log";

    private static final String APACHETUNE_LOG_FILE_FILTER = "apachetune.log.*";

    @SuppressWarnings({ "StringConcatenation" })
    private static final String SUPPORT_PRODUCT_EMAIL_URL = "mailto:" + SUPPORT_PRODUCT_EMAIL + "?subject=Error%20report";

    private final ResourceBundle resourceBundle = ResourceManager.getInstance().getResourceBundle(ErrorReportManager.class);

    public static ErrorReportManager getInstance() {
        return INSTANCE;
    }

    public final void sendErrorReport(final Component parentComponent, final String message, final Throwable cause, final AppManager nullableAppManager, final PreferencesManager nullablePreferencesManager) {
        logger.info(format("Sending error info to remote service... [message={0};\ncause=\n{1}\n]", message, getFullStackTrace(cause)));
        try {
            final Managers managers = prepareManagers(nullableAppManager, nullablePreferencesManager);
            String lastUsedNullableUserEmail = getUserEmail(managers.getAppManager(), managers.getPreferencesManager());
            final MutableObject userEmail = new MutableObject(lastUsedNullableUserEmail);
            boolean isUserInputEmail = showAskForReporterEmailDialog(parentComponent, userEmail);
            if (isUserInputEmail) {
                storeUserEMail((String) userEmail.getValue(), nullableAppManager, nullablePreferencesManager);
            }
            final String nullableUserEmail = (String) userEmail.getValue();
            final ErrorReportDialog errorReportDialog = new ErrorReportDialog(parentComponent);
            final ExecutorService executorService = newSingleThreadExecutor();
            executorService.execute(new Runnable() {

                @Override
                public final void run() {
                    try {
                        invokeLater(new Runnable() {

                            @Override
                            public final void run() {
                                errorReportDialog.setVisible(true);
                            }
                        });
                        boolean wasError = doSendErrorReport(nullableUserEmail, message, cause, managers.getAppManager(), managers.getPreferencesManager());
                        if (wasError) {
                            invokeLater(new Runnable() {

                                @Override
                                public final void run() {
                                    errorReportDialog.setVisible(false);
                                }
                            });
                            handleReportError(parentComponent, null);
                        }
                    } catch (ErrorReportManagerException e) {
                        errorReportDialog.setVisible(false);
                        handleReportError(parentComponent, e);
                    } catch (Throwable throwable) {
                        errorReportDialog.setVisible(false);
                        handleReportError(parentComponent, new ErrorReportManagerException(throwable));
                    } finally {
                        invokeLater(new Runnable() {

                            @Override
                            public final void run() {
                                errorReportDialog.dispose();
                            }
                        });
                        executorService.shutdown();
                    }
                }
            });
        } catch (Throwable throwable) {
            handleReportError(parentComponent, new ErrorReportManagerException(throwable));
        }
    }

    public final String getUserEmail(AppManager nullableAppManager, PreferencesManager nullablePreferencesManager) {
        Managers managers = prepareManagers(nullableAppManager, nullablePreferencesManager);
        if (managers.getPreferencesManager() == null) {
            logger.warn("Cannot get userEmail because cannot create preferences manager.");
            return null;
        }
        Preferences prefs = managers.getPreferencesManager().userNodeForPackage(ErrorReportManager.class);
        return prefs.get(REMOTE_SERVICE_USER_EMAIL_PROP_NAME, null);
    }

    public final void storeUserEMail(String userEMail, AppManager nullableAppManager, PreferencesManager nullablePreferencesManager) {
        Managers managers = prepareManagers(nullableAppManager, nullablePreferencesManager);
        if (managers.getPreferencesManager() == null) {
            logger.warn(format("Cannot store userEmail because cannot create preferences manager [userEmail={0}]", "" + userEMail));
            return;
        }
        Preferences prefs = managers.getPreferencesManager().userNodeForPackage(ErrorReportManager.class);
        if ((userEMail != null) && !userEMail.isEmpty()) {
            prefs.put(REMOTE_SERVICE_USER_EMAIL_PROP_NAME, userEMail);
        } else {
            prefs.remove(REMOTE_SERVICE_USER_EMAIL_PROP_NAME);
        }
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            logger.error("Internal error", e);
        }
    }

    private boolean showAskForReporterEmailDialog(Component parentComponent, MutableObject email) {
        String result = showInputDialog(parentComponent, resourceBundle.getString("errorReportManager.showAskForReporterEmailDialog.text"), email.getValue());
        if (result == null) {
            return false;
        }
        email.setValue(!result.trim().isEmpty() ? result.trim() : null);
        return true;
    }

    private boolean doSendErrorReport(String nullableUserEmail, String message, Throwable cause, AppManager nullableAppManager, PreferencesManager nullablePreferencesManager) throws ErrorReportManagerException {
        final Managers managers = prepareManagers(nullableAppManager, nullablePreferencesManager);
        UUID nullableAppInstallationUid = (managers.getAppManager() != null) ? managers.getAppManager().getAppInstallationUid() : null;
        String nullableAppFullName = (managers.getAppManager() != null) ? managers.getAppManager().getFullAppName() : null;
        initializeVelocity();
        String errorMessageReport = prepareErrorMessageReport(nullableUserEmail, nullableAppFullName, nullableAppInstallationUid, message, cause);
        boolean wasError = false;
        try {
            postMessageToRemoteService(REMOTE_REPORTING_SERVICE, SEND_ERROR_INFO_REPORT_ACTION, errorMessageReport);
        } catch (ErrorReportManagerException e) {
            wasError = true;
            logger.error(format("Cannot send error message report [" + "remoteReportingService={0};\n" + "action={1}\n" + "message=\n{2}\n]", REMOTE_REPORTING_SERVICE, SEND_ERROR_INFO_REPORT_ACTION, errorMessageReport));
        }
        wasError |= sendAppLogs(nullableUserEmail, nullableAppFullName, nullableAppInstallationUid);
        return wasError;
    }

    private boolean sendAppLogs(String nullableUserEmail, String nullableAppFullName, UUID nullableAppInstallationUid) {
        boolean wasError = false;
        final File currentLogFile = new File(LOGS_PATH, LOG_BASE_NAME);
        if (currentLogFile.exists()) {
            wasError = sendAppLog(nullableUserEmail, nullableAppInstallationUid, nullableAppFullName, LOG_BASE_NAME, false);
        } else {
            logger.warn(format("Log file {0} is not exists.", currentLogFile.getName()));
        }
        List<File> logs = asList(new File(LOGS_PATH).listFiles((FilenameFilter) new WildcardFileFilter(APACHETUNE_LOG_FILE_FILTER)));
        for (File log : logs) {
            wasError |= sendAppLog(nullableUserEmail, nullableAppInstallationUid, nullableAppFullName, log.getName(), true);
        }
        return wasError;
    }

    private boolean sendAppLog(String nullableUserEmail, UUID nullableAppInstallationUid, String nullableAppFullName, String logFileName, boolean deleteAfterSending) {
        boolean wasError = false;
        try {
            final File logFile = new File(LOGS_PATH, logFileName);
            final FileInputStream logFileIS = new FileInputStream(logFile);
            String logFileContent = IOUtils.toString(logFileIS, "UTF-8");
            logFileIS.close();
            String message = prepareLogFileReport(nullableUserEmail, nullableAppFullName, nullableAppInstallationUid, logFileName, logFileContent);
            postMessageToRemoteService(REMOTE_REPORTING_SERVICE, SEND_APP_LOG_ACTION, message);
            if (deleteAfterSending) {
                if (!logFile.delete()) {
                    logger.warn(format("Cannot delete log file {0}.", logFile.getName()));
                }
            }
        } catch (Throwable cause) {
            wasError = true;
            logger.error(format("Error occurred during sending log file [logFileName={0}]", logFileName));
        }
        return wasError;
    }

    private void initializeVelocity() {
        Velocity.setProperty(RUNTIME_LOG_LOGSYSTEM_CLASS, VELOCITY_LOG_CLASS);
        Velocity.setProperty(VELOCITY_LOGGER_NAME_PROP, VELOCITY_LOGGER);
        try {
            Velocity.init();
        } catch (Exception e) {
            throw createRuntimeException(e);
        }
    }

    private void handleReportError(Component parentComponent, ErrorReportManagerException nullableException) {
        logger.error("Error reporting subsystem.", nullableException);
        String errMsg = MessageFormat.format(resourceBundle.getString("errorReportManager.handleReportError.errorMessage"), new File(LOGS_PATH).getAbsolutePath());
        JXHyperlink supportEmail = new JXHyperlink();
        supportEmail.setText(SUPPORT_PRODUCT_EMAIL);
        supportEmail.addActionListener(new ActionListener() {

            @Override
            public final void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(SUPPORT_PRODUCT_EMAIL_URL));
                } catch (IOException e1) {
                    logger.error("Internal error", e1);
                } catch (URISyntaxException e1) {
                    logger.error("Internal error", e1);
                }
            }
        });
        Object[] dialogPane = new Object[] { errMsg, supportEmail };
        showMessageDialog(parentComponent, dialogPane);
    }

    private Managers prepareManagers(AppManager nullableAppManager, PreferencesManager nullablePreferencesManager) {
        PreferencesManager preferencesManager = null;
        AppManager appManager = null;
        try {
            preferencesManager = (nullablePreferencesManager != null) ? nullablePreferencesManager : new PreferencesManagerProxy();
            appManager = (nullableAppManager != null) ? nullableAppManager : new AppManagerProxy(preferencesManager);
            if (preferencesManager instanceof PreferencesManagerProxy) {
                PreferencesManagerProxy preferencesManagerProxy = (PreferencesManagerProxy) preferencesManager;
                preferencesManagerProxy.setAppManager(appManager);
            }
        } catch (Throwable cause) {
            logger.error("Error creating managers.");
        }
        return new Managers(appManager, preferencesManager);
    }

    private String prepareErrorMessageReport(String nullableUserEmail, String nullableAppFullName, UUID nullableAppInstallationUid, String errorMessage, Throwable cause) {
        VelocityContext ctx = new VelocityContext();
        ctx.put("appFullName", nullableAppFullName);
        ctx.put("appInstallationUid", nullableAppInstallationUid);
        ctx.put("userEMail", nullableUserEmail);
        @SuppressWarnings({ "StringConcatenation" }) String stackTrace = errorMessage + "\n" + getFullStackTrace(cause);
        try {
            ctx.put("base64EncodedStackTrace", encodeBase64String(stackTrace.getBytes("UTF-8")).trim());
        } catch (UnsupportedEncodingException e) {
            throw createRuntimeException(e);
        }
        return fillTemplate(ctx, "send_error_info_request.xml.vm");
    }

    private String prepareLogFileReport(String nullableUserEmail, String nullableAppFullName, UUID nullableAppInstallationUid, String logFileName, String logFileContent) {
        VelocityContext ctx = new VelocityContext();
        ctx.put("appFullName", nullableAppFullName);
        ctx.put("appInstallationUid", nullableAppInstallationUid);
        ctx.put("userEMail", nullableUserEmail);
        ctx.put("logFileName", logFileName);
        byte[] gzippedLogFileContent = gzip(logFileContent);
        ctx.put("base64EncodedGzippedLogFileContent", encodeBase64String(gzippedLogFileContent));
        return fillTemplate(ctx, "send_app_log_request.xml.vm");
    }

    private String fillTemplate(VelocityContext ctx, String templateResourceName) {
        Reader reader;
        try {
            reader = new InputStreamReader(getClass().getResourceAsStream(templateResourceName), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw createRuntimeException(e);
        }
        StringWriter writer = new StringWriter();
        try {
            boolean isOk = Velocity.evaluate(ctx, writer, VELOCITY_LOG4J_APPENDER_NAME, reader);
            isTrue(isOk);
            writer.close();
            return writer.toString();
        } catch (IOException e) {
            throw createRuntimeException(e);
        }
    }

    private void postMessageToRemoteService(String serviceUrl, String action, String content) throws ErrorReportManagerException {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(serviceUrl);
        method.getParams().setParameter(RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
        method.setQueryString("action=" + action);
        method.setRequestHeader(new Header("Content-Type", "text/xml; charset=UTF-8"));
        try {
            method.setRequestEntity(new ByteArrayRequestEntity(content.getBytes("UTF-8"), "text/html"));
        } catch (UnsupportedEncodingException e) {
            throw createRuntimeException(e);
        }
        int resultCode;
        try {
            resultCode = client.executeMethod(method);
            if (resultCode != SC_OK) {
                throw new ErrorReportManagerException("Remote service returned non successful result [resultCode=" + resultCode + ']');
            }
        } catch (IOException e) {
            throw new ErrorReportManagerException(e);
        }
        method.releaseConnection();
    }

    static class Managers {

        private final AppManager appManager;

        private final PreferencesManager preferencesManager;

        public Managers(AppManager appManager, PreferencesManager preferencesManager) {
            this.appManager = appManager;
            this.preferencesManager = preferencesManager;
        }

        public final AppManager getAppManager() {
            return appManager;
        }

        public final PreferencesManager getPreferencesManager() {
            return preferencesManager;
        }
    }

    static class AppManagerProxy implements AppManager {

        private final PreferencesManager preferencesManager;

        private AppManager appManager;

        public AppManagerProxy(PreferencesManager preferencesManager) {
            notNull(preferencesManager);
            this.preferencesManager = preferencesManager;
        }

        @Override
        public final UUID getAppInstallationUid() {
            return getAppManager().getAppInstallationUid();
        }

        @Override
        public final String getName() {
            return getAppManager().getName();
        }

        @Override
        public final AppVersion getVersion() {
            return getAppManager().getVersion();
        }

        @Override
        public final Date getBuildDate() {
            return getAppManager().getBuildDate();
        }

        @Override
        public final Date getDevelopmentStartDate() {
            return getAppManager().getDevelopmentStartDate();
        }

        @Override
        public final String getVendor() {
            return getAppManager().getVendor();
        }

        @Override
        public final URL getWebSite() {
            return getAppManager().getWebSite();
        }

        @Override
        public final String getCopyrightText() {
            return getAppManager().getCopyrightText();
        }

        @Override
        public final String getFullAppName() {
            return getAppManager().getFullAppName();
        }

        @Override
        public final String getProductWebPortalUri() {
            return getAppManager().getProductWebPortalUri();
        }

        private AppManager getAppManager() {
            notNull(preferencesManager);
            if (appManager == null) {
                appManager = new AppManagerImpl(preferencesManager);
            }
            return appManager;
        }
    }

    static class PreferencesManagerProxy implements PreferencesManager {

        private AppManager appManager;

        private PreferencesManager preferencesManager;

        public final void setAppManager(AppManager appManager) {
            notNull(appManager);
            this.appManager = appManager;
        }

        @Override
        public final Preferences systemNodeForPackage(Class<?> clazz) {
            return getPreferencesManager().systemNodeForPackage(clazz);
        }

        @Override
        public final Preferences systemRoot() {
            return getPreferencesManager().systemRoot();
        }

        @Override
        public final Preferences userNodeForPackage(Class<?> clazz) {
            return getPreferencesManager().userNodeForPackage(clazz);
        }

        @Override
        public final Preferences userRoot() {
            return getPreferencesManager().userRoot();
        }

        private PreferencesManager getPreferencesManager() {
            notNull(appManager);
            if (preferencesManager == null) {
                preferencesManager = new PreferencesManagerImpl(appManager);
            }
            return preferencesManager;
        }
    }
}
