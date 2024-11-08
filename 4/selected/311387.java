package bestidioms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public final class TestForResource {

    public static boolean testForResource() {
        final byte[][] result = new byte[1][];
        new ForResource<ByteArrayOutputStream>(new ByteArrayOutputStream()) {

            public void work(ByteArrayOutputStream out) throws IOException {
                out.write(1);
                out.write(2);
                result[0] = out.toByteArray();
            }
        };
        return result[0][0] == 1 && result[0][1] == 2;
    }

    public static boolean testWith2Resources() {
        final byte[] original = { 1, 10, 100 };
        new ForResource<ByteArrayInputStream>(new ByteArrayInputStream(original)) {

            public void work(final ByteArrayInputStream in) {
                new ForResource<ByteArrayOutputStream>(new ByteArrayOutputStream()) {

                    public void work(final ByteArrayOutputStream out) {
                        for (int a = 0; a < 3; a++) out.write(in.read());
                        if (!Arrays.equals(original, out.toByteArray())) throw null;
                    }
                };
            }
        };
        return true;
    }
}
