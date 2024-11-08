import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *Search if one of the keywords is in the Genbank page
 */
public class Compare {

    /**
	 * The GI number on which we want to affect a status: Select Agent or not Select Agent.
	 */
    String gi;

    String SA = "";

    int nb_BM = 0;

    int num_frame;

    int num_sub_sequence;

    BufferedWriter[][] bb;

    /**
	 * Constructor
	 * @param g Gi number of the sequence we want to check
	 * @param nb number of best match
	 * @param num_f number of current frame
	 * @param num_sub_s number of current sub sequence
	 * @param b tab of files to write information
	 */
    public Compare(String g, int nb, int num_f, int num_sub_s, BufferedWriter[][] b) {
        gi = g;
        nb_BM = nb;
        num_frame = num_f;
        num_sub_sequence = num_sub_s;
        bb = b;
    }

    /**
	 * Return the name of the select agent found
	 * @return name of select agent found, "" if not select agent
	 */
    public String get_SA() {
        return SA;
    }

    /**
	 * Put the important part of the Genbank page in a string
	 * @return string containing the important part to search
	 * @throws Exception
	 */
    public String getInfo() throws Exception {
        String ext = "";
        String inputLine = "";
        try {
            String[] possible_words = { "LOCUS", "ACCESSION", "VERSION", "KEYWORDS", "ORIGIN", "REFERENCE", "PUBMED", "DBSOURCE", "JOURNAL", "COMMENT", "AUTHORS", "SUBMITTER", "COMMENTS", "Authors:", "Year:", "Status:", "SEQUENCE" };
            String[] correct_words = { "DEFINITION", "SOURCE", "ORGANISM", "TITLE", "FEATURES", "Lib Name:", "Organism", "Cultivar:", "Description:", "Title:" };
            Socket info = new Socket("ncbi.nlm.nih.gov", 80);
            PrintWriter pw = new PrintWriter(info.getOutputStream());
            pw.println("GET /sviewer/viewer.fcgi?val=" + gi + "&amp;sat=NCBI&satkey=0");
            pw.flush();
            boolean end = false;
            InputStream inS = info.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inS));
            while ((inputLine = in.readLine()) != null && inputLine.indexOf("ORIGIN") == -1 && inputLine.indexOf("MAP DATA") == -1 && !end) {
                if ((inputLine.contains("complete genome") || inputLine.contains("complete sequence")) && inputLine.contains("DEFINITION")) {
                    bb[num_frame][num_sub_sequence].write("complete genome page ignored, just check the title " + gi);
                    bb[num_frame][num_sub_sequence].newLine();
                    end = true;
                }
                inputLine = inputLine.replaceAll("\"", "");
                for (int i = 0; i < correct_words.length; i++) inputLine = inputLine.replaceAll(correct_words[i], "###111###");
                for (int i = 0; i < possible_words.length; i++) inputLine = inputLine.replaceAll(possible_words[i], "###000###");
                ext = ext.concat(" ".concat(inputLine));
            }
            in.close();
            inS.close();
            info.close();
        } catch (IOException e) {
            System.out.println("URL problem try again gi" + gi);
            throw e;
        } catch (Exception e) {
            System.out.println("************************ERROR********************");
            throw e;
        }
        try {
            ext = extraction(ext);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("in extraction");
            wait(10);
        }
        ext = ext.toLowerCase();
        ext = ext.replaceAll("-", " ");
        ext = ext.replaceAll("[\\s\\p{Punct}]", " ");
        return ext;
    }

    /**
		 * Extract important part from the string
		 * Needs the prepared string in previous method
		 * @param in prepared string
		 * @return string to searched
		 * @throws Exception
		 */
    private String extraction(String in) throws Exception {
        String res = "";
        int cur = 0;
        int index = -1;
        int k1;
        int k2;
        if ((in.indexOf("###111###", cur) == -1) && (in.indexOf("###000###", cur) == -1)) {
            System.out.println("************************ERROR********************");
            System.out.println("CAUTION, the Genbank format may have been changed, will try again, if infinite loop rewrite compare class!");
            System.out.println("gi :" + gi);
            return getInfo();
        }
        while ((index = in.indexOf("###111###", cur)) != -1) {
            k1 = in.indexOf("###111###", index + 1);
            k2 = in.indexOf("###000###", index + 1);
            if (k1 == -1) k1 = in.length();
            if (k2 == -1) k2 = in.length();
            res = res.concat(in.substring(index + 2, Math.min(k1, k2)));
            cur = Math.min(k1, k2);
        }
        return res;
    }

    /**
	 * findAntiKeyword indicates if a file contains one of the anti-keywords present in the list of anti key-words
	 * @param list the list of anti-keywords that we are looking for
	 * @param infile the file in which we are looking for the anti-keywords
	 * @return a boolean: true if an anti-keyword has been found and false otherwise
	 */
    private boolean findAntiKeyword(String[] list, String line) throws Exception {
        for (int i = 0; i < list.length; i++) {
            if (OnlyOneWord(line, list[i])) {
                return true;
            }
        }
        return false;
    }

    /**
		 * Tells if all the different words of a keyword are present in the String line 
		 * @param line the String where we are looking for all the different words of a keyword
		 * @param keyword String that we have to divided in all its differents words
		 * @return a boolean indicating if all the different words of a keyword were found in the file or not
		 * (true if they were all in, and false otherwise)
		 */
    private boolean OnlyOneWord(String line, String keyword) throws Exception {
        String cour;
        keyword = keyword.trim();
        keyword = keyword.replaceAll("[\\s\\p{Punct}]", " ");
        keyword = keyword.replaceAll("-", " ");
        keyword = keyword.replaceAll("\\s", " ");
        cour = " ".concat(keyword.concat(" "));
        if (line.indexOf(cour) == -1) {
            return false;
        }
        bb[num_frame][num_sub_sequence].write(" for the gi: " + gi + ", real keyword: " + cour + " found in this: " + line);
        bb[num_frame][num_sub_sequence].newLine();
        return true;
    }

    /**
		 * findKeyword indicates if a file contains one of the keywords present in the list of keywords
		 * More precisely, a file needs to contains a keyword and the note associated to it (if it exit) to be considered as "containing a keyword".
		 * @param list the list of keywords that we are looking for
		 * @param infile the file in which we are looking for the keywords
		 * @return a boolean if a keyword has been found or not (true if one has been found and false otherwise)
		 */
    private String[] findKeyword(String[][] list, String in) throws Exception {
        String[] res = new String[2];
        for (int i = 0; i < list.length; i++) {
            if (OnlyOneWord(in, list[i][0])) {
                if (list[i][1] == null) {
                    res[0] = list[i][2];
                    res[1] = list[i][0];
                    return res;
                } else {
                    if (in.indexOf(list[i][1]) != -1) {
                        res[0] = list[i][2];
                        res[1] = list[i][0];
                        return res;
                    }
                }
            }
        }
        res[0] = "";
        res[1] = "";
        return res;
    }

    /**
		 * Tell if a file can be considered as a Select Agent or Toxin or not
		 * @param in file containing informations to be scanned
		 * @param sc set of parameters of the current screening
		 * @return true if one of the keywords is find
		 */
    public synchronized boolean compare(String in, Screen sc) throws Exception {
        try {
            String[] S = findKeyword(sc.get_keywords(), in);
            String s = S[0];
            if (!(s.equals(""))) {
                if (sc.get_antikey() == 0) {
                    bb[num_frame][num_sub_sequence].write(" for the gi: " + gi + ", keyword: " + S[1] + " found in this: " + in);
                    bb[num_frame][num_sub_sequence].newLine();
                    SA = s;
                    return true;
                }
                Keywords K = new Keywords(sc.get_connection());
                s = s.trim();
                s = s.replaceAll("\\s", " ");
                String[] antikeywords = K.getAntiKeywords(s);
                if (!findAntiKeyword(antikeywords, in)) {
                    bb[num_frame][num_sub_sequence].write(" for the gi: " + gi + ", keyword: " + S[1] + " found in this: " + in);
                    bb[num_frame][num_sub_sequence].newLine();
                    SA = s;
                    return true;
                }
                bb[num_frame][num_sub_sequence].write(" for the gi: " + gi + ", keyword: " + S[1] + " found (but an anti-keyword was also found) in this: " + in);
                bb[num_frame][num_sub_sequence].newLine();
                return false;
            } else {
                bb[num_frame][num_sub_sequence].write(" for the gi: " + gi + ", no keyword found in this: " + in);
                bb[num_frame][num_sub_sequence].newLine();
                return false;
            }
        } catch (IOException e) {
            System.out.println("**************************ERROR**************************");
            System.out.println("error while writing the sequence on file");
        }
        return false;
    }

    /**
	 * Indicates if the attribute gi can be considered as a select agent or not 
	 * @return true if it's a select agent and false otherwise
	 */
    public synchronized boolean present(Screen sc) throws Exception {
        if (sc.get_GiScreened().containsKey(gi)) {
            boolean b = (sc.get_GiScreened().get(gi)).equals("");
            if (!b) {
                bb[num_frame][num_sub_sequence].write("The gi: " + gi + " has already been scanned before. The result was: it's a Select Agent.");
                bb[num_frame][num_sub_sequence].newLine();
            } else {
                bb[num_frame][num_sub_sequence].write("The gi: " + gi + " has already been scanned before. The result was: it's a NOT Select Agent.");
                bb[num_frame][num_sub_sequence].newLine();
            }
            SA = sc.get_GiScreened().get(gi);
            return !b;
        }
        String inf = getInfo();
        boolean res = compare(inf, sc);
        String ishit = "";
        if (res) {
            ishit = SA;
        }
        sc.get_GiScreened().put(gi, ishit);
        return res;
    }
}
