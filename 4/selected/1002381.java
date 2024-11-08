package uk.ac.ebi.intact.dataexchange.imex.repository;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import uk.ac.ebi.intact.dataexchange.imex.repository.enrich.EntryEnricher;
import uk.ac.ebi.intact.dataexchange.imex.repository.enrich.impl.DefaultEntryEnricher;
import uk.ac.ebi.intact.dataexchange.imex.repository.ftp.ImexFTPFile;
import uk.ac.ebi.intact.dataexchange.imex.repository.model.Provider;
import uk.ac.ebi.intact.dataexchange.imex.repository.model.RepoEntityNotFoundException;
import uk.ac.ebi.intact.dataexchange.imex.repository.model.RepoEntry;
import uk.ac.ebi.intact.dataexchange.imex.repository.model.RepoEntrySet;
import uk.ac.ebi.intact.dataexchange.imex.repository.split.EntrySetSplitter;
import uk.ac.ebi.intact.dataexchange.imex.repository.split.impl.DefaultEntrySetSplitter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO comment this
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: Repository.java 10862 2008-01-17 10:38:43Z baranda $
 */
public class Repository {

    private static final Log log = LogFactory.getLog(Repository.class);

    private static final String CONFIG_DIR_NAME = ".config";

    private static final String ORIGINAL_DIR_NAME = "original";

    private static final String ENTRIES_DIR_NAME = "entries";

    private File repositoryDir;

    private boolean open;

    public Repository(File repositoryDir) {
        this.repositoryDir = repositoryDir;
        this.open = true;
    }

    public void storeEntrySet(ImexFTPFile entryXml, String providerName) throws IOException {
        storeEntrySet(entryXml.toFile(), providerName);
    }

    public void storeEntrySet(File entryXml, String providerName) {
        if (log.isDebugEnabled()) {
            log.debug("Adding entry: " + entryXml + " (Provider: " + providerName + ")");
        }
        ImexRepositoryContext context = ImexRepositoryContext.getInstance();
        Provider provider = context.getImexServiceProvider().getProviderService().findByName(providerName);
        if (provider == null) {
            throw new RepoEntityNotFoundException("No provider found with name: " + providerName);
        }
        String entryName = entryXml.getName();
        String name = fileDate(entryName);
        RepoEntrySet repoEntrySet = new RepoEntrySet(provider, name);
        RepositoryHelper repoHelper = new RepositoryHelper(this);
        File newFile = repoHelper.getEntrySetFile(repoEntrySet);
        if (log.isDebugEnabled()) {
            log.debug("Copying file to: " + newFile);
        }
        try {
            FileUtils.copyFile(entryXml, newFile);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
        beginTransaction();
        context.getImexServiceProvider().getRepoEntrySetService().saveRepoEntrySet(repoEntrySet);
        commitTransaction();
        beginTransaction();
        EntrySetSplitter splitter = new DefaultEntrySetSplitter();
        List<RepoEntry> splittedEntries = null;
        try {
            splittedEntries = splitter.splitRepoEntrySet(repoEntrySet);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
        commitTransaction();
        EntryEnricher enricher = new DefaultEntryEnricher();
        for (RepoEntry repoEntry : splittedEntries) {
            if (repoEntry.isValid()) {
                beginTransaction();
                try {
                    enricher.enrichEntry(repoEntry);
                } catch (IOException e) {
                    throw new RepositoryException(e);
                }
                commitTransaction();
            }
        }
    }

    public RepoEntry findRepoEntryByPmid(String name) {
        ImexRepositoryContext context = ImexRepositoryContext.getInstance();
        return context.getImexServiceProvider().getRepoEntryService().findByPmid(name);
    }

    /**
     * Gets a list of RepoEntries excluding the pmids in the passed list
     * @param pmidsToExclude
     * @return
     */
    public List<RepoEntry> findRepoEntriesByPmidExcluding(List<String> pmidsToExclude) {
        ImexRepositoryContext context = ImexRepositoryContext.getInstance();
        return context.getImexServiceProvider().getRepoEntryService().findImportableExcluding(pmidsToExclude);
    }

    /**
     * Gets a list of RepoEntries created or updated after the passed date
     * @param dateTime
     * @return
     */
    public List<RepoEntry> findRepoEntriesModifiedAfter(DateTime dateTime) {
        ImexRepositoryContext context = ImexRepositoryContext.getInstance();
        return context.getImexServiceProvider().getRepoEntryService().findImportableModifiedAfter(dateTime);
    }

    public void close() {
        this.open = false;
        ImexRepositoryContext.closeRepository();
    }

    public boolean isOpen() {
        return open;
    }

    public File getRepositoryDir() {
        return repositoryDir;
    }

    public File getConfigDir() {
        return new File(getRepositoryDir(), CONFIG_DIR_NAME);
    }

    public File getOriginalEntrySetDir() {
        return new File(getRepositoryDir(), ORIGINAL_DIR_NAME);
    }

    public File getOriginalEntrySetDir(String providerName) {
        return new File(getOriginalEntrySetDir(), providerName);
    }

    public File getEntriesDir() {
        return new File(getRepositoryDir(), ENTRIES_DIR_NAME);
    }

    public File getEntriesDir(String providerName) {
        return new File(getEntriesDir(), providerName);
    }

    protected static String fileDate(String entryName) {
        Pattern p = Pattern.compile(".*(\\d{4}-\\d{2}\\-\\d{2}).*");
        String fileDate = entryName;
        Matcher m = p.matcher(entryName);
        if (m.matches()) {
            fileDate = m.group(1);
        }
        return fileDate;
    }

    private void beginTransaction() {
        ImexRepositoryContext.getInstance().getImexPersistence().beginTransaction();
    }

    private void commitTransaction() {
        ImexRepositoryContext.getInstance().getImexPersistence().commitTransaction();
    }
}
