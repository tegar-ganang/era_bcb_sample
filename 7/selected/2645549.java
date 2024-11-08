package org.designerator.color.core;

import org.designerator.color.ColorPlugin;
import org.designerator.color.Prefs;
import org.designerator.color.interfaces.IColorManager;
import org.designerator.color.interfaces.IProxyColor;
import imagefp.color.ColorAndGradientData;
import imagefp.color.GradientData;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.designerator.color.dialog.HSBColorDialog;
import org.designerator.color.palette.PaletteLoader;
import org.designerator.color.parser.Aco;
import org.designerator.color.parser.GimpPaletteReader;
import org.designerator.color.utils.ColorUtils;
import org.designerator.color.utils.ProxyColor;
import org.designerator.color.utils.IOUtils;
import org.designerator.color.views.ColorView;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

public class ColorOrganizer {

    private static List<Color> extraColors = new ArrayList<Color>();

    public static void addExtraColor(Color c) {
        extraColors.add(c);
    }

    private static DisposeListener DisposeListener() {
        return null;
    }

    public static void main(String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setLayout(new GridLayout(1, false));
        ToolBar tb = new ToolBar(shell, SWT.FLAT);
        ColorOrganizer co = new ColorOrganizer();
        co.createControl(shell, false);
        new ColorOrganizerActions().createToolBarItems(tb, co, true);
        shell.setSize(300, 400);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        co.dispose();
        display.dispose();
    }

    protected Color background = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);

    private Canvas colorContainer;

    int colorHeight = 14;

    Label colorLabel;

    private IProxyColor[] colors;

    private int columns = 16;

    protected String currentDir = null;

    protected boolean drag;

    protected boolean editMode;

    private GradientData[] gradientDatas;

    private List<IColorManager> graphicsManagerListener;

    private List<ISelectionChangedListener> listeners;

    private Composite main;

    private String paletteName;

    private ScrolledComposite scrollComposite;

    ColorItem selectedCanvas;

    protected int selectedIndex;

    protected Canvas selItem;

    private Color currentColor;

    ISelectionProvider parent;

    private ColorItemMouseListener itemListener;

    protected int copyMode = 0;

    private boolean isDirty;

    protected boolean isDragging;

    public ColorOrganizer() {
    }

    public ColorOrganizer(Composite parent) {
        createControl(parent, true);
    }

    public ColorOrganizer(ISelectionProvider view) {
        parent = view;
    }

    public void addColor(Color color) {
        if (color == null) {
            return;
        }
        final ProxyColor pcolor = new ProxyColor(color, colors.length);
        addToProxyColorsAndItems(pcolor);
        isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public void addColor(RGB rgb) {
        if (rgb == null) {
            return;
        }
        if (colors[colors.length - 1].getRGB().equals(rgb)) {
            return;
        }
        final ProxyColor pcolor = new ProxyColor(main.getDisplay(), rgb, colors.length);
        addToProxyColorsAndItems(pcolor);
        isDirty = true;
    }

    public void addGradient(GradientData gradientData) {
        if (gradientData == null) {
            return;
        }
        addToGradientDatas(gradientData);
        isDirty = true;
    }

    public void addItems(GradientData[] gradientData) {
        final boolean missing = false;
        for (int i = 0; i < gradientData.length; i++) {
            if (gradientData[i] != null) {
                new ColorItem(colorContainer, this, SWT.NO_BACKGROUND, colors.length + i, gradientData[i]);
            }
        }
        colorContainer.layout(true, true);
    }

    public void addItems(IProxyColor[] colors2) {
        for (int i = 0; i < colors2.length; i++) {
            new ColorItem(colorContainer, this, SWT.NO_BACKGROUND, i, colors2[i]);
        }
        colorContainer.layout(true, true);
    }

    public void addItemToPalette(int index, IProxyColor color) {
        final ColorItem ci = new ColorItem(colorContainer, this, SWT.NO_BACKGROUND, index, color);
        if (gradientDatas != null) {
            final Control[] children = colorContainer.getChildren();
            ci.moveBelow(children[children.length - gradientDatas.length - 2]);
            colorContainer.layout(new Control[] { ci });
            return;
        }
        colorContainer.layout(true, true);
    }

    public void addListener(IColorManager gm) {
        if (gm != null) {
            if (graphicsManagerListener == null) {
                graphicsManagerListener = new ArrayList();
            }
            graphicsManagerListener.add(gm);
        }
    }

    public void addListener(ISelectionChangedListener listener) {
        if (listener != null) {
            if (listeners == null) {
                listeners = new ArrayList<ISelectionChangedListener>();
            }
            listeners.add(listener);
        }
    }

    private void addToGradientDatas(GradientData gradientData) {
        if ((gradientData) == null) {
            return;
        }
        GradientData[] gradientDatas2 = null;
        if (gradientDatas == null) {
            gradientDatas = new GradientData[1];
            gradientDatas[0] = gradientData;
        } else {
            if (gradientDatas[gradientDatas.length - 1].equals(gradientData)) {
                return;
            }
            gradientDatas2 = new GradientData[gradientDatas.length + 1];
            for (int i = 0; i < gradientDatas.length; i++) {
                gradientDatas2[i] = gradientDatas[i];
            }
            gradientDatas2[gradientDatas2.length - 1] = gradientData;
            gradientDatas = gradientDatas2;
        }
        new ColorItem(colorContainer, this, SWT.NO_BACKGROUND, colors.length + gradientDatas.length, gradientData);
        colorContainer.layout(true, true);
    }

    private void addToProxyColorsAndItems(ProxyColor color) {
        final IProxyColor[] colors2 = new ProxyColor[colors.length + 1];
        for (int i = 0; i < colors.length; i++) {
            colors2[i] = colors[i];
        }
        colors2[colors2.length - 1] = color;
        colors = colors2;
        addItemToPalette(colors.length - 1, colors[colors.length - 1]);
    }

    private void adjustColorContainerSize() {
    }

    private void convertToColorAndGradient(Object object, String palette) {
        final Display display = main.getDisplay();
        final ColorAndGradientData cad = (ColorAndGradientData) object;
        colorHeight = cad.colorHeight;
        columns = cad.cols;
        if (ColorPlugin.isPluginActive() && (palette != null)) {
            final IPreferenceStore store = ColorPlugin.getDefault().getPreferenceStore();
            if (store != null) {
                final int h = store.getInt(palette + "h");
                final int c = store.getInt(palette + "c");
                if (h > 0) {
                    colorHeight = h;
                }
                if (c > 0) {
                    columns = c;
                }
            }
        } else {
            colorHeight = 14;
            columns = 16;
        }
        if (cad.colors != null) {
            colors = new ProxyColor[cad.colors.length];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = new ProxyColor(display, cad.colors[i], i);
            }
        }
        if (cad.gradients != null) {
            final Object[] ogd = removeNullValues(cad.gradients);
            final GradientData[] gd = new GradientData[ogd.length];
            for (int i = 0; i < gd.length; i++) {
                gd[i] = (GradientData) ogd[i];
            }
            gradientDatas = gd;
        }
    }

    private void createColorCanvas(SashForm sashForm) {
        final Canvas colorMixer = new Canvas(sashForm, SWT.NONE);
    }

    private void createColorItems(final Composite sashForm) {
    }

    private void createContextMenu(final MouseEvent event, final Canvas parent) {
        if (parent != null) {
            final Menu contextmenu = new Menu(parent);
            ColorOrganizerActions.fillEditMenu(contextmenu, this, true);
            final Point p = parent.toDisplay(event.x, event.y);
            if (p == null) {
                contextmenu.dispose();
                return;
            }
            contextmenu.setLocation(p.x, p.y);
            contextmenu.setVisible(true);
            final Display display = parent.getDisplay();
            while (!contextmenu.isDisposed() && contextmenu.isVisible()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            contextmenu.dispose();
        }
    }

    public void createContextMenuItems(final MouseEvent event, final ColorItem parent) {
        if (parent != null) {
            final Menu contextmenu = new Menu(parent);
            if (parent.getColor() != null) {
                final MenuItem getRgb = new MenuItem(contextmenu, SWT.PUSH);
                getRgb.setText("Copy SWT RGB");
                getRgb.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        ColorUtils.copyToClipBoard(ColorUtils.getSWTColorString(parent.getColor().getRGB()), contextmenu.getDisplay());
                        copyMode = 0;
                    }
                });
                final MenuItem awt = new MenuItem(contextmenu, SWT.PUSH);
                awt.setText("Copy AWT Color");
                awt.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        String r = ColorUtils.getAWTColorString(parent.getColor().getRGB());
                        ColorUtils.copyToClipBoard(r, contextmenu.getDisplay());
                        copyMode = 1;
                    }
                });
                final MenuItem css = new MenuItem(contextmenu, SWT.PUSH);
                css.setText("Copy Css Rgb");
                css.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        String r = ColorUtils.getCssColorString(parent.getColor().getRGB());
                        ColorUtils.copyToClipBoard(r, contextmenu.getDisplay());
                        copyMode = 2;
                    }
                });
                final MenuItem hash = new MenuItem(contextmenu, SWT.PUSH);
                hash.setText("Copy Hex Color Value");
                hash.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        String h = ColorUtils.getHexColorString(parent.getColor().getRGB());
                        ColorUtils.copyToClipBoard(h, contextmenu.getDisplay());
                        copyMode = 3;
                    }
                });
                new MenuItem(contextmenu, SWT.SEPARATOR);
                final MenuItem createColor = new MenuItem(contextmenu, SWT.PUSH);
                createColor.setText("Modify Color");
                createColor.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        final ColorItem colorItem = parent;
                        modifySelectedcolor(colorItem);
                    }
                });
            } else {
                final MenuItem hash = new MenuItem(contextmenu, SWT.PUSH);
                hash.setText("Copy GradientData");
                hash.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        String h = parent.getGradientData().toString();
                        ColorUtils.copyToClipBoard(h, contextmenu.getDisplay());
                    }
                });
                new MenuItem(contextmenu, SWT.SEPARATOR);
                final MenuItem modifyGradient = new MenuItem(contextmenu, SWT.PUSH);
                modifyGradient.setText("Modify Gradient");
                modifyGradient.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event e) {
                        final GradientData gd = parent.gradientData.clone();
                        final Shell shell = ColorPlugin.getShell();
                        final HSBColorDialog colorDialog = new HSBColorDialog(shell, false);
                        colorDialog.setColorOrganizer(ColorOrganizer.this);
                        colorDialog.setGradient(shell.getDisplay(), gd);
                        final int code = colorDialog.open();
                        if (code == Window.OK) {
                            parent.updateGradient(colorDialog.getGradient());
                            parent.redraw();
                        }
                    }
                });
            }
            final MenuItem deleteItem = new MenuItem(contextmenu, SWT.PUSH);
            deleteItem.setText("Delete");
            deleteItem.addListener(SWT.Selection, new Listener() {

                public void handleEvent(Event e) {
                    final ColorItem colorItem = parent;
                    delete(colorItem);
                }
            });
            final Point p = parent.toDisplay(event.x, event.y);
            if (p == null) {
                contextmenu.dispose();
                return;
            }
            contextmenu.setLocation(p.x, p.y);
            contextmenu.setVisible(true);
            final Display display = parent.getDisplay();
            while (!contextmenu.isDisposed() && contextmenu.isVisible()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            contextmenu.dispose();
        }
    }

    public void createControl(Composite parent, boolean createSideBar) {
        main = new Composite(parent, SWT.None);
        final GridData gridData0 = new GridData(SWT.FILL, SWT.FILL, true, false);
        main.setLayoutData(gridData0);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        if (createSideBar) {
            gridLayout.numColumns = 2;
        }
        main.setLayout(gridLayout);
        createpaletteComposite(main);
        if (createSideBar) {
            final Composite sideBar = new Composite(main, SWT.NONE);
            final GridData layoutData = new GridData(SWT.FILL, SWT.FILL, false, true);
            sideBar.setLayoutData(layoutData);
            final GridLayout layout = new GridLayout();
            sideBar.setLayout(layout);
            sideBar.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_BLUE));
            final ToolBar toolBar = new ToolBar(sideBar, SWT.VERTICAL | SWT.FLAT);
            toolBar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
            new ColorOrganizerActions().createToolBarItems(toolBar, this, true);
            colorLabel = new Label(sideBar, SWT.NONE);
            colorLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        }
        setBackGround();
        itemListener = new ColorItemMouseListener(this);
        currentDir = Prefs.getString(Prefs.PALETTEFOLDER);
    }

    public void createItem(Canvas colorContainer2, int noBackground, int i, IProxyColor proxyColor) {
        new ColorItem(colorContainer2, this, SWT.NO_BACKGROUND, i, proxyColor);
    }

    void createMinimalPalette() {
        final ProxyColor[] pc = getMinmalpalete();
        disposeColorItems();
        colors = pc;
        addItems(colors);
    }

    private void createpaletteComposite(final Composite parent) {
        scrollComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        scrollComposite.setLayoutData(gridData);
        final GridLayout gridLayout2 = new GridLayout();
        gridLayout2.verticalSpacing = 0;
        gridLayout2.marginWidth = 0;
        gridLayout2.marginHeight = 0;
        gridLayout2.horizontalSpacing = 0;
        scrollComposite.setLayout(gridLayout2);
        colorContainer = new Canvas(scrollComposite, SWT.NONE);
        colorContainer.setBackground(background);
        colorContainer.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
                if (e.button == 3) {
                    createContextMenu(e, colorContainer);
                }
            }

            public void mouseUp(MouseEvent e) {
            }
        });
        colorContainer.addControlListener(new ControlListener() {

            @Override
            public void controlMoved(ControlEvent e) {
            }

            @Override
            public void controlResized(ControlEvent e) {
                adjustColorContainerSize();
            }
        });
        final GridLayout paletteLayout = new GridLayout();
        paletteLayout.numColumns = columns;
        paletteLayout.makeColumnsEqualWidth = true;
        paletteLayout.verticalSpacing = 1;
        paletteLayout.horizontalSpacing = 1;
        final RowLayout layout = new RowLayout();
        layout.spacing = 1;
        layout.marginTop = 1;
        layout.marginBottom = 1;
        layout.marginHeight = 1;
        layout.marginLeft = 1;
        layout.marginRight = 1;
        layout.marginWidth = 1;
        layout.marginHeight = 1;
        layout.wrap = true;
        colorContainer.setLayout(paletteLayout);
        scrollComposite.setContent(colorContainer);
        scrollComposite.setMinWidth(200);
        scrollComposite.setExpandVertical(true);
        scrollComposite.setExpandHorizontal(true);
        scrollComposite.addControlListener(new ControlAdapter() {

            public void controlResized(ControlEvent e) {
                final Rectangle rect = scrollComposite.getClientArea();
                scrollComposite.setMinSize(colorContainer.computeSize(rect.width, SWT.DEFAULT));
            }
        });
    }

    private void delete(ColorItem colorItem) {
        if (colorItem.gradientData != null) {
            final GradientData gd = colorItem.gradientData;
            for (int i = 0; i < gradientDatas.length; i++) {
                if (gradientDatas[i].equals(gd)) {
                    gradientDatas[i] = null;
                }
            }
            final GradientData[] gradientDatas2 = new GradientData[gradientDatas.length - 1];
            int p = 0;
            for (int i = 0; i < gradientDatas.length; i++) {
                if (gradientDatas[i] != null) {
                    gradientDatas2[p++] = gradientDatas[i];
                }
            }
            gradientDatas = gradientDatas2;
        } else {
            final IProxyColor c = colorItem.color;
            for (int i = 0; i < colors.length; i++) {
                if (colors[i].equals(c)) {
                    colors[i] = null;
                }
            }
            final IProxyColor[] colors2 = new ProxyColor[colors.length - 1];
            int p = 0;
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] != null) {
                    colors2[p++] = colors[i];
                }
            }
            colors = colors2;
        }
        colorItem.dispose();
        colorItem = null;
        updateColors();
        colorContainer.layout(true);
    }

    public void dispose() {
        System.out.println("ColorOrganizer.dispose():");
        if ((colorContainer != null) && !colorContainer.isDisposed()) {
            final Control[] children = colorContainer.getChildren();
            for (int i = 0; i < children.length; i++) {
                final Control control = children[i];
                if ((control != null) && !control.isDisposed()) {
                    control.dispose();
                }
            }
        }
        if (colors != null) {
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] != null) {
                    colors[i].dispose();
                }
            }
        }
        for (final Iterator<Color> iterator = extraColors.iterator(); iterator.hasNext(); ) {
            Color c = iterator.next();
            if ((c != null) && !c.isDisposed()) {
                c.dispose();
                c = null;
            }
        }
        if (currentColor != null && !currentColor.isDisposed()) {
            currentColor.dispose();
            currentColor = null;
        }
    }

    void disposeColorItems() {
        if (colorContainer != null) {
            final Control[] children = colorContainer.getChildren();
            if ((children != null) && (children.length > 0)) {
                for (int i = 0; i < children.length; i++) {
                    children[i].dispose();
                    children[i] = null;
                }
            }
            gradientDatas = null;
        }
    }

    public Color getBackground() {
        return background;
    }

    public int getColorHeight() {
        return colorHeight;
    }

    public IProxyColor[] getColors() {
        return colors;
    }

    public int getColumns() {
        return columns;
    }

    public Composite getControl() {
        return main;
    }

    public MouseListener getItemListener() {
        return itemListener;
    }

    ProxyColor[] getMinmalpalete() {
        final ProxyColor[] pc = new ProxyColor[5];
        pc[0] = new ProxyColor(main.getDisplay(), 0, 0, 0, 0);
        pc[1] = new ProxyColor(main.getDisplay(), 255, 255, 255, 1);
        pc[2] = new ProxyColor(main.getDisplay(), 255, 0, 0, 2);
        pc[3] = new ProxyColor(main.getDisplay(), 0, 255, 0, 3);
        pc[4] = new ProxyColor(main.getDisplay(), 0, 0, 255, 4);
        return pc;
    }

    public String getPaletteName() {
        return paletteName;
    }

    public ColorItem getSelectedCanvas() {
        return selectedCanvas;
    }

    public void initColorsAndGradients() {
        if ((colors != null) && (colors.length > 0)) {
            addItems(colors);
        }
        if ((gradientDatas != null) && (gradientDatas.length > 0)) {
            addItems(gradientDatas);
        }
        setNofColumns(columns);
    }

    public boolean isEditMode() {
        return editMode;
    }

    public boolean loadColors(File file) {
        if ((file != null) && file.exists()) {
            final Path path = new Path(file.getAbsolutePath());
            final String fileExt = path.getFileExtension();
            if (fileExt.equalsIgnoreCase("aco")) {
                disposeColorItems();
                final byte[] acoData = IOUtils.readFile(file, 0);
                colors = Aco.readColors(acoData, colorContainer.getDisplay());
                final Rectangle rect = colorContainer.getClientArea();
                columns = (rect.width / 17);
                savePrefs(file);
                return true;
            } else if (fileExt.equalsIgnoreCase("gpl")) {
                final ProxyColor[] colors2 = GimpPaletteReader.readGimpPalette(colorContainer.getDisplay(), file);
                if (colors2 == null) {
                    return false;
                }
                disposeColorItems();
                colors = colors2;
                final Rectangle rect = colorContainer.getClientArea();
                columns = (rect.width / 17);
                savePrefs(file);
                return true;
            } else if (fileExt.equalsIgnoreCase("ifpc")) {
                disposeColorItems();
                try {
                    final Object object = IOUtils.readObject(file);
                    convertToColorAndGradient(object, file.getName());
                    savePrefs(file);
                    return true;
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public void loadPalette(String palette) {
        confirmSavePalette();
        try {
            Object object = null;
            object = PaletteLoader.load(palette);
            disposeColorItems();
            convertToColorAndGradient(object, palette);
            initColorsAndGradients();
            paletteName = palette;
            Prefs.setPrefs(Prefs.PALETTE, palette);
            Prefs.setPrefs(Prefs.PALETTECUSTOM, false);
        } catch (final IOException e1) {
            createMinimalPalette();
            e1.printStackTrace();
        }
    }

    void modifySelectedcolor(ColorItem colorItem) {
        final IProxyColor pColor = colorItem.getColor();
        if (pColor != null) {
            RGB rgb = new RGB(pColor.getRed(), pColor.getGreen(), pColor.getBlue());
            final HSBColorDialog colorDialog = new HSBColorDialog(colorItem.getShell(), true);
            colorDialog.setColor(colorItem.getShell().getDisplay(), rgb);
            colorDialog.setColorOrganizer(ColorOrganizer.this);
            final int code = colorDialog.open();
            if (code == Window.OK) {
                rgb = colorDialog.getRGB();
                if (rgb != null) {
                    pColor.dispose();
                    pColor.setColor(new Color(colorItem.getDisplay(), rgb));
                    colorItem.redraw();
                }
            }
        }
    }

    protected void openSystemPalette() {
        disposeColorItems();
        final ProxyColor[] min = getMinmalpalete();
        final ProxyColor[] pc = new ProxyColor[SWTColors.colorIds.length + min.length];
        final Display display = colorContainer.getDisplay();
        for (int i = 0; i < min.length; i++) {
            pc[i] = min[i];
        }
        for (int i = min.length, j = 0; i < pc.length; i++, j++) {
            final Color c = display.getSystemColor(SWTColors.colorIds[j]);
            pc[i] = new ProxyColor(c, i);
        }
        colors = pc;
        for (int i = 0; i < min.length; i++) {
            new ColorItem(colorContainer, this, SWT.NO_BACKGROUND, i, colors[i]);
        }
        for (int i = min.length, j = 0; i < pc.length; i++, j++) {
            final ColorItem ci = new ColorItem(colorContainer, this, SWT.NO_BACKGROUND, i, colors[i]);
            ci.setToolTipText(ci.getToolTipText() + " " + SWTColors.colorNames[j]);
        }
        colorContainer.layout(true, true);
    }

    public void removeListener(IColorManager gm) {
        graphicsManagerListener.remove(gm);
    }

    private Object[] removeNullValues(Object[] object) {
        if (object == null) {
            return null;
        }
        int counter = 0;
        for (int i = 0; i < object.length; i++) {
            if (object[i] == null) {
                counter++;
            }
        }
        int p = 0;
        if (counter > 0) {
            final Object[] object2 = new Object[object.length - counter];
            for (int i = 0; i < object.length; i++) {
                if (object[i] != null) {
                    object2[p++] = object[i];
                }
            }
            return object2;
        }
        return object;
    }

    void resetColors() {
        if ((colors == null) || (colors.length == 0)) {
            return;
        }
        final Control[] children = colorContainer.getChildren();
        final int length = gradientDatas == null ? 0 : gradientDatas.length;
        if (colors.length != children.length - length) {
            return;
        }
        final IProxyColor[] colors2 = new ProxyColor[colors.length];
        for (int i = 0; i < colors.length; i++) {
            final int index = colors[i].getIndex();
            if ((index < children.length) && (index > -1)) {
                colors2[index] = colors[i];
            } else {
                System.err.println("resetColors()");
            }
        }
        colors = colors2;
        updateColors();
    }

    void saveColors(File file) {
        if ((file != null) && file.isFile()) {
            final ColorAndGradientData cad = new ColorAndGradientData();
            final RGB[] rgbs = new RGB[colors.length];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = colors[i].getRGB();
            }
            cad.colors = rgbs;
            cad.gradients = gradientDatas;
            final GridLayout gd = (GridLayout) colorContainer.getLayout();
            cad.cols = gd.numColumns;
            cad.colorHeight = colorHeight;
            try {
                IOUtils.writeObject(cad, file);
                isDirty = false;
                savePrefs(file);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveColumns() {
        if (paletteName != null) {
            final IPreferenceStore store = ColorPlugin.getDefault().getPreferenceStore();
            if (store != null) {
                store.setValue(paletteName + "h", colorHeight);
                store.setValue(paletteName + "c", columns);
            }
        }
    }

    public void savePrefs(File file) {
        Prefs.setPrefs(Prefs.PALETTECUSTOM, true);
        Prefs.setPrefs(Prefs.PALETTEPATH, file.getAbsolutePath());
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    public void setBackGround() {
        main.setBackground(background);
        final Control[] c = main.getChildren();
        for (int i = 0; i < c.length; i++) {
            c[i].setBackground(background);
        }
        colorContainer.setBackground(background);
        colorContainer.redraw();
    }

    void setBackgroundColor(RGB rgb) {
        if (graphicsManagerListener != null) {
            Color tmp = new Color(main.getDisplay(), rgb);
            for (final Iterator<IColorManager> iterator = graphicsManagerListener.iterator(); iterator.hasNext(); ) {
                final IColorManager manager = iterator.next();
                manager.setBackground(tmp);
            }
        }
    }

    void setColor3(RGB rgb) {
        if (graphicsManagerListener != null) {
            Color tmp = new Color(main.getDisplay(), rgb);
            for (final Iterator<IColorManager> iterator = graphicsManagerListener.iterator(); iterator.hasNext(); ) {
                final IColorManager manager = iterator.next();
                manager.setColor3(tmp);
            }
        }
    }

    void setColor4(RGB rgb) {
        if (graphicsManagerListener != null) {
            Color tmp = new Color(main.getDisplay(), rgb);
            for (final Iterator<IColorManager> iterator = graphicsManagerListener.iterator(); iterator.hasNext(); ) {
                final IColorManager manager = iterator.next();
                manager.setColor4(tmp);
            }
        }
    }

    public void setColors(ProxyColor[] colors) {
        this.colors = colors;
    }

    public void setDragDrop(final ColorItem item) {
        final Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
        final int operations = DND.DROP_MOVE;
        final DragSource source = new DragSource(item, operations);
        source.setTransfer(types);
        source.addDragListener(new DragSourceListener() {

            public void dragFinished(DragSourceEvent event) {
                if (event.detail == DND.DROP_MOVE) {
                }
            }

            public void dragSetData(DragSourceEvent event) {
                String string = "";
                if (item.getColor() != null) {
                    RGB rgb = item.getColor().getRGB();
                    if (copyMode == 0) {
                        string = ColorUtils.getSWTColorString(rgb);
                    } else if (copyMode == 1) {
                        string = ColorUtils.getAWTColorString(rgb);
                    } else if (copyMode == 2) {
                        string = ColorUtils.getCssColorString(rgb);
                    } else if (copyMode == 3) {
                        string = ColorUtils.getHexColorString(rgb);
                    }
                } else {
                    string = item.getGradientData().toString();
                }
                event.data = string;
            }

            public void dragStart(DragSourceEvent event) {
                event.doit = true;
                isDragging = true;
            }
        });
        final DropTarget target = new DropTarget(item, operations);
        target.setTransfer(types);
        target.addDropListener(new DropTargetAdapter() {

            public void drop(DropTargetEvent event) {
                if (event.data == null) {
                    event.detail = DND.DROP_NONE;
                    return;
                }
                ColorItem selectedCanvas2 = getSelectedCanvas();
                if (selectedCanvas2 == null || !isDragging) {
                    return;
                }
                isDragging = false;
                int oldPos = selectedCanvas2.getIndex();
                final int newPos = item.index;
                if ((oldPos < colors.length) && (newPos < colors.length)) {
                    final IProxyColor tmp = colors[oldPos];
                    if (oldPos > newPos) {
                        for (int i = oldPos; i > newPos; i--) {
                            colors[i] = colors[i - 1];
                        }
                        colors[newPos] = tmp;
                    } else {
                        for (int i = oldPos; i < newPos; i++) {
                            colors[i] = colors[i + 1];
                        }
                        colors[newPos] = tmp;
                    }
                    updateColors();
                    isDirty = true;
                }
            }
        });
    }

    public void setEditMode(boolean isEditing) {
        editMode = isEditing;
    }

    void setForegroundColor(RGB rgb) {
        if (graphicsManagerListener != null) {
            Color tmp = new Color(main.getDisplay(), rgb);
            for (final Iterator<IColorManager> iterator = graphicsManagerListener.iterator(); iterator.hasNext(); ) {
                final IColorManager manager = iterator.next();
                manager.setForeground(tmp);
            }
        }
        if (listeners != null && parent != null) {
            for (final Iterator<ISelectionChangedListener> iterator = listeners.iterator(); iterator.hasNext(); ) {
                final ISelectionChangedListener manager = iterator.next();
                manager.selectionChanged(new SelectionChangedEvent(parent, new StructuredSelection(rgb)));
            }
        }
    }

    void setGradientData(GradientData gradientData) {
        if (graphicsManagerListener != null) {
            for (final Iterator<IColorManager> iterator = graphicsManagerListener.iterator(); iterator.hasNext(); ) {
                final IColorManager manager = iterator.next();
                manager.setGradientData(gradientData);
            }
        }
        if (listeners != null && parent != null) {
            for (final Iterator<ISelectionChangedListener> iterator = listeners.iterator(); iterator.hasNext(); ) {
                final ISelectionChangedListener manager = iterator.next();
                manager.selectionChanged(new SelectionChangedEvent(parent, new StructuredSelection(gradientData)));
            }
        }
    }

    public void setLabelBackground(RGB rgb) {
        if (colorLabel != null && !colorLabel.isDisposed()) {
            Color tmp = new Color(colorLabel.getDisplay(), rgb);
            colorLabel.setBackground(tmp);
            if (currentColor != null && !currentColor.isDisposed()) {
                currentColor.dispose();
            }
            currentColor = tmp;
        }
    }

    public void setNofColumns(int cols) {
        columns = cols;
        final GridLayout gd = (GridLayout) colorContainer.getLayout();
        gd.numColumns = cols;
        colorContainer.layout(true);
        final Rectangle rect = scrollComposite.getClientArea();
        scrollComposite.setMinSize(colorContainer.computeSize(rect.width, SWT.DEFAULT));
    }

    public void setPaletteName(String paletteName) {
        this.paletteName = paletteName;
    }

    public void setSelectedCanvas(ColorItem selectedCanvas) {
        if (selectedCanvas != null && !selectedCanvas.equals(this.selectedCanvas)) {
            this.selectedCanvas = selectedCanvas;
            if (parent != null) {
                ((ColorView) parent).fireSelectionChanged(null);
            }
        }
    }

    public void setSizeOfColors(int selection) {
        final Control[] children = colorContainer.getChildren();
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.heightHint = selection;
        colorHeight = selection;
        for (int i = 0; i < children.length; i++) {
            children[i].setLayoutData(gridData);
        }
        colorContainer.layout(true);
        final Rectangle rect = scrollComposite.getClientArea();
        scrollComposite.setMinSize(colorContainer.computeSize(rect.width, SWT.DEFAULT));
    }

    void updateColors() {
        final Control[] children = colorContainer.getChildren();
        final int length = gradientDatas == null ? 0 : gradientDatas.length;
        if (colors.length != children.length - length) {
            return;
        }
        for (int i = 0; i < colors.length; i++) {
            final ColorItem colorItem = (ColorItem) children[i];
            colorItem.setColor(colors[i]);
            colorItem.setIndex(i);
        }
        for (int i = 0; i < colors.length; i++) {
            children[i].redraw();
        }
    }

    public void confirmSavePalette() {
        if (isDirty) {
            MessageBox m = new MessageBox(colorContainer.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
            m.setMessage("The Color Palette has changed. Would you like to save it?");
            if (m.open() == SWT.OK) {
                handleSavePalette();
            }
        }
    }

    public void handleSavePalette() {
        FileDialog fileChooser = new FileDialog(colorContainer.getShell(), SWT.SAVE);
        fileChooser.setText("Save Color file");
        fileChooser.setFilterPath(currentDir);
        fileChooser.setFilterExtensions(new String[] { "*." + "ifpc", "*.*" });
        fileChooser.setFileName("untitledColors.ifpc");
        String filename = fileChooser.open();
        if (filename != null) {
            File file = new File(filename);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    return;
                }
            } else {
                MessageBox mb = new MessageBox(colorContainer.getShell(), SWT.ICON_QUESTION | SWT.CANCEL | SWT.OK);
                mb.setText("Confirm");
                mb.setMessage(file.getName() + " already exists. Do you want to replace ?");
                int result = mb.open();
                if (result != SWT.OK) {
                    return;
                }
            }
            saveColors(file);
            currentDir = fileChooser.getFilterPath();
            Prefs.setPrefs(Prefs.PALETTEFOLDER, currentDir);
        }
    }
}
