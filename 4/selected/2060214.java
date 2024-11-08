package org.fudaa.dodico.fortran;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import org.fudaa.ctulu.CtuluLibString;

/**
 * Permet d'ecrire un fichier csv. Ne prend pas en compte le fait que les string peuvent contenir
 * des caract�res sp�ciaux
 * @author Fred Deniger
 * @version $Id: CsvWriter.java,v 1.2 2004-12-31 14:13:37 jm_lacombe Exp $
 */
public class CsvWriter {

    ByteBuffer buf_;

    FileOutputStream out_;

    CharBuffer charBuf_;

    FileChannel ch_;

    boolean firstCol = true;

    String lineSep = CtuluLibString.LINE_SEP;

    char sepChar_ = ' ';

    CharsetEncoder encoder_;

    /**
   * @param _f le fichier de destination
   * @throws IOException
   */
    public CsvWriter(File _f) throws IOException {
        out_ = new FileOutputStream(_f, false);
        ch_ = out_.getChannel();
        buf_ = ByteBuffer.allocate(8192);
        charBuf_ = CharBuffer.allocate(8192);
        encoder_ = Charset.forName(System.getProperty("file.encoding")).newEncoder();
    }

    /**
   * @param _s la chaine a ajouter
   * @param _addSepChar true si doit ajouter le sep de car
   * @throws IOException
   */
    private void appendString(String _s, boolean _addSepChar) throws IOException {
        boolean addSep = _addSepChar && !firstCol;
        int size = addSep ? _s.length() + 1 : _s.length();
        if (charBuf_.remaining() <= size) writeBuffer();
        if (addSep) charBuf_.put(sepChar_);
        charBuf_.put(_s);
        firstCol = false;
    }

    private void writeBuffer() throws IOException {
        charBuf_.flip();
        encoder_.encode(charBuf_, buf_, true);
        buf_.rewind();
        ch_.write(buf_);
        buf_.rewind();
        charBuf_.clear();
    }

    /**
   * @param _d le double a ajouter dans le champs.
   * @throws IOException
   */
    public void appendDoule(double _d) throws IOException {
        appendString(Double.toString(_d), true);
    }

    /**
   * @param _s la chaine a ajoute dans le champs
   * @throws IOException
   */
    public void appendString(String _s) throws IOException {
        appendString(_s, true);
    }

    /**
   * Ferme le flux
   * @throws IOException
   */
    public void close() throws IOException {
        writeBuffer();
        out_.close();
        ch_.close();
    }

    /**
   * @return le separateur de ligne
   */
    public final String getLineSep() {
        return lineSep;
    }

    /**
   * @return le separateur de champs
   */
    public final char getSepChar() {
        return sepChar_;
    }

    /**
   * Retour a la ligne.
   * @throws IOException
   */
    public void newLine() throws IOException {
        appendString(lineSep, false);
        firstCol = true;
    }

    /**
   * @param _lineSep le separateur de ligne
   */
    public final void setLineSep(String _lineSep) {
        lineSep = _lineSep;
    }

    /**
   * @param _sepChar
   */
    public final void setSepChar(char _sepChar) {
        sepChar_ = _sepChar;
    }
}
