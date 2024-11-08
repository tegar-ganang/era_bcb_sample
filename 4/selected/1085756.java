package org.vardb.admin;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;
import org.vardb.CConstants;
import org.vardb.CVardbException;
import org.vardb.alignment.IAlignmentService;
import org.vardb.blast.CBlastHits;
import org.vardb.blast.CBlastParams;
import org.vardb.blast.IBlastService;
import org.vardb.cache.ICacheService;
import org.vardb.genomes.IGenomeService;
import org.vardb.lists.IdList;
import org.vardb.resources.CResourceType;
import org.vardb.resources.IResourceService;
import org.vardb.resources.dao.CChain;
import org.vardb.resources.dao.CChainBlastHit;
import org.vardb.resources.dao.CStructure;
import org.vardb.sequences.ISequence;
import org.vardb.sequences.ISequenceService;
import org.vardb.sequences.dao.CSequence;
import org.vardb.tags.ITagService;
import org.vardb.users.IUserService;
import org.vardb.util.CFileHelper;
import org.vardb.util.CMathHelper;
import org.vardb.util.CMessageWriter;
import org.vardb.util.CPDBReader;
import org.vardb.util.CStringHelper;
import org.vardb.util.CTable;
import org.vardb.util.CXmlHelper;
import org.vardb.util.CXmlValidationException;

@Transactional(readOnly = false)
public class CAdminServiceImpl implements IAdminService {

    @Resource(name = "resourceService")
    private IResourceService resourceService;

    @Resource(name = "genomeService")
    private IGenomeService genomeService;

    @Resource(name = "sequenceService")
    private ISequenceService sequenceService;

    @Resource(name = "cacheService")
    private ICacheService cacheService;

    @Resource(name = "tagService")
    private ITagService tagService;

    @Resource(name = "userService")
    private IUserService userService;

    @Resource(name = "blastService")
    private IBlastService blastService;

    @Resource(name = "alignmentService")
    private IAlignmentService alignmentService;

    private String dataDir;

    private String tempDir;

    private Integer sequenceTableBatchSize = 1000;

    public String getDataDir() {
        return this.dataDir;
    }

    @Required
    public void setDataDir(final String dataDir) {
        this.dataDir = dataDir;
    }

    public String getTempDir() {
        return this.tempDir;
    }

    @Required
    public void setTempDir(final String tempDir) {
        this.tempDir = tempDir;
    }

    public void loadXml(String xml, CMessageWriter writer) {
        CXmlDataReader reader = new CXmlDataReader(this.resourceService, this.genomeService, this.tagService, this.userService, writer);
        try {
            reader.loadXml(xml);
        } catch (Exception e) {
            CFileHelper.writeFile("c:/setup.xml", xml);
            throw new CVardbException(e);
        }
    }

    public void loadXmlFromFile(String filename, CMessageWriter writer) {
        loadXml(CFileHelper.readFile(filename), writer);
        this.cacheService.clearCache();
        writer.message("Finished loading resources from file: " + filename);
    }

    public void loadXmlFromFolder(String folder, CMessageWriter writer) {
        String xml = CXmlDataReader.mergeXmlFiles(folder);
        loadXml(xml, writer);
        this.cacheService.clearCache();
        writer.message("Finished loading resources from folder: " + folder);
    }

    public void loadXmlFromFolder(String folder, Date date, CMessageWriter writer) {
        if (date != null) System.out.println("loading files updated since " + date + " (" + date.getTime() + ")");
        String xml = CXmlDataReader.mergeXmlFiles(folder, date);
        loadXml(xml, writer);
        this.cacheService.clearCache();
        writer.message("Finished loading resources from folder: " + folder);
    }

    public void validate(String xml, CMessageWriter writer) {
        try {
            String xsd = CFileHelper.getResource(CConstants.SCHEMA);
            CXmlHelper.validate(xml, xsd);
        } catch (CXmlValidationException e) {
            writer.message(e.getMessage());
        }
    }

    public void validateFolder(String folder, CMessageWriter writer) {
        List<String> filenames = CFileHelper.listFilesRecursively(folder, ".xml");
        for (String filename : filenames) {
            String xml = CFileHelper.readFile(filename);
            System.out.println("Validating " + filename);
            validate(xml, writer);
        }
    }

    public void loadTable(String str, CLoadTableParams params) {
        CTable table = new CTable(str);
        loadTable(table, params);
    }

    public void loadTableFromFile(String filename, CLoadTableParams params) {
        System.out.println("Loading file: " + filename);
        String identifier = CFileHelper.getIdentifierFromFilename(filename, CConstants.TABLE_SUFFIX);
        if (identifier.equals("gene_list") || identifier.equals("protein_list")) return;
        params.getWriter().write("loading table: " + filename + " ... ");
        CTable table = CTable.parseFile(filename);
        if (table == null) {
            params.getWriter().message("SKIP\n * file " + filename + " is empty. skipping.");
            return;
        }
        table.setIdentifier(identifier);
        params.setExcluded(getExcluded(filename, params.getWriter()));
        loadTable(table, params);
        params.getWriter().message("DONE\n");
    }

    private Map<String, String> getExcluded(String filename, CMessageWriter writer) {
        String exc_file = filename + CConstants.EXCLUDE_SUFFIX;
        if (!CFileHelper.exists(exc_file)) return new HashMap<String, String>();
        writer.message("Using exclusion file: " + exc_file);
        CTable exclusion_table = CTable.parseFile(exc_file);
        if (exclusion_table == null) return new HashMap<String, String>();
        return exclusion_table.getColumnData("reason");
    }

    public void loadTable(CTable table, CLoadTableParams params) {
        CResourceType type = table.getResourceType();
        if (type == CResourceType.SEQUENCE) loadSequenceTable(table, params); else if (type == CResourceType.CHROMOSOME) this.genomeService.loadChromosomeTable(table, params.getWriter()); else if (type == CResourceType.FEATURE) this.genomeService.loadFeatureTable(table, params.getWriter()); else throw new CVardbException("loadTable not supported for type " + table.getResourceType());
    }

    public void loadSequenceTable(CTable table, CLoadTableParams params) {
        CSequenceTableReader reader = new CSequenceTableReader(params, this.resourceService, this.sequenceService, this.genomeService);
        int batchsize = this.sequenceTableBatchSize;
        int numsequences = table.getRows().size();
        System.out.println("numsequences: " + numsequences);
        System.out.println("batchsize: " + batchsize);
        int numbatches = CMathHelper.getNumbatches(numsequences, batchsize);
        for (int batchnumber = 0; batchnumber < numbatches; batchnumber++) {
            int start = batchnumber * batchsize;
            reader.loadTable(table, start, batchsize);
        }
    }

    public void loadGenomeFromFolder(String folder, CMessageWriter writer) {
        this.genomeService.loadGenomeFromFolder(folder, writer);
    }

    public void loadDiegoGenomeFromFolder(String folder, CMessageWriter writer) {
        this.genomeService.loadDiegoGenomeFromFolder(folder, writer);
    }

    public void loadStructuresFromFolder(String folder, CMessageWriter writer) {
        String suffix = ".pdb";
        List<String> filenames = CFileHelper.listFilesRecursively(folder, suffix);
        for (String filename : filenames) {
            String identifier = CFileHelper.getIdentifierFromFilename(filename, suffix);
            CStructure structure = this.resourceService.findOrCreateStructure(identifier);
            CPDBReader.parsePDBFile(filename, structure);
            this.resourceService.updateStructure(structure);
        }
        blastChains();
        writer.message("Finished loading structures from folder: " + folder);
    }

    public void loadAlignmentsFromFolder(String folder, CMessageWriter writer) {
        try {
            this.alignmentService.loadAlignmentsFromFolder(folder, writer);
            writer.message("Finished adding alignments from folder: " + folder);
        } catch (Exception e) {
            writer.error("problem loading alignments from folder: " + folder, e);
        }
    }

    private void blastChains() {
        this.resourceService.getDao().deleteAllBlastChainHits();
        for (CStructure structure : this.resourceService.getDao().getStructures()) {
            blastChains(structure);
        }
    }

    private void blastChains(CStructure structure) {
        int total = 0;
        for (CChain chain : structure.getChains()) {
            if (!CStringHelper.hasContent(chain.getSequence())) continue;
            System.out.println("trying to find blast hits for chain: " + chain.getIdentifier());
            CBlastParams params = new CBlastParams(chain.getSequence());
            params.setMaxresults(1000);
            params.setMaxalignments(1);
            CBlastHits hits = this.blastService.blast(params);
            IdList ids = hits.getDefaultIteration().getIds(this.sequenceService);
            List<ISequence> sequences = this.sequenceService.getDao().getSequencesById(ids);
            System.out.println("--found " + sequences.size() + " hits");
            chain.setNumsequences(ids.size());
            total += ids.size();
            for (ISequence seq : sequences) {
                CSequence sequence = (CSequence) seq;
                CBlastHits.Hit hit = hits.getDefaultIteration().getHit(sequence.getAccession());
                CChainBlastHit chainblasthit = new CChainBlastHit(hit, sequence);
                chain.addHit(chainblasthit);
                this.resourceService.getDao().addChainBlastHit(chainblasthit);
            }
        }
        structure.setNumsequences(total);
        this.resourceService.getDao().updateStructure(structure);
    }

    public void updatePfamDomains(CMessageWriter writer) {
        List<String> identifiers = this.sequenceService.findPfamIdentifiers();
        this.resourceService.getDao().findOrCreatePfams(identifiers);
        this.sequenceService.indexPfamDomains(writer);
    }
}
