package net.sf.josser.rdf.impl;

/**
 * @author Copyright © Giovanni Novelli. All rights reserved.
 */
public class Dump {

    private String path = "g:/dmoz/";

    private String structureFile = "structure.rdf.u8";

    private String contentFile = "content.rdf.u8";

    protected String structurePath() {
        return this.getPath() + this.getStructureFile();
    }

    protected String contentPath() {
        return this.getPath() + this.getContentFile();
    }

    private StructurePre spreparser = null;

    private Structure sparser = null;

    private Content cparser = null;

    public Dump(final String path) {
        this.path = path;
        this.setSpreparser(new StructurePre(this.structurePath()));
        this.setSparser(new Structure(this.structurePath()));
        this.setCparser(new Content(this.contentPath()));
    }

    public void parse(final String dbtype, final int readlines, final int writelines) {
        this.getSpreparser().parse(readlines);
        this.getSparser().parse(writelines);
        System.gc();
        this.getCparser().parse(writelines);
    }

    /**
	 * @param contentFile
	 *            The contentFile to set.
	 */
    public void setContentFile(final String contentFile) {
        this.contentFile = contentFile;
    }

    /**
	 * @return Returns the contentFile.
	 */
    public String getContentFile() {
        return this.contentFile;
    }

    /**
	 * @param cparser
	 *            The cparser to set.
	 */
    public void setCparser(final Content cparser) {
        this.cparser = cparser;
    }

    /**
	 * @return Returns the cparser.
	 */
    public Content getCparser() {
        return this.cparser;
    }

    /**
	 * @param path
	 *            The path to set.
	 */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
	 * @return Returns the path.
	 */
    public String getPath() {
        return this.path;
    }

    /**
	 * @param sparser
	 *            The sparser to set.
	 */
    public void setSparser(final Structure sparser) {
        this.sparser = sparser;
    }

    /**
	 * @return Returns the sparser.
	 */
    public Structure getSparser() {
        return this.sparser;
    }

    /**
	 * @param spreparser
	 *            The spreparser to set.
	 */
    public void setSpreparser(final StructurePre spreparser) {
        this.spreparser = spreparser;
    }

    /**
	 * @return Returns the spreparser.
	 */
    public StructurePre getSpreparser() {
        return this.spreparser;
    }

    /**
	 * @param structureFile
	 *            The structureFile to set.
	 */
    public void setStructureFile(final String structureFile) {
        this.structureFile = structureFile;
    }

    /**
	 * @return Returns the structureFile.
	 */
    public String getStructureFile() {
        return this.structureFile;
    }
}
