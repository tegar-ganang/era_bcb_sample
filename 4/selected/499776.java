package com.apachetune.core.ui.feedbacksystem.impl;

import com.apachetune.core.AppManager;
import com.apachetune.core.ui.feedbacksystem.RemoteException;
import com.apachetune.core.ui.feedbacksystem.RemoteManager;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import java.io.*;
import static com.apachetune.core.Constants.REMOTE_FEEDBACK_SERVICE_URL_PROP;
import static com.apachetune.core.Constants.VELOCITY_LOG4J_APPENDER_NAME;
import static com.apachetune.core.utils.Utils.createRuntimeException;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.apache.commons.httpclient.params.HttpMethodParams.RETRY_HANDLER;
import static org.apache.commons.lang.Validate.isTrue;

/**
 * FIXDOC
 */
public class RemoteManagerImpl implements RemoteManager {

    private final String remoteFeedbackServiceUrl;

    private final AppManager appManager;

    @Inject
    public RemoteManagerImpl(@Named(REMOTE_FEEDBACK_SERVICE_URL_PROP) String remoteFeedbackServiceUrl, AppManager appManager) {
        this.remoteFeedbackServiceUrl = remoteFeedbackServiceUrl;
        this.appManager = appManager;
    }

    @Override
    public final void sendUserFeedback(String userEMail, String userMessage) throws RemoteException {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(remoteFeedbackServiceUrl);
        method.getParams().setParameter(RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
        method.setQueryString("action=send-user-feedback");
        method.setRequestHeader(new Header("Content-Type", "text/xml; charset=UTF-8"));
        String requestBody = prepareUserFeedbackRequestBody(userEMail, userMessage);
        try {
            method.setRequestEntity(new ByteArrayRequestEntity(requestBody.getBytes("UTF-8"), "text/html"));
        } catch (UnsupportedEncodingException e) {
            throw createRuntimeException(e);
        }
        int resultCode;
        try {
            resultCode = client.executeMethod(method);
            if (resultCode != SC_OK) {
                throw new RemoteException("Remote service returned non successful result [resultCode=" + resultCode + ']');
            }
        } catch (IOException e) {
            throw new RemoteException(e);
        }
        method.releaseConnection();
    }

    private String prepareUserFeedbackRequestBody(String userEMail, String userMessage) {
        VelocityContext ctx = new VelocityContext();
        ctx.put("appFullName", appManager.getFullAppName());
        ctx.put("appInstallationUid", appManager.getAppInstallationUid());
        ctx.put("userEMail", userEMail);
        try {
            ctx.put("base64EncodedUserMessage", encodeBase64String(userMessage.getBytes("UTF-8")).trim());
        } catch (UnsupportedEncodingException e) {
            throw createRuntimeException(e);
        }
        Reader reader;
        try {
            reader = new InputStreamReader(getClass().getResourceAsStream("user_feedback_request.xml.vm"), "UTF-8");
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
}
