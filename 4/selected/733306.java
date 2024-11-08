package vizz3d_data.layouts.ccvisu;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/*****************************************************************
 * Main class of the CCVisu package.
 * Contains the main pogram and some auxiliary methods.
 * @version  $Revision: 1.71 $; $Date: 2005/08/22 12:30:54 $
 * @author   Dirk Beyer
 *****************************************************************/
public class CCVisu {

    /** End of line.*/
    public static final String endl = System.getProperty("line.separator");

    /** CVS log format (only input).*/
    private static final int CVS = 0;

    /** Graph (relation) in relational standard format.*/
    private static final int RSF = 1;

    /** Graph layout in textual format.*/
    private static final int LAY = 2;

    /** Graph layout in VRML format (only output).*/
    private static final int VRML = 3;

    /** Graph layout in SVG format (only output).*/
    private static final int SVG = 4;

    /** Display gaph layout on screen (only output).*/
    private static final int DISP = 5;

    /*****************************************************************
   * Main program. Performes the following steps.
   * 1) Parses and handles the command line options.
   * 2) Creates the appropriate input reader and reads the input.
   * 3) Computes the layout (if necessary).
   * 4) Creates the appropriate output writer and writes the output.
   * @param args  Command line arguments.
   *****************************************************************/
    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            System.exit(0);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
        Verbosity.level = 0;
        int inFormat = RSF;
        int outFormat = DISP;
        int timeWindow = 180000;
        int nrDim = 2;
        int nrIterations = 50;
        int attrExponent = 1;
        boolean vertRepu = false;
        boolean noWeight = false;
        float gravitation = 0.001f;
        boolean hideSource = false;
        float minVert = 7.0f;
        int fontSize = 14;
        Color backColor = Color.WHITE;
        boolean blackCircle = true;
        float scalePos = 1.0f;
        boolean anim = false;
        boolean annotAll = false;
        boolean annotNone = false;
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--help")) {
                printHelp();
                out.close();
                System.exit(0);
            } else if (args[i].equalsIgnoreCase("-v") || args[i].equalsIgnoreCase("--version")) {
                printVersion();
                out.close();
                System.exit(0);
            } else if (args[i].equalsIgnoreCase("-q") || args[i].equalsIgnoreCase("--nowarnings")) {
                Verbosity.level = 0;
            } else if (args[i].equalsIgnoreCase("-w") || args[i].equalsIgnoreCase("--warnings")) {
                Verbosity.level = 1;
            } else if (args[i].equalsIgnoreCase("-verbose")) {
                Verbosity.level = 2;
            } else if (args[i].equalsIgnoreCase("-i")) {
                ++i;
                chkAvail(args, i);
                try {
                    in = new BufferedReader(new FileReader(args[i]));
                } catch (Exception e) {
                    System.err.println("Exception while opening file '" + args[i] + "' for reading: ");
                    System.err.println(e);
                }
            } else if (args[i].equalsIgnoreCase("-o")) {
                ++i;
                chkAvail(args, i);
                try {
                    out = new PrintWriter(new BufferedWriter(new FileWriter(args[i])));
                } catch (Exception e) {
                    System.err.println("Exception while opening file '" + args[i] + "' for writing: ");
                    System.err.println(e);
                }
            } else if (args[i].equalsIgnoreCase("-inFormat")) {
                ++i;
                chkAvail(args, i);
                inFormat = getFormat(args[i]);
                if (inFormat > LAY) {
                    System.err.println("Usage error: '" + args[i] + "' is not supported as input format.");
                    System.exit(1);
                }
            } else if (args[i].equalsIgnoreCase("-outFormat")) {
                ++i;
                chkAvail(args, i);
                outFormat = getFormat(args[i]);
                if (outFormat < RSF) {
                    System.err.println("Usage error: '" + args[i] + "' is not supported as output format.");
                    System.exit(1);
                }
            } else if (args[i].equalsIgnoreCase("-timeWindow")) {
                ++i;
                chkAvail(args, i);
                timeWindow = Integer.parseInt(args[i]);
            } else if (args[i].equalsIgnoreCase("-dim")) {
                ++i;
                chkAvail(args, i);
                nrDim = Integer.parseInt(args[i]);
            } else if (args[i].equalsIgnoreCase("-iter")) {
                ++i;
                chkAvail(args, i);
                nrIterations = Integer.parseInt(args[i]);
            } else if (args[i].equalsIgnoreCase("-attrExp")) {
                ++i;
                chkAvail(args, i);
                attrExponent = Integer.parseInt(args[i]);
            } else if (args[i].equalsIgnoreCase("-vertRepu")) {
                vertRepu = true;
            } else if (args[i].equalsIgnoreCase("-noWeight")) {
                noWeight = true;
            } else if (args[i].equalsIgnoreCase("-grav")) {
                ++i;
                chkAvail(args, i);
                gravitation = Float.parseFloat(args[i]);
            } else if (args[i].equalsIgnoreCase("-hideSource")) {
                hideSource = true;
            } else if (args[i].equalsIgnoreCase("-minVert")) {
                ++i;
                chkAvail(args, i);
                minVert = Float.parseFloat(args[i]);
            } else if (args[i].equalsIgnoreCase("-fontSize")) {
                ++i;
                chkAvail(args, i);
                fontSize = Integer.parseInt(args[i]);
            } else if (args[i].equalsIgnoreCase("-backcolor")) {
                ++i;
                chkAvail(args, i);
                if (args[i].equalsIgnoreCase("black")) {
                    backColor = Color.BLACK;
                } else if (args[i].equalsIgnoreCase("white")) {
                    backColor = Color.WHITE;
                } else if (args[i].equalsIgnoreCase("gray")) {
                    backColor = Color.GRAY;
                } else if (args[i].equalsIgnoreCase("lightgray")) {
                    backColor = Color.LIGHT_GRAY;
                } else {
                    System.err.println("Usage error: Color '" + args[i] + "' unknown.");
                }
            } else if (args[i].equalsIgnoreCase("-noBlackCircle")) {
                blackCircle = false;
            } else if (args[i].equalsIgnoreCase("-scalePos")) {
                ++i;
                chkAvail(args, i);
                scalePos = Float.parseFloat(args[i]);
            } else if (args[i].equalsIgnoreCase("-anim")) {
                anim = true;
            } else if (args[i].equalsIgnoreCase("-annotAll")) {
                annotAll = true;
            } else if (args[i].equalsIgnoreCase("-annotNone")) {
                annotNone = true;
            } else {
                System.err.println("Usage error: Option '" + args[i] + "' unknown.");
                System.exit(1);
            }
        }
        if (inFormat > outFormat) {
            System.err.println("Usage error: Combination of input and output formats not supported.");
            System.exit(1);
        }
        ReaderData graphReader;
        graphReader = new ReaderDataGraphCVS(in, timeWindow);
        if (inFormat == RSF) {
            graphReader = new ReaderDataGraphRSF(in);
        } else if (inFormat == LAY) {
            graphReader = new ReaderDataLAY(in);
        }
        GraphData graph = graphReader.read();
        if (Verbosity.level >= 2) {
            System.err.println("Graph reading finished.");
        }
        try {
            in.close();
        } catch (Exception e) {
            System.err.println("Exception while closing input file: ");
            System.err.println(e);
        }
        markSpecial(graph);
        if (annotAll) {
            for (int i = 0; i < graph.vertices.size(); ++i) {
                GraphVertex curVertex = (GraphVertex) graph.vertices.get(i);
                curVertex.showName = true;
            }
        }
        if (annotNone) {
            for (int i = 0; i < graph.vertices.size(); ++i) {
                GraphVertex curVertex = (GraphVertex) graph.vertices.get(i);
                curVertex.showName = false;
            }
        }
        WriterData dataWriter = null;
        if (inFormat < LAY && outFormat >= LAY) {
            initializeLayout(graph, nrDim);
            if (outFormat == DISP && anim) {
                dataWriter = new WriterDataGraphicsDISP(graph, hideSource, minVert, fontSize, backColor, blackCircle, anim);
            }
            computeLayout(graph, nrIterations, attrExponent, vertRepu, noWeight, gravitation);
        }
        if (outFormat == RSF) {
            dataWriter = new WriterDataRSF(graph, out);
        } else if (outFormat == LAY) {
            dataWriter = new WriterDataLAY(graph, out, hideSource);
        } else if (outFormat == VRML) {
            dataWriter = new WriterDataGraphicsVRML(graph, out, hideSource, minVert, fontSize, backColor, blackCircle, scalePos);
        } else if (outFormat == SVG) {
            dataWriter = new WriterDataGraphicsSVG(graph, out, hideSource, minVert, fontSize, backColor, blackCircle, scalePos);
        } else if (outFormat == DISP && dataWriter == null) {
            dataWriter = new WriterDataGraphicsDISP(graph, hideSource, minVert, fontSize, backColor, blackCircle, anim);
        }
        dataWriter.write();
        out.close();
    }

    /*****************************************************************
   * Prints version information.
   *****************************************************************/
    private static void printVersion() {
        System.out.println("CCVisu 1.0, 2005-08-22. " + endl + "Copyright (C) 2005  Dirk Beyer (EPFL, Lausanne, Switzerland). " + endl + "CCVisu is free software, released under the GNU LGPL.");
    }

    /*****************************************************************
   * Prints usage information.
   *****************************************************************/
    private static void printHelp() {
        System.out.print(endl + "This is CCVisu, a tool for Co-Change Visualization " + endl + "and general force-directed graph layout. " + endl + "   " + endl + "Usage: java CCVisu [OPTION]... " + endl + "Compute a layout for a given (co-change) graph (or convert). " + endl + "   " + endl + "Options: " + endl + "General options: " + endl + "   -h  --help        display this help message and exit. " + endl + "   -v  --version     print version information and exit. " + endl + "   -q  --nowarnings  quiet mode (default). " + endl + "   -w  --warnings    enable warnings. " + endl + "   -verbose          verbose mode. " + endl + "   -i <file>         read input data from given file (default: stdin). " + endl + "   -o <file>         write output data to given file (default: stdout). " + endl + "   -inFormat FORMAT  read input data in format FORMAT (default: RSF, see below). " + endl + "   -outFormat FORMAT write output data in format FORMAT (default: DISP, see below). " + endl + "   " + endl + "Layouting options: " + endl + "   -dim <int>        number of dimensions of the layout (2 or 3, default: 2). " + endl + "   -iter <int>       number of iterations to run the minimizer (default: 50). " + endl + "   " + endl + "Energy model options: " + endl + "   -attrExp <int>    exponent for the distance in the attraction term " + endl + "                     (default: 1). " + endl + "   -vertRepu         use vertex repulsion instead of edge repulsion " + endl + "                     (default: edge repulsion). " + endl + "   -noWeight         use unweighted model (default: weighted). " + endl + "   -grav <float>     gravitation factor for the Barnes-Hut-procedure " + endl + "                     (default: 0.001). " + endl + "   " + endl + "CVS reader option: " + endl + "   -timeWindow <int> time window for transaction recovery, in milli-seconds " + endl + "                     (default: 180'000). " + endl + "   " + endl + "Layout writer options: " + endl + "   -hideSource       hide source vertices (default: show). " + endl + "   -minVert <float>  size of the smallest vertex (diameter, default: 7.0). " + endl + "   -fontSize <int>   font size of vertex annotations (default: 14). " + endl + "   -backColor COLOR  background color (default: WHITE). " + endl + "                     Colors: BLACK, GRAY, LIGHTGRAY, WHITE." + endl + "   -noBlackCircle    no black circle around each vertex (default: with). " + endl + "   -scalePos <float> scaling factor for the layout to adjust " + endl + "                     (VRML and SVG only, default: 1.0). " + endl + "   -anim             show layout while minimizer is still improving it " + endl + "                     (default: no). " + endl + "   -annotAll         annotate each vertex with its name (default: no). " + endl + "   -annotNone        annotate no vertex (default: no). " + endl + "   " + endl + "Formats: " + endl + "   CVS               CVS log format (only input). " + endl + "   RSF               graph in relational standard format. " + endl + "   LAY               graph layout in textual format. " + endl + "   VRML              graph layout in VRML format (only output). " + endl + "   SVG               graph layout in SVG format (only output). " + endl + "   DISP              display gaph layout on screen (only output). " + endl + "To produce a file for input format CVS log, use e.g. 'cvs log -Nb'. " + endl + "   " + endl + "http://mtc.epfl.ch/~beyer/CCVisu/ " + endl + "   " + endl + "Report bugs to <Dirk.Beyer@epfl.ch>. " + endl + "   " + endl);
    }

    /*****************************************************************
   * Transforms the format given as a string into the appropriate integer value.
   * @param format  File format string to be transformed to int.
   * @return        File format identifier.
   *****************************************************************/
    private static int getFormat(String format) {
        int result = 0;
        if (format.equalsIgnoreCase("CVS")) {
            result = CVS;
        } else if (format.equalsIgnoreCase("RSF")) {
            result = RSF;
        } else if (format.equalsIgnoreCase("LAY")) {
            result = LAY;
        } else if (format.equalsIgnoreCase("VRML")) {
            result = VRML;
        } else if (format.equalsIgnoreCase("SVG")) {
            result = SVG;
        } else if (format.equalsIgnoreCase("DISP")) {
            result = DISP;
        } else {
            System.err.println("Usage error: '" + format + "' is not a valid format.");
            System.exit(1);
        }
        return result;
    }

    /*****************************************************************
   * Checks whether the command line argument at index i has a follower argument.
   * If there is no follower argument, it exits the program.
   * @param args  String array containing the command line arguments.
   * @param i     Index to check.
   *****************************************************************/
    private static void chkAvail(String[] args, int i) {
        if (i == args.length) {
            System.err.println("Usage error: Option '" + args[i - 1] + "' requires an argument (file).");
            System.exit(1);
        }
    }

    /*****************************************************************
   * Compute randomized initial layout for a given graph 
   * with the given number of dimensions.
   * @param graph  Graph representation, in/out parameter.
   * @param nrDim  Number of dimensions for the initial graph.
   *****************************************************************/
    public static void initializeLayout(GraphData graph, int nrDim) {
        graph.pos = new float[graph.vertices.size()][3];
        for (int i = 0; i < graph.vertices.size(); ++i) {
            graph.pos[i][0] = 2 * (float) Math.random() - 1;
            if (nrDim >= 2) {
                graph.pos[i][1] = 2 * (float) Math.random() - 1;
            } else {
                graph.pos[i][2] = 0;
            }
            if (nrDim == 3) {
                graph.pos[i][2] = 2 * (float) Math.random() - 1;
            } else {
                graph.pos[i][2] = 0;
            }
        }
    }

    /*****************************************************************
   * Compute layout for a given graph.
   * @param graph         In/Out parameter representing the graph.
   * @param nrIterations  Number of iterations.
   * @param attrExponent  Exponent of the Euclidian distance in the attraction term
   *                      of the energy (default: 1).
   * @param vertRepu      Use vertex repulsion instead of edge repulsion,
   *                      true for vertex repulsion, false for edge repulsion
   *                      (default: edge repulsion).
   * @param noWeight      Use unweighted model by ignoring the edge weights,
   *                      true for unweighted, false for weighted 
   *                      (default: weighted).
   * @param gravitation   Gravitation factor for the Barnes-Hut-procedure,
   *                      attraction to the barycenter
   *                      (default: 0.001).
   *****************************************************************/
    public static void computeLayout(GraphData graph, int nrIterations, int attrExponent, boolean vertRepu, boolean noWeight, float gravitation) {
        int verticeNr = graph.vertices.size();
        float[] repu = new float[verticeNr];
        for (int i = 0; i < verticeNr; ++i) {
            if (vertRepu) {
                repu[i] = 1.0f;
            } else {
                GraphVertex curVertex = (GraphVertex) graph.vertices.get(i);
                repu[i] = curVertex.degree;
            }
        }
        int[][] attrIndexes = new int[verticeNr][];
        float[][] attrValues = new float[verticeNr][];
        {
            int[] attrCounter = new int[verticeNr];
            for (int i = 0; i < graph.edges.size(); ++i) {
                GraphEdgeInt e = (GraphEdgeInt) graph.edges.get(i);
                if (e.x == e.y) {
                    if (Verbosity.level >= 1) {
                        GraphVertex curVertex = (GraphVertex) graph.vertices.get(e.x);
                        System.err.println("Layout warning: Reflexive edge for vertex '" + curVertex.name + "' found.");
                    }
                } else {
                    ++attrCounter[e.x];
                    ++attrCounter[e.y];
                }
            }
            for (int i = 0; i < verticeNr; i++) {
                attrIndexes[i] = new int[attrCounter[i]];
                attrValues[i] = new float[attrCounter[i]];
            }
            attrCounter = new int[verticeNr];
            for (int i = 0; i < graph.edges.size(); ++i) {
                GraphEdgeInt e = (GraphEdgeInt) graph.edges.get(i);
                if (e.x != e.y) {
                    attrIndexes[e.x][attrCounter[e.x]] = e.y;
                    attrIndexes[e.y][attrCounter[e.y]] = e.x;
                    if (noWeight) {
                        attrValues[e.x][attrCounter[e.x]] = 1.0f;
                        attrValues[e.y][attrCounter[e.y]] = 1.0f;
                    } else {
                        attrValues[e.x][attrCounter[e.x]] = e.w;
                        attrValues[e.y][attrCounter[e.y]] = e.w;
                    }
                    ++attrCounter[e.x];
                    ++attrCounter[e.y];
                }
            }
        }
        boolean[] fixedPos = new boolean[verticeNr];
        Minimizer minimizer = new MinimizerBarnesHut(verticeNr, attrIndexes, attrValues, repu, graph.pos, fixedPos, attrExponent, gravitation);
        minimizer.minimizeEnergy(nrIterations);
    }

    /*****************************************************************
   * Special marking for certain vertices by setting attributes of the vertex,
   * e.g., color and showName. 
   * This method is a user-defined method to implement some 
   * 'highlighting' functionality.
   *****************************************************************/
    private static void markSpecial(GraphData graph) {
        for (int i = 0; i < graph.vertices.size(); ++i) {
            GraphVertex curVertex = (GraphVertex) graph.vertices.get(i);
        }
    }
}

;
