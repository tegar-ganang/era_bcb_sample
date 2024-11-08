package org.jfree.xml.generator.model;

/**
 * The manual mapping describes, how a certain class is handled in the parser.
 * This defines the read and write handler implementations to be used to handle
 * the instantiation or serialisation of the described type.
 * <p>
 * Manual mappings will not be created by the generator, they have to be defined
 * manually. The parser will print warnings, if the definitions are invalid.
 * <p>
 * Manual mappings will always override automatic mappings. 
 */
public class ManualMappingInfo {

    /** The base class. */
    private Class baseClass;

    /** The read handler. */
    private Class readHandler;

    /** The write handler. */
    private Class writeHandler;

    /** The comments. */
    private Comments comments;

    /** The source. */
    private String source;

    /**
     * Creates a new manual mapping instance.
     * 
     * @param baseClass  the base class.
     * @param readHandler  the read handler class.
     * @param writeHandler  the write handler class.
     */
    public ManualMappingInfo(final Class baseClass, final Class readHandler, final Class writeHandler) {
        this.baseClass = baseClass;
        this.readHandler = readHandler;
        this.writeHandler = writeHandler;
    }

    /**
     * Returns the base class.
     * 
     * @return The base class.
     */
    public Class getBaseClass() {
        return this.baseClass;
    }

    /**
     * Returns the read handler class.
     * 
     * @return The read handler class.
     */
    public Class getReadHandler() {
        return this.readHandler;
    }

    /**
     * Returns the write handler class.
     * 
     * @return The write handler class.
     */
    public Class getWriteHandler() {
        return this.writeHandler;
    }

    /**
     * Returns the comments.
     * 
     * @return The comments.
     */
    public Comments getComments() {
        return this.comments;
    }

    /**
     * Sets the comments.
     * 
     * @param comments  the comments.
     */
    public void setComments(final Comments comments) {
        this.comments = comments;
    }

    /**
     * Returns the source.
     * 
     * @return The source.
     */
    public String getSource() {
        return this.source;
    }

    /**
     * Sets the source.
     * 
     * @param source  the source.
     */
    public void setSource(final String source) {
        this.source = source;
    }
}
