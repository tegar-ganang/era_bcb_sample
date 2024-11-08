package org.gbif.checklistbank.service.impl;

import org.gbif.checklistbank.imports.AcceptImport;
import org.gbif.checklistbank.imports.AcceptImportFactory;
import org.gbif.checklistbank.imports.ChecklistImport;
import org.gbif.checklistbank.imports.ChecklistImportError;
import org.gbif.checklistbank.imports.ChecklistImportFactory;
import org.gbif.checklistbank.jdbc.DataAccessException;
import org.gbif.checklistbank.jdbc.JdbcTemplate;
import org.gbif.checklistbank.model.Checklist;
import org.gbif.checklistbank.service.ChecklistImportService;
import org.gbif.checklistbank.service.ChecklistService;
import org.gbif.checklistbank.service.NameStringService;
import org.gbif.checklistbank.service.ParsedNameService;
import org.gbif.checklistbank.service.TermService;
import org.gbif.checklistbank.utils.SqlUtils;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.ecat.cfg.DataDirConfig;
import org.gbif.ecat.cfg.RsGbifOrg;
import org.gbif.ecat.voc.CitesAppendix;
import org.gbif.ecat.voc.EstablishmentMeans;
import org.gbif.ecat.voc.IdentifierFormat;
import org.gbif.ecat.voc.IdentifierType;
import org.gbif.ecat.voc.KnownTerm;
import org.gbif.ecat.voc.LifeStage;
import org.gbif.ecat.voc.NameType;
import org.gbif.ecat.voc.NomenclaturalCode;
import org.gbif.ecat.voc.NomenclaturalStatus;
import org.gbif.ecat.voc.OccurrenceStatus;
import org.gbif.ecat.voc.Rank;
import org.gbif.ecat.voc.Sex;
import org.gbif.ecat.voc.TaxonomicStatus;
import org.gbif.ecat.voc.TermType;
import org.gbif.ecat.voc.ThreatStatus;
import org.gbif.ecat.voc.TypeStatus;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.file.CompressionUtil;
import org.gbif.utils.file.CompressionUtil.UnsupportedCompressionType;
import org.gbif.utils.file.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

@Singleton
public class ChecklistImportServicePgSql extends PgSqlBaseService implements ChecklistImportService {

    private static final String MDC_KEY = "RESOURCE_ID";

    private final String INDEX_TABLESPACE = "indices";

    @Inject
    private ChecklistService checklistService;

    @Inject
    private TermService termService;

    @Inject
    private NameStringService nameService;

    @Inject
    private ParsedNameService parsedNameService;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private DataDirConfig cfg;

    @Inject
    private DataSource ds;

    @Inject
    private ChecklistImportFactory impFactory;

    @Inject
    private AcceptImportFactory accFactory;

    @Inject
    private HttpUtil http;

    private static void insertAltTerms(InputStream stream, int type, Connection con) throws IOException, SQLException {
        Map<String, String> terms = FileUtils.streamToMap(stream, 0, 1, true);
        String sql = "insert into term (id,term, type, ignore, source, preferred_term_fk, modified) values (nextval('term_id_seq'),?,?,false,'ECAT',?,CURRENT_TIMESTAMP)";
        PreparedStatement pstmt = con.prepareStatement(sql);
        for (String term : terms.keySet()) {
            if (StringUtils.trimToNull(term) == null) {
                continue;
            }
            Long prefId = Long.parseLong(terms.get(term).trim());
            pstmt.setObject(1, term);
            pstmt.setInt(2, type);
            pstmt.setObject(3, prefId);
            pstmt.execute();
        }
        terms = null;
    }

    private static File url2file(URL url) {
        File f = null;
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            try {
                f = new File(url.toURI());
            } catch (URISyntaxException e) {
                f = new File(url.getPath());
            }
        }
        return f;
    }

    @Override
    public void importChecklist(Checklist checklist, boolean dontUdpateSources, boolean ignoreValidator) throws IllegalArgumentException, ChecklistImportError {
        if (!ignoreValidator && !checklistService.isValid(checklist)) {
            log.warn("Checklist " + checklist.getId() + " is not valid, skip import");
            return;
        }
        try {
            MDC.put(MDC_KEY, "" + checklist.getId());
            if (!dontUdpateSources) {
                updateSources(checklist);
            }
            ChecklistImport imp = impFactory.create(checklist);
            log.debug("Importing ...");
            imp.importData();
            imp.processData();
            log.debug("Accepting ...");
            AcceptImport acc = accFactory.create(checklist);
            acc.acceptImport();
            log.debug("Updating checklist counts ...");
            checklistService.updateCounts(checklist);
            checklistService.update(checklist);
            log.debug("Done");
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void importNewTerms(Checklist checklist, boolean ignoreValidator) throws IllegalArgumentException {
        if (!ignoreValidator && !checklistService.isValid(checklist)) {
            log.warn("Checklist " + checklist.getId() + " is not valid, skip term import");
            return;
        }
        try {
            MDC.put(MDC_KEY, "" + checklist.getId());
            updateSources(checklist);
            ChecklistImport checklistImp = impFactory.create(checklist);
            checklistImp.importBasics();
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void initDatabase() {
        Connection con = null;
        Statement stmt = null;
        PreparedStatement pstmt = null;
        try {
            con = ds.getConnection();
            stmt = con.createStatement();
            String sql;
            try {
                sql = "DROP SCHEMA public CASCADE";
                stmt.execute(sql);
            } catch (Exception e) {
                System.out.println("Public schema not existing: " + e.getMessage());
            }
            sql = "CREATE SCHEMA public";
            stmt.execute(sql);
            System.out.println("Public schema recreated");
            InputStream ddl = cfg.classpathStream("org/gbif/checklistbank/checklist_bank_model.sql");
            SqlUtils.executeSqlScript(con, ddl);
            System.out.println("DDL executed");
            ddl = cfg.classpathStream("org/gbif/checklistbank/checklist_bank_model_additions.sql");
            SqlUtils.executeSqlScript(con, ddl);
            System.out.println("DDL addons executed");
            ddl = cfg.classpathStream("org/gbif/checklistbank/col_datasets.sql");
            SqlUtils.executeSqlScript(con, ddl);
            System.out.println("CoL datasets inserted");
            List<String> indices = jdbcTemplate.queryForStringList(con, "select indexname from pg_indexes where schemaname='public'");
            boolean tablespaceExists = false;
            for (String i : indices) {
                if (tablespaceExists) {
                    try {
                        jdbcTemplate.executeUpdate(con, "alter index " + i + " SET TABLESPACE " + INDEX_TABLESPACE);
                    } catch (DataAccessException e) {
                        log.debug("tablespace 'index' unaltered");
                        tablespaceExists = false;
                    }
                }
                jdbcTemplate.executeUpdate(con, "alter index " + i + " set (fillfactor=90)");
            }
            List<String> tables = jdbcTemplate.queryForStringList(con, "select tablename from pg_tables  where schemaname='public'");
            for (String t : tables) {
                jdbcTemplate.executeUpdate(con, "alter table " + t + " SET WITHOUT OIDS");
                jdbcTemplate.executeUpdate(con, "alter table " + t + " SET (fillfactor=90)");
            }
            System.out.println("Updated table and index storage parameters");
            for (TermType t : TermType.values()) {
                termService.insertTermType(t.ordinal(), t.toString());
            }
            System.out.println("Term types inserted");
            for (KnownTerm t : IdentifierFormat.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : IdentifierType.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : LifeStage.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : EstablishmentMeans.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : OccurrenceStatus.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : Sex.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : ThreatStatus.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : Rank.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : NomenclaturalStatus.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : TaxonomicStatus.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : NomenclaturalCode.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : CitesAppendix.values()) {
                termService.insertKnownTerm(t);
            }
            for (KnownTerm t : TypeStatus.values()) {
                termService.insertKnownTerm(t);
            }
            System.out.println("Known terms inserted from enumerations");
            termService.loadVocabularyTerms("http://rs.gbif.org/vocabulary/gbif/description_type.xml", TermType.DESCRIPTION_TYPE);
            termService.loadVocabularyTerms("http://rs.gbif.org/vocabulary/gbif/type_status.xml", TermType.TYPE_STATUS);
            termService.loadVocabularyTerms("http://rs.gbif.org/vocabulary/gbif/nomenclatural_status.xml", TermType.NOMENCLATURAL_STATUS);
            termService.loadVocabularyTerms("http://rs.gbif.org/vocabulary/gbif/establishment_means.xml ", TermType.ESTABLISHMENT_MEANS);
            termService.loadVocabularyTerms("http://rs.gbif.org/vocabulary/gbif/life_stage.xml ", TermType.LIFESTAGE);
            termService.loadVocabularyTerms("http://rs.gbif.org/vocabulary/gbif/life_form.xml ", TermType.GROWTHFORM);
            insertAltTerms(cfg.classpathStream("org/gbif/checklistbank/rank_terms.txt"), TermType.RANK.ordinal(), con);
            insertAltTerms(cfg.classpathStream("org/gbif/checklistbank/tax_status_terms.txt"), TermType.TAXONOMIC_STATUS.ordinal(), con);
            insertAltTerms(cfg.classpathStream("org/gbif/checklistbank/nom_code_terms.txt"), TermType.NOMENCLATURAL_CODE.ordinal(), con);
            insertAltTerms(cfg.classpathStream("org/gbif/checklistbank/cites_terms.txt"), TermType.CITES_APPENDICES.ordinal(), con);
            System.out.println("Alternative, known terms inserted");
            Set<String> blacklistedNames = FileUtils.streamToSet(cfg.classpathStream("org/gbif/checklistbank/blacklisted_names.txt"));
            nameService.blacklistNames(blacklistedNames);
            try {
                blacklistedNames = FileUtils.streamToSet(RsGbifOrg.authorityUrl(RsGbifOrg.FILENAME_BLACKLIST).openStream());
                nameService.blacklistNames(blacklistedNames);
            } catch (Exception e) {
                System.out.println("Failed to read blacklisted names from rs.gbif.org: " + e.getMessage());
            }
            System.out.println("Blacklisted known bad names");
            Map<String, String> names = FileUtils.streamToMap(cfg.classpathStream("org/gbif/checklistbank/names.txt"), 0, 1, true);
            sql = "insert into name_string (id,scientific_name,canonical_name_fk,type) values (?,?,?,?)";
            pstmt = con.prepareStatement(sql);
            for (String idString : names.keySet()) {
                if (StringUtils.trimToNull(idString) == null) {
                    continue;
                }
                Long id = Long.parseLong(idString.trim());
                pstmt.setObject(1, id);
                pstmt.setObject(2, names.get(idString).trim());
                pstmt.setObject(3, id);
                pstmt.setInt(4, NameType.wellformed.ordinal());
                pstmt.execute();
            }
            parsedNameService.updateAllNames();
            System.out.println("Known names inserted");
            names = null;
            System.out.println("New Checklist Bank initialised");
        } catch (Exception e) {
            System.out.println("Checklist Bank Init failed!\n" + e.getMessage() + "\n\n");
            e.printStackTrace();
        } finally {
            JdbcTemplate.close(pstmt);
            JdbcTemplate.close(stmt);
            JdbcTemplate.close(con);
        }
    }

    /**
   * Updates the dwca source files in the repository by doing a conditional download (or copying from local filesystem
   * if file uri).
   * Handles compressed archives or single data files and updates the checklist.lastDownload in case it was replaced.
   */
    private void updateSources(Checklist chkl) throws ChecklistImportError {
        boolean updated = false;
        URL url = chkl.getDownloadUriAsUrl();
        if (url == null) {
            log.warn("Checklist " + chkl.getId() + " missing download url");
            return;
        } else {
            File localDwca = url2file(url);
            if (localDwca == null) {
                localDwca = cfg.tmpFile(url);
                try {
                    updated = http.downloadIfChanged(url, chkl.getLastDownload(), localDwca);
                } catch (IOException e1) {
                    log.error("URL cant be reached: " + url, e1);
                    throw new ChecklistImportError("Cannot download dwc archive file from " + url);
                }
            } else {
                updated = true;
            }
            if (!updated || !localDwca.exists()) {
                log.info("Source files for checklist " + chkl.getId() + " already up to date");
            } else {
                File sourceFolder = localDwca;
                try {
                    File sourceFolderTmp = FileUtils.createTempDir(String.format("%04d", chkl.getId()), localDwca.getName());
                    log.debug("Decompressing archive " + localDwca.getAbsolutePath());
                    CompressionUtil.decompressFile(sourceFolderTmp, localDwca);
                    sourceFolder = sourceFolderTmp;
                } catch (UnsupportedCompressionType e) {
                    log.warn("Failed to decompress file " + localDwca.getAbsolutePath() + ". Maybe this is an uncompressed text file, lets try", e);
                } catch (IOException e) {
                    throw new ChecklistImportError("Cannot decompress dwc archive file " + localDwca.getAbsolutePath(), e);
                }
                try {
                    Archive archive = ArchiveFactory.openArchive(sourceFolder);
                } catch (Exception e) {
                    throw new ChecklistImportError("Cannot open dwc archive. Keep existing source files", e);
                }
                try {
                    File dwcaRepoDir = cfg.importDir(chkl.getId());
                    if (dwcaRepoDir.exists()) {
                        log.debug("clear existing source folder " + dwcaRepoDir.getAbsolutePath());
                        org.apache.commons.io.FileUtils.deleteDirectory(dwcaRepoDir);
                    }
                    log.debug("copy checklist " + sourceFolder.getName() + " to repo: " + dwcaRepoDir.getAbsolutePath());
                    if (sourceFolder.isFile()) {
                        org.apache.commons.io.FileUtils.copyFileToDirectory(sourceFolder, dwcaRepoDir);
                    } else {
                        org.apache.commons.io.FileUtils.copyDirectory(sourceFolder, dwcaRepoDir);
                    }
                } catch (IOException e) {
                    throw new ChecklistImportError("Failed to copy source files", e);
                }
                chkl.setLastDownload(new Date());
                checklistService.update(chkl);
            }
        }
    }
}
