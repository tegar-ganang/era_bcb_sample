package org.cyberaide.gridshell2.junit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.cyberaide.gridshell2.util.GridFileTools;
import org.globus.cog.abstraction.impl.file.GridFileImpl;
import org.globus.cog.abstraction.impl.file.PermissionsImpl;
import org.globus.cog.abstraction.interfaces.GridFile;
import org.globus.cog.abstraction.interfaces.Permissions;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class GridFileToolsTests {

    private GridFile file;

    /** Build a Permissions-Object from three booleans */
    private Permissions getPermissions(boolean read, boolean write, boolean exec) {
        Permissions result = new PermissionsImpl();
        result.setRead(read);
        result.setWrite(write);
        result.setExecute(exec);
        return result;
    }

    /** Recreate the file-object for each testrun */
    @Before
    public void beforeTest() {
        file = new GridFileImpl();
        file.setUserPermissions(getPermissions(true, true, true));
        file.setGroupPermissions(getPermissions(true, false, true));
        file.setAllPermissions(getPermissions(false, false, false));
    }

    /** Test uf the Permission-String is formatted correctly like in ls */
    @Test
    public void testPermissionsStringFile() {
        file.setFileType(file.FILE);
        String permissionString = GridFileTools.getPermissionsAsString(file);
        assertEquals(permissionString, "-rwxr-x---");
    }

    /** Test if the modification time is formatted correctly */
    @Test
    public void testModificationString() {
        Calendar date = (new GregorianCalendar(2000, Calendar.FEBRUARY, 1, 0, 50, 59));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        file.setLastModified(formatter.format(date.getTime()));
        assertEquals("2000-02-01 00:50", GridFileTools.getFileModificationTime(file));
    }

    /** Checks if unknown formats are unmodified.
     * 
     * Background: If GridFileTools detects an unknown format it should leave
     *             it unchanged.
     */
    @Test
    public void testWrongModificationString() {
        String str = "c8e3af338f5e2";
        file.setLastModified(str);
        assertEquals(str, GridFileTools.getFileModificationTime(file));
    }

    /** Checks if more tricky formats are unmodified.
     * 
     * Background: If GridFileTools detects an unknown format it should leave
     *             it unchanged.
     */
    @Test
    public void testTrickyModificationStrings() {
        String str = "yyyyMMddHHmmss";
        file.setLastModified(str);
        assertEquals(str, GridFileTools.getFileModificationTime(file));
        file.setLastModified("4");
        assertEquals("4", GridFileTools.getFileModificationTime(file));
    }

    /** Test if the String with the FileSize is correct */
    @Test
    public void testFileSizeString() {
        long size = 1024 * 1024;
        file.setSize(size);
        assertEquals("1M", GridFileTools.getFileSize(file, true));
        assertEquals(String.valueOf(size) + " ", GridFileTools.getFileSize(file, false));
    }
}
