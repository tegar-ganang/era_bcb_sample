package org.gbif.checklistbank.service.impl;

import org.gbif.checklistbank.imports.ChecklistImportError;
import org.gbif.checklistbank.imports.ImportBasePgSql;
import org.gbif.checklistbank.jdbc.JdbcTemplate;
import org.gbif.checklistbank.model.Checklist;
import org.gbif.checklistbank.model.Dataset;
import org.gbif.checklistbank.model.rowmapper.rs.ChecklistRowMapper;
import org.gbif.checklistbank.model.voc.ChecklistType;
import org.gbif.checklistbank.model.voc.Rating;
import org.gbif.checklistbank.service.ChecklistImportService;
import org.gbif.checklistbank.service.ChecklistService;
import org.gbif.checklistbank.service.DatasetService;
import org.gbif.checklistbank.service.RegistryService;
import org.gbif.checklistbank.utils.PgSqlUtils;
import org.gbif.checklistbank.utils.SqlStatement;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.ecat.voc.Rank;
import org.gbif.metadata.BasicMetadata;
import org.gbif.metadata.MetadataException;
import org.gbif.metadata.MetadataFactory;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.collection.CompactHashSet;
import org.gbif.utils.file.CompressionUtil;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang3.StringUtils;

@Singleton
public class ChecklistServicePgSql extends CRUDBaseServicePgSql<Checklist> implements ChecklistService {

    private final String INSERT_SQL = "insert into " + TABLE + " (resource_key,key,source_id,title,description,citation,keywords,num_usages,num_accepted_usages,num_canonicals, num_description,num_distribution,num_images,num_references,num_species_data,num_specimen,num_vernacular, num_k,num_p,num_c,num_o,num_f,num_sf,num_g,num_sg,num_s,num_is, popularity,taxonomy_roots,taxonomy_max_depth,taxonomy_max_width,nub_priority,type,last_download,logo_url,home_url,record_source_url,download_uri,ignore,published,modified) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,current_timestamp)";

    private final String UPDATE_SQL = "update " + TABLE + " set (resource_key,key,source_id,title,description,citation,keywords,num_usages,num_accepted_usages,num_canonicals, num_description,num_distribution,num_images,num_references,num_species_data,num_specimen,num_vernacular, num_k,num_p,num_c,num_o,num_f,num_sf,num_g,num_sg,num_s,num_is, popularity,taxonomy_roots,taxonomy_max_depth,taxonomy_max_width,nub_priority,type,last_download,logo_url,home_url,record_source_url,download_uri,ignore,published,modified) = (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,current_timestamp) where id=?";

    private MetadataFactory metaFactory;

    private ChecklistImportService impService;

    private RegistryService registryService;

    private DatasetService datasetService;

    private HttpUtil http;

    private static final String emlFileName = "eml.xml";

    @Inject
    public ChecklistServicePgSql(RegistryService registryService, ChecklistImportService impService, MetadataFactory metaFactory, DatasetService datasetService, HttpUtil http) {
        super("checklist", new ChecklistRowMapper(), true);
        this.metaFactory = metaFactory;
        this.impService = impService;
        this.registryService = registryService;
        this.datasetService = datasetService;
        this.http = http;
    }

    @Override
    public Set<Checklist> addAllChecklists(File sources) {
        Set<Checklist> checklists = new CompactHashSet<Checklist>();
        if (!sources.exists() || !sources.isDirectory()) {
            throw new IllegalArgumentException("Source folder given is no valid directory: " + sources);
        }
        FileFilter ff = HiddenFileFilter.VISIBLE;
        for (File resource : sources.listFiles(ff)) {
            log.info("\n***\nTRY ADDING " + resource.getAbsolutePath());
            Checklist c = addChecklist(resource, resource.getName());
            if (c != null) {
                checklists.add(c);
            }
        }
        return checklists;
    }

    @Override
    public Checklist addChecklist(File dwcaLocation, String title) {
        if (dwcaLocation == null) {
            return null;
        }
        Checklist chkl = addChecklist(dwcaLocation, null, title);
        if (chkl != null) {
            update(chkl);
        }
        return chkl;
    }

    /**
   * Creates a new checklist object, populates its metadata but doesnt persist it!
   *
   * @param dwca local file path
   *
   * @return a new checklist object with rating & type set according to file path. EML metadata is parsed and applied
   *         too.
   */
    private Checklist addChecklist(File dwca, @Nullable UUID resourceKey, @Nullable String title) {
        Checklist checklist = new Checklist();
        if (dwca.exists()) {
            String path = dwca.getPath();
            ChecklistType type = ChecklistType.unknown;
            Rating rating = Rating.average;
            if (path.contains("taxonomic") || path.contains("/col/")) {
                type = ChecklistType.taxonomic;
            } else if (path.contains("nomenclator")) {
                type = ChecklistType.nomenclator;
            } else if (path.contains("vernacular") || path.contains("inventory")) {
                type = ChecklistType.inventory;
                rating = Rating.poor;
            }
            log.info("Adding new " + type + " resource to CLB repository from " + dwca.getAbsolutePath());
            try {
                checklist.setResourceKey(resourceKey);
                checklist.setType(type);
                checklist.setDownloadUri("file://" + dwca.getAbsolutePath());
                updateChecklistWithArchiveMetadata(checklist);
                if (!StringUtils.isBlank(title)) {
                    checklist.setTitle(title);
                }
                insert(checklist);
                copyResourceToRepo(dwca, checklist.getId());
                impService.importNewTerms(checklist, false);
            } catch (ChecklistImportError e) {
                log.error("Adding of resource failed! " + e.getMessage());
                log.debug(org.gbif.ecat.utils.StringUtils.getStackTrace(e));
                if (checklist != null) {
                    if (checklist.getId() != null) {
                        remove(checklist.getId());
                        removeResourceRepoFolderIfExists(checklist.getId());
                    }
                }
                return null;
            }
        } else {
            log.warn("The path to the darwin core archive is not valid: " + dwca.getAbsolutePath());
        }
        return checklist;
    }

    @Override
    public Checklist addChecklist(URL dwcArchiveLocation, String title) {
        Checklist checklist = new Checklist();
        checklist.setTitle(title);
        checklist.setResourceKey((UUID) null);
        checklist.setType(ChecklistType.unknown);
        checklist.setDownloadUri(dwcArchiveLocation.toString());
        insert(checklist);
        try {
            impService.importNewTerms(checklist, false);
        } catch (ChecklistImportError e) {
            log.error("Adding of resource failed! " + e.getMessage());
            log.debug(org.gbif.ecat.utils.StringUtils.getStackTrace(e));
            if (checklist != null) {
                if (checklist.getId() != null) {
                    remove(checklist.getId());
                    removeResourceRepoFolderIfExists(checklist.getId());
                }
            }
            return null;
        }
        return checklist;
    }

    private void cleanMetadataString() {
        String sql = "update checklist set description=NULLIF(regexp_replace(description, '[\\s\\t\\r\\n]+', ' '),'')";
        executeUpdate(sql);
        sql = "update checklist set keywords=NULLIF(regexp_replace(keywords, '[\\s\\t\\r\\n]+', ' '),'')";
        executeUpdate(sql);
    }

    private void copyRegistryChecklistMetadata(Checklist source, Checklist target) {
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setDownloadUri(source.getDownloadUri());
        target.setKeywords(source.getKeywords());
        target.setHomeUrl(source.getHomeUrl());
        target.setLogoUrl(source.getLogoUrl());
        target.setCountry(source.getCountry());
    }

    /**
   * @param source
   * @param ds
   */
    private void copyRegistryDatasetMetadata(Checklist source, Dataset ds) {
        ds.setTitle(source.getTitle());
        ds.setDescription(source.getDescription());
        ds.setHomeUrl(source.getHomeUrl());
    }

    /**
   * reads an archive, either still zipped or expanded, and if valid copies it to the repository not touching the
   * database at all
   *
   * @param source      the dwc archive to use
   * @param checklistId the optional given checklist id. If null a new id from the postgres sequence will be used
   *
   * @return checklist id, either a new id or the one given if not null
   */
    private Integer copyResourceToRepo(File source, @Nullable Integer checklistId) throws ChecklistImportError {
        File sourceFolder = source;
        if (checklistId == null) {
            checklistId = generateId();
        }
        if (org.gbif.utils.file.FileUtils.isCompressedFile(source)) {
            try {
                sourceFolder = File.createTempFile(checklistId.toString(), source.getName());
                if (!(sourceFolder.delete())) {
                    throw new IOException("Could not delete temp file: " + sourceFolder.getAbsolutePath());
                }
                if (!(sourceFolder.mkdir())) {
                    throw new IOException("Could not create temp directory: " + sourceFolder.getAbsolutePath());
                }
                log.debug("DECOMPRESS ARCHIVE " + source.getAbsolutePath());
                CompressionUtil.decompressFile(sourceFolder, source);
            } catch (IOException e) {
                throw new ChecklistImportError("Cannot decompress dwc archive file " + source.getAbsolutePath());
            }
        }
        try {
            Archive archive = ArchiveFactory.openArchive(sourceFolder);
        } catch (Exception e) {
            throw new ChecklistImportError("Cannot open dwc archive", e);
        }
        try {
            File dwcaRepoDir = cfg.importDir(checklistId);
            if (dwcaRepoDir.exists()) {
                log.debug("clear existing source folder " + dwcaRepoDir.getAbsolutePath());
                FileUtils.deleteDirectory(dwcaRepoDir);
            }
            log.debug("copy checklist " + sourceFolder.getName() + " to repo: " + dwcaRepoDir.getAbsolutePath());
            if (sourceFolder.isFile()) {
                org.apache.commons.io.FileUtils.copyFileToDirectory(sourceFolder, dwcaRepoDir);
            } else {
                org.apache.commons.io.FileUtils.copyDirectory(sourceFolder, dwcaRepoDir);
            }
        } catch (IOException e) {
            throw new ChecklistImportError("Cannot copy resource", e);
        }
        return checklistId;
    }

    @Override
    public Map<Integer, Checklist> get(Collection<Integer> checklistIds) {
        if (checklistIds == null || checklistIds.size() == 0) {
            return new HashMap<Integer, Checklist>();
        }
        SqlStatement sql = sql();
        sql.where = "id in (" + StringUtils.join(checklistIds, ",") + ")";
        return queryForMap(sql, rowMapper);
    }

    @Override
    public Checklist get(String shortname) {
        if (shortname == null) {
            return null;
        }
        SqlStatement sql = sql();
        sql.where = "key = ?";
        return queryForObject(sql, rowMapper, shortname.toLowerCase());
    }

    @Override
    public Checklist get(UUID resourceKey) {
        if (resourceKey == null) {
            return null;
        }
        SqlStatement sql = sql();
        sql.where = "resource_key = ?";
        return queryForObject(sql, rowMapper, resourceKey.toString().toLowerCase());
    }

    private Checklist getByDownloadUri(String uri) {
        if (uri == null) {
            return null;
        }
        SqlStatement sql = sql();
        sql.where = "download_uri = ?";
        return queryForObject(sql, rowMapper, uri.toLowerCase());
    }

    @Override
    protected Object[] getInsertParameters(Checklist chk) {
        return new Object[] { chk.getResourceKey() == null ? null : chk.getResourceKey().toString(), StringUtils.lowerCase(chk.getKey()), chk.getSourceId(), chk.getTitle(), chk.getDescription(), chk.getCitation(), chk.getKeywords(), chk.getNumUsages(), chk.getNumAccepted(), chk.getNumCanonicals(), chk.getNumDescription(), chk.getNumDistribution(), chk.getNumImages(), chk.getNumReferences(), chk.getNumSpeciesData(), chk.getNumSpecimen(), chk.getNumVernacular(), chk.getNumK(), chk.getNumP(), chk.getNumC(), chk.getNumO(), chk.getNumF(), chk.getNumSF(), chk.getNumG(), chk.getNumSG(), chk.getNumS(), chk.getNumInfra(), chk.getPopularity(), chk.getNumRoot(), chk.getMaxDepth(), chk.getMaxWidth(), chk.getNubPriority(), chk.getType(), chk.getLastDownload(), chk.getLogoUrl(), chk.getHomeUrl(), chk.getRecordSourceUrl(), chk.getDownloadUri(), chk.isIgnore(), chk.getPubDate() };
    }

    @Override
    protected String getInsertSql() {
        return INSERT_SQL;
    }

    @Override
    protected Object[] getUpdateParameters(Checklist chk) {
        return new Object[] { chk.getResourceKey() == null ? null : chk.getResourceKey().toString(), StringUtils.lowerCase(chk.getKey()), chk.getSourceId(), chk.getTitle(), chk.getDescription(), chk.getCitation(), chk.getKeywords(), chk.getNumUsages(), chk.getNumAccepted(), chk.getNumCanonicals(), chk.getNumDescription(), chk.getNumDistribution(), chk.getNumImages(), chk.getNumReferences(), chk.getNumSpeciesData(), chk.getNumSpecimen(), chk.getNumVernacular(), chk.getNumK(), chk.getNumP(), chk.getNumC(), chk.getNumO(), chk.getNumF(), chk.getNumSF(), chk.getNumG(), chk.getNumSG(), chk.getNumS(), chk.getNumInfra(), chk.getPopularity(), chk.getNumRoot(), chk.getMaxDepth(), chk.getMaxWidth(), chk.getNubPriority(), chk.getType(), chk.getLastDownload(), chk.getLogoUrl(), chk.getHomeUrl(), chk.getRecordSourceUrl(), chk.getDownloadUri(), chk.isIgnore(), chk.getPubDate(), chk.getId() };
    }

    @Override
    protected String getUpdateSql() {
        return UPDATE_SQL;
    }

    @Override
    public List<Checklist> list() {
        return queryForList(sql(), rowMapper);
    }

    @Override
    public void remove(Integer id) {
        Connection con = null;
        try {
            con = getConnection(false);
            removeOneChecklist(con, id);
            con.commit();
        } catch (SQLException e) {
            log.error("Error deleting checklist " + id, e);
        } finally {
            JdbcTemplate.close(con);
        }
    }

    /**
   * Method to remove a single checklist in the database and on disc.
   * Note that we use a single connection and therefore transaction to be able to have inconsistent intermediate states
   * so that differed constraints are working fine without disabling them.
   */
    private void removeOneChecklist(Connection con, Integer id) {
        Checklist chkl = get(id);
        if (chkl == null) {
            log.warn("Cannot remove checklist " + id + ", it doesnt exist");
        } else {
            log.debug("Removing checklist " + id + " from CLB");
            removeResourceRepoFolderIfExists(id);
            String scheme = ImportBasePgSql.importSchema(chkl.getId());
            jdbcTemplate.executeUpdate(con, PgSqlUtils.dropSchemaSql(scheme));
            log.debug("Import schema " + scheme + " removed");
            String sql = "Delete from name_usage where checklist_fk=?";
            jdbcTemplate.executeUpdate(con, sql, id);
            sql = "Delete from hits where checklist_fk=?";
            jdbcTemplate.executeUpdate(con, sql, id);
            sql = "Delete from " + TABLE + " where id=?";
            jdbcTemplate.executeUpdate(con, sql, id);
        }
    }

    private void removeResourceRepoFolderIfExists(Integer checklistId) {
        File resourceDir = cfg.importDir(checklistId);
        log.debug("Removing resource folder from repository: " + resourceDir.getAbsolutePath());
        try {
            if (resourceDir.exists()) {
                FileUtils.deleteDirectory(resourceDir);
            } else {
                log.debug("Checklist resource folder not existing in repository");
            }
        } catch (IOException e) {
            log.error("Could not remove checklist repository dir " + resourceDir.getAbsolutePath(), e);
        }
    }

    @Override
    public List<Checklist> search(Integer minUsages, String title, ChecklistType... types) {
        SqlStatement sql = sql();
        if (minUsages != null) {
            sql.addWhereWithAnd("num_usages>=?");
            sql.params.add(minUsages);
        }
        if (title != null) {
            sql.addWhereWithAnd("title ~* ?");
            sql.params.add(title);
        }
        if (types != null && types.length > 0) {
            Set<Integer> tpyesAsInt = new HashSet<Integer>();
            for (ChecklistType ct : types) {
                tpyesAsInt.add(ct.ordinal());
            }
            sql.addWhereWithAnd("type in (" + StringUtils.join(tpyesAsInt, ",") + ")");
        }
        sql.orderby = "title";
        return queryForList(sql.toSql(), rowMapper, sql.paramArray());
    }

    @Override
    public int syncWithRegistry() {
        int newChecklists = 0;
        Collection<Checklist> registered = registryService.listChecklists();
        log.debug("Found " + registered.size() + " registered checklists to sync with");
        for (Checklist rc : registered) {
            try {
                Dataset ds = datasetService.get(rc.getResourceKey());
                if (ds != null) {
                    copyRegistryDatasetMetadata(rc, ds);
                    datasetService.update(ds);
                }
                Checklist chkl = get(rc.getResourceKey());
                if (chkl == null) {
                    chkl = getByDownloadUri(rc.getDownloadUri());
                }
                if (chkl == null) {
                    if (!StringUtils.isBlank(rc.getDownloadUri())) {
                        log.debug("Insert new checklist " + rc.getTitle() + " with archive url " + rc.getDownloadUri());
                        insert(rc);
                        newChecklists++;
                        try {
                            impService.importNewTerms(rc, false);
                        } catch (Exception e) {
                            log.warn("Failed to import basics for new checklist " + rc.getId(), e);
                        }
                    }
                } else {
                    copyRegistryChecklistMetadata(rc, chkl);
                    update(chkl);
                }
            } catch (Exception e) {
                if (rc.getResourceKey() == null) {
                    log.error("Exception syncing checklist without uuid", e);
                } else {
                    log.error("Exception syncing checklist " + rc.getResourceKey().toString(), e);
                }
            }
        }
        if (newChecklists == 0) {
            log.info("No new checklists found");
        } else {
            log.info("Added " + newChecklists + " new checklists");
        }
        return newChecklists;
    }

    /**
   * consult registry to see if checklist has passed the dwca validator yet.
   * Until the live registry webservices expose tags we look into the html keywords
   * of the corresponding validation report.
   *
   * @param c checklist to test
   */
    @Override
    public boolean isValid(Checklist c) {
        try {
            URL reportUrl = new URL("http://tools.gbif.org/dwca-reports/" + c.getResourceKey().toString() + ".html");
            String report = http.download(reportUrl);
            if (!StringUtils.isBlank(report) && report.contains("<meta name=\"keywords\" content=\"valid=true")) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Cannot check validation status", e);
        }
        return false;
    }

    /**
   * @param checklist checklist to update with resourceKey given
   *
   * @return same checklsit instance
   */
    @Override
    public Checklist updateChecklistWithArchiveMetadata(Checklist checklist) {
        if (checklist == null || checklist.getId() == null) {
            throw new IllegalArgumentException("Checklist with id required");
        }
        File chkFolder = cfg.importDir(checklist.getId());
        File eml = new File(chkFolder, emlFileName);
        if (eml.exists()) {
            try {
                BasicMetadata bm = metaFactory.read(eml);
                if (bm != null) {
                    if (bm.getTitle() != null) {
                        checklist.setTitle(bm.getTitle());
                    }
                    log.debug("EML metadata found in " + eml.getAbsolutePath() + " : " + bm.getTitle());
                    if (bm.getDescription() != null) {
                        checklist.setDescription(bm.getDescription());
                    }
                    if (bm.getSubject() != null) {
                        checklist.setKeywords(bm.getSubject());
                    }
                    if (bm.getPublished() != null) {
                        checklist.setPubDate(bm.getPublished());
                    }
                    if (bm.getHomepageUrl() != null) {
                        checklist.setHomeUrl(StringUtils.left(bm.getHomepageUrl(), Checklist.MAX_URL));
                    }
                    if (bm.getLogoUrl() != null) {
                        checklist.setLogoUrl(StringUtils.left(bm.getLogoUrl(), Checklist.MAX_URL));
                    }
                } else {
                    log.warn("EML metadata file found but not readable");
                }
            } catch (MetadataException e) {
                log.warn("EML metadata file not found or broken: " + e.getMessage());
            }
        }
        return checklist;
    }

    @Override
    public void updateCounts() {
        String sql;
        log.debug("Updating checklist usage counts ...");
        sql = "update checklist c set num_usages = (select count(u.id) from name_usage u where u.checklist_fk=c.id)";
        executeUpdate(sql);
        sql = "update checklist c set num_accepted_usages = (select count(u.id) from name_usage u where u.checklist_fk=c.id and is_synonym=false)";
        executeUpdate(sql);
        log.debug("Updating checklist usage counts by rank ...");
        sql = "update checklist c set num_k = (select count(u.id) from name_usage u where u.checklist_fk=c.id and u.id=kingdom_fk)";
        executeUpdate(sql);
        sql = "update checklist c set num_p = (select count(u.id) from name_usage u where u.checklist_fk=c.id and u.id=phylum_fk)";
        executeUpdate(sql);
        sql = "update checklist c set num_c = (select count(u.id) from name_usage u where u.checklist_fk=c.id and u.id=class_fk)";
        executeUpdate(sql);
        sql = "update checklist c set num_o = (select count(u.id) from name_usage u where u.checklist_fk=c.id and u.id=order_fk)";
        executeUpdate(sql);
        sql = "update checklist c set num_f = (select count(u.id) from name_usage u where u.checklist_fk=c.id and u.id=family_fk)";
        executeUpdate(sql);
        sql = "update checklist c set num_sf = (select count(u.id) from name_usage u where u.checklist_fk=c.id and u.id=subfamily_fk)";
        executeUpdate(sql);
        sql = "update checklist c set num_g = (select count(u.id) from name_usage u where u.checklist_fk=c.id and u.id=genus_fk)";
        executeUpdate(sql);
        sql = "update checklist c set num_sg = (select count(u.id) from name_usage u where u.checklist_fk=c.id and u.id=subgenus_fk)";
        executeUpdate(sql);
        sql = "update checklist c set num_s = (select count(u.id) from name_usage u where u.checklist_fk=c.id and u.id=species_fk)";
        executeUpdate(sql);
        sql = "update checklist c set num_is = (select count(u.id) from name_usage u join term r on rank_fk=r.id where checklist_fk=c.id and preferred_term_fk>? and preferred_term_fk <= ? and is_synonym=false)";
        executeUpdate(sql, Rank.SPECIES, Rank.Cultivar);
        log.debug("Updating checklist extension counts ...");
        sql = "update checklist c set num_description = (select count(v.id) from description v join name_usage u on v.usage_fk=u.id where u.checklist_fk=c.id) where c.id!=1";
        executeUpdate(sql);
        sql = "update checklist c set num_distribution = (select count(v.id) from distribution v join name_usage u on v.usage_fk=u.id where u.checklist_fk=c.id) where c.id!=1";
        executeUpdate(sql);
        sql = "update checklist c set num_images = (select count(v.id) from image v join name_usage u on v.usage_fk=u.id where u.checklist_fk=c.id) where c.id!=1";
        executeUpdate(sql);
        sql = "update checklist c set num_references = (select count(v.id) from name_usage_references v join name_usage u on v.usage_fk=u.id where u.checklist_fk=c.id) where c.id!=1";
        executeUpdate(sql);
        sql = "update checklist c set num_species_data = (select count(v.id) from species_data v join name_usage u on v.usage_fk=u.id where u.checklist_fk=c.id) where c.id!=1";
        executeUpdate(sql);
        sql = "update checklist c set num_specimen = (select count(v.id) from specimen v join name_usage u on v.usage_fk=u.id where u.checklist_fk=c.id) where c.id!=1";
        executeUpdate(sql);
        sql = "update checklist c set num_vernacular = (select count(v.id) from vernacular_name v join name_usage u on v.usage_fk=u.id where u.checklist_fk=c.id) where c.id!=1";
        executeUpdate(sql);
        log.debug("Updating nub extension counts ...");
        sql = "update checklist c set num_description= (select count(v.id) from description v join name_usage u on v.usage_fk=u.id join name_usage lu on lu.nub_fk=u.id   where lu.checklist_fk=c.id) where c.id=1";
        executeUpdate(sql);
        sql = "update checklist c set num_distribution = (select count(v.id) from distribution v join name_usage u on v.usage_fk=u.id join name_usage lu on lu.nub_fk=u.id   where lu.checklist_fk=c.id) where c.id=1";
        executeUpdate(sql);
        sql = "update checklist c set num_images = (select count(v.id) from image v join name_usage u on v.usage_fk=u.id join name_usage lu on lu.nub_fk=u.id   where lu.checklist_fk=c.id) where c.id=1";
        executeUpdate(sql);
        sql = "update checklist c set num_references = (select count(v.id) from name_usage_references v join name_usage u on v.usage_fk=u.id join name_usage lu on lu.nub_fk=u.id   where lu.checklist_fk=c.id) where c.id=1";
        executeUpdate(sql);
        sql = "update checklist c set num_species_data = (select count(v.id) from species_data v join name_usage u on v.usage_fk=u.id join name_usage lu on lu.nub_fk=u.id   where lu.checklist_fk=c.id) where c.id=1";
        executeUpdate(sql);
        sql = "update checklist c set num_specimen = (select count(v.id) from specimen v join name_usage u on v.usage_fk=u.id join name_usage lu on lu.nub_fk=u.id   where lu.checklist_fk=c.id) where c.id=1";
        executeUpdate(sql);
        sql = "update checklist c set num_vernacular = (select count(v.id) from vernacular_name v join name_usage u on v.usage_fk=u.id join name_usage lu on lu.nub_fk=u.id   where lu.checklist_fk=c.id) where c.id=1";
        executeUpdate(sql);
    }

    @Override
    public void updateCounts(Checklist c) {
        Integer checklistId = c.getId();
        String sql;
        log.debug("Updating usage counts for checklist " + checklistId + " ...");
        sql = "select count(u.id) from name_usage u where u.checklist_fk=?";
        c.setNumUsages(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and is_synonym=false";
        c.setNumAccepted(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and u.is_synonym=false and u.parent_fk is null";
        c.setNumRoot(queryForInt(sql, checklistId));
        sql = "select count(distinct(name_fk)) from name_usage where checklist_fk=?";
        c.setNumSciNames(queryForInt(sql, checklistId));
        sql = "select count(distinct(n.canonical_name_fk)) from name_usage u join name_string n on name_fk=n.id where u.checklist_fk=?";
        c.setNumCanonicals(queryForInt(sql, checklistId));
        log.debug("Updating checklist usage counts by rank ...");
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and u.id=kingdom_fk";
        c.setNumK(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and u.id=phylum_fk";
        c.setNumP(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and u.id=class_fk";
        c.setNumC(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and u.id=order_fk";
        c.setNumO(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and u.id=family_fk";
        c.setNumF(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and u.id=subfamily_fk";
        c.setNumSF(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and u.id=genus_fk";
        c.setNumG(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and u.id=subgenus_fk";
        c.setNumSG(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u where u.checklist_fk=? and u.id=species_fk";
        c.setNumS(queryForInt(sql, checklistId));
        sql = "select count(u.id) from name_usage u join term r on rank_fk=r.id where checklist_fk=? and preferred_term_fk>? and preferred_term_fk <= ? and is_synonym=false";
        c.setNumInfra(queryForInt(sql, checklistId, Rank.SPECIES, Rank.Cultivar));
        log.debug("Updating checklist extension counts ...");
        sql = "select count(v.id) from description v join name_usage u on v.usage_fk=u.id where u.checklist_fk=?";
        c.setNumDescription(queryForInt(sql, checklistId));
        sql = "select count(v.id) from distribution v join name_usage u on v.usage_fk=u.id where u.checklist_fk=?";
        c.setNumDistribution(queryForInt(sql, checklistId));
        sql = "select count(v.id) from image v join name_usage u on v.usage_fk=u.id where u.checklist_fk=?";
        c.setNumImages(queryForInt(sql, checklistId));
        sql = "select count(v.id) from name_usage_references v join name_usage u on v.usage_fk=u.id where u.checklist_fk=?";
        c.setNumReferences(queryForInt(sql, checklistId));
        sql = "select count(v.id) from specimen v join name_usage u on v.usage_fk=u.id where u.checklist_fk=?";
        c.setNumSpecimen(queryForInt(sql, checklistId));
        sql = "select count(v.id) from species_data v join name_usage u on v.usage_fk=u.id where u.checklist_fk=?";
        c.setNumSpeciesData(queryForInt(sql, checklistId));
        sql = "select count(v.id) from vernacular_name v join name_usage u on v.usage_fk=u.id where u.checklist_fk=?";
        c.setNumVernacular(queryForInt(sql, checklistId));
    }
}
