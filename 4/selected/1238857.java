package net.sourceforge.fluxion.beans;

import junit.framework.TestCase;
import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.util.JSONUtils;
import net.sourceforge.fluxion.graph.ColorSchemes;
import net.sourceforge.fluxion.graph.Graph;
import net.sourceforge.fluxion.graph.view.GraphView;
import net.sourceforge.fluxion.graph.view.renderer.GraphViewRenderer;
import net.sourceforge.fluxion.runcible.graph.mapping.MappingManager;
import net.sourceforge.fluxion.runcible.graph.view.factory.FlexGraphViewFactory;
import net.sourceforge.fluxion.runcible.graph.view.renderer.FlexGraphViewRenderer;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 17-Feb-2009
 */
public class TestMappingManagerUtilsBean extends TestCase {

    private MappingManagerUtilsBean mmBean;

    private JSONObject json;

    private URI srcURI;

    private URI targetURI;

    private URI rulesDoc;

    public void setUp() {
        mmBean = new MappingManagerUtilsBean();
        json = new JSONObject();
        URL srcURL = this.getClass().getResource("/homo_sapiens_core_47_36i.owl");
        URL targetURL = this.getClass().getResource("/cgaoV0-3-6.owl");
        URL rulesDocURL = this.getClass().getResource("/ensembl-rules-species.xml");
        try {
            srcURI = srcURL.toURI();
            targetURI = targetURL.toURI();
            rulesDoc = rulesDocURL.toURI();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void tearDown() {
        mmBean = null;
        json = null;
    }

    public void testSetSourceOntology() {
        MappingManager manager = new MappingManager();
        OWLOntologyManager omanager = OWLManager.createOWLOntologyManager();
        OWLOntology srcOntology;
        try {
            srcOntology = omanager.loadOntologyFromPhysicalURI(srcURI);
            if (srcOntology != null) {
                URI uriIn = srcOntology.getURI();
                manager.setSourceOntology(srcOntology);
                URI uriOut = manager.getSourceOntology().getURI();
                if (uriIn != uriOut) {
                    fail("Different URIs out vs. in: Out = " + uriOut + "; In = " + uriIn);
                }
                System.out.println("Class count: " + srcOntology.getReferencedClasses().size());
                manager.setTargetOntology(srcOntology);
                manager.createMapping();
                manager.getSourceGraph();
            } else {
                fail("Null ontology!");
            }
        } catch (OWLOntologyCreationException ooce) {
            fail(ooce.getMessage());
        }
        if (JSONUtils.SimpleJSONResponse("Source URL OK") == null) {
            fail("Bad response!");
        }
    }

    public void testSetTargetOntology() {
        MappingManager manager = new MappingManager();
        OWLOntologyManager omanager = OWLManager.createOWLOntologyManager();
        OWLOntology targetOntology;
        try {
            targetOntology = omanager.loadOntologyFromPhysicalURI(targetURI);
            if (targetOntology != null) {
                URI uriIn = targetOntology.getURI();
                manager.setTargetOntology(targetOntology);
                URI uriOut = manager.getTargetOntology().getURI();
                if (uriIn != uriOut) {
                    fail("Different URIs out vs. in: Out = " + uriOut + "; In = " + uriIn);
                }
                System.out.println("Class count: " + targetOntology.getReferencedClasses().size());
                manager.setSourceOntology(targetOntology);
                manager.createMapping();
                manager.getTargetGraph();
            } else {
                fail("Null ontology!");
            }
        } catch (OWLOntologyCreationException ooce) {
            fail(ooce.getMessage());
        }
        if (JSONUtils.SimpleJSONResponse("Target URL OK") == null) {
            fail("Bad response!");
        }
    }

    public void testMapping() {
        MappingManager manager = new MappingManager();
        OWLOntologyManager omanager = OWLManager.createOWLOntologyManager();
        OWLOntology srcOntology;
        OWLOntology targetOntology;
        try {
            srcOntology = omanager.loadOntologyFromPhysicalURI(srcURI);
            targetOntology = omanager.loadOntologyFromPhysicalURI(targetURI);
            manager.setSourceColours(ColorSchemes.BLUES);
            manager.setTargetColours(ColorSchemes.GREENS);
            manager.createMapping(srcOntology, targetOntology);
            manager.getSourceGraph();
            manager.getTargetGraph();
        } catch (OWLOntologyCreationException e) {
            fail(e.getMessage());
        }
    }

    public void testLoadRules() {
        try {
            MappingManager manager = new MappingManager();
            OWLOntologyManager omanager = OWLManager.createOWLOntologyManager();
            OWLOntology srcOntology;
            OWLOntology targetOntology;
            manager.loadMapping(rulesDoc.toURL());
            srcOntology = omanager.loadOntologyFromPhysicalURI(srcURI);
            targetOntology = omanager.loadOntologyFromPhysicalURI(targetURI);
            manager.setSourceOntology(srcOntology);
            manager.setTargetOntology(targetOntology);
            manager.getSourceGraph();
            manager.getTargetGraph();
        } catch (IOException e) {
            fail("Can't load from this URL: " + rulesDoc.toString());
        } catch (OWLOntologyCreationException e) {
            fail(e.getMessage());
        }
    }

    public void testRenderRules() {
        try {
            MappingManager manager = new MappingManager();
            OWLOntologyManager omanager = OWLManager.createOWLOntologyManager();
            OWLOntology srcOntology;
            OWLOntology targetOntology;
            manager.loadMapping(rulesDoc.toURL());
            srcOntology = omanager.loadOntologyFromPhysicalURI(srcURI);
            targetOntology = omanager.loadOntologyFromPhysicalURI(targetURI);
            manager.setSourceOntology(srcOntology);
            manager.setTargetOntology(targetOntology);
            Graph srcGraph = manager.getSourceGraph();
            Graph targetGraph = manager.getTargetGraph();
            System.out.println("Starting to render...");
            FlexGraphViewFactory factory = new FlexGraphViewFactory();
            factory.setColorScheme(ColorSchemes.BLUES);
            factory.visit(srcGraph);
            GraphView view = factory.getGraphView();
            GraphViewRenderer renderer = new FlexGraphViewRenderer();
            renderer.setGraphView(view);
            System.out.println("View updated with graph...");
            InputStream xmlStream = renderer.renderGraphView();
            StringWriter writer = new StringWriter();
            IOUtils.copy(xmlStream, writer);
            System.out.println("Finished writing");
            writer.close();
            System.out.println("Finished render... XML is:");
            System.out.println(writer.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }
}
