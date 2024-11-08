package org.xmlfield.tests.performance.shakspeare;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlfield.core.XmlField;
import org.xmlfield.core.impl.dom.XmlFieldDomSelectorTest;

/**
 * @author Olivier Lafon
 */
@Ignore
public class TestPack9Test {

    private final XmlField binder = new XmlField();

    String hugeRomeoAndFatPrincess;

    @Before
    public void setUp() throws Exception {
        File fichierTest = createFileFromClasspathResource("/r_and_j.xml");
        hugeRomeoAndFatPrincess = FileUtils.readFileToString(fichierTest, "UTF-8");
    }

    /**
     * Méthode qui uploade un fichier présent dans le classpath
     * 
     * @param resourceUrl
     *            l'url de la ressource
     * @return le fichier uploadé
     * @throws IOException
     */
    public File createFileFromClasspathResource(String resourceUrl) throws IOException {
        File fichierTest = File.createTempFile("xmlFieldTestFile", "");
        FileOutputStream fos = new FileOutputStream(fichierTest);
        InputStream is = XmlFieldDomSelectorTest.class.getResourceAsStream(resourceUrl);
        IOUtils.copy(is, fos);
        is.close();
        fos.close();
        return fichierTest;
    }

    @Test
    public void testPerfSelectXPathToString() throws Exception {
        StringBuilder sb = new StringBuilder();
        Play fatRomeo = binder.xmlToObject(hugeRomeoAndFatPrincess, Play.class);
        for (Act act : fatRomeo.getActs()) {
            sb.append(act.getPrologueTitle());
            for (Scene scene : act.getScenes()) {
                sb.append(scene.getTitle());
                sb.append(scene.getStageDir());
                for (Speech speech : scene.getSpeeches()) {
                    sb.append(speech.getSpeaker());
                    for (String line : speech.getLines()) {
                        sb.append(line);
                    }
                }
            }
        }
        assertTrue(StringUtils.isNotEmpty(sb.toString()));
    }
}
