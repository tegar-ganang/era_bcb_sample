package neembuu.vfs.test.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import neembuu.common.RangeArray;
import neembuu.common.RangeArrayElement;

/**
 *
 * @author Shashank Tulsyan
 */
public final class PartialFileCreator {

    private final RangeArray<RangeArrayElement> array;

    private final File destinationDirectory;

    private final File sourceFile;

    public PartialFileCreator(RangeArray<RangeArrayElement> array, File destinationDirectory, File sourceFile) {
        this.array = array;
        this.destinationDirectory = destinationDirectory;
        this.sourceFile = sourceFile;
    }

    public void create() throws IOException {
        FileChannel fc = new FileInputStream(sourceFile).getChannel();
        for (RangeArrayElement element : array) {
            FileChannel fc_ = fc.position(element.starting());
            File part = new File(destinationDirectory, "_0x" + Long.toHexString(element.starting()) + ".partial");
            FileChannel partfc = new FileOutputStream(part).getChannel();
            partfc.transferFrom(fc_, 0, element.getSize());
            partfc.force(true);
            partfc.close();
        }
        fc.close();
    }
}
