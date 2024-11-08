package com.aratana.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

/**
 * Utilidades para o manuseio de arquivos no sistema de arquivos local.
 * 
 * @author Dyorgio da Silva Nascimento.
 */
public class FileUtilities {

    @SuppressWarnings("serial")
    private static class InternalFileChooser extends JFileChooser {

        private class InternalFilePreviewPanel extends JPanel {

            public InternalFilePreviewPanel() {
                InternalFileChooser.this.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, new PropertyChangeListener() {

                    public void propertyChange(final PropertyChangeEvent ao_event) {
                        updatePreview();
                    }
                });
                setLayout(new BorderLayout());
                setPreferredSize(new Dimension(200, 200));
                setMinimumSize(getPreferredSize());
                setMaximumSize(getMinimumSize());
                setBorder(new TitledBorder("Preview"));
            }

            /**
			 * Atualiza o preview
			 */
            private void updatePreview() {
                removeAll();
                if (getSelectedFile() != null && getSelectedFile().exists()) {
                    final Iterator<ImageReader> lo_readers = ImageIO.getImageReadersBySuffix(getSelectedFile().getName().substring(getSelectedFile().getName().lastIndexOf('.') + 1).toLowerCase());
                    if (lo_readers.hasNext()) {
                        try {
                            final ImageReader io_read = lo_readers.next();
                            io_read.setInput(new FileImageInputStream(getSelectedFile()));
                            if (io_read.getNumImages(true) > 0) {
                                final Insets insets = getBorder().getBorderInsets(this);
                                final BufferedImage previewImage = io_read.read(io_read.getMinIndex());
                                int width = getWidth() < previewImage.getWidth() ? getWidth() : previewImage.getWidth(), height = getHeight() < previewImage.getHeight() ? getHeight() : previewImage.getHeight();
                                width = width > getWidth() - (insets.left + insets.right) ? getWidth() - (insets.left + insets.right) : width;
                                height = height > getHeight() - (insets.top + insets.bottom) ? getHeight() - (insets.top + insets.bottom) : height;
                                add(new JLabel(new ImageIcon(ImageUtilities.createProportionalImage(previewImage, width, height, true))));
                            }
                        } catch (final Exception e) {
                            Logger.getLogger(getClass().getName()).throwing(FileUtilities.class.getName(), "Image - Preview", e);
                        }
                    } else {
                        final byte[] lh_byte = new byte[512];
                        int ln_read = 1;
                        try {
                            ln_read = new FileInputStream(getSelectedFile()).read(lh_byte);
                            final String ls_preString = new String(lh_byte, 0, ln_read).trim();
                            if (ls_preString.length() > 0) {
                                add(new JTextArea(new String(lh_byte, 0, ln_read)) {

                                    {
                                        setEditable(false);
                                        setSelectionColor(getBackground());
                                        setSelectedTextColor(getForeground());
                                        setLineWrap(true);
                                        setBorder(new LineBorder(Color.BLACK));
                                    }
                                });
                            } else {
                                add(new JLabel(IconManager.getIcon(null)));
                            }
                        } catch (final Exception e) {
                            add(new JLabel(e.getMessage(), IconManager.getIcon(null), SwingConstants.CENTER) {

                                {
                                    setVerticalTextPosition(SwingConstants.BOTTOM);
                                    setHorizontalTextPosition(SwingConstants.CENTER);
                                }
                            });
                        }
                    }
                }
                validateTree();
                repaint();
            }
        }

        private class InternalFileView extends FileView {

            @Override
            public Icon getIcon(final File ao_file) {
                return getFileSystemView().getSystemIcon(ao_file);
            }
        }

        public InternalFileChooser(final File ao_initial_directory, final String as_title, final boolean ab_preview, final List<FileFilter> ao_filters, final boolean ab_all_option, final boolean directory) {
            super(ao_initial_directory);
            setDialogTitle(as_title);
            setFileSelectionMode(directory ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
            setFileView(new InternalFileView());
            setAcceptAllFileFilterUsed(false);
            if (ab_all_option || ao_filters == null || ao_filters.size() == 0) {
                setFileFilter(ALL_FILES_FILTER);
            }
            if (ao_filters != null && ao_filters.size() > 0) {
                if (!ab_all_option) {
                    setFileFilter(ao_filters.remove(0));
                }
                for (final FileFilter lo_filter : ao_filters) {
                    addChoosableFileFilter(lo_filter);
                }
            }
            if (ab_preview) {
                setAccessory(new InternalFilePreviewPanel());
            }
        }
    }

    ;

    /**
	 * Filtro que habilita todos os tipos de arquivos.
	 */
    public static FileFilter ALL_FILES_FILTER = new FileFilter() {

        @Override
        public boolean accept(final File ao_file) {
            return true;
        }

        @Override
        public String getDescription() {
            return "Todos os Arquivos (*.*)";
        }
    };

    private static HashSet<String> IMAGES_READERS_EXTENSIONS_HASH = new HashSet<String>();

    /**
	 * Filtro que habilita todos os tipos de imagens suportadas para a leitura
	 */
    public static FileFilter ALL_IMAGES_READER_FILTER = new FileFilter() {

        @Override
        public boolean accept(final File ao_file) {
            boolean lb_return = false;
            if (!(lb_return = ao_file.isDirectory())) {
                lb_return = IMAGES_READERS_EXTENSIONS_HASH.contains(ao_file.getName().substring(ao_file.getName().lastIndexOf('.') + 1).toLowerCase());
            }
            return lb_return;
        }

        @Override
        public String getDescription() {
            return "Images (" + IMAGES_READERS_EXTENSIONS_HASH.toString().substring(1, IMAGES_READERS_EXTENSIONS_HASH.toString().length() - 3).toUpperCase() + ")";
        }
    };

    /**
	 * Collection imutavel contendo as extens�es das imagens suportadas pelo
	 * sistema.
	 */
    public static Collection<String> IMAGES_READERS_EXTENSIONS = Collections.unmodifiableCollection(IMAGES_READERS_EXTENSIONS_HASH);

    private static HashSet<String> IMAGES_WRITERS_EXTENSIONS_HASH = new HashSet<String>();

    /**
	 * Filtro que habilita todos os tipos de imagens suportadas para a leitura
	 */
    public static FileFilter ALL_IMAGES_WRITER_FILTER = new FileFilter() {

        @Override
        public boolean accept(final File ao_file) {
            boolean lb_return = false;
            if (!(lb_return = ao_file.isDirectory())) {
                lb_return = IMAGES_WRITERS_EXTENSIONS_HASH.contains(ao_file.getName().substring(ao_file.getName().lastIndexOf('.') + 1).toLowerCase());
            }
            return lb_return;
        }

        @Override
        public String getDescription() {
            return "Images (" + IMAGES_WRITERS_EXTENSIONS_HASH.toString().substring(1, IMAGES_WRITERS_EXTENSIONS_HASH.toString().length() - 3).toUpperCase() + ")";
        }
    };

    /**
	 * Collection imutavel contendo as extens�es das imagens suportadas pelo
	 * sistema.
	 */
    public static Collection<String> IMAGES_WRITERS_EXTENSIONS = Collections.unmodifiableCollection(IMAGES_WRITERS_EXTENSIONS_HASH);

    static {
        for (final String ls_mine_type : ImageIO.getReaderMIMETypes()) {
            final Iterator<ImageReader> lo_readers = ImageIO.getImageReadersByMIMEType(ls_mine_type);
            while (lo_readers.hasNext()) {
                final String[] ls_extensions = lo_readers.next().getOriginatingProvider().getFileSuffixes();
                for (final String ls_extension : ls_extensions) {
                    IMAGES_READERS_EXTENSIONS_HASH.add(ls_extension.toLowerCase());
                }
            }
        }
    }

    static {
        for (final String ls_mine_type : ImageIO.getWriterMIMETypes()) {
            final Iterator<ImageWriter> lo_writers = ImageIO.getImageWritersByMIMEType(ls_mine_type);
            while (lo_writers.hasNext()) {
                final String[] ls_extensions = lo_writers.next().getOriginatingProvider().getFileSuffixes();
                for (final String ls_extension : ls_extensions) {
                    IMAGES_WRITERS_EXTENSIONS_HASH.add(ls_extension.toLowerCase());
                }
            }
        }
    }

    /**
	 * Escreve o conte�do de um array de <code>byte</code>s em um arquivo,
	 * adicionando ao do conte�do original.
	 * 
	 * @param ao_file
	 *            Arquivo a ser escrito.
	 * @param ah_data
	 *            Dados.
	 * @throws IOException
	 *             Caso ocorra algum problema na escrita.
	 * @throws FileNotFoundException
	 *             Caso o arquivo n�o exista.
	 */
    public static void appendFile(final File ao_file, final byte[] ah_data) throws IOException, FileNotFoundException {
        writeFile(ao_file, ah_data, true);
    }

    public static void concatFiles(final String as_base_file_name) throws IOException, FileNotFoundException {
        new File(as_base_file_name).createNewFile();
        final OutputStream lo_out = new FileOutputStream(as_base_file_name, true);
        int ln_part = 1, ln_readed = -1;
        final byte[] lh_buffer = new byte[32768];
        File lo_file = new File(as_base_file_name + "part1");
        while (lo_file.exists() && lo_file.isFile()) {
            final InputStream lo_input = new FileInputStream(lo_file);
            while ((ln_readed = lo_input.read(lh_buffer)) != -1) {
                lo_out.write(lh_buffer, 0, ln_readed);
            }
            ln_part++;
            lo_file = new File(as_base_file_name + "part" + ln_part);
        }
        lo_out.flush();
        lo_out.close();
    }

    /**
	 * recupera a extens�o de um arquivo
	 * 
	 * @param file
	 *            Arquivo a ser verificado
	 * @return A estens�o do arquivo em upperCase, <code>null</code> caso o
	 *         arquivo n�o possua extens�o.
	 */
    public static String getExtension(final File file) {
        String result = null;
        if (file.isFile()) {
            final int index = file.getName().lastIndexOf('.');
            if (index != -1) {
                result = file.getName().substring(index + 1).toUpperCase();
            }
        }
        return result;
    }

    /**
	 * L� o conte�do de um arquivo.
	 * 
	 * @param ao_file
	 *            Arquivo a ser lido.
	 * @return Um array de <code>byte<code>s contendo os dados do arquivo.
	 * @throws IOException
	 *             Caso ocorra algum problema na leitura.
	 * @throws FileNotFoundException
	 *             Caso o arquivo n�o exista.
	 */
    public static byte[] readFile(final File ao_file) throws IOException, FileNotFoundException {
        return readFile(ao_file, 0, -1);
    }

    /**
	 * L� o conte�do de um arquivo a partir de uma posi��o.
	 * 
	 * @param ao_file
	 *            Arquivo a ser lido.
	 * @param an_offset
	 *            Posi��o de inicio da leitura.
	 * @return Um array de <code>byte<code>s contendo os dados do arquivo.
	 * @throws IOException
	 *             Caso ocorra algum problema na leitura.
	 * @throws FileNotFoundException
	 *             Caso o arquivo n�o exista.
	 */
    public static byte[] readFile(final File ao_file, final long an_offset) throws IOException, FileNotFoundException {
        return readFile(ao_file, an_offset, -1);
    }

    /**
	 * L� o conte�do de um arquivo a partir de uma posi��o at� o tamanho
	 * especificado.
	 * 
	 * @param ao_file
	 *            Arquivo a ser lido.
	 * @param an_offset
	 *            Posi��o de inicio da leitura.
	 * @param an_length
	 *            Tamanho dos dados, -1 para ler at� o do arquivo.
	 * @return Um array de <code>byte<code>s contendo os dados do arquivo.
	 * @throws IOException
	 *             Caso ocorra algum problema na leitura.
	 * @throws FileNotFoundException
	 *             Caso o arquivo n�o exista.
	 */
    public static byte[] readFile(final File ao_file, final long an_offset, final long an_length) throws IOException, FileNotFoundException {
        byte[] lh_return = null;
        if (ao_file.exists() && ao_file.isFile()) {
            final FileInputStream lo_input = new FileInputStream(ao_file);
            try {
                final ByteBuffer lo_buffer = ByteBuffer.allocate(an_length > 0 ? (int) an_length : lo_input.available());
                final byte[] lh_buffer = new byte[32768];
                int ln_readed = -1, ln_total_readed = 0;
                lo_input.skip(an_offset);
                while (ln_total_readed < lo_buffer.limit() && (ln_readed = lo_input.read(lh_buffer, 0, (int) (ln_total_readed + 32768 < lo_buffer.limit() ? 32768 : lo_buffer.limit() - ln_total_readed))) != -1) {
                    lo_buffer.put(lh_buffer, 0, ln_readed);
                    ln_total_readed += ln_readed;
                }
                lh_return = new byte[ln_total_readed];
                lo_buffer.position(0);
                lo_buffer.get(lh_return);
            } finally {
                lo_input.close();
            }
        }
        return lh_return;
    }

    public static File selectDirectory(final File ao_initial_directory, final Component ao_parent, final String as_select_button_label, final String as_title) {
        final FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(final File pathname) {
                return pathname.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Diret�rios";
            }
        };
        final ArrayList<FileFilter> list = new ArrayList<FileFilter>();
        list.add(filter);
        return selectFile(ao_initial_directory, ao_parent, as_select_button_label, as_title, list, false, false, true);
    }

    public static File selectFile() {
        return selectFile(null, null, null, null);
    }

    public static File selectFile(final Component ao_parent, final String as_select_button_label, final String as_title, final List<FileFilter> ao_filters) {
        return selectFile(null, ao_parent, as_select_button_label, as_title, ao_filters, false, true);
    }

    public static File selectFile(final File ao_initial_directory, final Component ao_parent, final String as_select_button_label, final String as_title, final List<FileFilter> ao_filters, final boolean ab_all_option, final boolean ab_preview) {
        return selectFile(ao_initial_directory, ao_parent, as_select_button_label, as_title, ao_filters, ab_all_option, ab_preview, false);
    }

    private static File selectFile(final File ao_initial_directory, final Component ao_parent, final String as_select_button_label, final String as_title, final List<FileFilter> ao_filters, final boolean ab_all_option, final boolean ab_preview, final boolean directory) {
        File lo_selected = null;
        final InternalFileChooser lo_chooser = new InternalFileChooser(ao_initial_directory, as_title, ab_preview, ao_filters, ab_all_option, directory);
        if (lo_chooser.showDialog(ao_parent, as_select_button_label) == JFileChooser.APPROVE_OPTION) {
            lo_selected = lo_chooser.getSelectedFile();
        }
        return lo_selected;
    }

    public static void splitStreamToFiles(final InputStream ao_input, final String as_base_file_name, final long an_split_size) throws IOException, FileNotFoundException {
        long ln_position = 0;
        int ln_part = 1;
        int ln_readed = -1, ln_temp = 0;
        final byte[] lh_buffer = new byte[32768];
        OutputStream lo_out = new FileOutputStream(as_base_file_name + "part" + ln_part);
        while ((ln_readed = ao_input.read(lh_buffer)) != -1) {
            ln_position += ln_readed;
            do {
                if (ln_position > an_split_size) {
                    lo_out.write(lh_buffer, 0, ln_temp = ln_readed - (int) (ln_position - an_split_size));
                    lo_out.flush();
                    lo_out.close();
                    ln_part++;
                    lo_out = new FileOutputStream(as_base_file_name + "part" + ln_part);
                    lo_out.write(lh_buffer, ln_temp, ln_readed - ln_temp);
                    ln_position = ln_readed - ln_temp;
                } else {
                    lo_out.write(lh_buffer, 0, ln_readed);
                }
            } while (ln_position > an_split_size);
        }
        lo_out.flush();
        lo_out.close();
    }

    /**
	 * Escreve o conte�do de um array de <code>byte</code>s em um arquivo,
	 * sobreescrevendo o conte�do original.
	 * 
	 * @param ao_file
	 *            Arquivo a ser escrito.
	 * @param ah_data
	 *            Dados.
	 * @throws IOException
	 *             Caso ocorra algum problema na escrita.
	 * @throws FileNotFoundException
	 *             Caso o arquivo n�o exista.
	 */
    public static void writeFile(final File ao_file, final byte[] ah_data) throws IOException, FileNotFoundException {
        writeFile(ao_file, ah_data, false);
    }

    /**
	 * Escreve o conte�do de um array de <code>byte</code>s em um arquivo.
	 * 
	 * @param ao_file
	 *            Arquivo a ser escrito.
	 * @param ah_data
	 *            Dados.
	 * @param ab_append
	 *            Indica se os dados ser�o adicionados no do arquivo.
	 * @throws IOException
	 *             Caso ocorra algum problema na escrita.
	 * @throws FileNotFoundException
	 *             Caso o arquivo n�o exista.
	 */
    public static void writeFile(final File ao_file, final byte[] ah_data, final boolean ab_append) throws IOException, FileNotFoundException {
        if (ao_file.exists() && ao_file.isFile()) {
            final FileOutputStream lo_output = new FileOutputStream(ao_file, ab_append);
            lo_output.getChannel().write(ByteBuffer.wrap(ah_data));
            lo_output.getChannel().close();
        }
    }

    public static boolean isValidDirPath(String path) {
        try {
            File file = new File(path);
            return file.exists() && file.isDirectory();
        } catch (Exception e) {
            return false;
        }
    }

    private FileUtilities() {
    }
}
