package org.ala.preprocess;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.ala.dao.InfoSourceDAO;
import org.ala.documentmapper.MappingUtils;
import org.ala.model.InfoSource;
import org.ala.repository.DocumentOutputStream;
import org.ala.repository.Predicates;
import org.ala.repository.Repository;
import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.turtle.TurtleWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import au.com.bytecode.opencsv.CSVReader;

/**
 * This class converts a dump file of scientific family names
 * taken from Catalogue of Life 2009 to RDF that can be pulled into the
 * BIE profiler.
 *
 * This class creates a dublin core file and rdf turtle file
 * of this information so that it can be imported into the BIE profiles.
 * 
 * This should be ran with the JVM options "-Xmx1g -Xms1g"
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class ColFamilyNamesProcessor {

    protected static Logger logger = Logger.getLogger(ColFamilyNamesProcessor.class);

    private static final int NO_OF_COLUMNS = 3;

    @Inject
    protected Repository repository;

    @Inject
    protected InfoSourceDAO infoSourceDAO;

    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "classpath*:spring-profiler.xml", "classpath:spring.xml" });
        ColFamilyNamesProcessor p = new ColFamilyNamesProcessor();
        p.setInfoSourceDAO((InfoSourceDAO) context.getBean("infoSourceDAO"));
        p.setRepository((Repository) context.getBean("repository"));
        p.process();
        System.exit(1);
    }

    /**
	 * Pulls the raw files into the repository and convert to RDF.
	 * @throws Exception
	 */
    private void process() throws Exception {
        processFile("/data/bie-staging/col/familyCommonNames.txt", "http://www.catalogueoflife.org/familyCommonNames", "Catalogue of Life 2009", "http://www.catalogueoflife.org/");
        processFile("/data/bie-staging/col/commonNames.txt", "http://www.catalogueoflife.org/commonNames", "Catalogue of Life 2009", "http://www.catalogueoflife.org/");
    }

    /**
	 * Process a single file.
	 * 
	 * @param filePath
	 * @param uri
	 * @param publisher
	 * @param source
	 * @throws FileNotFoundException
	 * @throws Exception
	 * @throws IOException
	 * @throws RDFHandlerException
	 */
    private void processFile(String filePath, String uri, String publisher, String source) throws FileNotFoundException, Exception, IOException, RDFHandlerException {
        int documentId = copyRawFileToRepo(filePath, uri, "http://www.catalogueoflife.org/", "text/plain");
        Map<String, String> dc = new HashMap<String, String>();
        dc.put(Predicates.DC_IDENTIFIER.toString(), uri);
        dc.put(Predicates.DC_PUBLISHER.toString(), publisher);
        dc.put(Predicates.DC_SOURCE.toString(), uri);
        dc.put(Predicates.DC_FORMAT.toString(), "text/plain");
        repository.storeDublinCore(documentId, dc);
        Reader r = new FileReader(filePath);
        DocumentOutputStream rdfDos = repository.getRDFOutputStream(documentId);
        CSVReader csvr = new CSVReader(r, '\t');
        String[] fields = null;
        final RDFWriter rdfWriter = new TurtleWriter(new OutputStreamWriter(rdfDos.getOutputStream()));
        rdfWriter.startRDF();
        Pattern p = Pattern.compile(",");
        String subject = MappingUtils.getSubject();
        while ((fields = csvr.readNext()) != null) {
            if (fields.length == NO_OF_COLUMNS) {
                String[] commonNames = p.split(fields[0]);
                for (String commonName : commonNames) {
                    subject = MappingUtils.getNextSubject(subject);
                    BNode bnode = new BNodeImpl(subject);
                    rdfWriter.handleStatement(new StatementImpl(bnode, new URIImpl(Predicates.COMMON_NAME.toString()), new LiteralImpl(commonName.trim())));
                    rdfWriter.handleStatement(new StatementImpl(bnode, new URIImpl(Predicates.SCIENTIFIC_NAME.toString()), new LiteralImpl(fields[1])));
                    rdfWriter.handleStatement(new StatementImpl(bnode, new URIImpl(Predicates.KINGDOM.toString()), new LiteralImpl(fields[2])));
                }
            } else {
                logger.warn("Error reading from file. Was expecting " + NO_OF_COLUMNS + ", got " + fields.length);
            }
            subject = MappingUtils.getNextSubject(subject);
        }
        rdfWriter.endRDF();
        rdfDos.getOutputStream().flush();
        rdfDos.getOutputStream().close();
    }

    /**
	 * Copy the raw file into the repository.
	 * 
	 * @param filePath
	 * @param uri
	 * @param infosourceUri
	 * @param mimeType
	 * @return
	 * @throws FileNotFoundException
	 * @throws Exception
	 * @throws IOException
	 */
    private int copyRawFileToRepo(String filePath, String uri, String infosourceUri, String mimeType) throws FileNotFoundException, Exception, IOException {
        InfoSource infoSource = infoSourceDAO.getByUri(infosourceUri);
        Reader ir = new FileReader(filePath);
        DocumentOutputStream dos = repository.getDocumentOutputStream(infoSource.getId(), uri, mimeType);
        OutputStreamWriter w = new OutputStreamWriter(dos.getOutputStream());
        char[] buff = new char[1000];
        int read = 0;
        while ((read = ir.read(buff)) > 0) {
            w.write(buff, 0, read);
        }
        w.flush();
        w.close();
        return dos.getId();
    }

    /**
	 * @param repository the repository to set
	 */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
	 * @param infoSourceDAO the infoSourceDAO to set
	 */
    public void setInfoSourceDAO(InfoSourceDAO infoSourceDAO) {
        this.infoSourceDAO = infoSourceDAO;
    }
}
