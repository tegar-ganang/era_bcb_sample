package org.spantus.work.services.reader.impl;

import java.io.File;
import java.util.List;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spantus.core.extractor.IExtractorInputReader;
import org.spantus.core.test.DumyExtractorInputReader;

public class CsvDaoImplTest {

    CsvDaoImpl csvDaoImpl;

    @Before
    public void setUp() throws Exception {
        csvDaoImpl = new CsvDaoImpl();
    }

    @Test
    public void test() {
        IExtractorInputReader reader = new DumyExtractorInputReader();
        reader.getExtractorRegister3D().add(DumyExtractorInputReader.createExtractorVector("test1"));
        File file = new File("./target/test.csv");
        List<File> files = csvDaoImpl.write(reader, file);
        Assert.assertTrue("Exists", files.get(0).exists());
    }
}
