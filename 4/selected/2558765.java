package gr.demokritos.iit.jinsect.interoperability;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import gr.demokritos.iit.jinsect.structs.Correlation;

/**
 *
 * @author ggianna
 */
public class RInteroperator {

    /** Creates a new instance of RInteroperator. */
    public RInteroperator() throws IOException {
        Process p = Runtime.getRuntime().exec("R --help");
        try {
            p.waitFor();
        } catch (InterruptedException iee) {
            return;
        }
    }

    public Correlation getCorrelationBetween(String sFile, String sMeasure1, String sMeasure2) {
        Correlation cRes = new Correlation();
        try {
            File fTempFile = File.createTempFile("jinsect", "Rscript");
            PrintStream pOut = new PrintStream(fTempFile);
            pOut.println("Dataset <- read.table(\"" + sFile + "\", header=TRUE, sep=\"\", na.strings=\"NA\", dec=\".\", strip.white=TRUE)");
            pOut.println("cor(Dataset$" + sMeasure1 + ",Dataset$" + sMeasure2 + ", use = \"all.obs\", method = \"spearman\");");
            pOut.println("cor(Dataset$" + sMeasure1 + ",Dataset$" + sMeasure2 + ", use = \"all.obs\", method = \"pearson\");");
            pOut.flush();
            pOut.close();
            String[] saCmd = { "R", "--no-save", "--slave" };
            ProcessBuilder pbP = new ProcessBuilder(saCmd);
            Process p = pbP.start();
            InputStream isIn = p.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(isIn));
            OutputStream osOut = p.getOutputStream();
            PrintStream pR = new PrintStream(osOut);
            FileReader fr = new FileReader(fTempFile);
            int cChar;
            while ((cChar = fr.read()) != -1) pR.write(cChar);
            pR.flush();
            pR.close();
            osOut.close();
            String sStr = "";
            String sSpearman = "";
            String sPearson = "";
            while ((sStr = br.readLine()) != null) {
                if (sSpearman.length() == 0) {
                    if (sStr.indexOf("[1]") > -1) {
                        Iterator iIter = Arrays.asList(sStr.split("\\s")).iterator();
                        while (iIter.hasNext()) {
                            String sToken = (String) iIter.next();
                            if (sToken.matches("\\d+[.,]\\d+")) {
                                sSpearman = sToken;
                                cRes.Spearman = Double.valueOf(sSpearman);
                            }
                        }
                    }
                } else if (sPearson.length() == 0) {
                    if (sStr.indexOf("[1]") > -1) {
                        Iterator iIter = Arrays.asList(sStr.split("\\s")).iterator();
                        while (iIter.hasNext()) {
                            String sToken = (String) iIter.next();
                            if (sToken.matches("\\d+[.,]\\d+")) {
                                sPearson = sToken;
                                cRes.Pearson = Double.valueOf(sPearson);
                            }
                        }
                    }
                }
            }
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                System.err.println("Process was interrupted");
            }
            if (p.exitValue() != 0) System.err.println("Exit value was non-zero");
            br.close();
            isIn.close();
            fTempFile.delete();
            return cRes;
        } catch (IOException ioe) {
            System.out.println("Execution failed: " + ioe.getMessage());
            return cRes;
        }
    }

    public Correlation getCorrelationBetween(List l1, List l2, String sMethod) {
        Correlation cRes = new Correlation();
        try {
            File fTempFile = File.createTempFile("jinsect", "Rscript");
            PrintStream pOut = new PrintStream(fTempFile);
            StringBuffer sb1 = new StringBuffer();
            StringBuffer sb2 = new StringBuffer();
            Iterator iIter = l1.iterator();
            sb1.append("c(");
            while (iIter.hasNext()) {
                sb1.append(iIter.next().toString());
                if (iIter.hasNext()) sb1.append(",");
            }
            sb1.append(")");
            iIter = l2.iterator();
            sb2.append("c(");
            while (iIter.hasNext()) {
                sb2.append(iIter.next().toString());
                if (iIter.hasNext()) sb2.append(",");
            }
            sb2.append(")");
            pOut.println("L1<-" + sb1.toString() + ";");
            pOut.println("L2<-" + sb2.toString() + ";");
            pOut.println("cor(L1,L2, use = \"all.obs\", method = \"spearman\");");
            pOut.println("cor(L1,L2, use = \"all.obs\", method = \"pearson\");");
            pOut.flush();
            pOut.close();
            String[] saCmd = { "R", "--no-save", "--slave" };
            ProcessBuilder pbP = new ProcessBuilder(saCmd);
            Process p = pbP.start();
            InputStream isIn = p.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(isIn));
            OutputStream osOut = p.getOutputStream();
            PrintStream pR = new PrintStream(osOut);
            FileReader fr = new FileReader(fTempFile);
            int cChar;
            while ((cChar = fr.read()) != -1) pR.write(cChar);
            pR.flush();
            pR.close();
            osOut.close();
            String sStr = "";
            String sSpearman = "";
            String sPearson = "";
            while ((sStr = br.readLine()) != null) {
                if (sSpearman.length() == 0) {
                    if (sStr.indexOf("[1]") > -1) {
                        iIter = Arrays.asList(sStr.split("\\s")).iterator();
                        while (iIter.hasNext()) {
                            String sToken = (String) iIter.next();
                            if (sToken.matches("\\d+[.,]\\d+")) {
                                sSpearman = sToken;
                                cRes.Spearman = Double.valueOf(sSpearman);
                            }
                        }
                    }
                } else if (sPearson.length() == 0) {
                    if (sStr.indexOf("[1]") > -1) {
                        iIter = Arrays.asList(sStr.split("\\s")).iterator();
                        while (iIter.hasNext()) {
                            String sToken = (String) iIter.next();
                            if (sToken.matches("\\d+[.,]\\d+")) {
                                sPearson = sToken;
                                cRes.Pearson = Double.valueOf(sPearson);
                            }
                        }
                    }
                }
            }
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                System.err.println("Process was interrupted");
            }
            if (p.exitValue() != 0) System.err.println("Exit value was non-zero");
            br.close();
            isIn.close();
            fTempFile.delete();
            return cRes;
        } catch (IOException ioe) {
            System.out.println("Execution failed: " + ioe.getMessage());
            return cRes;
        }
    }
}
