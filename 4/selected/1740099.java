package gruntspud;

import gruntspud.connection.ConnectionProfile;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.netbeans.lib.cvsclient.file.DefaultTransmitTextFilePreprocessor;

public class GruntspudTransmitTextFilePreprocessor extends DefaultTransmitTextFilePreprocessor {

    private static final int CHUNK_SIZE = 32768;

    public GruntspudTransmitTextFilePreprocessor(ConnectionProfile profile) {
        this.profile = profile;
    }

    public String getSeparatorSequence() {
        switch(profile.getLineEndings()) {
            case ConnectionProfile.UNIX_LINE_ENDINGS:
                return "\n";
            case ConnectionProfile.WINDOWS_LINE_ENDINGS:
                return "\r\n";
            case ConnectionProfile.IGNORE_LINE_ENDINGS:
                return "";
            default:
                return System.getProperty("line.separator");
        }
    }

    private String debugSequence(String seq) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < seq.length(); i++) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(Integer.toHexString(seq.charAt(i)));
        }
        return buf.toString();
    }

    public void cleanup(File preprocessedTextFile) {
        Constants.CVS_LOG.debug("Cleaning up " + preprocessedTextFile.getAbsolutePath());
        if (preprocessedTextFile != null) {
            preprocessedTextFile.delete();
        }
    }

    public File getPreprocessedTextFile(File originalTextFile) throws IOException {
        File preprocessedTextFile = File.createTempFile("cvs", null);
        String separatorSeq = getSeparatorSequence();
        byte[] newLine = separatorSeq.getBytes();
        Constants.CVS_LOG.debug("Preprocessing " + originalTextFile.getAbsolutePath() + " to " + preprocessedTextFile.getAbsolutePath() + " using " + debugSequence(separatorSeq));
        byte[] crlf = "\r\n".getBytes();
        byte[] lf = "\n".getBytes();
        OutputStream out = null;
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(originalTextFile));
            out = new BufferedOutputStream(new FileOutputStream(preprocessedTextFile));
            byte[] fileChunk = new byte[CHUNK_SIZE];
            byte[] fileWriteChunk = new byte[CHUNK_SIZE];
            for (int readLength = in.read(fileChunk); readLength > 0; readLength = in.read(fileChunk)) {
                if (newLine.length == 0) {
                    out.write(fileChunk, 0, readLength);
                } else {
                    int writeLength = 0;
                    for (int i = 0; i < readLength; ) {
                        int pos = findIndexOf(fileChunk, crlf, i);
                        int lineSepLength = crlf.length;
                        if (pos < i || pos >= readLength) {
                            pos = findIndexOf(fileChunk, lf, i);
                            lineSepLength = lf.length;
                        }
                        if (pos >= i && pos < readLength) {
                            try {
                                System.arraycopy(fileChunk, i, fileWriteChunk, writeLength, pos - i);
                            } catch (ArrayIndexOutOfBoundsException aiobe) {
                                Constants.CVS_LOG.error("fileChunk.length=" + fileChunk.length + " i=" + i + " writeLength=" + writeLength + " pos=" + pos + " fileWriteChunk.length=" + fileWriteChunk.length);
                                throw aiobe;
                            }
                            writeLength += pos - i;
                            i = pos + lineSepLength;
                            for (int j = 0; j < newLine.length; j++) fileWriteChunk[writeLength++] = newLine[j];
                        } else {
                            System.arraycopy(fileChunk, i, fileWriteChunk, writeLength, readLength - i);
                            writeLength += readLength - i;
                            i = readLength;
                        }
                    }
                    out.write(fileWriteChunk, 0, writeLength);
                }
            }
            return preprocessedTextFile;
        } catch (IOException ex) {
            if (preprocessedTextFile != null) {
                cleanup(preprocessedTextFile);
            }
            throw ex;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private static int findIndexOf(byte[] array, byte[] pattern, int start) {
        int subPosition = 0;
        for (int i = start; i < array.length; i++) {
            if (array[i] == pattern[subPosition]) {
                if (++subPosition == pattern.length) {
                    return i - subPosition + 1;
                }
            } else {
                subPosition = 0;
            }
        }
        return -1;
    }

    private ConnectionProfile profile;
}
