package org.chessworks.chess.services.file;

import java.io.File;
import java.util.Collection;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.chessworks.chess.model.User;

public class TestFileUserService extends TestCase {

    private static final File USERS_FILE = new File("src/test/resources/org/chessworks/chess/services/file/Users.txt");

    private FileUserService service;

    private FileUserService service2;

    private File usersFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        usersFile = File.createTempFile("Users.", ".txt");
        FileUtils.copyFile(USERS_FILE, usersFile);
        usersFile.deleteOnExit();
        service = new FileUserService();
        service.setDataFile(usersFile);
        service2 = new FileUserService();
        service2.setDataFile(usersFile);
    }

    public void testFindUser() throws Exception {
        service.load();
        User duckstorm = service.findUser("DuckStorm");
        User testUser = service.findUser("TestUser1");
        assertEquals("Doug Bateman", duckstorm.getRealName());
        assertEquals("TestUser1", testUser.getRealName());
    }

    public void testFindAllKnownUsers() throws Exception {
        service.load();
        Collection<User> users = service.findAllKnownUsers();
        int size1 = users.size();
        assertTrue(size1 > 0);
        service.findUser("TestUser1");
        int size2 = users.size();
        assertEquals(size1 + 1, size2);
    }

    public void testFindRegisteredUsers() throws Exception {
        service.load();
        Collection<User> users = service.findRegisteredUsers();
        int size1 = users.size();
        assertTrue(size1 > 0);
        service.findUser("TestUser1");
        int size2 = users.size();
        assertEquals(size1, size2);
    }

    public void testRegister() throws Exception {
        service.load();
        Collection<User> users = service.findRegisteredUsers();
        int size1 = users.size();
        assertTrue(size1 > 0);
        User test = service.findUser("TestUser1");
        service.register(test);
        int size2 = users.size();
        assertEquals(size1 + 1, size2);
    }

    public void testLoad() throws Exception {
        service.load();
    }

    public void testSave() throws Exception {
        service.load();
        User user = service.findUser("TestUser1");
        Collection<User> all = service.findAllKnownUsers();
        Collection<User> registered = service.findRegisteredUsers();
        assertTrue(all.contains(user));
        assertFalse(registered.contains(user));
        service.register(user);
        service.save();
        service2.load();
        Collection<User> all2 = service.findAllKnownUsers();
        Collection<User> registered2 = service.findRegisteredUsers();
        assertTrue(all2.contains(user));
        assertTrue(registered2.contains(user));
    }

    public void testFindOrCreateRole() throws Exception {
    }

    public void testFindRole() throws Exception {
    }

    public void testFindUsersInRole() throws Exception {
    }

    public void testIsUserInRole() throws Exception {
    }

    public void testAddUserToRole() throws Exception {
    }
}
