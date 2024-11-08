package org.waterlanguage;

import java.io.File;
import static wb.Ents.*;
import static org.waterlanguage.Caps.*;
import static org.waterlanguage.Utils.*;
import static org.waterlanguage.Wbinfra.*;
import static org.waterlanguage.Wwrun.*;
import static org.waterlanguage.Symcodes.waterVersion;

class Wwentry {

    public static void main(String[] args) {
        Caps caps = makeCaps();
        init_water(caps);
        if (!doCmdlineArgs(caps, args.length, args)) print_usage();
        finalWb();
    }

    public static void init_water(Caps caps) {
        String os_name = os_name();
        String user_home_folder = System.getProperty("user.home");
        String runtimeFolder, userFolder;
        user_home_folder = user_home_folder.replace('\\', '/');
        System.out.println("OS name: " + os_name);
        if (os_name == "Windows") {
            runtimeFolder = "C:/Program Files/water/" + waterVersion + "/";
            userFolder = user_home_folder + "/Application Data/water/";
        } else if (os_name == "Mac") {
            runtimeFolder = "/Applications/water/" + waterVersion + "/";
            userFolder = user_home_folder + "/water/";
        } else {
            runtimeFolder = "../";
            userFolder = user_home_folder + "/water/";
        }
        initWw(null);
        importLogicals(caps, stringAppend(stringAppend("<logical user=<resource '", userFolder, "' read write append create execute/>"), stringAppend(" water_runtime=<resource '", runtimeFolder, "' read execute/>"), "/>"));
        File user_folder_java = new File(userFolder);
        if (!(user_folder_java.exists())) {
            user_folder_java.mkdir();
        }
    }

    public static String os_name() {
        String lv_name = System.getProperty("os.name");
        if (lv_name.startsWith("Windows")) return "Windows"; else if (lv_name.startsWith("Mac")) return "Mac"; else if (lv_name.startsWith("Linux")) return "Linux"; else return lv_name;
    }

    public static String execute_water_code(Caps caps, String water_code) {
        return execute_water_code(caps, water_code, null);
    }

    public static String execute_water_code(Caps caps, String water_code, String source_uri) {
        if (!(water_code.endsWith("<to_htm/>") || water_code.endsWith("<to_htm_page/>") || water_code.endsWith("<to_cxs/>"))) {
            water_code = water_code + ".<to_htm/>";
        }
        {
            byte[] cbyts = stringToBytes(water_code);
            String strng = ww_RunPgrm(caps, source_uri, cbyts, cbyts.length);
            return (strng);
        }
    }

    public static void print_usage() {
        System.out.print("\n" + "Usage: java -cp \".;wb.jar\" org.waterlanguage.Wwentry\n" + " Run an interactive Water-shell in a new session\n" + " and save the session state to the file \"logical://user/water_sh.h3o\".\n" + "\n" + "Usage: java -cp \".;wb.jar\" org.waterlanguage.Wwentry FILE.h3o\n" + " Run an interactive Water-shell using session state \"FILE.h3o\"\n" + " and save the session state to the file \"FILE.h3o\".\n" + "\n" + "Usage: java -cp \".;wb.jar\" org.waterlanguage.Wwentry FILE.h2o\n" + " Execute the Water program stored in the file \"FILE.h2o\" in a new\n" + " session; then save the session state to the file \"FILE.h3o\".\n" + "\n" + "Usage: java -cp \".;wb.jar\" org.waterlanguage.Wwentry FILE.h3o \"water_expression\"\n" + " Execute the Water code in the \"WATER_EXPRESSION\" argument using\n" + " session state \"FILE.h3o\".  The WATER_EXPRESSION string must be\n" + " between double-quotes.  The result is printed to standard output and\n" + " the session state is saved to the file \"FILE.h3o\".\n" + "\n" + "Usage: java -cp \".;wb.jar\" org.waterlanguage.Wwentry FILE.h2o \"water_expression\"\n" + " Execute the Water program stored in the file \"FILE.h2o\" in a new\n" + " session; then execute the Water code in the \"WATER_EXPRESSION\"\n" + " argument.  The WATER_EXPRESSION string must be between double quotes.\n" + " The result is printed to standard output and the session state is\n" + " saved to the file \"FILE.h3o\".\n" + "\n" + "Usage: java -cp \".;wb.jar\" org.waterlanguage.Wwentry FILE.h3o \"water_method\" \"URI-encoded-form-vars\"\n" + "Usage: java -cp \".;wb.jar\" org.waterlanguage.Wwentry FILE.h2o \"water_method\" \"URI-encoded-form-vars\"\n" + " Similar to the above commands but instead of taking a Water\n" + " expression, takes the name of a Water method to call whose arguments\n" + " are URI-encoded variables.\n");
    }
}
