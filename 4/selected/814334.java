package rres.ondex.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import net.sourceforge.ondex.InvalidPluginArgumentException;
import net.sourceforge.ondex.core.AttributeName;
import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;

/**
 * @author huf
 * @date 10-03-2010
 * 
 */
public class ClientWorker implements Runnable {

    /**
	 * network socket to listen on
	 */
    private Socket clientSocket;

    /**
	 * Ondex interface provider
	 */
    private OndexServiceProvider ondexProvider;

    /**
	 * Set network socket and Ondex interface provider
	 * 
	 * @param socket
	 * @param provider
	 */
    public ClientWorker(Socket socket, OndexServiceProvider provider) {
        this.clientSocket = socket;
        this.ondexProvider = provider;
    }

    @Override
    public void run() {
        PrintWriter out = null;
        BufferedReader in = null;
        String request;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String fromClient = in.readLine();
            request = "";
            while (!fromClient.equals("Bye.")) {
                try {
                    request = request + fromClient + "\n";
                    fromClient = in.readLine();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    out.println("Bye.");
                }
            }
            out.println(processRequest(request));
            out.println("Bye.");
        } catch (IOException e) {
            System.out.println(e.getMessage());
            out.println("Bye.");
        }
    }

    protected String processRequest(String query) throws UnsupportedEncodingException {
        String keyword = "";
        String mode = "";
        List<QTL> qtls = new ArrayList<QTL>();
        List<String> qtlString = new ArrayList<String>();
        List<String> list = new ArrayList<String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            String key = URLDecoder.decode(pair[0], "UTF-8");
            String value = "";
            if (pair.length > 1) {
                value = URLDecoder.decode(pair[1], "UTF-8");
            }
            if (key.toLowerCase().startsWith("qtl")) {
                qtlString.add(value.trim());
            } else if (key.toLowerCase().equals("keyword")) {
                keyword = value.trim().replace("\n", "");
            } else if (key.toLowerCase().equals("mode")) {
                mode = value.trim().replace("\n", "");
            } else if (key.toLowerCase().equals("list")) {
                Collections.addAll(list, value.split("\n"));
            }
        }
        boolean validQTL = false;
        for (String region : qtlString) {
            String[] r = region.split(":");
            int chr;
            long start, end;
            String label = "";
            try {
                if (r.length == 3 || r.length == 4) {
                    chr = Integer.parseInt(r[0]);
                    start = Long.parseLong(r[1]);
                    end = Long.parseLong(r[2]);
                    if (r.length == 4) {
                        label = r[3];
                    }
                    if (start < end) {
                        validQTL = true;
                        QTL qtl = new QTL(chr, Long.toString(start), Long.toString(end), label);
                        qtls.add(qtl);
                    }
                    System.out.println("chr: " + chr + "start: " + start + " end: " + end + "label: " + label);
                }
                if (!validQTL) {
                    System.out.println(region + " is not valid qtl region");
                }
            } catch (Exception e) {
                System.out.println(region + " is not valid qtl region");
            }
        }
        if (keyword != null && keyword.length() > 2) {
            if (mode.equals("network")) {
                try {
                    return callApplet(keyword, qtls, list);
                } catch (InvalidPluginArgumentException e) {
                    e.printStackTrace();
                }
                return "OndexWeb";
            } else if (mode.equals("genome")) {
                return callOndexProvider(keyword, qtls, list);
            } else if (mode.equals("qtl")) {
                return callOndexProvider(keyword, qtls, list);
            } else {
                return "Not valid request.";
            }
        } else {
            System.out.println("Not valid request.");
            return "Not valid request.";
        }
    }

    protected String callApplet(String keyword, List<QTL> qtls, List<String> list) throws UnsupportedEncodingException, InvalidPluginArgumentException {
        System.out.println("CALL APPLET");
        String request = "";
        Set<ONDEXConcept> genes = new HashSet();
        long timestamp = System.currentTimeMillis();
        String fileName = "result_" + timestamp + ".oxl";
        String exportPath = MultiThreadServer.props.getProperty("GraphPath");
        System.out.println("Searching for genes");
        System.out.println("list of genes " + list.size());
        if (list.size() > 0) {
            Set<ONDEXConcept> genesFromList = ondexProvider.searchGenes(list);
            System.out.println("found concepts" + list.size());
            genes.addAll(genesFromList);
        }
        System.out.println("list of qtls " + qtls.size());
        if (qtls.size() > 0) {
            long startTime = System.currentTimeMillis();
            Set<ONDEXConcept> genesWithinQTLs = ondexProvider.searchQTLs(qtls);
            long endTime = System.currentTimeMillis();
            genes.addAll(genesWithinQTLs);
            System.out.println("Total elapsed time in execution of method is :" + (endTime - startTime));
        }
        if (genes != null) {
            ONDEXGraph subGraph = ondexProvider.findSemanticMotifs(genes, keyword);
            try {
                boolean fileIsCreated = false;
                fileIsCreated = ondexProvider.exportGraph(subGraph, exportPath + fileName);
                if (fileIsCreated) {
                    request = "FileCreated:" + fileName;
                } else {
                    System.out.println("NoFile");
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return request;
    }

    protected String callOndexProvider(String keyword, List<QTL> qtls, List<String> list) {
        long timestamp = System.currentTimeMillis();
        String fileGViewer = keyword + "_" + timestamp + ".xml";
        String fileTxt = keyword + "_" + timestamp + ".txt";
        ONDEXGraph graph = null;
        String request = "";
        try {
            graph = ondexProvider.searchGenome(keyword);
            AttributeName attTAXID = graph.getMetaData().getAttributeName("TAXID");
            if (graph.getConcepts().size() == 0) {
                System.out.println("NoFile: no genes found");
                request = "NoFile:noGenesFound";
            } else {
                ConceptClass ccGene = graph.getMetaData().getConceptClass("Gene");
                AttributeName attSize = graph.getMetaData().getAttributeName("size");
                Set<ONDEXConcept> candidates = graph.getConceptsOfConceptClass(ccGene);
                Map<ONDEXConcept, Double> gene2count = new HashMap<ONDEXConcept, Double>();
                for (ONDEXConcept gene : candidates) {
                    if (gene.getAttribute(attTAXID) == null || !gene.getAttribute(attTAXID).getValue().toString().equals(ondexProvider.getTaxId())) {
                        continue;
                    }
                    if (gene.getAttribute(attSize) != null) {
                        Double score = ((Integer) gene.getAttribute(attSize).getValue()).doubleValue();
                        gene2count.put(gene, score);
                    } else {
                    }
                }
                SortedSet<Entry<ONDEXConcept, Double>> sortedCandidates = ondexProvider.entriesSortedByValues(gene2count);
                ArrayList<ONDEXConcept> genes = new ArrayList<ONDEXConcept>();
                int count = 0;
                for (Entry<ONDEXConcept, Double> rankedGene : sortedCandidates) {
                    if (++count > 100) break;
                    genes.add(rankedGene.getKey());
                }
                System.out.println("Genes(s) displayed in GViewer: " + genes.size());
                Set<ONDEXConcept> userGenes = null;
                if (list != null && list.size() > 0) {
                    userGenes = ondexProvider.searchGenes(list);
                }
                boolean xmlIsCreated = ondexProvider.writeAnnotationXML(genes, userGenes, qtls, MultiThreadServer.props.getProperty("AnnotationPath") + fileGViewer, keyword, 100);
                boolean txtIsCreated = ondexProvider.writeTableOut(genes, userGenes, qtls, MultiThreadServer.props.getProperty("AnnotationPath") + fileTxt, graph);
                if (xmlIsCreated && txtIsCreated) {
                    System.out.println("FileCreated:" + fileGViewer + ":" + fileTxt + ":" + genes.size());
                    request = "FileCreated:" + fileGViewer + ":" + fileTxt + ":" + genes.size();
                } else {
                    System.out.println("NoFile: File (Keyword) is not created");
                    System.out.println("NoFile: no genes found");
                    request = "NoFile: no genes found";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return request;
    }
}
