package javarequirementstracer;

import java.io.File;
import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Ronald Koster
 */
public class FileUtilsTest {

    @Test
    public void all() {
        final String text = "blabla etc blabla etc etc";
        final String filename1 = "target/file1.txt";
        FileUtils.writeFile(filename1, text);
        final String filename2 = "target/subdir/file1.txt";
        FileUtils.copyFile(new File(filename1), new File(filename2));
        String actual = FileUtils.readFileAsString(filename2);
        Assert.assertEquals(text, actual);
    }
}
