package coffeeviewer.source.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import coffeeviewer.gui.main.MainGui;
import coffeeviewer.source.llcommon.LLError;

public class Main {

    private static Main instancia = new Main();

    private static String gDebugInfo[];

    private static PrintWriter out;

    private static LLError llerror;

    private static LLVersionInfo llversioninfo;

    private static MainGui gui;

    public static Main getInstance() {
        return instancia;
    }

    public static void main(String[] args) throws Exception {
        llerror = new LLError();
        llversioninfo = new LLVersionInfo();
        initLogging();
        init_default_trans_args();
        writeSystemInfo();
        gui = new MainGui();
    }

    private static void writeSystemInfo() {
        gDebugInfo = new String[10];
        gDebugInfo[0] = llerror.getLogfilename();
        String clientinfo[] = new String[5];
        clientinfo[0] = llversioninfo.getChannelName();
        clientinfo[1] = llversioninfo.getLlversionmajor();
        clientinfo[2] = llversioninfo.getLlversionminor();
        clientinfo[3] = llversioninfo.getLlversionbuild();
        clientinfo[4] = llversioninfo.getLlversionpatch();
        out.println(gDebugInfo[0]);
        out.close();
    }

    private static void init_default_trans_args() throws Exception {
    }

    /**
	 * Init
	 * @throws FileNotFoundException 
	 */
    private static void initLogging() throws FileNotFoundException {
        File directorioFacturas = new File("./logs/");
        if (!directorioFacturas.exists()) directorioFacturas.mkdirs();
        File archivoFactura = new File(directorioFacturas, "Log.txt");
        out = new PrintWriter(archivoFactura);
    }

    public String giveSystemInfo() {
        return "as";
    }
}
