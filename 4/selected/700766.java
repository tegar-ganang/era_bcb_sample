package org.sosy_lab.ccvisu.readers;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import org.sosy_lab.ccvisu.graph.GraphData;
import org.sosy_lab.ccvisu.graph.GraphVertex;
import org.sosy_lab.ccvisu.graph.Group;
import org.sosy_lab.ccvisu.writers.WriterDataLayoutDISP;
import org.sosy_lab.util.Colors;

/**
 * A class that save and load group information in WriterDataGraphicsDISP
 */
public class ReaderWriterGroup {

    /**
   * read from a stream, build the groups and put them in the Writer...DISP
   * @param reader the input stream
   * @param writer the WriterDataGraphicsDISP that contains the group
   */
    public static void read(BufferedReader reader, WriterDataLayoutDISP writer) {
        while (writer.options.graph.getNumberOfGroups() > 1) {
            writer.options.graph.removeGroup(1);
        }
        try {
            Group currentGroup = null;
            String line = null;
            GraphData graph = writer.graph;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0 && line.charAt(0) == '\t') {
                    String name = line.substring(1, line.length());
                    GraphVertex currentVertex = graph.getVertexByName(name);
                    if (currentVertex != null) {
                        currentGroup.addNode(currentVertex);
                    }
                } else {
                    StringTokenizer st = new StringTokenizer(line, "\t");
                    currentGroup = new Group(st.nextToken(), writer.graph);
                    currentGroup.setColor(Colors.valueOfUpper(st.nextToken()).get());
                    currentGroup.visible = (Boolean.valueOf(st.nextToken())).booleanValue();
                    currentGroup.info = (Boolean.valueOf(st.nextToken())).booleanValue();
                    writer.options.graph.addGroup(currentGroup);
                }
            }
        } catch (Exception e) {
            System.err.println("Exception while reading (ReaderWriterGroup.read): ");
            System.err.println(e);
        }
    }

    /**
   * write on a stream the informations needed to rebuild the groups later
   * @param out    the output stream
   * @param writer the WriterDataGraphicsDISP that contains the group
   */
    public static void write(PrintWriter out, WriterDataLayoutDISP writer) {
        int end = writer.options.graph.getNumberOfGroups();
        for (int i = 1; i < end; i++) {
            Group currGroup = writer.options.graph.getGroup(i);
            out.println(currGroup.getName() + "\t" + (currGroup.getColor().toString()) + "\t" + currGroup.visible + "\t" + currGroup.info);
            for (GraphVertex curVertex : currGroup) {
                out.println("\t" + curVertex.getName());
            }
        }
    }
}
