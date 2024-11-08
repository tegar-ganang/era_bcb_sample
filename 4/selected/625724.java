package com.gele.apps.wow.mangos.editor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Created on 03 Oct 2006
 *
 * @author    <a href="mailto:lousy.kizura@gmail.com">Gerhard Leibrock aka Kizura</a>
 * @copyright <a href="mailto:lousy.kizura@gmail.com">Gerhard Leibrock </a>
 * <br/>
 *
 * License:<br/>
 * This software is placed under the GNU GPL.<br/>
 * For further information, see the page :<br/>
 * http://www.gnu.org/copyleft/gpl.html.<br/>
 * For a different license please contact the author.<br/>
 * 
 * $LastChangedDate: 2006-09-16 23:48:03 +0200 (Sa, 16 Sep 2006) $ <br/>
 * $LastChangedBy: gleibrock $ <br/>
 * $LastChangedRevision: 221 $ <br/>
 * $Author: gleibrock $ <br/>
 * $HeadURL: svn://painkiller/wdb-manager/development/src/java/com/gele/tools/wow/wdbearmanager/WDBearManager.java $<br/>
 * $Rev: 221 $<br/>
 *<br/> 
 * Changes:<br/>
 * 1.0 - First release<br/>
 * <br/>
 */
public class CreateStubXML {

    /**
   * @param args
   */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Specify name of the file, just one entry per line");
            System.exit(0);
        }
        File inFile = new File(args[0]);
        BufferedReader myBR = null;
        File outFile = new File(args[0] + ".xml");
        BufferedWriter myBW = null;
        try {
            myBR = new BufferedReader(new FileReader(inFile));
            myBW = new BufferedWriter(new FileWriter(outFile));
        } catch (Exception ex) {
            System.out.println("IN: " + inFile.getAbsolutePath());
            System.out.println("OUT: " + outFile.getAbsolutePath());
            ex.printStackTrace();
            System.exit(0);
        }
        try {
            String readLine;
            while ((readLine = myBR.readLine()) != null) {
                myBW.write("<dbColumn name=\"" + readLine + "\" display=\"" + readLine + "\" panel=\"CENTER\"  >");
                myBW.write("\n");
                myBW.write("<dbType name=\"text\" maxVal=\"10\" defaultVal=\"\" sizeX=\"5\"/>");
                myBW.write("\n");
                myBW.write("</dbColumn>");
                myBW.write("\n");
            }
            myBW.close();
            myBR.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        System.out.println("OUT: " + outFile.getAbsolutePath());
        System.out.println("erzeugt");
    }
}
