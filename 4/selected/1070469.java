package net.maizegenetics.data;

import net.maizegenetics.*;
import net.maizegenetics.util.TasselFileFilter;
import net.maizegenetics.data.*;
import net.maizegenetics.reports.*;
import net.maizegenetics.analysis.*;
import pal.misc.*;
import pal.alignment.*;
import java.io.*;
import java.awt.*;
import pal.alignment.*;
import javax.swing.*;

/**
 * Title:        TASSEL
 * Description:  A java program to deal with diversity
 * Copyright:    Copyright (c) 2000
 * Company:      USDA-ARS/NCSU
 *
 * @author
 * @version 1.0
 */
public class GBFormat {

    AnnotationAlignment aa;

    String Org, Comments, Lineage;

    StringBuffer sb;

    QTPAnalyzerFrame theQTPAnalyzerFrame;

    int[] mulFileStart;

    int[] mulFileEnd;

    int mulQMCount;

    public GBFormat(QTPAnalyzerFrame Frame, AnnotationAlignment aa, String Org, String Lineage, String Comments, int mulQMCount) {
        this.theQTPAnalyzerFrame = Frame;
        this.aa = aa;
        this.Org = Org;
        this.Lineage = Lineage;
        this.Comments = Comments;
        this.mulQMCount = mulQMCount;
        sb = new StringBuffer(aa.getSequenceCount() * aa.getSiteCount() + aa.getSequenceCount() * 200 + 1000);
        mulFileStart = new int[25];
        mulFileEnd = new int[25];
        GBformatter(aa, Org, Comments);
    }

    public void GBformatter(AnnotationAlignment aa, String Org, String Comments) {
        int start, end, flag = 0, x, count = 0, s, index = -1;
        int Begin = 0, Finish = 0;
        String[] taxa = new String[aa.getSequenceCount()];
        for (x = 0; x < aa.getSiteCount(); x++) {
            for (s = 0; s < aa.getSequenceCount(); s++) {
                if (aa.getData(s, x) == '?') {
                    continue;
                } else {
                    break;
                }
            }
            if (s != aa.getSequenceCount()) {
                if (flag == 0) {
                    flag = 1;
                    index++;
                    mulFileStart[index] = x;
                }
                continue;
            } else {
                if (flag == 1) {
                    flag = 0;
                    mulFileEnd[index] = x - 1;
                }
                count++;
            }
        }
        mulFileEnd[index] = aa.getSiteCount() - 1;
        int p = 0;
        for (p = 0; p <= index; p++) {
            Begin = mulFileStart[p];
            Finish = mulFileEnd[p];
            while ((mulFileStart[p + 1] - mulFileEnd[p] < mulQMCount) && (p < index)) {
                Finish = mulFileEnd[p + 1];
                p++;
            }
            sb.append("  " + aa.getSequenceCount() + " " + (Finish - Begin + 1) + "\n");
            for (s = 0; s < aa.getSequenceCount(); s++) {
                taxa[s] = "";
                for (x = 0; x < Math.min(10, aa.getIdentifier(s).getName().length()); x++) {
                    if (aa.getIdentifier(s).getName().charAt(x) != '.') {
                        sb.append(aa.getIdentifier(s).getName().charAt(x));
                        taxa[s] = taxa[s] + aa.getIdentifier(s).getName().charAt(x);
                        continue;
                    }
                    break;
                }
                while (x < 10) {
                    x++;
                    sb.append(" ");
                }
                sb.append("     ");
                appendSites(s, Begin, Finish);
                sb.append("\n");
            }
            for (int i = 0; i < aa.getSequenceCount(); i++) {
                sb.append(">[org=" + Org + "] [Lineage=" + Lineage + "] [cultivar=" + taxa[i] + "] " + Comments + " cultivar " + taxa[i] + "\n");
            }
        }
        saveGenBankFile();
    }

    void appendSites(int s, int SiteStart, int SiteEnd) {
        int start = -1, end = -1, flag = 0, x, i;
        for (i = SiteStart; i <= SiteEnd; i++) {
            for (x = 0; x < aa.getSequenceCount(); x++) {
                if (aa.getData(x, i) == '-') {
                    continue;
                } else {
                    break;
                }
            }
            if (x == aa.getSequenceCount()) {
                continue;
            } else {
                break;
            }
        }
        if (i == SiteEnd + 1) {
            return;
        }
        for (i = SiteStart; i <= SiteEnd; i++) {
            if (aa.getData(s, i) == '?') {
                if (flag == 0) {
                    start = i;
                    end = -1;
                    flag = 1;
                } else if (i == SiteEnd) {
                    end = i;
                    for (int p = 0; p < (end - start + 1); p++) {
                        sb.append("-");
                    }
                }
            } else {
                if (flag == 1) {
                    end = i - 1;
                    for (int p = 0; p < (end - start + 1); p++) {
                        if (start == SiteStart || end == SiteEnd) {
                            sb.append("-");
                        } else {
                            sb.append("N");
                        }
                    }
                    start = -1;
                    flag = 0;
                    sb.append(aa.getData(s, i));
                } else {
                    sb.append(aa.getData(s, i));
                }
            }
        }
    }

    void saveGenBankFile() {
        JFileChooser fileChooser = null;
        String preferredDir = theQTPAnalyzerFrame.getSettings().saveDir;
        if (preferredDir != null) {
            fileChooser = new JFileChooser(preferredDir);
        }
        TasselFileFilter fileFilter = new TasselFileFilter();
        fileFilter.addExtension("phy");
        fileFilter.setDescription("Phylip Format");
        fileChooser.setFileFilter(fileFilter);
        int userChoice = fileChooser.showSaveDialog(theQTPAnalyzerFrame);
        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File aFile = fileChooser.getSelectedFile();
            boolean notUniquelyNamed = true;
            while (notUniquelyNamed) {
                if (aFile.exists()) {
                    int returnVal = JOptionPane.showOptionDialog(theQTPAnalyzerFrame, "This file already exists.  " + "Do you wish to overwrite it?", "Overwrite File Confirmation", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                    if (returnVal == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                    if (returnVal == JOptionPane.NO_OPTION) {
                        fileChooser.showSaveDialog(theQTPAnalyzerFrame);
                    }
                    if (returnVal == JOptionPane.YES_OPTION) {
                        notUniquelyNamed = false;
                        try {
                            DataOutputStream dos = new DataOutputStream(new FileOutputStream(aFile));
                            dos.writeBytes("" + sb);
                            dos.flush();
                            dos.close();
                        } catch (Exception ee) {
                            ee.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
