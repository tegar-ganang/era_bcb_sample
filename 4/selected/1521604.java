package org.gbif.namefinder.action;

import org.gbif.namefinder.FileUtils;
import org.gbif.namefinder.model.Name;
import org.gbif.namefinder.model.Name.Container;
import org.gbif.utils.file.InputStreamUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

public class FinderAction extends SimpleAction {

    private static final AtomicInteger docIndex = new AtomicInteger(0);

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String service = "ruby";

    private String url;

    private String text;

    private File file;

    private String fileContentType;

    private String fileFileName;

    private String serviceUrl;

    private List<String> testUrls;

    private Container container = new Container();

    private InputStreamUtils isu = new InputStreamUtils();

    public void callFinderService(String params) throws IOException {
        if (service != null && service.equalsIgnoreCase("ruby")) {
            serviceUrl = tf;
        } else {
            serviceUrl = ws + "/indexer";
        }
        String ws = serviceUrl + "?format=json&" + params;
        log.debug("Calling finder service " + ws);
        try {
            URL youAreEll = new URL(ws);
            BufferedInputStream in = new BufferedInputStream(youAreEll.openStream());
            String jsonString = isu.readEntireStream(in, "utf8");
            in.close();
            container = mapper.readValue(jsonString, Container.class);
            log.info("Found " + container.getNames().size() + " names in " + params);
        } catch (Exception e) {
            addActionError("Error calling the finder service " + url);
            addActionError(e.getMessage());
            log.error("Error calling the finder service " + url, e);
        }
    }

    @Override
    public String execute() throws IOException {
        if (url != null) {
            callFinderService("type=url&input=" + URLEncoder.encode(url, "utf8"));
            return SUCCESS;
        } else if (file != null) {
            String uploadedDoc = upload();
            callFinderService("type=url&input=" + URLEncoder.encode(uploadedDoc, "utf8"));
            return SUCCESS;
        } else if (text != null) {
            callFinderService("type=text&input=" + URLEncoder.encode(text, "utf8"));
            return SUCCESS;
        } else {
            try {
                URL u = new URL(baseURL + "/testdata/finderpages.txt");
                InputStream testpages = new BufferedInputStream(u.openStream());
                testUrls = FileUtils.streamToList(testpages);
            } catch (Exception e) {
                addActionMessage("Cant read finder test urls");
                log.warn("Cant read finder test urls from data-dir", e);
            }
            return INPUT;
        }
    }

    public File getFile() {
        return file;
    }

    public String getFileContentType() {
        return fileContentType;
    }

    public String getFileFileName() {
        return fileFileName;
    }

    public List<Name> getNames() {
        return container.getNames();
    }

    public String getService() {
        return service;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public List<String> getTestUrls() {
        return testUrls;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setFileContentType(String fileContentType) {
        this.fileContentType = fileContentType;
    }

    public void setFileFileName(String fileFileName) {
        this.fileFileName = fileFileName;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setText(String text) {
        this.text = StringUtils.trimToNull(text);
    }

    public void setUrl(String url) {
        this.url = StringUtils.trimToNull(url);
    }

    public void setUrlExample(String urlExample) {
        if (!StringUtils.trimToEmpty(urlExample).equalsIgnoreCase("none")) {
            this.url = StringUtils.trimToNull(urlExample);
        }
    }

    public String upload() throws IOException {
        int idx = docIndex.incrementAndGet();
        String tmpName = "namefinder/doc_" + idx + "__" + fileFileName;
        File tmpFile = tmpFile(tmpName);
        if (tmpFile.exists()) {
            org.apache.commons.io.FileUtils.deleteQuietly(tmpFile);
        }
        org.apache.commons.io.FileUtils.touch(tmpFile);
        InputStream fileStream = new FileInputStream(file);
        OutputStream bos = new FileOutputStream(tmpFile);
        IOUtils.copy(fileStream, bos);
        bos.close();
        fileStream.close();
        return tmpUrl(tmpName);
    }
}
