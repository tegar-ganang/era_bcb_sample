package hu.schmidtsoft.map.main;

import java.io.IOException;
import java.lang.reflect.Method;
import hu.schmidtsoft.map.main.test.TestPerformanceOnPda;
import hu.schmidtsoft.map.srtm.SRTMConverter;
import hu.schmidtsoft.map.swt.SWTMain;
import hu.schmidtsoft.map.util.UtilFile;
import hu.schmidtsoft.map.util.UtilLog;
import hu.schmidtsoft.map.window.YamaWindow;

/**
 * The main method for yama map. TODO implement the starting modes described in
 * help.txt and redirect the main class entry in release
 * 
 * @author rizsi
 * 
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            String[] subArgs = subArgs(args);
            String arg = args[0];
            if ("window".equals(arg)) {
                WindowOptions wo = new WindowOptions().parse(subArgs);
                YamaWindow.main(wo);
                return;
            } else if ("convert".equals(arg)) {
                int ret = MainConvert.main_(subArgs);
                System.exit(ret);
                return;
            } else if ("util".equals(arg)) {
                int ret = 0;
                try {
                    ret = MainUtil.main(subArgs);
                } catch (Exception e) {
                    UtilLog.stackTrace(e);
                    ret = -1;
                }
                System.exit(ret);
                return;
            } else if ("srtm".equals(arg)) {
                int ret = 0;
                try {
                    ret = SRTMConverter.main(subArgs);
                } catch (Exception e) {
                    UtilLog.stackTrace(e);
                    ret = -1;
                }
                System.exit(ret);
                return;
            } else if ("portable".equals(arg)) {
                WindowOptions wo = new WindowOptions();
                wo = WindowOptions.parseNew(args);
                SWTMain.main(wo);
            } else if ("osm".equals(arg)) {
                try {
                    try {
                        Class<?> cl = Class.forName("hu.schmidtsoft.openstreetmap.CutOsm");
                        Method m = cl.getMethod("main", String[].class);
                        m.invoke(null, new Object[] { args });
                        System.exit(-1);
                    } catch (ClassNotFoundException e) {
                        UtilLog.println("OSM converter is not packaged with your yama distribution. Sorry. See http://yamamap.org/");
                        System.exit(-1);
                    }
                } catch (Exception e) {
                    UtilLog.error("error converting OSM map");
                    UtilLog.stackTrace(e);
                    System.exit(-1);
                }
            } else if ("measure".equals(arg)) {
                TestPerformanceOnPda.main(args);
                System.exit(0);
            } else {
                printhelp(args);
                return;
            }
        }
        printhelp(args);
        return;
    }

    public static String[] subArgs(String[] args) {
        String[] ret = new String[args.length - 1];
        for (int i = 0; i < ret.length; ++i) {
            ret[i] = args[i + 1];
        }
        return ret;
    }

    private static void printhelp(String[] args) {
        try {
            String s = UtilFile.loadFileAsString(Main.class.getResource("help.txt"));
            UtilLog.println(s);
        } catch (IOException e) {
            UtilLog.error("error loading help file resource");
            UtilLog.stackTrace(e);
        }
    }
}
