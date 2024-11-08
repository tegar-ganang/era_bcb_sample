package jp.ne.nifty.iga.midori.shell.util.dir;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import jp.ne.nifty.iga.midori.shell.util.*;
import jp.ne.nifty.iga.midori.shell.dir.*;
import jp.ne.nifty.iga.midori.shell.eng.*;
import jp.ne.nifty.iga.midori.shell.io.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.Collator;

/**
 * Dir Class.
 * <br>
 * $Revision: 1.3 $
 */
public class Dir {

    /**
	 * list files in this directory.
	 */
    public int process(MdShellCommandThread commandThread, String[] strArgs) {
        MdShellEnv shellenv = commandThread.getEnv();
        PrintWriter writer = new PrintWriter(commandThread.getOut());
        Vector vecList = buildData(shellenv, strArgs);
        if (vecList == null) {
            return -2;
        }
        showData(shellenv, vecList, writer);
        return 0;
    }

    private boolean isRecursive = false;

    private boolean isSortByTimeStamp = false;

    private boolean isSortByFileSize = false;

    public Vector buildData(MdShellEnv shellenv, String[] strArgs) {
        PrintWriter writer = new PrintWriter(shellenv.getOut());
        LongOpt[] longOptions = { new LongOpt("help", LongOpt.NO_ARGUMENT, null, '?'), new LongOpt("recursive", LongOpt.NO_ARGUMENT, null, 'R'), new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'V') };
        Getopt g = new Getopt("dir", strArgs, "ailrRStV?", longOptions);
        int c;
        while ((c = g.getopt()) != -1) {
            switch(c) {
                case 'R':
                    isRecursive = true;
                    break;
                case 'S':
                    isSortByFileSize = true;
                    break;
                case 't':
                    isSortByTimeStamp = true;
                    break;
                case 'V':
                    break;
                case '?':
                    BufferedReader reader = new BufferedReader(new InputStreamReader((Dir.class).getResourceAsStream("DirUsage.txt")));
                    try {
                        String strLine = null;
                        while ((strLine = reader.readLine()) != null) {
                            writer.println(strLine);
                            writer.flush();
                        }
                        reader.close();
                    } catch (IOException ex) {
                        System.out.println(ex.toString());
                        ex.printStackTrace();
                    }
                    return null;
            }
        }
        String strFileRegExp = "";
        {
            int optind = g.getOptind();
            if (optind < strArgs.length) {
                strFileRegExp = MdShellUtilString.eraseChar(strArgs[g.getOptind()], '*');
            }
        }
        Vector vecList = null;
        if (isRecursive) {
            vecList = shellenv.getCurrentDirectory().listDirectoryWithSubDirectory(shellenv, MdShellDirNodeInfo.SELECT_DIRECTORY_FILE, strFileRegExp);
        } else {
            vecList = shellenv.getCurrentDirectory().listDirectory(shellenv, MdShellDirNodeInfo.SELECT_DIRECTORY_FILE, strFileRegExp);
        }
        if (isSortByTimeStamp) {
            Collections.sort(vecList, new MdShellFileTimestampComparator());
        }
        if (isSortByFileSize) {
            Collections.sort(vecList, new MdShellFileSizeComparator());
        }
        return vecList;
    }

    public void showData(MdShellEnv shellenv, Vector vecList, PrintWriter writer) {
        int iBaseLength = shellenv.getCurrentDirectory().getName().length();
        MdShellDirLongListFormatter formatter = new MdShellDirLongListFormatter();
        for (int index = 0; index < vecList.size(); index++) {
            MdShellDirNodeInterface path = (MdShellDirNodeInterface) vecList.elementAt(index);
            writer.println(formatter.format(path));
            writer.flush();
        }
        NumberFormat numberfmt = NumberFormat.getNumberInstance();
        writer.println("     total: " + numberfmt.format(formatter.iTotalDirectoryCount) + " directories: " + numberfmt.format(formatter.iTotalFileCount) + " files: total " + numberfmt.format(formatter.iTotalFileLength) + " bytes");
        writer.flush();
    }
}
