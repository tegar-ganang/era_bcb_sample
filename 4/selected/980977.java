package ast.common.data;

import ast.XControllerDataClass;
import ast.common.PropertyHandler;
import java.util.Collection;

/**
 * Role with read and write permissions of an user.
 *
 * @author Chrissyx
 */
public class Role implements Comparable<String>, XControllerDataClass {

    /**
     * Define read access to Post-it comments.
     *
     * @see ast.notes.PostItController
     */
    private boolean readComments;

    /**
     * Define read access to the document.
     */
    private boolean readDocument;

    /**
     * Define write access to Post-it comments.
     *
     * @see ast.notes.PostItController
     */
    private boolean writeComments;

    /**
     * Define write access to the document.
     */
    private boolean writeDocument;

    /**
     * Name of the role.
     */
    private String name;

    /**
     * Constructor, sets the role permissions and name.
     *
     * @param bRC Permission for reading comments
     * @param bRD Permission for reading the document
     * @param bWC Permission for writing comments
     * @param bWD Permission for editing the document
     * @param sName Role name
     */
    public Role(final boolean bRC, final boolean bRD, final boolean bWC, final boolean bWD, final String sName) {
        this.readComments = bRC;
        this.readDocument = bRD;
        this.writeComments = bWC;
        this.writeDocument = bWD;
        this.name = sName;
    }

    /**
     * Returns permission for reading comments.
     *
     * @return Read comments permission
     */
    public boolean isReadComments() {
        return this.readComments;
    }

    /**
     * Returns permission for reading the document.
     *
     * @return Read document permission
     */
    public boolean isReadDocument() {
        return this.readDocument;
    }

    /**
     * Returns permission for writing comments.
     *
     * @return Write comments permission
     */
    public boolean isWriteComments() {
        return this.writeComments;
    }

    /**
     * Returns permission for editing the document.
     *
     * @return Edit document permission
     */
    public boolean isWriteDocument() {
        return this.writeDocument;
    }

    /**
     * Returns the name of the role.
     *
     * @return Role name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the role permissions.
     *
     * @param bRC Permission for reading comments
     * @param bRD Permission for reading the document
     * @param bWC Permission for writing comments
     * @param bWD Permission for editing the document
     */
    public void setRights(final boolean bRC, final boolean bRD, final boolean bWC, final boolean bWD) {
        this.readComments = bRC;
        this.readDocument = bRD;
        this.writeComments = bWC;
        this.writeDocument = bWD;
    }

    /**
     * Sets the name of the role.
     *
     * @param sName Role name
     */
    public void setName(final String sName) {
        this.name = sName;
    }

    /**
     * {@inheritDoc}
     *
     * @param sName Name to compare with this role
     * @return Natural ordering regarding the names
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final String sName) {
        return this.name.compareTo(sName);
    }

    /**
     * {@inheritDoc}
     *
     * @see ast.XControllerDataClass#fromString(java.lang.String[], java.util.Collection)
     */
    @Override
    public Role fromString(final String[] sAttributes, final Collection<XControllerDataClass> aCurCollection) {
        return new Role(Boolean.valueOf(sAttributes[0]), Boolean.valueOf(sAttributes[1]), Boolean.valueOf(sAttributes[2]), Boolean.valueOf(sAttributes[3]), sAttributes[4]);
    }

    /**
     * {@inheritDoc}
     *
     * @see ast.XControllerDataClass#toString()
     */
    @Override
    public String toString() {
        return this.readComments + PropertyHandler.SEPARATOR + this.readDocument + PropertyHandler.SEPARATOR + this.writeComments + PropertyHandler.SEPARATOR + this.writeDocument + PropertyHandler.SEPARATOR + this.name;
    }
}
