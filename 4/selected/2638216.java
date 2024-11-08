package com.jaeksoft.searchlib.template;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import com.jaeksoft.searchlib.SearchLibException;

public abstract class TemplateAbstract {

    private final String rootPath;

    private final String[] resources;

    private final String publicName;

    private final String description;

    protected TemplateAbstract(String rootPath, String[] resources, String publicName, String description) {
        this.rootPath = rootPath;
        this.resources = resources;
        this.publicName = publicName;
        this.description = description;
    }

    public String getPublicName() {
        return publicName;
    }

    public String getDescription() {
        return description;
    }

    public void createIndex(File indexDir) throws SearchLibException, IOException {
        if (!indexDir.mkdir()) throw new SearchLibException("directory creation failed (" + indexDir + ")");
        InputStream is = null;
        FileWriter target = null;
        for (String resource : resources) {
            String res = rootPath + '/' + resource;
            is = getClass().getResourceAsStream(res);
            if (is == null) is = getClass().getResourceAsStream("common" + '/' + resource);
            if (is == null) throw new SearchLibException("Unable to find resource " + res);
            try {
                File f = new File(indexDir, resource);
                if (f.getParentFile() != indexDir) f.getParentFile().mkdirs();
                target = new FileWriter(f);
                IOUtils.copy(is, target);
            } finally {
                if (target != null) target.close();
                if (is != null) is.close();
            }
        }
    }
}
