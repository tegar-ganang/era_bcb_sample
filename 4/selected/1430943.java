package se.infact.publish;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import se.infact.domain.Attachment;
import se.infact.domain.Bubble;
import se.infact.domain.Logo;
import se.infact.domain.Node;
import se.infact.domain.Participants;
import se.infact.domain.Presentation;
import se.infact.util.StringEncoder;

/**
 * @author David Granqvist
 * @author Malte Lenz
 * @author Johannes Nordkvist
 *
 */
public class InfactXmlGenerator {

    private String publishDirectory;

    private String previewDirectory;

    private String flashDir;

    private String previewUrl;

    private String publishUrl;

    private String infactUrl;

    private String generationDirectory;

    private String infactDirectoryName;

    private String uploadedFilesDir = "uploaded_files";

    public String publish(List<Presentation> presentationList, String publishName, boolean createZipFile) throws IOException {
        publishName = StringEncoder.makeLinkSafe(publishName);
        File infactFamilyDirectory = new File(publishDirectory, publishName);
        infactFamilyDirectory.mkdir();
        generationDirectory = infactFamilyDirectory.getPath();
        String mainPublicationName = "";
        for (Presentation presentation : presentationList) {
            infactDirectoryName = presentation.getId() + "-" + StringEncoder.makeLinkSafe(presentation.getName());
            createInfactDirectoryStructure();
            generateInfact(presentation, presentationList, true);
            if (mainPublicationName.contentEquals("")) {
                mainPublicationName = infactDirectoryName;
            }
        }
        createRedirectingIndex(mainPublicationName);
        if (createZipFile) {
            return createZipFromPublication(publishName);
        } else {
            return publishName;
        }
    }

    public String preview(Presentation presentation) throws IOException {
        generationDirectory = previewDirectory;
        infactDirectoryName = presentation.getId() + "-" + StringEncoder.makeLinkSafe(presentation.getName());
        createInfactDirectoryStructure();
        generateInfact(presentation, new ArrayList<Presentation>(), false);
        return infactDirectoryName;
    }

    private void generateInfact(Presentation presentation, List<Presentation> presentationList, boolean publish) throws IOException {
        String copyToDirectory = new File(generationDirectory, infactDirectoryName).getPath();
        copyFlashFiles(copyToDirectory, publish, presentation.isPlanet());
        generateXml(presentation, presentationList, publish);
        copyLogos(presentation);
    }

    private String createInfactDirectoryStructure() throws IOException {
        File presDir = new File(generationDirectory, infactDirectoryName);
        FileUtils.deleteQuietly(presDir);
        FileUtils.forceMkdir(presDir);
        File xmlDir = new File(presDir, "xml");
        xmlDir.mkdir();
        File uploadDir = new File(presDir, "uploaded_files");
        uploadDir.mkdir();
        File logosDir = new File(presDir, "logos");
        logosDir.mkdir();
        return presDir.getPath();
    }

    /** writes the attached file to the target Directory
	 * @param attachment the attached file
	 * @param presentation the associated presentation; we pull it's id to determine correct path to place the file
	 * @param targetDir 
	 * @param stripID
	 * @return the complete path of the new file
	 * @throws IOException
	 */
    public String writeAttachmentToFile(Attachment attachment, Presentation presentation, String targetDir, boolean stripID) throws IOException {
        if (attachment == null) {
            return "";
        } else {
            String fileName;
            String uploadDir = new File(new File(generationDirectory, presentation.getId() + "-" + StringEncoder.makeLinkSafe(presentation.getName())), targetDir).getPath();
            if (stripID) {
                fileName = attachment.getName();
            } else {
                fileName = attachment.getId() + "-" + attachment.getName();
            }
            String path = new File(uploadDir, fileName).getPath();
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(attachment.getData());
            fos.close();
            return targetDir + "/" + fileName;
        }
    }

    private String createZipFromPublication(String infactFamilyName) throws IOException {
        String zipName = infactFamilyName + ".zip";
        FileOutputStream fos = new FileOutputStream(new File(publishDirectory, zipName).getPath());
        ZipOutputStream zos = new ZipOutputStream(fos);
        zipDir(zos, publishDirectory, infactFamilyName);
        zos.close();
        fos.close();
        return zipName;
    }

    private void generateXml(Presentation presentation, List<Presentation> presentationList, boolean publish) throws IOException {
        String infactDirectory = new File(generationDirectory, infactDirectoryName).getPath();
        String xmlDir = new File(infactDirectory, "xml").getPath();
        XmlWriter writer = XmlWriterFactory.getWriter(new File(xmlDir, "structure.xml").getPath());
        boolean hasParticipants = presentation.getParticipants() != null;
        writer.writeHeader();
        writer.writeStartTag("xml");
        writer.writeStartTag("general");
        writer.writeSimpleTag("name", presentation.getName());
        if (!publish) {
            writer.writeSimpleTag("debug", "true");
        }
        if (presentation.getBgImage() != null) {
            writer.writeSimpleTag("background", writeAttachmentToFile(presentation.getBgImage(), presentation, uploadedFilesDir, false));
        }
        writer.writeSimpleTag("hoverColor", "#CCFFCC");
        writer.writeSimpleTag("visitedColor", "#CCCCCC");
        String fontStr = presentation.getFont();
        if (fontStr == null || fontStr.equals("")) {
            fontStr = "Arial, sans-serif;";
        }
        writer.writeSimpleTag("fonts", fontStr);
        writer.writeSimpleTag("nodes", "nodes.swf");
        writer.writeSimpleTag("defaultNode", "nodes.templates.DefaultNode");
        writer.writeSimpleTag("defaultRadius", "70");
        writer.writeSimpleTag("leafRoot", "xml/");
        writer.writeSimpleTag("searchXML", "xml/search.xml");
        writer.writeSimpleTag("distance", presentation.getStickDistance());
        if (hasParticipants) {
            writer.writeSimpleTag("participantsXML", "xml/participants.xml");
        }
        if (presentationList.size() > 1) {
            writeLanguagesTags(writer, presentation, presentationList);
        }
        writer.writeSimpleTag("animatedBackground", booleanToString(presentation.isAnimatedBackground()));
        writer.writeSimpleTag("tooltip", booleanToString(presentation.isToolTip()));
        writer.writeStartTag("logos");
        Properties attributes = new Properties();
        List<Logo> logoList = presentation.getLogos();
        for (Logo logo : logoList) {
            attributes.setProperty("url", logo.getUrl());
            writer.writeSimpleTag("logo", "logos/" + logo.getImage().getId() + "-" + logo.getImage().getName(), attributes);
        }
        writer.writeEndTag();
        writer.writeEndTag();
        writer.writeStartTag("languageTexts");
        writer.writeSimpleTag("otherLang", "Andra Språk");
        writeLocalizedTags(presentation, writer);
        writer.writeEndTag();
        StructureWriterVisitor visitor = new StructureWriterVisitor(writer, infactDirectory, this);
        Node startNode = presentation.getStartnode();
        startNode.accept(visitor);
        writer.writeEndTag();
        writer.closeXmlWriter();
        createSearchXml(infactDirectory, xmlDir, presentation);
        if (hasParticipants) {
            createParticipantsXml(xmlDir, presentation);
        }
    }

    private void writeLocalizedTags(Presentation presentation, XmlWriter writer) throws IOException {
        if (presentation.getLanguage().getId() == 1) {
            writer.writeSimpleTag("searchresult", "Rezultatet e kërkimit");
            writer.writeSimpleTag("searchhits", "# hits");
            writer.writeSimpleTag("related", "të lidhura");
            writer.writeSimpleTag("bubble", "Flluskë");
            writer.writeSimpleTag("movie", "Film");
            writer.writeSimpleTag("textimage", "tekst/imazh");
            writer.writeSimpleTag("timeline", "afat kohor");
            writer.writeSimpleTag("textline", "tekst/afat kohor");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Slideshow");
            writer.writeSimpleTag("planetflash", "animacion interaktive");
            writer.writeSimpleTag("planetmovie", "video interaktive");
        } else if (presentation.getLanguage().getId() == 2) {
            writer.writeSimpleTag("searchresult", "Резултати от търсенето");
            writer.writeSimpleTag("searchhits", "# хитове");
            writer.writeSimpleTag("related", "свързани с");
            writer.writeSimpleTag("bubble", "Мехур");
            writer.writeSimpleTag("movie", "Филм");
            writer.writeSimpleTag("textimage", "текст/снимка");
            writer.writeSimpleTag("timeline", "график");
            writer.writeSimpleTag("textline", "текст/график");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "слайдшоуто");
            writer.writeSimpleTag("planetflash", "интерактивна анимация");
            writer.writeSimpleTag("planetmovie", "интерактивна видео");
        } else if (presentation.getLanguage().getId() == 3) {
            writer.writeSimpleTag("searchresult", "Rezultati pretraživanja");
            writer.writeSimpleTag("searchhits", "# Hitova");
            writer.writeSimpleTag("related", "vezane");
            writer.writeSimpleTag("bubble", "Mjehurić");
            writer.writeSimpleTag("movie", "Film");
            writer.writeSimpleTag("textimage", "tekst/slika");
            writer.writeSimpleTag("timeline", "timeline");
            writer.writeSimpleTag("textline", "tekst/timeline");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Slideshow");
            writer.writeSimpleTag("planetflash", "interaktivna animacija");
            writer.writeSimpleTag("planetmovie", "interaktivni video");
        } else if (presentation.getLanguage().getId() == 4) {
            writer.writeSimpleTag("searchresult", "Výsledky hledání");
            writer.writeSimpleTag("searchhits", "# hity");
            writer.writeSimpleTag("related", "související");
            writer.writeSimpleTag("bubble", "Bublina");
            writer.writeSimpleTag("movie", "Film");
            writer.writeSimpleTag("textimage", "text/obrázek");
            writer.writeSimpleTag("timeline", "časové ose");
            writer.writeSimpleTag("textline", "text/časové ose");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Slideshow");
            writer.writeSimpleTag("planetflash", "interaktivní animace");
            writer.writeSimpleTag("planetmovie", "interaktivní video");
        } else if (presentation.getLanguage().getId() == 6) {
            writer.writeSimpleTag("searchresult", "Zoekresultaten");
            writer.writeSimpleTag("searchhits", "# hits");
            writer.writeSimpleTag("related", "gerelateerde");
            writer.writeSimpleTag("bubble", "Zeepbel");
            writer.writeSimpleTag("movie", "Film");
            writer.writeSimpleTag("textimage", "Tekst/Beeld");
            writer.writeSimpleTag("timeline", "tijdlijn");
            writer.writeSimpleTag("textline", "Tekst/tijdlijn");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Slideshow");
            writer.writeSimpleTag("planetflash", "interactieve animatie");
            writer.writeSimpleTag("planetmovie", "interactieve video");
        } else if (presentation.getLanguage().getId() == 8) {
            writer.writeSimpleTag("searchresult", "Hakutulokset");
            writer.writeSimpleTag("searchhits", "# osumia");
            writer.writeSimpleTag("related", "liittyvä");
            writer.writeSimpleTag("bubble", "Kupla");
            writer.writeSimpleTag("movie", "Elokuva");
            writer.writeSimpleTag("textimage", "Teksti/Kuva");
            writer.writeSimpleTag("timeline", "aikajana");
            writer.writeSimpleTag("textline", "Teksti/aikajana");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "diaesitys");
            writer.writeSimpleTag("planetflash", "interaktiivinen animaatio");
            writer.writeSimpleTag("planetmovie", "interaktiivinen video");
        } else if (presentation.getLanguage().getId() == 9) {
            writer.writeSimpleTag("searchresult", "Résultats de la recherche");
            writer.writeSimpleTag("searchhits", "# hits");
            writer.writeSimpleTag("related", "liées");
            writer.writeSimpleTag("bubble", "Bulle");
            writer.writeSimpleTag("movie", "Film");
            writer.writeSimpleTag("textimage", "Texte/Image");
            writer.writeSimpleTag("timeline", "Calendrier");
            writer.writeSimpleTag("textline", "Texte/Calendrier");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Diaporama");
            writer.writeSimpleTag("planetflash", "animation interactive");
            writer.writeSimpleTag("planetmovie", "vidéo interactive");
        } else if (presentation.getLanguage().getId() == 10) {
            writer.writeSimpleTag("searchresult", "Suchergebnisse");
            writer.writeSimpleTag("searchhits", "# Hits");
            writer.writeSimpleTag("related", "Sie");
            writer.writeSimpleTag("bubble", "Blase");
            writer.writeSimpleTag("movie", "Film");
            writer.writeSimpleTag("textimage", "Text/Bild");
            writer.writeSimpleTag("timeline", "Zeitleiste");
            writer.writeSimpleTag("textline", "Text/Zeitleiste");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Diashow");
            writer.writeSimpleTag("planetflash", "interaktive Animation");
            writer.writeSimpleTag("planetmovie", "interaktive Video");
        } else if (presentation.getLanguage().getId() == 11) {
            writer.writeSimpleTag("searchresult", "Αποτελέσματα αναζήτησης");
            writer.writeSimpleTag("searchhits", "# Επισκέψεις");
            writer.writeSimpleTag("related", "συναφείς");
            writer.writeSimpleTag("bubble", "φυσαλλίδα");
            writer.writeSimpleTag("movie", "Ταινία");
            writer.writeSimpleTag("textimage", "κείμενο/Εικόνα");
            writer.writeSimpleTag("timeline", "χρονοδιάγραμμα");
            writer.writeSimpleTag("textline", "κείμενο/χρονοδιάγραμμα");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Slideshow");
            writer.writeSimpleTag("planetflash", "διαδραστικό animation");
            writer.writeSimpleTag("planetmovie", "διαδραστικά βίντεο");
        } else if (presentation.getLanguage().getId() == 12) {
            writer.writeSimpleTag("searchresult", "A keresés eredménye");
            writer.writeSimpleTag("searchhits", "# találat");
            writer.writeSimpleTag("related", "kapcsolódó");
            writer.writeSimpleTag("bubble", "Buborék");
            writer.writeSimpleTag("movie", "Film");
            writer.writeSimpleTag("textimage", "szöveg/kép");
            writer.writeSimpleTag("timeline", "idősor");
            writer.writeSimpleTag("textline", "szöveg/idősor");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "diavetítés");
            writer.writeSimpleTag("planetflash", "interaktív animáció");
            writer.writeSimpleTag("planetmovie", "interaktív videó");
        } else if (presentation.getLanguage().getId() == 13) {
            writer.writeSimpleTag("searchresult", "Leitarniðurstöður");
            writer.writeSimpleTag("searchhits", "# hits");
            writer.writeSimpleTag("related", "tengdar");
            writer.writeSimpleTag("bubble", "Kúla");
            writer.writeSimpleTag("movie", "kvikmynd");
            writer.writeSimpleTag("textimage", "Texti/mynd");
            writer.writeSimpleTag("timeline", "Tímalína");
            writer.writeSimpleTag("textline", "Texti/Tímalína");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Slideshow");
            writer.writeSimpleTag("planetflash", "gagnvirka hreyfimynd");
            writer.writeSimpleTag("planetmovie", "gagnvirka vídeó");
        } else if (presentation.getLanguage().getId() == 14) {
            writer.writeSimpleTag("searchresult", "Risultati della ricerca");
            writer.writeSimpleTag("searchhits", "# hits");
            writer.writeSimpleTag("related", "correlate");
            writer.writeSimpleTag("bubble", "Bolla");
            writer.writeSimpleTag("movie", "Film");
            writer.writeSimpleTag("textimage", "Testo/immagine");
            writer.writeSimpleTag("timeline", "Timeline");
            writer.writeSimpleTag("textline", "Testo/Timeline");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "proiezione di diapositive");
            writer.writeSimpleTag("planetflash", "animazione interattiva");
            writer.writeSimpleTag("planetmovie", "video interattivo");
        } else if (presentation.getLanguage().getId() == 15) {
            writer.writeSimpleTag("searchresult", "Søkeresultater");
            writer.writeSimpleTag("searchhits", "# treff");
            writer.writeSimpleTag("related", "relaterte");
            writer.writeSimpleTag("bubble", "Bubbla");
            writer.writeSimpleTag("movie", "Film");
            writer.writeSimpleTag("textimage", "Tekst/Image");
            writer.writeSimpleTag("timeline", "Tidslinje");
            writer.writeSimpleTag("textline", "Tekst/Tidslinje");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Slideshow");
            writer.writeSimpleTag("planetflash", "interaktiv animasjon");
            writer.writeSimpleTag("planetmovie", "interaktiv video");
        } else if (presentation.getLanguage().getId() == 16) {
            writer.writeSimpleTag("searchresult", "Resultados da pesquisa");
            writer.writeSimpleTag("searchhits", "# hits");
            writer.writeSimpleTag("related", "relacionados");
            writer.writeSimpleTag("bubble", "Bolha");
            writer.writeSimpleTag("movie", "Filme");
            writer.writeSimpleTag("textimage", "Texto/Imagem");
            writer.writeSimpleTag("timeline", "Cronograma");
            writer.writeSimpleTag("textline", "Texto/Cronograma");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Slideshow");
            writer.writeSimpleTag("planetflash", "animação interativa");
            writer.writeSimpleTag("planetmovie", "vídeo interativo");
        } else if (presentation.getLanguage().getId() == 17) {
            writer.writeSimpleTag("searchresult", "Resultados de la búsqueda");
            writer.writeSimpleTag("searchhits", "# hits");
            writer.writeSimpleTag("related", "relacionados");
            writer.writeSimpleTag("bubble", "Burbuja");
            writer.writeSimpleTag("movie", "Película");
            writer.writeSimpleTag("textimage", "Texto/Imagen");
            writer.writeSimpleTag("timeline", "línea de tiempo");
            writer.writeSimpleTag("textline", "Texto/línea de tiempo");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "presentación de diapositivas");
            writer.writeSimpleTag("planetflash", "animación interactiva");
            writer.writeSimpleTag("planetmovie", "vídeo interactivo");
        } else if (presentation.getLanguage().getId() == 18) {
            writer.writeSimpleTag("searchresult", "Sökresultat");
            writer.writeSimpleTag("searchhits", "# träffar");
            writer.writeSimpleTag("related", "relaterad");
            writer.writeSimpleTag("bubble", "Bubbla");
            writer.writeSimpleTag("movie", "Film");
            writer.writeSimpleTag("textimage", "Text/Bild");
            writer.writeSimpleTag("timeline", "Tidslinje");
            writer.writeSimpleTag("textline", "Text/bild-linje");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Bildspel");
            writer.writeSimpleTag("planetflash", "Interaktiv animation");
            writer.writeSimpleTag("planetmovie", "Interaktiv video");
        } else {
            writer.writeSimpleTag("searchresult", "Search results");
            writer.writeSimpleTag("searchhits", "# hits");
            writer.writeSimpleTag("related", "related");
            writer.writeSimpleTag("bubble", "Bubble");
            writer.writeSimpleTag("movie", "Movie");
            writer.writeSimpleTag("textimage", "Text/Image");
            writer.writeSimpleTag("timeline", "Timeline");
            writer.writeSimpleTag("textline", "Text/Timeline");
            writer.writeSimpleTag("flash", "Flash");
            writer.writeSimpleTag("slideshow", "Bildspel");
            writer.writeSimpleTag("planetflash", "Interactive animation");
            writer.writeSimpleTag("planetmovie", "Interactive movie");
        }
    }

    private void writeLanguagesTags(XmlWriter writer, Presentation presentation, List<Presentation> presentationList) throws IOException {
        writer.writeStartTag("languages");
        Properties attributes = new Properties();
        attributes.setProperty("url", "../" + presentation.getId() + "-" + StringEncoder.makeLinkSafe(presentation.getName()) + "/index.html");
        writer.writeSimpleTag("lang", presentation.getLanguage().getName(), attributes);
        for (Presentation linkedPresentation : presentationList) {
            if (linkedPresentation.getId() != presentation.getId()) {
                attributes.clear();
                attributes.setProperty("url", "../" + linkedPresentation.getId() + "-" + StringEncoder.makeLinkSafe(linkedPresentation.getName()) + "/index.html");
                writer.writeSimpleTag("lang", linkedPresentation.getLanguage().getName(), attributes);
            }
        }
        writer.writeEndTag();
    }

    private void createSearchXml(String infactDir, String xmlDir, Presentation presentation) throws IOException {
        XmlWriter writer = XmlWriterFactory.getWriter(new File(xmlDir, "search.xml").getPath());
        writer.writeHeader();
        writer.writeStartTag("search");
        writeSearchTags(infactDir, writer, presentation.getStartnode());
        writer.writeEndTag();
        writer.closeXmlWriter();
    }

    private void writeSearchTags(String infactDir, XmlWriter writer, Node node) throws IOException {
        writer.writeStartTag("res");
        writer.writeSimpleTag("id", node.getId().toString());
        writer.writeSimpleTag("type", node.getTypeString());
        writer.writeSimpleTag("title", node.getName());
        String result = StringEncoder.makeSearchable(node.getContent(infactDir));
        writer.writeSimpleTag("text", result);
        writer.writeEndTag();
        if (node.getTypeString().equals("bubble")) {
            Bubble bubble = (Bubble) node;
            for (Node subnode : bubble.getSubnodes()) {
                writeSearchTags(infactDir, writer, subnode);
            }
        }
    }

    private void createParticipantsXml(String xmlDir, Presentation presentation) throws IOException {
        XmlWriter writer = XmlWriterFactory.getWriter(new File(xmlDir, "participants.xml").getPath());
        Participants participants = presentation.getParticipants();
        writer.writeHeader();
        writer.writeStartTag("xml");
        writer.writeSimpleTag("template", "leaves/templates/textimage/textimage.swf");
        Properties attributes = new Properties();
        if (participants.getImage() != null) {
            attributes.setProperty("url", writeAttachmentToFile(participants.getImage(), presentation, uploadedFilesDir, false));
            writer.writeSelfClosingTag("image", attributes);
        }
        writer.writeStartTag("texts");
        writer.writeSimpleTag("title", participants.getName());
        writer.writeStartTag("text");
        writer.writeCdata(XmlGeneratorUtil.replaceSpecialChars(participants.getText()));
        writer.writeEndTag();
        writer.writeEndTag();
        writer.writeSimpleTag("related", "");
        writer.writeEndTag();
        writer.closeXmlWriter();
    }

    private void regexReplaceAllInFile(String filepath, String regex, String replacement) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(filepath));
        String line, text = "";
        while ((line = in.readLine()) != null) {
            text += line + "\n";
        }
        in.close();
        text = text.replaceAll(regex, replacement);
        BufferedWriter out = new BufferedWriter(new FileWriter(filepath));
        out.write(text);
        out.close();
    }

    private void copyFlashFiles(String rootDir, boolean publish, boolean isPlanet) throws IOException {
        File destDir = new File(rootDir);
        FileUtils.copyFileToDirectory(new File(flashDir, "expressInstall.swf"), destDir);
        FileUtils.copyFileToDirectory(new File(flashDir, "infact.swf"), destDir);
        FileUtils.copyFileToDirectory(new File(flashDir, "nodes.swf"), destDir);
        FileUtils.copyFileToDirectory(new File(flashDir, "as2bridge.swf"), destDir);
        FileUtils.copyFileToDirectory(new File(flashDir, "as2_tubeloc.swf"), destDir);
        FileUtils.copyFileToDirectory(new File(flashDir, "index.html"), destDir);
        if (!publish) {
            regexReplaceAllInFile(destDir + File.separator + "index.html", "flashvars\\.baseurl\\s*=\\s*\".*?\"", "flashvars.baseurl = \"" + getInfactUrl() + "\"");
        }
        FileUtils.copyFileToDirectory(new File(flashDir, "MinimaSilverPlayBackSeekMute.swf"), destDir);
        FileUtils.copyFileToDirectory(new File(flashDir, "MinimaUnderPlayBackSeekCounterVolMuteFull.swf"), destDir);
        FileUtils.copyDirectoryToDirectory(new File(flashDir, "js"), destDir);
        if (isPlanet) {
            FileUtils.copyFileToDirectory(new File(flashDir, "planeten.swf"), destDir);
            FileUtils.copyDirectoryToDirectory(new File(flashDir, "leaves"), destDir);
            FileUtils.copyDirectory(new File(flashDir, "templates" + File.separator + "KS_video"), new File(destDir, "templates" + File.separator + "KS_video"));
            File iVideoDir = new File(rootDir, "media" + File.separator + "ivideo" + File.separator + "1337");
            FileUtils.forceMkdir(iVideoDir);
            File pFlashDir = new File(rootDir, "media" + File.separator + "flash" + File.separator + "1337");
            FileUtils.forceMkdir(pFlashDir);
            FileUtils.copyFileToDirectory(new File(flashDir, "scrubduck_vr.swf"), pFlashDir);
            File ksDir = new File(rootDir, "ks" + File.separator + "1337");
            FileUtils.forceMkdir(ksDir);
            FileUtils.copyFileToDirectory(new File(flashDir, "signkyoto_vr.swf"), ksDir);
            File movieDir = new File(rootDir, "media" + File.separator + "movie" + File.separator + "1337");
            FileUtils.forceMkdir(movieDir);
        } else {
            FileFilter filter = FileFilterUtils.notFileFilter(FileFilterUtils.orFileFilter(FileFilterUtils.nameFileFilter("planet"), FileFilterUtils.nameFileFilter("planetvideo")));
            FileUtils.copyDirectory(new File(flashDir, "leaves"), new File(rootDir, "leaves"), filter);
        }
    }

    private void copyLogos(Presentation presentation) throws IOException {
        List<Logo> logoList = presentation.getLogos();
        String logosDir = "logos";
        for (Logo logo : logoList) {
            writeAttachmentToFile(logo.getImage(), presentation, logosDir, false);
        }
    }

    private void zipDir(ZipOutputStream zos, String rootDir, String dirToZip) throws IOException {
        File dirToZipAbs = new File(rootDir, dirToZip);
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        for (String file : dirToZipAbs.list()) {
            File fileToZip = new File(dirToZip, file);
            File fileToZipAbs = new File(dirToZipAbs, file);
            if (fileToZipAbs.isFile()) {
                ZipEntry ze = new ZipEntry(fileToZip.getPath());
                zos.putNextEntry(ze);
                FileInputStream fis = new FileInputStream(fileToZipAbs);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
                zos.closeEntry();
            } else {
                zipDir(zos, rootDir, fileToZip.getPath());
            }
        }
    }

    private void createRedirectingIndex(String mainPublicationName) throws IOException {
        XmlWriter writer = XmlWriterFactory.getWriter(new File(generationDirectory, "index.html").getPath());
        writer.writeStartTag("html");
        writer.writeStartTag("head");
        Properties attributes = new Properties();
        attributes.setProperty("http-equiv", "REFRESH");
        attributes.setProperty("content", "0;url=" + mainPublicationName + "/index.html");
        writer.writeSelfClosingTag("meta", attributes);
        writer.writeEndTag();
        writer.writeEndTag();
        writer.closeXmlWriter();
    }

    private String booleanToString(boolean bool) {
        String stringContent;
        if (bool) {
            stringContent = "true";
        } else {
            stringContent = "false";
        }
        return stringContent;
    }

    public void setFlashDir(String flashDir) {
        this.flashDir = flashDir;
    }

    public String getFlashDir() {
        return flashDir;
    }

    public void setPublishDirectory(String publishDirectory) {
        this.publishDirectory = publishDirectory;
    }

    public String getPublishDirectory() {
        return publishDirectory;
    }

    public void setPreviewDirectory(String previewDirectory) {
        this.previewDirectory = previewDirectory;
    }

    public String getPreviewDirectory() {
        return previewDirectory;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPublishUrl(String publishUrl) {
        this.publishUrl = publishUrl;
    }

    public String getPublishUrl() {
        return publishUrl;
    }

    public String getInfactUrl() {
        return this.infactUrl;
    }

    public void setInfactUrl(String infactUrl) {
        this.infactUrl = infactUrl;
    }
}
