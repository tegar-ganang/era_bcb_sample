package org.fudaa.dodico.crue.io;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.dodico.crue.common.AbstractTestCase;
import org.fudaa.dodico.crue.common.DateDurationConverter;
import org.fudaa.dodico.crue.common.BusinessMessages;
import org.fudaa.dodico.crue.config.SeveriteManager;
import org.fudaa.dodico.crue.io.common.CrueData;
import org.fudaa.dodico.crue.io.common.CrueFileType;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.crue.metier.emh.CalcPseudoPerm;
import org.fudaa.dodico.crue.metier.emh.CalcPseudoPermBrancheOrificeManoeuvre;
import org.fudaa.dodico.crue.metier.emh.CalcPseudoPermNoeudNiveauContinuZimp;
import org.fudaa.dodico.crue.metier.emh.CalcPseudoPermNoeudQapp;
import org.fudaa.dodico.crue.metier.emh.CalcTrans;
import org.fudaa.dodico.crue.metier.emh.CalcTransBrancheOrificeManoeuvre;
import org.fudaa.dodico.crue.metier.emh.CalcTransBrancheSaintVenantQruis;
import org.fudaa.dodico.crue.metier.emh.CalcTransNoeudNiveauContinuTarage;
import org.fudaa.dodico.crue.metier.emh.CalcTransNoeudQapp;
import org.fudaa.dodico.crue.metier.emh.CatEMHBranche;
import org.fudaa.dodico.crue.metier.emh.CatEMHNoeud;
import org.fudaa.dodico.crue.metier.emh.DonCLimM;
import org.fudaa.dodico.crue.metier.emh.DonCLimMScenario;
import org.fudaa.dodico.crue.metier.emh.DonCalcSansPrt;
import org.fudaa.dodico.crue.metier.emh.DonCalcSansPrtBrancheSaintVenant;
import org.fudaa.dodico.crue.metier.emh.DonCalcSansPrtBrancheSeuilTransversal;
import org.fudaa.dodico.crue.metier.emh.DonPrtCIniBranche;
import org.fudaa.dodico.crue.metier.emh.DonPrtCIniNoeudNiveauContinu;
import org.fudaa.dodico.crue.metier.emh.EMHBrancheSaintVenant;
import org.fudaa.dodico.crue.metier.emh.EMHBrancheSeuilTransversal;
import org.fudaa.dodico.crue.metier.emh.EnumFormulePdc;
import org.fudaa.dodico.crue.metier.emh.EnumSensOuv;
import org.fudaa.dodico.crue.metier.emh.EnumTypeLoi;
import org.fudaa.dodico.crue.metier.emh.EvolutionFF;
import org.fudaa.dodico.crue.metier.emh.Loi;
import org.fudaa.dodico.crue.metier.emh.LoiDF;
import org.fudaa.dodico.crue.metier.emh.LoiFF;
import org.fudaa.dodico.crue.metier.emh.OrdCalc;
import org.fudaa.dodico.crue.metier.emh.OrdCalcScenario;
import org.fudaa.dodico.crue.metier.emh.OrdCalcTransIniCalcReprise;
import org.fudaa.dodico.crue.metier.emh.ParamCalcScenario;
import org.fudaa.dodico.crue.metier.emh.ParamNumModeleBase;
import org.fudaa.dodico.crue.metier.emh.Pdt;
import org.fudaa.dodico.crue.metier.emh.PdtCst;
import org.fudaa.dodico.crue.metier.emh.PtEvolutionFF;
import org.fudaa.dodico.crue.metier.helper.EMHHelper;
import org.fudaa.dodico.crue.projet.coeur.TestCoeurConfig;
import org.fudaa.dodico.crue.validation.ValidatorForValuesAndContents;
import org.joda.time.Duration;

/**
 * Tests junit pour les fichiers DH IO. fortran, .
 * 
 * @author Adrien Hadoux
 */
public class TestCrueDH extends AbstractTestCase {

    public static final String FICHIER_TEST_MODELE3_DH = "/crue9/M3-0_c9.dh";

    public static final String FICHIER_TEST_MODELE3_DH_ISORTI_0 = "/crue9/M3-0_c9_isorti_0.dh";

    public static final String FICHIER_TEST_MODELE3_DC = "/crue9/M3-0_c9.dc";

    public static final String FICHIER_TEST_MODELE4_DH = "/crue9/M4-0_c9.dh";

    public static final String FICHIER_TEST_MODELE5_DH = "/crue9/M5-0_c9.dh";

    protected static final String FICHIER_TEST_MODELE6_DH = "/crue9/M6-0_c9.dh";

    protected static final String FICHIER_TEST_MODELE7_DH = "/crue9/M7-0_c9.dh";

    protected static final String FICHIER_TEST_MODELE3_DCSP = TestCrueDCSPFile.FICHIER_TEST_DCSP_XML;

    protected static final String FICHIER_TEST_MODELE3_OCAL = TestCrueOCALFile.FICHIER_TEST_OCAL_XML;

    protected static final String FICHIER_TEST_MODELE3_OPTI = TestCrueOPTIFile.FICHIER_TEST_OPTI_XML;

    protected static final String FICHIER_TEST_MODELE3_PCAL = TestCruePCALFile.FICHIER_TEST_PCAL_XML;

    protected static final String FICHIER_TEST_MODELE3_PNUM = TestCruePNUMFile.FICHIER_TEST_PNUM_XML;

    protected static final String FICHIER_TEST_MODELE3_DPTI = TestCrueDPTIFile.FICHIER_TEST_DPTI_XML;

    protected static final String FICHIER_TEST_MODELE3_DLHY = TestCrueDLHYFile.FICHIER_TEST_DLHY_XML;

    protected static final String FICHIER_TEST_MODELE3_DCLM = TestCrueDCLMFile.FICHIER_TEST_DCLM_XML;

    protected static final String FICHIER_TEST_MODELE3_OPTG = TestCrueOPTGFile.FICHIER_TEST_OPTG_XML;

    protected static final String FICHIER_TEST_M6_0_C9_DH = "/crue9/M6-0_c9.dh";

    protected static final String FICHIER_TEST_M6_0_C9_DC = "/crue9/M6-0_c9.dc";

    /**
   * Test de lecture
   */
    public CrueData testLecture() {
        final CtuluLog analyzer = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<CrueData> data = readModeleCrue9(analyzer, FICHIER_TEST_MODELE3_DH);
        testAnalyser(analyzer);
        assertEquals("CrueX - Structuration des données||Modéle de test utilisant les éléments de modélisation hydraulique les plus courants||PBa Jan09 sur la base de Modele2", data.getCrueCommentaire());
        testData(data.getMetier(), false);
        TestCrueDC.assertSortieTrueAndDebug(data.getMetier().getOCAL().getSorties());
        TestCrueDC.assertSortieTrueAndInfo(data.getMetier().getOPTG().getSorties());
        TestCrueDC.assertSortieTrueAndInfo(data.getMetier().getOPTI().getSorties());
        TestCrueDC.assertSortieTrueAndInfo(data.getMetier().getOPTR().getSorties());
        return data.getMetier();
    }

    public void testLectureIsortiVaut0() {
        final CtuluLog analyzer = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<CrueData> data = readModeleCrue9(analyzer, FICHIER_TEST_MODELE3_DH_ISORTI_0);
        testAnalyser(analyzer);
        TestCrueDC.assertSortieTrueAndInfo(data.getMetier().getOCAL().getSorties());
    }

    public void testTransformIsorti0En1() {
        final CtuluLog analyzer = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<CrueData> data = readModeleCrue9(analyzer, FICHIER_TEST_MODELE3_DH_ISORTI_0);
        testAnalyser(analyzer);
        TestCrueDC.assertSortieTrueAndInfo(data.getMetier().getOCAL().getSorties());
        data.getMetier().getOCAL().getSorties().getTrace().setVerbositeAbonne(SeveriteManager.DEBUG3);
        File f = createTempFile();
        Crue9FileFormatFactory.getDHFileFormat().writeMetier(data, f, analyzer);
        testAnalyser(analyzer);
        try {
            final CrueIOResu<CrueData> dataWith1 = ReadHelper.readModele(analyzer, getClass().getResource(FICHIER_TEST_MODELE3_DC), f.toURI().toURL());
            testAnalyser(dataWith1.getAnalyse());
            TestCrueDC.assertSortieTrueAndDebug(dataWith1.getMetier().getOCAL().getSorties());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testTransformIsorti1En0() {
        final CtuluLog analyzer = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<CrueData> data = readModeleCrue9(analyzer, FICHIER_TEST_MODELE3_DH);
        testAnalyser(analyzer);
        TestCrueDC.assertSortieTrueAndDebug(data.getMetier().getOCAL().getSorties());
        data.getMetier().getOCAL().getSorties().getTrace().setVerbositeAbonne(SeveriteManager.INFO);
        data.getMetier().getOCAL().getSorties().getTrace().setVerbositeEcran(SeveriteManager.INFO);
        data.getMetier().getOCAL().getSorties().getTrace().setVerbositeFichier(SeveriteManager.INFO);
        File f = createTempFile();
        Crue9FileFormatFactory.getDHFileFormat().writeMetier(data, f, analyzer);
        testAnalyser(analyzer);
        try {
            final CrueIOResu<CrueData> dataWith1 = ReadHelper.readModele(analyzer, getClass().getResource(FICHIER_TEST_MODELE3_DC), f.toURI().toURL());
            testAnalyser(dataWith1.getAnalyse());
            TestCrueDC.assertSortieTrueAndInfo(dataWith1.getMetier().getOCAL().getSorties());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
   * Test du modele 4 fourni par la CNR.
   */
    public void testLectureSc_M4_0_v1c9() {
        final CtuluLog analyzer = new CtuluLog();
        final CrueIOResu<CrueData> readModele = ReadHelper.readModele(analyzer, "/Etu4-0/M4-0_c9.dc", "/Etu4-0/M4-0_c9.dh");
        testAnalyser(analyzer);
        assertNotNull(readModele.getMetier());
        testAnalyser(readModele.getAnalyse());
    }

    public void testLectureEcritureEtu21() {
        final CtuluLog analyzer = new CtuluLog();
        final CrueIOResu<CrueData> initModele = ReadHelper.readModele(analyzer, "/etu21/M21-2_c9.dc", "/etu21/M21-2_c9.dh");
        assert (analyzer.containsFatalError());
    }

    private void testOcalTransitoire(final CrueIOResu<CrueData> initModele) {
        OrdCalcScenario oCAL = initModele.getMetier().getOCAL();
        List<OrdCalc> ordCalc = oCAL.getOrdCalc();
        assertEquals(1, ordCalc.size());
        OrdCalc ocal = ordCalc.get(0);
        assertTrue(ocal instanceof OrdCalcTransIniCalcReprise);
        OrdCalcTransIniCalcReprise reprise = (OrdCalcTransIniCalcReprise) ocal;
        Duration tempsReprise = reprise.getTempsReprise();
        assertEquals("temps de reprise de 4H", 4 * 3600, tempsReprise.getStandardSeconds());
    }

    public void testLectureM6_0_C9() {
        final CrueIOResu<CrueData> data = readM6_0_C9();
        List<CtuluLog> validateValues = ValidatorForValuesAndContents.validateValues(data.getMetier().getScenarioData());
        for (CtuluLog ctuluAnalyze : validateValues) {
            if (ctuluAnalyze.containsErrors()) {
                ctuluAnalyze.printResume();
            }
            assertFalse(ctuluAnalyze.containsErrors());
        }
    }

    private CrueIOResu<CrueData> readM6_0_C9() {
        final CtuluLog analyzer = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<CrueData> data = ReadHelper.readModele(analyzer, FICHIER_TEST_M6_0_C9_DC, FICHIER_TEST_M6_0_C9_DH);
        testAnalyser(analyzer);
        return data;
    }

    public void testLectureModele7() {
        final CrueIOResu<CrueData> data = readModele7();
        testModele7(data);
    }

    public void testEcritureModele7() {
        CrueIOResu<CrueData> data = readModele7();
        final File fdc = createTempFile();
        final File fdh = createTempFile();
        final CtuluLog analyse = new CtuluLog();
        Crue9FileFormatFactory.getDCFileFormat().writeMetier(data, fdc, analyse);
        Crue9FileFormatFactory.getDHFileFormat().writeMetier(data, fdh, analyse);
        testAnalyser(analyse);
        try {
            data = ReadHelper.readModele(analyse, fdc.toURI().toURL(), fdh.toURI().toURL());
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }
        testAnalyser(analyse);
        testModele7(data);
    }

    private void testModele7(final CrueIOResu<CrueData> data) {
        final EMHBrancheSaintVenant findBrancheByReference = (EMHBrancheSaintVenant) data.getMetier().findBrancheByReference("b27");
        final DonCalcSansPrtBrancheSaintVenant dcsp = EMHHelper.selectFirstOfClass(findBrancheByReference.getInfosEMH(), DonCalcSansPrtBrancheSaintVenant.class);
        assertDoubleEquals(1.25, dcsp.getCoefRuis());
        final CalcTransBrancheSaintVenantQruis trans = EMHHelper.selectFirstOfClass(findBrancheByReference.getInfosEMH(), CalcTransBrancheSaintVenantQruis.class);
        assertEquals(109, trans.getLoi().getEvolutionFF().getPtEvolutionFF().size());
    }

    private CrueIOResu<CrueData> readModele7() {
        final CtuluLog analyzer = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<CrueData> data = ReadHelper.readModele(analyzer, TestCrueDC.FICHIER_TEST_MODELE7_DC, FICHIER_TEST_MODELE7_DH);
        if (analyzer.containsFatalError()) {
            analyzer.printResume();
        }
        assertFalse(analyzer.containsFatalError());
        return data;
    }

    public void testInclude() {
        final CtuluLog analyzer = new CtuluLog();
        final CrueIOResu<CrueData> dataGoto = readModeleCrue9(analyzer, "/crue9/M3-0_c9.dh.include");
        testAnalyser(analyzer);
        assertEquals("ligne 1\nligne 2\nligne 3\nligne 4\nligne 5", dataGoto.getCrueCommentaire());
    }

    /**
   * Test cycle de lecture/ecriture du modele 3
   */
    public void testLectureEcritureCRUE9Modele3() {
        final CtuluLog analyzer = new CtuluLog();
        final CrueIOResu<CrueData> data = readModeleCrue9(analyzer, FICHIER_TEST_MODELE3_DH);
        final File f = createTemptxtFile("modele3");
        ReadHelper.writeModeleCrue9(analyzer, f, data.getMetier());
        testAnalyser(analyzer);
    }

    /**
   * Test lecture/ecriture du modele 4.
   */
    public void testLectureEcritureCRUE9Modele4() {
        final CrueData data = testLectureUniquement(TestCrueDC.FICHIER_TEST_MODELE4_DC, FICHIER_TEST_MODELE4_DH);
        final File f = createTemptxtFile("modele4");
        final CtuluLog analyzer = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        ReadHelper.writeModeleCrue9(analyzer, f, data);
        testAnalyser(analyzer);
    }

    /**
   * Test lecture des fichiers crue 10 du modele 3 et ecriture du fichier DH correspondant
   */
    public void testLectureFichiersCrue10etEcritureCrue9() {
        final CtuluLog analyzer = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueData data = (CrueData) Crue10FileFormatFactory.getVersion(CrueFileType.DRSO, TestCoeurConfig.INSTANCE).read(TestCrueDRSOFile.FICHIER_TEST_DRSO_XML, analyzer, TestCrueDRSOFile.createDefault()).getMetier();
        testAnalyser(analyzer);
        Crue10FileFormatFactory.getVersion(CrueFileType.DCSP, TestCoeurConfig.INSTANCE).read(FICHIER_TEST_MODELE3_DCSP, analyzer, data);
        testAnalyser(analyzer);
        Crue10FileFormatFactory.getVersion(CrueFileType.PCAL, TestCoeurConfig.INSTANCE).read(FICHIER_TEST_MODELE3_PCAL, analyzer, data);
        testAnalyser(analyzer);
        Crue10FileFormatFactory.getVersion(CrueFileType.DPTI, TestCoeurConfig.INSTANCE).read(FICHIER_TEST_MODELE3_DPTI, analyzer, data);
        testAnalyser(analyzer);
        Crue10FileFormatFactory.getVersion(CrueFileType.PNUM, TestCoeurConfig.INSTANCE).read(FICHIER_TEST_MODELE3_PNUM, analyzer, data);
        testAnalyser(analyzer);
        Crue10FileFormatFactory.getVersion(CrueFileType.DLHY, TestCoeurConfig.INSTANCE).read(FICHIER_TEST_MODELE3_DLHY, analyzer, data);
        testAnalyser(analyzer);
        Crue10FileFormatFactory.getVersion(CrueFileType.DCLM, TestCoeurConfig.INSTANCE).read(FICHIER_TEST_MODELE3_DCLM, analyzer, data);
        testAnalyser(analyzer);
        Crue10FileFormatFactory.getVersion(CrueFileType.OCAL, TestCoeurConfig.INSTANCE).read(FICHIER_TEST_MODELE3_OCAL, analyzer, data);
        testAnalyser(analyzer);
        Crue10FileFormatFactory.getVersion(CrueFileType.OPTI, TestCoeurConfig.INSTANCE).read(FICHIER_TEST_MODELE3_OPTI, analyzer, data);
        testAnalyser(analyzer);
        Crue10FileFormatFactory.getVersion(CrueFileType.OPTG, TestCoeurConfig.INSTANCE).read(FICHIER_TEST_MODELE3_OPTG, analyzer, data);
        testAnalyser(analyzer);
        final File f = createTemptxtFile("modele3");
        ReadHelper.writeModeleCrue9(analyzer, f, data);
        testAnalyser(analyzer);
    }

    /**
   */
    public void testLectureFichierCrue9etEcritureCrue10() {
        final CtuluLog analyzer = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<CrueData> res = readModeleCrue9(analyzer, FICHIER_TEST_MODELE3_DH);
        final CrueData data = res.getMetier();
        final File fichierEcritureDCSP = createTemptxtFile("DCSP");
        Crue10FileFormatFactory.getVersion(CrueFileType.DCSP, TestCoeurConfig.INSTANCE).write(data, fichierEcritureDCSP, analyzer);
        if (data.getOCAL() != null) {
            final File fichierEcritureOCAL = createTemptxtFile("OCAL");
            Crue10FileFormatFactory.getVersion(CrueFileType.OCAL, TestCoeurConfig.INSTANCE).write(data, fichierEcritureOCAL, analyzer);
        }
        final File fichierEcriturePCAL = createTemptxtFile("PCAL");
        Crue10FileFormatFactory.getVersion(CrueFileType.PCAL, TestCoeurConfig.INSTANCE).write(data, fichierEcriturePCAL, analyzer);
        final File fichierEcritureDPTI = createTemptxtFile("DPTI");
        Crue10FileFormatFactory.getVersion(CrueFileType.DPTI, TestCoeurConfig.INSTANCE).write(data, fichierEcritureDPTI, analyzer);
        final File fichierEcriturePNUM = createTemptxtFile("PNUM");
        Crue10FileFormatFactory.getVersion(CrueFileType.PNUM, TestCoeurConfig.INSTANCE).write(data, fichierEcriturePNUM, analyzer);
        final File fichierEcritureDCLM = createTemptxtFile("DCLM");
        Crue10FileFormatFactory.getVersion(CrueFileType.DCLM, TestCoeurConfig.INSTANCE).write(data, fichierEcritureDCLM, analyzer);
        final File fichierEcritureDLHY = createTemptxtFile("DLHY");
        Crue10FileFormatFactory.getVersion(CrueFileType.DLHY, TestCoeurConfig.INSTANCE).write(data, fichierEcritureDLHY, analyzer);
    }

    /**
   * @param titre
   * @return
   */
    protected File createTemptxtFile(final String titre) {
        return super.createTempFile();
    }

    /**
   * Test de M3-2_c9
   */
    public void testLectureM3_2_c9() {
        final CtuluLog log = new CtuluLog();
        final CrueIOResu<CrueData> readModele = ReadHelper.readModele(log, "/Etu3-2/M3-2_c9.dc", "/Etu3-2/M3-2_c9.dh");
        final File createTempFile = createTempFile();
        ReadHelper.writeModeleCrue9(log, createTempFile, readModele.getMetier());
        testAnalyser(log);
    }

    /**
   * Test de M3-2_c9
   */
    public void testLectureM3_2_e1c9() {
        final CtuluLog log = new CtuluLog();
        final CrueIOResu<CrueData> readModele = ReadHelper.readModele(log, "/Etu4-0/M4-0_c9.dc", "/Etu4-0/M4-0_e1c9.dh");
        assertTrue(readModele.getAnalyse().containsErrorOrFatalError());
    }

    protected void testData(final CrueData data, final boolean isApresEcriture) {
        final ParamNumModeleBase pnum = data.getPNUM();
        assertDoubleEquals(12, pnum.getFrLinInf());
        assertDoubleEquals(23, pnum.getFrLinSup());
        assertNotNull(data);
        final ParamCalcScenario pcal = data.getPCAL();
        assertNotNull(pcal);
        assertNotNull(pcal.getPdtRes());
        assertEquals("P0Y0M0DT1H0M0S", DateDurationConverter.durationToXsd(((PdtCst) pcal.getPdtRes()).getPdtCst()));
        assertNotNull(pcal.getDateDebSce());
        assertEquals("0001-01-01T00:00:00.000", DateDurationConverter.dateToXsd(pcal.getDateDebSce()));
        assertEquals("P0Y0M1DT0H0M0S", DateDurationConverter.durationToXsd(pcal.getDureeSce()));
        final Pdt pdt = pnum.getParamNumCalcPseudoPerm().getPdt();
        assertTrue(pdt instanceof PdtCst);
        assertEquals("P0Y0M0DT1H0M0S", DateDurationConverter.durationToXsd(((PdtCst) pdt).getPdtCst()));
        assertEquals(0.01, pnum.getParamNumCalcPseudoPerm().getTolMaxQ());
        assertEquals(0.001, pnum.getParamNumCalcPseudoPerm().getTolMaxZ());
        assertEquals(50, pnum.getParamNumCalcPseudoPerm().getNbrPdtMax());
        assertEquals(0, pnum.getParamNumCalcPseudoPerm().getNbrPdtDecoup());
        assertEquals("P0Y0M0DT0H15M0S", DateDurationConverter.durationToXsd(((PdtCst) pnum.getParamNumCalcTrans().getPdt()).getPdtCst()));
        final List<Loi> dlhy = data.getLois();
        assertNotNull(dlhy);
        int countLoiDF = 0;
        int countLoiFF = 0;
        for (final Loi loi : dlhy) {
            if (loi instanceof LoiDF) {
                countLoiDF++;
                if (((LoiDF) loi).getNom().equals("HydrogrammeN1")) {
                    testHydrogrammeN1(loi);
                } else if (((LoiDF) loi).getNom().equals("ManoeuvreB8")) {
                    testManoeuvreB8(loi);
                }
            } else if (loi instanceof LoiFF) {
                countLoiFF++;
                if (((LoiFF) loi).getNom().equals("TarrageN5")) {
                    testTarrageN5(loi);
                }
            }
        }
        assertEquals(2, countLoiDF);
        assertEquals(1, countLoiFF);
        final DonCLimMScenario dclm = data.getConditionsLim();
        assertNotNull(dclm);
        for (final CalcPseudoPerm calculP : dclm.getCalcPseudoPerm()) {
            if (calculP.getNom().equals("CP1")) {
                final CalcPseudoPermNoeudQapp noeudNivContQapp = calculP.getCalcPseudoPermNoeudQapp().iterator().next();
                assertEquals(noeudNivContQapp.getQapp(), 100.0);
                CatEMHNoeud noeud = data.findNoeudByReference(noeudNivContQapp.getEmh().getNom());
                assertNotNull(noeud);
                List<DonCLimM> donclims = noeud.getDCLM();
                assertNotNull(donclims);
                boolean trouve = false;
                for (final DonCLimM donclim : donclims) {
                    if (donclim instanceof CalcPseudoPermNoeudQapp) {
                        trouve = true;
                        assertEquals(((CalcPseudoPermNoeudQapp) donclim).getQapp(), 100.0);
                        break;
                    }
                }
                assertTrue(trouve);
                final CalcPseudoPermNoeudNiveauContinuZimp noeudNivContZ = calculP.getCalcPseudoPermNoeudNiveauContinuZimp().iterator().next();
                assertEquals(noeudNivContZ.getZimp(), 1.5);
                noeud = data.findNoeudByReference(noeudNivContZ.getEmh().getNom());
                assertNotNull(noeud);
                donclims = noeud.getDCLM();
                assertNotNull(donclims);
                trouve = false;
                for (final DonCLimM donclim : donclims) {
                    if (donclim instanceof CalcPseudoPermNoeudNiveauContinuZimp) {
                        trouve = true;
                        assertEquals(((CalcPseudoPermNoeudNiveauContinuZimp) donclim).getZimp(), 1.5);
                        break;
                    }
                }
                assertTrue(trouve);
                for (final CalcPseudoPermBrancheOrificeManoeuvre brancheCast : calculP.getCalcPseudoPermBrancheOrificeManoeuvre()) {
                    assertEquals(brancheCast.getSensOuv(), EnumSensOuv.OUV_VERS_HAUT);
                    assertEquals(brancheCast.getOuv(), 100.0);
                    final CatEMHBranche brancheEMH = data.findBrancheByReference(brancheCast.getEmh().getNom());
                    assertNotNull(brancheEMH);
                    donclims = brancheEMH.getDCLM();
                    assertNotNull(donclims);
                    trouve = false;
                    for (final DonCLimM donclim : donclims) {
                        if (donclim instanceof CalcPseudoPermBrancheOrificeManoeuvre) {
                            trouve = true;
                            assertEquals(((CalcPseudoPermBrancheOrificeManoeuvre) donclim).getSensOuv(), EnumSensOuv.OUV_VERS_HAUT);
                            assertEquals(((CalcPseudoPermBrancheOrificeManoeuvre) donclim).getOuv(), 100.0);
                            break;
                        }
                    }
                    assertTrue(trouve);
                }
            }
        }
        for (final CalcTrans calculT : dclm.getCalcTrans()) {
            if (calculT.getNom().equals("CT1")) {
                final CalcTransNoeudQapp noeudCastQapp = calculT.getCalcTransNoeudQapp().iterator().next();
                LoiDF lois = noeudCastQapp.getHydrogrammeQapp();
                assertNotNull(lois);
                assertEquals(lois.getNom(), "LoiTQapp_N1");
                CatEMHNoeud noeud = data.findNoeudByReference(noeudCastQapp.getEmh().getNom());
                assertNotNull(noeud);
                List<DonCLimM> donclims = noeud.getDCLM();
                assertNotNull(donclims);
                boolean trouve = false;
                for (final DonCLimM donclim : donclims) {
                    if (donclim instanceof CalcTransNoeudQapp) {
                        trouve = true;
                        lois = ((CalcTransNoeudQapp) donclim).getHydrogrammeQapp();
                        assertNotNull(lois);
                        assertEquals(lois.getNom(), "LoiTQapp_N1");
                        assertEquals(lois.getEvolutionFF().getPtEvolutionFF().size(), 17);
                        break;
                    }
                }
                assertTrue(trouve);
                final CalcTransNoeudNiveauContinuTarage noeudCast = calculT.getCalcTransNoeudNiveauContinuTarage().iterator().next();
                LoiFF loiTarrage = noeudCast.getTarage();
                assertNotNull(loiTarrage);
                assertEquals(loiTarrage.getNom(), "LoiQZ_N5");
                noeud = data.findNoeudByReference(noeudCast.getEmh().getNom());
                assertNotNull(noeud);
                donclims = noeud.getDCLM();
                assertNotNull(donclims);
                trouve = false;
                for (final DonCLimM donclim : donclims) {
                    if (donclim instanceof CalcTransNoeudNiveauContinuTarage) {
                        trouve = true;
                        loiTarrage = ((CalcTransNoeudNiveauContinuTarage) donclim).getTarage();
                        assertNotNull(loiTarrage);
                        assertEquals(loiTarrage.getNom(), "LoiQZ_N5");
                        assertEquals(loiTarrage.getEvolutionFF().getPtEvolutionFF().size(), 10);
                        break;
                    }
                }
                assertTrue(trouve);
                for (final CalcTransBrancheOrificeManoeuvre brancheCast : calculT.getCalcTransBrancheOrificeManoeuvre()) {
                    assertEquals(brancheCast.getSensOuv(), EnumSensOuv.OUV_VERS_HAUT);
                    LoiDF loiManoeuvre = brancheCast.getManoeuvre();
                    assertNotNull(loiManoeuvre);
                    assertEquals(loiManoeuvre.getNom(), "LoiTOuv_B8");
                    final CatEMHBranche br = data.findBrancheByReference(brancheCast.getEmh().getNom());
                    assertNotNull(br);
                    donclims = br.getDCLM();
                    assertNotNull(donclims);
                    trouve = false;
                    for (final DonCLimM donclim : donclims) {
                        if (donclim instanceof CalcTransBrancheOrificeManoeuvre) {
                            trouve = true;
                            assertEquals(((CalcTransBrancheOrificeManoeuvre) donclim).getSensOuv(), EnumSensOuv.OUV_VERS_HAUT);
                            loiManoeuvre = ((CalcTransBrancheOrificeManoeuvre) donclim).getManoeuvre();
                            assertNotNull(loiManoeuvre);
                            assertEquals(loiManoeuvre.getNom(), "LoiTOuv_B8");
                            assertEquals(loiManoeuvre.getEvolutionFF().getPtEvolutionFF().size(), 4);
                            break;
                        }
                    }
                    assertTrue(trouve);
                }
            }
        }
        final DonPrtCIniBranche brancheB1 = (DonPrtCIniBranche) EMHHelper.collectDPTI(data.findBrancheByReference("B1")).get(0);
        assertEquals(100.0, brancheB1.getQini(), 0.001);
        final DonPrtCIniBranche brancheSVB4 = (DonPrtCIniBranche) EMHHelper.collectDPTI(data.findBrancheByReference("B4")).get(0);
        assertEquals(100.0, brancheSVB4.getQini(), 0.001);
        final DonPrtCIniBranche brancheSVB5 = (DonPrtCIniBranche) EMHHelper.collectDPTI(data.findBrancheByReference("B5")).get(0);
        assertEquals(0.0, brancheSVB5.getQini(), 0.001);
        final DonPrtCIniBranche brancheSVB8 = (DonPrtCIniBranche) EMHHelper.collectDPTI(data.findBrancheByReference("B8")).get(0);
        assertEquals(0.0, brancheSVB8.getQini(), 0.001);
        assertEquals(3.676, ((DonPrtCIniNoeudNiveauContinu) EMHHelper.collectDPTI(data.findNoeudByReference("N1")).get(0)).getZini(), 0.001);
        assertEquals(3.222, ((DonPrtCIniNoeudNiveauContinu) EMHHelper.collectDPTI(data.findNoeudByReference("N2")).get(0)).getZini(), 0.001);
        assertEquals(2.779, ((DonPrtCIniNoeudNiveauContinu) EMHHelper.collectDPTI(data.findNoeudByReference("N3")).get(0)).getZini(), 0.001);
        assertEquals(2.624, ((DonPrtCIniNoeudNiveauContinu) EMHHelper.collectDPTI(data.findNoeudByReference("N4")).get(0)).getZini(), 0.001);
        assertEquals(2.000, ((DonPrtCIniNoeudNiveauContinu) EMHHelper.collectDPTI(data.findNoeudByReference("N5")).get(0)).getZini(), 0.001);
        assertEquals(2.926, ((DonPrtCIniNoeudNiveauContinu) EMHHelper.collectDPTI(data.findNoeudByReference("N6")).get(0)).getZini(), 0.001);
        assertEquals(2.926, ((DonPrtCIniNoeudNiveauContinu) EMHHelper.collectDPTI(data.findNoeudByReference("N7")).get(0)).getZini(), 0.001);
        final EMHBrancheSaintVenant brancheSV = (EMHBrancheSaintVenant) data.findBrancheByReference("B1");
        controleRuisBrancheSV(brancheSV, isApresEcriture);
        final EMHBrancheSaintVenant brancheSV2 = (EMHBrancheSaintVenant) data.findBrancheByReference("B2");
        controleRuisBrancheSV(brancheSV2, isApresEcriture);
        final EMHBrancheSaintVenant brancheSV4 = (EMHBrancheSaintVenant) data.findBrancheByReference("B4");
        controleRuisBrancheSV(brancheSV4, isApresEcriture);
        final EMHBrancheSeuilTransversal brancheTransversal = (EMHBrancheSeuilTransversal) data.findBrancheByReference("B3");
        assertNotNull(brancheTransversal);
        final List<DonCalcSansPrt> listDcsps = brancheTransversal.getDCSP();
        if (listDcsps.size() > 0) {
            final DonCalcSansPrtBrancheSeuilTransversal donnee2 = (DonCalcSansPrtBrancheSeuilTransversal) listDcsps.get(0);
            assertNotNull(donnee2);
            assertEquals(EnumFormulePdc.BORDA, donnee2.getFormulePdc());
            assertEquals(20.0, donnee2.getElemSeuilAvecPdc().get(0).getLargeur());
            assertEquals(0.60, donnee2.getElemSeuilAvecPdc().get(0).getZseuil());
            assertEquals(0.90, donnee2.getElemSeuilAvecPdc().get(0).getCoefD());
            assertEquals(1.00, donnee2.getElemSeuilAvecPdc().get(0).getCoefPdc());
        }
    }

    /**
   * @param brancheSV
   */
    private void controleRuisBrancheSV(final EMHBrancheSaintVenant brancheSV, final boolean isApresEcriture) {
        assertNotNull(brancheSV);
        final List<DonCalcSansPrt> listDcsps = brancheSV.getDCSP();
        if (listDcsps.size() > 0) {
            final DonCalcSansPrtBrancheSaintVenant donnee = (DonCalcSansPrtBrancheSaintVenant) listDcsps.get(0);
            assertNotNull(donnee);
            assertEquals(1.0, donnee.getCoefBeta());
            assertEquals(0.0, donnee.getCoefRuisQdm());
            if (!isApresEcriture) {
                assertEquals(0.0, donnee.getCoefRuis());
            } else {
                assertEquals(1.0, donnee.getCoefRuis());
            }
        }
    }

    /**
   * @param loi
   */
    private void testTarrageN5(final Loi loi) {
        final LoiFF loiTarrage = (LoiFF) loi;
        assertEquals(loiTarrage.getCommentaire(), "TarrageN5");
        assertEquals(EnumTypeLoi.LoiQDz, loiTarrage.getType());
        final EvolutionFF evolFF = loiTarrage.getEvolutionFF();
        assertNotNull(evolFF);
        int countPoint = 0;
        for (final PtEvolutionFF ptEvolFF : evolFF.getPtEvolutionFF()) {
            if (countPoint == 0) {
                assertDoubleEquals(ptEvolFF.getAbscisse(), 1.0);
                assertDoubleEquals(ptEvolFF.getOrdonnee(), 0.0);
            } else if (countPoint == 3) {
                assertDoubleEquals(ptEvolFF.getAbscisse(), 2.5);
                assertDoubleEquals(ptEvolFF.getOrdonnee(), -175.0);
            } else if (countPoint == 9) {
                assertDoubleEquals(ptEvolFF.getAbscisse(), 5.5);
                assertDoubleEquals(ptEvolFF.getOrdonnee(), -500.0);
            }
            countPoint++;
        }
    }

    /**
   * @param loi
   */
    private void testManoeuvreB8(final Loi loi) {
        final LoiDF loiVanne = (LoiDF) loi;
        assertEquals(loiVanne.getCommentaire(), "ManoeuvreB8");
        assertEquals(EnumTypeLoi.LoiTOuv, loiVanne.getType());
        final EvolutionFF evolDF = loiVanne.getEvolutionFF();
        assertNotNull(evolDF);
        int countPoint = 0;
        for (final PtEvolutionFF ptEvolDF : evolDF.getPtEvolutionFF()) {
            if (countPoint == 0) {
                assertDoubleEquals(ptEvolDF.getAbscisse(), 0);
                assertDoubleEquals(ptEvolDF.getOrdonnee(), 90.0);
            } else if (countPoint == 3) {
                assertDoubleEquals(ptEvolDF.getAbscisse(), 86400);
                assertDoubleEquals(ptEvolDF.getOrdonnee(), 100.0);
            }
            countPoint++;
        }
    }

    /**
   * @param loi
   */
    private void testHydrogrammeN1(final Loi loi) {
        final LoiDF loiHydrogramme = (LoiDF) loi;
        assertEquals(loiHydrogramme.getCommentaire(), "HydrogrammeN1");
        assertEquals(EnumTypeLoi.LoiTQapp, loiHydrogramme.getType());
        final EvolutionFF evolDF = loiHydrogramme.getEvolutionFF();
        assertNotNull(evolDF);
        int countPoint = 0;
        for (final PtEvolutionFF ptEvolDF : evolDF.getPtEvolutionFF()) {
            if (countPoint == 0) {
                assertDoubleEquals(0, ptEvolDF.getAbscisse());
                assertDoubleEquals(100.0, ptEvolDF.getOrdonnee());
            } else if (countPoint == 6) {
                assertDoubleEquals(32400, ptEvolDF.getAbscisse());
                assertEquals(450.0, ptEvolDF.getOrdonnee());
            } else if (countPoint == 16) {
                assertDoubleEquals(86400, ptEvolDF.getAbscisse());
                assertDoubleEquals(100.0, ptEvolDF.getOrdonnee());
            }
            countPoint++;
        }
    }

    /**
   * @param analyzer
   * @param path
   * @return
   */
    private CrueIOResu<CrueData> readModeleCrue9(final CtuluLog analyzer, final String path) {
        final CrueIOResu<CrueData> read = ReadHelper.readModele(analyzer, FICHIER_TEST_MODELE3_DC, path);
        testAnalyser(analyzer);
        return read;
    }

    public void testLectureUniquement() {
        testLectureUniquement(TestCrueDC.FICHIER_TEST_MODELE4_DC, FICHIER_TEST_MODELE4_DH);
        testLectureUniquement(TestCrueDC.FICHIER_TEST_MODELE5_DC, FICHIER_TEST_MODELE5_DH);
        testLectureUniquement(TestCrueDC.FICHIER_TEST_MODELE6_DC, FICHIER_TEST_MODELE6_DH);
    }

    /**
   * @param dc
   * @param dh
   * @return
   */
    public CrueData testLectureUniquement(final String dc, final String dh) {
        final CtuluLog analyze = new CtuluLog();
        CrueIOResu<CrueData> read = Crue9FileFormatFactory.getDCFileFormat().read(dc, analyze, createDefaultCrueData());
        testAnalyser(analyze);
        read = Crue9FileFormatFactory.getDHFileFormat().read(dh, analyze, read.getMetier());
        testAnalyser(analyze);
        return read.getMetier();
    }
}
