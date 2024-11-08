package info.sharo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * @author Svetoslav Batchovski
 * @mailto: sharan4o@gmail.com
 * 
 */
public class DataFileExporter {

    /**
	 * exports data.txt files. Not recommended. Use data.xml files!!!
	 * @param movieInfoMap
	 * @param xmlDir
	 * @return
	 */
    public static boolean exportDataTxtFiles(HashMap<String, MovieInfo> movieInfoMap, File xmlDir) {
        String baseDir = xmlDir.getAbsolutePath() + "/dataFiles";
        for (String key : movieInfoMap.keySet()) {
            MovieInfo movieInfo = movieInfoMap.get(key);
            if (!movieInfo.getBaseFilename().equals("")) {
                String data;
                data = "VERSION=" + movieInfo.VERSION + "\n";
                data = data.concat("LANG=" + movieInfo.getLang() + "\n");
                data = data.concat("TITLE=" + movieInfo.getTitle() + "\n");
                data = data.concat("GENRE=" + movieInfo.getGenre() + "\n");
                data = data.concat("RUNTIME=" + movieInfo.getRuntime() + "\n");
                data = data.concat("DIRECTOR=" + movieInfo.getDirector() + "\n");
                data = data.concat("CAST=" + movieInfo.getCast() + "\n");
                data = data.concat("YEAR=" + movieInfo.getYear() + "\n");
                data = data.concat("RATING=" + movieInfo.getRating() + "\n");
                data = data.concat("AUDIO=" + movieInfo.getAudio() + "\n");
                data = data.concat("SUBS=" + movieInfo.getSubs() + "\n");
                data = data.concat("DETAILS=" + movieInfo.getDetails() + "\n");
                data = data.concat("RESOLUTION=" + movieInfo.getResolution() + "\n");
                data = data.concat("FORMAT=" + movieInfo.getFormat() + "\n");
                data = data.concat("CODEC=" + movieInfo.getCodec() + "\n");
                data = data.concat("SYNOPSIS=" + movieInfo.getSynopsis());
                FileWriter fileWriter;
                try {
                    boolean done = (new File(baseDir + "/" + movieInfo.getDir())).mkdirs();
                    copyImage(xmlDir.getAbsolutePath() + "/" + movieInfo.getBaseFilename() + ".jpg", baseDir + "/" + movieInfo.getDir() + "/" + "folder.jpg");
                    fileWriter = new FileWriter(baseDir + "/" + movieInfo.getDir() + "/data.txt");
                    fileWriter.write(data);
                    fileWriter.close();
                } catch (IOException e) {
                    System.out.println("Problem with the export of the data.txt-File");
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
	 * exports data.xml files
	 * @param movieInfoMap
	 * @param xmlDir
	 * @return
	 */
    public static boolean exportDataXmlFiles(HashMap<String, MovieInfo> movieInfoMap, File xmlDir) {
        String baseDir = xmlDir.getAbsolutePath() + "/dataFiles";
        for (String key : movieInfoMap.keySet()) {
            MovieInfo movieInfo = movieInfoMap.get(key);
            if (!movieInfo.getBaseFilename().equals("")) {
                SAXBuilder reader = new SAXBuilder();
                Document doc;
                try {
                    doc = reader.build(new File("data.xml"));
                    Element root = doc.getRootElement();
                    List<Element> textElements = root.getChildren("text");
                    String textData;
                    for (Element textElement : textElements) {
                        if (textElement.getAttributeValue("text").contains("Data")) {
                            textData = textElement.getAttributeValue("text");
                            if (textData.equalsIgnoreCase("TitleData")) textElement.setAttribute("text", movieInfo.getTitle()); else if (textData.equalsIgnoreCase("DirectorData")) textElement.setAttribute("text", movieInfo.getDirector()); else if (textData.equalsIgnoreCase("CastData")) textElement.setAttribute("text", movieInfo.getCast()); else if (textData.equalsIgnoreCase("GenreData")) textElement.setAttribute("text", movieInfo.getGenre()); else if (textData.equalsIgnoreCase("YearData")) textElement.setAttribute("text", movieInfo.getYear()); else if (textData.equalsIgnoreCase("RuntimeData")) textElement.setAttribute("text", movieInfo.getRuntime()); else if (textData.equalsIgnoreCase("RatingData")) textElement.setAttribute("text", movieInfo.getRating()); else if (textData.equalsIgnoreCase("SynopsisData")) textElement.setAttribute("text", movieInfo.getSynopsis()); else if (textData.equalsIgnoreCase("AudioData")) textElement.setAttribute("text", movieInfo.getAudio()); else if (textData.equalsIgnoreCase("SubtitleData")) textElement.setAttribute("text", movieInfo.getSubs()); else if (textData.equalsIgnoreCase("DetailsData")) textElement.setAttribute("text", movieInfo.getDetails()); else if (textData.equalsIgnoreCase("ResolutionData")) textElement.setAttribute("text", movieInfo.getResolution()); else if (textData.equalsIgnoreCase("FormatData")) textElement.setAttribute("text", movieInfo.getFormat()); else if (textData.equalsIgnoreCase("CodecData")) textElement.setAttribute("text", movieInfo.getCodec());
                        }
                    }
                    boolean done = (new File(baseDir + "/" + movieInfo.getDir())).mkdirs();
                    copyImage(xmlDir.getAbsolutePath() + "/" + movieInfo.getBaseFilename() + ".jpg", baseDir + "/" + movieInfo.getDir() + "/" + "folder.jpg");
                    saveXml(baseDir + "/" + movieInfo.getDir() + "/data.xml", doc);
                } catch (Exception e) {
                    System.out.println("Problem with the export of the data.xml-File");
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
	 * save JDOM-Document to xml-file
	 * @param xmlFile
	 * @param document
	 * @throws IOException
	 */
    private static void saveXml(String xmlFile, Document document) throws IOException {
        XMLOutputter outputter = new XMLOutputter();
        FileWriter writer = new FileWriter(xmlFile);
        outputter.output(document, writer);
        writer.close();
    }

    /**
	 * copy srcImg to destImg
	 * @param srcImg
	 * @param destImg
	 */
    private static void copyImage(String srcImg, String destImg) {
        try {
            FileChannel srcChannel = new FileInputStream(srcImg).getChannel();
            FileChannel dstChannel = new FileOutputStream(destImg).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
