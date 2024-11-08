package org.lightmtv.response;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletResponse;
import org.lightcommons.io.IOUtils;
import org.lightcommons.resource.Resource;
import org.lightcommons.util.Assert;
import org.lightmtv.response.AbstractContentResponse;

public class StreamResponse extends AbstractContentResponse {

    private InputStream input;

    public StreamResponse(String fileName, InputStream input) {
        super();
        this.input = input;
        setFileName(fileName);
    }

    public StreamResponse(String fileName, byte[] bytes) {
        this(new ByteArrayInputStream(bytes));
        setFileName(fileName);
    }

    public StreamResponse(String fileName, File file) throws FileNotFoundException {
        super();
        setInput(new FileInputStream(file));
        setContentLength((int) file.length());
        setFileName(fileName);
    }

    public StreamResponse(String fileName, Resource resource) throws IOException {
        super();
        Assert.state(resource.exists(), resource.getDescription() + " is not exists");
        setInput(resource.getInputStream());
        setFileName(fileName);
    }

    public StreamResponse(InputStream input) {
        super();
        this.input = input;
    }

    public StreamResponse(File file) throws FileNotFoundException {
        super();
        setInput(new FileInputStream(file));
        setContentLength((int) file.length());
        setFileName(file.getName());
    }

    public StreamResponse(Resource resource) throws IOException {
        super();
        Assert.state(resource.exists(), resource.getDescription() + " is not exists");
        setInput(resource.getInputStream());
        setFileName(resource.getFilename());
    }

    public StreamResponse(byte[] bytes) {
        this(new ByteArrayInputStream(bytes));
    }

    public InputStream getInput() {
        return input;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    @Override
    protected void sendQuietly(HttpServletResponse response) throws Exception {
        IOUtils.copy(input, response.getOutputStream());
    }
}
