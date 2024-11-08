package ch.laoe.clip;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.sound.sampled.AudioFormat;
import ch.laoe.audio.Audio;
import ch.laoe.clip.AChannelSelection.Point;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GEditableSegments;
import ch.laoe.ui.GPersistence;
import ch.laoe.ui.LProgressViewer;
import ch.laoe.ui.Laoe;
import ch.oli4.io.XmlInputStream;
import ch.oli4.io.XmlOutputStream;

/***********************************************************

This file is part of LAoE.

LAoE is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation; either version 2 of the License,
or (at your option) any later version.

LAoE is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LAoE; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


Class:			AClipStorage
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	clip fileformat saver/loader. file format definition:
		XML format is used, the filename-extension is ".laoe". it is gzipped. "complete" 
		clip-types describe a complete clip, "partial" clip describes only relative changes 
		to the previous clip. complete clips need all attributes of the tags, partial clips 
		only attributes to identify the tag and the changed attributes. the "length"-attribute 
		in the samples-tag is mandatory. following fields are optional on partial clips: 
		layer, channel, samples, selection. all data outside the "laoe" field are ignored. 
		the fileformat is identified	through the laoe-tag and its attribute "fileformat".

		the file consists of two parts, the xml-header described below, which contains general
		informations, and the samples-body, which is outside the xml-part and contains all
		sample values, as binary streamed float-values.


		<?xml version="1.0" encoding="UTF-8"?>
		<laoe version="v0.3.17 alpha" fileformat="one" samples="below/extern">
			<clip name="about.laoe" samplerate="44100" samplewidth="16" comment="created by LAoE! friday 13.07.2001 oli4">
				<audio loopEndPointer="4423" loopStartPointer="4079"/>
				<layer index="0" name="layer 1" type="audioLayer">
					<channel index="0" name="channel 1" id="ch0123" audible="true">
						<samples length="1200"/>
						<selection name="selection 1" offset="0" length="10030"/>
						<plotter xOffset="0" xLength="10030" yOffset="-128" yLength="256"/>
					</channel>
					...
				</layer>
				...
			</clip>
		</laoe>
		optional binary serialized samples in the same order than described in the XML-header...

		Since LAoE version 0.4.04 beta the channel-selection may optionally contain intensity-information.
		So the selection may be an element with separate begin- and end-tag in addition to the old data-less
		version. Backward-compatibility must be guaranteed!!! Here an example:
		...
					<channel index="0" name="channel 1" id="ch0123" audible="true">
						<samples length="1200"/>
						<selection name="selection 1" offset="0" length="10030">
							<intensity x="0" y=".49"/>
							<intensity x=".4" y="1"/>
							<intensity x=".735" y=".22"/>
							<intensity x="1" y="0"/>
						</selection>
						<plotter xOffset="0" xLength="10030" yOffset="-128" yLength="256"/>
					</channel>
		...

		Since LAoE version 0.4.05 beta the channel may optionally contain a mask.Backward-compatibility
		must be guaranteed!!! Here an example:
		...
					<channel index="0" name="channel 1" id="ch0123" audible="true">
						<mask name="mask 1">
							<volumePoint x="12345" y=".34"/>
							<volumePoint x="48347" y=".99"/>
						</mask>
						...
					</channel>
		...

		The layer contains a new optional attribute "plotType": at the time, two different plottypes exist: sample-curve
		and spectrogram.
		...
		<layer index="0" name="layer 1" type="audioLayer" plotType="spectrogram">
		...

		Since LAoE version 0.4.08 beta the channel may optionally contain markers.Backward-compatibility
		must be guaranteed!!! Here an example:
		...
					<channel index="0" name="channel 1" id="ch0123" audible="true">
						...
						<markers name="marker 1">
							<markerPoint x="2557"/>
							<markerPoint x="12345"/>
						</markers>
						...
					</channel>
		...

      Since LAoE version 0.6.02 beta the channel-plotter may optionally contain the color, in
      hexadecimal format. Backward-compatibility must be guaranteed!!! Here an example:
      ...
               <channel index="0" name="channel 1" id="ch0123" audible="true">
                  ...
                  <plotter xOffset="0" xLength="10030" yOffset="-128" yLength="256" color="00FF66"/>
               </channel>
      ...

      Since LAoE version 0.7.03 the optional clip-plotter contains background color and grid color, in
      hexadecimal format. Backward-compatibility must be guaranteed!!! Here an example:
      ...
               <clip name="about.laoe" samplerate="44100" samplewidth="16" comment="created by LAoE! friday 13.07.2001 oli4">
                  <clipPlotter bgColor="112233" gridColor="AABBCC"/>
                  ...
               </clip>
      ...



History:
Date:			Description:									Autor:
13.07.01		first draft										oli4
15.07.01		gzipped and serialized						oli4
18.07.01		put serialized samples at the end		oli4
22.01.02		add intensity to the selection			oli4
03.03.02		add channel-mask								oli4
18.03.02		add layer plottype							oli4
04.07.02		add markers										oli4
27.04.2003  add channel-plotter color              oli4
19-10-2010  add additional colors                  oli4

***********************************************************/
public class AClipStorage {

    /**
	 *	loads a clip from a file. returns true, if the fileformat
	 *	was accepted, and false if it was a wrong fileformat
	 */
    public static boolean supports(File f) throws IOException {
        return f.getPath().endsWith(".laoe");
    }

    /**
	 *	loads a clip from a file. returns true, if the fileformat
	 *	was accepted, and false if it was a wrong fileformat
	 * @param c clip reference to be reused (yes, reference only!)
	 */
    public static boolean load(AClip c, File f) throws IOException {
        ALayer l = null;
        AChannel ch = null;
        AChannelSelection chSel = null;
        AChannelMask chMask = null;
        AChannelMarker chMarker = null;
        int loopStartPointer = 0;
        int loopEndPointer = 0;
        try {
            GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)));
            XmlInputStream is = new XmlInputStream(zis);
            boolean xmlHeader = true;
            boolean samplesBelow = true;
            while (xmlHeader) {
                int t = is.read();
                switch(t) {
                    case XmlInputStream.SYSTEM_TAG:
                        break;
                    case XmlInputStream.BEGIN_TAG:
                        if (is.getTagName().equals("laoe")) {
                            if (is.getAttribute("samples").equals("below")) {
                                samplesBelow = true;
                            } else {
                                samplesBelow = false;
                            }
                        } else if (is.getTagName().equals("clip")) {
                            c.removeAll();
                            c.setName(is.getAttribute("name"));
                            c.setSampleRate(Float.parseFloat(is.getAttribute("samplerate")));
                            c.setSampleWidth(Integer.parseInt(is.getAttribute("samplewidth")));
                            c.setComments(is.getAttribute("comment"));
                        } else if (is.getTagName().equals("layer")) {
                            l = new ALayer();
                            l.setName(is.getAttribute("name"));
                            String ty = is.getAttribute("type");
                            if (ty.equals("audioLayer")) {
                                l.setType(ALayer.AUDIO_LAYER);
                            } else if (ty.equals("parameterLayer")) {
                                l.setType(ALayer.PARAMETER_LAYER);
                            }
                            String pt = is.getAttribute("plotType");
                            if (pt != null) {
                                if (pt.equals("sampleCurve")) {
                                    l.setPlotType(ALayer.SAMPLE_CURVE_TYPE);
                                } else if (pt.equals("spectrogram")) {
                                    l.setPlotType(ALayer.SPECTROGRAM_TYPE);
                                }
                            }
                            c.add(l);
                        } else if (is.getTagName().equals("channel")) {
                            ch = new AChannel();
                            if (!samplesBelow) {
                                ch.setChangeId(is.getAttribute("id"));
                            }
                            ch.setName(is.getAttribute("name"));
                            ch.setAudible(is.getAttribute("audible").equals("true"));
                            l.add(ch);
                        } else if (is.getTagName().equals("selection")) {
                            chSel = new AChannelSelection();
                            ch.setSelection(chSel);
                            chSel.setChannel(ch);
                            chSel.setName(is.getAttribute("name"));
                            chSel.setOffset(Integer.parseInt(is.getAttribute("offset")));
                            chSel.setLength(Integer.parseInt(is.getAttribute("length")));
                        } else if (is.getTagName().equals("mask")) {
                            chMask = ch.getMask();
                            chMask.setName(is.getAttribute("name"));
                        } else if (is.getTagName().equals("markers")) {
                            chMarker = ch.getMarker();
                            chMarker.setName(is.getAttribute("name"));
                        } else if (is.getTagName().equals("graphicObjects")) {
                            ch.getGraphicObjects().fromXmlElement(is);
                        }
                        break;
                    case XmlInputStream.END_TAG:
                        if (is.getTagName().equals("laoe")) {
                            xmlHeader = false;
                        }
                        break;
                    case XmlInputStream.BEGIN_END_TAG:
                        if (is.getTagName().equals("selection")) {
                            chSel = new AChannelSelection();
                            ch.setSelection(chSel);
                            chSel.setChannel(ch);
                            chSel.setName(is.getAttribute("name"));
                            chSel.setOffset(Integer.parseInt(is.getAttribute("offset")));
                            chSel.setLength(Integer.parseInt(is.getAttribute("length")));
                        } else if (is.getTagName().equals("plotter")) {
                            ch.getPlotter().setXRange(Float.parseFloat(is.getAttribute("xOffset")), Float.parseFloat(is.getAttribute("xLength")));
                            ch.getPlotter().setYRange(Float.parseFloat(is.getAttribute("yOffset")), Float.parseFloat(is.getAttribute("yLength")));
                            if (is.containsAttribute("color")) {
                                l.getPlotter().setColor(new Color(Integer.parseInt(is.getAttribute("color"), 16)));
                            }
                        } else if (is.getTagName().equals("clipPlotter")) {
                            if (is.containsAttribute("bgColor")) {
                                c.getPlotter().setBgColor(new Color(Integer.parseInt(is.getAttribute("bgColor"), 16)));
                            }
                            if (is.containsAttribute("gridColor")) {
                                c.getPlotter().setGridColor(new Color(Integer.parseInt(is.getAttribute("gridColor"), 16)));
                            }
                        } else if (is.getTagName().equals("audio")) {
                            loopEndPointer = Integer.parseInt(is.getAttribute("loopEndPointer"));
                            loopStartPointer = Integer.parseInt(is.getAttribute("loopStartPointer"));
                        } else if (is.getTagName().equals("samples")) {
                            ch.setSamples(new MMArray(Integer.parseInt(is.getAttribute("length")), 0));
                        } else if (is.getTagName().equals("intensity")) {
                            float x = Float.parseFloat(is.getAttribute("x"));
                            float y = Float.parseFloat(is.getAttribute("y"));
                            chSel.addIntensityPoint(x, y);
                        } else if (is.getTagName().equals("volumePoint")) {
                            float x = Float.parseFloat(is.getAttribute("x"));
                            float y = Float.parseFloat(is.getAttribute("y"));
                            chMask.getSegments().addPoint(x, y);
                        } else if (is.getTagName().equals("markerPoint")) {
                            int x = Integer.parseInt(is.getAttribute("x"));
                            chMarker.addMarker(x);
                        }
                        break;
                    case XmlInputStream.DATA_CHUNK:
                        break;
                    case XmlInputStream.EOF:
                        xmlHeader = false;
                        break;
                }
            }
            c.getAudio().setLoopEndPointer(loopEndPointer);
            c.getAudio().setLoopStartPointer(loopStartPointer);
            c.getAudio().setEncoding(AudioFormat.Encoding.PCM_SIGNED);
            c.getAudio().setFileType(Audio.fileTypeLaoe);
            if (samplesBelow) {
                ObjectInputStream ois = new ObjectInputStream(zis);
                LProgressViewer.getInstance().entrySubProgress(0.9, "clip " + f.getName());
                for (int i = 0; i < c.getNumberOfLayers(); i++) {
                    if (LProgressViewer.getInstance().setProgress(1.0 * (i + 1) / c.getNumberOfLayers())) return false;
                    l = c.getLayer(i);
                    LProgressViewer.getInstance().entrySubProgress(0.3, "layer " + i);
                    for (int j = 0; j < l.getNumberOfChannels(); j++) {
                        if (LProgressViewer.getInstance().setProgress(1.0 * (j + 1) / l.getNumberOfChannels())) return false;
                        ch = l.getChannel(j);
                        LProgressViewer.getInstance().entrySubProgress(0.3, "channel " + j);
                        ch.setSamples(loadSamples(ois));
                        LProgressViewer.getInstance().exitSubProgress();
                    }
                    LProgressViewer.getInstance().exitSubProgress();
                }
                LProgressViewer.getInstance().exitSubProgress();
                ois.close();
            } else {
                is.close();
            }
        } catch (IOException ioe) {
            Debug.printStackTrace(5, ioe);
        }
        return true;
    }

    /**
	 *	saves a clip to a file. 
	 */
    public static void save(AClip c, File f) throws IOException {
        save(c, f, true);
    }

    /**
	 *	saves a clip with link to external samples
	 */
    public static void saveWithoutSamples(AClip c, File f) throws IOException {
        save(c, f, false);
    }

    private static void save(AClip c, File f, boolean completeClip) throws IOException {
        try {
            GZIPOutputStream zos = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            XmlOutputStream os = new XmlOutputStream(zos);
            os.appendSystemTag();
            os.appendCR();
            HashMap<String, String> attr = new HashMap<String, String>();
            attr.put("version", Laoe.version);
            attr.put("fileformat", "one");
            if (completeClip) {
                attr.put("samples", "below");
            } else {
                attr.put("samples", "extern");
            }
            os.appendBeginTag("laoe", attr);
            os.appendCR();
            attr.clear();
            attr.put("name", c.getName());
            attr.put("samplerate", Float.toString(c.getSampleRate()));
            attr.put("samplewidth", Integer.toString(c.getSampleWidth()));
            attr.put("comment", c.getComments());
            os.appendTab(1);
            os.appendBeginTag("clip", attr);
            os.appendCR();
            Audio aud = c.getAudio();
            if (aud != null) {
                attr.clear();
                attr.put("loopStartPointer", Integer.toString(aud.getLoopStartPointer()));
                attr.put("loopEndPointer", Integer.toString(aud.getLoopEndPointer()));
                os.appendTab(2);
                os.appendBeginEndTag("audio", attr);
                os.appendCR();
            }
            AClipPlotter cplt = c.getPlotter();
            if (cplt != null) {
                attr.clear();
                attr.put("bgColor", Integer.toString(c.getPlotter().getBgColor().getRGB(), 16));
                attr.put("gridColor", Integer.toString(c.getPlotter().getGridColor().getRGB(), 16));
                os.appendTab(2);
                os.appendBeginEndTag("clipPlotter", attr);
                os.appendCR();
            }
            for (int i = 0; i < c.getNumberOfLayers(); i++) {
                ALayer l = c.getLayer(i);
                attr.clear();
                attr.put("index", Integer.toString(i));
                attr.put("name", l.getName());
                switch(l.getType()) {
                    case ALayer.PARAMETER_LAYER:
                        attr.put("type", "parameterLayer");
                        break;
                    default:
                        attr.put("type", "audioLayer");
                        break;
                }
                switch(l.getPlotType()) {
                    case ALayer.SAMPLE_CURVE_TYPE:
                        attr.put("plotType", "sampleCurve");
                        break;
                    case ALayer.SPECTROGRAM_TYPE:
                        attr.put("plotType", "spectrogram");
                        break;
                }
                os.appendTab(2);
                os.appendBeginTag("layer", attr);
                os.appendCR();
                for (int j = 0; j < l.getNumberOfChannels(); j++) {
                    AChannel ch = l.getChannel(j);
                    attr.clear();
                    attr.put("index", Integer.toString(j));
                    attr.put("name", ch.getName());
                    attr.put("id", ch.getChangeId());
                    attr.put("audible", String.valueOf(ch.isAudible()));
                    os.appendTab(3);
                    os.appendBeginTag("channel", attr);
                    os.appendCR();
                    attr.clear();
                    attr.put("length", Integer.toString(ch.getSampleLength()));
                    if (!completeClip) {
                        attr.put("location", ch.getChangeId());
                    }
                    os.appendTab(4);
                    os.appendBeginEndTag("samples", attr);
                    os.appendCR();
                    AChannelSelection sel = ch.getSelection();
                    if (sel != null) {
                        attr.clear();
                        attr.put("name", sel.getName());
                        attr.put("offset", Integer.toString(sel.getOffset()));
                        attr.put("length", Integer.toString(sel.getLength()));
                        os.appendTab(4);
                        os.appendBeginTag("selection", attr);
                        os.appendCR();
                        ArrayList<Point> intensity = sel.getIntensityPoints();
                        for (int k = 0; k < intensity.size(); k++) {
                            attr.clear();
                            attr.put("x", Float.toString((float) ((AChannelSelection.Point) intensity.get(k)).x));
                            attr.put("y", Float.toString((float) ((AChannelSelection.Point) intensity.get(k)).y));
                            os.appendTab(5);
                            os.appendBeginEndTag("intensity", attr);
                            os.appendCR();
                        }
                        os.appendTab(4);
                        os.appendEndTag("intensity");
                        os.appendCR();
                    }
                    AChannelMask chMask = ch.getMask();
                    if (chMask != null) {
                        attr.clear();
                        attr.put("name", chMask.getName());
                        os.appendTab(4);
                        os.appendBeginTag("mask", attr);
                        os.appendCR();
                        GEditableSegments seg = chMask.getSegments();
                        for (int k = 0; k < seg.getNumberOfPoints(); k++) {
                            attr.clear();
                            attr.put("x", Float.toString((float) seg.getPointX(k)));
                            attr.put("y", Float.toString((float) seg.getPointY(k)));
                            os.appendTab(5);
                            os.appendBeginEndTag("volumePoint", attr);
                            os.appendCR();
                        }
                        os.appendTab(4);
                        os.appendEndTag("mask");
                        os.appendCR();
                    }
                    AChannelMarker chMarker = ch.getMarker();
                    if (chMask != null) {
                        attr.clear();
                        attr.put("name", chMarker.getName());
                        os.appendTab(4);
                        os.appendBeginTag("markers", attr);
                        os.appendCR();
                        for (int k = 0; k < chMarker.getNumberOfMarkers(); k++) {
                            attr.clear();
                            attr.put("x", Integer.toString(chMarker.getMarkerX(k)));
                            os.appendTab(5);
                            os.appendBeginEndTag("markerPoint", attr);
                            os.appendCR();
                        }
                        os.appendTab(4);
                        os.appendEndTag("markers");
                        os.appendCR();
                    }
                    AChannelPlotter plt = ch.getPlotter();
                    if (plt != null) {
                        attr.clear();
                        attr.put("xOffset", Float.toString((float) plt.getXOffset()));
                        attr.put("xLength", Float.toString((float) plt.getXLength()));
                        attr.put("yOffset", Float.toString(plt.getYOffset()));
                        attr.put("yLength", Float.toString(plt.getYLength()));
                        attr.put("color", Integer.toString(l.getPlotter().getColor().getRGB(), 16));
                        os.appendTab(4);
                        os.appendBeginEndTag("plotter", attr);
                        os.appendCR();
                    }
                    if (ch.getGraphicObjects() != null) {
                        ch.getGraphicObjects().toXmlElement(os);
                    }
                    os.appendTab(3);
                    os.appendEndTag("channel");
                    os.appendCR();
                }
                os.appendTab(2);
                os.appendEndTag("layer");
                os.appendCR();
            }
            os.appendTab(1);
            os.appendEndTag("clip");
            os.appendCR();
            os.appendEndTag("laoe");
            if (completeClip) {
                ObjectOutputStream oos = new ObjectOutputStream(zos);
                LProgressViewer.getInstance().entrySubProgress(0.3, "clip " + f.getName());
                for (int i = 0; i < c.getNumberOfLayers(); i++) {
                    if (LProgressViewer.getInstance().setProgress(1.0 * (i + 1) / c.getNumberOfLayers())) return;
                    ALayer l = c.getLayer(i);
                    LProgressViewer.getInstance().entrySubProgress(0.3, "layer " + i);
                    for (int j = 0; j < l.getNumberOfChannels(); j++) {
                        if (LProgressViewer.getInstance().setProgress(1.0 * (j + 1) / l.getNumberOfChannels())) return;
                        AChannel ch = l.getChannel(j);
                        LProgressViewer.getInstance().entrySubProgress(0.3, "channel " + j);
                        saveSamples(ch, oos);
                        LProgressViewer.getInstance().exitSubProgress();
                    }
                    LProgressViewer.getInstance().exitSubProgress();
                }
                oos.close();
                LProgressViewer.getInstance().exitSubProgress();
            } else {
                os.appendCR();
                os.close();
            }
        } catch (IOException ioe) {
            Debug.printStackTrace(5, ioe);
        }
    }

    /**
	 *	loads a sample-array in serialized form
	 */
    private static MMArray loadSamples(ObjectInputStream ois) throws IOException {
        try {
            LProgressViewer.getInstance().entrySubProgress(0.9);
            int l = ois.readInt();
            MMArray s = new MMArray(l, 0);
            for (int i = 0; i < s.getLength(); i++) {
                s.set(i, ois.readFloat());
                if ((i & 0x3FF) == 0) {
                    if (LProgressViewer.getInstance().setProgress(1.0 * (i + 1) / s.getLength())) return s;
                }
            }
            LProgressViewer.getInstance().exitSubProgress();
            return s;
        } catch (IOException ioe) {
            Debug.printStackTrace(5, ioe);
            return null;
        }
    }

    /**
	 *	saves a sample-array in serialized form
	 */
    private static void saveSamples(AChannel ch, ObjectOutputStream oos) throws IOException {
        try {
            LProgressViewer.getInstance().entrySubProgress(0.9);
            oos.writeInt(ch.getSampleLength());
            for (int i = 0; i < ch.getSampleLength(); i++) {
                oos.writeFloat(ch.getSample(i));
                if ((i & 0x3FF) == 0) {
                    if (LProgressViewer.getInstance().setProgress(1.0 * (i + 1) / ch.getSampleLength())) return;
                }
            }
            oos.flush();
            LProgressViewer.getInstance().exitSubProgress();
        } catch (IOException ioe) {
            Debug.printStackTrace(5, ioe);
        }
    }

    private static final boolean channelCompressionEnable = GPersistence.createPersistance().getBoolean("history.compression");

    /**
	 *	saves a sample-array in serialized form
	 */
    public static void saveSamples(AChannel ch, File f) throws IOException {
        try {
            ObjectOutputStream oos;
            if (channelCompressionEnable) {
                oos = new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(f))));
            } else {
                oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            }
            saveSamples(ch, oos);
            oos.close();
        } catch (IOException ioe) {
            Debug.printStackTrace(5, ioe);
        }
    }

    /**
	 *	loads a sample-array in serialized form
	 */
    public static MMArray loadSamples(File f) throws IOException {
        try {
            ObjectInputStream ois;
            if (channelCompressionEnable) {
                ois = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(f))));
            } else {
                ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));
            }
            MMArray s = loadSamples(ois);
            ois.close();
            return s;
        } catch (IOException ioe) {
            Debug.printStackTrace(5, ioe);
            return null;
        }
    }
}
