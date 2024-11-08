package org.vardb.util.services;

import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.vardb.util.CException;
import org.vardb.util.CFileHelper;
import org.vardb.util.cache.ICacheService;
import org.vardb.util.web.CMessageWriter;
import org.vardb.util.xml.CXmlHelper;
import org.vardb.util.xml.CXmlValidationException;

@Transactional(readOnly = false)
public abstract class CAbstractXmlLoaderServiceImpl implements IXmlLoaderService {

    @Resource(name = "cacheService")
    protected ICacheService cacheService;

    protected abstract IXmlDataReader getReader(CMessageWriter writer);

    public void loadXml(String xml, CMessageWriter writer) {
        try {
            getReader(writer).loadXml(xml);
        } catch (Exception e) {
            CFileHelper.writeFile("c:/setup.xml", xml);
            throw new CException(e);
        }
    }

    public void loadXmlFromFile(String filename, CMessageWriter writer) {
        loadXml(CFileHelper.readFile(filename), writer);
        this.cacheService.clearCache();
        writer.message("Finished loading resources from file: " + filename);
    }

    public void loadXmlFromFolder(String folder, CMessageWriter writer) {
        String xml = CXmlHelper.mergeXmlFiles(folder, getReader(writer).getRoot());
        loadXml(xml, writer);
        this.cacheService.clearCache();
        writer.message("Finished loading resources from folder: " + folder);
    }

    public void loadXmlFromFolder(String folder, Date date, CMessageWriter writer) {
        if (date != null) System.out.println("loading files updated since " + date + " (" + date.getTime() + ")");
        String xml = CXmlHelper.mergeXmlFiles(folder, getReader(writer).getRoot(), date);
        loadXml(xml, writer);
        this.cacheService.clearCache();
        writer.message("Finished loading resources from folder: " + folder);
    }

    public void validate(String xml, String schema, CMessageWriter writer) {
        try {
            String xsd = CFileHelper.getResource(schema);
            CXmlHelper.validate(xml, xsd);
        } catch (CXmlValidationException e) {
            writer.message(e.getMessage());
        }
    }

    public void validateFolder(String folder, String schema, CMessageWriter writer) {
        List<String> filenames = CFileHelper.listFilesRecursively(folder, ".xml");
        for (String filename : filenames) {
            String xml = CFileHelper.readFile(filename);
            validate(xml, schema, writer);
        }
    }
}
