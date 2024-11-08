package com.extentech.toolkit;

import java.io.*;
import java.nio.channels.FileChannel;

/** File utilities.

 */
public class JFileWriter {

    java.lang.String path = "", filename = "", data = "";

    byte newLine = Character.LINE_SEPARATOR;

    public void setPath(String p) {
        path = p;
    }

    public void setFileName(String f) {
        filename = f;
    }

    public void setData(String d) {
        data = d;
    }

    void printErr(String err) {
        Logger.logInfo("Error in JFileWriter: " + err);
        Logger.logWarn("Error in JFileWriter: " + err);
    }

    /** append text to the end of a text file
    */
    public static final synchronized void appendToFile(String pth, String text) {
        try {
            byte[] bbuf = text.getBytes("UTF-8");
            File outp = new File(pth);
            if (!outp.exists()) {
                outp.mkdirs();
                outp.delete();
                outp = new java.io.File(pth);
            }
            RandomAccessFile outputFile = new RandomAccessFile(outp, "rw");
            outputFile.skipBytes((int) outputFile.length());
            int strt = 0;
            if (outp.exists()) strt = (int) outputFile.length();
            outputFile.write(bbuf, 0, (int) bbuf.length);
            outputFile.close();
        } catch (Exception e) {
            Logger.logInfo("JFileWriter.appendToFile() IO Error : " + e.toString());
        }
    }

    /** write the inputstream contents to file
	 * 
	 * 
	 * @author John McMahon [ Jan 22, 2008 ]
	 * @param is
	 * @param file
	 */
    public static void writeToFile(InputStream is, File file) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            is.close();
            out.close();
        } catch (IOException e) {
            Logger.logErr("JFileWriter.writeToFile failed.", e);
        }
    }

    public boolean writeIt() {
        try {
            path += filename;
            StringReader SR = new StringReader(data);
            File outputFile = new File(path);
            FileWriter out = new FileWriter(outputFile);
            int c;
            if (outputFile.length() > 0) {
                return false;
            }
            while ((c = SR.read()) != -1) out.write(c);
            out.flush();
            out.close();
        } catch (IOException e) {
            Logger.logInfo("JFileWriter IO Error : " + e.toString());
        }
        return true;
    }

    public boolean writeIt(String data, String filename, String path) {
        try {
            path += filename;
            StringReader SR = new StringReader(data);
            File outputFile = new File(path);
            FileWriter out = new FileWriter(outputFile);
            int c;
            if (outputFile.length() > 0) {
                return false;
            }
            while ((c = SR.read()) != -1) {
                out.write(c);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            Logger.logInfo("JFileWriter IO Error : " + e.toString());
        }
        return true;
    }

    public String readFile(String fname) {
        StringBuffer addTxt = new StringBuffer();
        try {
            BufferedReader d = new BufferedReader(new FileReader(fname));
            while (d.ready()) addTxt.append(d.readLine());
            d.close();
        } catch (Exception e) {
            printErr("problem reading file: " + e);
        }
        return addTxt.toString();
    }

    public static void copyFile(String infile, String outfile) throws FileNotFoundException, IOException {
        File fx = new File(infile);
        copyFile(fx, outfile);
    }

    /** Copy method, using FileChannel#transferTo 
	 * NOTE:  will overwrite existing files
	 * @param File source
	 * @param File target
	 * @throws IOException
	 * */
    public static void copyFile(File source, String target) throws FileNotFoundException, IOException {
        File fout = new File(target);
        fout.mkdirs();
        fout.delete();
        fout = new File(target);
        FileChannel in = new FileInputStream(source).getChannel();
        FileChannel out = new FileOutputStream(target).getChannel();
        in.transferTo(0, in.size(), out);
        in.close();
        out.close();
    }

    public void writeLine(String file, String line) {
        String s;
        try {
            File f = new File(file);
            FileWriter out = new FileWriter(f);
            DataInputStream inStream = new DataInputStream(new StringBufferInputStream(line));
            while ((s = inStream.readLine()) != null) {
                out.write(s);
                out.write(newLine);
            }
            out.close();
        } catch (FileNotFoundException e) {
            printErr(e.toString());
        } catch (Exception e) {
            printErr(e.toString());
        }
    }

    public void writeLogToFile(String fname, javax.swing.JTextArea jta) {
        try {
            OutFile n = new OutFile(fname);
            String logText = jta.getText();
            n.writeBytes(logText);
            jta.setText("");
            n.close();
        } catch (FileNotFoundException e) {
            printErr(e.toString());
        } catch (IOException e) {
            printErr(e.toString());
        }
    }

    public String readLog(String logFname) {
        String addTxt = "";
        try {
            InFile n = new InFile(logFname);
            while (n.available() != 0) {
                addTxt += n.readLine();
            }
        } catch (FileNotFoundException e) {
            printErr(e.toString());
        } catch (IOException e) {
            printErr(e.toString());
        }
        return addTxt += "\r\n";
    }
}
