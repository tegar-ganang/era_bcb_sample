package com.softwarementors.extjs.djn.router.processor.standard.form.simple;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import com.softwarementors.extjs.djn.api.Registry;
import com.softwarementors.extjs.djn.config.GlobalConfiguration;
import com.softwarementors.extjs.djn.router.dispatcher.Dispatcher;
import com.softwarementors.extjs.djn.router.processor.RequestProcessorUtils;
import com.softwarementors.extjs.djn.router.processor.standard.form.FormPostRequestProcessorBase;

public class SimpleFormPostRequestProcessor extends FormPostRequestProcessorBase {

    private static final Logger logger = Logger.getLogger(SimpleFormPostRequestProcessor.class);

    public SimpleFormPostRequestProcessor(Registry registry, Dispatcher dispatcher, GlobalConfiguration globalConfiguration) {
        super(registry, dispatcher, globalConfiguration);
    }

    public void process(Reader reader, Writer writer) throws IOException {
        String requestString = IOUtils.toString(reader);
        if (logger.isDebugEnabled()) {
            logger.debug("Request data (SIMPLE FORM)=>" + requestString);
        }
        Map<String, String> formParameters;
        formParameters = RequestProcessorUtils.getDecodedRequestParameters(requestString);
        String result = process(formParameters, new HashMap<String, FileItem>());
        writer.write(result);
        if (logger.isDebugEnabled()) {
            logger.debug("ResponseData data (SIMPLE FORM)=>" + result);
        }
    }
}
