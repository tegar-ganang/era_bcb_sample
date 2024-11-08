import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.imageio.ImageIO;
import tiiger.hash.JenkinsHash;

public class HashTest {

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("C:\\Documents and Settings\\roccetti\\Documenti\\Immagini\\Foto\\Da sistemare\\2007_12_31\\MVI_2511.AVI");
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        long readInitTime = System.currentTimeMillis();
        int read = -1;
        while ((read = fis.read()) != -1) {
            bais.write(read);
        }
        bais.write(-1);
        byte[] bytes = bais.toByteArray();
        long readEndTime = System.currentTimeMillis();
        System.out.println("ReadTime (ms): " + (readEndTime - readInitTime));
        JenkinsHash hasher = new JenkinsHash();
        for (int i = 0; i < 10; i++) {
            long initTime = System.currentTimeMillis();
            long hash = hasher.hash(bytes);
            long endTime = System.currentTimeMillis();
            System.out.println("Hash: " + hash);
            System.out.println("HashTime (ms): " + (endTime - initTime));
        }
    }
}
