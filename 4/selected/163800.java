package tool;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Packer {

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("2 args reqired. [Input Folder Path] [Output Folder Path]");
            return;
        }
        packFiles("submap", args[0], args[1], false);
        packFiles("submaplen", args[0], args[1], true);
    }

    /**
	 * inputPath�ɑ��݂��Ă���Aext�ŏI���t�@�C���S�Ă�ǂݍ��݁AoutputPath�ɏ������݂܂�
	 * �������ރt�@�C�����́upack.�v+ext�ł�
	 * @param ext �S�Ă̓�̓t�@�C���̊g���q�A�܂��o�̓t�@�C���̊g���q
	 * @param inputPath ��̓t�@�C���̃p�X
	 * @param outputPath �o�̓t�@�C���̃p�X
	 * @param headerEnable �t�@�C���̐擪�Ƀt�@�C�������������ޏꍇ true
	 */
    private static void packFiles(String ext, String inputPath, String outputPath, boolean headerEnable) {
        File inputDir = new File(inputPath);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (headerEnable) {
            int header = 0;
            for (String filename : inputDir.list()) {
                if (filename.endsWith(ext)) {
                    header++;
                }
            }
            bos.write(header);
        }
        for (String filename : inputDir.list()) {
            if (filename.endsWith(ext)) {
                loadToBOS(bos, inputPath + "/" + filename);
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputPath + "/pack." + ext);
            fos.write(bos.toByteArray());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * �w�肳�ꂽByteArrayOutputStream�Ƀt�@�C���̒��g�������o���܂�
	 * @param bos �����o�����ByteArrayOutputStream
	 * @param path ��̓t�@�C���̃p�X
	 */
    private static void loadToBOS(ByteArrayOutputStream bos, String path) {
        FileInputStream fis = null;
        DataInputStream dis = null;
        try {
            fis = new FileInputStream(new File(path));
            dis = new DataInputStream(fis);
            for (; ; ) {
                bos.write(dis.readByte());
            }
        } catch (EOFException e) {
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
