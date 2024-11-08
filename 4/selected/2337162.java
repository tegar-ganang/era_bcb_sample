package cn.com.once.deploytool.unit.validation.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.SchemaFactory;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import cn.com.once.deploytool.unit.parser.UnitParserException;
import cn.com.once.deploytool.unit.repository.OriginalDeployUnitDescription;
import cn.com.once.deploytool.unit.validation.UnitConfigFileSchemaValidator;
import cn.com.once.deploytool.unit.validation.UnitValidationException;
import cn.com.once.deploytool.unit.validation.ValidationMessage;
import cn.com.once.deploytool.unit.validation.ValidationReport;

public abstract class AbstractUnitConfigFileSchemaValidator implements UnitConfigFileSchemaValidator {

    private static Logger logger = Logger.getLogger(AbstractUnitConfigFileSchemaValidator.class);

    protected ErrorHandler errorHandler = null;

    public AbstractUnitConfigFileSchemaValidator() {
        addSchema();
        addConfigFile();
        registerSchema();
    }

    /**
	 * ��Ҫ�����ļ�
	 */
    protected List<String> configFiles = new ArrayList<String>();

    /**
	 * ����Ҫ��schema�ļ�
	 */
    protected List<String> schemasFile = new ArrayList<String>();

    protected List<Source> schemas = new ArrayList<Source>();

    private SAXParserFactory factory;

    /**
	 * ��ʼ����Ҫע���schema����ʼ����Ҫ�����ļ�
	 */
    protected abstract void addSchema();

    protected abstract void addConfigFile();

    private void registerSchema() {
        Iterator<String> it = this.schemasFile.iterator();
        while (it.hasNext()) {
            String schemafile = it.next();
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(schemafile);
            Source e = new SAXSource(new InputSource(inputStream));
            e.setSystemId(this.getClass().getClassLoader().getResource(schemafile).toString());
            schemas.add(e);
            logger.info(String.format("Add schema [%s] to OnceBPEL validator", e.getSystemId()));
        }
        factory = SAXParserFactory.newInstance();
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        try {
            factory.setSchema(schemaFactory.newSchema(this.schemas.toArray(new Source[0])));
        } catch (SAXException e) {
            logger.error("Something is wrong when register schema", e);
        }
    }

    /**
	 * ��֤������ʹ��ģ��ģʽ
	 * TODO �����������ʽ��֧��
	 */
    public ValidationReport validate(OriginalDeployUnitDescription unit) throws UnitValidationException {
        ValidationReport vr = new DefaultValidationReport();
        errorHandler = new SimpleErrorHandler(vr);
        vr.setFileUri(unit.getAbsolutePath());
        SAXParser parser;
        SAXReader reader = null;
        try {
            parser = factory.newSAXParser();
            reader = new SAXReader(parser.getXMLReader());
            reader.setValidation(false);
            reader.setErrorHandler(this.errorHandler);
        } catch (ParserConfigurationException e) {
            throw new UnitValidationException("The configuration of parser is illegal.", e);
        } catch (SAXException e) {
            String m = "Something is wrong when register schema";
            logger.error(m, e);
            throw new UnitValidationException(m, e);
        }
        ZipInputStream zipInputStream;
        InputStream tempInput = null;
        try {
            tempInput = new FileInputStream(unit.getAbsolutePath());
        } catch (FileNotFoundException e1) {
            String m = String.format("The file [%s] don't exist.", unit.getAbsolutePath());
            logger.error(m, e1);
            throw new UnitValidationException(m, e1);
        }
        zipInputStream = new ZipInputStream(tempInput);
        ZipEntry zipEntry = null;
        try {
            zipEntry = zipInputStream.getNextEntry();
            if (zipEntry == null) {
                String m = String.format("Error when get zipEntry. Maybe the [%s] is not zip file!", unit.getAbsolutePath());
                logger.error(m);
                throw new UnitValidationException(m);
            }
            while (zipEntry != null) {
                if (configFiles.contains(zipEntry.getName())) {
                    byte[] extra = new byte[(int) zipEntry.getSize()];
                    zipInputStream.read(extra);
                    File file = File.createTempFile("temp", "extra");
                    file.deleteOnExit();
                    logger.info("[TempFile:]" + file.getAbsoluteFile());
                    ByteArrayInputStream byteInputStream = new ByteArrayInputStream(extra);
                    FileOutputStream tempFileOutputStream = new FileOutputStream(file);
                    IOUtils.copy(byteInputStream, tempFileOutputStream);
                    tempFileOutputStream.flush();
                    IOUtils.closeQuietly(tempFileOutputStream);
                    InputStream inputStream = new FileInputStream(file);
                    reader.read(inputStream, unit.getAbsolutePath() + ":" + zipEntry.getName());
                    IOUtils.closeQuietly(inputStream);
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (IOException e) {
            ValidationMessage vm = new XMLValidationMessage("IOError", 0, 0, unit.getUrl() + ":" + zipEntry.getName(), e);
            vr.addValidationMessage(vm);
        } catch (DocumentException e) {
            ValidationMessage vm = new XMLValidationMessage("Document Error.", 0, 0, unit.getUrl() + ":" + zipEntry.getName(), e);
            vr.addValidationMessage(vm);
        } finally {
            IOUtils.closeQuietly(tempInput);
            IOUtils.closeQuietly(zipInputStream);
        }
        return vr;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
}
