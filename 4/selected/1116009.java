package equilibrium.commons.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils {

    public static void inputToOutput(InputStream input, OutputStream output) {
        try {
            byte[] byteBuffer = new byte[8192];
            int amount;
            while ((amount = input.read(byteBuffer)) >= 0) output.write(byteBuffer, 0, amount);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void outputToInput(ByteArrayOutputStream output, ByteArrayInputStream input) {
        byte[] outputByteArray = output.toByteArray();
        input = new ByteArrayInputStream(outputByteArray);
    }
}
