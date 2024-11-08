package org.roguelikedevelopment.data.mapdata;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

/**
 * This is the parser for "puzzle map sections" that are used in dweller
 * to build maps. The class reads a plain text file and parses map pieces
 * one by one together with a frequency value for each one. Format of file is
 * 
 * 7 (width)
 * 7 (height)
 * #######
 * #######
 * #######
 * #######
 * #######
 * #######
 * #######
 * 1 (frequency)
 * (empty line)
 * ###..##
 * ###..##
 * ####..#
 * ###...#
 * ##..###
 * ##..###
 * ##..###
 * 10
 * 
 * 
 * @author Bj�rn
 *
 */
public class MapDataGenerator {

    /**
	 * Parses a text file with map pieces and writes a file with a binary representation
	 * of the text file
	 * @param infile
	 * @param outfile
	 * @throws IOException
	 */
    public static final void parse(String infile, String outfile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(infile));
        DataOutputStream output = new DataOutputStream(new FileOutputStream(outfile));
        int w = Integer.parseInt(reader.readLine());
        int h = Integer.parseInt(reader.readLine());
        output.writeByte(w);
        output.writeByte(h);
        int lineCount = 2;
        try {
            do {
                for (int i = 0; i < h; i++) {
                    lineCount++;
                    String line = reader.readLine();
                    if (line == null) {
                        throw new RuntimeException("Unexpected end of file at line " + lineCount);
                    }
                    for (int j = 0; j < w; j++) {
                        char c = line.charAt(j);
                        System.out.print(c);
                        output.writeByte(c);
                    }
                    System.out.println("");
                }
                lineCount++;
                output.writeShort(Short.parseShort(reader.readLine()));
            } while (reader.readLine() != null);
        } finally {
            reader.close();
            output.close();
        }
    }

    public static void generate() {
        try {
            MapDataGenerator.parse("./res/dungeon.txt", "../dwellercore/res/dungeon.bin");
            MapDataGenerator.parse("./res/sewer.txt", "../dwellercore/res/sewer.bin");
            MapDataGenerator.parse("./res/cave.txt", "../dwellercore/res/cave.bin");
        } catch (Exception e) {
            System.err.println("Error when parsing map pieces. Message: " + e.getLocalizedMessage());
        }
    }

    public static void main(String args[]) {
        MapDataGenerator.generate();
    }
}
