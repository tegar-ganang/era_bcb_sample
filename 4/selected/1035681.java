package org.polepos.reporters;

import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import org.apache.velocity.*;
import org.apache.velocity.app.*;
import org.jfree.chart.*;
import org.polepos.framework.*;

public class HTMLReporter extends GraphReporter {

    private static final String TEMPLATEDIRNAME = "templates";

    public static final String ENCODING = "utf-8";

    public static final String OUTDIRNAME = "doc/results/html";

    private File outdir = null;

    private List<Circuit> circuits = new ArrayList<Circuit>();

    private List<Lap> laps = new ArrayList<Lap>();

    private VelocityEngine engine = null;

    private Graph graph = null;

    protected void report(Graph graph) {
        try {
            Circuit oldcircuit = circuit();
            if (oldcircuit != graph.circuit()) {
                if (oldcircuit == null) {
                    setup();
                }
                renderCircuitPage();
                circuits.add(graph.circuit());
                laps.clear();
            }
            this.graph = graph;
            laps.add(graph.lap());
            renderLapGraph(graph);
            renderLapPage();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private String lapFilePrefix() {
        return circuit().internalName() + "_" + lap().name();
    }

    protected void finish() {
        renderCircuitPage();
        renderIndexPage();
        copyStylesheet();
    }

    private void setup() throws Exception {
        outdir = new File(OUTDIRNAME);
        outdir.mkdirs();
        engine = new VelocityEngine();
        engine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, this);
        engine.setProperty(VelocityEngine.ENCODING_DEFAULT, ENCODING);
        engine.setProperty(VelocityEngine.FILE_RESOURCE_LOADER_PATH, TEMPLATEDIRNAME);
        engine.init();
    }

    private void renderIndexPage() {
        List<TeamCar> distinct = new ArrayList<TeamCar>();
        for (TeamCar teamCar : graph.teamCars()) {
            if (!distinct.contains(teamCar)) {
                distinct.add(teamCar);
            }
        }
        VelocityContext context = new VelocityContext();
        context.put("includefile", "index.vhtml");
        context.put("teamcars", distinct);
        context.put("circuits", circuits);
        renderPage("index.html", context);
    }

    private void renderCircuitPage() {
        if (circuit() == null) {
            return;
        }
        VelocityContext context = new VelocityContext();
        context.put("includefile", "circuitresults.vhtml");
        context.put("circuit", circuit());
        context.put("laps", laps);
        renderPage(circuit().internalName() + ".html", context);
    }

    private void renderLapPage() {
        String lapfileprefix = lapFilePrefix();
        VelocityContext context = new VelocityContext();
        context.put("includefile", "lapresult.vhtml");
        context.put("graph", graph);
        context.put("fileprefix", lapfileprefix);
        renderPage(lapfileprefix + ".html", context);
    }

    private void renderLapGraph(Graph graph) throws IOException {
        JFreeChart chart = new ChartBuilder().createChart(graph);
        BufferedImage img = chart.createBufferedImage(750, 500);
        ImageIO.write(img, "jpg", new File(outdir, lapFilePrefix() + ".jpg"));
    }

    private void renderPage(String targetName, VelocityContext context) {
        BufferedWriter out = null;
        try {
            Template template = engine.getTemplate("baselayout.vhtml");
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outdir, targetName)), ENCODING));
            template.merge(context, out);
            out.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void copyStylesheet() {
        File sourcefile = new File(new File(TEMPLATEDIRNAME), "style.css");
        File targetfile = new File(new File(OUTDIRNAME), "style.css");
        copyFile(sourcefile, targetfile);
    }

    private void copyFile(File sourcefile, File targetfile) {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(sourcefile));
            out = new BufferedOutputStream(new FileOutputStream(targetfile));
            byte[] buffer = new byte[4096];
            int bytesread = 0;
            while ((bytesread = in.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Lap lap() {
        return getLast(laps);
    }

    private Circuit circuit() {
        return getLast(circuits);
    }

    private <Item> Item getLast(List<Item> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }
}
