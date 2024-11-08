package uk.ac.ebi.intact.plugins;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import uk.ac.ebi.intact.bridges.ontologies.util.OntologyUtils;
import uk.ac.ebi.intact.bridges.ontologies.*;
import uk.ac.ebi.intact.bridges.ontologies.OntologyMapping;
import uk.ac.ebi.intact.psimitab.converters.util.DatabaseMitabExporter;
import uk.ac.ebi.intact.plugin.IntactAbstractMojo;
import uk.ac.ebi.intact.dataexchange.psimi.solr.ontology.OntologyIndexer;
import uk.ac.ebi.intact.dataexchange.psimi.solr.IntactSolrIndexer;
import uk.ac.ebi.intact.commons.util.ETACalculator;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Goal which which exports database interactions into MITAB file and Lucene indexes.
 *
 * @goal mitab-solr-indexer
 * @phase process-sources
 * @since 1.9.0
 */
public class MitabSolrIndexerMojo extends IntactAbstractMojo {

    /**
     * Project instance
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Where is the data core in the Solr server located.
     * eg. http://padney.ebi.ac.uk:8080/solr/core_stage
     *
     * @parameter
     * @required
     */
    private String solrServerUrl;

    /**
     * Where is the ontology core in the Solr server located.
     * eg. http://padney.ebi.ac.uk:8080/solr/core_ontology_stage
     *
     * @parameter
     * @required
     */
    private String ontologySolrServerUrl;

    /**
     * All ontologies used to build the ontology index.
     *
     * @parameter
     * @required
     */
    private List<OntologyMapping> ontologyMappings;

    /**
     * if true, the ontology data is deleted prior to start processing.
     *
     * @parameter
     * @required
     */
    private boolean recreateOntologyData;

    /**
     * has the data file got a header.
     *
     * @parameter
     * @required
     */
    private boolean hasMitabHeader;

    /**
     * Where is the MITAB data source be read from (as a URL).
     *
     * @parameter
     * @required
     */
    private String mitabFileUrl;

    /**
     * Should we overwrite existing data.
     *
     * @parameter
     * @required
     */
    private boolean removeExistingData;

    /**
     * Log file for reporting potential problems.
     *
     * @parameter
     * @required
     */
    private String logFilePath;

    /**
     * First line to be processed in the data file.
     *
     * @parameter
     * @required
     */
    private int firstLine;

    /**
     * count of line to be process by a single Thread.
     *
     * @parameter
     */
    private Integer maxLines;

    public String getSolrServerUrl() {
        return solrServerUrl;
    }

    public void setSolrServerUrl(String solrServerUrl) {
        this.solrServerUrl = solrServerUrl;
    }

    public String getOntologySolrServerUrl() {
        return ontologySolrServerUrl;
    }

    public void setOntologySolrServerUrl(String ontologySolrServerUrl) {
        this.ontologySolrServerUrl = ontologySolrServerUrl;
    }

    public List<OntologyMapping> getOntologyMappings() {
        return ontologyMappings;
    }

    public void setOntologyMappings(List<OntologyMapping> ontologies) {
        this.ontologyMappings = ontologies;
    }

    public boolean isRecreateOntologyData() {
        return recreateOntologyData;
    }

    public void setRecreateOntologyData(boolean recreateOntologyData) {
        this.recreateOntologyData = recreateOntologyData;
    }

    public boolean isHasMitabHeader() {
        return hasMitabHeader;
    }

    public void setHasMitabHeader(boolean hasMitabHeader) {
        this.hasMitabHeader = hasMitabHeader;
    }

    public String getMitabFileUrl() {
        return mitabFileUrl;
    }

    public void setMitabFileUrl(String mitabFileUrl) {
        this.mitabFileUrl = mitabFileUrl;
    }

    public boolean isRemoveExistingData() {
        return removeExistingData;
    }

    public void setRemoveExistingData(boolean removeExistingData) {
        this.removeExistingData = removeExistingData;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    public int getFirstLine() {
        return firstLine;
    }

    public void setFirstLine(int firstLine) {
        this.firstLine = firstLine;
    }

    public Integer getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(Integer maxLines) {
        this.maxLines = maxLines;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        System.out.println("=============================== MOJO PARAMETERS ================================");
        System.out.println("parameter 'solrServerUrl' = " + solrServerUrl);
        System.out.println("parameter 'ontologyMappings' = " + ontologyMappings);
        System.out.println("parameter 'recreateOntologyData' = " + recreateOntologyData);
        System.out.println("parameter 'mitabFileUrl' = " + mitabFileUrl);
        System.out.println("parameter 'hasMitabHeader' = " + hasMitabHeader);
        System.out.println("parameter 'removeExistingData' = " + removeExistingData);
        System.out.println("parameter 'firstLine' = " + firstLine);
        System.out.println("parameter 'maxLines' = " + maxLines);
        System.out.println("parameter 'logFilePath' = " + logFilePath);
        System.out.println("================================================================================");
        if (logFilePath != null) {
            File logFile = new File(logFilePath);
            try {
                logWriter = new BufferedWriter(new FileWriter(logFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        URL mitabFile = null;
        try {
            mitabFile = new URL(mitabFileUrl);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Could not parse the MITAB url: " + mitabFileUrl, e);
        }
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        writeLog("Available processors: " + availableProcessors);
        try {
            if (removeExistingData) {
                writeLog("Removing existing data...");
                SolrServer server = new CommonsHttpSolrServer(solrServerUrl);
                server.deleteByQuery("*:*");
                server.commit();
                server.optimize();
            }
        } catch (Exception e) {
            throw new MojoExecutionException("An error occur while removing existing data", e);
        }
        try {
            if (recreateOntologyData) {
                writeLog("Re-creating ontology data...");
                SolrServer ontologyServer = new CommonsHttpSolrServer(ontologySolrServerUrl);
                ontologyServer.deleteByQuery("*:*");
                ontologyServer.commit();
                ontologyServer.optimize();
                OntologyIndexer indexer = new OntologyIndexer(ontologyServer);
                indexer.indexObo(ontologyMappings.toArray(new uk.ac.ebi.intact.plugins.OntologyMapping[ontologyMappings.size()]));
            }
        } catch (Exception e) {
            throw new MojoExecutionException("An error occur while rebuilding ontologies", e);
        }
        int interactionsCount;
        try {
            interactionsCount = (maxLines != null) ? maxLines : countLines(mitabFile, hasMitabHeader);
        } catch (IOException e) {
            throw new MojoExecutionException("An error occur while counting the MITAB lines from " + mitabFile.getFile(), e);
        }
        writeLog("Interactions to process: " + interactionsCount);
        final int defaultBatchSize = interactionsCount / availableProcessors;
        final int originalFirstLine = firstLine;
        Collection<IndexerWorker> workers = new ArrayList<IndexerWorker>(availableProcessors);
        for (int i = 0; i < availableProcessors; i++) {
            final int remainingLines = originalFirstLine + interactionsCount - firstLine;
            final boolean isLastProcessor = (i == availableProcessors - 1);
            final int batchSize = isLastProcessor ? remainingLines : defaultBatchSize;
            final String threadName = "MITAB-processor-" + i;
            writeLog("Starting thread '" + threadName + "' to process lines " + firstLine + ".." + (firstLine + batchSize) + " (batchSize=" + batchSize + ") ...");
            final IndexerWorker worker = new IndexerWorker(mitabFile, hasMitabHeader, solrServerUrl, ontologySolrServerUrl, firstLine, batchSize);
            workers.add(worker);
            Thread thread = new Thread(worker, threadName);
            thread.start();
            firstLine = firstLine + batchSize;
        }
        writeLog("All processors have received a chunk of data to process.");
        Thread thread = new Thread(new DocCounter(solrServerUrl, interactionsCount), "DocCounter");
        thread.start();
        do {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (Iterator<IndexerWorker> iterator = workers.iterator(); iterator.hasNext(); ) {
                IndexerWorker worker = iterator.next();
                if (worker.isDone()) {
                    iterator.remove();
                }
            }
        } while (!workers.isEmpty());
    }

    private static Writer logWriter = null;

    private static void writeLog(String msg) {
        try {
            if (logWriter != null) logWriter.append(msg).append("\n").flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int countLines(URL mitabFile, boolean hasHeader) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(mitabFile.openStream()));
        int count = 0;
        while (reader.readLine() != null) {
            count++;
        }
        return count - (hasHeader ? 1 : 0);
    }

    private static class IndexerWorker implements Runnable {

        private URL mitabFile;

        private boolean hasHeader;

        private String solrServerUrl;

        private String ontologyServerUrl;

        private int firstLine;

        private int batchSize;

        private boolean isDone;

        public IndexerWorker(URL mitabFile, boolean hasHeader, String solrServerUrl, String ontologyServerUrl, int firstLine, int batchSize) {
            isDone = false;
            this.mitabFile = mitabFile;
            this.hasHeader = hasHeader;
            this.solrServerUrl = solrServerUrl;
            this.ontologyServerUrl = ontologyServerUrl;
            this.firstLine = firstLine;
            this.batchSize = batchSize;
        }

        public boolean isDone() {
            return isDone;
        }

        public void run() {
            final String name = Thread.currentThread().getName();
            writeLog(name + " - Thread started - firstLine:" + firstLine + " - batchSize:" + batchSize);
            try {
                SolrServer solrServer = new CommonsHttpSolrServer(solrServerUrl);
                SolrServer ontologyServer = new CommonsHttpSolrServer(ontologyServerUrl);
                final long startTime = System.currentTimeMillis();
                IntactSolrIndexer indexer = new IntactSolrIndexer(solrServer, ontologyServer);
                indexer.indexMitab(mitabFile.openStream(), hasHeader, firstLine, batchSize);
                final long elapsedTime = System.currentTimeMillis() - startTime;
                final long linePerSeconds = batchSize / (elapsedTime / 1000);
                writeLog(name + " - Completed chunk - Elapsed time :" + elapsedTime + "ms - " + linePerSeconds + " lines/sec.");
                isDone = true;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(name + " - Problem processing mitab file - firstLine=" + firstLine + " - batchSize=" + batchSize, e);
            }
        }
    }

    private static class DocCounter implements Runnable {

        private String solrServerUrl;

        private int totalCount;

        public DocCounter(String solrServerUrl, int totalCount) {
            this.solrServerUrl = solrServerUrl;
            this.totalCount = totalCount;
        }

        public void run() {
            final long startTime = System.currentTimeMillis();
            final String name = Thread.currentThread().getName();
            final ETACalculator eta = new ETACalculator(totalCount);
            try {
                long processed = 0;
                do {
                    SolrServer server = new CommonsHttpSolrServer(solrServerUrl);
                    SolrQuery query = new SolrQuery("*:*").setRows(0);
                    QueryResponse queryResponse = server.query(query);
                    processed = queryResponse.getResults().getNumFound();
                    writeLog(name + " - [" + new Date() + "] - Processed: " + processed + " - ETA: " + eta.printETA(processed));
                    Thread.sleep(60 * 1000);
                } while (processed < totalCount);
                final long elapsedTime = System.currentTimeMillis() - startTime;
                writeLog(name + " - Terminating - Processed: " + processed + " - Total elapsed time: " + (elapsedTime) + "ms");
            } catch (SolrServerException e) {
                Throwable t = e.getCause();
                boolean found = false;
                while (t != null) {
                    if (t instanceof java.net.ConnectException) {
                        writeLog(name + " -  could not connect to the Solr server(" + solrServerUrl + "), it might have been shut down.");
                        found = true;
                    }
                    t = e.getCause();
                }
                if (!found) {
                    throw new RuntimeException(name + ": Problem counting", e);
                }
            } catch (Exception e) {
                throw new RuntimeException(name + ": Problem counting", e);
            }
        }
    }

    public MavenProject getProject() {
        return project;
    }
}
