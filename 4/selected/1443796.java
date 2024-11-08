package edu.rice.cs.cunit.record.compactGraph;

import java.io.PrintWriter;
import java.util.*;

/**
 * Creates and analyzes the wait graph.
 *
 * @author Mathias Ricken
 */
public class CompactWaitGraph {

    /**
     * Map for fast lookup. Key is the thread ID, value is a MapEntry.
     */
    private HashMap<Long, MapEntry> _map;

    /**
     * List of list with cycles.
     */
    private List<List<CompactThreadInfo>> _cycles;

    /**
     * Writer for debug output.
     */
    private PrintWriter _writer;

    /**
     * Helper class containing CompactThreadInfo and data for breadth-first search.
     */
    private static class MapEntry {

        public static enum Color {

            WHITE, GRAY, BLACK
        }

        private CompactThreadInfo _threadInfo;

        private CompactWaitGraph.MapEntry.Color _color;

        private long _distance;

        private Long _parentId;

        public MapEntry(CompactThreadInfo threadInfo, CompactWaitGraph.MapEntry.Color color, long distance, Long parentId) {
            _threadInfo = threadInfo;
            _color = color;
            _distance = distance;
            _parentId = parentId;
        }

        public Long getParentId() {
            return _parentId;
        }

        public void setParentId(Long parentId) {
            _parentId = parentId;
        }

        public CompactThreadInfo getThreadInfo() {
            return _threadInfo;
        }

        public void setThreadInfo(CompactThreadInfo threadInfo) {
            _threadInfo = threadInfo;
        }

        public CompactWaitGraph.MapEntry.Color getColor() {
            return _color;
        }

        public void setColor(CompactWaitGraph.MapEntry.Color color) {
            _color = color;
        }

        public long getDistance() {
            return _distance;
        }

        public void setDistance(long distance) {
            _distance = distance;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Thread ");
            sb.append(_threadInfo.toString());
            if (_threadInfo != null) {
                sb.append(_threadInfo.toStringVerbose());
            }
            sb.append(", color ");
            sb.append(_color);
            return sb.toString();
        }
    }

    /**
     * Creates a new compact wait graph.
     * @param threads map  of threads to analyze
     * @param writer writer for output
     */
    public CompactWaitGraph(Map<Long, CompactThreadInfo> threads, PrintWriter writer) {
        _writer = writer;
        _cycles = new ArrayList<List<CompactThreadInfo>>();
        _map = new HashMap<Long, CompactWaitGraph.MapEntry>();
        for (CompactThreadInfo cti : threads.values()) {
            _map.put(cti.getThreadID(), new MapEntry(cti, MapEntry.Color.WHITE, 0, null));
        }
        for (CompactThreadInfo cti : threads.values()) {
            MapEntry me = _map.get(cti.getThreadID());
            if (me.getColor() == MapEntry.Color.WHITE) {
                bfs(cti, threads);
            }
        }
        if (_cycles.size() > 0) {
            _writer.println("// Deadlocks found:");
            int num = 1;
            for (List<CompactThreadInfo> cycle : _cycles) {
                _writer.println("// \tCycle " + (num++));
                for (CompactThreadInfo ti : cycle) {
                    _writer.println("// \t\tThread " + ti.toString());
                }
            }
        }
    }

    /**
     * Returns list of list with cycles.
     * @return list of list with cycles
     */
    public List<List<CompactThreadInfo>> getCycles() {
        return _cycles;
    }

    /**
     * Performs a breadth-first search, starting with the specified thread.
     * @param cti thread to start with
     * @param threads map of threads to analyze
     */
    private void bfs(CompactThreadInfo cti, Map<Long, CompactThreadInfo> threads) {
        MapEntry me = _map.get(cti.getThreadID());
        me.setColor(MapEntry.Color.GRAY);
        PriorityQueue<MapEntry> q = new PriorityQueue<MapEntry>(10, new Comparator<MapEntry>() {

            public int compare(MapEntry o1, MapEntry o2) {
                return (o1.getDistance() < o2.getDistance()) ? -1 : ((o1.getDistance() == o2.getDistance()) ? 0 : -1);
            }
        });
        q.add(me);
        while (q.size() > 0) {
            me = q.poll();
            if (me.getThreadInfo().getContendedLockID() != null) {
                long contendedLockID = me.getThreadInfo().getContendedLockID();
                CompactThreadInfo owningThread = null;
                for (CompactThreadInfo ti : threads.values()) {
                    if (ti.getOwnedLockIDs().get(contendedLockID) != null) {
                        owningThread = ti;
                        break;
                    }
                }
                if (owningThread != null) {
                    MapEntry owner = _map.get(owningThread.getThreadID());
                    if (owner.getColor() == MapEntry.Color.WHITE) {
                        owner.setColor(MapEntry.Color.GRAY);
                        owner.setDistance(me.getDistance() + 1);
                        owner.setParentId(me.getThreadInfo().getThreadID());
                        q.add(owner);
                    } else {
                        ArrayList<CompactThreadInfo> cycle = new ArrayList<CompactThreadInfo>();
                        cycle.add(owner.getThreadInfo());
                        MapEntry parent = me;
                        while ((parent != null) && (parent != owner)) {
                            cycle.add(parent.getThreadInfo());
                            parent = _map.get(parent.getParentId());
                        }
                        _cycles.add(cycle);
                    }
                }
            }
        }
    }
}
