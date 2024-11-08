package medieveniti.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import medieveniti.system.AppConfigUserHttpServletImpl;

/**
 * Liest die compressor.ini aus und verkettet die angegebenen Dateien
 * entsprechend miteinander und komprimiert diese anschließend mit dem yui-compressor.
 * Bei Übergabe eines unc-Parameters entfällt das komprimieren.
 * 
 * <h1>compressor.ini</h1>
 * <p>Die compressor.ini befindet sich im WEB-INF-Ordner und wird zeilenweise mit UTF-8-Kodierung
 * eingelesen. Dabei werden sowohl Leerzeilen, also auch Zeilen, die mit einem ! beginnen, 
 * nicht beachtet. Bei allen restlichen Zeilen, wird zwischen zwei Typen unterschieden:
 * den Output-Files, welche von eckigen Klammern umschlossen werden, und den Input-Files,
 * denen keine besondere Kennzeichnung zugrunde liegt.</p>
 * <p>Output-Files leiten einen neuen Block ein, da ein Output-File immer aus mehreren
 * Input-Files zusammengesetzt wird, die nach dieser Output-File-Deklaration aufgezählt werden.
 * Es darf demnach kein Input-File deklariert werden, bevor das erst Output-File deklariert wurde.</p>
 * 
 * @author Hans Kirchner
 * 
 */
public class CompressorServlet extends AppConfigUserHttpServletImpl {

    public void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        long startTime = System.currentTimeMillis();
        response.setContentType("text/plain; charset=utf-8");
        String absPath = getAppConfig().getPathConfig().getAbsoluteServerPath();
        PrintWriter out = response.getWriter();
        boolean compress = request.getParameter("unc") == null;
        String outputFile = null;
        ArrayList<String> inputFiles = null;
        out.println("Compressor servlet (use parameter unc to leave files uncompressed)");
        out.println("START");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(absPath + "WEB-INF/compressor.ini"), "utf-8"));
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.trim();
            if (!(line.isEmpty() || line.startsWith("!"))) {
                if (line.startsWith("[") && line.endsWith("]")) {
                    if (outputFile != null) {
                        compress(outputFile, inputFiles, out, compress);
                    }
                    outputFile = line.substring(1, line.length() - 1);
                    inputFiles = new ArrayList<String>();
                } else {
                    if (outputFile == null) {
                        out.println("Defined an input file before an outputfile in line " + lineNumber);
                    } else {
                        inputFiles.add(line);
                    }
                }
            }
        }
        if (outputFile != null) {
            compress(outputFile, inputFiles, out, compress);
        }
        reader.close();
        long duration = (System.currentTimeMillis() - startTime);
        out.println("FINISHED (in " + (duration / 1000.0) + " seconds)");
    }

    private void compress(String outputFile, ArrayList<String> inputFiles, PrintWriter log, boolean compress) throws Exception {
        String absPath = getAppConfig().getPathConfig().getAbsoluteServerPath();
        log.println("Concat files into: " + outputFile);
        OutputStream out = new FileOutputStream(absPath + outputFile);
        byte[] buffer = new byte[4096];
        int readBytes;
        for (String file : inputFiles) {
            log.println(" Read: " + file);
            InputStream in = new FileInputStream(absPath + file);
            while ((readBytes = in.read(buffer)) != -1) {
                out.write(buffer, 0, readBytes);
            }
            in.close();
        }
        out.close();
        if (compress) {
            long normalSize = new File(absPath + outputFile).length();
            ProcessBuilder builder = new ProcessBuilder("java", "-jar", "WEB-INF/yuicompressor.jar", outputFile, "-o", outputFile, "--line-break", "4000");
            builder.directory(new File(absPath));
            Process process = builder.start();
            process.waitFor();
            long minSize = new File(absPath + outputFile).length();
            long diff = normalSize - minSize;
            double percentage = Math.floor((double) diff / normalSize * 1000.0) / 10.0;
            double diffSize = (Math.floor(diff / 1024.0 * 10.0) / 10.0);
            log.println("Result: " + percentage + " % (" + diffSize + " KB)");
        }
    }
}
