package org.unicolet.ms.core.cmd;

import org.unicolet.ms.core.Registry;
import org.unicolet.ms.gui.Tokens;
import org.unicolet.ms.gui.Logger;
import org.unicolet.axl.AXLFile;
import org.unicolet.axl.AXLWriter;
import javax.swing.*;
import java.io.*;
import java.awt.*;

/**
 * Created:
 * User: unicoletti
 * Date: 10:16:58 AM Oct 18, 2005
 */
public class ConvertCommand extends BaseCommand {

    public void doExecute() throws Exception {
        JFileChooser fileChooser = new JFileChooser();
        if (Registry.getPreference("directory.lastopen") != null) {
            fileChooser.setCurrentDirectory(new File(Registry.getPreference("directory.lastopen")));
        }
        int returnVal = fileChooser.showSaveDialog((Component) Registry.getAttribute(Tokens.APPLICATION));
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            if (overwrite(outputFile)) {
                save2File(convertAxl2Mapserver(getAXLFile()), outputFile);
                copySymbols(outputFile.getParentFile());
                Logger.log("Remember to create the fontset.txt file");
            } else {
                Logger.log("File not overwritten.\n");
                log.info("File not overwritten");
            }
        }
    }

    private boolean overwrite(File file) {
        boolean overwritePreference = (Registry.getPreference(Tokens.SILENTOVERWRITE) != null) ? new Boolean(Registry.getPreference(Tokens.SILENTOVERWRITE)).booleanValue() : false;
        if (overwritePreference) {
            return true;
        } else {
            if (file.exists()) {
                Object[] options = { "Overwrite", "Cancel" };
                int response = JOptionPane.showOptionDialog((Component) Registry.getAttribute(Tokens.APPLICATION), "The file : " + file.getAbsolutePath() + " \nalready exists.\nDo you want to overwrite it?", "Confirm overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                if (response == JOptionPane.YES_OPTION) {
                    return true;
                }
            }
        }
        return false;
    }

    private void copySymbols(File destination) throws IOException {
        File symbolsOut = new File(destination.getAbsolutePath() + File.separator + "symbols.sym");
        if (overwrite(symbolsOut)) {
            File symbolsIn = new File(Tokens.SYMBOLSFILE);
            BufferedReader in = new BufferedReader(new FileReader(symbolsIn));
            BufferedWriter out = new BufferedWriter(new FileWriter(symbolsOut));
            String line = in.readLine();
            while (line != null) {
                out.write(line + "\n");
                line = in.readLine();
            }
            in.close();
            out.close();
            log.info("Symbols copied to: " + symbolsOut.getAbsolutePath());
            Logger.log("Symbols copied to: " + symbolsOut.getAbsolutePath() + "\n");
        } else {
            log.info("Symbols not ovewritten");
            Logger.log("Symbols not overwritten\n");
        }
    }

    private void save2File(String content, File outputMapserverFile) throws IOException {
        FileWriter writer = new FileWriter(outputMapserverFile);
        writer.write(content);
        writer.close();
        log.info("Conversion ended. Saved to: " + outputMapserverFile.getAbsolutePath());
        Logger.log("Conversion ended. Saved to: " + outputMapserverFile.getAbsolutePath() + "\n");
    }

    public AXLFile getAXLFile() {
        return (AXLFile) Registry.getAttribute(Tokens.CURRENTFILE);
    }

    private String convertAxl2Mapserver(AXLFile file) throws Exception {
        log.info("Conversion started");
        Logger.log("Conversion started\n");
        return AXLWriter.axl2Mapserver(file, getTemplateFile());
    }
}
