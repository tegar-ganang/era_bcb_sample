package paperfly.game.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import com.jme.app.AbstractGame;
import com.jme.app.BaseGame;
import com.jme.system.DisplaySystem;
import com.jmex.model.converters.FormatConverter;
import com.jmex.model.converters.ObjToJme;

public class ModelConverter extends BaseGame {

    private List<File> files;

    private File outputDir;

    private FormatConverter conversor;

    public ModelConverter(File outputDir, FormatConverter conversor, String... files) {
        this.files = new ArrayList<File>();
        this.outputDir = outputDir;
        this.conversor = conversor;
        for (String file : files) {
            File f = new File(file);
            if (!f.exists()) {
                throw new RuntimeException("File " + file + " doesn't exist.");
            }
            if (f.isDirectory()) {
                throw new RuntimeException("File " + file + " is a directory.");
            }
            this.files.add(f);
        }
    }

    public void convert() {
        setDialogBehaviour(AbstractGame.NEVER_SHOW_PROPS_DIALOG);
        super.start();
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        FormatConverter conversor = new ObjToJme();
        File out = new File("models/jme");
        String[] files = new String[] { "models/obj/f14.obj", "models/obj/f18.obj", "models/obj/pista.obj" };
        ModelConverter mConverter = new ModelConverter(out, conversor, files);
        mConverter.convert();
    }

    protected void initGame() {
        try {
            for (File fonte : files) {
                String absolutePath = outputDir.getAbsolutePath();
                String separator = System.getProperty("file.separator");
                String name = fonte.getName();
                String destName = name.substring(0, name.length() - 3);
                File destino = new File(absolutePath + separator + destName + "jme");
                FileInputStream reader = new FileInputStream(fonte);
                OutputStream writer = new FileOutputStream(destino);
                conversor.setProperty("mtllib", fonte.toURL());
                conversor.convert(reader, writer);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.finish();
    }

    protected void initSystem() {
        display = DisplaySystem.getDisplaySystem();
        display.createWindow(1, 1, 16, properties.getFreq(), false);
    }

    protected void reinit() {
    }

    protected void render(float interpolation) {
    }

    protected void update(float interpolation) {
    }

    protected void cleanup() {
    }
}
