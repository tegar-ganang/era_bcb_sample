package analysis;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class AggregateTraces {

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Use argument: <input-logfile-name>");
            System.out.println("Output directory is always 'out'");
        } else {
            File out = new File("out");
            out.mkdirs();
            new AggregateTraces().process(new File("issues-log.txt"), new File("out"));
            Desktop.getDesktop().browse(new File("out/index.html").toURI());
        }
    }

    private void process(File file, File outDir) throws IOException {
        Scanner scan = new Scanner(file, "iso-8859-1");
        List<String> lines = new ArrayList<String>();
        while (scan.hasNextLine()) {
            lines.add(scan.nextLine());
        }
        System.out.println("readed lines: " + lines.size());
        List<MetaStackTrace> tracesList = this.parceTraces(lines);
        lines = null;
        System.out.println("readed traces: " + tracesList.size());
        {
            for (File traceFile : outDir.listFiles()) {
                if (traceFile.isFile() && traceFile.getName().startsWith("exception") && traceFile.getName().endsWith(".html")) {
                    traceFile.delete();
                }
            }
            for (MetaStackTrace meta : tracesList) {
                PrintStream out = new PrintStream(new File(outDir, "exception-at-log-line-" + meta.getId() + ".html"));
                out.println("<pre>");
                for (String line : meta.getLines()) {
                    out.println(line);
                }
                out.println("</pre>");
                out.close();
            }
        }
        {
            PrintStream out = new PrintStream(new File(outDir, "index.html"));
            Scanner prefix = new Scanner(new File("template_index_prefix.html"));
            while (prefix.hasNextLine()) {
                out.println(prefix.nextLine());
            }
            {
                out.append("Total exceptions: " + tracesList.size() + "<br/>\n");
                out.append("<ul id='toc' class='filetree'>\n");
                for (MetaStackTrace meta : tracesList) {
                    if (meta.isHidden() == false) {
                        out.append("<li class='").append(meta.getFilteredFriends().size() > 0 ? "folder" : "file").append("'>\n");
                        out.append(meta.toHtmlAnchor());
                        if (meta.getFilteredFriends().size() > 0) {
                            out.append("<ul>\n");
                            for (Weight<MetaStackTrace> w : meta.getFilteredFriends()) {
                                out.append("<li class='file'>\n");
                                out.append(w.getRef().toHtmlAnchor() + " (distance=" + w.getWeight() + ")");
                                out.append("</li>\n");
                            }
                            out.append("</ul>\n");
                        }
                        out.append("</li>\n");
                    }
                }
                out.append("</ul>\n");
            }
            Scanner postfix = new Scanner(new File("template_index_postfix.html"));
            while (postfix.hasNextLine()) {
                out.println(postfix.nextLine());
            }
            out.close();
        }
    }

    private List<MetaStackTrace> parceTraces(List<String> _lines) {
        List<MetaStackTrace> resultList = new ArrayList<MetaStackTrace>();
        MetaStackTrace current = null;
        int index = -1;
        for (String lineRaw : _lines) {
            index++;
            String line = lineRaw.trim();
            if (line.isEmpty() == true) {
                continue;
            }
            if (Util.isTraceHeaderLike(line, Util.nextNotEmptyLine(_lines, index).trim())) {
                if (current != null) {
                    resultList.add(current);
                    current = null;
                }
                current = new MetaStackTrace(index + 1);
                current.getLines().add(lineRaw);
                continue;
            }
            if (current != null) {
                if ((Util.isTraceHeaderNestedLike(line) == true) || (Util.isTraceAtLike(line) == true)) {
                    current.getLines().add(lineRaw);
                } else {
                    resultList.add(current);
                    current = null;
                }
            } else {
                if ((Util.isTraceHeaderNestedLike(line) == true) || (Util.isTraceAtLike(line) == true)) {
                    System.out.println("WANING: line is not attached to any stack tarce: " + line);
                }
            }
        }
        if (current != null) {
            resultList.add(current);
        }
        for (MetaStackTrace meta : resultList) {
            meta.splitNestedTraces();
        }
        int progress = 0;
        for (MetaStackTrace meta : resultList) {
            meta.findFriends(resultList, "", 4);
            progress++;
            if (progress % 100 == 0) {
                System.out.printf("%d traces processed\n", progress);
            }
        }
        System.out.println("traces processing complete");
        WeightMap<MetaStackTrace> usedFriendsMap = new WeightMap<MetaStackTrace>();
        for (MetaStackTrace meta : resultList) {
            for (Weight<MetaStackTrace> friend : meta.getFriends()) {
                if (meta.getId() < friend.getRef().getId()) {
                    if (friend.getWeight() < usedFriendsMap.get(friend.getRef())) {
                        meta.getFilteredFriends().add(friend);
                        usedFriendsMap.put(friend.getRef(), friend.getWeight());
                        if (friend.getWeight() == 0) {
                            friend.getRef().setHidden(true);
                        }
                    }
                }
            }
            Collections.sort(meta.getFilteredFriends());
        }
        Collections.sort(resultList, new Comparator<MetaStackTrace>() {

            public int compare(MetaStackTrace _o1, MetaStackTrace _o2) {
                return -_o1.compareTo(_o2);
            }
        });
        return resultList;
    }
}

class Util {

    static String nextNotEmptyLine(List<String> _lines, int index) {
        if (index + 1 < _lines.size()) {
            String nextLine = _lines.get(index + 1);
            if (nextLine.trim().isEmpty() == false) {
                return nextLine;
            } else {
                return Util.nextNotEmptyLine(_lines, index + 1);
            }
        } else {
            return "";
        }
    }

    static boolean isTraceHeaderLike(String _line, String _nextLine) {
        if ((Util.isTraceAtLike(_line) == true) || (Util.isTraceHeaderNestedLike(_line) == true)) {
            return false;
        }
        if (Util.isTraceAtLike(_nextLine) == false) {
            return false;
        }
        return _line.matches(".*\\..*\\..*");
    }

    static boolean isTraceHeaderNestedLike(String _line) {
        return _line.startsWith("Caused by: ");
    }

    static boolean isTraceAtLike(String _line) {
        return _line.startsWith("at ") || _line.startsWith("...");
    }

    static String exctractMethodNameFromAtLine(String _lineAt) {
        int spacePos = _lineAt.indexOf(' ');
        int openBracePos = _lineAt.indexOf('(');
        if ((spacePos > 0) && (openBracePos > spacePos)) {
            return _lineAt.substring(spacePos + 1, openBracePos);
        } else {
            return _lineAt;
        }
    }

    static String exctractLineNumberFromAtLine(String _lineAt) {
        int openBracePos = _lineAt.indexOf('(');
        int closeBracePos = _lineAt.indexOf(')');
        if ((openBracePos > 0) && (closeBracePos > openBracePos)) {
            String between = _lineAt.substring(openBracePos + 1, closeBracePos);
            int colonPos = between.indexOf(':');
            if (colonPos > 0) {
                return between.substring(colonPos + 1);
            } else {
                return between;
            }
        } else {
            return "Unknown";
        }
    }
}

class Weight<T> implements Comparable<Weight<T>> {

    private float weight;

    private T ref;

    public Weight(T _ref, float _weight) {
        super();
        ref = _ref;
        weight = _weight;
    }

    public float getWeight() {
        return weight;
    }

    public T getRef() {
        return ref;
    }

    @Override
    public int compareTo(Weight<T> _other) {
        return (int) Math.signum(weight - _other.weight);
    }
}

class WeightMap<T> extends HashMap<T, Float> {

    @Override
    public Float get(Object key) {
        if (containsKey((T) key) == false) {
            put((T) key, Float.MAX_VALUE);
        }
        return super.get((T) key);
    }
}

class MetaStackTrace implements Comparable<MetaStackTrace> {

    int id;

    boolean hidden = false;

    Float selftWeight = null;

    List<String> lines = new ArrayList<String>();

    List<StackTrace> items = new ArrayList<StackTrace>(3);

    String signatureKey = null;

    String[] signature;

    List<Weight<MetaStackTrace>> friends = new ArrayList<Weight<MetaStackTrace>>();

    List<Weight<MetaStackTrace>> filteredFriends = new ArrayList<Weight<MetaStackTrace>>();

    public MetaStackTrace(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public List<String> getLines() {
        return lines;
    }

    public List<StackTrace> getItems() {
        return items;
    }

    public List<Weight<MetaStackTrace>> getFriends() {
        return friends;
    }

    public List<Weight<MetaStackTrace>> getFilteredFriends() {
        return filteredFriends;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setFilteredFriends(List<Weight<MetaStackTrace>> filteredFriends) {
        this.filteredFriends = filteredFriends;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public Float getSelftWeight() {
        if (selftWeight == null) {
            selftWeight = 1.0f + filteredFriends.size();
            for (Weight<MetaStackTrace> w : getFilteredFriends()) {
                selftWeight += w.getRef().getFilteredFriends().size() / (w.getWeight() + 1.0f);
            }
        }
        return selftWeight;
    }

    public void findFriends(List<MetaStackTrace> _tracesList, String _rootPackage, float _friendPropertyQualificationLimit) {
        String[] currentSignature = this.getSignature(_rootPackage);
        for (MetaStackTrace maybeFriend : _tracesList.subList(_tracesList.indexOf(this), _tracesList.size())) {
            if (maybeFriend != this) {
                String[] maybeFriendSignature = maybeFriend.getSignature(_rootPackage);
                float distance = LevenshteinDistance.computeLevenshteinDistance(currentSignature, maybeFriendSignature);
                if (distance <= _friendPropertyQualificationLimit) {
                    friends.add(new Weight<MetaStackTrace>(maybeFriend, distance));
                }
            }
        }
        Collections.sort(friends);
    }

    public void splitNestedTraces() {
        if (this.isEmpty() == false) {
            StackTrace current = new StackTrace();
            for (String lineRaw : lines) {
                String line = lineRaw.trim();
                if (Util.isTraceHeaderNestedLike(line) == true) {
                    items.add(current);
                    current = new StackTrace();
                }
                current.getLines().add(line);
            }
            items.add(current);
        }
    }

    public String getLastExceptionHeader() {
        int index = items.size() - 1;
        StackTrace trace = items.get(index);
        String[] header = trace.getLines().get(0).trim().split("\\s+");
        if (index > 0) {
            return header[2];
        } else {
            return header[0];
        }
    }

    public String[] getSignature() {
        return this.getSignature("");
    }

    public String[] getSignature(String _rootPackage) {
        if ((signatureKey == null) || (signatureKey.equals(_rootPackage) == false)) {
            List<String> result = new ArrayList<String>(items.size() * 5);
            int index = items.size() - 1;
            while (index >= 0) {
                StackTrace trace = items.get(index);
                if (_rootPackage.isEmpty() == false) {
                    for (String lineRaw : lines.subList(1, lines.size())) {
                        String line = lineRaw.trim();
                        if (line.startsWith("at " + _rootPackage)) {
                            result.add(Util.exctractMethodNameFromAtLine(line));
                            result.add(Util.exctractLineNumberFromAtLine(line));
                            break;
                        }
                    }
                }
                {
                    String line = trace.getLines().get(1).trim();
                    result.add(Util.exctractMethodNameFromAtLine(line));
                    result.add(Util.exctractLineNumberFromAtLine(line));
                }
                index--;
            }
            signatureKey = _rootPackage;
            signature = result.toArray(new String[result.size()]);
        }
        return signature;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + (lines.size() > 0 ? (lines.get(0) + " [" + lines.size() + "]") : "empty") + " sections > " + (items == null ? "None" : items.size());
    }

    public String toHtmlAnchor() {
        StringBuilder out = new StringBuilder();
        out.append("<a href='exception-at-log-line-").append("" + this.getId()).append(".html").append("'>");
        try {
            out.append(this.getSignature()[0] + ":" + this.getSignature()[1]).append(" ");
        } catch (Exception pass) {
        }
        out.append("Line: ").append("" + this.getId());
        out.append("</a>");
        out.append(" (Weight: <b>").append("" + this.getSelftWeight()).append("</b>) ");
        out.append("&nbsp;&nbsp;&nbsp;&nbsp;[").append(getLastExceptionHeader()).append("]");
        out.append("\n");
        return out.toString();
    }

    public int compareTo(MetaStackTrace _other) {
        return getSelftWeight().compareTo(_other.getSelftWeight());
    }
}

class StackTrace {

    List<String> lines = new ArrayList<String>();

    public List<String> getLines() {
        return lines;
    }
}

class LevenshteinDistance {

    private static int minimum(int a, int b, int c) {
        if ((a <= b) && (a <= c)) {
            return a;
        }
        if ((b <= a) && (b <= c)) {
            return b;
        }
        return c;
    }

    public static int computeLevenshteinDistance(Object[] str1, Object[] str2) {
        int[][] distance = new int[str1.length + 1][];
        for (int i = 0; i <= str1.length; i++) {
            distance[i] = new int[str2.length + 1];
            distance[i][0] = i;
        }
        for (int j = 0; j < str2.length + 1; j++) {
            distance[0][j] = j;
        }
        for (int i = 1; i <= str1.length; i++) {
            for (int j = 1; j <= str2.length; j++) {
                distance[i][j] = LevenshteinDistance.minimum(distance[i - 1][j] + (1 + str1.length - i), distance[i][j - 1] + (1 + str2.length - j), distance[i - 1][j - 1] + ((str1[i - 1].equals(str2[j - 1])) ? 0 : Math.min((1 + str1.length - i), (1 + str2.length - j))));
            }
        }
        return distance[str1.length][str2.length];
    }
}
