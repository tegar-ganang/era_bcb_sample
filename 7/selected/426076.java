package org.isodl.gui;

import org.isodl.gui.maker.LicenseBatchWriter;
import org.isodl.gui.maker.LicenseMakerFrame;
import org.isodl.gui.reader.DrivingLicenseApp;

/**
 * Runs the reader or the writer GUI for the driving license
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 */
public class GUIRunner {

    /**
     * @param args
     *            indicate wheter we sould run the reader or the writer. Reader
     *            is the default
     */
    public static void main(String[] args) {
        if (args.length == 1 && "writer".equals(args[0])) {
            LicenseMakerFrame.main(new String[0]);
        } else if (args.length > 1 && "batch".equals(args[0])) {
            String[] newArgs = new String[args.length - 1];
            for (int i = 0; i < newArgs.length; i++) {
                newArgs[i] = args[i + 1];
            }
            LicenseBatchWriter.main(newArgs);
        } else {
            DrivingLicenseApp.main(new String[0]);
        }
    }
}
