package se.mdh.mrtc.saveccm.custom.generation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.emf.ecore.xml.type.internal.RegEx;

public class IdGenerator {

    private Integer count;

    private File saveFile;

    public IdGenerator(File saveFile) {
        count = 0;
        this.saveFile = saveFile;
    }

    public void generation() {
        try {
            save(idGeneration(load()));
        } catch (IOException e) {
            System.out.println("ID generation failure");
            e.printStackTrace();
        }
    }

    private String load() throws IOException {
        FileChannel channel = new FileInputStream(saveFile).getChannel();
        try {
            ByteBuffer b = ByteBuffer.allocate((int) channel.size());
            channel.read(b);
            return new String(b.array());
        } finally {
            channel.close();
        }
    }

    private String idGeneration(String fileContent) {
        count = 0;
        String copy = fileContent;
        copy = copy.replaceAll("id=\"[0-9]*\"", "id=\"#Auto\"");
        while (copy.contains("#Auto")) {
            copy = copy.replaceFirst("#Auto", count.toString());
            count++;
        }
        return copy;
    }

    private void save(String contenuFichier) throws IOException {
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(saveFile));
        printWriter.print(contenuFichier);
        printWriter.flush();
        printWriter.close();
    }
}
