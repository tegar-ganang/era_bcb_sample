package org.jpedal.fonts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.jpedal.utils.LogWriter;

/** heights of non-CID fonts are currently approximated. This class provides
 * sensible height figures from a look-up table.
 */
public class PdfHeightTable {

    /**stores default list of character heights for speed*/
    private int[][] default_height_lookup = new int[200][256];

    /**loader to load data from jar*/
    private ClassLoader loader = this.getClass().getClassLoader();

    /**must use windows encoding because files were edited on Windows*/
    private final String enc = "Cp1252";

    /**
	 * read lookup tables from disk which include set of
	 * fix heights to approximate height of each glyph
	 */
    public PdfHeightTable() {
        BufferedReader input_stream = null;
        String line = null;
        int pointer1, pointer2, value;
        int key1, key2;
        try {
            input_stream = new BufferedReader(new InputStreamReader(loader.getResourceAsStream("org/jpedal/res/pdf/font_heights.cfg"), enc));
            if (input_stream == null) {
                LogWriter.writeLog("Unable to open font_heights.cfg from jar");
            }
            while (true) {
                line = input_stream.readLine();
                if (line == null) break;
                pointer1 = line.indexOf("-");
                pointer2 = line.indexOf(":");
                key1 = Integer.parseInt(line.substring(0, pointer1).trim());
                key2 = Integer.parseInt(line.substring(pointer1 + 1, pointer2).trim());
                value = Integer.parseInt(line.substring(pointer2 + 1, line.length()).trim());
                default_height_lookup[key1][key2] = value;
            }
        } catch (Exception ee) {
            LogWriter.writeLog("Exception " + ee + " reading line from height table");
        }
        if (input_stream != null) {
            try {
                input_stream.close();
            } catch (Exception e) {
                LogWriter.writeLog("Exception " + e + " reading lookup table for pdf  for abobe map");
            }
        }
    }

    /**
	 *get height of characters using our lookup table of approximations
	 */
    public final float getCharHeight(int char_number, int current_font_size) {
        if (current_font_size < 0) current_font_size = -current_font_size;
        float height = 0;
        if ((current_font_size < 200) & (char_number < 256)) height = default_height_lookup[current_font_size][char_number];
        return height;
    }
}
