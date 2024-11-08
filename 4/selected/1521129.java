package org.snipsnap.graph;

public class TestHorizontal {

    public static void main(String[] args) {
        error(args);
        ReadFile readFile = new ReadFile();
        Tree myTree = readFile.read(args[0]);
        Renderer renderer = new HorizontalRenderer();
        DrawTree drawTree = new DrawTree();
        drawTree.draw(myTree, renderer, args[1]);
    }

    private static void error(String[] args) {
        if (args.length != 2) {
            System.out.println("The program needs two filenames. The first one is the file," + " where to read from, the second one is, where to write on!!");
            System.exit(0);
        }
    }
}
