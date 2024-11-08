package org.exteca.ontology;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import org.exteca.ontology.xml.Dom4jOntologyReader;
import org.exteca.ontology.xml.Dom4jOntologyWriter;

/**
 *  OntologyPersistence is responsible for reading and writing Ontologies.
 * 
 *  @author Mauro Talevi
 */
public class OntologyPersistence implements OntologyReader, OntologyWriter {

    /** The default OntologyReader implementation */
    private static final String DEFAULT_READER = Dom4jOntologyReader.class.getName();

    /** The default OntologyReader implementation */
    private static final String DEFAULT_WRITER = Dom4jOntologyWriter.class.getName();

    /** The specified OntologyReader implementation */
    private String readerClassName;

    /** The specified OntologyWriter implementation */
    private String writerClassName;

    /**
	 * Creates an OntologyPersistence
	 */
    public OntologyPersistence() {
        this(DEFAULT_READER, DEFAULT_WRITER);
    }

    /**
	 * Creates an OntologyPersistence
	 * @param readerClassName the Class name of the OntologyReader implementation
	 * @param writerClassName the Class name of the OntologyWriter implementation
	 */
    public OntologyPersistence(String readerClassName, String writerClassName) {
        this.readerClassName = readerClassName;
        this.writerClassName = writerClassName;
    }

    /**
	 * @see org.exteca.ontology.OntologyReader#read(java.io.File)
	 */
    public Ontology read(File input) throws OntologyException {
        try {
            return read(new FileReader(input));
        } catch (FileNotFoundException e) {
            throw new OntologyException(e);
        }
    }

    /**
	 * @see org.exteca.ontology.OntologyReader#read(java.io.InputStream)
	 */
    public Ontology read(InputStream input) throws OntologyException {
        return read(new InputStreamReader(input));
    }

    /**
	 * @see org.exteca.ontology.OntologyReader#read(java.net.URL)
	 */
    public Ontology read(URL input) throws OntologyException {
        try {
            return read(input.openStream());
        } catch (IOException e) {
            throw new OntologyException(e);
        }
    }

    /**
	 * @see org.exteca.ontology.OntologyReader#read(java.io.Reader)
	 */
    public Ontology read(Reader input) throws OntologyException {
        return getReader().read(input);
    }

    /**
     * @see org.exteca.ontology.OntologyWriter#write(org.exteca.ontology.Ontology, java.io.File)
     */
    public void write(Ontology ontology, File output) throws OntologyException {
        getWriter().write(ontology, output);
    }

    /**
     * @see org.exteca.ontology.OntologyWriter#write(org.exteca.ontology.Ontology, java.io.OutputStream)
     */
    public void write(Ontology ontology, OutputStream output) throws OntologyException {
        getWriter().write(ontology, output);
    }

    /**
     * @see org.exteca.ontology.OntologyWriter#write(org.exteca.ontology.Ontology, java.io.Writer)
     */
    public void write(Ontology ontology, Writer output) throws OntologyException {
        getWriter().write(ontology, output);
    }

    /**
	 * Instantiates an OntologyReader from the specified class
	 * @return OntologyReader
	 * @throws OntologyException
	 */
    private OntologyReader getReader() throws OntologyException {
        try {
            return (OntologyReader) Class.forName(readerClassName).newInstance();
        } catch (Exception e) {
            throw new OntologyException("Cannot instantiate OntologyReader for " + readerClassName, e);
        }
    }

    /**
	 * Instantiates an OntologyWriter from the specified class
	 * @return OntologyWriter
	 * @throws OntologyException
	 */
    private OntologyWriter getWriter() throws OntologyException {
        try {
            return (OntologyWriter) Class.forName(writerClassName).newInstance();
        } catch (Exception e) {
            throw new OntologyException("Cannot instantiate OntologyWriter for " + writerClassName, e);
        }
    }
}
