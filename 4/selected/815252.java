package org.fudaa.dodico.crue.io;

import java.io.File;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.dodico.crue.config.SeveriteManager;
import org.fudaa.dodico.crue.io.common.CrueFileType;
import org.fudaa.dodico.crue.metier.emh.OrdPrtCIniModeleBase;
import org.fudaa.dodico.crue.projet.coeur.CoeurConfigContrat;
import org.fudaa.dodico.crue.projet.coeur.TestCoeurConfig;

/**
 * test unitaires pour les opti.
 * 
 * @author Adrien Hadoux
 */
public class TestCrueOPTIFile extends AbstractIOTestCase {

    public static final String FICHIER_TEST_OPTI_XML = "/v1_2/M3-0_c10.opti.xml";

    public static final String FICHIER_TEST_OPTI_XML_V1_1_1 = "/v1_1_1/M3-0_c10.opti.xml";

    /**
   * 
   */
    public TestCrueOPTIFile() {
        super(Crue10FileFormatFactory.getVersion(CrueFileType.OPTI, TestCoeurConfig.INSTANCE), FICHIER_TEST_OPTI_XML);
    }

    public void testValideVersion_v1p1p1() {
        CtuluLog log = new CtuluLog();
        boolean valide = Crue10FileFormatFactory.getInstance().getFileFormat(CrueFileType.OPTI, TestCoeurConfig.getInstance(Crue10FileFormatFactory.V_1_1_1)).isValide(FICHIER_TEST_OPTI_XML_V1_1_1, log);
        if (log.containsErrorOrFatalError()) log.printResume();
        assertTrue(valide);
    }

    public void testLecture() {
        final OrdPrtCIniModeleBase data = read(FICHIER_TEST_OPTI_XML, TestCoeurConfig.INSTANCE);
        testDataModele3(data);
    }

    public void testLecture_v1p1p1() {
        final OrdPrtCIniModeleBase data = read(FICHIER_TEST_OPTI_XML_V1_1_1, TestCoeurConfig.getInstance(Crue10FileFormatFactory.V_1_1_1));
        testDataModele3(data);
    }

    public OrdPrtCIniModeleBase read(String file, CoeurConfigContrat version) {
        final CtuluLog analyzer = new CtuluLog();
        final OrdPrtCIniModeleBase data = (OrdPrtCIniModeleBase) Crue10FileFormatFactory.getInstance().getFileFormat(CrueFileType.OPTI, version).read(file, analyzer, createDefault()).getMetier();
        testAnalyser(analyzer);
        return data;
    }

    private void testDataModele3(final OrdPrtCIniModeleBase data) {
        assertNotNull(data);
    }

    private void testSortiesFalseAndDebug(final OrdPrtCIniModeleBase data) {
        assertFalse(data.getSorties().getResultat().getSortieFichier());
        assertFalse(data.getSorties().getAvancement().getSortieFichier());
        assertFalse(data.getSorties().getTrace().getSortieFichier());
        assertFalse(data.getSorties().getTrace().getSortieEcran());
        assertTrue(data.getSorties().getTrace().getVerbositeAbonne().equals(SeveriteManager.DEBUG3));
        assertTrue(data.getSorties().getTrace().getVerbositeEcran().equals(SeveriteManager.DEBUG3));
        assertTrue(data.getSorties().getTrace().getVerbositeFichier().equals(SeveriteManager.DEBUG3));
    }

    private void testSortiesTrueAndInfo(final OrdPrtCIniModeleBase data) {
        assertTrue(data.getSorties().getResultat().getSortieFichier());
        assertTrue(data.getSorties().getAvancement().getSortieFichier());
        assertTrue(data.getSorties().getTrace().getSortieFichier());
        assertTrue(data.getSorties().getTrace().getSortieEcran());
        assertTrue(data.getSorties().getTrace().getVerbositeAbonne().equals(SeveriteManager.INFO));
        assertTrue(data.getSorties().getTrace().getVerbositeEcran().equals(SeveriteManager.INFO));
        assertTrue(data.getSorties().getTrace().getVerbositeFichier().equals(SeveriteManager.INFO));
    }

    public void testEcriture() {
        testEcriture(FICHIER_TEST_OPTI_XML, TestCoeurConfig.INSTANCE);
    }

    public void testEcriture(String file, CoeurConfigContrat version) {
        final CtuluLog analyzer = new CtuluLog();
        final File f = createTempFile();
        final boolean res = Crue10FileFormatFactory.getInstance().getFileFormat(CrueFileType.OPTI, version).writeMetierDirect(read(file, version), f, analyzer, version.getCrueConfigMetier());
        testAnalyser(analyzer);
        assertFalse(analyzer.containsErrors());
        final OrdPrtCIniModeleBase read = (OrdPrtCIniModeleBase) Crue10FileFormatFactory.getInstance().getFileFormat(CrueFileType.OPTI, version).read(f, analyzer, createDefault()).getMetier();
        testDataModele3(read);
        if (version.getXsdVersion().equals(Crue10FileFormatFactory.V_1_1_1)) {
            testSortiesTrueAndInfo(read);
        } else {
            testSortiesFalseAndDebug(read);
        }
    }

    public void testEcriture_v1p1p1() {
        testEcriture(FICHIER_TEST_OPTI_XML_V1_1_1, TestCoeurConfig.INSTANCE_1_1_1);
    }
}
