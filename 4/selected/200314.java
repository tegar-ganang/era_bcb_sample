package net.mikaboshi.servlet.monitor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.Cookie;
import net.mikaboshi.csv.CSVStrategy;
import net.mikaboshi.csv.StandardCSVStrategy;
import net.mikaboshi.log.SimpleFileLogger;
import net.mikaboshi.util.MkCollectionUtils;
import net.mikaboshi.util.ObjectDescriber;
import net.mikaboshi.validator.SimpleValidator;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

public class LogWriter {

    public static final String NULL_VALUE = Character.toString((char) 0);

    public static final String END_OF_LINE = Character.toString((char) 0x1E);

    private SimpleFileLogger logger;

    private CSVStrategy csvStrategy = new StandardCSVStrategy();

    private static boolean invalidHttpOnly = false;

    public LogWriter(SimpleFileLogger logger) {
        SimpleValidator.validateNotNull(logger, "logger");
        this.logger = logger;
        this.csvStrategy.setNullString(NULL_VALUE);
    }

    public void write(LogEntry logEntry) throws IOException {
        SimpleValidator.validateNotNull(logEntry, "logEntry");
        String logId = logEntry.getLogId() == null ? "" : logEntry.getLogId();
        synchronized (this.logger) {
            write(logId, LogType.DATE, logEntry.getDate(), false);
            write(logId, LogType.TIME, logEntry.getTime(), false);
            write(logId, LogType.THREAD_ID, logEntry.getThreadId(), false);
            write(logId, LogType.THREAD_NAME, logEntry.getThreadName(), true);
            write(logId, LogType.METHOD, logEntry.getMethod(), false);
            write(logId, LogType.REQUEST_URL, logEntry.getRequestUrl(), true);
            write(logId, LogType.PROTOCOL, logEntry.getProtocol(), true);
            writeArrayMap(logId, LogType.REQUEST_PARAMETERS, logEntry.getRequestParameters());
            writeListMap(logId, LogType.REQUEST_HEADERS, logEntry.getRequestHeaders());
            writeCookies(logId, LogType.REQUEST_COOKIES, logEntry.getRequestCookies());
            write(logId, LogType.REQUEST_CONTENT_LENGTH, logEntry.getRequestContentLength(), false);
            write(logId, LogType.REQUEST_CHARACTER_ENCODING, logEntry.getRequestCharacterEncoding(), false);
            write(logId, LogType.REQUEST_CONTENT_TYPE, logEntry.getRequestContentType(), true);
            write(logId, LogType.REMOTE_ADDR, logEntry.getRemoteAddr(), false);
            write(logId, LogType.REMOTE_HOST, logEntry.getRemoteHost(), false);
            write(logId, LogType.REMOTE_PORT, logEntry.getRemotePort(), false);
            writeList(logId, LogType.REQUEST_LOCALES, logEntry.getRequestLocales());
            write(logId, LogType.AUTH_TYPE, logEntry.getAuthType(), true);
            write(logId, LogType.USER_PRINCIPAL, logEntry.getUserPrincipal(), true);
            write(logId, LogType.SESSION_ID, logEntry.getSessionId(), true);
            this.logger.flush();
            LogMode logMode = LogMode.getInstance();
            try {
                writeAttributes(logId, LogType.REQUEST_ATTRIBUTES_BEFORE, logEntry.getRequestAttributesBefore(), logMode.getRequestAttributeDescribeModeAsEnum());
                writeAttributes(logId, LogType.REQUEST_ATTRIBUTES_AFTER, logEntry.getRequestAttributesAfter(), logMode.getRequestAttributeDescribeModeAsEnum());
                writeAttributes(logId, LogType.SESSION_ATTRIBUTES_BEFORE, logEntry.getSessionAttributesBefore(), logMode.getSessionAttributeDescribeModeAsEnum());
                writeAttributes(logId, LogType.SESSION_ATTRIBUTES_AFTER, logEntry.getSessionAttributesAfter(), logMode.getSessionAttributeDescribeModeAsEnum());
                writeAttributes(logId, LogType.CONTEXT_ATTRIBUTES_BEFORE, logEntry.getContextAttributesBefore(), logMode.getContextAttributeDescribeModeAsEnum());
                writeAttributes(logId, LogType.CONTEXT_ATTRIBUTES_AFTER, logEntry.getContextAttributesAfter(), logMode.getContextAttributeDescribeModeAsEnum());
            } catch (IllegalArgumentException e) {
                throw new IOException(e);
            } catch (IllegalAccessException e) {
                throw new IOException(e);
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
            writeListMap(logId, LogType.RESPONSE_HEADERS, logEntry.getResponseHeaders());
            writeCookies(logId, LogType.RESPONSE_COOKIES, logEntry.getResponseCookies());
            write(logId, LogType.RESPONSE_CONTENT_LENGTH, logEntry.getResponseContentLength(), false);
            write(logId, LogType.RESPONSE_CHARACTER_ENCODING, logEntry.getResponseCharacterEncoding(), false);
            write(logId, LogType.RESPONSE_CONTENT_TYPE, logEntry.getResponseContentType(), false);
            write(logId, LogType.RESPONSE_LOCALE, logEntry.getResponseLocale(), false);
            write(logId, LogType.STATUS_CODE, logEntry.getStatusCode(), false);
            write(logId, LogType.STATUS_MESSAGE, logEntry.getStatusMessage(), true);
            this.logger.flush();
            write(logId, LogType.RESPONSE_BODY, logEntry.getResponseBody(), true);
            this.logger.flush();
            write(logId, LogType.EXCEPTION, logEntry.getException(), true);
            this.logger.flush();
            write(logId, LogType.ELAPSED_TIME, logEntry.getElapsedTime(), false);
            write(logId, LogType.RESULT, logEntry.getResult(), false);
            this.logger.flush();
            String log = new StringBuilder().append(logId).append(this.csvStrategy.getDelimiter()).append(LogType.END).append(this.csvStrategy.getDelimiter()).append(END_OF_LINE).toString();
            this.logger.put(log);
            this.logger.flush();
        }
    }

    private void write(String logId, LogType logType, Object value, boolean escape) throws IOException {
        if (value == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(logId);
        sb.append(this.csvStrategy.getDelimiter());
        sb.append(logType);
        sb.append(this.csvStrategy.getDelimiter());
        if (escape) {
            sb.append(this.csvStrategy.escape(value));
        } else {
            sb.append(value);
        }
        sb.append(this.csvStrategy.getDelimiter());
        sb.append(END_OF_LINE);
        this.logger.put(sb.toString());
    }

    private void writeArrayMap(String logId, LogType logType, Map<String, String[]> values) throws IOException {
        if (values == null) {
            return;
        }
        for (Map.Entry<String, String[]> entry : values.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(logId);
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(logType);
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(this.csvStrategy.escape(entry.getKey()));
            for (String value : entry.getValue()) {
                sb.append(this.csvStrategy.getDelimiter());
                sb.append(this.csvStrategy.escape(value));
            }
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(END_OF_LINE);
            this.logger.put(sb.toString());
        }
    }

    private void writeListMap(String logId, LogType logType, Map<String, List<String>> values) throws IOException {
        if (values == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(logId);
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(logType);
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(this.csvStrategy.escape(entry.getKey()));
            for (String value : entry.getValue()) {
                sb.append(this.csvStrategy.getDelimiter());
                sb.append(this.csvStrategy.escape(value));
            }
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(END_OF_LINE);
            this.logger.put(sb.toString());
        }
    }

    private void writeCookies(String logId, LogType logType, List<Cookie> cookies) throws IOException {
        if (MkCollectionUtils.isNullOrEmpty(cookies)) {
            return;
        }
        for (Cookie cookie : cookies) {
            StringBuilder sb = new StringBuilder();
            sb.append(logId);
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(logType);
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(this.csvStrategy.escape(cookie.getName()));
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(this.csvStrategy.escape(cookie.getValue()));
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(this.csvStrategy.escape(cookie.getDomain()));
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(this.csvStrategy.escape(cookie.getPath()));
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(cookie.getMaxAge());
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(cookie.getSecure());
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(cookie.getVersion());
            sb.append(this.csvStrategy.getDelimiter());
            if (!invalidHttpOnly) {
                try {
                    sb.append(PropertyUtils.getProperty(cookie, "httpOnly"));
                } catch (IllegalAccessException e) {
                    sb.append(StringUtils.EMPTY);
                    invalidHttpOnly = true;
                } catch (InvocationTargetException e) {
                    sb.append(StringUtils.EMPTY);
                    invalidHttpOnly = true;
                } catch (NoSuchMethodException e) {
                    sb.append(StringUtils.EMPTY);
                    invalidHttpOnly = true;
                }
            }
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(this.csvStrategy.escape(cookie.getComment()));
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(END_OF_LINE);
            this.logger.put(sb.toString());
        }
    }

    private void writeList(String logId, LogType logType, List<?> values) throws IOException {
        if (values == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(logId);
        sb.append(this.csvStrategy.getDelimiter());
        sb.append(logType);
        for (Object value : values) {
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(this.csvStrategy.escape(value));
        }
        sb.append(this.csvStrategy.getDelimiter());
        sb.append(END_OF_LINE);
        this.logger.put(sb.toString());
    }

    private void writeAttributes(String logId, LogType logType, Map<String, Object> values, LogMode.AttributeDescribeMode mode) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (values == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(logId);
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(logType);
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(this.csvStrategy.escape(entry.getKey()));
            sb.append(this.csvStrategy.getDelimiter());
            ObjectDescriber describer = null;
            if (mode != null) {
                switch(mode) {
                    case FIELD:
                        describer = new ObjectDescriber(ObjectDescriber.Mode.FIELD);
                        break;
                    case FIELD_RECURSIVE:
                        describer = new ObjectDescriber(ObjectDescriber.Mode.FIELD_RECURSIVE);
                        break;
                    case ACCESSOR:
                        describer = new ObjectDescriber(ObjectDescriber.Mode.ACCESSOR);
                        break;
                    case ACCESSOR_RECURSIVE:
                        describer = new ObjectDescriber(ObjectDescriber.Mode.ACCESSOR_RECURSIVE);
                        break;
                    default:
                }
            }
            if (describer != null) {
                sb.append(this.csvStrategy.escape(describer.toString(entry.getValue())));
            } else {
                sb.append(this.csvStrategy.escape(entry.getValue()));
            }
            sb.append(this.csvStrategy.getDelimiter());
            sb.append(END_OF_LINE);
            this.logger.put(sb.toString());
            this.logger.flush();
        }
    }
}
