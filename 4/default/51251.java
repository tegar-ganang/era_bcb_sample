import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 * 当遇到有人拿gbk做工程代码的默认编码
 * 自己又不会拿脚本转的时候可以用
 * lol
 * 
 * @author kabbesy@smth
 *
 */
public class IConv {

    private String fromEncoding;

    private String toEncoding;

    /**
	 * @param fromEncoding 原始编码
	 * @param toEncoding 新编码
	 */
    public IConv(String fromEncoding, String toEncoding) {
        this.fromEncoding = fromEncoding;
        this.toEncoding = toEncoding;
    }

    /**
	 * 像IConv一样转化编码
	 * 
	 * @param file
	 * @throws IOException
	 */
    public void covertFile(File file) throws IOException {
        if (!file.isFile()) {
            return;
        }
        Reader reader = null;
        OutputStream os = null;
        File newfile = null;
        String filename = file.getName();
        boolean succeed = false;
        try {
            newfile = new File(file.getParentFile(), filename + ".bak");
            reader = new InputStreamReader(new FileInputStream(file), fromEncoding);
            os = new FileOutputStream(newfile);
            IOUtils.copy(reader, os, toEncoding);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Encoding error for file [" + file.getAbsolutePath() + "]");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            file.delete();
            succeed = newfile.renameTo(file);
        } catch (Exception e) {
            throw new IOException("Clear bak error for file [" + file.getAbsolutePath() + "]");
        }
        if (succeed) {
            System.out.println("Changed encoding for file [" + file.getAbsolutePath() + "]");
        }
    }

    /**
	 * 转化目录下所有文件
	 * 广度优先便于查错
	 * 
	 * @param rootdir 待转化的根目录
	 */
    public void convertDirectory(File rootdir) {
        File[] child = rootdir.listFiles();
        for (int i = 0; i < child.length; i++) {
            List<File> childDir = new ArrayList<File>();
            try {
                if (child[i].isFile()) {
                    covertFile(child[i]);
                } else {
                    childDir.add(child[i]);
                }
                for (File file : childDir) {
                    convertDirectory(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new IConv("GBK", "utf-8").convertDirectory(new File("C:\\src"));
    }
}
