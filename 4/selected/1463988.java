package javamorph;

import java.io.*;
import javax.swing.*;

/**
 * File belongs to javamorph (Merging of human-face-pictures).
 * Copyright (C) 2009 - 2010  Claus Wimmer
 * See file ".../help/COPYING" for details!
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA * 
 *
 * @version 1.5
 * <br/>
 * @author claus.erhard.wimmer@googlemail.com
 * <br/>
 * Program: JavaMorph.
 * <br/>
 * Class: CStrings.
 * <br/>
 * License: GPLv2.
 * <br/>
 * Description: Collection of directory names.
 * <br/>
 * Hint: Defines the working directory at the desktop & its subdirectories.
 */
public class CStrings {

    /** File separator depending on OS. */
    public static final String SEP = System.getProperty("file.separator");

    /** User's home. */
    public static final String HOME = System.getProperty("user.home") + SEP;

    /** Dir from which this application has been started. */
    public static final String DIR = System.getProperty("user.dir");

    /** Location of the .jar distribution file. */
    public static final String JAR = DIR + SEP + "JavaMorph_2009_01_19.jar";

    /** Program name. */
    public static final String PROG = "JavaMorph";

    /** Program version. */
    public static final String VERSION = "V 1.5";

    /** Author of the program. */
    public static final String AUTHOR = "claus.erhard.wimmer@googlemail.com";

    /** License description. */
    public static final String LICENSE = "GPLv2";

    /** Working directory name. */
    public static final String APPDIR = "Desktop" + SEP + PROG + SEP;

    /** Working directory path. */
    public static final String WORKDIR = HOME + APPDIR;

    /** Input directory path. */
    public static final String INPUTDIR = HOME + APPDIR + "input" + SEP;

    /** Debug directory path. */
    public static final String DEBUGDIR = HOME + APPDIR + "debug" + SEP;

    /** Help directory path. */
    public static final String HELPDIR = HOME + APPDIR + "help" + SEP;

    /** Polygon data directory path. */
    public static final String POLYGONDIR = HOME + APPDIR + "polygon" + SEP;

    /** Mesh data directory path. */
    public static final String MESHDIR = HOME + APPDIR + "mesh" + SEP;

    /** Output directory path. */
    public static final String OUTPUTDIR = WORKDIR + "output" + SEP;

    /** Property file name & path. */
    public static final String PROPS = WORKDIR + "properties.props";

    /** Left input picture file name & path. */
    public static final String LEFT_INPUT = INPUTDIR + "left.jpg";

    /** Right input picture file name & path. */
    public static final String RIGHT_INPUT = INPUTDIR + "right.jpg";

    /** Left mesh data file name & path. */
    public static final String LEFT_MESH = MESHDIR + "left.msh";

    /** Right mesh data file name & path. */
    public static final String RIGHT_MESH = MESHDIR + "right.msh";

    /** Left polygon data file name & path. */
    public static final String LEFT_POLYGON = POLYGONDIR + "left.pol";

    /** Right polygon data file name & path. */
    public static final String RIGHT_POLYGON = POLYGONDIR + "right.pol";

    /** Left clip matrix file name & path. */
    public static final String LEFT_DEBUG = DEBUGDIR + "left_debug.png";

    /** Right clip matrix file name & path. */
    public static final String RIGHT_DEBUG = DEBUGDIR + "right_debug.png";

    /** Copyright file name & path. */
    public static final String COPYING = HELPDIR + "COPYING";

    /** Help file name & path. */
    public static final String HELP = HELPDIR + "JavaMorph.pdf";

    /** File to store the triangulation of the left mesh to. */
    public static final String LEFT_TRI = DEBUGDIR + "t_left_triangles.png";

    /** File to store the triangulation of the right mesh to. */
    public static final String RIGHT_TRI = DEBUGDIR + "t_right_triangles.png";

    /** File to store the triangulation of the 50% merged mesh to. */
    public static final String MIDDLE_TRI = DEBUGDIR + "t_middle_triangles.png";

    /** Left file name prefix. */
    public static final String LEFT_PREFIX = "left";

    /** Right file name prefix. */
    public static final String RIGHT_PREFIX = "right";

    /**
     * Compose the name of one result output file.
     * @param n Number of the morph step.
     * @return Filename consisting of 3 numerical digits + extension.
     */
    public static String getOutput(int n) {
        String str_n = "" + n;
        while (3 > str_n.length()) {
            str_n = "0" + str_n;
        }
        return OUTPUTDIR + str_n + ".jpg";
    }

    /**
     * Called if the application is called for the first time on one PC.
     * Try to copy the sample data from the .jar distribution file into the 
     * working directory. Create sub directories therefore.
     * @return <code>true</code>if the copy process has been successful.
     */
    public static boolean initialize() {
        try {
            File work = new File(WORKDIR), input = new File(INPUTDIR), output = new File(OUTPUTDIR), polygon = new File(POLYGONDIR), mesh = new File(MESHDIR), help = new File(HELPDIR), debug = new File(DEBUGDIR);
            if (!work.exists()) {
                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Workdir = " + WORKDIR + " doesn't exist, create?")) {
                    work.mkdir();
                    input.mkdir();
                    output.mkdir();
                    mesh.mkdir();
                    polygon.mkdir();
                    help.mkdir();
                    debug.mkdir();
                    extractFiles();
                    JOptionPane.showMessageDialog(null, "Please handle app's files within the following workdir = " + WORKDIR + '!');
                    return true;
                } else {
                    JOptionPane.showMessageDialog(null, "Don't create workdir = " + WORKDIR + ", exit now");
                    return false;
                }
            } else {
                return true;
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Can't create workdir (" + WORKDIR + "), see console output!");
            return false;
        }
    }

    /**
     * Extract files from the .jar distribution into the generated
     * sub directories of the working directory.
     */
    public static void extractFiles() {
        copyFile("help/COPYING", COPYING);
        copyFile("help/JavaMorph.pdf", HELP);
        copyFile("input/left.jpg", LEFT_INPUT);
        copyFile("input/right.jpg", RIGHT_INPUT);
        copyFile("mesh/left.msh", LEFT_MESH);
        copyFile("mesh/right.msh", RIGHT_MESH);
        copyFile("polygon/left.pol", LEFT_POLYGON);
        copyFile("polygon/right.pol", RIGHT_POLYGON);
    }

    /**
     * Copy one single file from the .jar distribution into the sub directory
     * of the working directory.
     * @param in Location of the .jar file entry.
     * @param out Target path & name of the file to copy the content to.
     */
    public static void copyFile(String in, String out) {
        try {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            InputStream raw = loader.getResourceAsStream(in);
            BufferedInputStream bin = new BufferedInputStream(raw);
            BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(out));
            int i;
            while (-1 != (i = bin.read())) bout.write(i);
            bin.close();
            bout.close();
        } catch (Exception e) {
            System.out.println("in = " + in + '.');
            System.out.println("out = " + out + '.');
            System.err.println(e.getMessage());
            System.out.println("in = " + in + '.');
            System.out.println("out = " + out + '.');
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Can't copy file (" + out + "), see console output!");
        }
    }
}
