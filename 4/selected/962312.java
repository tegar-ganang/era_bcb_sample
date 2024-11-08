package org.designerator.color.dialog;

import imagefp.color.GradientData;
import org.designerator.color.ColorPlugin;
import org.designerator.color.core.ColorOrganizer;
import org.designerator.color.core.ColorValueVerifier;
import org.designerator.color.core.HSL;
import org.designerator.color.gradient.GradientOrganizerComposite;
import org.designerator.color.interfaces.IColorListener;
import org.designerator.color.interfaces.IHsbUpdateable;
import org.designerator.color.interfaces.IProxyColor;
import org.designerator.color.interfaces.IUpdateable;
import org.designerator.icons.Cursors;
import org.designerator.icons.Icons;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class HSBColorDialog extends Dialog implements IHsbUpdateable, IUpdateable {

    private static final int BUT_ADD = 11;

    private static final int PICKERWIDTH = 256;

    private Text redfield;

    private Text bluefield;

    private Text greenfield;

    private Text huefield;

    private Text saturationfield;

    private Text lightfield;

    private Label colorLabel;

    private Canvas canvas;

    private HSL hsl;

    int channel = 0;

    private Image image;

    private ImageData idata;

    private Image imageScale;

    private Color originalColor;

    private Color previewColor;

    protected boolean mouseDown;

    protected boolean scaleMouseDown;

    private Canvas scaleCanvas;

    protected Cursor picker;

    private Image pickerImage;

    protected int pickerY = 20;

    protected int pickerX = 20;

    protected int scaleY = 10;

    boolean isScale = false;

    private Text hexfield;

    private Font font;

    private Control previewLabel;

    ColorOrganizer colorOrganizer;

    private RGB rgb;

    protected boolean colorMode;

    private GradientData gradientData;

    private GradientOrganizerComposite gradientOrganizerComposite;

    private Composite gradientComposite;

    private Composite stack0HSB;

    private StackLayout stackLayout;

    private Composite stackComposite;

    private Composite stack1Palette;

    private Canvas colorContainer;

    private boolean addButton = true;

    private IColorListener colorListener;

    /**
	 * Create the dialog
	 * 
	 * @param data.getRgb()
	 * @param parentShell
	 */
    public HSBColorDialog(Shell shell, boolean colorMode) {
        super(shell);
        init(colorMode);
    }

    public HSBColorDialog(Shell shell, boolean colorMode, IColorListener updateAble) {
        super(shell);
        this.colorListener = updateAble;
        init(colorMode);
    }

    private void init(boolean colorMode) {
        hsl = new HSL(256, 256);
        picker = Cursors.getcolorPickerCursor();
        pickerImage = Icons.getColorPicker(ColorPlugin.getDisplay());
        this.colorMode = colorMode;
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Color&Gradient");
    }

    /**
	 * Create contents of the dialog
	 * 
	 * @param parent
	 */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout());
        if (!colorMode) {
            createGradientModeComposite(container);
        } else {
            createColorModeComposite(container);
        }
        return container;
    }

    private void createGradientModeComposite(Composite composite) {
        createCenterComposite(composite);
        createGradientComposite(composite);
        initColorValues(originalColor.getRGB(), false);
        getShell().layout();
    }

    private void createColorModeComposite(final Composite composite) {
        createColorChooserComposites(composite);
        initColorValues(originalColor.getRGB(), false);
        getShell().layout();
    }

    private void createModeComposite(final Composite hsbcomposite2) {
        if (stack1Palette == null) {
            return;
        }
        Composite toolBar = new Composite(hsbcomposite2, SWT.NONE);
        final Button colorBut = new Button(toolBar, SWT.RADIO);
        colorBut.setText("Color");
        colorBut.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (colorBut.getSelection()) {
                    stackLayout.topControl = stack0HSB;
                    stackComposite.layout(true);
                }
            }
        });
        final Button gradientBut = new Button(toolBar, SWT.RADIO);
        gradientBut.setText("Palette");
        gradientBut.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                if (gradientBut.getSelection()) {
                    stackLayout.topControl = stack1Palette;
                    stackComposite.layout(true);
                }
            }
        });
        toolBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        GridLayout gridLayout2 = new GridLayout();
        gridLayout2.numColumns = 2;
        toolBar.setLayout(gridLayout2);
    }

    private void createCenterComposite(final Composite hsbcomposite2) {
        stackComposite = new Composite(hsbcomposite2, SWT.None);
        stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        stackLayout = new StackLayout();
        stackComposite.setLayout(stackLayout);
        createColorChooserComposites(stackComposite);
        stackLayout.topControl = stack0HSB;
    }

    private void createColorChooserComposites(final Composite parent) {
        stack0HSB = new Composite(parent, SWT.NONE);
        stack0HSB.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 0;
        stack0HSB.setLayout(gridLayout);
        createColorCanvas(stack0HSB);
        createScaleCanvas(stack0HSB);
        createColorValues(stack0HSB);
    }

    private Composite createpaletteComposite(final Composite parent) {
        stack1Palette = new Composite(parent, SWT.NONE);
        stack1Palette.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        IProxyColor[] pc = colorOrganizer.getColors();
        int cols = colorOrganizer.getColumns();
        int height = colorOrganizer.getColorHeight();
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = cols;
        gridLayout.makeColumnsEqualWidth = true;
        gridLayout.verticalSpacing = 1;
        gridLayout.horizontalSpacing = 1;
        stack1Palette.setLayout(gridLayout);
        stack1Palette.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.heightHint = height;
        final MouseListener mouseListener = new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
                if (gradientOrganizerComposite != null) {
                    Label label = (Label) e.widget;
                    gradientOrganizerComposite.updateItem(label.getBackground());
                }
            }

            public void mouseUp(MouseEvent e) {
            }
        };
        for (int i = 0; i < pc.length; i++) {
            Label label = new Label(stack1Palette, SWT.NONE);
            label.setLayoutData(gridData);
            label.setBackground(pc[i].getColor());
            label.addMouseListener(mouseListener);
        }
        return stack1Palette;
    }

    private void createColorCanvas(final Composite hsbcomposite) {
        canvas = new Canvas(hsbcomposite, SWT.NO_BACKGROUND | SWT.BORDER);
        final GridData gridData_1 = new GridData(SWT.FILL, SWT.FILL, false, false);
        gridData_1.heightHint = PICKERWIDTH;
        gridData_1.widthHint = PICKERWIDTH;
        canvas.setLayoutData(gridData_1);
        canvas.addMouseTrackListener(new MouseTrackAdapter() {

            public void mouseEnter(final MouseEvent e) {
                canvas.setCursor(picker);
            }

            public void mouseExit(final MouseEvent e) {
                canvas.setCursor(null);
            }
        });
        canvas.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
                mouseDown = true;
                if (e.x > 255 || e.y > 255 || e.x < 0 || e.y < 0) {
                    return;
                }
                isScale = false;
                pickerX = e.x;
                pickerY = e.y;
                setColorFromPicker();
                canvas.redraw();
            }

            public void mouseUp(MouseEvent e) {
                mouseDown = false;
                canvas.redraw();
            }
        });
        canvas.addMouseMoveListener(new MouseMoveListener() {

            public void mouseMove(final MouseEvent e) {
                if (mouseDown) {
                    if (e.x > 255 || e.y > 255 || e.x < 0 || e.y < 0) {
                        return;
                    }
                    pickerX = e.x;
                    pickerY = e.y;
                    setColorFromPicker();
                }
            }
        });
        canvas.addPaintListener(new PaintListener() {

            public void paintControl(final PaintEvent e) {
                GC gc = e.gc;
                if (image != null) {
                    Rectangle screen = canvas.getClientArea();
                    Image bufferImage = new Image(getShell().getDisplay(), screen);
                    GC bufferGc = new GC(bufferImage);
                    bufferGc.fillRectangle(screen);
                    bufferGc.drawImage(image, 0, 0);
                    if (!mouseDown && pickerImage != null) {
                        bufferGc.drawImage(pickerImage, pickerX - 7, pickerY - 7);
                    }
                    gc.drawImage(bufferImage, 0, 0);
                    bufferGc.dispose();
                    bufferImage.dispose();
                    bufferImage = null;
                }
            }
        });
    }

    private void createScaleCanvas(final Composite hsbcomposite) {
        final GridLayout gridLayout_2 = new GridLayout();
        gridLayout_2.marginWidth = 0;
        gridLayout_2.marginHeight = 0;
        gridLayout_2.horizontalSpacing = 0;
        scaleCanvas = new Canvas(hsbcomposite, SWT.NO_BACKGROUND | SWT.BORDER);
        final GridData gridData_1 = new GridData(SWT.LEFT, SWT.FILL, false, false);
        gridData_1.widthHint = 24;
        gridData_1.heightHint = PICKERWIDTH;
        scaleCanvas.setLayout(gridLayout_2);
        scaleCanvas.setLayoutData(gridData_1);
        scaleCanvas.addMouseTrackListener(new MouseTrackAdapter() {

            public void mouseEnter(final MouseEvent e) {
                scaleCanvas.setCursor(picker);
            }

            public void mouseExit(final MouseEvent e) {
                scaleCanvas.setCursor(null);
            }
        });
        scaleCanvas.addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
                if (e.y > 255 || e.y < 0) {
                    return;
                }
                isScale = true;
                scaleY = e.y;
                scaleMouseDown = true;
                setColorFromPicker();
                scaleCanvas.redraw();
            }

            public void mouseUp(MouseEvent e) {
                scaleMouseDown = false;
                scaleCanvas.redraw();
            }
        });
        scaleCanvas.addMouseMoveListener(new MouseMoveListener() {

            public void mouseMove(final MouseEvent e) {
                if (scaleMouseDown) {
                    if (e.y > 255 || e.y < 0) {
                        return;
                    }
                    scaleY = e.y;
                    setColorFromPicker();
                }
            }
        });
        scaleCanvas.addPaintListener(new PaintListener() {

            public void paintControl(final PaintEvent e) {
                Rectangle screen = canvas.getClientArea();
                Image bufferImage = new Image(getShell().getDisplay(), screen);
                GC bufferGc = new GC(bufferImage);
                bufferGc.fillRectangle(screen);
                bufferGc.drawImage(imageScale, 0, 0);
                if (!scaleMouseDown && pickerImage != null) {
                    bufferGc.drawImage(pickerImage, 5, scaleY - 7);
                }
                e.gc.drawImage(bufferImage, 0, 0);
                bufferGc.dispose();
                bufferImage.dispose();
                bufferImage = null;
            }
        });
    }

    private void createColorValues(final Composite hsbcomposite) {
        final Composite colorValuesComposite = new Composite(hsbcomposite, SWT.BORDER);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false);
        gridData.heightHint = PICKERWIDTH;
        gridData.widthHint = 100;
        colorValuesComposite.setLayoutData(gridData);
        final GridLayout gridLayout_4 = new GridLayout();
        gridLayout_4.marginWidth = 0;
        gridLayout_4.marginHeight = 0;
        gridLayout_4.horizontalSpacing = 0;
        colorValuesComposite.setLayout(gridLayout_4);
        final Composite composite = new Composite(colorValuesComposite, SWT.NONE);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        composite.setLayoutData(gridData);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        composite.setLayout(gridLayout);
        huefield = createHueFields(composite, "H: ");
        new ColorValueVerifier(this, huefield, null, 0, 360, 0);
        saturationfield = createSaturationFields(composite, "S: ");
        new ColorValueVerifier(this, saturationfield, null, 0, 100, 0);
        lightfield = createBrightnessFields(composite, "L: ");
        new ColorValueVerifier(this, lightfield, null, 0, 100, 0);
        Label spacer = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
        final GridData gd_label = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
        spacer.setLayoutData(gd_label);
        redfield = createRedFields(composite, "R: ");
        new ColorValueVerifier(this, redfield, null, 0, 255, 1);
        greenfield = createGreenFields(composite, "G: ");
        new ColorValueVerifier(this, greenfield, null, 0, 255, 1);
        bluefield = createBlueFields(composite, "B: ");
        new ColorValueVerifier(this, bluefield, null, 0, 255, 1);
        hexfield = createHexFields(colorValuesComposite, "#:");
        Composite preview = new Composite(colorValuesComposite, SWT.NONE);
        preview.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        gridLayout = new GridLayout();
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.numColumns = 2;
        preview.setLayout(gridLayout);
        colorLabel = new Label(preview, SWT.NONE);
        final GridData gridData_3 = new GridData(SWT.FILL, SWT.FILL, true, true);
        colorLabel.setLayoutData(gridData_3);
        previewLabel = new Label(preview, SWT.NONE);
        previewLabel.setLayoutData(gridData_3);
        if (originalColor != null && !originalColor.isDisposed()) {
            colorLabel.setBackground(originalColor);
            previewLabel.setBackground(originalColor);
        }
    }

    private void createGradientComposite(final Composite hsbcomposite) {
        gradientOrganizerComposite = new GradientOrganizerComposite(this, gradientData);
        gradientOrganizerComposite.createComposite(hsbcomposite);
    }

    private Text createHueFields(final Composite composite, String string) {
        final Button hueButton = new Button(composite, SWT.RADIO);
        hueButton.setSelection(true);
        SelectionAdapter hueAction = new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (hueButton.getSelection()) {
                    channel = 0;
                    setColorFromFields();
                }
            }
        };
        hueButton.addSelectionListener(hueAction);
        hueButton.setText(string);
        hueButton.setFont(composite.getFont());
        Text text = new Text(composite, SWT.BORDER);
        text.setFont(composite.getFont());
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return text;
    }

    private Text createSaturationFields(final Composite composite, String string) {
        final Button satButton = new Button(composite, SWT.RADIO);
        satButton.setText(string);
        SelectionAdapter saturationAction = new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (satButton.getSelection()) {
                    channel = 1;
                    setColorFromFields();
                }
            }
        };
        satButton.addSelectionListener(saturationAction);
        satButton.setFont(composite.getFont());
        Text text = new Text(composite, SWT.BORDER);
        text.setFont(composite.getFont());
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return text;
    }

    private Text createBrightnessFields(final Composite composite, String string) {
        final Button Light = new Button(composite, SWT.RADIO);
        Light.setText(string);
        Light.setFont(composite.getFont());
        SelectionAdapter action = new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (Light.getSelection()) {
                    channel = 2;
                    setColorFromFields();
                }
            }
        };
        Light.addSelectionListener(action);
        Text text = new Text(composite, SWT.BORDER);
        text.setFont(composite.getFont());
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return text;
    }

    private Text createRedFields(final Composite composite, String string) {
        final Button red = new Button(composite, SWT.RADIO);
        red.setText(string);
        red.setFont(composite.getFont());
        SelectionAdapter action = new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (red.getSelection()) {
                    channel = 3;
                    setColorFromFields();
                }
            }
        };
        red.addSelectionListener(action);
        Text text = new Text(composite, SWT.BORDER);
        text.setFont(composite.getFont());
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return text;
    }

    private Text createGreenFields(final Composite composite, String string) {
        final Button green = new Button(composite, SWT.RADIO);
        green.setText(string);
        green.setFont(composite.getFont());
        SelectionAdapter action = new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (green.getSelection()) {
                    channel = 4;
                    setColorFromFields();
                }
            }
        };
        green.addSelectionListener(action);
        Text text = new Text(composite, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        text.setFont(composite.getFont());
        return text;
    }

    private Text createBlueFields(final Composite composite, String string) {
        final Button blue = new Button(composite, SWT.RADIO);
        blue.setFont(composite.getFont());
        blue.setText(string);
        SelectionAdapter action = new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
                if (blue.getSelection()) {
                    channel = 5;
                    setColorFromFields();
                }
            }
        };
        blue.addSelectionListener(action);
        Text text = new Text(composite, SWT.BORDER);
        text.setFont(composite.getFont());
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return text;
    }

    private Text createHexFields(final Composite colorValuesComposite, String string) {
        final Composite composite = new Composite(colorValuesComposite, SWT.NONE);
        final GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        composite.setLayoutData(gridData);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.verticalSpacing = 2;
        gridLayout.marginHeight = 2;
        composite.setLayout(gridLayout);
        final Label label = new Label(composite, SWT.NONE);
        label.setFont(colorValuesComposite.getFont());
        label.setText(string);
        Text text = new Text(composite, SWT.BORDER);
        text.setFont(colorValuesComposite.getFont());
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return text;
    }

    /**
	 * Create contents of the button bar
	 * 
	 * @param parent
	 */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        if (addButton) {
            createButton(parent, BUT_ADD, "Add", false);
        }
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private void createOptions(final Composite composite) {
        final Composite optionsComposite = new Composite(composite, SWT.NONE);
        optionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        final GridLayout gridLayout_3 = new GridLayout();
        gridLayout_3.numColumns = 3;
        optionsComposite.setLayout(gridLayout_3);
    }

    public void setColor(Display display, RGB rgb) {
        float[] hsb = java.awt.Color.RGBtoHSB(rgb.red, rgb.green, rgb.blue, null);
        image = hsl.getHslColortableImage(display, hsb[0], channel = 0);
        idata = image.getImageData();
        imageScale = hsl.getHslScaletableImage(display, 0f, 1.0f, 1.0f, 0);
        pickerY = 255 - (int) (hsb[2] * 255f + 0.5f);
        pickerX = 255 - (int) (hsb[1] * 255f + 0.5f);
        scaleY = 255 - (int) (hsb[0] * 255f + 0.5f);
        if (colorMode) {
            setOriginalColor(display, rgb);
        }
        setSelectedColor(display, rgb.red, rgb.green, rgb.blue);
    }

    public void setGradient(Display display, GradientData gradientData) {
        RGB rgb2 = gradientData.getRgb()[0];
        if (rgb2 != null) {
            colorMode = false;
            setColor(display, rgb2);
            this.gradientData = gradientData;
        }
    }

    public void setColor(int red, int green, int blue, int hue, int saturation, int brightness) {
        setHuefield(Integer.toString(hue));
        setSaturationfield(Integer.toString(saturation));
        setLightfield(Integer.toString(brightness));
        setRedfield(Integer.toString(red));
        setGreenfield(Integer.toString(green));
        setBluefield(Integer.toString(blue));
        int intfromRGB = 0xff000000 | (red & 0xff) << 16 | (blue & 0xff) << 8 | green & 0xff;
        hexfield.setText(Integer.toHexString(intfromRGB));
        setSelectedColor(getShell().getDisplay(), red, green, blue);
        previewLabel.setBackground(previewColor);
        if (!colorMode) {
            colorLabel.setBackground(previewColor);
        }
    }

    /**
	 * This method sets the final Color selection
	 * @param display
	 * @param red
	 * @param green
	 * @param blue
	 */
    private void setSelectedColor(Display display, int red, int green, int blue) {
        if (previewColor != null && !previewColor.isDisposed()) {
            previewColor.dispose();
            previewColor = null;
        }
        previewColor = new Color(display, red, green, blue);
        if (gradientOrganizerComposite != null) {
            gradientOrganizerComposite.updateItem(previewColor);
        }
        if (!colorMode) {
            originalColor = previewColor;
        }
        if (colorListener != null) {
            if (gradientOrganizerComposite == null) {
                colorListener.update(previewColor.getRGB());
            } else {
                colorListener.updateGradient(gradientOrganizerComposite.getGradientData().clone(), false);
            }
        }
    }

    private void setOriginalColor(Display display, RGB rgb) {
        if (originalColor != null && !originalColor.isDisposed()) {
            originalColor.dispose();
            originalColor = null;
        }
        originalColor = new Color(display, rgb.red, rgb.green, rgb.blue);
    }

    private void setColorPicker() {
        float[] hsb;
        switch(channel) {
            case 0:
                hsb = getHSBFieldValues();
                pickerY = 255 - (int) (hsb[2] * 255f + 0.5f);
                pickerX = 255 - (int) (hsb[1] * 255f + 0.5f);
                scaleY = 255 - (int) (hsb[0] * 255f + 0.5f);
                break;
            case 1:
                hsb = getHSBFieldValues();
                pickerY = 255 - (int) (hsb[2] * 255f + 0.5f);
                pickerX = 255 - (int) (hsb[0] * 255f + 0.5f);
                scaleY = 255 - (int) (hsb[1] * 255f + 0.5f);
                break;
            case 2:
                hsb = getHSBFieldValues();
                pickerY = 255 - (int) (hsb[1] * 255f + 0.5f);
                pickerX = 255 - (int) (hsb[0] * 255f + 0.5f);
                scaleY = 255 - (int) (hsb[2] * 255f + 0.5f);
                break;
            case 3:
                int[] rgb = getRGBFieldValues();
                pickerY = rgb[2];
                pickerX = rgb[1];
                scaleY = rgb[0];
                break;
            case 4:
                rgb = getRGBFieldValues();
                pickerY = rgb[2];
                pickerX = rgb[0];
                scaleY = rgb[1];
                break;
            case 5:
                rgb = getRGBFieldValues();
                pickerY = rgb[0];
                pickerX = rgb[1];
                scaleY = rgb[2];
                break;
            default:
                break;
        }
        pickerY = pickerY < 0 ? 0 : pickerY;
        pickerX = pickerX < 0 ? 0 : pickerX;
        scaleY = scaleY < 0 ? 0 : scaleY;
    }

    private void setColorFromFields() {
        float[] hsb;
        switch(channel) {
            case 0:
                hsb = getHSBFieldValues();
                hsl.getHslColortableImage(getShell().getDisplay(), hsb[0], channel, HSBColorDialog.this);
                updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), 0f, 1f, 1f, channel));
                setColorPicker();
                canvas.redraw();
                scaleCanvas.redraw();
                break;
            case 1:
                hsb = getHSBFieldValues();
                updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), hsb[0], 0f, hsb[2], channel));
                hsl.getHslColortableImage(getShell().getDisplay(), hsb[1], channel, HSBColorDialog.this);
                setColorPicker();
                canvas.redraw();
                scaleCanvas.redraw();
                break;
            case 2:
                hsb = getHSBFieldValues();
                updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), hsb[0], hsb[1], 0f, channel));
                hsl.getHslColortableImage(getShell().getDisplay(), hsb[2], channel, HSBColorDialog.this);
                setColorPicker();
                canvas.redraw();
                scaleCanvas.redraw();
                break;
            case 3:
                int[] rgb = getRGBFieldValues();
                updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), rgb[0], rgb[1], rgb[2], channel));
                hsl.getHslColortableImage(getShell().getDisplay(), rgb[0], channel, HSBColorDialog.this);
                setColorPicker();
                canvas.redraw();
                scaleCanvas.redraw();
                break;
            case 4:
                rgb = getRGBFieldValues();
                updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), rgb[0], rgb[1], rgb[2], channel));
                hsl.getHslColortableImage(getShell().getDisplay(), rgb[1], channel, HSBColorDialog.this);
                setColorPicker();
                canvas.redraw();
                scaleCanvas.redraw();
                break;
            case 5:
                rgb = getRGBFieldValues();
                updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), rgb[0], rgb[1], rgb[2], channel));
                hsl.getHslColortableImage(getShell().getDisplay(), rgb[2], channel, HSBColorDialog.this);
                setColorPicker();
                canvas.redraw();
                scaleCanvas.redraw();
                break;
            default:
                break;
        }
    }

    private void setColorFromPicker() {
        int hue, saturation, brightness;
        float bright;
        float sat;
        int[] rgb = new int[3];
        float[] hsb;
        switch(channel) {
            case 0:
                sat = 1.0f - pickerX / 255f;
                bright = 1.0f - pickerY / 255f;
                hue = 360 - (int) (scaleY / 255f * 360f + 0.5f);
                saturation = (int) (sat * 100f + 0.5f);
                brightness = (int) (bright * 100f + 0.5f);
                HSL.toRGB(hue, sat, bright, rgb);
                setColor(rgb[2], rgb[1], rgb[0], hue, saturation, brightness);
                if (isScale) {
                    float val = (1f / 255f) * (float) scaleY;
                    hsl.getHslColortableImage(getShell().getDisplay(), 1.0f - val, channel, HSBColorDialog.this);
                }
                break;
            case 1:
                sat = 1.0f - scaleY / 255f;
                bright = 1.0f - pickerY / 255f;
                float hue0 = pickerX / 255f;
                hue = 360 - (int) (hue0 * 360f + 0.5f);
                saturation = (int) (sat * 100f + 0.5f);
                brightness = (int) (bright * 100f + 0.5f);
                HSL.toRGB(hue, sat, bright, rgb);
                setColor(rgb[2], rgb[1], rgb[0], hue, saturation, brightness);
                if (!isScale) {
                    updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), 1.0f - hue0, 0f, bright, channel));
                } else {
                    float val = (1f / 255f) * (float) scaleY;
                    hsl.getHslColortableImage(getShell().getDisplay(), 1.0f - val, channel, HSBColorDialog.this);
                }
                break;
            case 2:
                sat = 1.0f - pickerY / 255f;
                bright = 1.0f - scaleY / 255f;
                hue0 = pickerX / 255f;
                hue = 360 - (int) (pickerX / 255f * 360f + 0.5f);
                saturation = (int) (sat * 100f + 0.5f);
                brightness = (int) (bright * 100f + 0.5f);
                HSL.toRGB(hue, sat, bright, rgb);
                setColor(rgb[2], rgb[1], rgb[0], hue, saturation, brightness);
                if (!isScale) {
                    updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), 1.0f - hue0, sat, 0f, channel));
                } else {
                    float val = (1f / 255f) * (float) scaleY;
                    hsl.getHslColortableImage(getShell().getDisplay(), 1.0f - val, channel, HSBColorDialog.this);
                }
                break;
            case 3:
                hsb = HSL.toHSB(scaleY, pickerX, pickerY);
                setColor(scaleY, pickerX, pickerY, (int) hsb[0], (int) (hsb[1] * 100f + 0.5f), (int) (hsb[2] * 100f + 0.5f));
                if (!isScale) {
                    updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), scaleY, pickerX, pickerY, channel));
                } else {
                    hsl.getHslColortableImage(getShell().getDisplay(), scaleY, channel, HSBColorDialog.this);
                }
                break;
            case 4:
                hsb = HSL.toHSB(pickerY, scaleY, pickerX);
                setColor(pickerX, scaleY, pickerY, (int) hsb[0], (int) (hsb[1] * 100f + 0.5f), (int) (hsb[2] * 100f + 0.5f));
                if (!isScale) {
                    updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), pickerX, scaleY, pickerY, channel));
                } else {
                    hsl.getHslColortableImage(getShell().getDisplay(), scaleY, channel, HSBColorDialog.this);
                }
                break;
            case 5:
                hsb = HSL.toHSB(pickerY, pickerX, scaleY);
                setColor(pickerY, pickerX, scaleY, (int) hsb[0], (int) (hsb[1] * 100f + 0.5f), (int) (hsb[2] * 100f + 0.5f));
                if (!isScale) {
                    updateScaleImage(hsl.getHslScaletableImage(getShell().getDisplay(), pickerY, pickerX, scaleY, channel));
                } else {
                    hsl.getHslColortableImage(getShell().getDisplay(), scaleY, channel, HSBColorDialog.this);
                }
                break;
            default:
                break;
        }
    }

    private void initColorValues(RGB rgb, boolean scale) {
        float[] hsb = new float[3];
        java.awt.Color.RGBtoHSB(rgb.red, rgb.green, rgb.blue, hsb);
        int hue = (int) (hsb[0] * 360);
        int saturation = (int) (hsb[1] * 100);
        int brightness = (int) (hsb[2] * 100);
        if (scale && channel == 2) {
            updateScaleImage(hsl.getHslScaletableImage(ColorPlugin.getDisplay(), hsb[0], hsb[1], 0f, channel));
        } else if (scale && channel == 1) {
            updateScaleImage(hsl.getHslScaletableImage(ColorPlugin.getDisplay(), hsb[0], 0f, hsb[2], channel));
        }
        setHuefield(Integer.toString(hue));
        setSaturationfield(Integer.toString(saturation));
        setLightfield(Integer.toString(brightness));
        setRedfield(Integer.toString(rgb.red));
        setGreenfield(Integer.toString(rgb.green));
        setBluefield(Integer.toString(rgb.blue));
        int intfromRGB = 0xff000000 | (rgb.red & 0xff) << 16 | (rgb.blue & 0xff) << 8 | rgb.green & 0xff;
        hexfield.setText(Integer.toHexString(intfromRGB));
        colorLabel.setBackground(originalColor);
        previewLabel.setBackground(previewColor);
    }

    public void update(int colorMode) {
        if (colorMode == 0) {
            float[] hsb = getHSBFieldValues();
            int[] rgb = new int[3];
            HSL.toRGB(hsb[0] * 360f, hsb[1], hsb[2], rgb);
            setRedfield(Integer.toString(rgb[2]));
            setGreenfield(Integer.toString(rgb[1]));
            setBluefield(Integer.toString(rgb[0]));
            setSelectedColor(getShell().getDisplay(), rgb[2], rgb[1], rgb[0]);
        } else if (colorMode == 1) {
            int[] rgb = getRGBFieldValues();
            float[] hsb = HSL.toHSB(rgb[0], rgb[1], rgb[2]);
            setHuefield(Integer.toString((int) hsb[0]));
            setSaturationfield(Integer.toString((int) (hsb[1] * 100f + 0.5f)));
            setLightfield(Integer.toString((int) (hsb[2] * 100f + 0.5f)));
            setSelectedColor(getShell().getDisplay(), rgb[0], rgb[1], rgb[2]);
        }
        previewLabel.setBackground(previewColor);
        if (!this.colorMode) {
            colorLabel.setBackground(previewColor);
        }
        setColorFromFields();
    }

    private void deselectColorButtons(final Composite colorValuesComposite, final Button selected) {
        Control[] children = colorValuesComposite.getChildren();
        for (int i = 0; i < children.length; i++) {
            Control control = children[i];
            if (control instanceof Composite) {
                Control[] children2 = ((Composite) control).getChildren();
                for (int j = 0; j < children2.length; j++) {
                    if (children2[j] instanceof Button) {
                        Button button = (Button) children2[j];
                        if (!button.equals(selected)) {
                            (button).setSelection(false);
                        }
                    }
                }
            }
        }
    }

    private float[] getHSBFieldValues() {
        String h = huefield.getText();
        String s = saturationfield.getText();
        String b = lightfield.getText();
        float[] hsb = new float[3];
        if (h == null || h.equals("") || h.equals(" ")) {
            return null;
        }
        if (s == null || s.equals("") || s.equals(" ")) {
            return null;
        }
        if (b == null || b.equals("") || b.equals(" ")) {
            return null;
        }
        try {
            int valHue = Integer.valueOf(h);
            hsb[0] = valHue / 360f;
            int valSat = Integer.valueOf(s);
            hsb[1] = valSat / 100f;
            int valLight = Integer.valueOf(b);
            hsb[2] = valLight / 100f;
        } catch (NumberFormatException e1) {
            e1.printStackTrace();
            return null;
        }
        return hsb;
    }

    private int[] getRGBFieldValues() {
        String h = redfield.getText();
        String s = greenfield.getText();
        String b = bluefield.getText();
        int[] rgb = new int[3];
        if (h == null || h.equals("") || h.equals(" ")) {
            return null;
        }
        if (s == null || s.equals("") || s.equals(" ")) {
            return null;
        }
        if (b == null || b.equals("") || b.equals(" ")) {
            return null;
        }
        try {
            rgb[0] = Integer.valueOf(h);
            rgb[1] = Integer.valueOf(s);
            rgb[2] = Integer.valueOf(b);
        } catch (NumberFormatException e1) {
            e1.printStackTrace();
            return null;
        }
        return rgb;
    }

    public void updateImage(byte[] colortable) {
        idata.data = colortable;
        if (image != null) {
            image.dispose();
            image = null;
        }
        image = new Image(getShell().getDisplay(), idata);
        canvas.redraw();
    }

    public void updateScaleImage(Image scaleImage) {
        if (imageScale != null) {
            imageScale.dispose();
            imageScale = null;
        }
        imageScale = scaleImage;
        scaleCanvas.redraw();
    }

    void dispose() {
        if (originalColor != null && !originalColor.isDisposed()) {
            originalColor.dispose();
        }
        if (previewColor != null && !previewColor.isDisposed()) {
            rgb = previewColor.getRGB();
            previewColor.dispose();
        }
        if (image != null && !image.isDisposed()) {
            image.dispose();
        }
        if (imageScale != null && !imageScale.isDisposed()) {
            imageScale.dispose();
        }
        if (font != null && !font.isDisposed()) {
            font.dispose();
            font = null;
        }
        if (gradientOrganizerComposite != null) {
            gradientOrganizerComposite.dispose();
            font = null;
        }
    }

    @Override
    public boolean close() {
        dispose();
        return super.close();
    }

    protected void buttonPressed(int buttonId) {
        if (BUT_ADD == buttonId) {
            if (colorMode && colorOrganizer != null) {
                RGB rgb2 = previewColor.getRGB();
                colorOrganizer.addColor(rgb2);
                setOriginalColor(getShell().getDisplay(), rgb2);
            } else if (gradientOrganizerComposite != null && colorOrganizer != null) {
                colorOrganizer.addGradient(gradientOrganizerComposite.getGradientData().clone());
            } else if (gradientOrganizerComposite != null && colorListener != null) {
                colorListener.updateGradient(gradientOrganizerComposite.getGradientData().clone(), true);
            }
        }
        super.buttonPressed(buttonId);
    }

    /**
	 * @return the redfield
	 */
    public String getRedfield() {
        return redfield.getText();
    }

    /**
	 * @param redfield
	 *            the redfield to set
	 */
    public void setRedfield(String s) {
        this.redfield.setText(s);
    }

    /**
	 * @return the bluefield
	 */
    public String getBluefield() {
        return bluefield.getText();
    }

    /**
	 * @param bluefield
	 *            the bluefield to set
	 */
    public void setBluefield(String s) {
        this.bluefield.setText(s);
    }

    /**
	 * @return the greenfield
	 */
    public String getGreenfield() {
        return greenfield.getText();
    }

    /**
	 * @param greenfield
	 *            the greenfield to set
	 */
    public void setGreenfield(String s) {
        this.greenfield.setText(s);
    }

    /**
	 * @return the huefield
	 */
    public String getHuefield() {
        return huefield.getText();
    }

    /**
	 * @param huefield
	 *            the huefield to set
	 */
    public void setHuefield(String s) {
        this.huefield.setText(s);
    }

    /**
	 * @return the saturationfield
	 */
    public String getSaturationfield() {
        return saturationfield.getText();
    }

    /**
	 * @param saturationfield
	 *            the saturationfield to set
	 */
    public void setSaturationfield(String s) {
        this.saturationfield.setText(s);
    }

    /**
	 * @return the lightfield
	 */
    public Text getLightfield() {
        return lightfield;
    }

    /**
	 * @param lightfield
	 *            the lightfield to set
	 */
    public void setLightfield(String s) {
        this.lightfield.setText(s);
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public RGB getRGB() {
        if (previewColor == null || previewColor.isDisposed()) {
            if (rgb != null) {
                return rgb;
            }
            return null;
        }
        return previewColor.getRGB();
    }

    /**
	 * Beta - in development
	 * Package only - DO NOT USE - call getRGB instead
	 */
    public Color getColor() {
        if (rgb != null) {
            Color c = new Color(ColorPlugin.getDisplay(), rgb);
            ColorOrganizer.addExtraColor(c);
            return c;
        } else if (previewColor != null && !previewColor.isDisposed()) {
            Color c = new Color(ColorPlugin.getDisplay(), previewColor.getRGB());
            ColorOrganizer.addExtraColor(c);
            return c;
        }
        return null;
    }

    public void setColorOrganizer(ColorOrganizer colorOrganizer) {
        this.colorOrganizer = colorOrganizer;
    }

    public GradientData getGradient() {
        return gradientData;
    }

    public void setPreviewColor(Color previewColor2) {
        this.previewColor = previewColor2;
    }

    public boolean isAddButton() {
        return addButton;
    }

    public void setAddButton(boolean addButton) {
        this.addButton = addButton;
    }
}
