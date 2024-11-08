package edu.ucsd.ncmir.jinx.segmentation.surfacers;

import JSci.maths.matrices.AbstractDoubleSquareMatrix;
import JSci.maths.matrices.DoubleSquareMatrix;
import JSci.maths.vectors.AbstractDoubleVector;
import JSci.maths.vectors.Double2Vector;
import edu.ucsd.ncmir.jinx.core.JxPoint;
import edu.ucsd.ncmir.jinx.core.JxView;
import edu.ucsd.ncmir.jinx.events.JxErrorEvent;
import edu.ucsd.ncmir.jinx.events.JxStatusEvent;
import edu.ucsd.ncmir.jinx.exception.JxTerminateException;
import edu.ucsd.ncmir.jinx.gui.workspace.JxCapper;
import edu.ucsd.ncmir.jinx.gui.workspace.JxNuagesErrorDialog;
import edu.ucsd.ncmir.jinx.objects.JxObject;
import edu.ucsd.ncmir.jinx.objects.JxObjectTreeNode;
import edu.ucsd.ncmir.jinx.objects.JxPlaneTraceList;
import edu.ucsd.ncmir.jinx.objects.trace.JxClosedTrace;
import edu.ucsd.ncmir.jinx.objects.trace.JxOrientation;
import edu.ucsd.ncmir.jinx.objects.trace.JxTrace;
import edu.ucsd.ncmir.jinx.segmentation.JxSegmentation;
import edu.ucsd.ncmir.spl.smoothers.SurfaceSmoother;
import edu.ucsd.ncmir.nuages.repros.Repros;
import edu.ucsd.ncmir.nuages.tools.prepros.Prepros;
import edu.ucsd.ncmir.nuages.tools.repros2visu.Repros2Visu;
import edu.ucsd.ncmir.spl.graphics.PlanarPolygon;
import edu.ucsd.ncmir.spl.graphics.PlanarPolygonTable;
import edu.ucsd.ncmir.spl.graphics.Threader;
import edu.ucsd.ncmir.spl.graphics.meshables.TriangleMesh;
import edu.ucsd.ncmir.spl.graphics.Triplet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 *
 * @author spl
 */
public class JxNuagesCreateSurfacesThread extends JxAbstractCreateSurfacesThread {

    public JxNuagesCreateSurfacesThread(JxSegmentation segmentation, SurfaceSmoother interpolator, JxCapper capper, JxObjectTreeNode node) {
        super("Nuages", segmentation, interpolator, capper, node);
    }

    protected TriangleMesh processClosedObject(JxView view, JxObject object, JxCapper capper, SurfaceSmoother interpolator) throws Exception {
        String name = object.getName();
        Threader threader = object.getContourThreader(view);
        TriangleMesh mesh = new TriangleMesh();
        while (threader.hasMoreElements()) {
            PlanarPolygonTable plist = threader.nextElement();
            Double[] dlist = plist.getKeys();
            if (dlist.length < 2) continue;
            int levels = dlist.length + capper.getCapTop() + capper.getCapBottom();
            Arrays.sort(dlist);
            double delta = dlist[1].doubleValue() - dlist[0].doubleValue();
            File log_file = File.createTempFile("nuages", ".log");
            log_file.deleteOnExit();
            PrintStream log_stream = new PrintStream(log_file);
            File input_contour_file = File.createTempFile("nuages", ".cont");
            input_contour_file.deleteOnExit();
            PrintStream stream = new PrintStream(input_contour_file);
            stream.printf("S %d\n", levels);
            if (capper.getCapTop() > 0) {
                Double d = dlist[0];
                this.emitPoint(d - delta, plist.get(d), stream);
            }
            for (Double key : dlist) {
                if (this.isInterrupted()) throw new JxTerminateException();
                ArrayList<PlanarPolygon> traces = plist.get(key);
                int count = 0;
                for (PlanarPolygon p : traces) count += p.size();
                double offset = key.doubleValue();
                stream.printf("v %d z %g\n", count, offset);
                for (PlanarPolygon p : traces) {
                    stream.printf("{\n");
                    for (Triplet t : p) stream.printf("%g %g\n", t.getU(), t.getV());
                    stream.printf("}\n");
                }
            }
            if (capper.getCapBottom() > 0) {
                Double d = dlist[dlist.length - 1];
                this.emitPoint(d + delta, plist.get(d), stream);
            }
            stream.close();
            Prepros prepros = new Prepros();
            prepros.setLinear();
            prepros.enableShake();
            prepros.setLogStream(log_stream);
            File prepros_file = File.createTempFile("nuages", ".coord");
            prepros_file.deleteOnExit();
            File repros_out_file = File.createTempFile("nuages", ".tri");
            repros_out_file.deleteOnExit();
            File repros_new_file = File.createTempFile("nuages", ".new");
            repros_new_file.deleteOnExit();
            File out_file = File.createTempFile("nuages", ".out");
            out_file.deleteOnExit();
            TriangleMesh triangulation = null;
            JxSegmentation segmentation = super.getSegmentation();
            try {
                prepros.setInput(input_contour_file);
                prepros.setOutput(prepros_file);
                new JxStatusEvent().send("Preprocessing " + name);
                if (this.isInterrupted()) throw new JxTerminateException();
                if (prepros.process() == 0) {
                    if (this.isInterrupted()) throw new JxTerminateException();
                    Repros repros = new Repros();
                    repros.enableMinimizeVolume();
                    repros.disableInternalVertices();
                    repros.disableExternalTetrahedra();
                    repros.enableStatistics();
                    repros.setInput(prepros_file);
                    repros.setOutput(repros_out_file);
                    repros.setNewFile(repros_new_file);
                    repros.setLogStream(log_stream);
                    if (this.isInterrupted()) throw new JxTerminateException();
                    new JxStatusEvent().send("Tiling " + name);
                    repros.process();
                    if (this.isInterrupted()) throw new JxTerminateException();
                    Repros2Visu repros2visu = new Repros2Visu();
                    repros2visu.setOutputFormat(Repros2Visu.IDX);
                    repros2visu.enableRemoveHorizontal();
                    repros2visu.setObjectName(name);
                    repros2visu.setHeader(name + ": " + new Date().toString());
                    repros2visu.setFacetFile(repros_out_file);
                    repros2visu.setCoordinateFile(repros_new_file);
                    repros2visu.setOutputFile(out_file);
                    repros2visu.setLogStream(log_stream);
                    if (this.isInterrupted()) throw new JxTerminateException();
                    new JxStatusEvent().send("Postprocessing " + name);
                    repros2visu.process();
                    if (this.isInterrupted()) throw new JxTerminateException();
                    log_stream.close();
                    log_file.delete();
                    triangulation = this.getRenderable(out_file, segmentation, view);
                    mesh.addTriangleMesh(triangulation);
                    prepros_file.delete();
                    input_contour_file.delete();
                    repros_out_file.delete();
                    repros_new_file.delete();
                    out_file.delete();
                } else {
                    log_stream.close();
                    this.processError(segmentation, log_file, input_contour_file, object, view);
                }
            } catch (Throwable throwable) {
                String jar_name = this.jarDiagnostics(throwable, new File[] { log_file, input_contour_file, prepros_file, repros_out_file, repros_new_file, out_file });
                new JxErrorEvent().send("Internal error processing " + name + ". Please send file " + jar_name + " to maintainer.");
                throw new JxTerminateException();
            }
        }
        return interpolator.smooth(mesh, new JxStatusCallbackImpl());
    }

    private void emitPoint(double z, ArrayList<PlanarPolygon> traces, PrintStream stream) {
        stream.printf("v %d z %g\n", traces.size() * 3, z);
        for (PlanarPolygon p : traces) {
            double[] centroid = p.getCentroid();
            double[][] bounds = p.getBounds();
            double dx = bounds[1][0] - bounds[0][0];
            double dy = bounds[1][1] - bounds[0][1];
            double radius = Math.sqrt((dx * dx) + (dy * dy)) / 100;
            stream.printf("{\n");
            for (int i = 0; i < 3; i++) {
                double ang = Math.toRadians(i * 2 * Math.PI);
                double x = centroid[0] + (Math.cos(ang) * radius);
                double y = centroid[1] + (-Math.sin(ang) * radius);
                stream.printf("%g %g\n", x, y);
            }
            stream.printf("}\n");
        }
    }

    protected TriangleMesh processOpenObject(JxView view, JxObject object, SurfaceSmoother interpolator) throws Exception {
        String name = object.getName();
        File log_file = File.createTempFile("nuages", ".log");
        log_file.deleteOnExit();
        PrintStream log_stream = new PrintStream(log_file);
        File input_contour_file = File.createTempFile("nuages", ".cont");
        input_contour_file.deleteOnExit();
        PrintStream stream = new PrintStream(input_contour_file);
        JxPlaneTraceList[] planes = object.getPlaneTraceList(view);
        stream.printf("S %d\n", planes.length);
        for (JxPlaneTraceList plane : planes) {
            if (this.isInterrupted()) throw new JxTerminateException();
            JxTrace[] traces = plane.getList();
            double offset = traces[0].getOffset();
            traces = this.processOpenTraces(traces);
            int count = 0;
            for (JxTrace trace : traces) {
                int size = trace.size();
                count += size;
            }
            stream.printf("v %d z %g\n", count, offset);
            for (JxTrace trace : traces) {
                stream.printf("{\n");
                for (JxPoint point : trace) stream.printf("%g %g\n", point.getU(), point.getV());
                stream.printf("}\n");
            }
        }
        stream.close();
        Prepros prepros = new Prepros();
        prepros.setLinear();
        prepros.enableShake();
        prepros.setLogStream(log_stream);
        File prepros_file = File.createTempFile("nuages", ".coord");
        prepros_file.deleteOnExit();
        File repros_out_file = File.createTempFile("nuages", ".tri");
        repros_out_file.deleteOnExit();
        File repros_new_file = File.createTempFile("nuages", ".new");
        repros_new_file.deleteOnExit();
        File out_file = File.createTempFile("nuages", ".out");
        out_file.deleteOnExit();
        TriangleMesh triangulation = null;
        JxSegmentation segmentation = super.getSegmentation();
        try {
            prepros.setInput(input_contour_file);
            prepros.setOutput(prepros_file);
            new JxStatusEvent().send("Preprocessing " + name);
            if (this.isInterrupted()) throw new JxTerminateException();
            if (prepros.process() == 0) {
                if (this.isInterrupted()) throw new JxTerminateException();
                Repros repros = new Repros();
                repros.enableMinimizeVolume();
                repros.disableInternalVertices();
                repros.disableExternalTetrahedra();
                repros.enableStatistics();
                repros.setInput(prepros_file);
                repros.setOutput(repros_out_file);
                repros.setNewFile(repros_new_file);
                repros.setLogStream(log_stream);
                if (this.isInterrupted()) throw new JxTerminateException();
                new JxStatusEvent().send("Tiling " + name);
                repros.process();
                if (this.isInterrupted()) throw new JxTerminateException();
                Repros2Visu repros2visu = new Repros2Visu();
                repros2visu.setOutputFormat(Repros2Visu.IDX);
                repros2visu.enableRemoveHorizontal();
                repros2visu.setObjectName(name);
                repros2visu.setHeader(name + ": " + new Date().toString());
                repros2visu.setFacetFile(repros_out_file);
                repros2visu.setCoordinateFile(repros_new_file);
                repros2visu.setOutputFile(out_file);
                repros2visu.setLogStream(log_stream);
                if (this.isInterrupted()) throw new JxTerminateException();
                new JxStatusEvent().send("Postprocessing " + name);
                repros2visu.process();
                if (this.isInterrupted()) throw new JxTerminateException();
                log_stream.close();
                log_file.delete();
                triangulation = this.getRenderable(out_file, segmentation, view);
                prepros_file.delete();
                input_contour_file.delete();
                repros_out_file.delete();
                repros_new_file.delete();
                out_file.delete();
            } else {
                log_stream.close();
                this.processError(segmentation, log_file, input_contour_file, object, view);
            }
        } catch (Throwable ex) {
            String jar_name = this.jarDiagnostics(ex, new File[] { log_file, input_contour_file, prepros_file, repros_out_file, repros_new_file, out_file });
            new JxErrorEvent().send("Internal error processing " + name + ". Please send file " + jar_name + " to maintainer.");
            throw new JxTerminateException();
        }
        return interpolator.smooth(triangulation, new JxStatusCallbackImpl());
    }

    protected TriangleMesh processThread(JxView view, JxObject object, SurfaceSmoother interpolator) {
        throw new UnsupportedOperationException("processThread");
    }

    protected TriangleMesh processPointThread(JxView view, JxObject object, SurfaceSmoother interpolator) {
        throw new UnsupportedOperationException("processPointThread");
    }

    private String jarDiagnostics(Throwable throwable, File[] files) throws IOException {
        File home = new File(System.getProperty("user.home"));
        File jar_file = File.createTempFile("nuages-diagnostic.", ".jar", home);
        FileOutputStream fos = new FileOutputStream(jar_file);
        JarOutputStream jos = new JarOutputStream(fos);
        ZipEntry ze = new ZipEntry("Exception");
        jos.putNextEntry(ze);
        PrintStream ps = new PrintStream(jos);
        throwable.printStackTrace(ps);
        for (File file : files) if ((file != null) && file.exists()) {
            ze = new ZipEntry(file.getName());
            jos.putNextEntry(ze);
            byte[] buffer = new byte[65536];
            FileInputStream fis = new FileInputStream(file);
            int len;
            while ((len = fis.read(buffer)) > 0) jos.write(buffer, 0, len);
            fis.close();
        }
        jos.close();
        return jar_file.getPath();
    }

    private void processError(JxSegmentation segmentation, File log_file, File coord_file, JxObject object, JxView view) throws FileNotFoundException, IOException {
        LineNumberReader log_reader = new LineNumberReader(new FileReader(log_file));
        String text;
        HashSet<String> error_set = new HashSet<String>();
        while ((text = log_reader.readLine()) != null) if (text.startsWith("ERROR:")) {
            text += " " + log_reader.readLine();
            error_set.add(text);
        }
        log_reader.close();
        if (error_set.size() > 0) {
            ArrayList<Triplet> points = this.readErrorPoints(coord_file);
            Hashtable<ErrorLocation, JxNuagesError> errors = new Hashtable<ErrorLocation, JxNuagesError>();
            for (String s : error_set) {
                String[] tokens = s.split(" +");
                ErrorLocation error_location = new ErrorLocation();
                error_location.setSection(Integer.parseInt(tokens[2]));
                error_location.setContour1(Integer.parseInt(tokens[4]));
                int seg;
                if (tokens[6].equals("self-intersecting")) {
                    seg = 8;
                    error_location.setType(ErrorLocation.SELF_INTERSECTING);
                } else {
                    error_location.setContour2(Integer.parseInt(tokens[8]));
                    error_location.setType(ErrorLocation.INTERSECTING);
                    seg = 10;
                }
                JxNuagesError error = errors.get(error_location);
                if (error == null) errors.put(error_location, error = new JxNuagesError());
                error.setType(error_location.getType().equals(ErrorLocation.SELF_INTERSECTING) ? "Self-Intersection" : "Contour Overlap");
                error.addErrorCoordinate(Integer.parseInt(tokens[seg]), points);
                error.addErrorCoordinate(Integer.parseInt(tokens[seg + 1]), points);
                error.addErrorCoordinate(Integer.parseInt(tokens[seg + 3]), points);
                error.addErrorCoordinate(Integer.parseInt(tokens[seg + 4]), points);
            }
            JxNuagesError[] list = errors.values().toArray(new JxNuagesError[errors.size()]);
            Arrays.sort(list);
            new JxNuagesErrorDialog(segmentation, object, view, list);
        }
    }

    private ArrayList<Triplet> readErrorPoints(File coord_file) throws IOException {
        LineNumberReader lnr = new LineNumberReader(new FileReader(coord_file));
        ArrayList<Triplet> points = new ArrayList<Triplet>();
        try {
            String[] tokens = lnr.readLine().split(" +");
            int sections = Integer.parseInt(tokens[1]);
            for (int s = 0; s < sections; s++) this.readSection(points, lnr);
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            lnr.close();
        }
        return points;
    }

    @SuppressWarnings("empty-statement")
    private void readSection(ArrayList<Triplet> points, LineNumberReader reader) throws IOException {
        String[] tokens = reader.readLine().split(" +");
        int n = Integer.parseInt(tokens[1]);
        double w = Double.parseDouble(tokens[3]);
        while (n > 0) {
            while (!reader.readLine().split(" +")[0].equals("{")) ;
            while (!((tokens = reader.readLine().split(" +"))[0].equals("}"))) {
                Triplet point = new Triplet(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]), w, -1);
                points.add(point);
                n--;
            }
        }
    }

    private class ErrorLocation {

        static final int SELF_INTERSECTING = 0;

        static final int INTERSECTING = 1;

        private int _type;

        void setType(int type) {
            this._type = type;
        }

        private int _contour_1;

        void setContour1(int contour_1) {
            this._contour_1 = contour_1;
        }

        private int _contour_2;

        void setContour2(int contour_2) {
            this._contour_2 = contour_2;
        }

        private int _section;

        void setSection(int section) {
            this._section = section;
        }

        @Override
        public boolean equals(Object o) {
            boolean equals = false;
            if (o instanceof ErrorLocation) {
                ErrorLocation el = (ErrorLocation) o;
                equals = (this._type == el._type) && (this._section == el._section) && (this._contour_1 == el._contour_1) && (this._contour_2 == el._contour_2);
            }
            return equals;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 61 * hash + this._type;
            hash = 61 * hash + this._contour_1;
            hash = 61 * hash + this._contour_2;
            hash = 61 * hash + this._section;
            return hash;
        }

        @Override
        public String toString() {
            return this._type + " " + this._section + " " + this._contour_1 + " " + this._contour_2;
        }

        private Object getType() {
            return this._type;
        }
    }

    private TriangleMesh getRenderable(File surface_file, JxSegmentation segmentation, JxView view) throws FileNotFoundException, IOException {
        LineNumberReader surface_reader = new LineNumberReader(new FileReader(surface_file));
        String[] tokens;
        ArrayList<Triplet[]> vertices = new ArrayList<Triplet[]>();
        ArrayList<int[]> indices = new ArrayList<int[]>();
        String s;
        JxOrientation orientation = super.getOrientation(view);
        while ((s = surface_reader.readLine()) != null) {
            tokens = s.trim().split(" +");
            if (tokens[0].equals("V")) {
                int n_vertices = Integer.parseInt(tokens[1]);
                double z = Double.parseDouble(tokens[3]);
                for (int v = 0; v < n_vertices; v++) {
                    tokens = surface_reader.readLine().trim().split(" +");
                    Triplet vertex = orientation.transform(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]), z);
                    Triplet normal = view.rotate(Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3]), Double.parseDouble(tokens[4]));
                    vertices.add(new Triplet[] { vertex, normal });
                }
            } else if (tokens[0].equals("T")) {
                int triangles = Integer.parseInt(tokens[1]);
                for (int t = 0; t < triangles; t++) {
                    tokens = surface_reader.readLine().trim().split(" +");
                    indices.add(new int[] { Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]) });
                }
            }
        }
        return new TriangleMesh(vertices, indices);
    }

    private JxTrace[] processOpenTraces(JxTrace[] traces) {
        JxTrace[] new_traces = new JxTrace[traces.length];
        for (int i = 0; i < traces.length; i++) {
            JxTrace original = traces[i];
            double min = original.findMinimumInterval();
            double[][] rs_xy = original.resample(min).getXY();
            double[][] xy = new double[rs_xy.length + 2][2];
            double[][] new_xy = new double[rs_xy.length * 2][];
            for (int p = 0, pxy = 1; p < rs_xy.length; p++, pxy++) xy[pxy] = rs_xy[p];
            xy[0][0] = xy[1][0] - (xy[2][0] - xy[1][0]);
            xy[0][1] = xy[1][1] - (xy[2][1] - xy[1][1]);
            int j = rs_xy.length;
            xy[j + 1][0] = xy[j][0] + (xy[j][0] - xy[j - 1][0]);
            xy[j + 1][1] = xy[j][1] + (xy[j][1] - xy[j - 1][1]);
            for (int pxy = 1, p0 = 0, p1 = new_xy.length - 1; pxy < xy.length - 1; pxy++, p0++, p1--) this.calculateTranslations(xy, pxy, min, new_xy[p0] = new double[2], new_xy[p1] = new double[2]);
            JxOrientation orientation = original.getOrientation();
            JxClosedTrace trace = new JxClosedTrace(orientation);
            JxPoint[] points = new JxPoint[new_xy.length];
            double offset = original.getOffset();
            for (int p = 0; p < new_xy.length; p++) points[p] = new JxPoint(new_xy[p][0], new_xy[p][1], offset, 0);
            trace.addArray(points);
            new_traces[i] = trace;
        }
        return new_traces;
    }

    private void calculateTranslations(double[][] xy, int p, double min, double[] left, double[] right) {
        double vx0 = xy[p - 1][0] - xy[p][0];
        double vy0 = xy[p - 1][1] - xy[p][1];
        double l0 = Math.sqrt((vx0 * vx0) + (vy0 * vy0));
        vx0 /= l0;
        vy0 /= l0;
        double vx1 = xy[p + 1][0] - xy[p][0];
        double vy1 = xy[p + 1][1] - xy[p][1];
        double l1 = Math.sqrt((vx1 * vx1) + (vy1 * vy1));
        vx1 /= l1;
        vy1 /= l1;
        double vx = (vx1 - vx0) / 2;
        double vy = (vy1 - vy0) / 2;
        double l = Math.sqrt((vx * vx) + (vy * vy));
        if (l == 0) {
            vx = vx1;
            vy = vy1;
        } else {
            vx /= l;
            vy /= l;
        }
        double nx = -vy;
        double ny = vx;
        double npx0 = xy[p][0];
        double npy0 = xy[p][1];
        double npx1 = npx0 + nx;
        double npy1 = npy0 + ny;
        double lx = -vy1;
        double ly = vx1;
        this.intersect(npx0, npy0, npx1, npy1, xy[p][0], xy[p][1], xy[p + 1][0], xy[p + 1][1], lx, ly, min, left);
        this.intersect(npx0, npy0, npx1, npy1, xy[p][0], xy[p][1], xy[p + 1][0], xy[p + 1][1], lx, ly, -min, right);
    }

    private void intersect(double npx0, double npy0, double npx1, double npy1, double px0, double py0, double px1, double py1, double tx, double ty, double offset, double[] result) {
        double tpx0 = (tx * 0.01 * offset) + px0;
        double tpy0 = (ty * 0.01 * offset) + py0;
        double tpx1 = (tx * 0.01 * offset) + px1;
        double tpy1 = (ty * 0.01 * offset) + py1;
        double dxt = tpx1 - tpx0;
        double dyt = tpy1 - tpy0;
        double dxn = npx1 - npx0;
        double dyn = npy1 - npy0;
        DoubleSquareMatrix matrix = new DoubleSquareMatrix(new double[][] { { dxt, -dxn }, { dyt, -dyn } });
        AbstractDoubleSquareMatrix inv = matrix.inverse();
        Double2Vector rhs = new Double2Vector(npx0 - tpx0, npy0 - tpy0);
        AbstractDoubleVector tau = inv.multiply(rhs);
        double t = tau.getComponent(0);
        result[0] = (dxt * t) + tpx0;
        result[1] = (dyt * t) + tpy0;
    }
}
