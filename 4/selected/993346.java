package org.unitils.core.util;

import static org.junit.Assert.*;
import org.junit.Test;
import org.unitils.UnitilsJUnit4;
import org.unitils.core.UnitilsException;
import org.unitils.inject.annotation.TestedObject;
import org.unitils.thirdparty.org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Fabian Krueger
 *
 *         Test for {@link PropertiesReader}
 */
public class PropertiesReaderTest extends UnitilsJUnit4 {

    /**
     * System under Test
     */
    @TestedObject
    private PropertiesReader sut = new PropertiesReader();

    private static final String TEST_FILE = "propertiesReaderTest.properties";

    @Test
    public void loadPropertiesFileFromUserHome_withNullArgument_shouldThrowUnitilsException() {
        String configurationFile = null;
        String expectedMessage = "Unable to load configuration file: " + configurationFile + " from user home";
        try {
            sut.loadPropertiesFileFromUserHome(configurationFile);
            fail("UnitilsExcepton expected");
        } catch (UnitilsException ue) {
            assertEquals(expectedMessage, ue.getMessage());
        }
    }

    @Test
    public void loadPropertiesFileFromUserHome_withEmptyStringFile_shouldThrowUnitilsException() {
        String configurationFile = "";
        String expectedMessage = "Unable to load configuration file: " + configurationFile + " from user home";
        try {
            sut.loadPropertiesFileFromUserHome(configurationFile);
            fail("UnitilsExcepton expected");
        } catch (UnitilsException ue) {
            assertEquals(expectedMessage, ue.getMessage());
        }
    }

    @Test
    public void loadPropertiesFileFromUserHome_withMissingFile_shouldReturnNull() {
        String configurationFile = "nofilefound.foo";
        Properties returnedProperties = sut.loadPropertiesFileFromUserHome(configurationFile);
        assertNull(returnedProperties);
    }

    @Test
    public void loadPropertiesFileFromUserHome_withExistingFile_shouldReturnProperties() throws IOException {
        copyDummyPropertiesFileToUserHome();
        Properties returnedProperties = sut.loadPropertiesFileFromUserHome(TEST_FILE);
        assertNotNull(returnedProperties);
        assertEquals("some value", returnedProperties.getProperty("testprop"));
        deleteDummyPropertiesFileFromUserHome();
    }

    @Test
    public void loadPropertiesFileFromClasspath_withNullArgument_shouldThrowUnitilsException() {
        String configurationFile = null;
        String expectedMessage = "Unable to load configuration file: " + configurationFile;
        try {
            sut.loadPropertiesFileFromClasspath(configurationFile);
            fail("UnitilsExcepton expected");
        } catch (UnitilsException ue) {
            assertEquals(expectedMessage, ue.getMessage());
        }
    }

    @Test
    public void loadPropertiesFileFromClasspath_withEmptyStringFile_shouldThrowUnitilsException() {
        String configurationFile = "";
        String expectedMessage = "Unable to load configuration file: " + configurationFile;
        try {
            sut.loadPropertiesFileFromClasspath(configurationFile);
            fail("UnitilsExcepton expected");
        } catch (UnitilsException ue) {
            assertEquals(expectedMessage, ue.getMessage());
        } catch (Exception e) {
            fail("UnitilsExcepton expected");
        }
    }

    @Test
    public void loadPropertiesFileFromClasspath_withMissingFile_shouldReturnNull() {
        String configurationFile = "nofilefound.foo";
        Properties returnedProperties;
        returnedProperties = sut.loadPropertiesFileFromClasspath(configurationFile);
        assertNull(returnedProperties);
    }

    @Test
    public void loadPropertiesFileFromClasspath_withExistingFile_shouldReturnProperties() throws IOException {
        Properties returnedProperties = sut.loadPropertiesFileFromClasspath(TEST_FILE);
        assertNotNull(returnedProperties);
        assertEquals("some value", returnedProperties.getProperty("testprop"));
    }

    private void copyDummyPropertiesFileToUserHome() throws IOException {
        String userHome = System.getProperty("user.home");
        String classPath = this.getClass().getClassLoader().getResource(".").getPath();
        File fileToCopy = new File(classPath + "/" + TEST_FILE);
        FileUtils.copyFileToDirectory(fileToCopy, new File(userHome));
        File copiedFile = new File(userHome + "/" + TEST_FILE);
        assertTrue("File " + TEST_FILE + " should be in user home.", copiedFile.exists());
    }

    private void deleteDummyPropertiesFileFromUserHome() {
        String userHome = System.getProperty("user.home");
        File targetFile = new File(userHome + "/" + TEST_FILE);
        targetFile.delete();
        assertFalse("File " + TEST_FILE + " should be deleted from user home.", targetFile.exists());
    }
}
