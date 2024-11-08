package org.snipsnap.graph;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import org.snipsnap.graph.builder.*;
import org.snipsnap.graph.context.*;
import org.snipsnap.graph.graph.uml.*;
import org.snipsnap.graph.renderer.*;

public class TestUML {

    private static final String PREFIX = "examples/Bsp11";

    private ImageIcon imageIcon;

    public static void main(String[] args) {
        TestUML t = new TestUML();
        t.createImage();
        t.showImage();
        System.out.println("ready");
    }

    private void createImage() {
        ReadUMLFile readFile = new ReadUMLFile();
        UMLGraph umlGraph = null;
        umlGraph = readFile.read(PREFIX + ".org");
        try {
            UMLGraphRenderer graphRenderer = new UMLGraphRenderer();
            RendererContext umlContext = new UMLRendererContext();
            graphRenderer.render(umlGraph, new FileOutputStream(PREFIX + ".png"), umlContext);
        } catch (IOException e) {
            System.err.println("Cannot open file.");
        }
    }

    private void showImage() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        imageIcon = new ImageIcon(PREFIX + ".png", "Bild");
        int width = imageIcon.getIconWidth();
        int height = imageIcon.getIconHeight();
        frame.setBounds(0, 0, width + 10, height + 20);
        frame.setContentPane(makeContentPane());
        frame.setVisible(true);
    }

    private Container makeContentPane() {
        JPanel panel = new JPanel() {

            public void paintComponent(Graphics g) {
                Image image = imageIcon.getImage();
                super.paintComponent(g);
                g.drawImage(image, 0, 0, this);
            }
        };
        return panel;
    }

    private static void error(String[] args) {
        if (args.length != 2) {
            System.out.println("The program needs two filenames. The first one is the file," + " where to read from, the second one is, where to write on!!");
            System.exit(0);
        }
    }
}
