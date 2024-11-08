package org.fudaa.dodico.crue.io;

import java.io.File;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.dodico.crue.config.TestCrueConfigMetierLoaderDefault;
import org.fudaa.dodico.crue.io.common.CrueData;
import org.fudaa.dodico.crue.io.common.CrueFileType;
import org.fudaa.dodico.crue.metier.emh.OrdPrtGeoModeleBase;
import org.fudaa.dodico.crue.metier.emh.PlanimetrageNbrPdzCst;
import org.fudaa.dodico.crue.projet.coeur.CoeurConfigContrat;
import org.fudaa.dodico.crue.projet.coeur.TestCoeurConfig;

/**
 * @author Adrien Hadoux
 */
public class TestCrueOPTGFile extends AbstractIOTestCase {

    /**
   * 
   */
    protected static final String FICHIER_TEST_OPTG_XML = "/v1_2/M3-0_c10.optg.xml";

    protected static final String FICHIER_TEST_OPTG_XML_V1_1_1 = "/v1_1_1/M3-0_c10.optg.xml";

    /**
   * 
   */
    public TestCrueOPTGFile() {
        super(Crue10FileFormatFactory.getVersion(CrueFileType.OPTG, TestCoeurConfig.INSTANCE), FICHIER_TEST_OPTG_XML);
    }

    public void testValideVersion1p1() {
        CtuluLog log = new CtuluLog();
        boolean valide = Crue10FileFormatFactory.getVersion(CrueFileType.OPTG, TestCoeurConfig.INSTANCE_1_1_1).isValide("/v1_1_1/M3-0_c10.optg.xml", log);
        if (log.containsErrorOrFatalError()) log.printResume();
        assertTrue(valide);
    }

    public void testLecture() {
        final OrdPrtGeoModeleBase data = readModele(FICHIER_TEST_OPTG_XML, TestCoeurConfig.INSTANCE);
        testDataModele3(data);
    }

    public void testLectureV1P1P1() {
        final OrdPrtGeoModeleBase data = readModele(FICHIER_TEST_OPTG_XML_V1_1_1, TestCoeurConfig.INSTANCE_1_1_1);
        testDataModele3(data);
    }

    private void testDataModele3(final OrdPrtGeoModeleBase data) {
        assertNotNull(data);
        assertEquals(50, ((PlanimetrageNbrPdzCst) data.getPlanimetrage()).getNbrPdz());
        assertEquals(0.2, data.getRegles().get(0).getSeuilDetect());
    }

    public void testEcriture() {
        testEcriture(FICHIER_TEST_OPTG_XML, TestCoeurConfig.INSTANCE);
    }

    public void testEcritureV1P1P1() {
        testEcriture(FICHIER_TEST_OPTG_XML_V1_1_1, TestCoeurConfig.INSTANCE_1_1_1);
    }

    public void testEcriture(String file, CoeurConfigContrat version) {
        final CtuluLog analyzer = new CtuluLog();
        final File f = createTempFile();
        Crue10FileFormat<Object> fileFormat = Crue10FileFormatFactory.getVersion(CrueFileType.OPTG, version);
        final boolean res = fileFormat.writeMetierDirect(readModele(file, version), f, analyzer, TestCrueConfigMetierLoaderDefault.DEFAULT);
        assertFalse(analyzer.containsErrors());
        analyzer.clear();
        boolean valide = fileFormat.isValide(f, analyzer);
        testAnalyser(analyzer);
        assertTrue(valide);
        final OrdPrtGeoModeleBase read = (OrdPrtGeoModeleBase) fileFormat.read(f, analyzer, createDefault()).getMetier();
        testDataModele3(read);
    }

    public static OrdPrtGeoModeleBase readModeleLastVersion(CrueData crueData) {
        final CtuluLog analyzer = new CtuluLog();
        final OrdPrtGeoModeleBase data = (OrdPrtGeoModeleBase) Crue10FileFormatFactory.getVersion(CrueFileType.OPTG, TestCoeurConfig.INSTANCE).read(FICHIER_TEST_OPTG_XML, analyzer, crueData).getMetier();
        testAnalyser(analyzer);
        return data;
    }

    public OrdPrtGeoModeleBase readModele(String file, CoeurConfigContrat version) {
        final CtuluLog analyzer = new CtuluLog();
        final OrdPrtGeoModeleBase data = (OrdPrtGeoModeleBase) Crue10FileFormatFactory.getVersion(CrueFileType.OPTG, version).read(file, analyzer, createDefault()).getMetier();
        testAnalyser(analyzer);
        return data;
    }
}
