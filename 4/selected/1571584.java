package app.controllers;

import org.javalite.activeweb.AppController;
import java.io.IOException;
import static org.javalite.common.Util.readResourceBytes;

/**
 * @author Igor Polevoy
 */
public class DownloadController extends AppController {

    public void index() throws IOException {
        outputStream("application/pdf").write(readResourceBytes("/test.pdf"));
    }
}
