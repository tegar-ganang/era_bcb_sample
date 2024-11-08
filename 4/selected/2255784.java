package com.google.code.ptrends.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import com.google.code.ptrends.catparsers.CompositeCategoryParser;
import com.google.code.ptrends.common.entities.Session;
import com.google.code.ptrends.entities.SourceType;
import com.google.code.ptrends.entities.SupplierConfiguration;
import com.google.code.ptrends.excel.POIReader;
import com.google.code.ptrends.excel.entities.Worksheet;
import com.google.code.ptrends.locators.Locator;
import com.google.code.ptrends.processors.implementations.RVExcelProcessor;
import com.google.code.ptrends.processors.interfaces.ETLProcessor;

public class ProcessorBuilderImpl implements ProcessorBuilder {

    private static final Logger LOG = Logger.getLogger(ProcessorBuilderImpl.class);

    public ProcessorBuilderImpl(Session session) {
        if (session == null) throw new IllegalArgumentException("Illegal null-reference session");
        this.session = session;
    }

    public ETLProcessor buildProcessor(final SupplierConfiguration cfg, final Locator locator) {
        switch(cfg.getSupplierID()) {
            case SupplierIdentifiers.RET:
                try {
                    InputStream stream = locator.getStream();
                    if (cfg.getSourceType() == SourceType.ZIP) {
                        final byte[] unzippedBytes = unzipFromStream(stream);
                        stream.close();
                        stream = new ByteArrayInputStream(unzippedBytes);
                    }
                    return createRetProcessor(stream);
                } catch (IOException e) {
                    LOG.error("Error while creating RVExcelProcessor", e);
                }
                break;
            case SupplierIdentifiers.DNS:
                throw new NotImplementedException("Not yet implemented");
            case SupplierIdentifiers.KEY:
                throw new NotImplementedException("Not yet implemented");
            default:
                throw new IllegalArgumentException("Illegal non supported supplier identifier");
        }
        return null;
    }

    private RVExcelProcessor createRetProcessor(final InputStream sourceStream) {
        POIReader reader = null;
        try {
            reader = new POIReader(sourceStream);
        } catch (InvalidFormatException e1) {
            LOG.error("Error while creating POIReader", e1);
        } catch (IOException e2) {
            LOG.error("Error while creating POIReader", e2);
        }
        final Worksheet sheet = new Worksheet("Прайс-лист");
        final CompositeCategoryParser parser = new CompositeCategoryParser(reader, sheet, session);
        return new RVExcelProcessor(reader, parser);
    }

    private byte[] unzipFromStream(final InputStream sourceStream) throws IOException {
        ZipInputStream zipStream = null;
        byte[] result = new byte[] {};
        try {
            zipStream = new ZipInputStream(sourceStream);
            zipStream.getNextEntry();
            final int bufferSize = 10240;
            byte[] buffer = new byte[bufferSize];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            while (zipStream.available() > 0) {
                int readBytes = zipStream.read(buffer, 0, bufferSize);
                if (readBytes > 0) {
                    outputStream.write(buffer, 0, readBytes);
                }
            }
            outputStream.flush();
            result = outputStream.toByteArray();
            outputStream.close();
        } catch (IOException e) {
            LOG.error("Error while uncompressing data", e);
        } finally {
            if (zipStream != null) zipStream.close();
        }
        return result;
    }

    private Session session;
}
