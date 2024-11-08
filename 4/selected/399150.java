package org.cumt.tools.generators;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.cumt.model.BaseModelNode;
import org.cumt.model.DiagramModel;
import org.cumt.model.PackageObject;
import org.cumt.model.analysis.usecases.Condition;
import org.cumt.model.analysis.usecases.Flow;
import org.cumt.model.analysis.usecases.FlowActivity;
import org.cumt.model.analysis.usecases.UseCase;
import org.cumt.tools.PropertyOptions;
import org.cumt.tools.ToolExecutionException;
import org.cumt.view.DiagramView;
import org.cumt.view.DiagramViewFactory;

/**
 * FIXME Sucks !!!!
 * Default Documentation generator. Generates a set of HTML files.
 * @author <a href="mailto:cdescalzi2001@yahoo.com.ar">Carlos E.Descalzi</a>
 */
public class DefaultHTMLDocGenerator extends DocGenerator {

    @PropertyOptions(optional = true)
    private File stylesheetFile;

    public void execute() throws ToolExecutionException {
        PackageObject packageObject = this.getBasePackage();
        File targetDirectory = getTargetDirectory();
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        try {
            copyStylesheet(targetDirectory);
            IndexNode index = generateContents(targetDirectory, packageObject);
            generateIndex(targetDirectory, index);
        } catch (IOException e) {
            throw new ToolExecutionException(e);
        }
    }

    private void copyStylesheet(File targetDirectory) throws IOException {
        if (stylesheetFile != null && stylesheetFile.exists()) {
            File newFile = new File(targetDirectory, stylesheetFile.getName());
            newFile.createNewFile();
            FileUtils.copyFile(stylesheetFile, newFile);
        }
    }

    private void generateIndex(File targetDirectory, IndexNode node) throws IOException {
        File indexFile = new File(targetDirectory, "index.html");
        PrintStream stream = new PrintStream(indexFile);
        indexFile.createNewFile();
        stream.append("<html>\n<head>");
        if (stylesheetFile != null) {
            stream.append("<link rel=\"stylesheet\" href=\"").append(stylesheetFile.getName()).append("\"/>");
        }
        stream.append("</head><body><h1>").append(node.getName()).append("</h1>\n").append("<h2>Index</h2>\n");
        stream.append("<table>\n");
        for (IndexNode child : node.getChildren()) {
            writeIndexNode(stream, child);
        }
        stream.append("</table>\n");
        stream.append("</body>\n</html>");
        stream.flush();
        stream.close();
    }

    private void writeIndexNode(PrintStream stream, IndexNode node) {
        stream.append("<tr><td>").append(node.getIndexNumber()).append("&nbsp;&nbsp;<a href=\"").append(node.getFileName());
        (node.getAnchor() == null ? stream : stream.append("#").append(node.getAnchor())).append("\">").append(node.getName()).append("</a></td></tr>\n");
        for (IndexNode child : node.getChildren()) {
            writeIndexNode(stream, child);
        }
    }

    private String normalizeName(String aName) {
        return aName.replaceAll(" ", "_").toLowerCase();
    }

    private IndexNode generateContents(File targetDirectory, PackageObject packageObject) throws IOException {
        IndexNode indexNode = new IndexNode();
        indexNode.setName(packageObject.getName());
        int index = 1;
        for (BaseModelNode node : packageObject.getContents()) {
            final String fileName = (index++) + "_" + normalizeName(node.getName()) + ".html";
            File file = new File(targetDirectory, fileName);
            file.createNewFile();
            PrintStream stream = new PrintStream(file);
            writeFile(fileName, targetDirectory, (BaseModelNode) node, stream, indexNode);
            stream.close();
        }
        return indexNode;
    }

    private void writeFile(String fileName, File targetDirectory, BaseModelNode node, PrintStream stream, IndexNode parentNode) throws IOException {
        IndexNode indexNode = new IndexNode();
        indexNode.setFileName(fileName);
        indexNode.setAnchor(node.getName());
        indexNode.setName(node.getName());
        parentNode.addChild(indexNode);
        stream.append("<html>\n<head>\n");
        if (stylesheetFile != null) {
            stream.append("<link rel=\"stylesheet\" href=\"").append(stylesheetFile.getName()).append("\"/>\n");
        }
        stream.append("</head>/n<body>\n").append("<h1>").append(indexNode.getIndexNumber()).append("&nbsp;&nbsp;").append(node.getName()).append("</h1>\n");
        if (node instanceof DiagramModel) {
            writeImage(targetDirectory, node, stream);
        }
        stream.append("<p>").append(node.getDescription()).append("</p>");
        if (node instanceof PackageObject) {
            for (BaseModelNode child : ((PackageObject) node).getContents()) {
                writeContent(fileName, targetDirectory, child, stream, indexNode);
            }
        }
        stream.append("</body>\n").append("</html>");
    }

    private void writeContent(String fileName, File targetDirectory, BaseModelNode node, PrintStream stream, IndexNode parentNode) throws IOException {
        IndexNode indexNode = new IndexNode();
        indexNode.setAnchor(node.getName());
        indexNode.setName(node.getName());
        indexNode.setFileName(fileName);
        indexNode.setAnchor(String.valueOf(System.currentTimeMillis()));
        parentNode.addChild(indexNode);
        stream.append("<h2><a name=\"").append(indexNode.getAnchor()).append("\">").append(indexNode.getIndexNumber()).append("&nbsp;&nbsp;").append(node.getName()).append("</a></h2>\n");
        if (node instanceof DiagramModel && !((DiagramModel) node).getContents().isEmpty()) {
            writeImage(targetDirectory, node, stream);
        }
        stream.append("<p>").append(node.getDescription()).append("</p>\n");
        if (node instanceof UseCase) {
            writeUseCase((UseCase) node, stream);
        }
        writeAttributes(node.getAttributes(), stream);
        if (node instanceof PackageObject) {
            for (BaseModelNode child : ((PackageObject) node).getContents()) {
                writeContent(fileName, targetDirectory, child, stream, indexNode);
            }
        }
    }

    private void writeUseCase(UseCase useCase, PrintStream stream) {
        if (!useCase.getPreConditions().isEmpty()) {
            stream.append("<h3>Pre Conditions</h3>\n");
            writeConditions(useCase.getPreConditions(), stream);
        }
        stream.append("<h3>Main Flow</h3>\n");
        Flow flow = useCase.getMainFlow();
        writeFlow(flow, stream);
        if (!useCase.getAlternativeFlows().isEmpty()) {
            stream.append("<h3>Alternative Flows</h3>\n");
            for (Flow alternativeFlow : useCase.getAlternativeFlows()) {
                stream.append("<h4>").append(alternativeFlow.getName()).append("</h4>\n");
                writeFlow(alternativeFlow, stream);
            }
        }
        if (!useCase.getPostConditions().isEmpty()) {
            stream.append("<h3>Post Conditions</h3>\n");
            writeConditions(useCase.getPostConditions(), stream);
        }
    }

    private void writeConditions(List<Condition> conditions, PrintStream stream) {
        stream.append("<ol>");
        for (Condition condition : conditions) {
            stream.append("<li>").append(condition.getName()).append("</li>");
        }
        stream.append("</ol>");
    }

    private void writeFlow(Flow flow, PrintStream stream) {
        stream.append("<ol>");
        for (FlowActivity activity : flow.getActivities()) {
            stream.append("<li>").append(activity.getDescription()).append("</li>");
        }
        stream.append("</ol>");
    }

    private void writeAttributes(Map<String, String> attributes, PrintStream stream) {
        if (attributes.isEmpty()) {
            return;
        }
        stream.append("<table>\n<tr><th>Attribute</th><th>Value</th></tr>\n");
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            stream.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
        }
        stream.append("</table>\n");
    }

    private void writeImage(File targetDirectory, BaseModelNode node, PrintStream stream) throws IOException {
        DiagramView view = DiagramViewFactory.createViewForDiagram((DiagramModel) node);
        view.setGridVisible(false);
        view.setBackground(Color.WHITE);
        view.modelToView();
        Rectangle area = view.getEffectiveArea();
        area.x -= 10;
        area.y -= 10;
        area.width += 20;
        area.height += 20;
        Dimension size = new Dimension(area.width + area.x, area.height + area.y);
        view.setSize(size);
        BufferedImage image = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics().create(-area.x, -area.y, size.width, size.height);
        view.print(g);
        g.dispose();
        String imageFileName = System.currentTimeMillis() + "_" + normalizeName(node.getName()) + ".png";
        File file = new File(targetDirectory, imageFileName);
        file.createNewFile();
        ImageIO.write(image, "png", file);
        stream.append("<img src=\"" + imageFileName + "\"/>");
    }

    private class IndexNode {

        private String name;

        private String fileName;

        private String anchor;

        private IndexNode parent;

        public final List<IndexNode> children = new ArrayList<IndexNode>();

        public String getAnchor() {
            return anchor;
        }

        public void setAnchor(String anchor) {
            this.anchor = anchor;
        }

        public List<IndexNode> getChildren() {
            return children;
        }

        public void addChild(IndexNode child) {
            child.setParent(this);
            this.children.add(child);
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public IndexNode getParent() {
            return parent;
        }

        public void setParent(IndexNode parent) {
            this.parent = parent;
        }

        public String getIndexNumber() {
            List<Integer> number = new ArrayList<Integer>();
            for (IndexNode node = this; node.getParent() != null; node = node.getParent()) {
                number.add(0, node.getParent().getChildren().indexOf(node) + 1);
            }
            StringBuilder buffer = new StringBuilder();
            for (Iterator<Integer> i = number.iterator(); i.hasNext(); ) {
                buffer.append(i.next());
                if (i.hasNext()) {
                    buffer.append(".");
                }
            }
            return buffer.toString();
        }
    }

    public File getStylesheetFile() {
        return stylesheetFile;
    }

    public void setStylesheetFile(File stylesheetFile) {
        this.stylesheetFile = stylesheetFile;
    }
}
