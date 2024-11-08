package eu.cherrytree.paj.gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.FloatBuffer;
import javax.media.opengl.GL2;
import eu.cherrytree.paj.utilities.BufferUtil;
import eu.cherrytree.paj.base.AppDefinition;
import eu.cherrytree.paj.base.AppState;
import eu.cherrytree.paj.graphics.Graphics;
import eu.cherrytree.paj.graphics.ImageManager;
import eu.cherrytree.paj.utilities.Timer;

public class Console {

    public enum State {

        HIDDEN, SHOWN, OPEN
    }

    ;

    private static boolean intiated = false;

    private static State consoleState;

    private static float timepassed;

    private static float timeout;

    private static float resolutionX;

    private static int maxVisibleLines;

    private static int numberOfLinesEntered;

    private static final float backgroundImageWidth = 256.0f;

    private static final float backgroundImageHeight = 256.0f;

    private static int background;

    private static float[] vertices = { 0.0f, 0.0f, -0.1f, 0.0f, 0.0f, -0.1f, 0.0f, 0.0f, -0.1f, 0.0f, 0.0f, -0.1f };

    private static FloatBuffer backgroundVertices;

    private static FloatBuffer backgroundTextureCoordinates;

    private static TextArea area;

    private static String[] text;

    private static boolean logging;

    private static boolean flushToStd;

    private static PrintWriter logFile = null;

    private static boolean consoleAutoOpen = false;

    public Console(float resX, float resY, int maxlines, float fontHeight, float time) {
        resolutionX = resX;
        maxVisibleLines = maxlines;
        timeout = time;
        numberOfLinesEntered = 0;
        consoleState = State.HIDDEN;
        area = new TextArea(AppDefinition.getConsoleFontPath(), AppDefinition.getConsoleFontSize(), AppDefinition.getConsoleFontColor()[0], AppDefinition.getConsoleFontColor()[1], AppDefinition.getConsoleFontColor()[2], AppDefinition.getConsoleFontColor()[3], 1);
        area.setSize(resX, resY);
        area.setPosition(0.0f, 0.0f);
        background = ImageManager.loadUnindexedImage(AppDefinition.getDefaultDataPackagePath() + "/images/static/" + AppDefinition.getConsoleBackroundImagePath(), true, true);
        text = new String[maxlines];
        vertices[3] = resX;
        vertices[6] = resX;
        setVertexBuffer();
        intiated = true;
        print("Console initiated");
    }

    public void destroy() {
        int[] back = { background };
        Graphics.getGL().glDeleteTextures(1, back, 0);
        area.destroy();
        if (logging) stopLog();
    }

    public void setConsoleAutoOpen(boolean auto_open) {
        consoleAutoOpen = auto_open;
    }

    private static void setVertexBuffer() {
        backgroundVertices = BufferUtil.newFloatBuffer(vertices.length);
        backgroundVertices.put(vertices);
        backgroundVertices.flip();
    }

    private static void setTextArea() {
        switch(consoleState) {
            case HIDDEN:
                break;
            case SHOWN:
                area.setText(text[maxVisibleLines - 1]);
                break;
            case OPEN:
                {
                    String fulltext = new String();
                    for (int i = 0; i < maxVisibleLines - 1; i++) if (text[i] != null && text[i].length() > 0) fulltext += text[i] + "\n";
                    fulltext += text[maxVisibleLines - 1];
                    area.setText(fulltext);
                    break;
                }
        }
    }

    public static void print(String str) {
        if (logging) {
            if (logFile != null || logFile.checkError()) {
                logFile.println(Timer.getTime() + ": " + str);
            } else {
                stopLog();
                print("Logging failed.");
            }
        }
        if (flushToStd) System.out.println(AppDefinition.getApplicationShortName() + " [" + Timer.getTime() + "]: " + str);
        if (!intiated) {
            return;
        }
        if (numberOfLinesEntered < maxVisibleLines) numberOfLinesEntered++;
        for (int i = 0; i < maxVisibleLines - 1; i++) text[i] = text[i + 1];
        text[maxVisibleLines - 1] = str;
        if (consoleState == State.HIDDEN && consoleAutoOpen) openConsole();
        if (consoleState != State.HIDDEN) {
            setTextArea();
            vertices[7] = area.getActualHeight() + 5.0f;
            vertices[10] = area.getActualHeight() + 5.0f;
        }
    }

    public static State getConsoleState() {
        return consoleState;
    }

    public static void openConsole() {
        switch(consoleState) {
            case HIDDEN:
                consoleState = State.SHOWN;
                setTextArea();
                timepassed = timeout;
                vertices[7] = area.getActualHeight() + 5.0f;
                vertices[10] = area.getActualHeight() + 5.0f;
                setVertexBuffer();
                break;
            case SHOWN:
                consoleState = State.OPEN;
                setTextArea();
                timepassed = timeout;
                vertices[7] = area.getActualHeight() + 5.0f;
                vertices[10] = area.getActualHeight() + 5.0f;
                setVertexBuffer();
                break;
            case OPEN:
                timepassed = timeout;
                break;
        }
    }

    public static void closeConsole() {
        consoleState = State.HIDDEN;
        timepassed = 0;
    }

    public static void setFlushToStd(boolean flushToStd) {
        Console.flushToStd = flushToStd;
    }

    public static boolean isFlushToStd() {
        return flushToStd;
    }

    public static boolean isLoging() {
        return logging;
    }

    public static void startLog(String homedir) {
        logging = true;
        String logspath = homedir + "logs/";
        (new File(logspath)).mkdir();
        logspath += "console/";
        (new File(logspath)).mkdir();
        String filename = logspath + "(" + Timer.getDate() + ")" + ".log";
        if (logFile != null) stopLog();
        try {
            logFile = new PrintWriter(new FileWriter(filename, true));
        } catch (IOException e) {
            print("Couldn't start log.");
            logging = false;
            return;
        }
        logFile.println("--------------(Started at: " + Timer.getTime() + ")---------------");
    }

    public static void stopLog() {
        logging = false;
        if (logFile != null) {
            logFile.println("--------------(Ended at:   " + Timer.getTime() + ")---------------");
            logFile.close();
            logFile = null;
        }
    }

    public static void flush(float frame) {
        switch(consoleState) {
            case HIDDEN:
                break;
            case SHOWN:
                timepassed -= frame * 4;
                setTexture();
                display();
                if (timepassed < 0) {
                    timepassed = 0;
                    consoleState = State.HIDDEN;
                }
                break;
            case OPEN:
                timepassed -= frame;
                setTexture();
                display();
                if (timepassed < 0) {
                    timepassed = 0;
                    consoleState = State.SHOWN;
                }
                break;
        }
    }

    public static void setTexture() {
        float height = area.getActualHeight();
        long time = Timer.millisPassed() / 10;
        float shiftX = ((float) (time % 1000) / 1000.0f);
        float shiftY = (float) Math.sin(Math.PI * ((float) (time % 1000) / 1000.0f));
        float[] uv = { 0.0f + shiftX, height / backgroundImageHeight + shiftY, resolutionX / backgroundImageWidth + shiftX, height / backgroundImageHeight + shiftY, resolutionX / backgroundImageWidth + shiftX, 0.0f + shiftY, 0.0f + shiftX, 0.0f + shiftY };
        backgroundTextureCoordinates = BufferUtil.newFloatBuffer(uv.length);
        backgroundTextureCoordinates.put(uv);
        backgroundTextureCoordinates.flip();
    }

    public static void display() {
        GL2 gl = Graphics.getGL();
        gl.glBindTexture(GL2.GL_TEXTURE_2D, background);
        gl.glPushMatrix();
        gl.glTranslatef(0.0f, 0.0f, -0.5f);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, backgroundVertices);
        gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, backgroundTextureCoordinates);
        gl.glDrawArrays(GL2.GL_QUADS, 0, 4);
        gl.glPopMatrix();
        area.display();
    }
}
