import java.io.*;

public class Filesplit {

    public BufferedReader bufread;

    public BufferedWriter bufwriter;

    File writefile;

    String filepath, filecontent, read;

    String readStr = "";

    String name = "data_temp2/data";

    public String readfile(String path, int interval) {
        try {
            filepath = path;
            File file = new File(filepath);
            FileReader fileread = new FileReader(file);
            bufread = new BufferedReader(fileread);
            int i = 0;
            int j = 0;
            while ((read = bufread.readLine()) != null) {
                readStr = readStr + read + "\n";
                i++;
                if (i == interval) {
                    j++;
                    String tname = "" + name + j;
                    writefile(tname, readStr, false);
                    System.out.println("wirte file:" + j);
                    readStr = "";
                    i = 0;
                }
            }
        } catch (Exception d) {
            System.out.println(d.getMessage());
        }
        return readStr;
    }

    public void writefile(String path, String content, boolean append) {
        try {
            boolean addStr = append;
            filepath = path;
            filecontent = content;
            writefile = new File(filepath);
            if (writefile.exists() == false) {
                writefile.createNewFile();
                writefile = new File(filepath);
            }
            FileWriter filewriter = new FileWriter(writefile, addStr);
            bufwriter = new BufferedWriter(filewriter);
            filewriter.append(filecontent);
            filewriter.flush();
            System.out.println("write file");
        } catch (Exception d) {
            d.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Filesplit file = new Filesplit();
        file.readfile("rivers.txt", 1000);
    }
}
