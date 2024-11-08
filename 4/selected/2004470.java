package org.exist.xmldb;

import junit.textui.TestRunner;
import org.exist.security.Permission;
import org.exist.security.PermissionFactory;
import org.exist.storage.DBBroker;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;

/** A test case for accessing user management service remotely ? 
 * @author Sebastian Bossung, Technische Universitaet Hamburg-Harburg
 * @author Pierrick Brihaye <pierrick.brihaye@free.fr>
 */
public class RemoteDatabaseImplTest extends RemoteDBTest {

    protected static final String ADMIN_PASSWORD = "somepwd";

    protected static final String ADMIN_COLLECTION_NAME = "admin-collection";

    public RemoteDatabaseImplTest(String name) {
        super(name);
    }

    protected void setUp() {
        try {
            initServer();
            setUpRemoteDatabase();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testGetCollection() {
        try {
            Class<?> cl = Class.forName(DB_DRIVER);
            Database database = (Database) cl.newInstance();
            DatabaseManager.registerDatabase(database);
            Collection rootCollection = DatabaseManager.getCollection(URI + DBBroker.ROOT_COLLECTION, "admin", "");
            CollectionManagementService cms = (CollectionManagementService) rootCollection.getService("CollectionManagementService", "1.0");
            Collection adminCollection = cms.createCollection(ADMIN_COLLECTION_NAME);
            UserManagementService ums = (UserManagementService) rootCollection.getService("UserManagementService", "1.0");
            if (ums != null) {
                Permission p = ums.getPermissions(adminCollection);
                p.setMode(Permission.USER_STRING + "=+read,+write," + Permission.GROUP_STRING + "=-read,-write," + Permission.OTHER_STRING + "=-read,-write");
                ums.setPermissions(adminCollection, p);
                Collection guestCollection = DatabaseManager.getCollection(URI + DBBroker.ROOT_COLLECTION + "/" + ADMIN_COLLECTION_NAME, "guest", "guest");
                Resource resource = guestCollection.createResource("testguest", "BinaryResource");
                resource.setContent("123".getBytes());
                try {
                    guestCollection.storeResource(resource);
                    fail();
                } catch (XMLDBException e) {
                }
                cms.removeCollection(ADMIN_COLLECTION_NAME);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public static void main(String[] args) {
        TestRunner.run(RemoteDatabaseImplTest.class);
        System.exit(0);
    }
}
