package nl.headspring.photoz.server.multithreadedserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Class EchoService.
 *
 * @author Eelco Sommer
 * @since Oct 9, 2010
 */
public class EchoService implements Service {

    public void service(InputStream inputStream, OutputStream outputStream) throws ServiceException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        PrintWriter writer = new PrintWriter(outputStream);
        try {
            writer.println(reader.readLine());
            writer.flush();
            writer.close();
            reader.close();
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }
}
