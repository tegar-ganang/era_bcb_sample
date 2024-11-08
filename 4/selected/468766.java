package de.sistemich.mafrasi.stopmotion.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.event.EventListenerList;

/**
 * This class manages all captured frames.
 * @author Max Sistemich
 *
 */
public class FrameManager {

    private static final FrameManager instance = new FrameManager();

    private ArrayList<Frame> frames_;

    private DecimalFormat formatter_;

    private EventListenerList listeners = new EventListenerList();

    /**
	 * Enum to show how to manage the given files
	 * @author Max Sistemich
	 */
    public enum INSERT_TYPE {

        /**
		 * Move the file
		 */
        MOVE, /**
		 * Copy the file
		 */
        COPY
    }

    /**
	 * Constructs an empty FrameOrganizer.
	 */
    private FrameManager() {
        frames_ = new ArrayList<Frame>();
        formatter_ = new DecimalFormat("00000");
    }

    /**
	 * Return the frame at index
	 * @param index
	 * @return the frame at index
	 */
    public Frame getFrameAtIndex(int index) {
        return frames_.get(index);
    }

    /**
	 * Return the frame at the given time
	 * @param time The time in seconds
	 * @return the frame at time
	 */
    public Frame getFrameAtTime(float time) {
        if (time <= 0.0f) throw new IllegalArgumentException();
        if (time * Settings.getPropertyInteger(ConstantKeys.export_fps) >= getFrameCount()) throw new IllegalArgumentException();
        return frames_.get((int) (time * Settings.getPropertyInteger(ConstantKeys.export_fps)));
    }

    public String formatFileName(int number) {
        return "image_" + formatter_.format(number) + ".jpg";
    }

    /**
	 * Inserts a new frame at the end of the list
	 * 
	 * @param source the source image
	 * @param type decide, wether the file is copied (COPY) or moved (MOVE)
	 * @throws IOException
	 */
    private synchronized Frame addFrame(INSERT_TYPE type, File source) throws IOException {
        if (source == null) throw new NullPointerException("Parameter 'source' is null");
        if (!source.exists()) throw new IOException("File does not exist: " + source.getAbsolutePath());
        if (source.length() <= 0) throw new IOException("File is empty: " + source.getAbsolutePath());
        File newLocation = new File(Settings.getPropertyString(ConstantKeys.project_dir), formatFileName(frames_.size()));
        if (newLocation.compareTo(source) != 0) {
            switch(type) {
                case MOVE:
                    source.renameTo(newLocation);
                    break;
                case COPY:
                    FileChannel inChannel = new FileInputStream(source).getChannel();
                    FileChannel outChannel = new FileOutputStream(newLocation).getChannel();
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                    if (inChannel != null) inChannel.close();
                    if (outChannel != null) outChannel.close();
                    break;
            }
        }
        Frame f = new Frame(newLocation);
        f.createThumbNail();
        frames_.add(f);
        return f;
    }

    public synchronized void addFrames(INSERT_TYPE type, File... sources) {
        if (sources == null) throw new NullPointerException("Parameter 'source' is null");
        Frame[] frames = new Frame[sources.length];
        for (int i = sources.length - 1; i >= 0; i--) {
            File f = sources[i];
            try {
                Frame frame = addFrame(type, f);
                notifyInsert(new FrameUpdateEvent(this, frame));
                frames[i] = frame;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        notifyUpdateFinished(new FrameUpdateEvent(this, frames));
    }

    /**
	 * Inserts a new Frame at the given index. All following files are updated.
	 * 
	 * @param index the index, were the new frame is inserted
	 * @param source the source image
	 * @param type decide, wether the file is copied (COPY) or moved (MOVE)
	 * @throws IOException
	 */
    private synchronized Frame insertFrame(int index, File source, INSERT_TYPE type) throws IOException {
        if (source == null) throw new NullPointerException("Parameter 'source' is null");
        if (!source.exists()) throw new IOException("File does not exist: " + source.getAbsolutePath());
        if (source.length() <= 0) throw new IOException("File is empty: " + source.getAbsolutePath());
        if (index < 0) throw new IndexOutOfBoundsException("index < 0");
        if (index >= frames_.size()) throw new IndexOutOfBoundsException("index >= frames_.size()");
        File tmp = new File(Settings.getPropertyString(ConstantKeys.project_dir), "tmp.jpg");
        switch(type) {
            case MOVE:
                if (source.getParentFile().compareTo(new File(Settings.getPropertyString(ConstantKeys.project_dir))) == 0 && source.getName().matches("img_[0-9]{5}\\.jpg")) {
                    for (int i = 0; i < frames_.size(); i++) {
                        Frame f = frames_.get(i);
                        if (f.getFile().compareTo(source) == 0) {
                            frames_.remove(i);
                            break;
                        }
                    }
                }
                source.renameTo(tmp);
                break;
            case COPY:
                FileChannel inChannel = new FileInputStream(source).getChannel();
                FileChannel outChannel = new FileOutputStream(tmp).getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
                if (inChannel != null) inChannel.close();
                if (outChannel != null) outChannel.close();
                break;
        }
        for (int i = frames_.size() - 1; i >= index; i--) {
            Frame newFrame = new Frame(new File(Settings.getPropertyString(ConstantKeys.project_dir), formatFileName(i)));
            frames_.get(i).moveTo(newFrame);
            frames_.set(i, newFrame);
        }
        File newLocation = new File(Settings.getPropertyString(ConstantKeys.project_dir), formatFileName(index));
        tmp.renameTo(newLocation);
        Frame f = new Frame(newLocation);
        f.createThumbNail();
        frames_.set(index, f);
        return f;
    }

    public synchronized void insertFrames(int index, INSERT_TYPE type, File... sources) throws IOException {
        Frame[] frames = new Frame[sources.length];
        for (int i = sources.length - 1; i >= 0; i--) {
            File f = sources[i];
            try {
                Frame frame = insertFrame(index, f, type);
                notifyInsert(new FrameUpdateEvent(this, frame));
                frames[i] = frame;
            } catch (IOException e) {
            }
        }
        notifyUpdateFinished(new FrameUpdateEvent(this, frames));
    }

    /**
	 * Counts all registered frames
	 * @return how much images are saved
	 */
    public int getFrameCount() {
        return frames_.size();
    }

    /**
	 * Counts all registered frames
	 * @return how much images are saved
	 */
    public int getIndexOf(Frame f) {
        return frames_.indexOf(f);
    }

    /**
	 * Deletes the frame from drive
	 * @param index the index of the frame, you want to delete
	 */
    public synchronized Frame deleteFrame(int index) {
        if (index < 0) throw new IndexOutOfBoundsException("index < 0");
        if (index >= frames_.size()) throw new IndexOutOfBoundsException("index >= frames_.size()");
        Frame f = frames_.get(index);
        f.clearImage();
        f.clearThumb();
        f.getFile().delete();
        f.getThumbFile().delete();
        frames_.remove(index);
        for (int i = index; i < frames_.size(); i++) {
            Frame newFrame = new Frame(new File(Settings.getPropertyString(ConstantKeys.project_dir), formatFileName(i)));
            frames_.get(i).moveTo(newFrame);
            frames_.set(i, newFrame);
        }
        return f;
    }

    /**
	 * Deletes all images in the indexex array
	 * @param indexes
	 */
    public synchronized void deleteFrames(int... indexes) {
        Arrays.sort(indexes);
        Frame[] frames = new Frame[indexes.length];
        for (int i = indexes.length - 1; i >= 0; i--) {
            Frame frame = deleteFrame(indexes[i]);
            for (int j = indexes[i]; j < frames_.size(); j++) {
                Frame newFrame = new Frame(new File(Settings.getPropertyString(ConstantKeys.project_dir), formatFileName(j)));
                frames_.get(j).moveTo(newFrame);
                frames_.set(j, newFrame);
            }
            frames[i] = frame;
            notifyRemove(new FrameUpdateEvent(this, frame));
        }
        notifyUpdateFinished(new FrameUpdateEvent(this, frames));
    }

    /**
	 * Adds a listener to the list
	 * @param listener the listener, you want to add
	 */
    public void addFrameUpdateListener(FrameUpdateListener listener) {
        listeners.add(FrameUpdateListener.class, listener);
    }

    /**
	 * Removes a listener from the list
	 * @param listener a listener to remove
	 */
    public void removeFrameUpdateListener(FrameUpdateListener listener) {
        listeners.remove(FrameUpdateListener.class, listener);
    }

    /**
	 * Notifies all listeners by using {@code frameInserted()}
	 * @param event the event
	 */
    protected synchronized void notifyInsert(FrameUpdateEvent event) {
        for (FrameUpdateListener l : listeners.getListeners(FrameUpdateListener.class)) l.frameInserted(event);
    }

    /**
	 * Notifies all listeners by using {@code frameRemoved()}
	 * @param event the event
	 */
    protected synchronized void notifyRemove(FrameUpdateEvent event) {
        for (FrameUpdateListener l : listeners.getListeners(FrameUpdateListener.class)) l.frameRemoved(event);
    }

    /**
	 * Notifies all listeners that the update progress has finished
	 * @param event the event
	 */
    protected synchronized void notifyUpdateFinished(FrameUpdateEvent event) {
        for (FrameUpdateListener l : listeners.getListeners(FrameUpdateListener.class)) l.frameUpdateFinished(event);
    }

    /**
	 * Returns the filename for the next image.
	 * @return the next filename
	 */
    public File getNextFrame() {
        return new File(Settings.getPropertyString(ConstantKeys.project_dir), formatFileName(frames_.size()));
    }

    /**
	 * Returns the instance.
	 * @return the instance
	 */
    public static FrameManager getInstance() {
        return instance;
    }
}
