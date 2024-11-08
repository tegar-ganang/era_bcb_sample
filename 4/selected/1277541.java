package view.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import javax.swing.JOptionPane;
import org.asmeta.interpreter.Simulator;
import org.apache.log4j.Logger;
import parser.Reader;

/**
 *
 * @author joas.baia
 */
public class Execute {

    private String model = "engine\\simulator.asm";

    private Integer runs = 100;

    private Logger logger = Logger.getLogger(Simulator.class);

    Simulator sim;

    public Execute(String path) {
        Option config = new Option();
        runs = config.getIterations();
        execute(path);
    }

    public void execute(String path) {
        try {
            String enginePath = new File("engine").getCanonicalPath();
            model = enginePath + "\\simulator.asm";
            copyFile(path, enginePath + "\\initialStates.asm");
            sim = Simulator.createSimulator(model);
            sim.run(runs);
            logger.info("<Final>" + sim.getCurrentState() + "</Final>");
            this.writer(path);
            File f = new File(enginePath + "\\initialStates.asm");
            if (f.delete()) {
                System.out.println("Removing temp");
            }
        } catch (Exception e) {
            System.out.println("Erro: " + e.toString());
        }
    }

    public void writer(String path) {
        try {
            Reader reader = new Reader("log.sps");
            System.out.println(reader.getStringBuffer().toString());
            String aux = reader.getStringBuffer().toString();
            FileWriter fileWriter = new FileWriter(new File(path.replace(".asm", ".sps")));
            PrintWriter saida = new PrintWriter(fileWriter);
            saida.println(aux);
            fileWriter.close();
            saida.close();
            FileOutputStream erasor = new FileOutputStream("log.sps");
            byte[] b = null;
            erasor.write(b);
            erasor.close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, ex.getStackTrace());
        }
    }

    public void copyFile(String from, String to) {
        try {
            FileChannel from2 = new RandomAccessFile(from, "r").getChannel();
            FileChannel to2 = new RandomAccessFile(to, "rwd").getChannel();
            from2.transferTo(0, from2.size(), to2);
            from2.close();
            to2.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro copyFile(): " + e.toString());
        }
    }
}
