package org.exist.security;

import org.exist.security.internal.SecurityManagerImpl;
import org.exist.security.internal.RealmImpl;
import java.io.IOException;
import org.exist.storage.io.VariableByteInput;
import java.util.Random;
import org.exist.storage.io.VariableByteOutputStream;
import org.exist.util.SyntaxException;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import org.easymock.classextension.EasyMock;
import static org.easymock.classextension.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

/**
 *
 * @author Adam Retter <adam@exist-db.org>
 */
public class UnixStylePermissionTest {

    @Test
    public void writeRead_roundtrip() throws IOException {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random().nextInt();
        final int mode = 0700;
        final int ownerGroupId = new Random().nextInt();
        final VariableByteOutputStream mockOstream = EasyMock.createMock(VariableByteOutputStream.class);
        final VariableByteInput mockIstream = EasyMock.createMock(VariableByteInput.class);
        TestableUnixStylePermission permission = new TestableUnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, mode);
        final long permissionVector = permission.getVector_testable();
        mockOstream.writeLong(permissionVector);
        expect(mockIstream.readLong()).andReturn(permissionVector);
        replay(mockSecurityManager, mockOstream, mockIstream);
        permission.write(mockOstream);
        permission.read(mockIstream);
        verify(mockSecurityManager, mockOstream, mockIstream);
        assertEquals(permissionVector, permission.getVector_testable());
    }

    /**
     * Tests that if we are the owner of a resource, and that resource has read permission
     * granted to the owner, then we can read the resource
     */
    @Test
    public void validate_can_read_WhenOwnerWithRead() {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        final int mode = 0700;
        final int ownerGroupId = new Random().nextInt(SecurityManagerImpl.MAX_GROUP_ID);
        final boolean hasDbaRole = false;
        final Subject mockUser = EasyMock.createMock(Subject.class);
        expect(mockUser.hasDbaRole()).andReturn(hasDbaRole);
        expect(mockUser.getId()).andReturn(ownerId);
        replay(mockSecurityManager, mockUser);
        Permission permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, mode);
        boolean isValid = permission.validate(mockUser, Permission.READ);
        verify(mockSecurityManager, mockUser);
        assertTrue(isValid);
    }

    /**
     * Tests that if we are the owner of a resource, and that resource
     * does not have read permission granted to the owner,
     * then we cannot read the resource
     */
    @Test
    public void validate_cant_read_WhenOwnerWithoutRead() {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        final int mode = 0077;
        final int ownerGroupId = new Random(SecurityManagerImpl.MAX_GROUP_ID).nextInt();
        final boolean hasDbaRole = false;
        final Subject mockUser = EasyMock.createMock(Subject.class);
        expect(mockUser.hasDbaRole()).andReturn(hasDbaRole);
        expect(mockUser.getId()).andReturn(ownerId);
        replay(mockSecurityManager, mockUser);
        Permission permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, mode);
        boolean isValid = permission.validate(mockUser, Permission.READ);
        verify(mockSecurityManager, mockUser);
        assertFalse(isValid);
    }

    /**
     * Tests that if we are the owner of a resource, and that resource
     * does not have read permission granted to the owner,
     * then we cannot read the resource even if the owner
     * is in a group that has read permission on the resource
     */
    @Test
    public void validate_cant_read_WhenOwnerWithoutRead_and_OwnerInGroupWithRead() {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        final int mode = 0077;
        final int ownerGroupId = new Random().nextInt(SecurityManagerImpl.MAX_GROUP_ID);
        final boolean hasDbaRole = false;
        final Subject mockUser = EasyMock.createMock(Subject.class);
        expect(mockUser.hasDbaRole()).andReturn(hasDbaRole);
        expect(mockUser.getId()).andReturn(ownerId);
        replay(mockSecurityManager, mockUser);
        Permission permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, mode);
        boolean isValid = permission.validate(mockUser, Permission.READ);
        verify(mockSecurityManager, mockUser);
        assertFalse(isValid);
    }

    /**
     * Tests that if we are not the owner of a resource
     * but are in the group for the resource and that group
     * has read permission,
     * then we cannot read the resource even if other(s)
     * have read permission on the resource
     */
    @Test
    public void validate_cant_read_WhenNotOwner_and_InGroupWithoutRead_and_OtherCanRead() {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        final int mode = 0007;
        final int ownerGroupId = new Random().nextInt(SecurityManagerImpl.MAX_GROUP_ID);
        final boolean hasDbaRole = false;
        final Subject mockUser = EasyMock.createMock(Subject.class);
        final int userId = new Random(SecurityManagerImpl.MAX_GROUP_ID).nextInt();
        expect(mockUser.hasDbaRole()).andReturn(hasDbaRole);
        expect(mockUser.getId()).andReturn(userId);
        expect(mockUser.getGroupIds()).andReturn(new int[] { ownerGroupId });
        replay(mockSecurityManager, mockUser);
        Permission permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, mode);
        boolean isValid = permission.validate(mockUser, Permission.READ);
        verify(mockSecurityManager, mockUser);
        assertFalse(isValid);
    }

    /**
     * Tests that if we are not the owner of a resource
     * but are in the group for the resource and that group
     * has read permission,
     * then we cannot read the resource even if other(s)
     * have read permission on the resource
     */
    @Test
    public void validate_can_read_WhenNotOwner_and_InGroupWithRead() {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        final int mode = 0077;
        final int ownerGroupId = new Random().nextInt(SecurityManagerImpl.MAX_GROUP_ID);
        final boolean hasDbaRole = false;
        final Subject mockUser = EasyMock.createMock(Subject.class);
        final int userId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        expect(mockUser.hasDbaRole()).andReturn(hasDbaRole);
        expect(mockUser.getId()).andReturn(userId);
        expect(mockUser.getGroupIds()).andReturn(new int[] { ownerGroupId });
        replay(mockSecurityManager, mockUser);
        Permission permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, mode);
        boolean isValid = permission.validate(mockUser, Permission.READ);
        verify(mockSecurityManager, mockUser);
        assertTrue(isValid);
    }

    @Test
    public void validate_can_read_WhenNotOwner_and_NotInGroup_and_OtherWithRead() {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random(SecurityManagerImpl.MAX_USER_ID).nextInt();
        final int mode = 0007;
        final int ownerGroupId = new Random().nextInt(SecurityManagerImpl.MAX_GROUP_ID);
        final boolean hasDbaRole = false;
        final Subject mockUser = EasyMock.createMock(Subject.class);
        final int userId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        expect(mockUser.hasDbaRole()).andReturn(hasDbaRole);
        expect(mockUser.getId()).andReturn(userId);
        expect(mockUser.getGroupIds()).andReturn(new int[] { new Random().nextInt(SecurityManagerImpl.MAX_GROUP_ID) });
        replay(mockSecurityManager, mockUser);
        Permission permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, mode);
        boolean isValid = permission.validate(mockUser, Permission.READ);
        verify(mockSecurityManager, mockUser);
        assertTrue(isValid);
    }

    @Test
    public void validate_cant_write_WhenNotOwner_and_NotInGroup_and_OtherWithoutWrite() {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = RealmImpl.SYSTEM_ACCOUNT_ID;
        final int mode = 0001;
        final int ownerGroupId = RealmImpl.DBA_GROUP_ID;
        final boolean hasDbaRole = false;
        final Subject mockUser = EasyMock.createMock(Subject.class);
        final int userId = RealmImpl.GUEST_ACCOUNT_ID;
        final int userGroupIds[] = new int[] { RealmImpl.GUEST_GROUP_ID };
        expect(mockUser.hasDbaRole()).andReturn(hasDbaRole);
        expect(mockUser.getId()).andReturn(userId);
        expect(mockUser.getGroupIds()).andReturn(userGroupIds);
        replay(mockSecurityManager, mockUser);
        Permission permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, mode);
        boolean isValid = permission.validate(mockUser, Permission.WRITE);
        verify(mockSecurityManager, mockUser);
        assertFalse(isValid);
    }

    @Test
    public void permission_toString() {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        final int ownerGroupId = new Random().nextInt(SecurityManagerImpl.MAX_GROUP_ID);
        Permission permission = permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 07777);
        assertEquals("rwsrwsrwt", permission.toString());
        permission = permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 04777);
        assertEquals("rwsrwxrwx", permission.toString());
        permission = permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 02777);
        assertEquals("rwxrwsrwx", permission.toString());
        permission = permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 01777);
        assertEquals("rwxrwxrwt", permission.toString());
        permission = permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 07000);
        assertEquals("--s--s--t", permission.toString());
        permission = permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 04000);
        assertEquals("--s------", permission.toString());
        permission = permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 02000);
        assertEquals("-----s---", permission.toString());
        permission = permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 01000);
        assertEquals("--------t", permission.toString());
        permission = permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 0777);
        assertEquals("rwxrwxrwx", permission.toString());
        permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 0770);
        assertEquals("rwxrwx---", permission.toString());
        permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 0700);
        assertEquals("rwx------", permission.toString());
        permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 0070);
        assertEquals("---rwx---", permission.toString());
        permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 0007);
        assertEquals("------rwx", permission.toString());
        permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 0744);
        assertEquals("rwxr--r--", permission.toString());
        permission = new UnixStylePermission(mockSecurityManager, ownerId, ownerGroupId, 0740);
        assertEquals("rwxr-----", permission.toString());
    }

    @Test
    public void permission_setFromModeString_existSymbolic() throws SyntaxException, PermissionDeniedException {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        final int ownerGroupId = new Random().nextInt(SecurityManagerImpl.MAX_GROUP_ID);
        Permission permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0);
        permission.setMode("user=+read,+write,-execute");
        assertEquals(06, permission.getOwnerMode());
        permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0);
        permission.setMode("user=+execute,group=+execute,other=+execute");
        assertEquals(0111, permission.getMode());
        permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0666);
        permission.setMode("user=+execute,group=+execute,other=+execute");
        assertEquals(0777, permission.getMode());
        permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0777);
        permission.setMode("user=-read,-write,-execute,group=-read,-write,-execute,other=-read,-write,-execute");
        assertEquals(0, permission.getMode());
    }

    @Test
    public void permission_setFromModeString_unixSymbolic() throws SyntaxException, PermissionDeniedException {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        final int ownerGroupId = new Random().nextInt(SecurityManagerImpl.MAX_GROUP_ID);
        Permission permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0);
        permission.setMode("u+rw-x");
        assertEquals(06, permission.getOwnerMode());
        permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0);
        permission.setMode("u+x,g+x,o+x");
        assertEquals(0111, permission.getMode());
        permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0666);
        permission.setMode("u+x,g+x,o+x");
        assertEquals(0777, permission.getMode());
        permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0777);
        permission.setMode("u-rwx,g-rwx,o-rwx");
        assertEquals(0, permission.getMode());
    }

    @Test
    public void permission_setFromModeString_simpleSymbolic() throws SyntaxException, PermissionDeniedException {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        final int ownerId = new Random().nextInt(SecurityManagerImpl.MAX_USER_ID);
        final int ownerGroupId = new Random().nextInt(SecurityManagerImpl.MAX_GROUP_ID);
        Permission permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0);
        permission.setMode("rw-------");
        assertEquals(06, permission.getOwnerMode());
        permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0);
        permission.setMode("rwxrwxrwx");
        assertEquals(0777, permission.getMode());
        permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0666);
        permission.setMode("--x--x--x");
        assertEquals(0111, permission.getMode());
        permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0777);
        permission.setMode("--r--r--r");
        assertEquals(0444, permission.getMode());
        permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, ownerId, ownerGroupId, 0777);
        permission.setMode("---------");
        assertEquals(0, permission.getMode());
    }

    @Test
    public void permission_setMode_roundtrip() throws PermissionDeniedException {
        final SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        Permission permission = permission = new TestableUnixStylePermissionWithCurrentSubject(mockSecurityManager, 1, 1, 0);
        for (int mode = 0; mode <= 07777; mode++) {
            permission.setMode(mode);
            assertEquals(mode, permission.getMode());
        }
    }

    public class TestableUnixStylePermission extends UnixStylePermission {

        public TestableUnixStylePermission(SecurityManager sm, int ownerId, int ownerGroupId, int mode) {
            super(sm, ownerId, ownerGroupId, mode);
        }

        public long getVector_testable() {
            return super.getVector();
        }

        public long encodeAsBitVector_testable(int userId, int groupId, int mode) {
            return super.encodeAsBitVector(userId, groupId, mode);
        }
    }

    public class TestableUnixStylePermissionWithCurrentSubject extends UnixStylePermission {

        public TestableUnixStylePermissionWithCurrentSubject(SecurityManager sm, int ownerId, int ownerGroupId, int mode) {
            super(sm, ownerId, ownerGroupId, mode);
        }

        @Override
        public boolean isCurrentSubjectDBA() {
            return true;
        }

        @Override
        public boolean isCurrentSubjectInGroup() {
            return true;
        }

        @Override
        public boolean isCurrentSubjectOwner() {
            return true;
        }
    }
}
