package cz.vse.xkucf03.svnStatistika.jadro.vystup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import cz.vse.xkucf03.svnStatistika.jadro.mezivysledky.Adresar;
import cz.vse.xkucf03.svnStatistika.jadro.mezivysledky.Data;
import cz.vse.xkucf03.svnStatistika.jadro.vyjimky.SVNstatistikaChybaVystupu;

/** Převede mezivýsledky ze třídy Data na výstup: html + obrázky a uloží */
public class ZpracovaniVystupu {

    private Data data;

    private Document dokument;

    /** cesta k uložení výstupu */
    private String cestaProVystup;

    /** cesta k šabloně */
    private String cestaSablony;

    /** Log4j logovač */
    private static Logger log = Logger.getLogger(ZpracovaniVystupu.class);

    public ZpracovaniVystupu(Data data) throws SVNstatistikaChybaVystupu {
        this.data = data;
        cestaProVystup = data.getNastaveni().getVystup();
        if (!cestaProVystup.endsWith("/")) {
            cestaProVystup = cestaProVystup + "/";
        }
        vytvorAdresar(cestaProVystup);
        cestaSablony = data.getNastaveni().getSablona();
        if (!cestaSablony.endsWith("/")) {
            cestaSablony = cestaSablony + "/";
        }
        try {
            nakopirujStatickePrvky();
        } catch (SVNstatistikaChybaVystupu e) {
            log.warn("Nepodařilo se nakopírovat statické prvky", e);
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            dokument = builder.parse(cestaSablony + "index.html");
        } catch (ParserConfigurationException e) {
            throw new SVNstatistikaChybaVystupu("Chyba při načítání šablony", e);
        } catch (SAXException e) {
            throw new SVNstatistikaChybaVystupu("Chyba při načítání šablony", e);
        } catch (IOException e) {
            throw new SVNstatistikaChybaVystupu("Chyba při načítání šablony", e);
        }
    }

    /** Nakopíruje styl.css a obrázek pro odrážky ze šablony do výstupu */
    private void nakopirujStatickePrvky() throws SVNstatistikaChybaVystupu {
        try {
            kopirujSoubor(new File(cestaSablony + "styl.css"), new File(cestaProVystup + "styl.css"));
            kopirujSoubor(new File(cestaSablony + "dir.png"), new File(cestaProVystup + "dir.png"));
        } catch (Exception e) {
            throw new SVNstatistikaChybaVystupu("Nepodařilo se zkopírovat statické prvky ze šablony do výstupu", e);
        }
    }

    public static void kopirujSoubor(File vstup, File vystup) throws IOException {
        FileChannel sourceChannel = new FileInputStream(vstup).getChannel();
        FileChannel destinationChannel = new FileOutputStream(vystup).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    /** Hlavní metoda, vytvoří z dat výstup */
    public void zpracujVystup() throws SVNstatistikaChybaVystupu {
        setTextContent("souhrnCesta", data.getNastaveni().getUrl());
        setAttribute("souhrnCesta", "href", data.getNastaveni().getUrl());
        setTextContent("souhrnVerze", data.getVerzeUloziste());
        setTextContent("souhrnDatumVerze", Formatovac.formatuj(data.getDatum()));
        setTextContent("souhrnDatumStatistiky", Formatovac.formatuj(new Date()));
        setTextContent("souhrnSlozek", data.getPocetAdresaru());
        setTextContent("souhrnSouboru", data.getPocetSouboru());
        setTextContent("souhrnZmen", data.getPocetZmen());
        {
            pridejPodadresare(dokument.getElementById("stromovaStruktura"), data.getKorenovyAdresar());
        }
        Graf.ulozGrafPodleVerzi(data.getZmenyPodleVerzi(), cestaProVystup + "grafPodleVerzi.svg", true);
        Graf.ulozGrafUzivatelu(data.getZmenyUzivatelu(), cestaProVystup + "grafUzivatelu.svg", true);
        zapisDokument();
    }

    private void pridejPodadresare(Element element, Adresar adresar) {
        for (Adresar podadresar : adresar.getPotomci().values()) {
            Element ul = dokument.createElement("ul");
            Element li = dokument.createElement("li");
            li.setTextContent(podadresar.getNazev());
            ul.appendChild(li);
            element.appendChild(ul);
            pridejPodadresare(li, podadresar);
        }
    }

    private void setTextContent(String element, Object hodnota) {
        if (hodnota == null) {
            dokument.getElementById(element).setTextContent("");
        } else {
            dokument.getElementById(element).setTextContent(hodnota.toString());
        }
    }

    private void setAttribute(String element, String atribut, String hodnota) {
        if (hodnota == null) {
            dokument.getElementById(element).removeAttribute(atribut);
        } else {
            dokument.getElementById(element).setAttribute(atribut, hodnota);
        }
    }

    private void zapisDokument() throws SVNstatistikaChybaVystupu {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tFactory.newTransformer();
            DOMSource source = new DOMSource(dokument);
            FileOutputStream fos = new FileOutputStream(cestaProVystup + "index.html");
            StreamResult result = new StreamResult(fos);
            {
                DocumentType doctype = dokument.getDoctype();
                transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
            }
            transformer.transform(source, result);
            fos.close();
        } catch (TransformerConfigurationException e) {
            throw new SVNstatistikaChybaVystupu("Chyba při zápisu výstupu", e);
        } catch (TransformerException e) {
            throw new SVNstatistikaChybaVystupu("Chyba při zápisu výstupu", e);
        } catch (FileNotFoundException e) {
            throw new SVNstatistikaChybaVystupu("Chyba při zápisu výstupu", e);
        } catch (IOException e) {
            throw new SVNstatistikaChybaVystupu("Chyba při zápisu výstupu", e);
        }
    }

    /** Zkontroluje zadanou cestu. Pokud je potřeba, vytvoří ji.
     * Vrací true/false podle toho, jestli bylo nutné adresář vytvořit.
     */
    private boolean vytvorAdresar(String cesta) throws SVNstatistikaChybaVystupu {
        try {
            File adresar = new File(cesta);
            if (adresar.exists()) {
                return false;
            } else {
                adresar.mkdirs();
                return true;
            }
        } catch (Exception e) {
            throw new SVNstatistikaChybaVystupu("Nepodařilo se vytvořit výstupní adresář", e);
        }
    }
}
