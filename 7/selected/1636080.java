package org.silentsquare.codejam.y2009.round2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class CrazyRows {

    private static final String PATH = "./src/org/silentsquare/codejam/y2009/round2/";

    private BufferedReader in;

    private PrintWriter out;

    public void solveSmall() throws IOException {
        _solve("small");
    }

    public void solveLarge() throws IOException {
        _solve("large");
    }

    public void _solve(String name) throws IOException {
        System.out.println("Solving the " + name + " dataset ...");
        long begin = System.currentTimeMillis();
        in = new BufferedReader(new FileReader(PATH + "A-" + name + "-practice.in"));
        out = new PrintWriter(new BufferedWriter(new FileWriter(PATH + "A-" + name + "-practice.out")));
        int tests = Integer.parseInt(in.readLine().trim());
        for (int i = 0; i < tests; i++) {
            int[] rows = readRows();
            int min = minSteps(rows);
            out.println("Case #" + (i + 1) + ": " + min);
        }
        in.close();
        out.close();
        System.out.println("Solving the " + name + " dataset: " + (System.currentTimeMillis() - begin) + "ms");
    }

    private int[] readRows() throws IOException {
        int n = Integer.parseInt(in.readLine().trim());
        int[] rows = new int[n];
        for (int i = 0; i < n; i++) {
            char[] cs = in.readLine().trim().toCharArray();
            int rightmost = 0;
            for (int j = 0; j < n; j++) {
                if (cs[j] == '1') rightmost = j;
            }
            rows[i] = rightmost;
        }
        return rows;
    }

    private int minSteps(int[] rows) {
        allNodes.clear();
        Node root = getNode(rows);
        root.steps = 0;
        PriorityQueue<Node> pq = new PriorityQueue<Node>();
        pq.add(root);
        while (true) {
            Node head = pq.remove();
            if (!head.isCrazy()) return head.steps;
            List<Node> neighbours = head.neighbours();
            for (Node node : neighbours) {
                if (!node.isCrazy()) return head.steps + 1;
                if (node.steps > head.steps + 1) {
                    pq.remove(node);
                    node.steps = head.steps + 1;
                    pq.add(node);
                }
            }
        }
    }

    private Map<Node, Node> allNodes = new HashMap<Node, Node>();

    private Node getNode(int[] rows) {
        Node node = new Node(rows);
        if (allNodes.containsKey(node)) return allNodes.get(node); else {
            allNodes.put(node, node);
            return node;
        }
    }

    private class Node implements Comparable<Node> {

        int[] rows;

        int steps;

        Node(int[] rows) {
            this.rows = rows;
            this.steps = Integer.MAX_VALUE;
        }

        public List<Node> neighbours() {
            List<Node> neighbours = new ArrayList<Node>();
            for (int i = 0; i < rows.length - 1; i++) {
                int[] newrows = Arrays.copyOf(rows, rows.length);
                int temp = newrows[i];
                newrows[i] = newrows[i + 1];
                newrows[i + 1] = temp;
                neighbours.add(getNode(newrows));
            }
            return neighbours;
        }

        boolean isCrazy() {
            for (int i = 0; i < rows.length; i++) if (rows[i] > i) return true;
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(rows);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Node)) return false;
            Node node = (Node) obj;
            if (node == this) return true;
            return Arrays.equals(rows, node.rows);
        }

        @Override
        public int compareTo(Node node) {
            return this.steps - node.steps;
        }
    }

    /**
	 * @param args
	 * @throws IOException 
	 */
    public static void main(String[] args) throws IOException {
        new CrazyRows().solveSmall();
    }
}
