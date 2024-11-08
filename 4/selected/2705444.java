package org.vardb.genomes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Required;
import org.vardb.CConstants;
import org.vardb.blast.CGenomeBlastParams;
import org.vardb.blast.IBlastService;
import org.vardb.genomes.dao.CChromosome;
import org.vardb.genomes.dao.CGene;
import org.vardb.genomes.dao.CGenome;
import org.vardb.genomes.dao.CGenomeFeature;
import org.vardb.genomes.dao.IGenomeDao;
import org.vardb.lists.IdList;
import org.vardb.resources.CResourceType;
import org.vardb.sequences.CSimpleLocation;
import org.vardb.util.CFileHelper;
import org.vardb.util.CMessageWriter;
import org.vardb.util.CStringHelper;
import org.vardb.util.CTable;
import org.vardb.util.IPaging;

public class CGenomeServiceImpl implements IGenomeService {

    @Resource(name = "blastService")
    protected IBlastService blastService;

    protected IGenomeDao dao;

    public IGenomeDao getDao() {
        return this.dao;
    }

    @Required
    public void setDao(IGenomeDao dao) {
        this.dao = dao;
    }

    public CGenome addGenome() {
        CGenome genome = new CGenome();
        getDao().addGenome(genome);
        return genome;
    }

    public CChromosome addChromosome(int genome_id) {
        CGenome genome = getDao().getGenome(genome_id);
        CChromosome chromosome = new CChromosome();
        genome.add(chromosome);
        getDao().addChromosome(chromosome);
        return chromosome;
    }

    public List<CGenome> getGenomes() {
        List<CGenome> genomes = new ArrayList<CGenome>();
        for (CGenome genome : getDao().getGenomes()) {
            {
                genome.getPathogen().getIdentifier();
                genomes.add(genome);
            }
        }
        return genomes;
    }

    public CGenome getGenome(int genome_id) {
        CGenome genome = getDao().getGenome(genome_id);
        genome.initialize();
        return genome;
    }

    public CGenome getGenome(String identifier) {
        CGenome genome = getDao().getGenome(identifier);
        genome.initialize();
        return genome;
    }

    public CGenome findOrCreateGenome(String identifier) {
        CStringHelper.checkIdentifier(identifier);
        CGenome genome = getDao().getGenome(identifier);
        if (genome == null) {
            genome = new CGenome(identifier);
            getDao().addGenome(genome);
        }
        return genome;
    }

    public Map<String, CGenome> findOrCreateGenomes(Collection<String> identifiers) {
        Map<String, CGenome> map = new HashMap<String, CGenome>();
        for (CGenome taxon : getDao().getGenomes(identifiers)) {
            map.put(taxon.getIdentifier(), taxon);
        }
        for (String identifier : identifiers) {
            CStringHelper.checkIdentifier(identifier);
            if (map.containsKey(identifier)) continue;
            CGenome genome = new CGenome(identifier);
            getDao().addGenome(genome);
            map.put(genome.getIdentifier(), genome);
        }
        return map;
    }

    public CChromosome getChromosome(int chromosome_id) {
        CChromosome chromosome = getDao().getChromosome(chromosome_id);
        chromosome.initialize();
        return chromosome;
    }

    public CChromosome getChromosome(String identifier) {
        CChromosome chromosome = getDao().getChromosome(identifier);
        chromosome.initialize();
        return chromosome;
    }

    public CChromosome findOrCreateChromosome(CGenome genome, String identifier) {
        CStringHelper.checkIdentifier(identifier);
        CChromosome chromosome = getDao().getChromosome(identifier);
        if (chromosome == null) {
            chromosome = new CChromosome(identifier);
            genome.add(chromosome);
            getDao().addChromosome(chromosome);
        }
        return chromosome;
    }

    public Map<String, CChromosome> findOrCreateChromosomes(CGenome genome, Collection<String> identifiers) {
        Map<String, CChromosome> map = new HashMap<String, CChromosome>();
        for (CChromosome chromosome : getDao().getChromosomes(identifiers)) {
            map.put(chromosome.getIdentifier(), chromosome);
        }
        List<CChromosome> added = new ArrayList<CChromosome>();
        for (String identifier : identifiers) {
            CStringHelper.checkIdentifier(identifier);
            if (map.containsKey(identifier)) continue;
            CChromosome chromosome = new CChromosome(identifier);
            genome.add(chromosome);
            map.put(chromosome.getIdentifier(), chromosome);
            added.add(chromosome);
        }
        getDao().addOrUpdateChromosomes(added);
        return map;
    }

    public void convertGenome(String infolder, String outfolder, CMessageWriter writer) {
        String identifier = CFileHelper.getIdentifierFromDirname(infolder);
        outfolder = outfolder + identifier + "/";
        if (!CFileHelper.exists(outfolder)) CFileHelper.createDirectory(outfolder);
        CGenomeConverter.convertFolder(identifier, infolder, outfolder, writer);
    }

    public void loadGenomeFromFolder(String folder, CMessageWriter writer) {
        String genome_identifier = CFileHelper.getIdentifierFromDirname(folder);
        writer.message("loading genome from folder=" + folder + " identifer=" + genome_identifier);
        String genomefile = folder + genome_identifier + CConstants.TABLE_SUFFIX;
        getDao().deleteAllFeatures(genome_identifier);
        getDao().deleteAllFragments(genome_identifier);
        if (CFileHelper.exists(genomefile)) {
            CTable table = CTable.parseFile(genomefile);
            CResourceType type = table.getResourceType();
            if (type == CResourceType.CHROMOSOME) loadChromosomeTable(table, writer);
            writer.message("finished loading chromosome list file: " + genomefile);
        } else writer.message("could not find chromosome list file: " + genomefile);
        List<String> filenames = CFileHelper.listFilesRecursively(folder, CConstants.TABLE_SUFFIX);
        for (String filename : filenames) {
            CTable table = CTable.parseFile(filename);
            CResourceType type = table.getResourceType();
            if (type != CResourceType.FEATURE) continue;
            writer.message("loading table: " + filename + "...");
            loadFeatureTable(table, writer);
            writer.message("finished loading tables");
        }
        filenames = CFileHelper.listFilesRecursively(folder, CConstants.FASTA_SUFFIX);
        for (String filename : filenames) {
            writer.message("loading table: " + filename + "...");
            String chromsome_identifier = CFileHelper.getIdentifierFromFilename(filename, CConstants.FASTA_SUFFIX);
            loadChromosomeSequence(chromsome_identifier, filename);
        }
        writer.message("Finished loading genomes from folder: " + folder);
    }

    public void loadChromosomeTable(CTable table, CMessageWriter writer) {
        CChromosomeTableReader reader = new CChromosomeTableReader(this, writer);
        reader.loadTable(table);
    }

    public void loadFeatureTable(CTable table, CMessageWriter writer) {
        CFeatureTableReader reader = new CFeatureTableReader(this, writer);
        reader.loadTable(table);
    }

    private void loadChromosomeSequence(String identifier, String filename) {
        CChromosome chromosome = getDao().getChromosome(identifier);
        CGenomeConverter.partitionSequence(filename, chromosome);
        getDao().addFragments(chromosome.getFragments());
    }

    public void loadDiegoGenomeFromFolder(String folder, CMessageWriter writer) {
        CDiegoGenomeLoader loader = new CDiegoGenomeLoader(this, folder, writer);
        loader.load();
    }

    public List<CGenomeFeature> getFeatures(String identifier, CSimpleLocation location) {
        CChromosome chromosome = getDao().getChromosome(identifier);
        List<CGenomeFeature> features = new ArrayList<CGenomeFeature>();
        for (CSimpleLocation.SubLocation loc : location.getSublocations()) {
            features.addAll(getDao().getFeatures(chromosome.getId(), loc.getStart(), loc.getEnd()));
        }
        return features;
    }

    public List<CGenomeFeature> getFeatures(String identifier, int start, int end) {
        CChromosome chromosome = getDao().getChromosome(identifier);
        return getDao().getFeatures(chromosome.getId(), start, end);
    }

    public String getSequence(String identifier, CSimpleLocation location) {
        CChromosome chromosome = getDao().getChromosome(identifier);
        StringBuilder buffer = new StringBuilder();
        for (CSimpleLocation.SubLocation loc : location.getSublocations()) {
            buffer.append(getDao().getSequence(chromosome.getId(), loc.getStart(), loc.getEnd()));
        }
        return buffer.toString();
    }

    public String getSequence(String identifier, int start, int end) {
        CChromosome chromosome = getDao().getChromosome(identifier);
        return getDao().getSequence(chromosome.getId(), start, end);
    }

    public String getFlankingRegions(List<String> locus_tags, List<CConstants.FlankingSequenceType> types, int length) {
        CDownloadFlankingHelper helper = new CDownloadFlankingHelper(this.dao);
        return helper.getFlankingRegions(locus_tags, types, length);
    }

    public CGene findGeneByLocusTag(String locus_tag) {
        return getDao().findGeneByLocusTag(locus_tag);
    }

    public CContig getContig(String identifier, int start, int end) {
        CChromosome chromosome = getDao().getChromosome(identifier);
        return this.dao.getContig(chromosome, start, end);
    }

    public CGenomeTile getTile(CGenomeTile.Params params) {
        String identifier = params.getChr();
        CChromosome chromosome = getDao().getChromosome(identifier);
        CContig contig = new CContig(chromosome, params.getStart(), params.getEnd());
        contig.setFeatures(getDao().getFeatures(chromosome.getId(), params.getStart(), params.getEnd(), CGenomeTile.FEATURE_TYPES));
        if (params.getResolution() <= 1) contig.setSequence(getDao().getSequence(chromosome.getId(), params.getStart(), params.getEnd()));
        return new CGenomeTile(chromosome, contig, params);
    }

    public List<CGenomeFeature> findFeatures(String identifier, int bp, int offset) {
        CChromosome chromosome = getDao().getChromosome(identifier);
        List<CGenomeFeature> features = getDao().getFeatures(chromosome.getId(), bp);
        return CGenomeTile.selectFeatures(features, offset);
    }

    public List<CGenomeFeature> getFeatures(String identifier, IPaging paging) {
        CChromosome chromosome = getDao().getChromosome(identifier);
        return getDao().getFeatures(chromosome.getId(), paging);
    }

    public Map<String, String> downloadSequences(CConstants.GenomeDownloadType type, IdList ids) {
        CDownloadHelper helper = new CDownloadHelper(this.dao);
        return helper.downloadSequences(type, ids);
    }

    public List<CGenomeFeature> findFeatures(String search, IPaging paging) {
        String sql = CGenomeQueryHelper.convert(search);
        if (sql == null) return null;
        return getDao().findFeatures(sql, paging);
    }

    public List<CGenomeFeature> blast(CGenomeBlastParams params) {
        List<String> accessions = blastService.blast(params);
        for (String accession : accessions) {
            System.out.println("accession=" + accession);
        }
        return getDao().getFeaturesByAccession(accessions);
    }
}
