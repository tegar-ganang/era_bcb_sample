package org.jvaptools.PatternEditor;

/**
 *
 * @author root
 */
public class PatternEditor extends javax.swing.JPanel {

    javax.sound.midi.Sequence seq;

    javax.sound.midi.Track[] tracks;

    int len;

    int ticklen;

    int barspace;

    int noteheight;

    java.awt.Rectangle clip = new java.awt.Rectangle();

    float zoom = 1f;

    EditorNote DragNote;

    boolean wasDragged = false;

    long dragoffset;

    /** Creates a new instance of PatternEditor */
    public PatternEditor(javax.sound.midi.Sequence s) {
        super();
        noteheight = 7;
        this.seq = s;
        if (s != null) {
            ticklen = (int) s.getTickLength();
            this.tracks = s.getTracks();
            zoom = (float) 75 / (float) s.getResolution();
            barspace = seq.getResolution();
            this.ExtractNotes();
        } else {
            ticklen = 1000;
            zoom = 1f;
            barspace = 50;
        }
        len = java.lang.Math.round(ticklen * zoom);
        this.setSize(len, 128 * noteheight);
        this.setPreferredSize(new java.awt.Dimension(len, 128 * noteheight));
        javax.swing.event.MouseInputAdapter ma = new javax.swing.event.MouseInputAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int notenum = 127 - e.getPoint().y / noteheight;
                    int tickstart = (int) (((float) e.getPoint().x) / zoom);
                    EditorNote n = findNote(notenum, tickstart);
                    if (n != null) {
                        if (n.track != null) {
                            n.track.remove(n.onEvt);
                            n.track.remove(n.offEvt);
                        }
                        notes.remove(n);
                        repaint((int) ((float) n.notestart * zoom), (127 - notenum) * noteheight, (int) ((float) (n.notelen) * zoom) + 3, noteheight + 2);
                    } else {
                        int tickend = tickstart + barspace / 2;
                        javax.sound.midi.ShortMessage msgOn = new javax.sound.midi.ShortMessage();
                        javax.sound.midi.ShortMessage msgOff = new javax.sound.midi.ShortMessage();
                        try {
                            msgOn.setMessage(msgOn.NOTE_ON, 0, notenum, 100);
                            msgOff.setMessage(msgOff.NOTE_OFF, 0, notenum, 100);
                            javax.sound.midi.MidiEvent onEvt = new javax.sound.midi.MidiEvent(msgOn, tickstart);
                            javax.sound.midi.MidiEvent offEvt = new javax.sound.midi.MidiEvent(msgOff, tickend);
                            if (tracks != null) tracks[0].add(onEvt);
                            if (tracks != null) tracks[0].add(offEvt);
                            if (tracks != null) notes.add(new EditorNote(onEvt, offEvt, tracks[0])); else notes.add(new EditorNote(onEvt, offEvt, null));
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        repaint((int) ((float) tickstart * zoom), (127 - notenum) * noteheight, (int) ((float) (tickend - tickstart) * zoom) + 3, noteheight + 2);
                    }
                }
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                int notenum = 127 - e.getPoint().y / noteheight;
                int tickstart = (int) (((float) e.getPoint().x) / zoom);
                DragNote = findNote(notenum, tickstart);
                wasDragged = false;
                if (DragNote != null) {
                    dragoffset = tickstart - DragNote.notestart;
                } else dragoffset = 0;
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (DragNote != null) {
                    int notenum = 127 - e.getPoint().y / noteheight;
                    long tickstart = (int) (((float) e.getPoint().x) / zoom) - dragoffset;
                    if ((tickstart != DragNote.notestart) || (notenum != DragNote.noteval) || wasDragged) {
                        if (DragNote.track != null) {
                            DragNote.track.remove(DragNote.onEvt);
                            DragNote.track.remove(DragNote.offEvt);
                        }
                        notes.remove(DragNote);
                        repaint((int) ((float) DragNote.notestart * zoom), (127 - DragNote.noteval) * noteheight, (int) ((float) (DragNote.notelen) * zoom) + 2, noteheight + 2);
                        if ((tickstart != DragNote.notestart) || wasDragged) {
                            DragNote.onEvt.setTick(tickstart);
                            DragNote.offEvt.setTick(tickstart + DragNote.notelen);
                            DragNote.notestart = tickstart;
                        }
                        if ((notenum != DragNote.noteval) || wasDragged) {
                            try {
                                DragNote.noteval = notenum;
                                DragNote.on.setMessage(DragNote.on.NOTE_ON, DragNote.chan, notenum, DragNote.vel);
                                DragNote.off.setMessage(DragNote.on.NOTE_OFF, DragNote.chan, notenum, DragNote.offVel);
                            } catch (Exception ex) {
                            }
                        }
                        if (DragNote.track != null) {
                            DragNote.track.add(DragNote.offEvt);
                            DragNote.track.add(DragNote.onEvt);
                        }
                        notes.add(DragNote);
                        repaint((int) ((float) DragNote.notestart * zoom), (127 - DragNote.noteval) * noteheight, (int) ((float) (DragNote.notelen) * zoom) + 2, noteheight + 2);
                    }
                }
                DragNote = null;
            }

            public void mouseMoved(java.awt.event.MouseEvent e) {
            }

            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (DragNote != null) {
                    wasDragged = true;
                    int notenum = 127 - e.getPoint().y / noteheight;
                    long tickstart = (int) (((float) e.getPoint().x) / zoom) - dragoffset;
                    if ((tickstart != DragNote.notestart) || (notenum != DragNote.noteval)) {
                        long oldstart = DragNote.notestart;
                        int oldv = DragNote.noteval;
                        DragNote.notestart = tickstart;
                        DragNote.noteval = notenum;
                        repaint((int) ((float) oldstart * zoom), (127 - oldv) * noteheight, (int) ((float) (DragNote.notelen) * zoom) + 2, noteheight + 2);
                        repaint((int) ((float) DragNote.notestart * zoom), (127 - DragNote.noteval) * noteheight, (int) ((float) (DragNote.notelen) * zoom) + 2, noteheight + 2);
                        repaint();
                    }
                }
            }
        };
        this.addMouseListener(ma);
        this.addMouseMotionListener(ma);
    }

    class EditorNote {

        javax.sound.midi.ShortMessage on, off;

        javax.sound.midi.MidiEvent onEvt, offEvt;

        javax.sound.midi.Track track;

        int noteval;

        long notestart;

        long notelen;

        int chan;

        int vel;

        int offVel;

        public EditorNote(javax.sound.midi.MidiEvent onEvt, javax.sound.midi.MidiEvent offEvt, javax.sound.midi.Track track) {
            this.onEvt = onEvt;
            this.offEvt = offEvt;
            this.on = (javax.sound.midi.ShortMessage) this.onEvt.getMessage();
            this.off = (javax.sound.midi.ShortMessage) this.offEvt.getMessage();
            this.track = track;
            this.noteval = on.getData1();
            this.notestart = onEvt.getTick();
            this.notelen = offEvt.getTick() - onEvt.getTick();
            this.chan = on.getChannel();
            this.vel = on.getData2();
            this.offVel = off.getData2();
        }
    }

    public java.util.Vector<EditorNote> notes = new java.util.Vector<EditorNote>();

    public void ExtractNotes() {
        if (tracks != null && tracks.length > 0) {
            for (int i = 0; i < tracks.length; i++) {
                for (int j = 0; j < tracks[i].size(); j++) {
                    javax.sound.midi.MidiMessage e = tracks[i].get(j).getMessage();
                    if (e instanceof javax.sound.midi.ShortMessage) {
                        javax.sound.midi.ShortMessage m = ((javax.sound.midi.ShortMessage) e);
                        if ((m.getCommand() == javax.sound.midi.ShortMessage.NOTE_ON) && m.getData2() != 0) {
                            int chan = m.getChannel();
                            int notenum = m.getData1();
                            javax.sound.midi.MidiEvent notenende = null;
                            int k = j + 1;
                            while ((notenende == null) && k < tracks[i].size()) {
                                javax.sound.midi.MidiMessage e2 = tracks[i].get(k).getMessage();
                                if (e2 instanceof javax.sound.midi.ShortMessage) {
                                    javax.sound.midi.ShortMessage m2 = ((javax.sound.midi.ShortMessage) e2);
                                    if (((m2.getCommand() == javax.sound.midi.ShortMessage.NOTE_OFF) || ((m2.getCommand() == javax.sound.midi.ShortMessage.NOTE_ON))) && ((m2.getChannel() == chan) && (m2.getData1() == notenum))) {
                                        notenende = tracks[i].get(k);
                                    }
                                }
                                k++;
                            }
                            if (notenende != null) {
                                notes.add(new EditorNote(tracks[i].get(j), notenende, tracks[i]));
                            }
                        }
                    }
                }
            }
        }
    }

    public EditorNote findNote(int notenum, long tick) {
        int len = notes.size();
        for (int i = 0; i < len; i++) {
            if ((notes.get(i).noteval == notenum) && (notes.get(i).notestart <= tick) && ((notes.get(i).notestart + notes.get(i).notelen) >= tick)) return notes.get(i);
        }
        return null;
    }

    public long getTickLen() {
        return ticklen;
    }

    public float getZoom() {
        return zoom;
    }

    public int getBarspace() {
        return this.barspace;
    }

    public int getNoteheight() {
        return this.noteheight;
    }

    EditorHeader header;

    public void setHeader(EditorHeader h) {
        this.header = h;
    }

    protected void paintComponent(java.awt.Graphics g) {
        clip.x = 0;
        clip.y = 0;
        clip.height = this.getHeight();
        clip.width = this.getWidth();
        g.getClipBounds(clip);
        java.awt.Color oldc = g.getColor();
        g.setColor(this.getBackground());
        g.fillRect(clip.x, clip.y, clip.width + 1, clip.height + 1);
        g.setColor(this.getForeground());
        int ypos = -1;
        for (int i = 1; i < 128; i++) {
            ypos = ypos + noteheight;
            if ((ypos >= clip.y) && (ypos <= (clip.y + clip.height))) g.drawLine(clip.x, ypos, clip.x + clip.width, ypos);
        }
        int tickpos = 0;
        int bars = ticklen / barspace;
        for (int i = 1; i < bars; i++) {
            tickpos = tickpos + barspace;
            int xpos = java.lang.Math.round(tickpos * zoom);
            if ((xpos >= clip.x) && (xpos <= clip.x + clip.width)) g.drawLine(xpos, clip.y, xpos, clip.y + clip.height);
        }
        int len = notes.size();
        for (int i = 0; i < len; i++) {
            EditorNote note = notes.get(i);
            int notenum = 127 - note.noteval;
            int sx = java.lang.Math.round((float) note.notestart * zoom);
            int sx2 = java.lang.Math.round((float) (note.notestart + note.notelen) * zoom);
            if ((sx < clip.x + clip.width) && (sx2 >= clip.x) && (notenum * noteheight <= clip.y + clip.height) && ((notenum + 1) * noteheight - 1 >= clip.y)) {
                g.setColor(java.awt.Color.ORANGE);
                g.fillRect(sx + 1, notenum * noteheight + 1, sx2 - sx - 1, noteheight - 3);
                g.setColor(this.getForeground());
                g.drawRect(sx, notenum * noteheight, sx2 - sx, noteheight - 2);
            }
        }
        if (header.isBarDragged()) {
            int xpos = java.lang.Math.round(header.getDraggedPos() * zoom);
            if ((xpos >= clip.x) && (xpos <= clip.x + clip.width)) {
                g.setColor(java.awt.Color.RED);
                g.drawLine(xpos, clip.y, xpos, clip.y + clip.height);
            }
        }
        g.setColor(oldc);
    }
}
