package se.mdh.mrtc.saveccm.custom.generation.uppaalExport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileHandler {

    private File uppaalFile;

    public static void main(String[] args) {
        new FileHandler(new File("C:\\Documents and Settings\\Aurelien\\Bureau\\System.save"));
    }

    public FileHandler(File uppaalFile) {
        this.uppaalFile = uppaalFile;
        replaceCharacters();
    }

    private String load() throws IOException {
        FileChannel channel = new FileInputStream(uppaalFile).getChannel();
        try {
            ByteBuffer b = ByteBuffer.allocate((int) channel.size());
            channel.read(b);
            return new String(b.array());
        } finally {
            channel.close();
        }
    }

    public void replaceCharacters() {
        try {
            save(replaceSpecialCharacter(load()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String replaceSpecialCharacter(String fileContent) {
        return fileContent.replaceAll("%a", "&").replaceAll("&gt;", ">").replaceAll("%lt;", "&lt;").replaceAll("%gt;", "&gt;");
    }

    private void save(String fileContent) throws IOException {
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(uppaalFile));
        printWriter.print(fileContent);
        printWriter.flush();
        printWriter.close();
    }
}
