package uk.ac.ebi.rhea.biopax.level2;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import org.apache.log4j.Logger;
import uk.ac.ebi.biobabel.util.db.OracleDatabaseInstance;
import uk.ac.ebi.rhea.domain.Direction;
import uk.ac.ebi.rhea.domain.Reaction;
import uk.ac.ebi.rhea.mapper.IRheaReader;
import uk.ac.ebi.rhea.mapper.db.RheaCompoundDbReader;
import uk.ac.ebi.rhea.mapper.db.RheaDbReader;

/**
 * Application to dump Rhea reactions in BioPAX Level 2 OWL format.
 * @author rafalcan
 * @deprecated use RheaExporter in apps module
 *
 */
public class BiopaxWriter {

    private static final Logger LOGGER = Logger.getLogger(BiopaxWriter.class);

    /**
	 * Dumps publicly available reactions in BioPAX format.
     * Please note that only <i>master</i> reactions (undefined direction)
     * are exported due to BioPAX not allowing explicit directionality for
     * reaction elements other than inside a catalysis. Complex reactions
     * (decompositions) are currently also included for completeness,
     * despite their being directional.
	 * @param args
	 * <ol>
	 * 	<li>name of the configuration file for Rhea database connection</li>
	 * 	<li>name of the file to dump the BioPAX OWL to</li>
	 * </ol>
	 */
    public static void main(String[] args) throws Exception {
        Connection rheaCon = null;
        OutputStream os = null;
        try {
            rheaCon = OracleDatabaseInstance.getInstance(args[0]).getConnection();
            os = new FileOutputStream(args[1]);
            IRheaReader rheaReader = new RheaDbReader(new RheaCompoundDbReader(rheaCon));
            Collection<Long> allPublicReactionIds = rheaReader.findAllPublic();
            Collection<Reaction> allPublicReactions = new HashSet<Reaction>();
            for (Long id : allPublicReactionIds) {
                Reaction r = rheaReader.findByReactionId(id);
                if (!r.isComplex() && !r.getDirection().equals(Direction.UN)) {
                    LOGGER.warn("Skipping reaction of defined direction RHEA:" + r.getId());
                    continue;
                }
                allPublicReactions.add(r);
                LOGGER.info("Added RHEA:" + r.getId().toString());
            }
            LOGGER.info("Writing OWL model...");
            Biopax.write(allPublicReactions, os, null);
            LOGGER.info("OWL model written!");
        } catch (IOException e) {
            LOGGER.error("Unable to read/write", e);
        } finally {
            if (rheaCon != null) rheaCon.close();
            if (os != null) os.close();
        }
    }
}
