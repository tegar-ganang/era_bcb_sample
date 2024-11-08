package mkk.princess.infrastructure.ui;

import mkk.princess.core.shared.LogHelper;
import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.WriterAppender;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;

/**
 * Do output log4j content to UI component action.
 *
 * @author Shengzhao Li
 */
public abstract class Log4jUIAppender extends WriterAppender {

    private static final LogHelper log = LogHelper.create(Log4jUIAppender.class);

    /**
     * Log4j console appender name, default
     */
    protected static final String CONSOLE_APPENDER_NAME = "stdout";

    protected String appenderName;

    private PipedReader reader;

    protected Log4jUIAppender() {
        this.appenderName = CONSOLE_APPENDER_NAME;
    }

    /**
     * Construct by specify <code>appenderName</code> .
     * The <code>appenderName</code>  from <i>log4j</i>  configuration.
     *
     * @param appenderName appenderName
     */
    protected Log4jUIAppender(String appenderName) {
        this.appenderName = appenderName;
        log.info("Create by [" + this.appenderName + "]");
    }

    protected void initReader() {
        this.reader = new PipedReader();
    }

    protected void append() throws IOException {
        Logger rootLogger = LogManager.getRootLogger();
        Appender appender = rootLogger.getAppender(appenderName);
        if (appender == null) {
            log.info("Null appender get by [" + appenderName + "]");
            return;
        }
        if (appender instanceof WriterAppender) {
            Writer writer = new PipedWriter(this.reader);
            WriterAppender writerAppender = (WriterAppender) appender;
            writerAppender.setWriter(writer);
        } else {
            log.info("UnSupport appender [" + appender + "],current support 'WriterAppender' or it's subclass");
        }
    }

    public String appenderName() {
        return appenderName;
    }

    public void appenderName(String appenderName) {
        this.appenderName = appenderName;
    }

    /**
     * Call it get Log4j output reader.
     *
     * @return PipedReader
     * @throws java.io.IOException IOException
     */
    public PipedReader reader() throws IOException {
        if (this.reader == null) {
            initReader();
            append();
        }
        return this.reader;
    }
}
