package com.softwarementors.extjs.djn.router;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;
import com.softwarementors.extjs.djn.api.Registry;
import com.softwarementors.extjs.djn.config.GlobalConfiguration;
import com.softwarementors.extjs.djn.router.dispatcher.Dispatcher;
import com.softwarementors.extjs.djn.router.processor.RequestException;
import com.softwarementors.extjs.djn.router.processor.poll.PollRequestProcessor;
import com.softwarementors.extjs.djn.router.processor.standard.form.simple.SimpleFormPostRequestProcessor;
import com.softwarementors.extjs.djn.router.processor.standard.form.upload.UploadFormPostRequestProcessor;
import com.softwarementors.extjs.djn.router.processor.standard.json.JsonRequestProcessor;

public class RequestRouter {

    static final Logger logger = Logger.getLogger(RequestRouter.class);

    private Registry registry;

    private Dispatcher dispatcher;

    private GlobalConfiguration globalConfiguration;

    public RequestRouter(Registry registry, GlobalConfiguration globalConfiguration, Dispatcher dispatcher) {
        assert registry != null;
        assert globalConfiguration != null;
        assert dispatcher != null;
        this.registry = registry;
        this.dispatcher = dispatcher;
        this.globalConfiguration = globalConfiguration;
    }

    public void processSimpleFormPostRequest(Reader reader, Writer writer) throws IOException {
        new SimpleFormPostRequestProcessor(this.registry, this.dispatcher, this.globalConfiguration).process(reader, writer);
    }

    public UploadFormPostRequestProcessor createUploadFromProcessor() {
        return new UploadFormPostRequestProcessor(this.registry, this.dispatcher, this.globalConfiguration);
    }

    public void processUploadFormPostRequest(UploadFormPostRequestProcessor processor, List<FileItem> fileItems, Writer writer) throws IOException {
        assert processor != null;
        processor.process(fileItems, writer);
    }

    public void processJsonRequest(Reader reader, Writer writer) throws IOException {
        new JsonRequestProcessor(this.registry, this.dispatcher, this.globalConfiguration).process(reader, writer);
    }

    public void processPollRequest(Reader reader, Writer writer, String pathInfo) throws IOException {
        new PollRequestProcessor(this.registry, this.dispatcher, this.globalConfiguration).process(reader, writer, pathInfo);
    }

    public void handleFileUploadException(UploadFormPostRequestProcessor processor, FileUploadException e) {
        assert e != null;
        processor.handleFileUploadException(e);
    }

    public static final String SOURCE_NAME_PREFIX = "/src";

    public void processSourceRequest(BufferedReader reader, PrintWriter writer, String pathInfo) {
        assert reader != null;
        assert writer != null;
        assert pathInfo != null;
        int lastIndex = pathInfo.lastIndexOf(SOURCE_NAME_PREFIX);
        int position = lastIndex + SOURCE_NAME_PREFIX.length();
        String sourceName = pathInfo.substring(position + 1);
        if (!this.registry.hasSource(sourceName)) {
            RequestException ex = RequestException.forSourceNotFound(sourceName);
            logger.error(sourceName);
            throw ex;
        }
        String code = this.registry.getSource(sourceName);
        assert code != null;
        writer.append(code);
    }

    public static boolean isSourceRequest(String pathInfo) {
        assert pathInfo != null;
        int lastIndex = pathInfo.lastIndexOf(SOURCE_NAME_PREFIX);
        boolean isSource = lastIndex >= 0;
        return isSource;
    }
}
