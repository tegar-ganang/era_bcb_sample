package org.sf.xrime.algorithms.partitions.connected.strongly;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.ToolRunner;
import org.sf.xrime.ProcessorExecutionException;
import org.sf.xrime.Transformer;
import org.sf.xrime.algorithms.GraphAlgorithm;
import org.sf.xrime.algorithms.transform.vertex.InAdjVertex2AdjBiSetVertexTransformer;
import org.sf.xrime.algorithms.transform.vertex.Vertex2LabeledTransformer;
import org.sf.xrime.model.Graph;
import org.sf.xrime.model.vertex.LabeledAdjBiSetVertex;
import org.sf.xrime.utils.MRConsoleReader;
import org.sf.xrime.utils.SequenceTempDirMgr;

/**
 * Algorithm to calculate strongly connected components of a graph. We assume the input is the original
 * graph represented with incoming adjacency vertexes lists.
 * @author xue
 */
public class SCCAlgorithm extends GraphAlgorithm {

    /**
   * Used to generate sequential temporary paths.
   */
    private SequenceTempDirMgr _dirMgr;

    /**
   * The current temporary path. This is also used as a flag to indicate that
   * the algorithm should stop. When this is set to null, the algorithm should
   * stop.
   */
    private Path _curr_path;

    /**
   * Used to accumulate scc components.
   */
    private List<Path> _scc_components;

    /**
   * Default constructor is not allowed.
   */
    private SCCAlgorithm() {
        super();
    }

    /**
   * Constructor.
   * @throws ProcessorExecutionException 
   */
    public SCCAlgorithm(Graph src, Graph dest) throws ProcessorExecutionException {
        this();
        this.setSource(src);
        this.setDestination(dest);
    }

    /**
   * Do the transformation needed at the beginning of this algorithm.
   * @throws ProcessorExecutionException
   */
    private void preTransform() throws ProcessorExecutionException {
        System.out.println("##########>" + _dirMgr.getSeqNum() + " Transform input to AdjBiSetVertex");
        Transformer transformer = new InAdjVertex2AdjBiSetVertexTransformer();
        transformer.setConf(context);
        transformer.setSrcPath(_curr_path);
        try {
            _curr_path = _dirMgr.getTempDir();
        } catch (IOException e) {
            throw new ProcessorExecutionException(e);
        }
        transformer.setDestPath(_curr_path);
        transformer.setMapperNum(getMapperNum());
        transformer.setReducerNum(getReducerNum());
        transformer.execute();
        System.out.println("##########>" + _dirMgr.getSeqNum() + " Transform to LabeledAdjBiSetVertex");
        Vertex2LabeledTransformer l_transformer = new Vertex2LabeledTransformer();
        l_transformer.setConf(context);
        l_transformer.setSrcPath(_curr_path);
        try {
            _curr_path = _dirMgr.getTempDir();
        } catch (IOException e) {
            throw new ProcessorExecutionException(e);
        }
        l_transformer.setDestPath(_curr_path);
        l_transformer.setMapperNum(getMapperNum());
        l_transformer.setReducerNum(getReducerNum());
        l_transformer.setLabelAdderClass(null);
        l_transformer.setOutputValueClass(LabeledAdjBiSetVertex.class);
        l_transformer.execute();
    }

    /**
   * Check the size of the graph in term of vertexes number.
   * @throws ProcessorExecutionException
   */
    private void checkSize() throws ProcessorExecutionException {
        System.out.println("##########>" + _dirMgr.getSeqNum() + " Check graph size");
        Graph src = new Graph(Graph.defaultGraph());
        src.setPath(_curr_path);
        Path tmpDir;
        try {
            tmpDir = _dirMgr.getTempDir();
        } catch (IOException e) {
            throw new ProcessorExecutionException(e);
        }
        Graph dest = new Graph(Graph.defaultGraph());
        dest.setPath(tmpDir);
        GraphAlgorithm check_size = new CountSize();
        check_size.setConf(context);
        check_size.setSource(src);
        check_size.setDestination(dest);
        check_size.setMapperNum(getMapperNum());
        check_size.setReducerNum(getReducerNum());
        check_size.execute();
        long graph_size = MRConsoleReader.getMapInputRecordNum(check_size.getFinalStatus());
        if (graph_size == 0) {
            _curr_path = null;
        } else if (graph_size == 1) {
            _scc_components.add(_curr_path);
            _curr_path = null;
        }
    }

    /**
   * Forward trim.
   * @param dirMgr
   * @param input_path
   * @param result_paths
   * @return
   * @throws ProcessorExecutionException
   */
    private void trimForward() throws ProcessorExecutionException {
        Graph src;
        Graph dest;
        long trimed_graph_size = -1;
        long remaining_graph_size = -1;
        Path tmpDir;
        while (true) {
            System.out.println("##########>" + _dirMgr.getSeqNum() + " ForwardTrimPartA");
            GraphAlgorithm forward_trim_a = new ForwardTrimPartA();
            forward_trim_a.setConf(context);
            src = new Graph(Graph.defaultGraph());
            src.setPath(_curr_path);
            dest = new Graph(Graph.defaultGraph());
            try {
                tmpDir = _dirMgr.getTempDir();
            } catch (IOException e) {
                throw new ProcessorExecutionException(e);
            }
            dest.setPath(tmpDir);
            forward_trim_a.setSource(src);
            forward_trim_a.setDestination(dest);
            forward_trim_a.setMapperNum(getMapperNum());
            forward_trim_a.setReducerNum(getReducerNum());
            forward_trim_a.execute();
            trimed_graph_size = MRConsoleReader.getMapOutputRecordNum(forward_trim_a.getFinalStatus());
            if (trimed_graph_size == 0) {
                break;
            } else {
                _scc_components.add(tmpDir);
            }
            System.out.println("##########>" + _dirMgr.getSeqNum() + " ForwardTrimPartB");
            GraphAlgorithm forward_trim_b = new ForwardTrimPartB();
            forward_trim_b.setConf(context);
            src = new Graph(Graph.defaultGraph());
            src.setPath(_curr_path);
            dest = new Graph(Graph.defaultGraph());
            try {
                tmpDir = _dirMgr.getTempDir();
            } catch (IOException e) {
                throw new ProcessorExecutionException(e);
            }
            dest.setPath(tmpDir);
            forward_trim_b.setSource(src);
            forward_trim_b.setDestination(dest);
            forward_trim_b.setMapperNum(getMapperNum());
            forward_trim_b.setReducerNum(getReducerNum());
            forward_trim_b.execute();
            remaining_graph_size = MRConsoleReader.getReduceOutputRecordNum(forward_trim_b.getFinalStatus());
            if (remaining_graph_size == 0) {
                _curr_path = null;
                break;
            } else {
                _curr_path = tmpDir;
            }
        }
    }

    /**
   * 
   * @throws ProcessorExecutionException
   */
    private void trimBackward() throws ProcessorExecutionException {
        Graph src;
        Graph dest;
        long trimed_graph_size = -1;
        long remaining_graph_size = -1;
        Path tmpDir;
        while (true) {
            System.out.println("##########>" + _dirMgr.getSeqNum() + " BackwardTrimPartA");
            GraphAlgorithm backward_trim_a = new BackwardTrimPartA();
            backward_trim_a.setConf(context);
            src = new Graph(Graph.defaultGraph());
            src.setPath(_curr_path);
            dest = new Graph(Graph.defaultGraph());
            try {
                tmpDir = _dirMgr.getTempDir();
            } catch (IOException e1) {
                throw new ProcessorExecutionException(e1);
            }
            dest.setPath(tmpDir);
            backward_trim_a.setSource(src);
            backward_trim_a.setDestination(dest);
            backward_trim_a.setMapperNum(getMapperNum());
            backward_trim_a.setReducerNum(getReducerNum());
            backward_trim_a.execute();
            trimed_graph_size = MRConsoleReader.getMapOutputRecordNum(backward_trim_a.getFinalStatus());
            if (trimed_graph_size == 0) {
                break;
            } else {
                _scc_components.add(tmpDir);
            }
            System.out.println("##########>" + _dirMgr.getSeqNum() + " BackwardTrimPartB");
            GraphAlgorithm backward_trim_b = new BackwardTrimPartB();
            backward_trim_b.setConf(context);
            src = new Graph(Graph.defaultGraph());
            src.setPath(_curr_path);
            dest = new Graph(Graph.defaultGraph());
            try {
                tmpDir = _dirMgr.getTempDir();
            } catch (IOException e) {
                throw new ProcessorExecutionException(e);
            }
            dest.setPath(tmpDir);
            backward_trim_b.setSource(src);
            backward_trim_b.setDestination(dest);
            backward_trim_b.setMapperNum(getMapperNum());
            backward_trim_b.setReducerNum(getReducerNum());
            backward_trim_b.execute();
            remaining_graph_size = MRConsoleReader.getReduceOutputRecordNum(backward_trim_b.getFinalStatus());
            if (remaining_graph_size == 0) {
                _curr_path = null;
                break;
            } else {
                _curr_path = tmpDir;
            }
        }
    }

    /**
   * Choose a pivot vertex from the graph.
   * @return return the id of the chosen vertex.
   * @throws ProcessorExecutionException
   */
    private String choosePivotVertex() throws ProcessorExecutionException {
        String result = null;
        Graph src;
        Graph dest;
        Path tmpDir;
        System.out.println("##########>" + _dirMgr.getSeqNum() + " Choose the pivot vertex");
        src = new Graph(Graph.defaultGraph());
        src.setPath(_curr_path);
        dest = new Graph(Graph.defaultGraph());
        try {
            tmpDir = _dirMgr.getTempDir();
        } catch (IOException e) {
            throw new ProcessorExecutionException(e);
        }
        dest.setPath(tmpDir);
        GraphAlgorithm choose_pivot = new PivotChoose();
        choose_pivot.setConf(context);
        choose_pivot.setSource(src);
        choose_pivot.setDestination(dest);
        choose_pivot.setMapperNum(getMapperNum());
        choose_pivot.setReducerNum(getReducerNum());
        choose_pivot.execute();
        try {
            Path the_file = new Path(tmpDir.toString() + "/part-00000");
            FileSystem client = FileSystem.get(context);
            if (!client.exists(the_file)) {
                throw new ProcessorExecutionException("Did not find the chosen vertex in " + the_file.toString());
            }
            FSDataInputStream input_stream = client.open(the_file);
            ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
            IOUtils.copyBytes(input_stream, output_stream, context, false);
            String the_line = output_stream.toString();
            result = the_line.substring(PivotChoose.KEY_PIVOT.length()).trim();
            input_stream.close();
            output_stream.close();
            System.out.println("##########> Chosen pivot id = " + result);
        } catch (IOException e) {
            throw new ProcessorExecutionException(e);
        }
        return result;
    }

    /**
   * Do labels propagation.
   * @param label
   * @throws ProcessorExecutionException
   */
    private void propagateLabel(String label) throws ProcessorExecutionException {
        Graph src;
        Graph dest;
        long label_num = -1;
        Path tmpDir;
        while (true) {
            System.out.println("##########>" + _dirMgr.getSeqNum() + " Propagate the label : (" + label + ") from " + _curr_path.toString());
            src = new Graph(Graph.defaultGraph());
            src.setPath(_curr_path);
            try {
                tmpDir = _dirMgr.getTempDir();
            } catch (IOException e) {
                throw new ProcessorExecutionException(e);
            }
            dest = new Graph(Graph.defaultGraph());
            dest.setPath(tmpDir);
            GraphAlgorithm propagator = new LabelPropagation();
            propagator.setConf(context);
            propagator.setSource(src);
            propagator.setDestination(dest);
            propagator.setMapperNum(getMapperNum());
            propagator.setReducerNum(getReducerNum());
            propagator.setParameter(ConstantLabels.SCC_LABEL, label);
            propagator.execute();
            _curr_path = tmpDir;
            System.out.println("##########>" + _dirMgr.getSeqNum() + " Test label propagation convergence");
            src = new Graph(Graph.defaultGraph());
            src.setPath(_curr_path);
            try {
                tmpDir = _dirMgr.getTempDir();
            } catch (IOException e) {
                throw new ProcessorExecutionException(e);
            }
            dest = new Graph(Graph.defaultGraph());
            dest.setPath(tmpDir);
            GraphAlgorithm conv_tester = new PropagationConvergenceTest();
            conv_tester.setConf(context);
            conv_tester.setSource(src);
            conv_tester.setDestination(dest);
            conv_tester.setMapperNum(getMapperNum());
            conv_tester.setReducerNum(getReducerNum());
            conv_tester.execute();
            long found_label_num = MRConsoleReader.getMapOutputRecordNum(conv_tester.getFinalStatus());
            System.out.println("##########> label number = " + found_label_num);
            if (found_label_num == label_num) {
                break;
            } else {
                label_num = found_label_num;
            }
        }
    }

    /**
   * Extract a SCC from propagation result.
   * @throws ProcessorExecutionException
   */
    private void extractSCC() throws ProcessorExecutionException {
        Graph src;
        Graph dest;
        Path tmpDir;
        System.out.println("##########>" + _dirMgr.getSeqNum() + " Extract a SCC");
        src = new Graph(Graph.defaultGraph());
        src.setPath(_curr_path);
        try {
            tmpDir = _dirMgr.getTempDir();
        } catch (IOException e) {
            throw new ProcessorExecutionException(e);
        }
        dest = new Graph(Graph.defaultGraph());
        dest.setPath(tmpDir);
        GraphAlgorithm extractor = new ExtractSCC();
        extractor.setConf(context);
        extractor.setSource(src);
        extractor.setDestination(dest);
        extractor.setMapperNum(getMapperNum());
        extractor.setReducerNum(getReducerNum());
        extractor.execute();
        _scc_components.add(tmpDir);
    }

    /**
   * Extract Pred(G,v)\SCC(G,v).
   * @return the path to the extracted Pred\SCC.
   * @throws ProcessorExecutionException
   */
    private Path extractPredMinusSCC() throws ProcessorExecutionException {
        Graph src;
        Graph dest;
        Path tmpDir;
        System.out.println("##########>" + _dirMgr.getSeqNum() + " Extract Pred\\SCC");
        src = new Graph(Graph.defaultGraph());
        src.setPath(_curr_path);
        dest = new Graph(Graph.defaultGraph());
        try {
            tmpDir = _dirMgr.getTempDir();
        } catch (IOException e) {
            throw new ProcessorExecutionException(e);
        }
        dest.setPath(tmpDir);
        GraphAlgorithm extractor = new ExtractPredMinusSCC();
        extractor.setConf(context);
        extractor.setSource(src);
        extractor.setDestination(dest);
        extractor.setMapperNum(getMapperNum());
        extractor.setReducerNum(getReducerNum());
        extractor.execute();
        return tmpDir;
    }

    /**
   * Extract Desc(G,v)\SCC(G,v).
   * @return the path to Desc\SCC.
   * @throws ProcessorExecutionException
   */
    private Path extractDescMinusSCC() throws ProcessorExecutionException {
        Graph src;
        Graph dest;
        Path tmpDir;
        System.out.println("##########>" + _dirMgr.getSeqNum() + " Extract Desc\\SCC");
        src = new Graph(Graph.defaultGraph());
        src.setPath(_curr_path);
        dest = new Graph(Graph.defaultGraph());
        try {
            tmpDir = _dirMgr.getTempDir();
        } catch (IOException e) {
            throw new ProcessorExecutionException(e);
        }
        dest.setPath(tmpDir);
        GraphAlgorithm extractor = new ExtractDescMinusSCC();
        extractor.setConf(context);
        extractor.setSource(src);
        extractor.setDestination(dest);
        extractor.setMapperNum(getMapperNum());
        extractor.setReducerNum(getReducerNum());
        extractor.execute();
        return tmpDir;
    }

    /**
   * Extract Rem(G,v).
   * @return path to Rem.
   * @throws ProcessorExecutionException
   */
    private Path extractRemains() throws ProcessorExecutionException {
        Graph src;
        Graph dest;
        Path tmpDir;
        System.out.println("##########>" + _dirMgr.getSeqNum() + " Extract Rem");
        src = new Graph(Graph.defaultGraph());
        src.setPath(_curr_path);
        dest = new Graph(Graph.defaultGraph());
        try {
            tmpDir = _dirMgr.getTempDir();
        } catch (IOException e) {
            throw new ProcessorExecutionException(e);
        }
        dest.setPath(tmpDir);
        GraphAlgorithm extractor = new ExtractRemains();
        extractor.setConf(context);
        extractor.setSource(src);
        extractor.setDestination(dest);
        extractor.setMapperNum(getMapperNum());
        extractor.setReducerNum(getReducerNum());
        extractor.execute();
        return tmpDir;
    }

    /**
   * The recursive part of this algorithm.
   * @throws ProcessorExecutionException
   */
    private void recursiveCore() throws ProcessorExecutionException {
        checkSize();
        if (_curr_path == null) {
            return;
        }
        trimForward();
        if (_curr_path == null) {
            return;
        }
        trimBackward();
        if (_curr_path == null) {
            return;
        }
        String pivot_id = choosePivotVertex();
        propagateLabel(pivot_id);
        extractSCC();
        Path pred_scc = extractPredMinusSCC();
        Path desc_scc = extractDescMinusSCC();
        Path rem = extractRemains();
        _curr_path = pred_scc;
        recursiveCore();
        _curr_path = desc_scc;
        recursiveCore();
        _curr_path = rem;
        recursiveCore();
    }

    /**
   * Used to summarize the results.
   * @param paths
   * @throws ProcessorExecutionException
   */
    private void extractComponents() throws ProcessorExecutionException {
        Graph src;
        Graph dest;
        System.out.println("##########>" + _dirMgr.getSeqNum() + " Extract the final partitions");
        src = new Graph(Graph.defaultGraph());
        src.setPaths(_scc_components);
        dest = getDestination();
        GraphAlgorithm extractor = new ExtractPartitions();
        extractor.setConf(context);
        extractor.setSource(src);
        extractor.setDestination(dest);
        extractor.setMapperNum(getMapperNum());
        extractor.setReducerNum(1);
        extractor.execute();
    }

    @Override
    public void execute() throws ProcessorExecutionException {
        _scc_components = new LinkedList<Path>();
        if (getSource().getPaths() == null || getSource().getPaths().size() == 0 || getDestination().getPaths() == null || getDestination().getPaths().size() == 0) {
            throw new ProcessorExecutionException("No input and/or output paths specified.");
        }
        String temp_dir_prefix;
        try {
            temp_dir_prefix = getDestination().getPath().getParent().toString() + "/scc_alg1_" + getDestination().getPath().getName() + "_";
            _curr_path = getSource().getPath();
        } catch (IllegalAccessException e) {
            throw new ProcessorExecutionException(e);
        }
        _dirMgr = new SequenceTempDirMgr(temp_dir_prefix, context);
        _dirMgr.setSeqNum(0);
        preTransform();
        recursiveCore();
        if (_scc_components.size() > 0) {
            extractComponents();
        }
        _dirMgr.deleteAll();
    }

    @Override
    public void setArguments(String[] params) throws ProcessorExecutionException {
        if (params.length != 2) {
            throw new ProcessorExecutionException("Wrong number of parameters: " + params.length + " instead of 2.");
        }
        Graph src = new Graph(Graph.defaultGraph());
        src.setPath(new Path(params[0]));
        Graph dest = new Graph(Graph.defaultGraph());
        dest.setPath(new Path(params[1]));
        setSource(src);
        setDestination(dest);
    }

    /**
   * @param args
   */
    public static void main(String[] args) {
        try {
            int res = ToolRunner.run(new SCCAlgorithm(), args);
            System.exit(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
