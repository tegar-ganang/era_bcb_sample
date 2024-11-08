package ch.unibe.im2.inkanno.util;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import ch.unibe.eindermu.euclidian.Vector;
import ch.unibe.eindermu.utils.FileUtil;
import ch.unibe.im2.inkanno.Document;
import ch.unibe.im2.inkanno.InkAnnoAnnotationStructure;
import ch.unibe.inkml.InkBind;
import ch.unibe.inkml.InkCanvasTransform;
import ch.unibe.inkml.InkChannel;
import ch.unibe.inkml.InkContext;
import ch.unibe.inkml.InkInkSource;
import ch.unibe.inkml.InkMLComplianceException;
import ch.unibe.inkml.InkMapping;
import ch.unibe.inkml.InkTrace;
import ch.unibe.inkml.InkTraceFormat;
import ch.unibe.inkml.InkTraceLeaf;
import ch.unibe.inkml.InkTracePoint;
import ch.unibe.inkml.InkTraceView;
import ch.unibe.inkml.InkTraceViewContainer;
import ch.unibe.inkml.InkTraceViewLeaf;
import ch.unibe.inkml.InkChannel.ChannelName;
import ch.unibe.inkml.util.TraceViewTreeManipulationException;
import ch.unibe.inkml.util.TraceVisitor;

public class DocumentRepair {

    private Document doc;

    public DocumentRepair(Document d) {
        doc = d;
    }

    public boolean run() throws InkMLComplianceException, ManualInteractionNeededException {
        boolean res = false;
        return res;
    }

    /**
     * @return
     */
    private boolean setDocumentId() {
        String id = "http://iam.unibe.ch/fki/database/iamondodb/" + FileUtil.getInfo(doc.getFile()).name;
        if (!id.equals(doc.getInk().getDocumentId())) {
            doc.getInk().setDocumentId(id);
            return true;
        }
        return false;
    }

    public boolean correctingInkSource() {
        return false;
    }

    public boolean notifyNewTimeIssue() throws IOException, InvalidDocumentException {
        List<InkTrace> list = doc.getInk().getTraces();
        boolean ok = false;
        for (int index1 = 0; index1 < list.size() - 1; index1++) {
            if (list.get(index1).getTimeSpan().end > list.get(index1 + 1).getTimeSpan().start) {
                ok = true;
                System.out.format("Time issue detected: trace %s in document %s has size %f, but should be aroud %f\n", list.get(index1).getId(), doc.getFile().getName(), list.get(index1).getTimeSpan().getDuration(), list.get(index1 + 1).getTimeSpan().start - list.get(index1).getTimeSpan().start);
                Document d = new Document(new File(doc.getFile().getPath().replaceFirst("\\.inkml$", ".txt")), doc.getAnnotationStructure());
                InkTraceLeaf correct = (InkTraceLeaf) d.getInk().getDefinitions().get(list.get(index1).getId());
                if (correct.getPointCount() != list.get(index1).getPointCount()) {
                    throw new Error("point count is not matching");
                }
                boolean same = true;
                double startDifference = list.get(index1).getPoint(0).get(ChannelName.T) - correct.getPoint(0).get(ChannelName.T);
                for (int j = 0; j < correct.getPointCount(); j++) {
                    InkTracePoint correct_tp = null;
                    try {
                        correct_tp = list.get(index1).getContext().getCanvasTransform().transform(correct.getPoint(j), list.get(index1).getContext().getSourceFormat(), list.get(index1).getContext().getCanvasTraceFormat());
                    } catch (InkMLComplianceException e) {
                        throw new Error("can not transform Point as expected");
                    }
                    if (list.get(index1).getPoint(j).get(ChannelName.X) != correct_tp.get(ChannelName.X)) {
                        throw new Error(String.format("not same point: x1 = %f, x2 = %f", list.get(index1).getPoint(j).get(ChannelName.X), correct_tp.get(ChannelName.X)));
                    }
                    if (list.get(index1).getPoint(j).get(ChannelName.T) != correct_tp.get(ChannelName.T)) {
                        same = false;
                    }
                    list.get(index1).getPoint(j).set(ChannelName.T, startDifference + correct_tp.get(ChannelName.T));
                }
                if (same) {
                    double shift = list.get(index1).getTimeSpan().end - list.get(index1 + 1).getTimeSpan().start + 0.5;
                    System.out.format("Time issue more severe than expected: postpone following strokes by %f\n", shift);
                    for (int index2 = index1 + 1; index2 < list.size(); index2++) {
                        for (int pointIndex = 0; pointIndex < list.get(index2).getPointCount(); pointIndex++) {
                            list.get(index2).getPoint(pointIndex).set(ChannelName.T, list.get(index2).getPoint(pointIndex).get(ChannelName.T) + shift);
                        }
                    }
                }
            }
        }
        return ok;
    }

    public boolean addTraceGroupIds() {
        final List<Boolean> handled = new ArrayList<Boolean>(1);
        handled.add(false);
        TraceVisitor nv = new TraceVisitor() {

            protected void visitHook(InkTraceViewContainer container) {
                if (container.getId() == null) {
                    container.getIdNow(InkTraceViewContainer.ID_PREFIX);
                    handled.set(0, true);
                }
                super.visitHook(container);
            }

            @Override
            protected void visitHook(InkTraceViewLeaf leaf) {
            }
        };
        nv.go(doc.getCurrentViewRoot());
        return handled.get(0);
    }

    public boolean repairCanvasTraceFormat() {
        boolean handled = false;
        try {
            handled = true;
            InkTraceFormat format = doc.getInk().getCurrentContext().getCanvasTraceFormat();
            InkTraceFormat new_format = format.clone(doc.getInk());
            new_format.getChannel(ChannelName.T).setUnits("s");
            new_format.getChannel(ChannelName.F).setMax("255");
            new_format.getChannel(ChannelName.F).setMin("0");
            new_format.getChannel(ChannelName.F).setIntermittent(false);
            new_format.setId("inkAnnoCanvasFormat");
            new_format.setFinal();
            doc.getInk().getCurrentContext().getCanvas().setInkTraceFormat(new_format);
            InkTraceFormat logitechFormat = ((InkTraceFormat) doc.getInk().getDefinitions().get("Logitechformat")).clone(doc.getInk());
            logitechFormat.getChannel(ChannelName.T).setUnits("s");
            logitechFormat.getChannel(ChannelName.F).setMax("255");
            logitechFormat.getChannel(ChannelName.F).setMin("0");
            doc.getInk().getDefinitions().remove("Logitechformat");
            logitechFormat.setId("Logitechformat");
            logitechFormat.setFinal();
            InkInkSource source = doc.getInk().getCurrentContext().getInkSource();
            if (source != null) {
                source.setTraceFormat(logitechFormat);
            } else {
                doc.getInk().getDefinitions().enterElement(logitechFormat);
            }
            if (doc.getInk().containsAnnotation("PenId")) {
                source.setSerialNo(doc.getInk().getAnnotation("PenId"));
            }
            source.setSampleRate(0.013);
            source.setSampleRateUniform(false);
        } catch (InkMLComplianceException e) {
        }
        return handled;
    }

    public boolean isAffectedByTimeIssue() {
        for (InkTrace t : doc.getFlatTraces()) {
            double start = (Double) t.getPoints().get(0).get(InkChannel.ChannelName.T);
            double end = (Double) t.getPoints().get(t.getPointCount() - 1).get(InkChannel.ChannelName.T);
            if (end - start < (t.getPointCount() * 0.013) - 0.040) {
                return true;
            }
        }
        return false;
    }

    private boolean repairTableIssue() throws TraceViewTreeManipulationException {
        boolean res = false;
        List<InkTraceViewContainer> tables = new ArrayList<InkTraceViewContainer>();
        repairTableIssueHelper1(doc.getCurrentViewRoot(), tables);
        for (InkTraceViewContainer table : tables) {
            res = repairTableIssue(table);
        }
        return res;
    }

    public int recognizeTableIssue(InkTraceViewContainer table) {
        int wordCount = 0;
        for (InkTraceView v : table.getContent()) {
            if (!v.isLeaf() && v.testAnnotation(InkAnnoAnnotationStructure.TYPE, "Textline")) {
                InkTraceViewContainer textline = (InkTraceViewContainer) v;
                if (wordCount == 0) {
                    wordCount = textline.getContent().size();
                } else {
                    if (wordCount != textline.getContent().size()) {
                        wordCount = -1;
                    }
                }
            }
        }
        return wordCount;
    }

    public boolean repairTableIssue(InkTraceViewContainer table) {
        boolean res = false;
        int wordCount = recognizeTableIssue(table);
        if (wordCount == 1) {
            System.out.println("Table is well formed.");
            return res;
        }
        if (wordCount == -1) {
            System.out.println("Can't automaticly repair table.");
            return res;
        }
        List<InkTraceView> words = new ArrayList<InkTraceView>();
        for (InkTraceView v : table.getContent()) {
            if (!v.isLeaf() && v.containsAnnotation(InkAnnoAnnotationStructure.TYPE) && v.getAnnotation(InkAnnoAnnotationStructure.TYPE).equals("Textline")) {
                int i = 0;
                InkTraceViewContainer textline = (InkTraceViewContainer) v;
                for (InkTraceView v2 : textline.getContent()) {
                    if (i != 0) {
                        words.add(v2);
                    } else {
                        textline.annotate("transcription", v2.getAnnotation("transcription"));
                    }
                    i++;
                }
            }
        }
        for (InkTraceView word : words) {
            List<InkTraceView> list = new ArrayList<InkTraceView>();
            list.add(word);
            InkTraceView newLine = table.createChildContainer(list);
            newLine.annotate(InkAnnoAnnotationStructure.TYPE, "Textline");
            newLine.annotate("transcription", word.getAnnotation("transcription"));
        }
        System.out.println("Table missformed: repaired.");
        return res;
    }

    private void repairTableIssueHelper1(InkTraceViewContainer container, List<InkTraceViewContainer> tables) {
        for (InkTraceView v : container.getContent()) {
            if (!v.isLeaf()) {
                if (v.containsAnnotation(InkAnnoAnnotationStructure.TYPE) && v.getAnnotation(InkAnnoAnnotationStructure.TYPE).equals("Table")) {
                    tables.add((InkTraceViewContainer) v);
                } else {
                    repairTableIssueHelper1((InkTraceViewContainer) v, tables);
                }
            }
        }
    }

    private boolean repairTimeIssue() throws InkMLComplianceException {
        for (InkTrace t : doc.getFlatTraces()) {
            int i = 0;
            double time = 0;
            double base = 0;
            for (InkTracePoint p : t.getPoints()) {
                if (i == 0) {
                    time = (Double) p.get(InkChannel.ChannelName.T);
                    base = time;
                } else {
                    time = time + (Double) p.get(InkChannel.ChannelName.T) - base;
                    p.set(InkChannel.ChannelName.T, time);
                }
                i++;
            }
            ((InkTraceLeaf) t).backTransformPoints();
        }
        return true;
    }

    private boolean repairCanvasTransform() {
        InkContext c = doc.getCurrentViewRoot().getContext();
        InkCanvasTransform tf = c.getCanvasTransform();
        boolean flip = false;
        if (tf.getForwardMapping().getType() == InkMapping.Type.IDENTITY) {
            if (tf.getForwardMapping().getBinds().size() > 0) {
                for (InkBind b : tf.getForwardMapping().getBinds()) {
                    if (b.hasSource() && b.hasTarget() && b.source != b.target && (b.source == InkChannel.ChannelName.X || b.source == InkChannel.ChannelName.Y)) {
                        flip = true;
                        break;
                    }
                }
            }
        }
        if (flip) {
            System.out.println("repair canvasTransform: invalid identity-mapping replaced by affine-mapping");
            tf.flipAxis(c.getSourceFormat(), c.getCanvasTraceFormat());
        }
        return flip;
    }

    private boolean repairAxisMirroring() throws InkMLComplianceException {
        boolean res = false;
        InkContext c = doc.getCurrentViewRoot().getContext();
        InkCanvasTransform tf = c.getCanvasTransform();
        List<InkChannel.ChannelName> invertChannels = new ArrayList<InkChannel.ChannelName>();
        for (InkChannel channel : c.getCanvasTraceFormat().getChannels()) {
            if (channel.getOrientation() == InkChannel.Orientation.M) {
                invertChannels.add(channel.getName());
                channel.setOrientation(InkChannel.Orientation.P);
            }
        }
        for (InkChannel.ChannelName name : invertChannels) {
            tf.invertAxis(c.getSourceFormat(), c.getCanvasTraceFormat(), name);
            System.out.println("repair canvas traceFormat: orientation -ve of channel " + name + " tranfered to canvasTranform.");
            res = true;
        }
        doc.getInk().reloadTraces();
        return res;
    }

    /**
     * @return
     */
    private boolean removeUnusedTranscription() {
        return removeUnusedTranscription_help(doc.getCurrentViewRoot());
    }

    private boolean removeUnusedTranscription_help(InkTraceView view) {
        boolean res = false;
        if (view.isLeaf()) {
            return false;
        }
        InkTraceViewContainer c = (InkTraceViewContainer) view;
        for (InkTraceView v : c.getContent()) {
            res = removeUnusedTranscription_help(v) || res;
        }
        if (!doc.getAnnotationStructure().getItem(c).annotations.containsKey("transcription") && c.containsAnnotation("transcription")) {
            c.removeAnnotation("transcription");
            System.err.println("transcription removed from " + c.getAnnotation(InkAnnoAnnotationStructure.TYPE));
            res = true;
        }
        return res;
    }

    private boolean repairLabel() throws ManualInteractionNeededException {
        return repairLabel_rec(doc.getCurrentViewRoot());
    }

    private boolean repairLabel_rec(InkTraceView view) throws ManualInteractionNeededException {
        boolean res = false;
        if (view.isLeaf()) {
            return false;
        }
        InkTraceViewContainer c = (InkTraceViewContainer) view;
        for (InkTraceView v : c.getContent()) {
            res = repairLabel_rec(v) || res;
        }
        if (c.testAnnotation(InkAnnoAnnotationStructure.TYPE, "Textline")) {
            String lineTranscription = "";
            if (c.getAnnotation("transcription") != null) {
                lineTranscription = c.getAnnotation("transcription");
            }
            Rectangle2D r = null;
            Vector p = null;
            final Vector pv = new Vector(0.0, 0.0);
            List<InkTraceViewContainer> words = new ArrayList<InkTraceViewContainer>();
            for (InkTraceView word : c.getContent()) {
                if (word.isLeaf()) continue;
                String s = word.getAnnotation(InkAnnoAnnotationStructure.TYPE);
                if (s == null) {
                    System.out.println("view in Textline: '" + c.getAnnotation("transcription") + "' is not defined.");
                    continue;
                }
                if (s.equals("Word") || s.equals("Arrow") || s.equals("Symbol")) {
                    words.add((InkTraceViewContainer) word);
                    if (r == null) {
                        p = new Vector(word.getCenterOfGravity());
                        r = new Rectangle();
                        r.setFrameFromDiagonal(p, p);
                    } else {
                        r.add(word.getCenterOfGravity());
                        pv.setLocation(pv.plus((new Vector(word.getCenterOfGravity())).minus(p).norm()));
                        p.setLocation(word.getCenterOfGravity());
                    }
                }
                if (s.equals("Correction")) {
                    System.out.println("can't correct Textline: it contains Correction'");
                    return false;
                }
            }
            pv.setLocation(pv.norm());
            if (words.size() == 1) {
                if (!lineTranscription.trim().equals(repairLabel_getTrans(words.get(0)).trim())) {
                    System.out.println("change Textline: '" + lineTranscription + "' to '" + repairLabel_getTrans(words.get(0)) + "'.");
                    res = true;
                }
            } else {
                if (Math.max(r.getWidth(), r.getHeight()) / Math.min(r.getWidth(), r.getHeight()) < 2) {
                    System.out.println("Textline '" + lineTranscription + "' is weird.");
                    return res;
                }
                Collections.sort(words, new Comparator<InkTraceViewContainer>() {

                    public int compare(InkTraceViewContainer o1, InkTraceViewContainer o2) {
                        double d = (new Vector(o2.getCenterOfGravity())).minus(o1.getCenterOfGravity()).scalar(pv);
                        return (int) -(d / Math.abs(d));
                    }
                });
                String str = "";
                for (InkTraceViewContainer word : words) {
                    String s = repairLabel_getTrans(word);
                    if (!s.trim().isEmpty()) {
                        str += s.trim() + " ";
                    }
                }
                str = str.trim();
                if (!lineTranscription.replace("  ", " ").trim().equals(str)) {
                    System.out.println("change Textline: '" + lineTranscription + "' to '" + str + "'.");
                    res = true;
                }
            }
        }
        return res;
    }

    private String repairLabel_getTrans(InkTraceView v) throws ManualInteractionNeededException {
        if (v.testAnnotation(InkAnnoAnnotationStructure.TYPE, "Word")) {
            if (!v.containsAnnotation("transcription")) {
                return "";
            }
            return v.getAnnotation("transcription");
        } else if (v.testAnnotation(InkAnnoAnnotationStructure.TYPE, "Symbol")) {
            return "";
        } else if (v.testAnnotation(InkAnnoAnnotationStructure.TYPE, "Arrow")) {
            if (!v.containsAnnotation("transcription")) {
                throw new ManualInteractionNeededException("There is an arrow without transcription.");
            }
            return "";
        }
        return "";
    }
}
