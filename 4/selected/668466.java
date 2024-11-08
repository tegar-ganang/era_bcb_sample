package us.ExcellentIngenuity.Software_Smith;

import java.io.*;
import java.nio.channels.FileChannel;

public class File_Copy {

    protected String FileName;

    protected String SourcePath;

    protected String DestinationPath;

    protected File FileSource;

    protected File FileDestination;

    public String Get_FileName() {
        return FileName;
    }

    public void Set_FileName(String FN) {
        FileName = FN;
    }

    public String Get_SourcePath() {
        return SourcePath;
    }

    public void Set_SourcePath(String SP) {
        SourcePath = SP;
    }

    public String Get_DestianationPath() {
        return DestinationPath;
    }

    public void Set_DestinationPath(String DP) {
        DestinationPath = DP;
    }

    public void Ini(String FN, String SP, String DP) {
        Set_FileName(FN);
        Set_SourcePath(SP);
        Set_DestinationPath(DP);
        FileSource = new File(SourcePath + FileName);
        FileDestination = new File(DestinationPath + FileName);
    }

    public void Copy() throws IOException {
        if (!FileDestination.exists()) {
            FileDestination.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(FileSource).getChannel();
            destination = new FileOutputStream(FileDestination).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }
}
