package massim.visualization;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import massim.visualization.svg.SvgXmlFile;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class PreviewSvg extends SvgXmlFile {

    private String imageHeight;

    private String imageWidth;

    private String namePreviewSvg;

    private String nameOutputFile;

    private Document configDocument;

    private void setDefaultValues() {
        MainPolicy mainPolicy = new MainPolicy();
        this.namePreviewSvg = mainPolicy.getNamePreviewSvg();
        this.nameOutputFile = mainPolicy.getNameOutputFile();
    }

    public void setImageHeight(String imageHeight) {
        this.imageHeight = imageHeight;
    }

    public void setImageWidth(String imageWidth) {
        this.imageWidth = imageWidth;
    }

    public String readSvgConf() {
        MainPolicy mainPolicy = new MainPolicy();
        String configPath = mainPolicy.getConfigFile();
        setDefaultValues();
        String image = "no";
        SvgXmlFile config = new SvgXmlFile();
        configDocument = config.openFile(configDocument, configPath);
        Node root = configDocument.getDocumentElement();
        int length = root.getChildNodes().getLength();
        for (int i = 0; i < length; i++) {
            Node configElement = root.getChildNodes().item(i);
            if ((configElement.getNodeName() == "simulationOutput")) {
                Element simulationConfig = (Element) configDocument.getElementsByTagName("simulationOutput").item(0);
                int numberAttributes = configElement.getAttributes().getLength();
                for (int j = 0; j < numberAttributes; j++) {
                    if (configElement.getAttributes().item(j).getNodeName() == "nameOutputFile") {
                        nameOutputFile = simulationConfig.getAttribute("nameOutputFile");
                    } else if (configElement.getAttributes().item(j).getNodeName() == "namePreviewSvg") {
                        namePreviewSvg = simulationConfig.getAttribute("namePreviewSvg");
                    } else {
                    }
                }
            }
            if ((configElement.getNodeName() == "imageSize")) {
                image = "yes";
                Element simulationConfig = (Element) configDocument.getElementsByTagName("imageSize").item(0);
                int numberAttributes = configElement.getAttributes().getLength();
                for (int j = 0; j < numberAttributes; j++) {
                    if (configElement.getAttributes().item(j).getNodeName() == "height") imageHeight = (simulationConfig.getAttribute("height")); else if (configElement.getAttributes().item(j).getNodeName() == "width") imageWidth = (simulationConfig.getAttribute("width"));
                }
            }
        }
        namePreviewSvg = namePreviewSvg + svgEnding;
        return image;
    }

    /**
	 * generate the PreviewSvg its the main SVG in the visualisation, with the
	 * control panal, the main title and import all generated svg's
	 * 
	 * @param numberSvg
	 *            is the number of generated svg files
	 * @param headInformationFirstLevel
	 *            text in head of the svg file
	 * @param headInformationSecondLevel
	 *            text in subhead of the svg file
	 */
    public void createPreviewSvg(String path, String configPath, long numberSvg, String headInformationFirstLevel, String headInformationSecondLevel) {
        readSvgConf();
        DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Document doc = impl.createDocument(svgNS, "svg", null);
        Element svgRoot = doc.getDocumentElement();
        Element title = doc.createElement("title");
        Text theTitle = doc.createTextNode("MasSim Simulation");
        title.appendChild(theTitle);
        svgRoot.appendChild(title);
        Element defs = doc.createElement("defs");
        Element script = doc.createElement("script");
        script.setAttribute("type", "text/ecmascript");
        String arraySVGs = "";
        for (int i = 1; i <= numberSvg; i++) {
            if (i != 1) {
                arraySVGs = arraySVGs + ", ";
            }
            arraySVGs = arraySVGs + "\"" + nameOutputFile + "-" + i + ".svg" + "\"";
        }
        String line;
        String myScript = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(configPath + "svg-preview-javascript")));
            StringBuffer contentOfFile = new StringBuffer();
            while ((line = br.readLine()) != null) {
                contentOfFile.append(line + "\n");
            }
            String content = contentOfFile.toString();
            myScript = "var dia=new Array (" + arraySVGs + ");\n" + "var numberSvgs=" + numberSvg + ";\n" + content;
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        CDATASection cdata1 = doc.createCDATASection(myScript);
        script.appendChild(cdata1);
        defs.appendChild(script);
        svgRoot.appendChild(defs);
        String content1 = null;
        Element style = doc.createElement("style");
        style.setAttribute("type", "text/css");
        try {
            BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream(configPath + "svg-preview-css")));
            StringBuffer contentOfFile1 = new StringBuffer();
            String line1;
            while ((line1 = br1.readLine()) != null) {
                contentOfFile1.append(line1 + "\n");
            }
            content1 = contentOfFile1.toString();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        CDATASection cdata2 = doc.createCDATASection(content1);
        style.appendChild(cdata2);
        defs.appendChild(style);
        svgRoot.appendChild(defs);
        Element mainGroup = doc.createElement("g");
        mainGroup.setAttribute("id", "zooming");
        mainGroup.setAttribute("transform", "scale(1.4)");
        Text headTextFirst = doc.createTextNode(headInformationFirstLevel);
        Element headTextElement1 = doc.createElement("text");
        headTextElement1.setAttribute("x", "256");
        headTextElement1.setAttribute("y", "17");
        headTextElement1.setAttribute("style", "font-family:Arial Unicode;font-size:20;");
        headTextElement1.appendChild(headTextFirst);
        mainGroup.appendChild(headTextElement1);
        Text headTextSecond = doc.createTextNode(headInformationSecondLevel);
        Element headTextElement2 = doc.createElement("text");
        headTextElement2.setAttribute("x", "256");
        headTextElement2.setAttribute("y", "37");
        headTextElement2.setAttribute("style", "font-family:Arial Unicode;font-size:20;");
        headTextElement2.appendChild(headTextSecond);
        mainGroup.appendChild(headTextElement2);
        Element first = doc.createElement("use");
        Element back = doc.createElement("use");
        Element pause = doc.createElement("use");
        Element stop = doc.createElement("use");
        Element play = doc.createElement("use");
        Element next = doc.createElement("use");
        Element last = doc.createElement("use");
        Element plus = doc.createElement("use");
        Element minus = doc.createElement("use");
        first.setAttribute("class", "press");
        first.setAttribute("xlink:href", "first.svg#first");
        first.setAttribute("transform", "translate(10,10) scale(0.2)");
        first.setAttribute("onclick", "firstSvg()");
        back.setAttribute("class", "press");
        back.setAttribute("xlink:href", "back.svg#back");
        back.setAttribute("transform", "translate(10,10) scale(0.2)");
        back.setAttribute("onclick", "backSvg()");
        pause.setAttribute("class", "press");
        pause.setAttribute("xlink:href", "pause.svg#pause");
        pause.setAttribute("transform", "translate(10,10) scale(0.2)");
        pause.setAttribute("onclick", "pauseSvg()");
        stop.setAttribute("class", "press");
        stop.setAttribute("xlink:href", "stop.svg#stop");
        stop.setAttribute("transform", "translate(10,10) scale(0.2)");
        stop.setAttribute("onclick", "firstSvg()");
        play.setAttribute("class", "press");
        play.setAttribute("xlink:href", "play.svg#play");
        play.setAttribute("transform", "translate(10,10) scale(0.2)");
        play.setAttribute("onclick", "playSvg()");
        next.setAttribute("class", "press");
        next.setAttribute("xlink:href", "next.svg#next");
        next.setAttribute("transform", "translate(10,10) scale(0.2)");
        next.setAttribute("onclick", "nextSvg()");
        last.setAttribute("class", "press");
        last.setAttribute("xlink:href", "last.svg#last");
        last.setAttribute("transform", "translate(10,10) scale(0.2)");
        last.setAttribute("onclick", "lastSvg()");
        plus.setAttribute("class", "press");
        plus.setAttribute("xlink:href", "plus.svg#plus");
        plus.setAttribute("transform", "translate(10,10) scale(0.2)");
        plus.setAttribute("onclick", "speedPlus()");
        minus.setAttribute("class", "press");
        minus.setAttribute("xlink:href", "minus.svg#minus");
        minus.setAttribute("transform", "translate(10,10) scale(0.2)");
        minus.setAttribute("onclick", "speedMinus()");
        Element background = doc.createElement("use");
        Element image1 = doc.createElement("use");
        Element image = doc.createElement("use");
        try {
            background.setAttribute("id", "background");
            background.setAttribute("xlink:href", nameOutputFile + "-0.svg" + "#scaleSvg");
            background.setAttribute("x", "0");
            background.setAttribute("y", "40");
            background.setAttribute("z-index", "0");
            image1.setAttribute("id", "image1");
            image1.setAttribute("xlink:href", nameOutputFile + "-2.svg" + "#scaleSvg");
            image1.setAttribute("x", "0");
            image1.setAttribute("y", "40");
            image1.setAttribute("z-index", "0");
            image1.setAttribute("visibility", "hidden");
            image.setAttribute("id", "image");
            image.setAttribute("xlink:href", nameOutputFile + "-1.svg" + "#scaleSvg");
            image.setAttribute("x", "0");
            image.setAttribute("y", "40");
            image.setAttribute("z-index", "0");
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        mainGroup.appendChild(first);
        mainGroup.appendChild(back);
        mainGroup.appendChild(pause);
        mainGroup.appendChild(stop);
        mainGroup.appendChild(play);
        mainGroup.appendChild(next);
        mainGroup.appendChild(last);
        mainGroup.appendChild(plus);
        mainGroup.appendChild(minus);
        mainGroup.appendChild(background);
        mainGroup.appendChild(image);
        mainGroup.appendChild(image1);
        svgRoot.appendChild(mainGroup);
        saveXML(doc, path + namePreviewSvg);
        copy(configPath, path, "first.svg");
        copy(configPath, path, "back.svg");
        copy(configPath, path, "pause.svg");
        copy(configPath, path, "stop.svg");
        copy(configPath, path, "play.svg");
        copy(configPath, path, "next.svg");
        copy(configPath, path, "last.svg");
        copy(configPath, path, "plus.svg");
        copy(configPath, path, "minus.svg");
    }

    private void copy(String inputPath, String outputPath, String name) {
        try {
            FileReader in = new FileReader(inputPath + name);
            FileWriter out = new FileWriter(outputPath + name);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
