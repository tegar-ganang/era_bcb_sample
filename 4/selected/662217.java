package org.nightlabs.jfire.servermanager.config;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import org.nightlabs.config.ConfigModule;
import org.nightlabs.jdo.ObjectIDUtil;
import org.nightlabs.jfire.idgenerator.IDGenerator;
import org.nightlabs.jfire.organisation.Organisation;
import org.nightlabs.jfire.organisation.id.OrganisationID;
import org.nightlabs.jfire.person.Person;
import org.nightlabs.jfire.person.PersonStruct;
import org.nightlabs.jfire.prop.IStruct;
import org.nightlabs.jfire.prop.PropertySet;
import org.nightlabs.jfire.prop.StructLocal;
import org.nightlabs.jfire.server.Server;

/**
 * @author marco
 * @author Marc Klinger - marc[at]nightlabs[dot]de
 */
public class OrganisationCf implements Serializable, Comparable<OrganisationCf>, Cloneable {

    /**
	 * The serial version of this class.
	 */
    private static final long serialVersionUID = 1L;

    public static final String DATASOURCE_PREFIX_RELATIVE = "jfire/datasource/";

    public static final String DATASOURCE_PREFIX_ABSOLUTE = "java:/jfire/datasource/";

    public static final String PERSISTENCE_MANAGER_FACTORY_PREFIX_RELATIVE = "jfire/persistenceManagerFactory/";

    public static final String PERSISTENCE_MANAGER_FACTORY_PREFIX_ABSOLUTE = "java:/jfire/persistenceManagerFactory/";

    private String organisationID;

    private String organisationName;

    private Set<String> serverAdmins = null;

    private boolean readOnly = false;

    /**
	 * The parent config module. This is only set if a config module
	 * owns this instance.
	 */
    private ConfigModule parentConfigModule = null;

    public OrganisationCf() {
    }

    public OrganisationCf(String _organisationID, String _organisationName) {
        this.setOrganisationID(_organisationID);
        this.setOrganisationName(_organisationName);
    }

    /**
	 * @return Returns the organisationID.
	 */
    public String getOrganisationID() {
        return organisationID;
    }

    /**
	 * @param organisationID The organisationID to set.
	 */
    public void setOrganisationID(String _organisationID) {
        assertWritable();
        if (_organisationID == null) throw new NullPointerException("organisationID must not be null!");
        if (!"".equals(_organisationID) && !ObjectIDUtil.isValidIDString(_organisationID)) throw new IllegalArgumentException("organisationID \"" + _organisationID + "\" is not a valid id!");
        this.organisationID = _organisationID;
        setChanged();
    }

    /**
	 * @return Returns the organisationName.
	 */
    public String getOrganisationName() {
        return organisationName;
    }

    /**
	 * @param organisationName The organisationName to set.
	 */
    public void setOrganisationName(String _organisationName) {
        assertWritable();
        if (_organisationName == null) throw new NullPointerException("organisationName must not be null!");
        this.organisationName = _organisationName;
        setChanged();
    }

    /**
	 * If this instance is set readOnly, this method will return a copy of
	 * Set serverAdmins. Later, it won't return a copy, but a readonly Set
	 * that throws exceptions when write methods are executed.
	 * <br/><br/>
	 * Anyway, you should never use the returned Set
	 * for write accesses! Use addServerAdmin(...) and removeServerAdmin(...)
	 * for manipulations.
	 * <br/><br/>
	 * Note that this method will return <code>null</code> if no serverAdmins
	 * are existent. It will never return an empty Set as it is nulled if it
	 * becomes empty.
	 *
	 * @return Returns the serverAdmins.
	 * @see addServerAdmin(String userID)
	 * @see removeServerAdmin(String userID)
	 */
    public Set<String> getServerAdmins() {
        if (readOnly && serverAdmins != null) return new HashSet<String>(serverAdmins);
        return serverAdmins;
    }

    /**
	 * After having set a new list of serverAdmins, don't manipulate
	 * the Set directly anymore! Use the methods addServerAdmin(...) and
	 * removeServerAdmin(...) instead!
	 *
	 * @param serverAdmins The serverAdmins to set.
	 * @see addServerAdmin(String userID)
	 * @see removeServerAdmin(String userID)
	 */
    public void setServerAdmins(Set<String> _serverAdmins) {
        assertWritable();
        this.serverAdmins = _serverAdmins;
        setChanged();
    }

    public void addServerAdmin(String userID) {
        assertWritable();
        if (serverAdmins == null) serverAdmins = new HashSet<String>();
        serverAdmins.add(userID);
        setChanged();
    }

    public boolean removeServerAdmin(String userID) {
        assertWritable();
        if (serverAdmins == null) return false;
        boolean res = serverAdmins.remove(userID);
        if (serverAdmins.size() == 0) serverAdmins = null;
        setChanged();
        return res;
    }

    public boolean isServerAdmin(String userID) {
        if (serverAdmins == null) return false;
        return serverAdmins.contains(userID);
    }

    public void makeReadOnly() {
        readOnly = true;
    }

    public void assertWritable() {
        if (readOnly) throw new IllegalStateException("This instance of OrganisationCf does not allow write!");
    }

    /**
	 * This method creates a JDO Organisation object with the given persistenceManager
	 * in case it does not yet exist.
	 *
	 * @param pm The PersistenceManager in which's datastore the Organisation should be
	 * 	created.
	 */
    public Organisation createOrganisation(PersistenceManager pm, Server server) {
        pm.getExtent(Organisation.class, true);
        Organisation organisation;
        try {
            organisation = (Organisation) pm.getObjectById(OrganisationID.create(getOrganisationID()), true);
        } catch (JDOObjectNotFoundException x) {
            organisation = new Organisation(getOrganisationID());
            organisation.setServer(server);
            pm.makePersistent(organisation);
        }
        if (organisation.getPerson() == null) {
            Person person = new Person(organisationID, IDGenerator.nextID(PropertySet.class));
            PersonStruct.getPersonStruct(getOrganisationID(), pm);
            IStruct structLocal = StructLocal.getStructLocal(Person.class, Person.STRUCT_SCOPE, Person.STRUCT_LOCAL_SCOPE, pm);
            person.inflate(structLocal);
            try {
                person.getDataField(PersonStruct.PERSONALDATA_COMPANY).setData(organisationName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            person.setDisplayName(organisationName);
            person.setAutoGenerateDisplayName(true);
            person.deflate();
            organisation.setPerson(person);
        }
        return organisation;
    }

    /**
	 * @see java.lang.Object#clone()
	 */
    @Override
    public Object clone() {
        OrganisationCf n;
        try {
            n = (OrganisationCf) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        if (this.serverAdmins != null) n.serverAdmins = new HashSet<String>(this.serverAdmins);
        return n;
    }

    public int compareTo(OrganisationCf other) {
        if (this.organisationID == null) {
            if (other.organisationID == null) return 0; else return -1;
        }
        if (other.organisationID == null) return 1;
        return this.organisationID.compareTo(other.organisationID);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        return this.toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    protected String thisString = null;

    @Override
    public String toString() {
        if (thisString == null) {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getClass().getName());
            sb.append('{');
            sb.append(organisationID);
            sb.append(',');
            sb.append(organisationName);
            sb.append(",serverAdmins{");
            if (serverAdmins != null) {
                for (Iterator<String> it = serverAdmins.iterator(); it.hasNext(); ) {
                    String userID = it.next();
                    sb.append(userID);
                    if (it.hasNext()) sb.append(',');
                }
            }
            sb.append("}}");
            thisString = sb.toString();
        }
        return thisString;
    }

    /**
	 * Get the parentConfigModule.
	 * @return the parentConfigModule
	 */
    public ConfigModule getParentConfigModule() {
        return parentConfigModule;
    }

    /**
	 * Set the parentConfigModule.
	 * @param parentConfigModule the parentConfigModule to set
	 */
    public void setParentConfigModule(ConfigModule parentConfigModule) {
        this.parentConfigModule = parentConfigModule;
    }

    public void setChanged() {
        if (parentConfigModule != null) parentConfigModule.setChanged();
        thisString = null;
    }
}
