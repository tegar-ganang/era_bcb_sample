package uk101.hardware;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import uk101.Main;

/**
 * This is a system ROM loaded from a resource.
 *
 * @author Baldwin
 */
public class ROM extends Memory {

    private String name;

    public ROM(String id) throws IOException {
        name = new File(id).getName().toUpperCase();
        if (name.lastIndexOf('.') != -1) name = name.substring(0, name.lastIndexOf('.'));
        InputStream in = Main.class.getResourceAsStream("rom/" + id);
        if (in == null) {
            in = Main.class.getResourceAsStream("/" + id);
            if (in == null) {
                in = new FileInputStream(id);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int b = in.read(); b != -1; b = in.read()) out.write(b);
        in.close();
        setStore(out.toByteArray());
    }

    public void writeByte(int offset, byte b) {
        return;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "ROM" + super.toString() + ": " + name;
    }
}
