package edu.unibi.agbi.biodwh.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import org.hibernate.HibernateException;
import org.hibernate.StatelessSession;
import org.hibernate.exception.ConstraintViolationException;
import edu.unibi.agbi.biodwh.config.log.Log;
import edu.unibi.agbi.biodwh.database.ParserConnector;
import edu.unibi.agbi.biodwh.entity.go.Association;
import edu.unibi.agbi.biodwh.entity.go.AssociationQualifier;
import edu.unibi.agbi.biodwh.entity.go.AssociationSpeciesQualifier;
import edu.unibi.agbi.biodwh.entity.go.Db;
import edu.unibi.agbi.biodwh.entity.go.Dbxref;
import edu.unibi.agbi.biodwh.entity.go.Evidence;
import edu.unibi.agbi.biodwh.entity.go.EvidenceDbxref;
import edu.unibi.agbi.biodwh.entity.go.GeneProduct;
import edu.unibi.agbi.biodwh.entity.go.GeneProductCount;
import edu.unibi.agbi.biodwh.entity.go.GeneProductHomolset;
import edu.unibi.agbi.biodwh.entity.go.GeneProductSynonym;
import edu.unibi.agbi.biodwh.entity.go.GeneProductSynonymId;
import edu.unibi.agbi.biodwh.entity.go.GraphPath;
import edu.unibi.agbi.biodwh.entity.go.Homolset;
import edu.unibi.agbi.biodwh.entity.go.RelationComposition;
import edu.unibi.agbi.biodwh.entity.go.RelationProperties;
import edu.unibi.agbi.biodwh.entity.go.SourceAudit;
import edu.unibi.agbi.biodwh.entity.go.Species;
import edu.unibi.agbi.biodwh.entity.go.Term;
import edu.unibi.agbi.biodwh.entity.go.Term2term;
import edu.unibi.agbi.biodwh.entity.go.Term2termMetadata;
import edu.unibi.agbi.biodwh.entity.go.TermDbxref;
import edu.unibi.agbi.biodwh.entity.go.TermDbxrefId;
import edu.unibi.agbi.biodwh.entity.go.TermDefinition;
import edu.unibi.agbi.biodwh.entity.go.TermSubset;
import edu.unibi.agbi.biodwh.entity.go.TermSynonym;

/**
 * @author Benjamin Kormeier
 * @version 1.0 17.02.2009
 */
public class GOParser extends BioDWHParser implements FilenameFilter {

    private StatelessSession session = null;

    private final String SEPARATOR_TAB = new String("\t");

    private int commit_point = 0;

    private int COMMIT = 40000;

    private int progress = 0;

    private int read_position = 0;

    private long file_length = 0;

    private boolean abort = false;

    private void writeInit() {
        Dbxref dbxref = new Dbxref();
        dbxref.setId(0);
        dbxref.setXrefDbname("unknown");
        dbxref.setXrefKey("unknown");
        dbxref.setXrefKeytype("unknown");
        insertObject(dbxref);
        GeneProduct gp = new GeneProduct();
        gp.setId(0);
        gp.setFullName("unknown");
        gp.setSymbol("unknown");
        gp.setDbxref(dbxref);
        insertObject(gp);
    }

    private void writeDbxref(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Dbxref entry = new Dbxref();
            entry.setId(Integer.valueOf(data[0]));
            if (!data[1].equals("\\N")) entry.setXrefDbname(data[1]);
            if (!data[2].equals("\\N")) {
                if (data[1].equalsIgnoreCase("ec")) {
                    if (data[2].startsWith("EC")) data[2] = data[2].replace("EC", "");
                    if (data[2].startsWith(":")) data[2] = data[2].replace(":", "");
                }
                entry.setXrefKey(data[2]);
            }
            if (!data[3].equals("\\N")) entry.setXrefKeytype(data[3]);
            insertObject(entry);
        }
        reader.close();
    }

    private void writeDb(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Db entry = new Db();
            entry.setId(Integer.valueOf(data[0]));
            if (!data[1].equals("\\N")) entry.setName(data[1]);
            if (!data[2].equals("\\N")) entry.setFullname(data[2]);
            if (!data[3].equals("\\N")) entry.setDatatype(data[3]);
            if (!data[4].equals("\\N")) entry.setGenericUrl(data[4]);
            if (!data[5].equals("\\N")) entry.setUrlSyntax(data[5]);
            if (!data[6].equals("\\N")) entry.setUrlExample(data[6]);
            if (!data[7].equals("\\N")) entry.setUriPrefix(data[7]);
            insertObject(entry);
        }
        reader.close();
    }

    private void writeTerm(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Term entry = new Term();
            entry.setId(Integer.valueOf(data[0]));
            entry.setName(data[1]);
            entry.setTermType(data[2]);
            entry.setAcc(data[3]);
            entry.setIsObsolete(Integer.valueOf(data[4]));
            entry.setIsRoot(Integer.valueOf(data[5]));
            entry.setIsRelation(Integer.valueOf(data[6]));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeTerm2Term(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Term2term entry = new Term2term();
            entry.setId(Integer.valueOf(data[0]));
            entry.setTermByRelationshipTypeId((Term) session.get(Term.class, Integer.valueOf(data[1])));
            entry.setTermByTerm1Id((Term) session.get(Term.class, Integer.valueOf(data[2])));
            entry.setTermByTerm2Id((Term) session.get(Term.class, Integer.valueOf(data[3])));
            entry.setComplete(Integer.valueOf(data[4]));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeRelationProperties(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            RelationProperties entry = new RelationProperties();
            Term term = (Term) session.get(Term.class, Integer.valueOf(data[0]));
            if (term != null) entry.setTerm(term); else entry.setTerm((Term) session.get(Term.class, Integer.valueOf(1)));
            if (!data[1].equals("\\N")) entry.setIsTransitive(Integer.valueOf(data[1]));
            if (!data[2].equals("\\N")) entry.setIsSymmetric(Integer.valueOf(data[2]));
            if (!data[3].equals("\\N")) entry.setIsAntiSymmetric(Integer.valueOf(data[3]));
            if (!data[4].equals("\\N")) entry.setIsCyclic(Integer.valueOf(data[4]));
            if (!data[5].equals("\\N")) entry.setIsReflexive(Integer.valueOf(data[5]));
            if (!data[6].equals("\\N")) entry.setIsMetadataTag(Integer.valueOf(data[6]));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeRelationComposition(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            RelationComposition entry = new RelationComposition();
            entry.setId(Integer.valueOf(data[0]));
            entry.setTermByRelation1Id((Term) session.get(Term.class, Integer.valueOf(data[1])));
            entry.setTermByRelation2Id((Term) session.get(Term.class, Integer.valueOf(data[2])));
            entry.setTermByInferredRelationId((Term) session.get(Term.class, Integer.valueOf(data[3])));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeTermDefinition(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            TermDefinition entry = new TermDefinition();
            entry.setTermId((Term) session.get(Term.class, Integer.valueOf(data[0])));
            entry.setTermDefinition(data[1]);
            if (!data[2].equals("\\N")) entry.setDbxref((Dbxref) session.get(Dbxref.class, Integer.valueOf(data[2])));
            if (!data[3].equals("\\N")) entry.setTermComment(data[3]);
            if (!data[4].equals("\\N")) entry.setReference(data[4]);
            insertObject(entry);
        }
        reader.close();
    }

    private void writeTermSynonym(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            TermSynonym entry = new TermSynonym();
            entry.setTermId((Term) session.get(Term.class, Integer.valueOf(data[0].trim())));
            entry.setTermSynonym(data[1]);
            if (!data[2].equals("\\N")) entry.setAccSynonym(data[2]);
            entry.setTermSynonymTypeId((Term) session.get(Term.class, Integer.valueOf(data[3])));
            if (!data[4].equals("\\N")) entry.setTermSynonymCategoryId((Term) session.get(Term.class, Integer.valueOf(data[4])));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeTermDbxref(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            TermDbxref entry = new TermDbxref();
            TermDbxrefId id = new TermDbxrefId(Integer.valueOf(data[0]), Integer.valueOf(data[1]), Integer.valueOf(data[2]));
            entry.setId(id);
            entry.setTerm((Term) session.get(Term.class, Integer.valueOf(data[0])));
            entry.setDbxref((Dbxref) session.get(Dbxref.class, Integer.valueOf(data[1])));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeTermSubset(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            TermSubset entry = new TermSubset();
            entry.setTermId((Term) session.get(Term.class, Integer.valueOf(data[0])));
            entry.setSubsetId((Term) session.get(Term.class, Integer.valueOf(data[1])));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeTerm2TermMetadata(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Term2termMetadata entry = new Term2termMetadata();
            entry.setId(Integer.valueOf(data[0]));
            entry.setTermByRelationshipTypeId((Term) session.get(Term.class, Integer.valueOf(data[1])));
            entry.setTermByTerm1Id((Term) session.get(Term.class, Integer.valueOf(data[2])));
            entry.setTermByTerm2Id((Term) session.get(Term.class, Integer.valueOf(data[3])));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeSpecies(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Species entry = new Species();
            entry.setId(Integer.valueOf(data[0]));
            entry.setNcbiTaxaId(Integer.valueOf(data[1]));
            if (!data[2].equals("\\N")) entry.setCommonName(data[2]);
            if (!data[3].equals("\\N")) entry.setLineageString(data[3]);
            entry.setGenus(data[4]);
            entry.setSpecies(data[5]);
            if (!data[6].equals("\\N")) entry.setParentId(Integer.valueOf(data[6]));
            if (!data[6].equals("\\N")) entry.setLeftValue(Integer.valueOf(data[7]));
            if (!data[6].equals("\\N")) entry.setRightValue(Integer.valueOf(data[8]));
            entry.setTaxonomicRank(data[9]);
            insertObject(entry);
        }
        reader.close();
    }

    private void writeGeneProduct(BufferedReader reader) throws IOException {
        String line;
        boolean valid = true;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            if (line.endsWith("\\")) {
                line = line.substring(0, line.length() - 1).concat(" ");
                line = line.concat(reader.readLine());
            }
            String data[] = line.split(SEPARATOR_TAB);
            GeneProduct entry = new GeneProduct();
            if (data.length == 2) {
                line = line.replace('\\', ' ');
                String next_line = reader.readLine();
                line = line.concat(next_line);
                data = line.split("\\t");
            } else if (data.length > 2) {
                if (data[2].matches("\\D{1,}\\d*")) {
                    line = line.replace('\\', ' ');
                    ArrayList<String> tmp_data = new ArrayList<String>(data.length - 1);
                    for (String content : data) tmp_data.add(content);
                    tmp_data.set(1, data[1].concat(data[2]));
                    tmp_data.remove(2);
                    data = tmp_data.toArray(new String[tmp_data.size()]);
                }
            }
            try {
                entry.setId(Integer.valueOf(data[0]));
                entry.setSymbol(data[1]);
                entry.setDbxref((Dbxref) session.get(Dbxref.class, Integer.valueOf(data[2])));
                entry.setSpecies((Species) session.get(Species.class, Integer.valueOf(data[3])));
                entry.setTerm((Term) session.get(Term.class, Integer.valueOf(data[4])));
                if (data.length > 5) entry.setFullName(data[5]);
                valid = true;
            } catch (NumberFormatException e) {
                Log.writeWarningLog(this.getClass(), e.getMessage(), e);
                valid = false;
            }
            if (valid) {
                insertObject(entry);
            }
        }
        reader.close();
    }

    private void writeGeneProductSynonym(BufferedReader reader) throws IOException {
        String line;
        GeneProductSynonym before = new GeneProductSynonym();
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Integer id = 0;
            try {
                if (data.length == 2) {
                    id = Integer.valueOf(data[0]);
                    GeneProductSynonym entry = new GeneProductSynonym(new GeneProductSynonymId(id, data[1]), (GeneProduct) session.get(GeneProduct.class, id));
                    try {
                        insertObject(entry);
                    } catch (ConstraintViolationException e) {
                        Log.writeWarningLog(this.getClass(), e.getMessage(), e);
                    }
                    before = entry;
                }
            } catch (NumberFormatException e) {
                GeneProductSynonymId entry_id = before.getId();
                GeneProductSynonym entry = before;
                entry_id.setProductSynonym(entry_id.getProductSynonym().replace("\\", "").concat(line));
                entry.setId(entry_id);
                updateObject(entry);
            }
        }
        reader.close();
    }

    private void writeAssociation(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Association entry = new Association();
            entry.setId(Integer.valueOf(data[0]));
            entry.setTerm((Term) session.get(Term.class, Integer.valueOf(data[1])));
            GeneProduct gp = (GeneProduct) session.get(GeneProduct.class, Integer.valueOf(data[2]));
            if (gp != null) entry.setGeneProduct(gp); else {
                entry.setGeneProduct((GeneProduct) session.get(GeneProduct.class, Integer.valueOf(0)));
            }
            entry.setIsNot(Integer.valueOf(data[3]));
            if (!data[4].equals("\\N")) entry.setRoleGroup(Integer.valueOf(data[4]));
            entry.setAssocdate(Integer.valueOf(data[5]));
            Db db = (Db) session.get(Db.class, Integer.valueOf(data[6]));
            if (db != null) entry.setDb(db); else {
                entry.setDb((Db) session.get(Db.class, Integer.valueOf(1)));
            }
            insertObject(entry);
        }
        reader.close();
    }

    private void writeAssociationQualifier(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            AssociationQualifier entry = new AssociationQualifier();
            entry.setId(Integer.valueOf(data[0]));
            entry.setAssociation((Association) session.get(Association.class, Integer.valueOf(data[1])));
            entry.setTerm((Term) session.get(Term.class, Integer.valueOf(data[2])));
            if (!data[3].equals("\\N")) entry.setValue(data[3]);
            insertObject(entry);
        }
        reader.close();
    }

    private void writeAssociationSpeciesQualifier(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            AssociationSpeciesQualifier entry = new AssociationSpeciesQualifier();
            entry.setId(Integer.valueOf(data[0]));
            entry.setAssociation((Association) session.get(Association.class, Integer.valueOf(data[1])));
            entry.setSpecies((Species) session.get(Species.class, Integer.valueOf(data[2])));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeEvidence(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Evidence entry = new Evidence();
            entry.setId(Integer.valueOf(data[0]));
            entry.setCode(data[1]);
            entry.setAssociation((Association) session.get(Association.class, Integer.valueOf(data[2])));
            entry.setDbxref((Dbxref) session.get(Dbxref.class, Integer.valueOf(data[3])));
            if (data.length > 4) entry.setSeqAcc(data[4]);
            insertObject(entry);
        }
        reader.close();
    }

    private void writeEvidenceDbxref(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Evidence evidence = (Evidence) session.createQuery("from go_evidence a where a.id = :id").setParameter("id", Integer.valueOf(data[0].trim())).uniqueResult();
            Dbxref dxref = (Dbxref) session.createQuery("from go_dbxref a where a.id = :id").setParameter("id", Integer.valueOf(data[1].trim())).uniqueResult();
            if (evidence != null && dxref != null) {
                EvidenceDbxref entry = new EvidenceDbxref();
                entry.setEvidenceId(evidence);
                entry.setDbxrefId(dxref);
                try {
                    insertObject(entry);
                } catch (HibernateException e) {
                    Log.writeWarningLog(this.getClass(), e.getMessage(), e);
                }
            }
        }
        reader.close();
    }

    private void writeGraphPath(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            GraphPath entry = new GraphPath();
            entry.setId(Integer.valueOf(data[0]));
            entry.setTermByTerm1Id((Term) session.get(Term.class, Integer.valueOf(data[1])));
            entry.setTermByTerm2Id((Term) session.get(Term.class, Integer.valueOf(data[2])));
            if (!data[3].equals("\\N")) entry.setTermByRelationshipTypeId((Term) session.get(Term.class, Integer.valueOf(data[3])));
            entry.setDistance(Integer.valueOf(data[4]));
            if (!data[5].equals("\\N")) entry.setRelationDistance(Integer.valueOf(data[5]));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeGeneProductCount(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            GeneProductCount entry = new GeneProductCount();
            entry.setTerm((Term) session.get(Term.class, Integer.valueOf(data[0])));
            entry.setCode(data[1]);
            if (!data[2].equals("\\N")) entry.setSpeciesdbname(data[2]);
            if (!data[3].equals("\\N")) entry.setSpecies((Species) session.get(Species.class, Integer.valueOf(data[3])));
            entry.setProductCount(Integer.valueOf(data[4]));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeHomolset(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            Homolset entry = new Homolset();
            entry.setId(Integer.valueOf(data[0]));
            entry.setSymbol(data[1]);
            entry.setDbxref((Dbxref) session.get(Dbxref.class, Integer.valueOf(data[2])));
            if (!data[3].equals("\\N")) entry.setGeneProduct((GeneProduct) session.get(GeneProduct.class, Integer.valueOf(data[3])));
            if (!data[4].equals("\\N")) entry.setSpecies((Species) session.get(Species.class, Integer.valueOf(data[4])));
            if (!data[5].equals("\\N")) entry.setTerm((Term) session.get(Term.class, Integer.valueOf(data[5])));
            if (!data[6].equals("\\N")) entry.setDescription(data[6]);
            insertObject(entry);
        }
        reader.close();
    }

    private void writeGeneProductHomolset(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            GeneProductHomolset entry = new GeneProductHomolset();
            entry.setId(Integer.valueOf(data[0]));
            entry.setGeneProduct((GeneProduct) session.get(GeneProduct.class, Integer.valueOf(data[1])));
            entry.setHomolset((Homolset) session.get(Homolset.class, Integer.valueOf(data[2])));
            insertObject(entry);
        }
        reader.close();
    }

    private void writeSourceAudit(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !abort) {
            progress = calculateProgress(line.length());
            String data[] = line.split(SEPARATOR_TAB);
            SourceAudit entry = new SourceAudit();
            entry.setSourceId(data[0]);
            entry.setSourceFullpath(data[1]);
            entry.setSourcePath(data[2]);
            entry.setSourceType(data[3]);
            entry.setSourceMd5(data[4]);
            entry.setSourceParsetime(Integer.valueOf(data[5]));
            entry.setSourceMtime(Integer.valueOf(data[6]));
            insertObject(entry);
        }
        reader.close();
    }

    private void insertObject(Object obj) {
        if (!session.getTransaction().isActive()) {
            session.beginTransaction();
        }
        session.insert(obj);
        commit_point++;
        if (commit_point == COMMIT) {
            session.getTransaction().commit();
            commit_point = 0;
        }
    }

    private void updateObject(Object obj) {
        if (!session.getTransaction().isActive()) {
            session.beginTransaction();
        }
        session.update(obj);
        commit_point++;
        if (commit_point == COMMIT) {
            session.getTransaction().commit();
            commit_point = 0;
        }
    }

    private int calculateProgress(int line_length) {
        read_position += line_length;
        double result = (double) read_position / (double) file_length;
        return (int) (result * 100);
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#abort()
	 */
    @Override
    public void abort() {
        abort = true;
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#getCreationDate()
	 */
    @Override
    public Date getCreationDate() {
        return new GregorianCalendar(2009, 2, 17).getTime();
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#getDefaultDownloadURL()
	 */
    @Override
    public String getDefaultDownloadURL() {
        return "http://archive.geneontology.org/latest-full/";
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#getEntityPackage()
	 */
    @Override
    public String getEntityPackage() {
        return new String("edu.unibi.agbi.biodwh.entity.go");
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#getFileNames()
	 */
    @Override
    public String[] getFileNames() {
        return new String[] { "go_201111-assocdb-tables.tar.gz" };
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#getParserAuthor()
	 */
    @Override
    public String getParserAuthor() {
        return new String("Benjamin Kormeier");
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#getParserDescription()
	 */
    @Override
    public String getParserDescription() {
        return new String("GO parser works with 201111 assocdb tables.");
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#getParserID()
	 */
    @Override
    public String getParserID() {
        return new String("unibi.go");
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#getParserName()
	 */
    @Override
    public String getParserName() {
        return new String("GO Parser");
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#getProgress()
	 */
    @Override
    public int getProgress() {
        return progress;
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#getVersion()
	 */
    @Override
    public double getVersion() {
        return 1.0;
    }

    /** 
	 * @see edu.unibi.agbi.biodwh.parser.BioDWHParser#start(edu.unibi.agbi.biodwh.database.ParserConnector, java.lang.String)
	 */
    @Override
    public void start(ParserConnector connector, String source_dir) throws Throwable {
        BufferedReader reader = null;
        session = connector.getStatelessSession();
        session.beginTransaction();
        File[] files = new File(source_dir).listFiles(this);
        for (File file : files) {
            file_length += file.length();
        }
        writeInit();
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "dbxref.txt")));
        writeDbxref(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "db.txt")));
        writeDb(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "term.txt")));
        writeTerm(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "term2term.txt")));
        writeTerm2Term(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "relation_properties.txt")));
        writeRelationProperties(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "relation_composition.txt")));
        writeRelationComposition(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "term_definition.txt")));
        writeTermDefinition(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "term_synonym.txt")));
        writeTermSynonym(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "term_dbxref.txt")));
        writeTermDbxref(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "term_subset.txt")));
        writeTermSubset(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "term2term_metadata.txt")));
        writeTerm2TermMetadata(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "species.txt")));
        writeSpecies(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "gene_product.txt")));
        writeGeneProduct(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "gene_product_synonym.txt")));
        writeGeneProductSynonym(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "association.txt")));
        writeAssociation(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "association_qualifier.txt")));
        writeAssociationQualifier(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "association_species_qualifier.txt")));
        writeAssociationSpeciesQualifier(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "evidence.txt")));
        writeEvidence(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "evidence_dbxref.txt")));
        writeEvidenceDbxref(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "graph_path.txt")));
        writeGraphPath(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "gene_product_count.txt")));
        writeGeneProductCount(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "homolset.txt")));
        writeHomolset(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "gene_product_homolset.txt")));
        writeGeneProductHomolset(reader);
        session.getTransaction().commit();
        reader = new BufferedReader(new FileReader(new File(source_dir + File.separator + "source_audit.txt")));
        writeSourceAudit(reader);
        session.getTransaction().commit();
        if (session.getTransaction().isActive()) {
            session.getTransaction().commit();
        }
        session.close();
    }

    public boolean accept(File dir, String name) {
        if (name.endsWith(".txt")) return true; else return false;
    }
}
