package org.bbop.dataadapter;

import java.util.*;
import org.apache.log4j.*;

public class FileAdapterConfiguration implements AdapterConfiguration {

    protected static final Logger logger = Logger.getLogger(FileAdapterConfiguration.class);

    protected int maxReadHistorySize;

    protected int maxWriteHistorySize;

    protected Collection<String> readPaths = new Vector<String>();

    protected String writePath;

    protected String readPath;

    public FileAdapterConfiguration(String readFile) {
        this();
        readPaths.add(readFile);
    }

    public FileAdapterConfiguration() {
    }

    public Collection<String> getReadPaths() {
        return readPaths;
    }

    public void setReadPaths(Collection<String> c) {
        this.readPaths = c;
    }

    public void setReadPath(String path) {
        this.readPath = path;
    }

    public void setWritePath(String path) {
        this.writePath = path;
    }

    public String getWritePath() {
        return writePath;
    }

    @Override
    public String toString() {
        return "readPaths = " + readPaths + "; writePath = " + writePath;
    }
}
