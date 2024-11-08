package com.nach0x.theTVDBrenamer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.regex.Matcher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author nacho
 */
public class FileTools {

    private ArrayList<SeriesPattern> seriesPatterns;

    public FileTools() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new File("patterns.xml"));
        } catch (Exception e) {
            throw new Exception("Missing file: 'patterns.xml'");
        }
        seriesPatterns = new ArrayList<SeriesPattern>();
        NodeList patternList = document.getElementsByTagName("Pattern");
        for (int i = 0; i < patternList.getLength(); i++) {
            Node pattern = patternList.item(i);
            NodeList groupList = pattern.getChildNodes();
            String regex = "";
            int seriesNameGroup = 0;
            int seasonNbrGroup = 0;
            int episodeNbrGroup = 0;
            for (int j = 0; j < groupList.getLength(); j++) {
                Node node = groupList.item(j);
                if (node.getNodeName().equalsIgnoreCase("regex")) regex = node.getTextContent();
                if (node.getNodeName().equalsIgnoreCase("seriesNameGroup")) seriesNameGroup = new Integer(node.getTextContent());
                if (node.getNodeName().equalsIgnoreCase("seasonNbrGroup")) seasonNbrGroup = new Integer(node.getTextContent());
                if (node.getNodeName().equalsIgnoreCase("episodeNbrGroup")) episodeNbrGroup = new Integer(node.getTextContent());
            }
            seriesPatterns.add(new SeriesPattern(regex, seriesNameGroup, seasonNbrGroup, episodeNbrGroup));
        }
    }

    public EpisodeAtrb parseFile(File file) throws Exception {
        if (!file.exists() || (file.exists() && !file.isFile())) throw new Exception("'" + file.getPath() + "': file does not exist");
        EpisodeAtrb ea;
        for (SeriesPattern pattern : seriesPatterns) {
            Matcher matcher = pattern.getPattern().matcher(file.getName());
            if (!matcher.matches()) continue;
            ea = new EpisodeAtrb(file);
            String group = matcher.group(pattern.getSeriesNameGroup());
            group = group.replaceAll("\\.", " ");
            ea.setSeriesName(group.trim());
            group = matcher.group(pattern.getSeasonNbrGroup());
            ea.setSeasonNbr(group.trim());
            group = matcher.group(pattern.getEpisodeNbrGroup());
            ea.setEpisodeNbr(group.trim());
            return ea;
        }
        throw new Exception(file.getName() + ": Unknow episode");
    }

    public void fileCopy(File inFile, File outFile) {
        try {
            FileInputStream in = new FileInputStream(inFile);
            FileOutputStream out = new FileOutputStream(outFile);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (IOException e) {
            System.err.println("Hubo un error de entrada/salida!!!");
        }
    }

    public void fileCopy2(File inFile, File outFile) throws Exception {
        try {
            FileChannel srcChannel = new FileInputStream(inFile).getChannel();
            FileChannel dstChannel = new FileOutputStream(outFile).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
            throw new Exception("Could not copy file: " + inFile.getName());
        }
    }
}
