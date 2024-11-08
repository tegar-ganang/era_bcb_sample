package view.visualization.image;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import model.datapath.AbstractDataPathElement;
import model.datapath.DataPath;
import view.datapath.element.Factory;
import view.visualization.Visualization;

/**
 * A Visualization that displays two images.
 * 
 * @author gijs
 */
public class ImageVisualization extends Observable implements Visualization {

    public static final String DEFAULT_IMAGE_FILE = "duke.gif";

    private Factory<DataPath>[] factories = createFactories();

    private BufferedImage image;

    private final ImageVisualizationComponent component;

    public ImageVisualization() {
        try {
            InputStream input = getClass().getClassLoader().getResourceAsStream(DEFAULT_IMAGE_FILE);
            ImageInputStream imageInput = ImageIO.createImageInputStream(input);
            ImageVisualization.this.loadImage(imageInput, new ExceptionHandler<IOException>() {

                public void handle(IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        component = new ImageVisualizationComponent();
    }

    public String getName() {
        return "Image Visualization";
    }

    public JComponent getComponent() {
        return component;
    }

    @SuppressWarnings("unchecked")
    private Factory<DataPath>[] createFactories() {
        return new Factory[0];
    }

    private void loadImage(ImageInputStream input, final ExceptionHandler<IOException> handler) {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        if (readers == null) {
            handler.handle(new IOException("Can't find suitable ImageInputStreamSpi for object: " + input));
        }
        final ImageReader ir = ImageIO.getImageReaders(input).next();
        ir.setInput(input, true, false);
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    setImage(ir.read(0));
                } catch (IOException e) {
                    handler.handle(e);
                }
            }
        });
    }

    private void setImage(BufferedImage newImage) {
        this.image = newImage;
        setChanged();
        notifyObservers();
    }

    private class ImageVisualizationComponent extends JComponent implements Observer {

        private static final long serialVersionUID = 1L;

        private boolean scaleImages = true;

        private ImageComponentComponent imageComponentComponent;

        private JPanel bottom;

        private JButton pauseButton;

        private boolean paused;

        private JCheckBox scaleBox;

        private WriteThread writeThread;

        private FactoryUpdateThread currentUpdate, nextUpdate;

        private boolean continuus;

        ImageVisualizationComponent() {
            addObserver(this);
            JPanel top = new JPanel();
            top.setLayout(new GridBagLayout());
            bottom = new JPanel() {

                private static final long serialVersionUID = 1L;

                @Override
                public void paint(Graphics g) {
                    Component[] components = getComponents();
                    for (Component c : components) {
                        c.paint(g);
                    }
                }
            };
            bottom.setLayout(new GridBagLayout());
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            c.weightx = 1.0;
            c.weighty = 0.0;
            add(top, c);
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1.0;
            add(bottom, c);
            addButtons(top);
            update(null, null);
        }

        private void addButtons(JPanel panel) {
            pauseButton = new JButton("Start");
            pauseButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (imageComponentComponent != null) {
                        setPaused(!paused);
                    }
                }
            });
            JButton fileB = new JButton("File...");
            fileB.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(new FileFilter() {

                        @Override
                        public boolean accept(File f) {
                            if (f.isDirectory()) {
                                return true;
                            }
                            String path = f.getPath();
                            int eIndex = path.lastIndexOf('.');
                            if (eIndex == -1) {
                                return false;
                            }
                            String extension = path.substring(eIndex + 1, path.length());
                            for (String s : new String[] { "jpg", "gif", "png" }) {
                                if (s.equalsIgnoreCase(extension)) {
                                    return true;
                                }
                            }
                            return false;
                        }

                        @Override
                        public String getDescription() {
                            return "Images (jpg, gif, png)";
                        }
                    });
                    int returnVal = chooser.showOpenDialog(ImageVisualizationComponent.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        loadImage(chooser.getSelectedFile());
                    }
                }
            });
            scaleBox = new JCheckBox("Scale images", scaleImages);
            scaleBox.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    setScale(scaleBox.isSelected());
                }
            });
            final JCheckBox continuusBox = new JCheckBox("Continuus");
            continuusBox.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    setContinuus(continuusBox.isSelected());
                }
            });
            panel.add(pauseButton);
            panel.add(fileB);
            panel.add(scaleBox);
            panel.add(continuusBox);
            setPaused(true);
        }

        private void loadImage(File file) {
            try {
                ImageInputStream imageInput = ImageIO.createImageInputStream(file);
                ImageVisualization.this.loadImage(imageInput, new ExceptionHandler<IOException>() {

                    public void handle(IOException e) {
                        JOptionPane.showMessageDialog(ImageVisualizationComponent.this, "IOException: " + e.getMessage());
                    }
                });
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "IOException: " + e.getMessage());
            }
        }

        private void setScale(boolean scale) {
            scaleImages = scale;
            if (imageComponentComponent != null) {
                imageComponentComponent.myComponent.setScaleImages(scale);
            }
            setChanged();
            notifyObservers();
        }

        public synchronized void update(Observable o, Object arg) {
            if (image == null || factories.length == 0) return;
            if (writeThread != null) {
                writeThread.stop = true;
                writeThread.interrupt();
                try {
                    writeThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (continuus && imageComponentComponent != null && factories.length == imageComponentComponent.sinks.length && image == imageComponentComponent.source.getImage()) {
                imageComponentComponent = new ImageComponentComponent(imageComponentComponent);
            } else {
                imageComponentComponent = new ImageComponentComponent();
            }
            setPaused(paused);
            writeThread = new WriteThread(imageComponentComponent.source);
            writeThread.start();
            revalidate();
        }

        private class WriteThread extends Thread {

            private ImageSource mysource;

            private boolean stop;

            public WriteThread(ImageSource theSource) {
                assert theSource != null;
                this.mysource = theSource;
            }

            @Override
            public void run() {
                while (mysource.writeNext()) {
                    if (stop) {
                        return;
                    }
                    while (paused) {
                        try {
                            synchronized (this) {
                                wait();
                            }
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
                if (continuus) {
                    new Thread() {

                        @Override
                        public void run() {
                            update(null, null);
                        }
                    }.start();
                }
            }
        }

        private synchronized void setPaused(boolean paused) {
            if (imageComponentComponent == null) return;
            this.paused = paused;
            if (paused) {
                pauseButton.setText("Resume");
            } else {
                pauseButton.setText("Pause");
                if (writeThread != null) {
                    synchronized (writeThread) {
                        writeThread.notify();
                    }
                }
            }
        }

        private synchronized void setContinuus(boolean continuus) {
            this.continuus = continuus;
            update(null, null);
        }

        private class MyComponent extends JComponent {

            private static final long serialVersionUID = 1L;

            private ImageComponent sourceComponent;

            private ImageComponent[] sinkComponents;

            MyComponent(ImageSource source, ImageSink[] sinks) {
                sourceComponent = createSourceComponent(source);
                sinkComponents = new ImageComponent[sinks.length];
                ;
                setLayout(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();
                c.anchor = GridBagConstraints.WEST;
                c.gridy = 0;
                c.weightx = 1.0;
                c.fill = GridBagConstraints.BOTH;
                add(new JLabel("Source image:"));
                for (int i = 0; i < sinks.length; i++) {
                    add(new JLabel("Sink image " + (i + 1) + ":"));
                }
                for (int i = 0; i < sinkComponents.length; i++) {
                    if (i < sinks.length) {
                        sinkComponents[i] = new ImageComponent(sinks[i]);
                    } else {
                        sinkComponents[i] = new ImageComponent(sinks[0]);
                        sinkComponents[i].setVisible(false);
                    }
                }
                c.gridy++;
                c.weighty = 1.0;
                setScaleImages(scaleImages);
                add(sourceComponent, c);
                for (ImageComponent sinkComponent : sinkComponents) {
                    add(sinkComponent, c);
                }
            }

            private ImageComponent createSourceComponent(ImageSource source) {
                ImageComponent result = new ImageComponent(source);
                result.setScaleImage(scaleImages);
                return result;
            }

            public void setScaleImages(boolean scale) {
                sourceComponent.setScaleImage(scale);
                for (ImageComponent i : sinkComponents) {
                    i.setScaleImage(scale);
                }
            }

            public void setObserved(ImageSink[] sinks) {
                boolean validate = false;
                for (int i = 0; i < sinks.length; i++) {
                    sinkComponents[i].setSink(sinks[i]);
                    sinkComponents[i].setScaleImage(scaleImages);
                    if (!sinkComponents[i].isVisible()) {
                        sinkComponents[i].setVisible(true);
                        validate = true;
                    }
                }
                for (int i = sinks.length; i < sinkComponents.length; i++) {
                    if (sinkComponents[i].isVisible()) {
                        sinkComponents[i].setVisible(false);
                        validate = true;
                    }
                }
                setScaleImages(scaleImages);
                if (validate) {
                    validate();
                }
            }
        }

        private class ImageComponentComponent {

            private final ImageSource source;

            private final ImageSink[] sinks;

            private final MyComponent myComponent;

            ImageComponentComponent() {
                source = new ImageSource(image);
                sinks = new ImageSink[factories.length];
                for (int i = 0; i < factories.length; i++) {
                    sinks[i] = new ImageSink(source.getImage().getWidth(), source.getImage().getHeight());
                }
                myComponent = new MyComponent(source, sinks);
                init();
                if (imageComponentComponent != null) {
                    bottom.remove(imageComponentComponent.myComponent);
                }
                GridBagConstraints c = new GridBagConstraints();
                c.weightx = 1.0;
                c.weighty = 1.0;
                c.fill = GridBagConstraints.BOTH;
                bottom.add(myComponent, c);
            }

            ImageComponentComponent(ImageComponentComponent old) {
                sinks = new ImageSink[old.sinks.length];
                Position p = old.sinks[0].getPosition();
                for (ImageSink sink : old.sinks) {
                    if (sink.getPosition().compareTo(p) < 0) {
                        p = sink.getPosition();
                    }
                }
                if (old.source.isLastPosition(p)) {
                    p = new Position(0, 0, 0);
                }
                source = new ImageSource(image, p);
                for (int i = 0; i < sinks.length; i++) {
                    sinks[i] = new ImageSink(old.sinks[i], p);
                }
                myComponent = old.myComponent;
                myComponent.setObserved(sinks);
                init();
            }

            private void init() {
                final DataPath[] datapaths = new DataPath[factories.length];
                for (int i = 0; i < factories.length; i++) {
                    datapaths[i] = factories[i].create();
                    datapaths[i].setNext(sinks[i]);
                }
                source.setNext(new AbstractDataPathElement<Boolean, Boolean>() {

                    public synchronized void write(Boolean data) {
                        for (DataPath path : datapaths) {
                            path.write(data);
                        }
                    }

                    @Override
                    public synchronized void wroteLast() {
                        for (DataPath path : datapaths) {
                            path.wroteLast();
                        }
                    }
                });
            }
        }
    }

    private interface ExceptionHandler<T extends Exception> {

        public void handle(T exception);
    }

    public synchronized void setFactories(Factory<DataPath> factory1, Factory<DataPath> factory2) {
        if (component == null) {
            setFactoriesPrivate(factory1, factory2);
        } else {
            if (component.currentUpdate == null) {
                component.currentUpdate = new FactoryUpdateThread(factory1, factory2);
                component.currentUpdate.start();
            } else {
                component.nextUpdate = new FactoryUpdateThread(factory1, factory2);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setFactoriesPrivate(Factory<DataPath> factory1, Factory<DataPath> factory2) {
        if (factory2 == null) {
            this.factories = new Factory[] { factory1 };
        } else {
            this.factories = new Factory[] { factory1, factory2 };
        }
        setChanged();
        notifyObservers();
    }

    private class FactoryUpdateThread extends Thread {

        private Factory<DataPath> f1, f2;

        public FactoryUpdateThread(Factory<DataPath> factory1, Factory<DataPath> factory2) {
            f1 = factory1;
            f2 = factory2;
        }

        public void run() {
            setFactoriesPrivate(f1, f2);
            component.currentUpdate = component.nextUpdate;
            if (component.currentUpdate != null) {
                component.nextUpdate = null;
                component.currentUpdate.start();
            }
        }
    }
}
