package org.apache.batik.transcoder.wmf.tosvg;

import com.google.code.appengine.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.ToSVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** This class implements the <tt>Transcoder</tt> interface and
 *  can convert a WMF input document into an SVG document.
 *  <p>This class is copied from
 *  batik org.apache.batik.transcoder.wmf.tosvg.WMFTranscoder class.</p>
 *  <p>It can use <tt>TranscoderInput</tt> that are either a URI
 *  or a <tt>InputStream</tt> or a <tt>Reader</tt>. The
 *  <tt>XMLReader</tt> and <tt>Document</tt> <tt>TranscoderInput</tt>
 *  types are not supported.</p>
 *
 *  <p>This transcoder can use <tt>TranscoderOutputs</tt> that are
 *  of any type except the <tt>XMLFilter</tt> type.</p>
 *
 *  <p>Corrected bugs from the original class:</p>
 *  <ul>
 *  <li> Manage images size</li>
 *  </ul>
 *  <p>Exemple of use :</p>
 *  <pre>
 *    WMFTranscoder transcoder = new WMFTranscoder();
 *    try {
 *       TranscoderInput wmf = new TranscoderInput(wmffile.toURL().toString());
 *       FileOutputStream fos = new FileOutputStream(svgFile);
 *       TranscoderOutput svg =
 *           new TranscoderOutput(new OutputStreamWriter(fos, "UTF-8"));
 *       transcoder.transcode(wmf, svg);
 *    } catch (MalformedURLException e){
 *       throw new TranscoderException(e);
 *    } catch (IOException e){
 *       throw new TranscoderException(e);
 *    }
 *  </pre>
 *  <p>Several transcoding hints are available for this transcoder :</p>
 *  <ul>
 *  <li>KEY_INPUT_WIDTH, KEY_INPUT_HEIGHT, KEY_XOFFSET, KEY_YOFFSET : this Integer values allows to
 *  set the  portion of the image to transcode, defined by the width, height, and offset
 *  of this portion in Metafile units.
 *  </ul>
 *  <pre>
 *     transcoder.addTranscodingHint(FromWMFTranscoder.KEY_INPUT_WIDTH, new Integer(input_width));
 *  </pre>
 *  </li>
 *  <li>KEY_WIDTH, KEY_HEIGHT : this Float values allows to force the width and height of the output:
 *  </ul>
 *  <pre>
 *     transcoder.addTranscodingHint(FromWMFTranscoder.KEY_WIDTH, new Float(width));
 *  </pre>
 *  </li>
 *  </ul>
 *
 * @version $Id: WMFTranscoder.java 577540 2007-09-20 04:26:59Z cam $
 */
public class WMFTranscoder extends ToSVGAbstractTranscoder {

    /**
     * Default constructor
     */
    public WMFTranscoder() {
    }

    /**
     * Transcodes the specified input in the specified output.
     * @param input the input to transcode
     * @param output the ouput where to transcode
     * @exception TranscoderException if an error occured while transcoding
     */
    public void transcode(TranscoderInput input, TranscoderOutput output) throws TranscoderException {
        DataInputStream is = getCompatibleInput(input);
        WMFRecordStore currentStore = new WMFRecordStore();
        try {
            currentStore.read(is);
        } catch (IOException e) {
            handler.fatalError(new TranscoderException(e));
            return;
        }
        float wmfwidth;
        float wmfheight;
        float conv = 1.0f;
        if (hints.containsKey(KEY_INPUT_WIDTH)) {
            wmfwidth = ((Integer) hints.get(KEY_INPUT_WIDTH)).intValue();
            wmfheight = ((Integer) hints.get(KEY_INPUT_HEIGHT)).intValue();
        } else {
            wmfwidth = currentStore.getWidthPixels();
            wmfheight = currentStore.getHeightPixels();
        }
        float width = wmfwidth;
        float height = wmfheight;
        if (hints.containsKey(KEY_WIDTH)) {
            width = ((Float) hints.get(KEY_WIDTH)).floatValue();
            conv = width / wmfwidth;
            height = height * width / wmfwidth;
        }
        int xOffset = 0;
        int yOffset = 0;
        if (hints.containsKey(KEY_XOFFSET)) {
            xOffset = ((Integer) hints.get(KEY_XOFFSET)).intValue();
        }
        if (hints.containsKey(KEY_YOFFSET)) {
            yOffset = ((Integer) hints.get(KEY_YOFFSET)).intValue();
        }
        float sizeFactor = currentStore.getUnitsToPixels() * conv;
        int vpX = (int) (currentStore.getVpX() * sizeFactor);
        int vpY = (int) (currentStore.getVpY() * sizeFactor);
        int vpW;
        int vpH;
        if (hints.containsKey(KEY_INPUT_WIDTH)) {
            vpW = (int) (((Integer) hints.get(KEY_INPUT_WIDTH)).intValue() * conv);
            vpH = (int) (((Integer) hints.get(KEY_INPUT_HEIGHT)).intValue() * conv);
        } else {
            vpW = (int) (currentStore.getWidthUnits() * sizeFactor);
            vpH = (int) (currentStore.getHeightUnits() * sizeFactor);
        }
        WMFPainter painter = new WMFPainter(currentStore, xOffset, yOffset, conv);
        Document doc = this.createDocument(output);
        svgGenerator = new SVGGraphics2D(doc);
        svgGenerator.getGeneratorContext().setPrecision(4);
        painter.paint(svgGenerator);
        svgGenerator.setSVGCanvasSize(new Dimension(vpW, vpH));
        Element svgRoot = svgGenerator.getRoot();
        svgRoot.setAttributeNS(null, SVG_VIEW_BOX_ATTRIBUTE, String.valueOf(vpX) + ' ' + vpY + ' ' + vpW + ' ' + vpH);
        writeSVGToOutput(svgGenerator, svgRoot, output);
    }

    /**
     * Checks that the input is one of URI or an <tt>InputStream</tt>
     * returns it as a DataInputStream
     */
    private DataInputStream getCompatibleInput(TranscoderInput input) throws TranscoderException {
        if (input == null) {
            handler.fatalError(new TranscoderException(String.valueOf(ERROR_NULL_INPUT)));
        }
        InputStream in = input.getInputStream();
        if (in != null) {
            return new DataInputStream(new BufferedInputStream(in));
        }
        String uri = input.getURI();
        if (uri != null) {
            try {
                URL url = new URL(uri);
                in = url.openStream();
                return new DataInputStream(new BufferedInputStream(in));
            } catch (MalformedURLException e) {
                handler.fatalError(new TranscoderException(e));
            } catch (IOException e) {
                handler.fatalError(new TranscoderException(e));
            }
        }
        handler.fatalError(new TranscoderException(String.valueOf(ERROR_INCOMPATIBLE_INPUT_TYPE)));
        return null;
    }

    public static final String WMF_EXTENSION = ".wmf";

    public static final String SVG_EXTENSION = ".svg";

    /**
     * Unit testing : Illustrates how the transcoder might be used.
     */
    public static void main(String[] args) throws TranscoderException {
        if (args.length < 1) {
            System.out.println("Usage : WMFTranscoder.main <file 1> ... <file n>");
            System.exit(1);
        }
        WMFTranscoder transcoder = new WMFTranscoder();
        int nFiles = args.length;
        for (int i = 0; i < nFiles; i++) {
            String fileName = args[i];
            if (!fileName.toLowerCase().endsWith(WMF_EXTENSION)) {
                System.err.println(args[i] + " does not have the " + WMF_EXTENSION + " extension. It is ignored");
            } else {
                System.out.print("Processing : " + args[i] + "...");
                String outputFileName = fileName.substring(0, fileName.toLowerCase().indexOf(WMF_EXTENSION)) + SVG_EXTENSION;
                File inputFile = new File(fileName);
                File outputFile = new File(outputFileName);
                try {
                    TranscoderInput input = new TranscoderInput(inputFile.toURL().toString());
                    TranscoderOutput output = new TranscoderOutput(new FileOutputStream(outputFile));
                    transcoder.transcode(input, output);
                } catch (MalformedURLException e) {
                    throw new TranscoderException(e);
                } catch (IOException e) {
                    throw new TranscoderException(e);
                }
                System.out.println(".... Done");
            }
        }
        System.exit(0);
    }
}
