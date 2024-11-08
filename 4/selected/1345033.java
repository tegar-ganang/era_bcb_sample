package neembuu.vfs.test.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * @author Shashank Tulsyan
 */
public final class VerifyDataIntegrity {

    final File dataDirectory;

    final File completeFile;

    public VerifyDataIntegrity(File dataDirectory, File completeFile) {
        this.dataDirectory = dataDirectory;
        this.completeFile = completeFile;
    }

    public final void verify() throws IOException {
        File[] files = dataDirectory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".partial");
            }
        });
        for (File f : files) {
            String nm = f.getName();
            long startingOffset = Long.parseLong(nm.substring(nm.indexOf("_0x") + 3, nm.lastIndexOf('.')), 16);
            FileChannel fc_ = new FileInputStream(f).getChannel();
            FileChannel fc = new FileInputStream(completeFile).getChannel().position(startingOffset);
            ByteBuffer buffer = ByteBuffer.allocate((int) fc_.size());
            ByteBuffer buffer_ = ByteBuffer.allocate((int) fc_.size());
            fc_.read(buffer_);
            fc.read(buffer);
            System.out.println("file=" + nm);
            BoundaryConditions.printContentPeek(buffer, buffer_);
            System.out.println("buffersize=" + buffer.capacity() + " filesize=" + f.length());
            assert (buffer.equals(buffer_));
            fc.close();
            fc_.close();
        }
    }

    public static void main(String[] args) throws Exception {
        VerifyDataIntegrity itegrity = new VerifyDataIntegrity(new File("j:\\neembuu\\heap\\test120k.http.rmvb_neembuu_download_data"), new File("j:\\neembuu\\realfiles\\test120k.rmvb"));
        itegrity.verify();
    }
}
