package perestrojka.ui;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import perestrojka.common.Edge;
import perestrojka.common.IConversionGraph;
import perestrojka.common.Vertex;
import perestrojka.extensionSupport.IConversionExtension;
import perestrojka.extensionSupport.Logger;

/**
 * Creates a graph based on the registered conversion extensions. This is needed to
 * find a way for converting two file formats even though no extension is registered
 * that can convert the formats directly.
 * The vertices of the graph are the supported file formats in all registered conversion
 * extensions. Edges link two file formats if there is a conversion extension registered
 * that supportes conversion between the two formats. Every edge has a weight that is
 * 1 if the connected vertices have the lossless property set, else 2 
 * 
 * @author christian.mader
 *
 */
public class ConversionGraphCreator implements IConversionGraph {

    private List<Vertex> V = new ArrayList<Vertex>();

    private List<Edge> E = new ArrayList<Edge>();

    private List<ExtensionDescription> allExtensions;

    public ConversionGraphCreator() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extPoint = registry.getExtensionPoint(Activator.getId(), "encoder");
        IExtension[] extensions = extPoint.getExtensions();
        allExtensions = new ArrayList<ExtensionDescription>();
        for (int i = 0; i < extensions.length; i++) {
            List<Vertex> readableFormats = new ArrayList<Vertex>();
            List<Vertex> writeableFormats = new ArrayList<Vertex>();
            IConversionExtension conversionExtension = null;
            IExtension extension = extensions[i];
            IConfigurationElement[] configurations = extension.getConfigurationElements();
            for (int j = 0; j < configurations.length; j++) {
                IConfigurationElement element = configurations[j];
                String fileExtension = element.getAttribute("fileExtension");
                String isLossless = element.getAttribute("isLossless");
                if (isLossless == null) {
                    isLossless = Boolean.FALSE.toString();
                }
                if (fileExtension != null) {
                    Vertex fileFormat = new Vertex(fileExtension, isLossless.equals(Boolean.TRUE.toString()));
                    if (element.getName().equals("writeableFormat")) {
                        writeableFormats.add(fileFormat);
                    } else if (element.getName().equals("readableFormat")) {
                        readableFormats.add(fileFormat);
                    }
                }
                if (element.getName().equals("implementingClass")) {
                    try {
                        conversionExtension = (IConversionExtension) element.createExecutableExtension("className");
                    } catch (CoreException e) {
                        Logger.logError("Error instantiating extension " + extension.getUniqueIdentifier(), this.getClass());
                        break;
                    }
                }
            }
            allExtensions.add(new ExtensionDescription(conversionExtension, readableFormats, writeableFormats));
        }
        int pos = -1;
        for (ExtensionDescription extDesc : allExtensions) {
            for (Vertex sourceVertex : extDesc.getReadableFormats()) {
                pos = V.indexOf(sourceVertex);
                if (pos == -1) {
                    V.add(sourceVertex);
                    Logger.logInfo("adding source vertex: " + sourceVertex, this.getClass());
                } else {
                    sourceVertex = V.get(pos);
                }
                for (Vertex targetVertex : extDesc.getWriteableFormats()) {
                    pos = V.indexOf(targetVertex);
                    if (pos == -1) {
                        V.add(targetVertex);
                        Logger.logInfo("adding target vertex: " + targetVertex, this.getClass());
                    } else {
                        targetVertex = V.get(pos);
                    }
                    if (sourceVertex.equals(targetVertex)) {
                        continue;
                    }
                    Edge edge = new Edge(sourceVertex, targetVertex, extDesc.getConversionExtension());
                    if (!E.contains(edge)) {
                        E.add(edge);
                        Logger.logInfo("adding edge: " + edge, this.getClass());
                    }
                }
            }
        }
    }

    public List<String> getAllWriteableFormats() {
        List<String> ret = new ArrayList<String>();
        for (ExtensionDescription ed : allExtensions) {
            for (Vertex vertex : ed.writeableFormats) {
                if (ret.contains(vertex.getFormat())) continue;
                ret.add(vertex.getFormat().toUpperCase());
            }
        }
        return ret;
    }

    public List<Edge> getAllEdges() {
        return E;
    }

    public List<Vertex> getAllVertices() {
        return V;
    }

    private class ExtensionDescription {

        private List<Vertex> readableFormats;

        private List<Vertex> writeableFormats;

        private IConversionExtension conversionExtension;

        ExtensionDescription(IConversionExtension conversionExtension, List<Vertex> readableFormats, List<Vertex> writeableFormats) {
            this.conversionExtension = conversionExtension;
            this.writeableFormats = writeableFormats;
            this.readableFormats = readableFormats;
        }

        public List<Vertex> getReadableFormats() {
            return readableFormats;
        }

        public List<Vertex> getWriteableFormats() {
            return writeableFormats;
        }

        public IConversionExtension getConversionExtension() {
            return conversionExtension;
        }
    }
}
