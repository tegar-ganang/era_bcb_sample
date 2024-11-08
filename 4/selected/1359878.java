package com.genia.toolbox.projects.csv.business.util.impl;

import java.io.File;
import java.util.Arrays;
import javax.annotation.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.assertEquals;
import com.genia.toolbox.basics.exception.BundledException;
import com.genia.toolbox.basics.manager.CsvManager;
import com.genia.toolbox.basics.manager.FileManager;
import com.genia.toolbox.projects.csv.business.util.CsvUtils;
import com.genia.toolbox.spring.initializer.PluginContextLoader;

/**
 * test class for {@link CsvUtils}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = PluginContextLoader.class)
public class CsvUtilsImplTest {

    /**
   * the {@link CsvUtils} reference.
   */
    @Resource
    private CsvUtils csvUtils;

    /**
   * the {@link FileManager} reference.
   */
    @Resource
    private FileManager fileManager;

    /**
   * the {@link CsvManager} to use.
   */
    @Resource
    private CsvManager csvManager;

    /**
   * list of columns of the csv file for the test.
   */
    private static final String[] COLMUNS = new String[] { "Code classe", "Nom", "Description" };

    /**
   * list of comments for the test.
   */
    private static final String[] COMMENTS = new String[] { "Code classe : code de la famille", "Nom: nom de la famille", "Description : description de la famille" };

    /**
   * text for the readme writing test.
   */
    private static final String[] README_TEXTS = new String[] { "Nous vous conseillons de traiter les dossiers (repertoires) dans l'ordre suivant :", "Type_certification,Type_agrement, Type_fournisseur, Famille", "Fournisseur, Critere", "Certification_fournisseur, Agrement_fournisseur, Valeur_indicateur" };

    /**
   * test {@link CsvUtils} methods.
   * 
   * @throws BundledException
   *           if an error occur
   */
    @Test
    public void testCsvUtils() throws BundledException {
        final File basePath = fileManager.createAutoDeletableTempDirectory("csvDir", ".dir");
        final File folderPathTest = new File(basePath, "Famille");
        folderPathTest.mkdirs();
        final File filePathTest = new File(folderPathTest, "1_2007-12-11.csv");
        final File readmeFilePath = new File(basePath, "readme.txt");
        csvUtils.writeCsvHeaders(filePathTest, Arrays.asList(COLMUNS), Arrays.asList(COMMENTS));
        assertEquals(Arrays.asList(csvManager.parseCsv(filePathTest)[0]), Arrays.asList(COLMUNS));
        csvUtils.writeReadmeTexts(readmeFilePath, Arrays.asList(README_TEXTS));
        fileManager.deleteRecursively(basePath);
    }
}
