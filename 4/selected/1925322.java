package org.gcreator.pineapple.pinedl.cpp;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import org.gcreator.pineapple.events.Event;
import org.gcreator.pineapple.formats.Actor;
import org.gcreator.pineapple.formats.Scene;
import org.gcreator.pineapple.game2d.GameProjectType;
import org.gcreator.pineapple.core.PineappleCore;
import org.gcreator.pineapple.formats.ClassResource;
import org.gcreator.pineapple.formats.ClassResource.Field;
import org.gcreator.pineapple.game2d.GamePlugin;
import org.gcreator.pineapple.pinedl.AccessControlKeyword;
import org.gcreator.pineapple.pinedl.PineClass;
import org.gcreator.pineapple.pinedl.Type;
import org.gcreator.pineapple.project.Project;
import org.gcreator.pineapple.project.ProjectElement;
import org.gcreator.pineapple.project.io.BasicFile;
import org.gcreator.pineapple.validators.Glob;
import org.gcreator.pineapple.validators.UniversalValidator;

/**
 * This class is used to compile the game.
 * 
 * @author Luís Reis
 * @author Serge Humphrey
 */
public class GameCompiler {

    Project p;

    File outputFolder;

    File binFolder;

    File resFolder;

    File resoutFolder;

    File compConf;

    File imageArchive;

    Scene mainScene;

    static CompilerFrame compFrame;

    Vector<File> pineScripts = new Vector<File>();

    Vector<File> imageFiles = new Vector<File>();

    Vector<File> compFiles = new Vector<File>();

    private Vector<String> context = new Vector<String>();

    String gamePackage = "Game";

    boolean worked = true;

    PrintWriter headerH = null;

    File outputFile = null;

    CompilationProfile profile = null;

    int resIndex = 0;

    HashMap<File, String> imageNames = new HashMap<File, String>();

    HashMap<String, String> config = new HashMap<String, String>();

    HashMap<String, ClassResource> clsres = new HashMap<String, ClassResource>();

    static boolean copiedLib = false;

    int scenes = 0;

    public GameCompiler(final Project p) {
        this(p, getDefaultProfile());
    }

    public static CompilationProfile getDefaultProfile() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            return CompilationProfile.MINGW_WINDOWS;
        } else {
            return CompilationProfile.UNIX_GCC;
        }
    }

    public GameCompiler(final Project p, final CompilationProfile profile) {
        this.p = p;
        this.profile = profile;
        if (compFrame != null) {
            compFrame.dispose();
        }
        GlobalLibrary.userDefinedClasses.clear();
        compFrame = new CompilerFrame(this);
        compFrame.setSize(330, 460);
        compFrame.setVisible(true);
        if (p.getProjectType() instanceof GameProjectType) {
            try {
                compFrame.write("Checking resource names... ");
                if (GamePlugin.checkResourceNames()) {
                    compFrame.writeLine("ok");
                } else {
                    compFrame.writeLine("<span style='color: red;'>bad!</span>");
                    compFrame.writeLine("<span style='color: red;'>Cannot continue with bad resource names." + "<br/>Please correct them and re-compile the game.</span>");
                    JButton fix = new JButton("Fix Resource Names");
                    fix.setSize(164, 36);
                    fix.setLocation(4, compFrame.output.getPreferredSize().height);
                    fix.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            GamePlugin.showCheckResourcesDialog();
                        }
                    });
                    compFrame.output.add(fix);
                    return;
                }
                prepare();
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        try {
                            for (BasicFile f : Glob.glob(new UniversalValidator(), true)) {
                                ProjectElement e = f.getElement();
                                if (e.getFile() == null) {
                                    continue;
                                }
                                String name = e.getFile().getName();
                                String format = name.substring(name.lastIndexOf('.') + 1);
                                format = format.toLowerCase();
                                if (format.equals("actor")) {
                                    createActorScript(e);
                                } else if (format.equals("scene")) {
                                    createSceneScript(e);
                                    scenes++;
                                } else if (format.equals("pdl")) {
                                    copyScript(e);
                                } else {
                                    for (String s : ImageIO.getReaderFileSuffixes()) {
                                        if (s.equalsIgnoreCase(format)) {
                                            copyImage(e);
                                            break;
                                        }
                                    }
                                }
                            }
                            createImageArchive();
                            buildContext();
                            compFrame.writeLine("Generating C++ and header files");
                            context.add("TextureList");
                            HashMap<File, PineClass> tmp = new HashMap<File, PineClass>();
                            Vector<GlobalLibrary.ClassDefinition> clsDef = new Vector<GlobalLibrary.ClassDefinition>();
                            Vector<InitialParser> parsers = new Vector<InitialParser>();
                            for (File script : pineScripts) {
                                if (!worked) {
                                    return;
                                }
                                compFrame.writeLine("<em>" + script.getName() + "...</em>");
                                InputStream is = new FileInputStream(script);
                                InitialParser parser = new InitialParser(is);
                                clsDef.add(GlobalLibrary.addUserClass(parser.cls));
                                parsers.add(parser);
                                String name = script.getName();
                                name = name.substring(0, name.indexOf('.'));
                                tmp.put(script, parser.cls);
                            }
                            generateTextureList();
                            for (int i = 0; i < parsers.size(); i++) {
                                InitialParser parser = parsers.get(i);
                                GlobalLibrary.ClassDefinition cDef = clsDef.get(i);
                                if (parser.cls.superClass != null) {
                                    cDef.parent = parser.classFromName(parser.cls.superClass.type);
                                } else {
                                    cDef.parent = null;
                                }
                            }
                            for (File script : tmp.keySet()) {
                                generateHeader(script, tmp.get(script));
                                generateCppFile(script, tmp.get(script));
                            }
                            copyLib(!copiedLib);
                            copiedLib = true;
                            if (worked) {
                                compFrame.writeLine("Compiling C++ code");
                                generateMain();
                                compile();
                            } else {
                                compFrame.writeLine("<font color='red'>Failed to compile.</font>");
                            }
                        } catch (Exception ex) {
                            compFrame.writeLine("<font color='red'>COMPILE EXCEPTION: " + ex.getMessage() + "</font>");
                            ex.printStackTrace();
                            worked = false;
                        }
                    }
                };
                t.start();
            } catch (Exception e) {
                compFrame.writeLine("<font color='red'>Compile Exception: " + e.getMessage() + "</font>");
                e.printStackTrace();
                worked = false;
            }
        }
    }

    public void generateTextureList() throws Exception {
        File h = new File(outputFolder, "texturelistdefined.h");
        File cpp = new File(outputFolder, "TextureListDefined.cpp");
        PrintWriter w = new PrintWriter(h);
        w.println("#ifndef __PINEDL_TEXTURELISTDEFINED_H__");
        w.println("#define __PINEDL_TEXTURELISTDEFINED_H__");
        w.println();
        int imgn = 0;
        for (File image : imageFiles) {
            String fname = imageNames.get(image);
            int index = fname.lastIndexOf('.');
            fname = fname.substring(0, index) + "_" + fname.substring(index + 1);
            w.println(("static const int " + fname + " = " + imgn++ + ";"));
        }
        w.println();
        w.println("#endif");
        w.println();
        w.close();
        w = new PrintWriter(cpp);
        Vector<BasicFile> simgs = new Vector<BasicFile>();
        GlobalLibrary.ClassDefinition textureList = new GlobalLibrary.ClassDefinition("TextureList");
        for (File image : imageFiles) {
            String name = imageNames.get(image);
            int index = name.lastIndexOf('.');
            name = name.substring(0, index) + "_" + name.substring(index + 1);
            System.out.println("Adding field " + name + " to TextureList.");
            textureList.fields.add(new GlobalLibrary.FieldDefinition(name, Type.INT, true, true, AccessControlKeyword.PUBLIC));
        }
        GlobalLibrary.coreClasses.add(textureList);
        w.println("#include \"texturelist.h\"");
        w.println("void Pineapple::TextureList::init()\n{");
        w.println("\tTextureList::archive_size = " + imageArchive.length() + ";");
        if (mainScene == null) {
            if (scenes == 0) {
                throw new Exception("<font color='red'>Hey you! Add some scenes to your project and set the scene order in Tools>Game Settings.<br/>BUILD FAILED</font>");
            } else {
                throw new Exception("<font color='red'>Hey you! Set the scene order in Tools>Game Settings>Scene Order.<br/>BUILD FAILED</font>");
            }
        }
        for (Scene.ActorInScene s : mainScene.actors) {
            if (!simgs.contains(s.actor.getImage())) {
                simgs.add(s.actor.getImage());
            }
        }
        for (Scene.Background b : mainScene.backgrounds) {
            if (b.drawImage && !simgs.contains(b.image)) {
                simgs.add(b.image);
            }
        }
        w.println("\tTextureList::START_IMAGES = new unsigned int[" + simgs.size() + "];");
        int i = 0;
        for (BasicFile f : simgs) {
            if (f != null) {
                w.println("\tTextureList::START_IMAGES[" + i++ + "] = " + f.getName().replaceAll("\\.", "_") + ";");
            }
        }
        w.println("\tTextureList::N_START_IMAGES = " + simgs.size() + ";");
        w.println("\tTextureList::load();");
        w.println("}\n");
        w.close();
    }

    public static void copyFile(String resFolder, File outFolder, String fname) throws IOException {
        copyFile(resFolder, outFolder, fname, true);
    }

    public static void copyFile(String resFolder, File outFolder, String fname, boolean replace) throws IOException {
        File f = new File(outFolder, fname);
        if (f.exists() && !f.canWrite()) {
            return;
        }
        if (f.exists() && !replace) {
            return;
        }
        FileOutputStream fos = new FileOutputStream(f);
        InputStream is = GameCompiler.class.getResourceAsStream(resFolder + fname);
        if (is == null) {
            throw new IOException("Resource " + resFolder + fname + " does not exist!");
        }
        int read;
        byte[] buffer = new byte[8192];
        while ((read = is.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
        }
        fos.close();
        is.close();
    }

    private void generateMain() throws IOException {
        File f = new File(outputFolder, "main.cpp");
        PrintWriter w = new PrintWriter(f);
        w.println("#include \"header.h\"");
        w.println();
        w.println("int main(int argc, char** argv) {");
        w.println("\tPineapple::Application::init();");
        w.println("\tPineapple::Window::setCaption(\"Pineapple Game\");");
        Hashtable<String, String> hs = PineappleCore.getProject().getSettings();
        if (hs.containsKey(("scene-order"))) {
            String scene = hs.get("scene-order").split(";")[0];
            scene = scene.substring(scene.lastIndexOf('/') + 1);
            scene = scene.substring(0, scene.indexOf('.'));
            w.println(("\tPineapple::Application::setScene(new Game::" + scene + "());"));
        }
        w.println("\t/* No idea why, but we got to call these here before we load textures. */");
        w.println("\tSDL_GL_SwapBuffers();");
        w.println("\tSDL_Delay(100);");
        w.println("\tPineapple::TextureList::init();");
        String fullscreen = p.getSettings().get("game-fullscreen");
        if (fullscreen != null) {
            w.println("\tPineapple::Window::setFullscreen(" + fullscreen + ");");
        }
        w.println("\tPineapple::Window::run();");
        w.println("\t/* No code is executed beyond this point until after " + "the game window has been closed. */");
        w.println("}");
        w.close();
    }

    private void copyLib(boolean replace) throws IOException {
        compFrame.writeLine("Copying static library");
        if (profile == CompilationProfile.UNIX_GCC) {
            copyFile("/org/gcreator/pineapple/pinedl/cpp/res/linux/", outputFolder, "libPineapple.a", replace);
        } else if (profile == CompilationProfile.MINGW_WINDOWS) {
            copyFile("/org/gcreator/pineapple/pinedl/cpp/res/windows/", binFolder, "SDL.dll", replace);
            copyFile("/org/gcreator/pineapple/pinedl/cpp/res/windows/", binFolder, "SDL_image.dll", replace);
            copyFile("/org/gcreator/pineapple/pinedl/cpp/res/windows/", binFolder, "jpeg.dll", replace);
            copyFile("/org/gcreator/pineapple/pinedl/cpp/res/windows/", binFolder, "libpng12-0.dll", replace);
            copyFile("/org/gcreator/pineapple/pinedl/cpp/res/windows/", binFolder, "libtiff-3.dll", replace);
            copyFile("/org/gcreator/pineapple/pinedl/cpp/res/windows/", outputFolder, "libPineapple.a", replace);
        }
        compFrame.writeLine("Copying header files");
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "actor.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "array.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "application.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "background.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "collision.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "color.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "core.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "drawing.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "exceptions.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "io.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "keyboard.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "keycodes.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "pamath.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "scene.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "shapes.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "texture.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "texturelist.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "timer.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "vector.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "view.h", replace);
        copyFile("/org/gcreator/pineapple/pinedl/cpp/res/headers/", outputFolder, "window.h", replace);
    }

    private void compile() throws Exception {
        headerH.println("#endif");
        headerH.close();
        Vector<String> command = new Vector<String>();
        String compiler = "g++";
        command.add(compiler);
        command.add("-o");
        command.add(outputFile.getAbsolutePath());
        for (File script : pineScripts) {
            String path = script.getAbsolutePath();
            command.add(path.substring(0, path.lastIndexOf('.')) + ".cpp");
        }
        command.add((new File(outputFolder, "TextureListDefined.cpp")).getAbsolutePath());
        command.add((new File(outputFolder, "main.cpp")).getAbsolutePath());
        command.add((new File(outputFolder, "libPineapple.a")).getAbsolutePath());
        int c;
        Process version = Runtime.getRuntime().exec(compiler + " --version");
        InputStream veris = new BufferedInputStream(version.getInputStream());
        String verout = "";
        while ((c = veris.read()) != -1) {
            verout += (char) c;
        }
        version.waitFor();
        compFrame.writeLine("g++ --version:\n" + verout);
        if (profile == CompilationProfile.UNIX_GCC) {
            Process sdlconfig = Runtime.getRuntime().exec("sdl-config --cflags --libs");
            InputStream sdlis = new BufferedInputStream(sdlconfig.getInputStream());
            String sdlout = "";
            while ((c = sdlis.read()) != -1) {
                sdlout += (char) c;
            }
            sdlconfig.waitFor();
            String[] sdloutSplit = sdlout.split("\\s");
            for (String cmd : sdloutSplit) {
                command.add(cmd);
            }
            command.add("-lSDL_image");
            command.add("-lGLU");
        } else if (profile == CompilationProfile.MINGW_WINDOWS) {
            command.add("-lmingw32");
            command.add("-lSDLmain");
            command.add("-lSDL");
            command.add("-mwindows");
            command.add("-lSDL_image");
            command.add("-lglu32");
            command.add("-lopengl32");
            command.add("-lz");
        }
        command.add("-ggdb");
        String[] args = command.toArray(new String[command.size()]);
        compFrame.writeLine("Calling GCC C++ for executable generation");
        StringBuffer cmd = new StringBuffer("<font color='green'><em>");
        for (String s : args) {
            s = s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            if (s.contains(" ")) {
                s = "\"" + s + "\"";
            }
            cmd.append(" ").append(s);
        }
        cmd.append("</em></font>");
        compFrame.writeLine(cmd.toString());
        Process proc = Runtime.getRuntime().exec(args);
        String res = "";
        InputStream is = new BufferedInputStream(proc.getErrorStream());
        while ((c = is.read()) != -1) {
            if (c != '\n') {
                res += (char) c;
            } else {
                compFrame.writeLine(res);
                res = "";
            }
        }
        int x = proc.waitFor();
        if (x != 0) {
            compFrame.writeLine("<font color='red'>There seems to have been some errors with the compiler<br/> " + "Please report them to the G-Creator team</font>");
        } else {
            appendArchive();
            compFrame.writeLine("Finished!");
            compFrame.runGameButton.setEnabled(true);
        }
        saveConfig();
    }

    private void buildContext() {
        for (File script : pineScripts) {
            String fname = script.getName();
            fname = fname.substring(0, fname.indexOf('.'));
            context.add(fname);
        }
    }

    private PineClass generateHeader(File script, PineClass cls) throws IOException {
        String fname = script.getName();
        fname = fname.substring(0, fname.lastIndexOf('.'));
        File output = new File(outputFolder, fname + ".h");
        headerH.print("#include \"");
        headerH.print(fname);
        headerH.println(".h\"");
        FileOutputStream fos = new FileOutputStream(output);
        HGenerator gen = new HGenerator(fos, this, fname, cls);
        if (!gen.wasSuccessful()) {
            worked = false;
        }
        fos.close();
        return gen.cls;
    }

    private void generateCppFile(File script, PineClass cls) throws IOException {
        InputStream is = new FileInputStream(script);
        String fname = script.getName();
        fname = fname.substring(0, fname.lastIndexOf('.'));
        File output = new File(outputFolder, fname + ".cpp");
        FileOutputStream fos = new FileOutputStream(output);
        if (is.available() == 0) {
            System.out.println("Blank file: " + fname + "; skipping");
            return;
        }
        CppGenerator gen = new CppGenerator(fos, this, fname, cls);
        if (!gen.wasSuccessful()) {
            worked = false;
        }
        fos.close();
    }

    private void copyImage(ProjectElement e) throws Exception {
        String fn = e.getName();
        if (!fn.toLowerCase().endsWith(".png")) {
            if (fn.contains(".")) {
                fn = fn.substring(0, fn.lastIndexOf('.')) + ".png";
            } else {
                fn += ".png";
            }
        }
        File img = new File(resFolder, fn);
        File imgz = new File(resoutFolder.getAbsolutePath(), fn + ".zlib");
        boolean copy = true;
        if (img.exists() && config.containsKey(img.getName())) {
            long modified = Long.parseLong(config.get(img.getName()));
            if (modified >= img.lastModified()) {
                copy = false;
            }
        }
        if (copy) {
            convertImage(e.getFile(), img);
            config.put(img.getName(), String.valueOf(img.lastModified()));
        }
        DeflaterOutputStream out = new DeflaterOutputStream(new BufferedOutputStream(new FileOutputStream(imgz)));
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(img));
        int read;
        while ((read = in.read()) != -1) {
            out.write(read);
        }
        out.close();
        in.close();
        imageFiles.add(imgz);
        imageNames.put(imgz, e.getName());
    }

    private void createImageArchive() throws Exception {
        imageArchive = new File(resoutFolder, "images.CrAr");
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(imageArchive)));
        out.writeInt(toNativeEndian(imageFiles.size()));
        for (int i = 0; i < imageFiles.size(); i++) {
            File f = imageFiles.get(i);
            out.writeLong(toNativeEndian(f.length()));
            out.writeLong(toNativeEndian(new File(resFolder, f.getName().substring(0, f.getName().length() - 5)).length()));
        }
        for (int i = 0; i < imageFiles.size(); i++) {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(imageFiles.get(i)));
            int read;
            while ((read = in.read()) != -1) {
                out.write(read);
            }
            in.close();
        }
        out.close();
    }

    private int toNativeEndian(int x) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(x);
        b.order(ByteOrder.nativeOrder());
        return b.getInt(0);
    }

    private long toNativeEndian(long x) {
        ByteBuffer b = ByteBuffer.allocate(8);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putLong(x);
        b.order(ByteOrder.nativeOrder());
        return b.getLong(0);
    }

    private void appendArchive() throws Exception {
        String cmd;
        if (profile == CompilationProfile.UNIX_GCC) {
            cmd = "cat";
        } else if (profile == CompilationProfile.MINGW_WINDOWS) {
            cmd = "type";
        } else {
            throw new Exception("Unknown cat equivalent for profile " + profile);
        }
        compFrame.writeLine("<span style='color: green;'>" + cmd + " \"" + imageArchive.getAbsolutePath() + "\" >> \"" + outputFile.getAbsolutePath() + "\"</span>");
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile, true));
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(imageArchive));
        int read;
        while ((read = in.read()) != -1) {
            out.write(read);
        }
        in.close();
        out.close();
    }

    private void copyScript(ProjectElement e) throws Exception {
        compFrame.writeLine("Copying PineDL script " + e.getName());
        File f = new File(outputFolder, e.getName());
        FileOutputStream fos = new FileOutputStream(f);
        InputStream is = e.getFile().getReader();
        int c = 0;
        while ((c = is.read()) != -1) {
            fos.write(c);
        }
        pineScripts.add(f);
        fos.close();
    }

    private void createActorScript(ProjectElement e) throws Exception {
        Actor a = new Actor(e.getFile());
        clsres.put(a.getName(), a);
        String fname = e.getName();
        fname = fname.substring(0, fname.lastIndexOf('.'));
        fname = fname.replaceAll("\\s", "_");
        compFrame.writeLine("Creating PineDL Script for actor " + fname);
        File f = new File(outputFolder, fname + ".pdl");
        PrintWriter w = new PrintWriter(f);
        w.print("package ");
        w.print(gamePackage);
        w.println(';');
        w.println();
        w.print("class ");
        w.print(fname);
        w.println(" extends Actor {");
        w.println();
        printFields(a.fields, w);
        w.println();
        boolean hasCreate = false;
        for (Event evt : a.events) {
            if (evt.getType().equals(Event.TYPE_CREATE)) {
                hasCreate = true;
                printCreateEvent(w, a, evt);
            }
            if (evt.getType().equals(Event.TYPE_UPDATE)) {
                w.println("\tpublic void update() {");
                w.print(outputEvent(a, evt));
                w.println("\t}");
            }
            if (evt.getType().equals(Event.TYPE_DRAW)) {
                w.println("\tpublic void draw(Drawing d) {");
                w.print(outputEvent(a, evt));
                w.println("\t}");
            }
            if (evt.getType().equals(Event.TYPE_KEYPRESS)) {
                w.print("\tpublic void onKeyDown(int key) {");
                w.print(outputEvent(a, evt));
                w.println("\t}");
            }
            if (evt.getType().equals(Event.TYPE_KEYRELEASE)) {
                w.println("\tpublic void onKeyUp(int key) {");
                w.print(outputEvent(a, evt));
                w.println("\t}");
            }
            if (evt.getType().equals(Event.TYPE_KEYPRESSED)) {
                w.println("\tpublic void onKeyPressed(int key) {");
                w.print(outputEvent(a, evt));
                w.println("\t}");
            }
            if (evt.getType().equals(Event.TYPE_COLLISION)) {
                w.println("\tpublic void handleCollision(Actor other) {");
                w.print(outputEvent(a, evt));
                w.println("\t}");
            }
        }
        if (!hasCreate) {
            printCreateEvent(w, a, null);
        }
        w.println('}');
        pineScripts.add(f);
        w.close();
    }

    private void printCreateEvent(PrintWriter w, Actor a, Event evt) throws IOException {
        w.println("\tpublic this(float x, float y, float depth) : super(x, y, depth) {");
        w.println("\t\tsetDepth(depth);");
        if (a.getImage() != null) {
            w.print(("\t\ttexture = TextureList."));
            String iname = a.getImage().getName();
            int index = iname.indexOf('.');
            iname = iname.substring(0, index) + "_" + iname.substring(index + 1);
            w.print(iname);
            w.println(";");
        }
        if (evt != null) {
            w.print(outputEvent(a, evt));
        }
        w.println("\t}");
    }

    private void createSceneScript(ProjectElement e) throws Exception {
        Scene scene = new Scene(e.getFile());
        clsres.put(e.getName().substring(0, e.getName().indexOf('.')), scene);
        if (e.getFile().getPath().equals(PineappleCore.getProject().getSettings().get("scene-order").split(";")[0])) {
            mainScene = scene;
        }
        String fname = e.getName();
        fname = fname.substring(0, fname.lastIndexOf('.'));
        fname = fname.replaceAll("\\s", "_");
        compFrame.writeLine("Creating PineDL Script for scene " + fname);
        File f = new File(outputFolder, fname + ".pdl");
        PrintWriter w = new PrintWriter(f);
        w.print("package ");
        w.print(gamePackage);
        w.println(';');
        w.println();
        w.print("class ");
        w.print(fname);
        w.println(" extends Scene {");
        w.println();
        printFields(scene.fields, w);
        w.println();
        boolean hasCreate = false;
        for (Event evt : scene.events) {
            if (evt.getType().equals(Event.TYPE_CREATE)) {
                w.print("\tpublic this() : super");
                w.print("(" + scene.getWidth() + ", " + scene.getHeight());
                w.println("){");
                w.print(evt.getPineDL() + '\n');
                w.println("}");
                w.println();
                hasCreate = true;
            } else if (evt.getType().equals(Event.TYPE_UPDATE)) {
                w.println("\tpublic void update(){");
                w.print(evt.getPineDL() + '\n');
                w.println("}");
                w.println();
            } else if (evt.getType().equals(Event.TYPE_DRAW)) {
                w.println("\tpublic void draw(Drawing d){");
                w.print(evt.getPineDL() + '\n');
                w.println("}");
                w.println();
            } else if (evt.getType().equals(Event.TYPE_KEYPRESS)) {
                w.println("\tpublic void onKeyDown(int key){");
                w.print(evt.getPineDL() + '\n');
                w.println("}");
                w.println();
            } else if (evt.getType().equals(Event.TYPE_KEYRELEASE)) {
                w.println("\tpublic void onKeyUp(int key){");
                w.print(evt.getPineDL() + '\n');
                w.println("}");
                w.println();
            } else if (evt.getType().equals(Event.TYPE_KEYPRESSED)) {
                w.println("\tpublic void onKeyPressed(int key){");
                w.print(evt.getPineDL() + '\n');
                w.println("}");
                w.println();
            }
        }
        if (!hasCreate) {
            w.print("\tpublic this() : super");
            w.print("(" + scene.getWidth() + ", " + scene.getHeight());
            w.println("){");
            w.println("setupScene();");
            w.println("}");
            w.println();
        }
        w.println("private void setupScene(){");
        Color c = scene.getBackgroundColor();
        String cs = ((Integer) c.getRed()).toString();
        cs += ", ";
        cs += c.getGreen();
        cs += ", ";
        cs += c.getBlue();
        w.println("\t\tsetBackground(new Color(" + cs + "));");
        for (Scene.ActorInScene a : scene.actors) {
            String aname = a.file.getName();
            aname = aname.substring(0, aname.lastIndexOf('.'));
            w.println("\t\taddActor(new " + aname + "(" + a.x + ", " + a.y + ", " + a.actor.getZ() + "));");
        }
        for (Scene.Background b : scene.backgrounds) {
            String iname = b.image.getName().replaceAll("\\.", "_");
            w.println("\t\taddBackground(new Background(TextureList." + iname + ", " + b.x + ", " + b.y + ", " + b.drawImage + ", " + b.hrepeat + ", " + b.vrepeat + "));");
        }
        w.println("\t}");
        w.println("}");
        w.close();
        pineScripts.add(f);
    }

    private String outputEvent(Actor a, Event evt) {
        return evt.getPineDL() + "\n";
    }

    private void prepare() throws Exception {
        compFrame.writeLine("<b>Preparing game compilation</b>");
        outputFolder = new File(p.getProjectFolder(), "output/cpp-opengl/");
        outputFolder.mkdirs();
        binFolder = new File(outputFolder, "bin");
        binFolder.mkdir();
        resFolder = new File(outputFolder, "res");
        resFolder.mkdir();
        resoutFolder = new File(outputFolder, "res-output");
        resoutFolder.mkdir();
        compConf = new File(outputFolder, "compile.conf");
        if (profile == CompilationProfile.UNIX_GCC) {
            outputFile = new File(binFolder, "game");
        } else if (profile == CompilationProfile.MINGW_WINDOWS) {
            outputFile = new File(binFolder, "game.exe");
        } else {
            throw new Exception("Invalid Compilation Profile; output file not set");
        }
        compFrame.writeLine("Loading config...");
        loadConfig();
        compFrame.writeLine("Generating header...");
        headerH = new PrintWriter(new File(outputFolder, "header.h"));
        headerH.println("#ifndef _PINEAPPLE_HEADER_H_");
        headerH.println("#define _PINEAPPLE_HEADER_H_");
        headerH.println("#include \"actor.h\"");
        headerH.println("#include \"array.h\"");
        headerH.println("#include \"application.h\"");
        headerH.println("#include \"background.h\"");
        headerH.println("#include \"collision.h\"");
        headerH.println("#include \"color.h\"");
        headerH.println("#include \"drawing.h\"");
        headerH.println("#include \"exceptions.h\"");
        headerH.println("#include \"io.h\"");
        headerH.println("#include \"keyboard.h\"");
        headerH.println("#include \"keycodes.h\"");
        headerH.println("#include \"pamath.h\"");
        headerH.println("#include \"scene.h\"");
        headerH.println("#include \"texture.h\"");
        headerH.println("#include \"timer.h\"");
        headerH.println("#include \"texturelist.h\"");
        headerH.println("#include \"vector.h\"");
        headerH.println("#include \"view.h\"");
        headerH.println("#include \"window.h\"");
        headerH.println("#include \"shapes.h\"");
    }

    private void loadConfig() {
        if (!compConf.exists()) {
            return;
        }
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(compConf));
            String l;
            while ((l = r.readLine()) != null) {
                if (l.matches("\\W*#.*")) {
                    continue;
                }
                String[] line = l.split("=");
                if (line.length < 2) {
                    System.out.println("Error: invalid line: " + l);
                    continue;
                }
                String var = line[0];
                String value = line[1];
                for (int i = 2; i < line.length; i++) {
                    value += line[i];
                }
                config.put(var, value);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GameCompiler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GameCompiler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Logger.getLogger(GameCompiler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void saveConfig() {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(compConf));
            for (String s : config.keySet()) {
                w.write(s);
                w.write("=");
                w.write(config.get(s));
                w.newLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(GameCompiler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException ex) {
                    Logger.getLogger(GameCompiler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void convertImage(BasicFile src, File dest) throws IOException {
        compFrame.writeLine("Converting image to PNG " + src.getName());
        BufferedImage img = ImageIO.read(src.getReader());
        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.getGraphics().drawImage(img, 0, 0, null);
        ImageIO.write(copy, "PNG", dest);
    }

    private void printFields(Vector<Field> fields, PrintWriter w) {
        for (Field v : fields) {
            w.print("\t");
            if (v.getAccess() == ClassResource.Field.Access.PRIVATE) {
                w.print("private ");
            } else if (v.getAccess() == ClassResource.Field.Access.PROTECTED) {
                w.print("protected ");
            } else {
                w.print("public ");
            }
            if (v.isStatic()) {
                w.print("static ");
            }
            if (v.isFinal()) {
                w.print("final ");
            }
            w.print(v.getType() + " " + v.getName());
            if (v.getDefaultValue() != null && !v.getDefaultValue().equals("")) {
                w.print(" = " + v.getDefaultValue());
            }
            w.println(";");
        }
    }
}
