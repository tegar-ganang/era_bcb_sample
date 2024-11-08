package net.smartlab.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Represents a configuration expressed through an XML document.
 * 
 * @author rlogiacco
 */
public final class XMLConfiguration extends Configuration {

    /**
	 * Last access time to the underlying byte representation. Used to identify
	 * a possible need to update the in-memory representation.
	 */
    private long access;

    /**
	 * The location of the byte representation of the configuration.
	 */
    private URL source;

    /**
	 * The optional ciphering structure to decode an encrypted configuration.
	 */
    private Cipher cipher;

    /**
	 * Constructs a configuration structure from an XML file.
	 * 
	 * @param source a <code>File</code> object pointing to the underlying
	 *        represantation of the configuration.
	 * @exception ConfigurationException if the file specified is not a valid
	 *            XML file or is no more readable.
	 */
    public XMLConfiguration(File source) throws ConfigurationException {
        try {
            this.source = source.toURL();
        } catch (MalformedURLException murle) {
            throw new ConfigurationException(murle);
        }
        this.update();
    }

    /**
	 * Constructs a configuration structure from an XML file accessible through
	 * a file system dependent string representation of a path.
	 * 
	 * @param source a <code>String</code> representation of a system
	 *        dependant path pointing to a valid configuration representation.
	 * @exception ConfigurationException if the file is not a valid XML file or
	 *            is no more accessible through the specified path.
	 */
    public XMLConfiguration(String source) throws ConfigurationException {
        this(new File(source));
    }

    /**
	 * Constructs a configuration structure from an XML stream of characters.
	 * The <code>isChanged</code> method returns always <code>false</code>
	 * 
	 * @param source a stream of characters representing a valid XML document.
	 * @exception ConfigurationException if the stream doesn't represent a valid
	 *            XML document or if an IO error were generated while accessing
	 *            the stream.
	 */
    public XMLConfiguration(URL source) throws ConfigurationException {
        this(source, null);
    }

    /**
	 * Constructs a configuration structure from an encrypted XML file. This
	 * constructor needs a <code>Cipher</code> reference correctly initialized
	 * for the appropriate encoding scheme.
	 * 
	 * @param source a <code>File</code> object pointing to the underlying
	 *        represantation of the configuration.
	 * @param cipher an appropriately initialized instance of a
	 *        <code>Cipher</code> to be used to decrypt the document.
	 * @throws ConfigurationException if the file specified is not a valid XML
	 *         file or is no more readable.
	 */
    public XMLConfiguration(File source, Cipher cipher) throws ConfigurationException {
        try {
            this.source = source.toURL();
        } catch (MalformedURLException murle) {
            throw new ConfigurationException(murle);
        }
        this.cipher = cipher;
        this.update();
    }

    /**
	 * Constructs a configuration structure from an encrypted XML file
	 * accessible through a file system dependent string representation of a
	 * path. This constructor needs a <code>Cipher</code> reference correctly
	 * initialized for the appropriate encoding scheme.
	 * 
	 * @param source a <code>String</code> representation of a system
	 *        dependant path pointing to a valid configuration representation.
	 * @param cipher an appropriately initialized instance of a
	 *        <code>Cipher</code> to be used to decrypt the document.
	 * @exception ConfigurationException if the file is not a valid XML file or
	 *            is no more accessible through the specified path.
	 */
    public XMLConfiguration(String source, Cipher cipher) throws ConfigurationException {
        this(new File(source), cipher);
    }

    /**
	 * Constructs a configuration structure from an encrypted XML stream of
	 * characters. This constructor needs a <code>Cipher</code> reference
	 * correctly initialized for the appropriate encoding scheme. The
	 * <code>isChanged</code> method returns <code>true</code> only if the
	 * <code>update</code> method was never called.
	 * 
	 * @param source a stream of characters representing a valid XML document.
	 * @param cipher an appropriately initialized instance of a
	 *        <code>Cipher</code> to be used to decrypt the document.
	 * @exception ConfigurationException if the stream doesn't represent a valid
	 *            XML document or if an IO error were generated while accessing
	 *            the stream.
	 */
    public XMLConfiguration(URL source, Cipher cipher) throws ConfigurationException {
        this.cipher = cipher;
        this.source = source;
        this.update();
    }

    /**
	 * @see net.smartlab.config.Configuration#isChanged()
	 */
    public boolean isChanged() throws ConfigurationException {
        try {
            if (source.openConnection().getLastModified() > access) {
                return true;
            } else {
                return false;
            }
        } catch (IOException ioe) {
            throw new ConfigurationException(ioe);
        }
    }

    /**
	 * @see net.smartlab.config.Configuration#update()
	 */
    public void update() throws ConfigurationException {
        try {
            this.access = System.currentTimeMillis();
            this.parent = null;
            this.children.clear();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            if (cipher != null) {
                factory.setValidating(true);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", true);
                factory.newSAXParser().parse(new CipherInputStream(source.openStream(), cipher), new XMLHandler(this));
            } else {
                factory.setValidating(true);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", true);
                factory.newSAXParser().parse(source.openStream(), new XMLConfiguration.XMLHandler(this));
            }
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    /**
	 * SAX Handler implementation to provide the in-memory representation of an
	 * XML configuration file.
	 * 
	 * @author Roberto Lo Giacco <rlogiacco@smartlab.net>
	 */
    protected static class XMLHandler extends DefaultHandler {

        /**
		 * The owning element also known as the parent element.
		 */
        protected Element owner;

        /**
		 * The document locator.
		 */
        protected Locator locator;

        /**
		 * The XML stream reader.
		 */
        protected XMLReader reader;

        /**
		 * Constructs the handler starting from the configuration root element.
		 * 
		 * @param owner
		 */
        public XMLHandler(Configuration owner) {
            this.owner = owner;
        }

        /**
		 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
		 */
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        /**
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
		 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
        public void startElement(String namespace, String prefix, String name, Attributes attributes) {
            try {
                if (owner instanceof Configuration && owner.parent == null) {
                    ((Configuration) owner).init(name, attributes);
                } else {
                    Element child = null;
                    if (attributes.getIndex(Reference.REFERENCE) > -1) {
                        child = new Reference((Node) owner, name, attributes.getValue(Reference.REFERENCE));
                    } else {
                        child = new Node((Node) owner, name, attributes);
                    }
                    ((Node) owner).children.add(child);
                    owner = child;
                }
            } catch (ClassCastException cce) {
                throw new RuntimeException("Reference elements cannot have a body");
            }
        }

        /**
		 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
		 */
        public void characters(char[] chars, int start, int length) {
            try {
                ((Node) owner).setContent(new String(chars, start, length));
            } catch (ClassCastException cce) {
                throw new RuntimeException("Reference elements cannot have contents");
            }
        }

        /**
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
		 *      java.lang.String, java.lang.String)
		 */
        public void endElement(String namespace, String prefix, String name) {
            owner = owner.parent;
        }

        /**
		 * Blocks external entity resolution failures.
		 * 
		 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,
		 *      java.lang.String)
		 */
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
            try {
                InputSource source = super.resolveEntity(publicId, systemId);
                if (source == null) {
                    return new InputSource(new StringReader(""));
                }
                return source;
            } catch (IOException ioe) {
                throw new SAXException(ioe);
            }
        }
    }

    /**
	 * TODO documentation
	 * 
	 * @param plain
	 * @param symKey
	 * @param ciphered
	 * @param algorithm
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 */
    public static void encrypt(File plain, File symKey, File ciphered, String algorithm) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        Key key = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(symKey));
            key = (Key) in.readObject();
        } catch (IOException ioe) {
            KeyGenerator generator = KeyGenerator.getInstance(algorithm);
            key = generator.generateKey();
            ObjectOutputStream out = new ObjectOutputStream(new java.io.FileOutputStream(symKey));
            out.writeObject(key);
            out.close();
        }
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getEncoded(), algorithm));
        FileInputStream in = new FileInputStream(plain);
        CipherOutputStream out = new CipherOutputStream(new FileOutputStream(ciphered), cipher);
        byte[] buffer = new byte[4096];
        for (int read = in.read(buffer); read > -1; read = in.read(buffer)) {
            out.write(buffer, 0, read);
        }
        out.close();
    }
}
