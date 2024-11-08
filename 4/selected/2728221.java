package spdrender.net;

import spdrender.parser.*;
import spdrender.raytracer.Scene;
import spdrender.raytracer.RayTracer;
import spdrender.raytracer.LoadBalancer;
import java.io.*;
import java.net.*;

/**
 * Distributed render client implementation.
 * @author Maximiliano Monterrubio Gutierrez
 */
public class RenderNode {

    public static String TEMP_SCENE_FILENAME = "temp.xml";

    private int clock, threads;

    private BufferedReader br;

    private PrintWriter pw;

    private Scene scene;

    private int[] pieces;

    private RayTracer rt;

    /**
     * Creates a new render client.
     * @param host The server address to connect to.
     * @param port The TCP port number used to establish a connection.
     * @param clock The processor speed in Mhz.
     * @param threads Number of threads to run in this node.
     * @throws java.net.UnknownHostException In case the host address cannot be resolved.
     * @throws java.io.IOException In case a network I/O error occurs.
     * @throws spdrender.parser.SceneParserException In case the server sends an invalid XML scene
     * specification to render.
     */
    public RenderNode(String host, int port, int clock, int threads) throws UnknownHostException, IOException, SceneParserException {
        this.clock = clock;
        this.threads = threads;
        Socket s = new Socket(host, port);
        InputStream is = s.getInputStream();
        OutputStream os = s.getOutputStream();
        pw = new PrintWriter(os, true);
        File ts = new File(TEMP_SCENE_FILENAME);
        if (ts.exists()) if (!ts.delete()) {
            throw new IOException("Couldn't delete file: " + ts.getAbsolutePath());
        }
        FileOutputStream fos = new FileOutputStream(ts);
        int fileSize = 0x0;
        int f;
        while ((f = is.read()) != '\n') {
            fileSize *= 10;
            fileSize += f & 0xF;
        }
        for (int i = 0; i < fileSize; ++i) {
            fos.write(is.read());
        }
        br = new BufferedReader(new InputStreamReader(is));
        fos.close();
        pw.println(threads + "," + clock);
        scene = new SceneParser(ts).parseScene();
        String[] sp = br.readLine().split(",");
        pieces = new int[sp.length];
        for (int i = 0; i < pieces.length; ++i) {
            pieces[i] = Integer.valueOf(sp[i]);
        }
    }

    /**
     * Retrieves the progress of the local node render.
     * @return A <code>float</code> between 0 and 100 representing the progress of the local render.
     */
    public double getProgress() {
        if (rt != null) {
            return rt.getProgress();
        } else return 0.0f;
    }

    /**
     * Starts the render job on this node.
     */
    public void renderAndSend() {
        LoadBalancer lb = new LoadBalancer(1, 1);
        lb.setPieces(pieces);
        int[][] segments = lb.getUniformSegments(threads);
        rt = new RayTracer(scene, 5);
        rt.render(pieces, segments, threads, pw);
    }
}
