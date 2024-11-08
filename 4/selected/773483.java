package cmspider.utilities.executor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FilePipe extends PipeHandler {

    private InputStream input = null;

    private FileOutputStream output = null;

    public FilePipe(String pathname) {
        try {
            output = new FileOutputStream(new File(pathname));
        } catch (FileNotFoundException e) {
            System.out.println("Error initing file output stream: " + e.getMessage());
        }
    }

    @Override
    void startListen(InputStream is) {
        input = is;
        this.start();
    }

    @Override
    public void run() {
        try {
            int byteIn;
            while ((byteIn = input.read()) != -1) output.write(byteIn);
            if (output != null) output.close();
        } catch (IOException ioe) {
            System.out.println("Error during process pipe listening: " + ioe.getMessage());
        }
    }
}
