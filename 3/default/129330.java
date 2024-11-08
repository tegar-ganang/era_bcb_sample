import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import gnu.getopt.*;

interface Metrics {

    int calculate(List<List<Graph.Vertex>> cycles);
}

class MetricSumSquares implements Metrics {

    private List<List<Graph.Vertex>> cycles;

    private int sumOfSquares;

    public int calculate(List<List<Graph.Vertex>> cycles) {
        sumOfSquares = 0;
        for (List<Graph.Vertex> cycle : cycles) sumOfSquares += cycle.size() * cycle.size();
        this.cycles = cycles;
        return sumOfSquares;
    }

    public String toString() {
        int[] groups = new int[cycles.size()];
        String str = "[ ";
        for (int j = 0; j < cycles.size(); j++) groups[j] = cycles.get(j).size();
        Arrays.sort(groups);
        str = str + sumOfSquares + " :";
        for (int j = groups.length - 1; j >= 0; j--) str = str + " " + groups[j];
        return str + " ]";
    }
}

;

class MetricUsersTrading implements Metrics {

    private int count;

    public int calculate(List<List<Graph.Vertex>> cycles) {
        HashSet<String> users = new HashSet<String>();
        for (List<Graph.Vertex> cycle : cycles) for (Graph.Vertex vert : cycle) users.add(vert.user);
        count = users.size();
        return -count;
    }

    public String toString() {
        return "[ users trading = " + count + " ]";
    }
}

;

class MetricCombineShipping implements Metrics {

    private int count;

    public int calculate(List<List<Graph.Vertex>> cycles) {
        Map<String, Integer> pairs = new HashMap<String, Integer>();
        for (List<Graph.Vertex> cycle : cycles) for (Graph.Vertex vert : cycle) {
            String key = vert.user + " receives " + vert.match.twin.user;
            pairs.put(key, 1 + (pairs.containsKey(key) ? pairs.get(key) : 0));
        }
        count = 0;
        Iterator iter = pairs.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            int value = pairs.get(key).intValue();
            if (value > 1) count = count + value - 1;
        }
        return -count;
    }

    public String toString() {
        return "[ combine shipping = " + count + " ]";
    }
}

;

class MetricFavorUser implements Metrics {

    private String user;

    private int count;

    public MetricFavorUser(String user) {
        this.user = "(" + user + ")";
    }

    public int calculate(List<List<Graph.Vertex>> cycles) {
        Map<String, Integer> users = new HashMap<String, Integer>();
        Integer xyz = null;
        int sum = 0;
        for (List<Graph.Vertex> cycle : cycles) for (Graph.Vertex vert : cycle) {
            String user = vert.user.toUpperCase();
            users.put(user, 1 + (users.containsKey(user) ? users.get(user) : 0));
        }
        xyz = users.get(this.user);
        if (xyz != null) count = xyz.intValue(); else count = 0;
        return -count;
    }

    public String toString() {
        return "[ " + user + " trading = " + count + " ]";
    }
}

;

class MetricUsersSumOfSquares implements Metrics {

    private int sum;

    private int count;

    public int calculate(List<List<Graph.Vertex>> cycles) {
        Map<String, Integer> users = new HashMap<String, Integer>();
        sum = 0;
        for (List<Graph.Vertex> cycle : cycles) for (Graph.Vertex vert : cycle) users.put(vert.user, 1 + (users.containsKey(vert.user) ? users.get(vert.user) : 0));
        count = users.size();
        for (Integer user : users.values()) sum += user.intValue() * user.intValue();
        return sum;
    }

    public String toString() {
        return "[ users trading = " + count + ", sum of squares = " + sum + " ]";
    }
}

;

public class TradeMaximizer {

    public static void main(String[] args) {
        new TradeMaximizer().run(args);
    }

    final String version = "Version 1.3a";

    Metrics metric = new MetricSumSquares();

    void run(String[] args) {
        InputStream istream = System.in;
        System.out.println("TradeMaximizer " + version);
        String filename = parseArgs(args, false);
        if (filename != null) {
            System.out.println("Input from: " + filename);
            try {
                if (filename.startsWith("http:") || filename.startsWith("ftp:")) {
                    URL url = new URL(filename);
                    istream = url.openStream();
                } else istream = new FileInputStream(filename);
            } catch (IOException ex) {
                fatalError(ex.toString());
            }
        }
        List<String[]> wantLists = readWantLists(istream);
        if (wantLists == null) return;
        if (options.size() > 0) {
            System.out.print("Options:");
            for (String option : options) System.out.print(" " + option);
            System.out.println();
        }
        System.out.println();
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (String[] wset : wantLists) {
                for (String w : wset) {
                    digest.update((byte) ' ');
                    digest.update(w.getBytes());
                }
                digest.update((byte) '\n');
            }
            System.out.println("Input Checksum: " + toHexString(digest.digest()));
        } catch (NoSuchAlgorithmException ex) {
        }
        parseArgs(args, true);
        if (iterations > 1 && seed == -1) {
            seed = System.currentTimeMillis();
            System.out.println("No explicit SEED, using " + seed);
        }
        if (!(metric instanceof MetricSumSquares) && priorityScheme != NO_PRIORITIES) System.out.println("Warning: using priorities with the non-default metric is normally worthless");
        buildGraph(wantLists);
        if (showMissing && officialNames != null && officialNames.size() > 0) {
            for (String name : usedNames) officialNames.remove(name);
            List<String> missing = new ArrayList<String>(officialNames);
            Collections.sort(missing);
            for (String name : missing) {
                System.out.println("**** Missing want list for official name " + name);
            }
            System.out.println();
        }
        if (showErrors && errors.size() > 0) {
            Collections.sort(errors);
            System.out.println("ERRORS:");
            for (String error : errors) System.out.println(error);
            System.out.println();
        }
        long startTime = System.currentTimeMillis();
        graph.removeImpossibleEdges();
        List<List<Graph.Vertex>> bestCycles = graph.findCycles();
        int bestMetric = metric.calculate(bestCycles);
        if (iterations > 1) {
            System.out.println(metric);
            graph.saveMatches();
            for (int i = 0; i < iterations - 1; i++) {
                graph.shuffle();
                List<List<Graph.Vertex>> cycles = graph.findCycles();
                int newMetric = metric.calculate(cycles);
                if (newMetric < bestMetric) {
                    bestMetric = newMetric;
                    bestCycles = cycles;
                    graph.saveMatches();
                    System.out.println(metric);
                } else if (verbose) System.out.println("# " + metric);
            }
            System.out.println();
            graph.restoreMatches();
        }
        long stopTime = System.currentTimeMillis();
        displayMatches(bestCycles);
        if (showElapsedTime) System.out.println("Elapsed time = " + (stopTime - startTime) + "ms");
    }

    boolean caseSensitive = false;

    boolean requireColons = false;

    boolean requireUsernames = false;

    boolean showErrors = true;

    boolean showRepeats = true;

    boolean showLoops = true;

    boolean showSummary = true;

    boolean showNonTrades = true;

    boolean showStats = true;

    boolean showMissing = false;

    boolean sortByItem = false;

    boolean allowDummies = false;

    boolean showElapsedTime = false;

    long seed = -1;

    static final int NO_PRIORITIES = 0;

    static final int LINEAR_PRIORITIES = 1;

    static final int TRIANGLE_PRIORITIES = 2;

    static final int SQUARE_PRIORITIES = 3;

    static final int SCALED_PRIORITIES = 4;

    static final int EXPLICIT_PRIORITIES = 5;

    int priorityScheme = NO_PRIORITIES;

    int smallStep = 1;

    int bigStep = 9;

    long nonTradeCost = 1000000000L;

    int iterations = 1;

    boolean verbose = false;

    boolean debug = false;

    List<String> options = new ArrayList<String>();

    HashSet<String> officialNames = null;

    List<String> usedNames = new ArrayList<String>();

    List<String[]> readWantLists(InputStream istream) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(istream));
            List<String[]> wantLists = new ArrayList<String[]>();
            boolean readingOfficialNames = false;
            for (int lineNumber = 1; ; lineNumber++) {
                String line = in.readLine();
                if (line == null) return wantLists;
                line = line.trim();
                if (line.length() == 0) continue;
                if (line.matches("#!.*")) {
                    if (wantLists.size() > 0) fatalError("Options (#!...) cannot be declared after first real want list", lineNumber);
                    if (officialNames != null) fatalError("Options (#!...) cannot be declared after official names", lineNumber);
                    for (String option : line.toUpperCase().substring(2).trim().split("\\s+")) {
                        if (option.equals("CASE-SENSITIVE")) caseSensitive = true; else if (option.equals("REQUIRE-COLONS")) requireColons = true; else if (option.equals("REQUIRE-USERNAMES")) requireUsernames = true; else if (option.equals("HIDE-ERRORS")) showErrors = false; else if (option.equals("HIDE-REPEATS")) showRepeats = false; else if (option.equals("HIDE-LOOPS")) showLoops = false; else if (option.equals("HIDE-SUMMARY")) showSummary = false; else if (option.equals("HIDE-NONTRADES")) showNonTrades = false; else if (option.equals("HIDE-STATS")) showStats = false; else if (option.equals("SHOW-MISSING")) showMissing = true; else if (option.equals("SORT-BY-ITEM")) sortByItem = true; else if (option.equals("ALLOW-DUMMIES")) allowDummies = true; else if (option.equals("SHOW-ELAPSED-TIME")) showElapsedTime = true; else if (option.equals("LINEAR-PRIORITIES")) priorityScheme = LINEAR_PRIORITIES; else if (option.equals("TRIANGLE-PRIORITIES")) priorityScheme = TRIANGLE_PRIORITIES; else if (option.equals("SQUARE-PRIORITIES")) priorityScheme = SQUARE_PRIORITIES; else if (option.equals("SCALED-PRIORITIES")) priorityScheme = SCALED_PRIORITIES; else if (option.equals("EXPLICIT-PRIORITIES")) priorityScheme = EXPLICIT_PRIORITIES; else if (option.startsWith("SMALL-STEP=")) {
                            String num = option.substring(11);
                            if (!num.matches("\\d+")) fatalError("SMALL-STEP argument must be a non-negative integer", lineNumber);
                            smallStep = Integer.parseInt(num);
                        } else if (option.startsWith("BIG-STEP=")) {
                            String num = option.substring(9);
                            if (!num.matches("\\d+")) fatalError("BIG-STEP argument must be a non-negative integer", lineNumber);
                            bigStep = Integer.parseInt(num);
                        } else if (option.startsWith("NONTRADE-COST=")) {
                            String num = option.substring(14);
                            if (!num.matches("[1-9]\\d*")) fatalError("NONTRADE-COST argument must be a positive integer", lineNumber);
                            nonTradeCost = Long.parseLong(num);
                        } else if (option.startsWith("ITERATIONS=")) {
                            String num = option.substring(11);
                            if (!num.matches("[1-9]\\d*")) fatalError("ITERATIONS argument must be a positive integer", lineNumber);
                            iterations = Integer.parseInt(num);
                        } else if (option.startsWith("SEED=")) {
                            String num = option.substring(5);
                            if (!num.matches("[1-9]\\d*")) fatalError("SEED argument must be a positive integer", lineNumber);
                            seed = Long.parseLong(num);
                            graph.setSeed(seed);
                        } else if (option.equals("VERBOSE")) verbose = true; else if (option.equals("DEBUG")) debug = true; else if (option.startsWith("METRIC=")) {
                            String met = option.substring(7);
                            if (met.matches("USERS-TRADING")) metric = new MetricUsersTrading(); else if (met.matches("USERS-SOS")) metric = new MetricUsersSumOfSquares(); else if (met.startsWith("FAVOR-USER=")) {
                                String user = met.substring(11);
                                metric = new MetricFavorUser(user);
                            } else if (met.matches("CHAIN-SIZES-SOS")) {
                            } else fatalError("Unknown metric option \"" + met + "\"", lineNumber);
                        } else fatalError("Unknown option \"" + option + "\"", lineNumber);
                        options.add(option);
                    }
                    continue;
                }
                if (line.matches("#.*")) continue;
                if (line.indexOf("#") != -1) {
                    if (readingOfficialNames) {
                        if (line.split("[:\\s]")[0].indexOf("#") != -1) {
                            fatalError("# symbol cannot be used in an item name", lineNumber);
                        }
                    } else fatalError("Comments (#...) cannot be used after beginning of line", lineNumber);
                }
                if (line.equalsIgnoreCase("!BEGIN-OFFICIAL-NAMES")) {
                    if (officialNames != null) fatalError("Cannot begin official names more than once", lineNumber);
                    if (wantLists.size() > 0) fatalError("Official names cannot be declared after first real want list", lineNumber);
                    officialNames = new HashSet<String>();
                    readingOfficialNames = true;
                    continue;
                }
                if (line.equalsIgnoreCase("!END-OFFICIAL-NAMES")) {
                    if (!readingOfficialNames) fatalError("!END-OFFICIAL-NAMES without matching !BEGIN-OFFICIAL-NAMES", lineNumber);
                    readingOfficialNames = false;
                    continue;
                }
                if (readingOfficialNames) {
                    if (line.charAt(0) == ':') fatalError("Line cannot begin with colon", lineNumber);
                    if (line.charAt(0) == '%') fatalError("Cannot give official names for dummy items", lineNumber);
                    String[] toks = line.split("[:\\s]");
                    String name = toks[0];
                    if (!caseSensitive) name = name.toUpperCase();
                    if (officialNames.contains(name)) fatalError("Official name " + name + "+ already defined", lineNumber);
                    officialNames.add(name);
                    continue;
                }
                if (line.indexOf("(") == -1 && requireUsernames) fatalError("Missing username with REQUIRE-USERNAMES selected", lineNumber);
                if (line.charAt(0) == '(') {
                    if (line.lastIndexOf("(") > 0) fatalError("Cannot have more than one '(' per line", lineNumber);
                    int close = line.indexOf(")");
                    if (close == -1) fatalError("Missing ')' in username", lineNumber);
                    if (close == line.length() - 1) fatalError("Username cannot appear on a line by itself", lineNumber);
                    if (line.lastIndexOf(")") > close) fatalError("Cannot have more than one ')' per line", lineNumber);
                    if (close == 1) fatalError("Cannot have empty parentheses", lineNumber);
                    if (line.indexOf(" ") < close) {
                        line = line.substring(0, close + 1).replaceAll(" ", "#") + " " + line.substring(close + 1);
                    }
                } else if (line.indexOf("(") > 0) fatalError("Username can only be used at the front of a want list", lineNumber); else if (line.indexOf(")") > 0) fatalError("Bad ')' on a line that does not have a '('", lineNumber);
                line = line.replaceAll(";", " ; ");
                int semiPos = line.indexOf(";");
                if (semiPos != -1) {
                    if (semiPos < line.indexOf(":")) fatalError("Semicolon cannot appear before colon", lineNumber);
                    String before = line.substring(0, semiPos).trim();
                    if (before.length() == 0 || before.charAt(before.length() - 1) == ')') fatalError("Semicolon cannot appear before first item on line", lineNumber);
                }
                int colonPos = line.indexOf(":");
                if (colonPos != -1) {
                    if (line.lastIndexOf(":") != colonPos) fatalError("Cannot have more that one colon on a line", lineNumber);
                    String header = line.substring(0, colonPos).trim();
                    if (!header.matches("(.*\\)\\s+)?[^(\\s)]\\S*")) fatalError("Must have exactly one item before a colon (:)", lineNumber);
                    line = line.replaceFirst(":", " ");
                } else if (requireColons) {
                    fatalError("Missing colon with REQUIRE-COLONS selected", lineNumber);
                }
                if (!caseSensitive) line = line.toUpperCase();
                wantLists.add(line.trim().split("\\s+"));
            }
        } catch (Exception e) {
            fatalError(e.getMessage());
            return null;
        }
    }

    String parseArgs(String[] args, boolean doit) {
        int c, optind;
        LongOpt[] longopts = new LongOpt[22];
        longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
        longopts[1] = new LongOpt("allow-dummies", LongOpt.OPTIONAL_ARGUMENT, null, 'd');
        longopts[2] = new LongOpt("require-colons", LongOpt.OPTIONAL_ARGUMENT, null, 'c');
        longopts[3] = new LongOpt("require-usernames", LongOpt.OPTIONAL_ARGUMENT, null, 'u');
        longopts[4] = new LongOpt("hide-loops", LongOpt.OPTIONAL_ARGUMENT, null, 'l');
        longopts[5] = new LongOpt("hide-summary", LongOpt.OPTIONAL_ARGUMENT, null, 's');
        longopts[6] = new LongOpt("hide-nontrades", LongOpt.OPTIONAL_ARGUMENT, null, 'n');
        longopts[7] = new LongOpt("hide-errors", LongOpt.OPTIONAL_ARGUMENT, null, 'e');
        longopts[8] = new LongOpt("hide-stats", LongOpt.OPTIONAL_ARGUMENT, null, 't');
        longopts[9] = new LongOpt("hide-repeats", LongOpt.OPTIONAL_ARGUMENT, null, 'r');
        longopts[10] = new LongOpt("case-sensitive", LongOpt.OPTIONAL_ARGUMENT, null, 'C');
        longopts[11] = new LongOpt("sort-by-item", LongOpt.OPTIONAL_ARGUMENT, null, 'i');
        longopts[12] = new LongOpt("small-step", LongOpt.REQUIRED_ARGUMENT, null, 'm');
        longopts[13] = new LongOpt("big-step", LongOpt.REQUIRED_ARGUMENT, null, 'b');
        longopts[14] = new LongOpt("nontrade-cost", LongOpt.REQUIRED_ARGUMENT, null, 'N');
        longopts[15] = new LongOpt("seed", LongOpt.REQUIRED_ARGUMENT, null, 'S');
        longopts[16] = new LongOpt("iterations", LongOpt.REQUIRED_ARGUMENT, null, 'I');
        longopts[17] = new LongOpt("priorities", LongOpt.OPTIONAL_ARGUMENT, null, 'p');
        longopts[18] = new LongOpt("show-missing", LongOpt.OPTIONAL_ARGUMENT, null, 'G');
        longopts[19] = new LongOpt("show-elapsed-time", LongOpt.OPTIONAL_ARGUMENT, null, 'T');
        longopts[20] = new LongOpt("metric", LongOpt.REQUIRED_ARGUMENT, null, 'M');
        longopts[21] = new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v');
        Getopt g = new Getopt("TradeMaximizer", args, "hdculsnetrCim:b:N:S:I:p:GTM:v", longopts);
        while ((c = g.getopt()) != -1) {
            String arg = g.getOptarg();
            boolean bool = true;
            if (arg != null && (arg.equalsIgnoreCase("false") || arg.equalsIgnoreCase("off") || arg.equals("0"))) bool = false;
            if (doit) switch(c) {
                case 'h':
                    System.err.println("TradeMaximizer " + version + "\n" + "Please see http://www.boardgamegeek.com/wiki/page/TradeMaximizer for details\n" + "on each option.  For binary style options optional argument can be\n" + "'false', 'off' or '0', anything else ia sssumed true/on");
                    int i;
                    for (i = 0; i < longopts.length; i++) {
                        System.out.println("    -" + (char) longopts[i].getVal() + " --" + longopts[i].getName());
                    }
                    break;
                case 'd':
                    allowDummies = bool;
                    break;
                case 'c':
                    requireColons = bool;
                    break;
                case 'u':
                    requireUsernames = bool;
                    break;
                case 'l':
                    showLoops = !bool;
                    break;
                case 's':
                    showSummary = !bool;
                    break;
                case 'n':
                    showNonTrades = !bool;
                    break;
                case 'e':
                    showErrors = !bool;
                    break;
                case 't':
                    showStats = !bool;
                    break;
                case 'r':
                    showRepeats = !bool;
                    break;
                case 'C':
                    caseSensitive = bool;
                    break;
                case 'i':
                    sortByItem = bool;
                    break;
                case 'm':
                    smallStep = Integer.parseInt(arg);
                    break;
                case 'b':
                    bigStep = Integer.parseInt(arg);
                    break;
                case 'N':
                    nonTradeCost = Long.parseLong(arg);
                    break;
                case 'I':
                    iterations = Integer.parseInt(arg);
                    break;
                case 'G':
                    showMissing = bool;
                    break;
                case 'v':
                    verbose = bool;
                    break;
                case 'T':
                    showElapsedTime = bool;
                    break;
                case 'S':
                    seed = Long.parseLong(arg);
                    graph.setSeed(seed);
                    break;
                case 'p':
                    if (arg == null) arg = "linear";
                    if (arg.equalsIgnoreCase("linear")) priorityScheme = LINEAR_PRIORITIES; else if (arg.equalsIgnoreCase("triangle")) priorityScheme = TRIANGLE_PRIORITIES; else if (arg.equalsIgnoreCase("square")) priorityScheme = SQUARE_PRIORITIES; else if (arg.equalsIgnoreCase("scaled")) priorityScheme = SCALED_PRIORITIES; else if (arg.equalsIgnoreCase("explicit")) priorityScheme = EXPLICIT_PRIORITIES; else if (arg.equalsIgnoreCase("none")) priorityScheme = NO_PRIORITIES; else fatalError("Unknown priority type: " + arg);
                    break;
                case 'M':
                    String met = arg.toUpperCase();
                    if (met.matches("USERS-TRADING")) metric = new MetricUsersTrading(); else if (met.matches("USERS-SOS")) metric = new MetricUsersSumOfSquares(); else if (met.matches("COMBINE-SHIPPING")) metric = new MetricCombineShipping(); else if (met.startsWith("FAVOR-USER=")) {
                        String user = met.substring(11);
                        metric = new MetricFavorUser(user);
                    } else if (met.matches("CHAIN-SIZES-SOS")) metric = new MetricSumSquares(); else fatalError("Unknown metric: " + met);
                    break;
                case '?':
                    fatalError("Exiting due to unknown or badly form command line option");
                    break;
                default:
                    break;
            }
        }
        optind = g.getOptind();
        return optind < args.length ? args[optind] : null;
    }

    void fatalError(String msg) {
        System.out.println();
        System.out.println("FATAL ERROR: " + msg);
        System.exit(1);
    }

    void fatalError(String msg, int lineNumber) {
        fatalError(msg + " (line " + lineNumber + ")");
    }

    Graph graph = new Graph();

    List<String> errors = new ArrayList<String>();

    final long INFINITY = 100000000000000L;

    final long UNIT = 1L;

    int ITEMS;

    int DUMMY_ITEMS;

    String[] deleteFirst(String[] a) {
        assert a.length > 0;
        String[] b = new String[a.length - 1];
        for (int i = 0; i < b.length; i++) b[i] = a[i + 1];
        return b;
    }

    void buildGraph(List<String[]> wantLists) {
        HashMap<String, Integer> unknowns = new HashMap<String, Integer>();
        for (int i = 0; i < wantLists.size(); i++) {
            String[] list = wantLists.get(i);
            assert list.length > 0;
            String name = list[0];
            String user = null;
            int offset = 0;
            if (name.charAt(0) == '(') {
                user = name.replaceAll("#", " ");
                list = deleteFirst(list);
                wantLists.set(i, list);
                name = list[0];
            }
            boolean isDummy = (name.charAt(0) == '%');
            if (isDummy) {
                if (user == null) errors.add("**** Dummy item " + name + " declared without a username."); else if (!allowDummies) errors.add("**** Dummy items not allowed. (" + name + ")"); else {
                    name += " for user " + user;
                    list[0] = name;
                }
            }
            if (officialNames != null && !officialNames.contains(name) && name.charAt(0) != '%') {
                errors.add("**** Cannot define want list for " + name + " because it is not an official name.  (Usually indicates a typo by the item owner.)");
                wantLists.set(i, null);
            } else if (graph.getVertex(name) != null) {
                errors.add("**** Item " + name + " has multiple want lists--ignoring all but first.  (Sometimes the result of an accidental line break in the middle of a want list.)");
                wantLists.set(i, null);
            } else {
                ITEMS++;
                if (isDummy) DUMMY_ITEMS++;
                Graph.Vertex vertex = graph.addVertex(name, user, isDummy);
                if (officialNames != null && officialNames.contains(name)) usedNames.add(name);
                if (!isDummy) width = Math.max(width, show(vertex).length());
            }
        }
        for (String[] list : wantLists) {
            if (list == null) continue;
            String fromName = list[0];
            Graph.Vertex fromVertex = graph.getVertex(fromName);
            graph.addEdge(fromVertex, fromVertex.twin, nonTradeCost);
            long rank = 1;
            for (int i = 1; i < list.length; i++) {
                String toName = list[i];
                if (toName.equals(";")) {
                    rank += bigStep;
                    continue;
                }
                if (toName.indexOf('=') >= 0) {
                    if (priorityScheme != EXPLICIT_PRIORITIES) {
                        errors.add("**** Cannot use '=' annotation in item " + toName + " in want list for item " + fromName + " unless using EXPLICIT_PRIORITIES.");
                        continue;
                    }
                    if (!toName.matches("[^=]+=[0-9]+")) {
                        errors.add("**** Item " + toName + " in want list for item " + fromName + " must have the format 'name=number'.");
                        continue;
                    }
                    String[] parts = toName.split("=");
                    assert (parts.length == 2);
                    long explicitCost = Long.parseLong(parts[1]);
                    if (explicitCost < 1) {
                        errors.add("**** Explicit priority must be positive in item " + toName + " in want list for item " + fromName + ".");
                        continue;
                    }
                    rank = explicitCost;
                    toName = parts[0];
                }
                if (toName.charAt(0) == '%') {
                    if (fromVertex.user == null) {
                        errors.add("**** Dummy item " + toName + " used in want list for item " + fromName + ", which does not have a username.");
                        continue;
                    }
                    toName += " for user " + fromVertex.user;
                }
                Graph.Vertex toVertex = graph.getVertex(toName);
                if (toVertex == null) {
                    if (officialNames != null && officialNames.contains(toName)) {
                        rank += smallStep;
                    } else {
                        int occurrences = unknowns.containsKey(toName) ? unknowns.get(toName) : 0;
                        unknowns.put(toName, occurrences + 1);
                    }
                    continue;
                }
                toVertex = toVertex.twin;
                if (toVertex == fromVertex.twin) {
                    errors.add("**** Item " + toName + " appears in its own want list.");
                } else if (graph.getEdge(fromVertex, toVertex) != null) {
                    if (showRepeats) errors.add("**** Item " + toName + " is repeated in want list for " + fromName + ".");
                } else if (!toVertex.isDummy && fromVertex.user != null && fromVertex.user.equals(toVertex.user)) {
                    errors.add("**** Item " + fromVertex.name + " contains item " + toVertex.name + " from the same user (" + fromVertex.user + ")");
                } else {
                    long cost = UNIT;
                    switch(priorityScheme) {
                        case LINEAR_PRIORITIES:
                            cost = rank;
                            break;
                        case TRIANGLE_PRIORITIES:
                            cost = rank * (rank + 1) / 2;
                            break;
                        case SQUARE_PRIORITIES:
                            cost = rank * rank;
                            break;
                        case SCALED_PRIORITIES:
                            cost = rank;
                            break;
                        case EXPLICIT_PRIORITIES:
                            cost = rank;
                            break;
                    }
                    if (fromVertex.isDummy) cost = nonTradeCost;
                    graph.addEdge(fromVertex, toVertex, cost);
                    rank += smallStep;
                }
            }
            if (!fromVertex.isDummy) {
                switch(priorityScheme) {
                    case SCALED_PRIORITIES:
                        int n = fromVertex.edges.size() - 1;
                        for (Graph.Edge edge : fromVertex.edges) {
                            if (edge.sender != fromVertex.twin) edge.cost = 1 + (edge.cost - 1) * 2520 / n;
                        }
                        break;
                }
            }
        }
        graph.freeze();
        for (Map.Entry<String, Integer> entry : unknowns.entrySet()) {
            String item = entry.getKey();
            int occurrences = entry.getValue();
            String plural = occurrences == 1 ? "" : "s";
            errors.add("**** Unknown item " + item + " (" + occurrences + " occurrence" + plural + ")");
        }
    }

    String show(Graph.Vertex vertex) {
        if (vertex.user == null || vertex.isDummy) return vertex.name; else if (sortByItem) return vertex.name + " " + vertex.user; else return vertex.user + " " + vertex.name;
    }

    void displayMatches(List<List<Graph.Vertex>> cycles) {
        int numTrades = 0;
        int numGroups = cycles.size();
        int totalCost = 0;
        int sumOfSquares = 0;
        List<Integer> groupSizes = new ArrayList<Integer>();
        List<String> summary = new ArrayList<String>();
        List<String> loops = new ArrayList<String>();
        for (List<Graph.Vertex> cycle : cycles) {
            int size = cycle.size();
            numTrades += size;
            sumOfSquares += size * size;
            groupSizes.add(size);
            for (Graph.Vertex v : cycle) {
                assert v.match != v.twin;
                loops.add(pad(show(v)) + " receives " + show(v.match.twin));
                summary.add(pad(show(v)) + " receives " + pad(show(v.match.twin)) + " and sends to " + show(v.twin.match));
                totalCost += v.matchCost;
            }
            loops.add("");
        }
        if (showNonTrades) {
            for (Graph.Vertex v : graph.RECEIVERS) {
                if (v.match == v.twin && !v.isDummy) summary.add(pad(show(v)) + "             does not trade");
            }
            for (Graph.Vertex v : graph.orphans) {
                if (!v.isDummy) summary.add(pad(show(v)) + "             does not trade");
            }
        }
        if (showLoops) {
            System.out.println("TRADE LOOPS (" + numTrades + " total trades):");
            System.out.println();
            for (String item : loops) System.out.println(item);
        }
        if (showSummary) {
            Collections.sort(summary);
            System.out.println("ITEM SUMMARY (" + numTrades + " total trades):");
            System.out.println();
            for (String item : summary) System.out.println(item);
            System.out.println();
        }
        System.out.print("Num trades  = " + numTrades + " of " + (ITEMS - DUMMY_ITEMS) + " items");
        if (ITEMS - DUMMY_ITEMS == 0) System.out.println(); else System.out.println(new DecimalFormat(" (0.0%)").format(numTrades / (double) (ITEMS - DUMMY_ITEMS)));
        if (showStats) {
            System.out.print("Total cost  = " + totalCost);
            if (numTrades == 0) System.out.println(); else System.out.println(new DecimalFormat(" (avg 0.00)").format(totalCost / (double) numTrades));
            System.out.println("Num groups  = " + numGroups);
            System.out.print("Group sizes =");
            Collections.sort(groupSizes);
            Collections.reverse(groupSizes);
            for (int groupSize : groupSizes) System.out.print(" " + groupSize);
            System.out.println();
            System.out.println("Sum squares = " + sumOfSquares);
        }
    }

    int width = 1;

    String pad(String name) {
        while (name.length() < width) name += " ";
        return name;
    }

    String toHexString(byte[] bytes) {
        String str = new String();
        for (byte b : bytes) str = str + Integer.toHexString(b & 0xff);
        return str;
    }
}
