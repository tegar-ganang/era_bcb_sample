package edu.clemson.cs.nestbed.server.nesc.weaver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.clemson.cs.nestbed.server.util.FileUtils;

public class MakefileWeaver {

    private static final Log log = LogFactory.getLog(MakefileWeaver.class);

    private File makefile;

    private File makefileBackup;

    private List<String> linesAdded = new ArrayList<String>();

    public MakefileWeaver(File makefile) throws FileNotFoundException, Exception {
        this.makefile = makefile;
        this.makefileBackup = new File(makefile + ".orig");
        FileUtils.copyFile(makefile, makefileBackup);
    }

    public void addLine(String line) {
        linesAdded.add(line);
    }

    public void regenerateMakefile() throws FileNotFoundException, IOException {
        boolean append = true;
        PrintWriter out = new PrintWriter(new FileWriter(makefile, append));
        for (String i : linesAdded) {
            out.println(i);
        }
        out.close();
    }
}
