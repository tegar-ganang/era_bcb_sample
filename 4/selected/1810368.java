package net.sourceforge.seqware.queryengine.backend.store.impl;

import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.comparators.ContigPositionComparator;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.comparators.TagComparator;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.keycreators.ContigPositionKeyCreator;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.keycreators.ConsequenceVariantIdKeyCreator;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.keycreators.TagKeyOnlyCreator;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.tuplebinders.ConsequenceTB;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.tuplebinders.CoverageTB;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.tuplebinders.ContigPositionTB;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.tuplebinders.FeatureTB;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.tuplebinders.StringIdTB;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.tuplebinders.VariantTB;
import net.sourceforge.seqware.queryengine.backend.io.berkeleydb.tuplebinders.TagTB;
import net.sourceforge.seqware.queryengine.backend.model.Consequence;
import net.sourceforge.seqware.queryengine.backend.model.Coverage;
import net.sourceforge.seqware.queryengine.backend.model.Feature;
import net.sourceforge.seqware.queryengine.backend.model.LocatableModel;
import net.sourceforge.seqware.queryengine.backend.model.Model;
import net.sourceforge.seqware.queryengine.backend.model.Variant;
import net.sourceforge.seqware.queryengine.backend.model.ContigPosition;
import net.sourceforge.seqware.queryengine.backend.model.StringId;
import net.sourceforge.seqware.queryengine.backend.model.Tag;
import net.sourceforge.seqware.queryengine.backend.store.Store;
import net.sourceforge.seqware.queryengine.backend.util.SeqWareIterator;
import net.sourceforge.seqware.queryengine.backend.util.SeqWareSettings;
import net.sourceforge.seqware.queryengine.backend.util.iterators.LocatableSecondaryCursorIterator;
import net.sourceforge.seqware.queryengine.backend.util.iterators.CursorIterator;
import net.sourceforge.seqware.queryengine.backend.util.iterators.PostgresModelIterator;
import net.sourceforge.seqware.queryengine.backend.util.iterators.PostgresTagModelIterator;
import net.sourceforge.seqware.queryengine.backend.util.iterators.PostgresVariantModelIterator;
import net.sourceforge.seqware.queryengine.backend.util.iterators.SecondaryCursorIterator;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.db.BtreeStats;
import com.sleepycat.db.CheckpointConfig;
import com.sleepycat.db.Cursor;
import com.sleepycat.db.CursorConfig;
import com.sleepycat.db.DatabaseConfig;
import com.sleepycat.db.DatabaseEntry;
import com.sleepycat.db.DatabaseException;
import com.sleepycat.db.DatabaseType;
import com.sleepycat.db.DeadlockException;
import com.sleepycat.db.Environment;
import com.sleepycat.db.EnvironmentConfig;
import com.sleepycat.db.Database;
import com.sleepycat.db.LockDetectMode;
import com.sleepycat.db.LockMode;
import com.sleepycat.db.OperationStatus;
import com.sleepycat.db.SecondaryConfig;
import com.sleepycat.db.SecondaryCursor;
import com.sleepycat.db.SecondaryDatabase;
import com.sleepycat.db.StatsConfig;
import com.sleepycat.db.Transaction;
import com.sleepycat.db.TransactionConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

/**
 * @author boconnor
 * 
 * This is a simple first pass at a bulk loader for the PostgreSQL backend type. Note that only
 * variants and tags are currently implemented and only some of the fields in variants.  This is
 * really more of a proof of concept to see if there are speed gains when loading whole genomes
 * worth of variants.
 * 
 * In it's current form this bulk loader stores the tags in memory while writing two temp files 
 * out to disk, one for the variant_tags table and the other for the variant. When the user
 * calls close the tags are then written out too.  The user then needs to load these bulk load
 * files into the database.  By default the files are /tmp/variant.sql, /tmp/variant_tag.sql
 * and /tmp/tag.sql.
 * 
 * TODO: need to complete the implementation of this...
 * 
 */
public class PostgreSQLBulkWriterStore extends Store {

    private static final int MAX_RETRY = 20;

    FeatureTB ftb = new FeatureTB();

    VariantTB mtb = new VariantTB();

    ConsequenceTB ctb = new ConsequenceTB();

    CoverageTB covtb = new CoverageTB();

    TagTB ttb = new TagTB();

    StringIdTB midtb = new StringIdTB();

    Long currId = null;

    Long currFeatureId = null;

    Long currConsequenceId = null;

    Long currCoverageId = null;

    public static int OID = 0;

    public static int BYTEA = 1;

    public static int FIELDS = 2;

    private int persistenceMethod = PostgreSQLBulkWriterStore.OID;

    HashMap<String, HashMap<String, Integer>> tags = new HashMap<String, HashMap<String, Integer>>();

    BufferedWriter variantWriter = null;

    BufferedWriter tagWriter = null;

    BufferedWriter variantTagWriter = null;

    int uid = 0;

    public void setup(SeqWareSettings settings) throws FileNotFoundException, DatabaseException, Exception {
        super.setup(settings);
        variantWriter = new BufferedWriter(new FileWriter(new File("/tmp/variant.sql")));
        variantWriter.write("COPY variant (variant_id, type, contig, start, stop, referencebase, consensusbase, calledbase, consensuscallquality, readcount, keyvalues) FROM stdin;");
        variantWriter.newLine();
        tagWriter = new BufferedWriter(new FileWriter(new File("/tmp/tag.sql")));
        tagWriter.write("COPY tag (tag_id, key, value) FROM stdin;");
        tagWriter.newLine();
        variantTagWriter = new BufferedWriter(new FileWriter(new File("/tmp/variant_tag.sql")));
        variantTagWriter.write("COPY variant_tag (variant_tag_id, variant_id, tag_id) FROM stdin;");
        variantTagWriter.newLine();
    }

    public void close() throws DatabaseException {
        try {
            variantWriter.write("\\.");
            variantWriter.newLine();
            variantWriter.close();
            tagWriter.write("\\.");
            tagWriter.newLine();
            tagWriter.close();
            variantTagWriter.write("\\.");
            variantTagWriter.newLine();
            variantTagWriter.close();
        } catch (IOException se) {
            System.out.println("Couldn't close files: print out a stack trace and exit.");
            se.printStackTrace();
            System.exit(1);
        }
    }

    public void addTags(Model model, String table) {
    }

    public void readTags(Model model, String table) {
    }

    public PostgresModelIterator getFeaturesUnordered() {
        return (null);
    }

    public PostgresModelIterator getFeatures() {
        return (null);
    }

    public PostgresModelIterator getFeatures(String contig, int start, int stop) {
        return (null);
    }

    public Feature getFeature(String featureId) throws Exception {
        return (null);
    }

    public PostgresModelIterator getFeaturesByTag(String tag) {
        return (null);
    }

    public PostgresTagModelIterator getFeaturesTags() {
        return (null);
    }

    public SeqWareIterator getFeatureTagsBySearch(String tagSearchStr) {
        return (null);
    }

    public synchronized String putFeature(Feature feature, SeqWareIterator it, boolean transactional) {
        return (null);
    }

    public synchronized String putFeature(Feature feature) {
        return (null);
    }

    public SeqWareIterator getMismatchesUnordered() {
        return (null);
    }

    public SeqWareIterator getMismatches() {
        return (getMismatchesUnordered());
    }

    public SeqWareIterator getMismatches(String contig, int start, int stop) {
        return (null);
    }

    public SeqWareIterator getMismatches(String contig) {
        return (getMismatches(contig, 1, Integer.MAX_VALUE));
    }

    public Variant getMismatch(String mismatchId) throws Exception {
        return (null);
    }

    public SeqWareIterator getMismatchesByTag(String tag) {
        return (null);
    }

    /**
   * Get access to an iterator containing all the mismatch tags.
   * @return
   */
    public PostgresTagModelIterator getMismatchesTags() {
        return (null);
    }

    /**
   * Get access to an iterator containing all the mismatch tags.
   * @return
   */
    public PostgresTagModelIterator getMismatchTagsBySearch(String tagSearchStr) {
        return (null);
    }

    public synchronized String putMismatch(Variant variant) {
        int variantId = nextId();
        try {
            StringBuffer sb = new StringBuffer();
            for (String key : variant.getTags().keySet()) {
                if (key != null && !"".equals(key) && !key.matches("^\\s+$") && !key.matches("^\\d+$")) {
                    String value = variant.getTagValue(key);
                    int tagId = nextId();
                    if (value != null && !"".equals(value)) {
                        sb.append(key + "=" + value + ":");
                    } else {
                        sb.append(key + ":");
                    }
                    if (value == null || "".equals(value)) {
                        value = "\\N";
                    }
                    HashMap<String, Integer> values = tags.get(key);
                    if (values == null) {
                        values = new HashMap<String, Integer>();
                        values.put(value, tagId);
                        tags.put(key, values);
                        tagWriter.write(tagId + "\t" + key + "\t" + value);
                        tagWriter.newLine();
                    } else {
                        if (values.get(value) == null) {
                            values.put(value, tagId);
                            tagWriter.write(tagId + "\t" + key + "\t" + value);
                            tagWriter.newLine();
                        } else {
                            tagId = values.get(value);
                        }
                    }
                    int variantTagId = nextId();
                    variantTagWriter.write(variantTagId + "\t" + variantId + "\t" + tagId);
                    variantTagWriter.newLine();
                }
            }
            variant.setKeyvalues(sb.substring(0, sb.length() - 1));
            variantWriter.write(variantId + "\t" + variant.getType() + "\t" + variant.getContig() + "\t" + variant.getStartPosition() + "\t" + variant.getStopPosition() + "\t" + variant.getReferenceBase() + "\t" + variant.getConsensusBase() + "\t" + variant.getCalledBase() + "\t" + variant.getConsensusCallQuality() + "\t" + variant.getReadCount() + "\t" + variant.getKeyvalues());
            variantWriter.newLine();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
        return (new Integer(variantId).toString());
    }

    public synchronized String putMismatch(Variant variant, SeqWareIterator it, boolean transactional) {
        return (putMismatch(variant));
    }

    /**
   * Calling function will get back the coverage blocks that are contained within the range of interest
   * so make sure it pads +/- the bin size in the requested range otherwise may miss data!
   * @param contig
   * @param start
   * @param stop
   * @return
   */
    public LocatableSecondaryCursorIterator getCoverages(String contig, int start, int stop) {
        LocatableSecondaryCursorIterator cci = null;
        return (cci);
    }

    public LocatableSecondaryCursorIterator getCoverages(String contig) {
        return (getCoverages(contig, 1, Integer.MAX_VALUE));
    }

    public synchronized String putCoverage(Coverage coverage) {
        return (putCoverage(coverage, true));
    }

    public synchronized String putCoverage(Coverage coverage, boolean transactional) {
        return (null);
    }

    public synchronized String putConsequence(Consequence consequence) {
        return (putConsequence(consequence, true));
    }

    public synchronized String putConsequence(Consequence consequence, boolean transactional) {
        return (null);
    }

    public Consequence getConsequence(String consequenceId) throws Exception {
        return (null);
    }

    public SecondaryCursorIterator getConsequencesByTag(String tag) {
        SecondaryCursorIterator i = null;
        return (i);
    }

    public SeqWareIterator getConsequenceTagsBySearch(String tagSearchStr) {
        return (null);
    }

    public SecondaryCursorIterator getConsequencesByMismatch(String mismatchId) {
        SecondaryCursorIterator i = null;
        return (i);
    }

    protected int nextId() {
        uid++;
        return (uid);
    }
}
