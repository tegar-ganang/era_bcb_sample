package org.fudaa.dodico.crue.io;

import java.io.File;
import java.util.List;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.dodico.crue.config.TestCrueConfigMetierLoaderDefault;
import org.fudaa.dodico.crue.config.SeveriteManager;
import org.fudaa.dodico.crue.io.common.CrueFileType;
import org.fudaa.dodico.crue.metier.emh.EnumMethodeOrdonnancement;
import org.fudaa.dodico.crue.metier.emh.EnumRegle;
import org.fudaa.dodico.crue.metier.emh.OrdPrtReseau;
import org.fudaa.dodico.crue.metier.emh.Regle;
import org.fudaa.dodico.crue.metier.emh.Sorties;
import org.fudaa.dodico.crue.projet.coeur.CoeurConfigContrat;
import org.fudaa.dodico.crue.projet.coeur.TestCoeurConfig;

/**
 * @author Adrien Hadoux
 */
public class TestCrueOPTRFile extends AbstractIOTestCase {

    /**
   * 
   */
    protected static final String FICHIER_TEST_OPTR_XML = "/v1_2/M3-0_c10.optr.xml";

    protected static final String FICHIER_TEST_OPTR_XML_FALSE = "/v1_2/M3-0_c10-false.optr.xml";

    /**
   * 
   */
    public TestCrueOPTRFile() {
        super(Crue10FileFormatFactory.getVersion(CrueFileType.OPTR, TestCoeurConfig.INSTANCE), FICHIER_TEST_OPTR_XML);
    }

    public void testLecture() {
        final OrdPrtReseau data = readModele(FICHIER_TEST_OPTR_XML, TestCoeurConfig.INSTANCE);
        testData(data);
    }

    public void testLectureFalse() {
        final OrdPrtReseau data = readModele(FICHIER_TEST_OPTR_XML_FALSE, TestCoeurConfig.INSTANCE);
        testDataFalse(data);
    }

    private void testData(final OrdPrtReseau data) {
        assertNotNull(data);
        assertEquals(EnumMethodeOrdonnancement.ORDRE_DRSO, data.getMethodeOrdonnancement());
        Sorties sorties = data.getSorties();
        assertTrue(sorties.getAvancement().getSortieFichier());
        assertTrue(sorties.getResultat().getSortieFichier());
        assertTrue(sorties.getTrace().getSortieFichier());
        assertTrue(sorties.getTrace().getSortieEcran());
        assertEquals(SeveriteManager.INFO, sorties.getTrace().getVerbositeAbonne());
        assertEquals(SeveriteManager.DEBUG3, sorties.getTrace().getVerbositeEcran());
        assertEquals(SeveriteManager.INFO, sorties.getTrace().getVerbositeFichier());
        List<Regle> regles = data.getRegles();
        assertEquals(2, regles.size());
        assertTrue(regles.get(0).isActive());
        assertEquals(EnumRegle.ORD_PRT_RESEAU_COMPATIBILITE_CLIMM, regles.get(0).getType());
        assertTrue(regles.get(1).isActive());
        assertEquals(EnumRegle.ORD_PRT_RESEAU_SIGNALER_OBJETS_INACTIFS, regles.get(1).getType());
    }

    private void testDataFalse(final OrdPrtReseau data) {
        assertNotNull(data);
        assertEquals(EnumMethodeOrdonnancement.ORDRE_DRSO, data.getMethodeOrdonnancement());
        Sorties sorties = data.getSorties();
        assertFalse(sorties.getAvancement().getSortieFichier());
        assertFalse(sorties.getResultat().getSortieFichier());
        assertFalse(sorties.getTrace().getSortieFichier());
        assertFalse(sorties.getTrace().getSortieEcran());
        assertEquals(SeveriteManager.INFO, sorties.getTrace().getVerbositeAbonne());
        assertEquals(SeveriteManager.INFO, sorties.getTrace().getVerbositeEcran());
        assertEquals(SeveriteManager.INFO, sorties.getTrace().getVerbositeFichier());
        List<Regle> regles = data.getRegles();
        assertEquals(2, regles.size());
        assertFalse(regles.get(0).isActive());
        assertEquals(EnumRegle.ORD_PRT_RESEAU_COMPATIBILITE_CLIMM, regles.get(0).getType());
        assertFalse(regles.get(1).isActive());
        assertEquals(EnumRegle.ORD_PRT_RESEAU_SIGNALER_OBJETS_INACTIFS, regles.get(1).getType());
    }

    public void testEcriture() {
        testData(doEcriture(FICHIER_TEST_OPTR_XML, TestCoeurConfig.INSTANCE));
    }

    public void testEcritureFalse() {
        testDataFalse(doEcriture(FICHIER_TEST_OPTR_XML_FALSE, TestCoeurConfig.INSTANCE));
    }

    public OrdPrtReseau doEcriture(String file, CoeurConfigContrat version) {
        final CtuluLog analyzer = new CtuluLog();
        final File f = createTempFile();
        Crue10FileFormat<Object> fileFormat = Crue10FileFormatFactory.getVersion(CrueFileType.OPTR, version);
        fileFormat.writeMetierDirect(readModele(file, version), f, analyzer, TestCrueConfigMetierLoaderDefault.DEFAULT);
        assertFalse(analyzer.containsErrors());
        analyzer.clear();
        boolean valide = fileFormat.isValide(f, analyzer);
        testAnalyser(analyzer);
        assertTrue(valide);
        return (OrdPrtReseau) fileFormat.read(f, analyzer, createDefault()).getMetier();
    }

    public OrdPrtReseau readModele(String file, CoeurConfigContrat version) {
        final CtuluLog analyzer = new CtuluLog();
        final OrdPrtReseau data = (OrdPrtReseau) Crue10FileFormatFactory.getVersion(CrueFileType.OPTR, version).read(file, analyzer, createDefault()).getMetier();
        testAnalyser(analyzer);
        return data;
    }
}
