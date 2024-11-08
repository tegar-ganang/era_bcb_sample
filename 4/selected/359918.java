package spdrender;

import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JOptionPane;

/**
 * Ray tracer framebuffer visualizer.
 * @author  Maximiliano Monterrubio Gutierrez.
 */
public class FrameBufferWindow extends javax.swing.JFrame {

    private int w, h;

    private double fb[][][];

    private BufferedImage img;

    public BufferUpdater bufferUpdater;

    /** Creates new form FrameBuffer
     * @param width Width of the framebuffer in pixels.
     * @param height Height of the framebuffer in pixels;
     */
    public FrameBufferWindow(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Framebuffer size must be at least 1 pixel long");
        }
        w = width;
        h = height;
        fb = new double[w][h][3];
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                fb[i][j][0] = fb[i][j][1] = fb[i][j][2] = 0.0f;
            }
        }
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        initComponents();
        javax.swing.ImageIcon ii = new javax.swing.ImageIcon(img);
        jLabel5.setIcon(ii);
        jPanel2.repaint();
        jLabel1.setDoubleBuffered(false);
        bufferUpdater = new BufferUpdater();
        bufferUpdater.start();
    }

    public double[][][] getFrameBuffer() {
        return fb;
    }

    public BufferedImage getRender() {
        return img;
    }

    /** Overwrites a section of the framebuffer.
     * 
     * @param section A float tridimensional array that represents the new section to update in the framebuffer.  The last coordinate
     * represents de R, G and B values of the color to se.
     * @param x The x position to start overwriting.
     * @param y The y position to start overwriting.
     * @param w The width of the overwriting section.
     * @param h The height of the overwriting section.
     */
    public void writeSection(double[][][] section, int x, int y, int w, int h) {
        BufferUpdateEvent bue = new BufferUpdateEvent(section, x, y, w, h);
        bufferUpdater.enqueue(bue);
    }

    /** An event to signal that we should update a portion of the buffer
     *
     * @author Luis Hector Chavez
     */
    public class BufferUpdateEvent {

        public double[][][] section;

        public int x;

        public int y;

        public int w;

        public int h;

        boolean finish;

        /** An event that signals a pending section of the framebuffer.
         *
         * @param section A float tridimensional array that represents the new section to update in the framebuffer.  The last coordinate
         * represents de R, G and B values of the color to se.
         * @param x The x position to start overwriting.
         * @param y The y position to start overwriting.
         * @param w The width of the overwriting section.
         * @param h The height of the overwriting section.
         */
        public BufferUpdateEvent(double[][][] sect, int x, int y, int w, int h) {
            this.section = sect;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            finish = false;
        }

        /** A fake event that signals that this is the last event of the queue.
         *
         */
        public BufferUpdateEvent() {
            finish = true;
        }
    }

    /** A class that serves as the only thread that updates the image buffer
     *
     * @author Luis Hector Chavez
     */
    public class BufferUpdater extends Thread {

        private LinkedBlockingQueue<BufferUpdateEvent> q;

        /** The constructor of the buffer updater
         *
         */
        public BufferUpdater() {
            q = new LinkedBlockingQueue<BufferUpdateEvent>();
        }

        /** Adds an update event to the queue
         *
         * @param e The event to be queued.
         */
        public void enqueue(BufferUpdateEvent e) {
            try {
                q.put(e);
            } catch (InterruptedException ex) {
            }
        }

        /** Adds a special terminating update that signals this thread to silently die
         *
         */
        public void terminate() {
            enqueue(new BufferUpdateEvent());
        }

        /** The running loop for the thread
         *
         */
        @Override
        public void run() {
            try {
                while (true) {
                    BufferUpdateEvent e = q.take();
                    if (e.finish) break;
                    for (int i = 0; i < e.w; ++i) {
                        for (int j = 0; j < e.h; ++j) {
                            fb[i + e.x][j + e.y][0] = e.section[i][j][0];
                            fb[i + e.x][j + e.y][1] = e.section[i][j][1];
                            fb[i + e.x][j + e.y][2] = e.section[i][j][2];
                            int rgb;
                            rgb = ((int) (fb[e.x + i][e.y + j][0] * 255)) & 0xFF;
                            rgb <<= 8;
                            rgb |= ((int) (fb[e.x + i][e.y + j][1] * 255)) & 0xFF;
                            rgb <<= 8;
                            rgb |= ((int) (fb[e.x + i][e.y + j][2] * 255)) & 0xFF;
                            img.setRGB(i + e.x, e.y + j, rgb);
                        }
                    }
                    jLabel5.imageUpdate(img, ImageObserver.SOMEBITS, e.x, e.y, e.w, e.h);
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        widthLabel = new javax.swing.JLabel();
        heightLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        threadLabel = new javax.swing.JLabel();
        timeLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jMenuItem2 = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(spdrender.SPDRenderApp.class).getContext().getResourceMap(FrameBufferWindow.class);
        setTitle(resourceMap.getString("Form.title"));
        setName("Form");
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });
        jPanel1.setName("jPanel1");
        jLabel1.setFont(resourceMap.getFont("jLabel1.font"));
        jLabel1.setText(resourceMap.getString("jLabel1.text"));
        jLabel1.setName("jLabel1");
        jLabel2.setFont(resourceMap.getFont("jLabel2.font"));
        jLabel2.setText(resourceMap.getString("jLabel2.text"));
        jLabel2.setName("jLabel2");
        widthLabel.setText(resourceMap.getString("widthLabel.text"));
        widthLabel.setName("widthLabel");
        heightLabel.setText(resourceMap.getString("heightLabel.text"));
        heightLabel.setName("heightLabel");
        jLabel3.setFont(resourceMap.getFont("jLabel3.font"));
        jLabel3.setText(resourceMap.getString("jLabel3.text"));
        jLabel3.setName("jLabel3");
        jLabel4.setFont(resourceMap.getFont("jLabel4.font"));
        jLabel4.setText(resourceMap.getString("jLabel4.text"));
        jLabel4.setName("jLabel4");
        threadLabel.setText(resourceMap.getString("threadLabel.text"));
        threadLabel.setName("threadLabel");
        timeLabel.setText(resourceMap.getString("timeLabel.text"));
        timeLabel.setName("timeLabel");
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jLabel1).addGap(12, 12, 12).addComponent(widthLabel)).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jLabel2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(heightLabel))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 510, Short.MAX_VALUE).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jLabel4).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(timeLabel)).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jLabel3).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(threadLabel))).addGap(233, 233, 233)));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel1).addComponent(widthLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel2).addComponent(heightLabel))).addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel3).addComponent(threadLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel4).addComponent(timeLabel)))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_END);
        jPanel2.setName("jPanel2");
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText(resourceMap.getString("jLabel5.text"));
        jLabel5.setDoubleBuffered(true);
        jLabel5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel5.setName("jLabel5");
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 990, Short.MAX_VALUE));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE));
        getContentPane().add(jPanel2, java.awt.BorderLayout.PAGE_START);
        jMenuBar1.setName("jMenuBar1");
        jMenu1.setMnemonic('f');
        jMenu1.setText(resourceMap.getString("jMenu1.text"));
        jMenu1.setName("jMenu1");
        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setMnemonic('s');
        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text"));
        jMenuItem1.setName("jMenuItem1");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);
        jSeparator1.setName("jSeparator1");
        jMenu1.add(jSeparator1);
        jMenuItem2.setMnemonic('x');
        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text"));
        jMenuItem2.setName("jMenuItem2");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenuBar1.add(jMenu1);
        setJMenuBar(jMenuBar1);
        pack();
    }

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {
        this.setVisible(false);
        this.dispose();
    }

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {
        FileNameExtensionFilter fef = new FileNameExtensionFilter("Portable Network Graphics (PNG)", "png");
        JFileChooser jfc = new JFileChooser();
        jfc.setFileFilter(fef);
        int ret = jfc.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();
            int ow = JOptionPane.OK_OPTION;
            if (f.exists()) {
                ow = JOptionPane.showConfirmDialog(this, "The file " + f.getName() + " already exists.\nDo you want to overwrite it?");
            }
            if (ow == JOptionPane.OK_OPTION) {
                try {
                    ImageIO.write(img, "png", f);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Couldn't write to: " + f.getName() + "\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        bufferUpdater.terminate();
        Runtime.getRuntime().gc();
    }

    public javax.swing.JLabel heightLabel;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JMenu jMenu1;

    private javax.swing.JMenuBar jMenuBar1;

    private javax.swing.JMenuItem jMenuItem1;

    private javax.swing.JMenuItem jMenuItem2;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JSeparator jSeparator1;

    public javax.swing.JLabel threadLabel;

    public javax.swing.JLabel timeLabel;

    public javax.swing.JLabel widthLabel;
}
