package au.org.ala.bhl;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.checklist.lucene.HomonymException;
import au.org.ala.checklist.lucene.SearchResultException;
import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.checklist.lucene.model.NameSearchResult.MatchType;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * This class takes the bhlexport.txt file and extracts the australian records.
 *
 * It makes use of the name_matching API and assumes that the index is
 * /data/lucene/namematching.
 *
 * It assume that the source location is /data/bhl/bhlexport.txt.
 * The target will be /data/bhl/bhlexport_au.txt
 * @author Natasha
 */
public class ExtractAustralianRecords {

    private CBIndexSearch searcher;

    private OutputStreamWriter writer;

    private BufferedReader reader;

    long count = 0;

    long auCount = 0;

    public void init(boolean includeLSID) throws Exception {
        searcher = new CBIndexSearch("/data/lucene/namematching");
        String title = includeLSID ? "/data/bhl/bhlexport_au_lsid.txt" : "/data/bhl/bhlexport_au.txt";
        System.out.println("Exporting to " + title);
        writer = new OutputStreamWriter(new FileOutputStream(title), "UTF-8");
        reader = new BufferedReader(new InputStreamReader(new FileInputStream("/data/bhl/bhlexport.txt"), "UTF-16LE"));
    }

    /**
     * extracts the australian records from the source file.
     *
     * Australian record are identified by the name matching API when the LSID
     * is from AFD, APC or APNI.
     */
    public void extract(boolean includeLSID) throws IOException {
        long start = System.currentTimeMillis();
        writer.write(reader.readLine());
        if (includeLSID) writer.write("\tLSID");
        writer.write("\n");
        System.out.println("Starting to extract...");
        String line = reader.readLine();
        while (line != null) {
            count++;
            try {
                String[] values = line.split("\t");
                String lsid = null;
                if (values != null && values.length == 9) {
                    String name = values[8];
                    try {
                        NameSearchResult result = searcher.searchForRecord(name, null);
                        if (result != null && result.getMatchType() != MatchType.SEARCHABLE) {
                            lsid = getAustralianLsid(result);
                        }
                    } catch (SearchResultException sre) {
                        if (sre instanceof HomonymException) {
                            List<NameSearchResult> results = ((HomonymException) sre).getResults();
                            if (results != null && results.size() > 0) {
                                lsid = getAustralianLsid((NameSearchResult[]) results.toArray(new NameSearchResult[] {}));
                            }
                        }
                    }
                }
                if (lsid != null) {
                    auCount++;
                    writer.write(line);
                    if (includeLSID) writer.write("\t" + lsid);
                    writer.write("\n");
                }
                if (count % 100000 == 0) printStats(start);
                line = reader.readLine();
            } catch (IOException ie) {
                ie.printStackTrace();
                printStats(start);
                System.exit(-1);
            }
        }
        printStats(start);
        writer.flush();
        writer.close();
    }

    private void printStats(long start) {
        System.out.println("Processed " + count + " (au:" + auCount + ") in " + (System.currentTimeMillis() - start) + " ms. ");
    }

    /**
     * Returns an LSID if one of the results is Australian otherwise null.
     *
     * @param results
     * @return
     */
    public String getAustralianLsid(NameSearchResult... results) {
        for (NameSearchResult result : results) {
            if (result != null && result.getLsid() != null && result.getLsid().startsWith("urn:lsid:biodiversity.org.au")) return result.getLsid();
        }
        return null;
    }

    /**
     * args -lsid include the lsids
     * @param args
     */
    public static void main(String[] args) {
        try {
            boolean includeLSID = args.length > 0 && args[0].equals("-lsid");
            ExtractAustralianRecords extracter = new ExtractAustralianRecords();
            extracter.init(includeLSID);
            extracter.extract(includeLSID);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
