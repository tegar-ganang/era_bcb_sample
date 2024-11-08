package com.tomjudge.gui.splash;

import java.awt.Canvas;
import java.awt.Graphics;
import java.net.URL;

/**
 *
 * @author  Tom Judge
 */
public class StatusSplashScreen extends SplashScreen {

    private String[] lines;

    private int pos;

    /** Creates a new instance of StatusSplashScreen
     * @param image Image to be displayed.
     * @param width Width of the screen
     * @param height Height of the screen
     */
    public StatusSplashScreen(URL image, int width, int height) {
        super(image, width, height);
        pos = 0;
        lines = new String[6];
        for (int i = 0; i < 6; i++) {
            lines[i] = new String();
        }
        this.remove(canvas);
        canvas = new Canvas() {

            @Override
            public void paint(Graphics g) {
                g.drawImage(splashLogo, 0, 0, null);
                for (int i = 0; i < 6; i++) {
                    g.setColor(java.awt.Color.BLACK);
                    g.drawString(lines[i], 10, 215 + (14 * i));
                }
            }

            @Override
            public void update(Graphics g) {
                paint(g);
            }
        };
        canvas.setSize(width, height);
        add(canvas);
        this.invalidate();
    }

    /**
     * Add a message to the splash screen
     * @param message The message to add.
     */
    public void logMessage(String message) {
        if (pos == 6) {
            for (int i = 0; i < 5; i++) {
                lines[i] = lines[i + 1];
            }
            lines[5] = message;
        } else {
            lines[pos] = message;
            pos++;
        }
        canvas.update(canvas.getGraphics());
    }
}
