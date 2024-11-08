package backend.mapping.neighborhood.test;

import java.io.File;
import java.io.IOException;
import backend.core.AbstractConcept;
import backend.core.AbstractONDEXGraph;
import backend.core.AbstractONDEXGraphMetaData;
import backend.core.AbstractONDEXIterator;
import backend.core.AbstractRelation;
import backend.core.AbstractRelationTypeSet;
import backend.core.CV;
import backend.core.ConceptClass;
import backend.core.EvidenceType;
import backend.core.RelationType;
import backend.core.persistent.berkeley.BerkeleyEnv;
import backend.core.searchable.LuceneEnv;
import backend.core.security.Session;
import backend.logging.ONDEXLogger;
import backend.tools.DirUtils;

public class CreateExampleOntologies {

    private Session s = Session.NONE;

    private AbstractONDEXGraph aog;

    private AbstractONDEXGraphMetaData md;

    private ConceptClass ccElement;

    private ConceptClass ccCompound;

    private ConceptClass ccEnv;

    private CV chemicalAccCv;

    private AbstractRelationTypeSet rts;

    private BerkeleyEnv benv;

    private LuceneEnv lenv;

    CreateExampleOntologies() throws Exception {
        String dbdir = getTempDir() + File.separator + System.currentTimeMillis();
        File dir = new File(dbdir);
        if (dir.exists()) DirUtils.deleteTree(dir);
        dir.mkdir();
        if (!dir.canRead() || !dir.canWrite()) {
            throw new IOException("Cant read or write directory " + dbdir);
        }
        benv = new BerkeleyEnv(s, dbdir, new ONDEXLogger());
        aog = benv.getONDEXGraph();
        md = aog.getONDEXGraphData(s);
        ccElement = md.createConceptClass(s, "Element");
        ccCompound = md.createConceptClass(s, "Compound");
        ccEnv = md.createConceptClass(s, "Environment");
        chemicalAccCv = md.createCV(s, "ChemicalAccession");
        RelationType rt = md.createRelationType(s, "is_componentof");
        rts = md.createRelationTypeSet(s, "is_componentof", rt);
        CV cv = md.createCV(s, "ChemicalOntologyOne");
        CV cv2 = md.createCV(s, "ChemicalOntologyTwo");
        CV cv3 = md.createCV(s, "ChemicalOntologyThree");
        createChemicalOntology(true, true, true, true, cv);
        createChemicalOntology(true, true, true, false, cv2);
        createChemicalOntology(false, false, false, false, cv3);
        lenv = new LuceneEnv(s, dir + File.separator + "index", true);
        lenv.addONDEXListener(new ONDEXLogger());
        lenv.setONDEXGraph(aog);
        AbstractONDEXIterator<AbstractConcept> cit = lenv.getONDEXGraph().getConcepts(s);
        System.out.println(cit.size() + " Concepts made");
        cit.close();
        cit = null;
        AbstractONDEXIterator<AbstractRelation> rit = lenv.getONDEXGraph().getRelations(s);
        System.out.println(rit.size() + " Relations made");
        rit.close();
        rit = null;
    }

    public void createChemicalOntology(boolean incCAccess, boolean incHAccess, boolean incMethaneAccess, boolean incCo2Access, CV cv) {
        EvidenceType et = md.createEvidenceType(s, "CONTRIVED");
        AbstractConcept h = aog.createConcept(s, "H", cv, ccElement, et);
        if (incHAccess) h.createConceptAccession(s, "H", chemicalAccCv);
        AbstractConcept c = aog.createConcept(s, "C", cv, ccElement, et);
        if (incCAccess) c.createConceptAccession(s, "C", chemicalAccCv);
        AbstractConcept n = aog.createConcept(s, "N", cv, ccElement, et);
        n.createConceptAccession(s, "N", chemicalAccCv);
        AbstractConcept o = aog.createConcept(s, "O", cv, ccElement, et);
        o.createConceptAccession(s, "O", chemicalAccCv);
        AbstractConcept na = aog.createConcept(s, "Na", cv, ccElement, et);
        na.createConceptAccession(s, "Na", chemicalAccCv);
        AbstractConcept cl = aog.createConcept(s, "Cl", cv, ccElement, et);
        cl.createConceptAccession(s, "Cl", chemicalAccCv);
        AbstractConcept co2 = aog.createConcept(s, "CO2", cv, ccCompound, et);
        if (incCo2Access) co2.createConceptAccession(s, "CO2", chemicalAccCv);
        AbstractConcept h2o = aog.createConcept(s, "H20", cv, ccCompound, et);
        h2o.createConceptAccession(s, "H20", chemicalAccCv);
        AbstractConcept nacl = aog.createConcept(s, "NaCl", cv, ccCompound, et);
        nacl.createConceptAccession(s, "NaCl", chemicalAccCv);
        AbstractConcept naoh = aog.createConcept(s, "NaOH", cv, ccCompound, et);
        naoh.createConceptAccession(s, "NaOH", chemicalAccCv);
        AbstractConcept ch3 = aog.createConcept(s, "CH3", cv, ccCompound, et);
        if (incMethaneAccess) ch3.createConceptAccession(s, "CH3", chemicalAccCv);
        aog.createRelation(s, c, co2, rts, et);
        aog.createRelation(s, o, co2, rts, et);
        aog.createRelation(s, h, h2o, rts, et);
        aog.createRelation(s, o, h2o, rts, et);
        aog.createRelation(s, na, nacl, rts, et);
        aog.createRelation(s, cl, nacl, rts, et);
        aog.createRelation(s, na, naoh, rts, et);
        aog.createRelation(s, o, naoh, rts, et);
        aog.createRelation(s, h, naoh, rts, et);
        aog.createRelation(s, c, ch3, rts, et);
        aog.createRelation(s, h, ch3, rts, et);
        AbstractConcept atmos = aog.createConcept(s, "Atmosphere", cv, ccEnv, et);
        atmos.createConceptAccession(s, "Atmos", chemicalAccCv);
        AbstractConcept ocean = aog.createConcept(s, "Ocean", cv, ccEnv, et);
        ocean.createConceptAccession(s, "Ocean", chemicalAccCv);
        AbstractConcept home = aog.createConcept(s, "Home", cv, ccEnv, et);
        home.createConceptAccession(s, "Home", chemicalAccCv);
        aog.createRelation(s, co2, atmos, rts, et);
        aog.createRelation(s, ch3, atmos, rts, et);
        aog.createRelation(s, co2, ocean, rts, et);
        aog.createRelation(s, h2o, ocean, rts, et);
        aog.createRelation(s, ch3, ocean, rts, et);
        aog.createRelation(s, naoh, ocean, rts, et);
        aog.createRelation(s, naoh, home, rts, et);
        aog.createRelation(s, co2, home, rts, et);
        aog.createRelation(s, h2o, home, rts, et);
    }

    public AbstractONDEXGraph getGraph() {
        return aog;
    }

    private static String getTempDir() {
        File f = null;
        try {
            f = File.createTempFile("tmpjava", "r");
        } catch (IOException e) {
            e.printStackTrace();
        }
        File dir = f.getParentFile();
        f.delete();
        return dir.getAbsolutePath();
    }

    public BerkeleyEnv getBenv() {
        return benv;
    }

    public void setBenv(BerkeleyEnv benv) {
        this.benv = benv;
    }

    public LuceneEnv getLenv() {
        return lenv;
    }
}
