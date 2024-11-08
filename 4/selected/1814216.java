package org.exmaralda.orthonormal.lexicon;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import org.exmaralda.common.helpers.LevenshteinComparator;
import org.exmaralda.common.jdomutilities.IOUtilities;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 *
 * @author thomas
 */
public class GlobalNeighbourhood {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            if (args.length != 4) {
                System.out.println("Usage: GlobalNeighborhood in.txt combinations.txt lexicon.xml out.xml");
                System.out.println("   in.txt - the path to the input file with child forms");
                System.out.println("   combinations.txt - the path to the input file with the compound phonemes");
                System.out.println("   lexicon.xml - the path to the phonetic lexicon");
                System.out.println("   out.xml - the output file to be written");
                System.exit(1);
            }
            GlobalNeighbourhood l = new GlobalNeighbourhood();
            l.doit(args);
        } catch (JDOMException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    static String FILENAME = "S:\\TP-E3\\Aleksandra\\PBU_Z2\\LexiconProfiles\\PAIDUS_Johannes_phon_Nachbarschaftsdichte_Aug_27.txt";

    static String XML_OUT = "S:\\TP-E3\\Aleksandra\\PBU_Z2\\LexiconProfiles\\PAIDUS_Johannes_phon_Nachbarschaftsdichte_Global_Aug_27_Phon_Celex.xml";

    static String LEXICON_NAME = "S:\\TP-E3\\Aleksandra\\PBU_Z2\\lexicon\\German-celex.xml";

    static String COMBINATIONS_NAME = "S:\\TP-E3\\Aleksandra\\PBU_Z2\\LexiconProfiles\\Phoneme_Combinations.txt";

    private void doit(String[] args) throws JDOMException, IOException {
        FILENAME = args[0];
        COMBINATIONS_NAME = args[1];
        LEXICON_NAME = args[2];
        XML_OUT = args[3];
        Document d = IOUtilities.readDocumentFromLocalFile(LEXICON_NAME);
        PhoneticLexicon lexicon = new PhoneticLexicon(d);
        System.out.println("Lexicon read.");
        Vector<String> combinations = new Vector<String>();
        FileInputStream fis2 = new FileInputStream(COMBINATIONS_NAME);
        InputStreamReader isr2 = new InputStreamReader(fis2, "UTF-8");
        BufferedReader myInput2 = new BufferedReader(isr2);
        String nextLine2 = new String();
        while ((nextLine2 = myInput2.readLine()) != null) {
            if ((nextLine2.length() > 0) && (!(nextLine2.startsWith("//")))) {
                combinations.add(nextLine2);
            }
        }
        System.out.println(combinations.size() + " combinations read.");
        Vector<String> orthoList = new Vector<String>();
        Vector<String> phonList = new Vector<String>();
        Vector<String> localOrthoList = new Vector<String>();
        Vector<String> localPhonList = new Vector<String>();
        for (PhoneticLexiconEntry ple : lexicon.entries) {
            orthoList.addElement(ple.lemma);
            phonList.addElement(ple.ph);
        }
        FileInputStream fis = new FileInputStream(FILENAME);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader myInput = new BufferedReader(isr);
        String nextLine = new String();
        while ((nextLine = myInput.readLine()) != null) {
            System.out.println("Read line " + nextLine);
            String[] items = nextLine.split("\\t");
            String orthoWord = items[2];
            if (orthoWord.contains(" ")) {
                orthoWord = orthoWord.substring(0, orthoWord.indexOf(" "));
            }
            System.out.println("Processing " + orthoWord);
            PhoneticLexiconEntry entry = lexicon.getEntry(orthoWord);
            if (entry == null) {
                String capitalizedOrthoWord = orthoWord.substring(0, 1).toUpperCase() + orthoWord.substring(1);
                entry = lexicon.getEntry(capitalizedOrthoWord);
            }
            String ph = "";
            if (entry != null) {
                ph = entry.ph;
            }
            localOrthoList.addElement(orthoWord);
            localPhonList.addElement(ph);
        }
        Document doc = new Document();
        Element root = new Element("neighbourhood-profile");
        doc.setRootElement(root);
        int count = 0;
        for (String pho : localPhonList) {
            String orth = localOrthoList.elementAt(count);
            Element entry = new Element("entry");
            entry.setAttribute("orth", orth);
            entry.setAttribute("phon", pho);
            List<String> neighbours = null;
            HashSet<String> done = new HashSet<String>();
            if (pho.length() > 0) {
                neighbours = LevenshteinComparator.getNeighbours(pho, phonList, 1, combinations);
                for (String nb : neighbours) {
                    if (nb.length() == 0) continue;
                    int index = phonList.indexOf(nb);
                    String ortho = orthoList.elementAt(index);
                    if (done.contains(ortho)) continue;
                    Element neighbour = new Element("neighbour");
                    neighbour.setAttribute("orth", ortho);
                    neighbour.setAttribute("phon", nb);
                    entry.addContent(neighbour);
                    done.add(ortho);
                }
            }
            root.addContent(entry);
            count++;
            System.out.println(count + "/" + localPhonList.size() + ": " + orth);
        }
        IOUtilities.writeDocumentToLocalFile(XML_OUT, doc);
        System.out.println("Output written to " + XML_OUT);
    }
}
