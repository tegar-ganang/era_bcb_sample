package com.softwarementors.extjs.djn.router.processor.poll;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import com.softwarementors.extjs.djn.StringUtils;
import com.softwarementors.extjs.djn.api.RegisteredMethod;
import com.softwarementors.extjs.djn.api.RegisteredPollMethod;
import com.softwarementors.extjs.djn.api.Registry;
import com.softwarementors.extjs.djn.config.GlobalConfiguration;
import com.softwarementors.extjs.djn.router.dispatcher.Dispatcher;
import com.softwarementors.extjs.djn.router.processor.RequestException;
import com.softwarementors.extjs.djn.router.processor.RequestProcessorBase;
import com.softwarementors.extjs.djn.router.processor.RequestProcessorUtils;
import com.softwarementors.extjs.djn.router.processor.ResponseData;

public class PollRequestProcessor extends RequestProcessorBase {

    private static Logger logger = Logger.getLogger(PollRequestProcessor.class);

    public static final String PATHINFO_POLL_PREFIX = "/poll/";

    private String eventName;

    private String requestString;

    private String resultString;

    public PollRequestProcessor(Registry registry, Dispatcher dispatcher, GlobalConfiguration globalConfiguration) {
        super(registry, dispatcher, globalConfiguration);
    }

    private String getEventName(String pathInfo) {
        assert !StringUtils.isEmpty(pathInfo);
        return pathInfo.replace(PATHINFO_POLL_PREFIX, "");
    }

    protected Logger getLogger() {
        return logger;
    }

    protected void logRequestEnterInfo() {
        getLogger().debug("Request data (POLL)=>" + this.requestString + " Event name='" + this.eventName + "'");
    }

    protected void logRequestExitInfo(Logger logger) {
        logger.debug("ResponseData data (POLL)=>" + this.resultString);
    }

    protected RegisteredMethod getMethod() {
        RegisteredPollMethod method = getRegistry().getPollMethod(this.eventName);
        if (method == null) {
            RequestException ex = RequestException.forPollEventNotFound(this.eventName);
            logger.error(ex.getMessage(), ex);
            throw ex;
        }
        return method;
    }

    protected Object[] getParameters() {
        return new Object[] { RequestProcessorUtils.getDecodedRequestParameters(this.requestString) };
    }

    protected ResponseData createSuccessResponse(Object result) {
        PollSuccessResponseData r = new PollSuccessResponseData(this.eventName);
        r.setResult(result);
        return r;
    }

    protected ResponseData createErrorResponse(String message, String where) {
        PollErrorResponseData result = new PollErrorResponseData();
        result.setMessageAndWhere(message, where);
        return result;
    }

    protected void logErrorResponse(Throwable t) {
        assert t != null;
        getLogger().error("(Controlled) server error: " + t.getMessage() + " for Poll Event '" + this.eventName + "'", t);
    }

    private void logEnterInfo() {
        if (getLogger().isDebugEnabled()) {
            logRequestEnterInfo();
        }
    }

    private void logExitInfo() {
        if (getLogger().isDebugEnabled()) {
            logRequestExitInfo(logger);
        }
    }

    public void process(Reader reader, Writer writer, String pathInfo) throws IOException {
        assert !StringUtils.isEmpty(pathInfo);
        this.requestString = IOUtils.toString(reader);
        this.eventName = getEventName(pathInfo);
        logEnterInfo();
        ResponseData response;
        try {
            RegisteredMethod method = getMethod();
            Object[] parameters = getParameters();
            Object result = getDispatcher().dispatch(method, parameters);
            response = createSuccessResponse(result);
        } catch (Throwable t) {
            Throwable reportedException = RequestProcessorUtils.getExceptionToReport(t);
            String message = RequestProcessorUtils.getExceptionMessage(reportedException);
            String where = RequestProcessorUtils.getExceptionWhere(reportedException, getDebug());
            response = createErrorResponse(message, where);
            logErrorResponse(t);
        }
        StringBuilder result = new StringBuilder();
        appendIndividualResponseJsonString(response, result);
        this.resultString = result.toString();
        writer.write(this.resultString);
        logExitInfo();
    }
}
