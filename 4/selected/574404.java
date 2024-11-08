package org.snipsnap.graph;

import java.io.FileOutputStream;
import java.io.IOException;
import org.snipsnap.graph.builder.*;
import org.snipsnap.graph.context.*;
import org.snipsnap.graph.graph.*;
import org.snipsnap.graph.renderer.*;

public class TestAll {

    private static final String PREFIX = "examples";

    public static void main(String[] args) {
        ReadTreeFile readFile = new ReadTreeFile();
        Tree myTree = readFile.read(PREFIX + "/Tree1.org");
        RendererContext urlcontext;
        RendererContext imageContext;
        Renderer htmlMapRenderer;
        try {
            Renderer horizontalRenderer = new HorizontalRenderer();
            htmlMapRenderer = new HtmlMapRenderer();
            urlcontext = new UrlContext("zufallszahl", horizontalRenderer);
            imageContext = new GraphRendererContext();
            htmlMapRenderer.render(myTree, new FileOutputStream(PREFIX + "/UrlHori.txt"), urlcontext);
            horizontalRenderer.render(myTree, new FileOutputStream(PREFIX + "/Hori.png"), imageContext);
            Renderer verticalRenderer = new VerticalRenderer();
            htmlMapRenderer = new HtmlMapRenderer();
            urlcontext = new UrlContext("zufallszahl", verticalRenderer);
            imageContext = new GraphRendererContext();
            ((PicInfo) imageContext.getPicInfo()).setVLimit(3);
            ((PicInfo) urlcontext.getPicInfo()).setVLimit(3);
            htmlMapRenderer.render(myTree, new FileOutputStream(PREFIX + "/UrlVert.txt"), urlcontext);
            verticalRenderer.render(myTree, new FileOutputStream(PREFIX + "/Vert.png"), imageContext);
            Renderer explorerRenderer = new ExplorerRenderer();
            htmlMapRenderer = new HtmlMapRenderer();
            urlcontext = new UrlContext("zufallszahl", explorerRenderer);
            imageContext = new GraphRendererContext();
            htmlMapRenderer.render(myTree, new FileOutputStream(PREFIX + "/UrlExpl.txt"), urlcontext);
            explorerRenderer.render(myTree, new FileOutputStream(PREFIX + "/Expl.png"), imageContext);
            Renderer mindMapRenderer = new MindMapRenderer();
            htmlMapRenderer = new HtmlMapRenderer();
            urlcontext = new UrlContext("zufallszahl", mindMapRenderer);
            imageContext = new GraphRendererContext();
            htmlMapRenderer.render(myTree, new FileOutputStream(PREFIX + "/UrlMindMap.txt"), urlcontext);
            mindMapRenderer.render(myTree, new FileOutputStream(PREFIX + "/MindMap.png"), imageContext);
        } catch (IOException e) {
            System.err.println("Cannot open file.");
        }
        System.out.println("ready");
        ReadDirectedAcyclicGraphFile readFile1 = new ReadDirectedAcyclicGraphFile();
        DirectedGraph directedGraph = null;
        for (int i = 0; i <= 0; i++) {
            System.out.println(i);
            directedGraph = readFile1.read(PREFIX + "/Graph.org");
            try {
                DirectedAcyclicGraphRenderer graphRenderer = new DirectedAcyclicGraphRenderer();
                htmlMapRenderer = new HtmlMapRenderer();
                urlcontext = new UrlContext("zufallszahl", graphRenderer);
                imageContext = new GraphRendererContext();
                htmlMapRenderer.render(directedGraph, new FileOutputStream(PREFIX + "/UrlGraph.txt"), urlcontext);
                graphRenderer.render(directedGraph, new FileOutputStream(PREFIX + "/Graph.png"), imageContext);
            } catch (IOException e) {
                System.err.println("Cannot open file.");
            }
        }
        System.out.println("ready");
    }

    private static void error(String[] args) {
        if (args.length != 2) {
            System.out.println("The program needs two filenames. The first one is the file," + " where to read from, the second one is, where to write on!!");
            System.exit(0);
        }
    }
}
