package abbot.swt.gef.tester;

import java.util.HashSet;
import java.util.Set;
import junit.framework.Assert;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import abbot.swt.display.DisplayTester;
import abbot.swt.display.Result;
import abbot.swt.finder.Finder;
import abbot.swt.finder.MultipleFoundException;
import abbot.swt.finder.NotFoundException;
import abbot.swt.finder.WidgetFinderImpl;
import abbot.swt.gef.util.GEFWorkbenchUtilities;
import abbot.swt.matcher.ClassMatcher;
import abbot.swt.tester.AbstractTester;
import abbot.swt.tester.Action;
import abbot.swt.tester.ActionFailedException;
import abbot.swt.tester.CanvasTester;
import abbot.swt.tester.ItemPath;
import abbot.swt.tester.MenuTester;
import abbot.swt.tester.SWTConstants;

/**
 * A tester for GEF {@link IFigure}s.
 */
public class FigureTester extends AbstractTester {

    public static FigureTester getFigureTester() {
        return new FigureTester();
    }

    /**
	 * Gets an {@link IFigure}'s top-level {@link Viewport}.
	 * 
	 * @param figure
	 *            the {@link IFigure}
	 * @return the {@link IFigure}'s top-level {@link Viewport} (or <code>null</code> if there isn't
	 *         one)
	 */
    public Viewport getViewport(IFigure figure) {
        checkFigure(figure);
        Viewport viewport = null;
        while (figure != null) {
            if (figure instanceof Viewport) viewport = (Viewport) figure;
            figure = figure.getParent();
        }
        return viewport;
    }

    /**
	 * Finds the {@link FigureCanvas} an {@link IFigure} is in.
	 * <p>
	 * <b>Note:</b> This is a relatively expensive operation because it involves searching the
	 * {@link Widget} hierarchy.
	 * 
	 * @param figure
	 *            the {@link IFigure}
	 * @return the {@link FigureCanvas} the figure is under, or <code>null</code> if there isn't one
	 */
    public FigureCanvas findCanvas(IFigure figure) {
        checkFigure(figure);
        final IFigure root = getRoot(figure);
        for (Display display : DisplayTester.getDisplays()) {
            try {
                Finder<Widget> finder = WidgetFinderImpl.getFinder(display);
                return (FigureCanvas) finder.find(new ClassMatcher<Widget>(FigureCanvas.class) {

                    public boolean matches(Widget widget) {
                        return super.matches(widget) && getRoot((FigureCanvas) widget) == root;
                    }
                });
            } catch (NotFoundException e) {
            } catch (MultipleFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
	 * 
	 */
    private IFigure getRoot(FigureCanvas canvas) {
        return canvas.getLightweightSystem().getRootFigure();
    }

    /**
	 * Gets a figure's root figure.
	 * 
	 * @param figure
	 *            an {@link IFigure}
	 * @return the root {@link IFigure} of the figure's tree
	 */
    public IFigure getRoot(IFigure figure) {
        checkFigure(figure);
        IFigure parent = figure.getParent();
        if (parent == null) return figure;
        return getRoot(parent);
    }

    /**
	 * Gets the {@link GraphicalEditPart} that references an {@link IFigure}.
	 * 
	 * @param figure
	 *            an IFigure
	 * @return the {@link GraphicalEditPart} that references the <code>figure</code> or null
	 */
    public GraphicalEditPart findEditPart(IFigure figure) {
        checkFigure(figure);
        GraphicalViewer viewer = findViewer(figure);
        if (viewer != null) return (GraphicalEditPart) viewer.getVisualPartMap().get(figure);
        return null;
    }

    public GraphicalViewer findViewer(IFigure figure) {
        final IFigure root = getRoot(figure);
        Set<GraphicalViewer> viewers = new HashSet<GraphicalViewer>();
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference editorReference : page.getEditorReferences()) {
                    IEditorPart editor = editorReference.getEditor(false);
                    GraphicalViewer viewer = getViewer(editor);
                    if (viewer != null && viewers.add(viewer)) {
                        if (getRoot(viewer) == root) return viewer;
                        viewer = GEFWorkbenchUtilities.getPaletteViewer(viewer);
                        if (viewer != null && viewers.add(viewer) && getRoot(viewer) == root) return viewer;
                    }
                }
                for (IViewReference viewReference : page.getViewReferences()) {
                    IViewPart view = viewReference.getView(false);
                    GraphicalViewer viewer = getViewer(view);
                    if (viewer != null && viewers.add(viewer) && getRoot(viewer) == root) return viewer;
                }
            }
        }
        return null;
    }

    private IFigure getRoot(GraphicalViewer viewer) {
        GraphicalEditPart rootEditPart = (GraphicalEditPart) viewer.getRootEditPart();
        return getRoot(rootEditPart.getFigure());
    }

    private GraphicalViewer getViewer(IWorkbenchPart part) {
        GraphicalViewer viewer = (GraphicalViewer) part.getAdapter(GraphicalViewer.class);
        if (viewer != null) return viewer;
        EditPart editPart = (EditPart) part.getAdapter(EditPart.class);
        if (editPart != null) {
            EditPartViewer editPartViewer = editPart.getViewer();
            if (editPartViewer instanceof GraphicalViewer) return (GraphicalViewer) editPartViewer;
        }
        return null;
    }

    /**
	 * Gets an {@link IFigure}'s bounding rectangle
	 * 
	 * @param figure
	 *            the {@link IFigure}
	 * @return a {@link Rectangle} that describdes the {@link IFigure}'s location and size in
	 *         {@link Display} coordinates (or an empty {@link Rectangle} if there isn't one)
	 */
    public Rectangle getBounds(IFigure figure) {
        Rectangle relative = getBoundsRelative(figure);
        FigureCanvas canvas = findCanvas(figure);
        return toDisplay(canvas, relative);
    }

    /**
	 * Gets an {@link IFigure}'s bounding rectangle relative to its {@link FigureCanvas}.
	 * 
	 * @param figure
	 *            the {@link IFigure}
	 * @return a {@link Rectangle} that describdes the {@link IFigure}'s location and size in
	 *         {@link Display} coordinates (or an empty {@link Rectangle} if there isn't one)
	 */
    public Rectangle getBoundsRelative(IFigure figure) {
        return toAbsolute(figure, figure.getBounds());
    }

    /**
	 * Gets an {@link IFigure}'s client area in {@link Display} coordinates.
	 * 
	 * @param figure
	 *            an {@link IFigure}
	 * @return a {@link Rectangle} representing the {@link IFigure}'s client area in {@link Display}
	 *         coordinates
	 */
    public Rectangle getClientArea(IFigure figure) {
        Rectangle rectangle = toAbsolute(figure, figure.getClientArea());
        FigureCanvas canvas = findCanvas(figure);
        return toDisplay(canvas, rectangle);
    }

    /**
	 * Translate a {@link org.eclipse.draw2d.geometry.Rectangle} that is in the coordinate system of
	 * an {@link IFigure} into a {@link Rectangle} that is absolute (i.e., is in the coordinate
	 * system of the {@link IFigure}'s {@link FigureCanvas}.
	 * 
	 * @param figure
	 *            an IFigure
	 * @param rectangle
	 *            a {@link org.eclipse.draw2d.geometry.Rectangle} in the coordinate system of the
	 *            {@link IFigure}
	 * @return a {@link Rectangle} in the coordinate system of the {@link IFigure}'s
	 *         {@link FigureCanvas}
	 */
    public Rectangle toAbsolute(IFigure figure, org.eclipse.draw2d.geometry.Rectangle rectangle) {
        rectangle = rectangle.getCopy();
        figure.translateToAbsolute(rectangle);
        return new Rectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    /**
	 * Translate a {@link Rectangle} that is in the coordinate system of a {@link Control} (a
	 * {@link FigureCanvas}, for example) into a {@link Rectangle} in {@link Display} coordinates.
	 * 
	 * @param control
	 *            a {@link Control}
	 * @param rectangle
	 *            a {@link Rectangle} in the coordinate system of the {@link Control}
	 * @return a {@link Rectangle} in {@link Display} coordinates
	 */
    public Rectangle toDisplay(final Control control, final Rectangle rectangle) {
        return syncExec(control, new Result<Rectangle>() {

            public Rectangle result() {
                return control.getDisplay().map(control, null, rectangle);
            }
        });
    }

    /**
	 * Gets the display's bounding rectangle
	 * 
	 * @return a {@link Rectangle} that describdes the {@link Display}'s location and size.
	 */
    protected Rectangle getDisplayBounds() {
        return DisplayTester.getDefault().getBounds();
    }

    /**
	 * Clicks mouse button 1 on the center of an {@link IFigure}.
	 * 
	 * @param figure
	 *            the IFigure
	 */
    @Action
    public void click(IFigure figure) {
        checkFigure(figure);
        checkFigureShowing(figure);
        clickInternal(figure, -1, -1, SWT.BUTTON1);
    }

    /**
	 * Clicks mouse button 1 at a particular point in an {@link IFigure}.
	 * 
	 * @param figure
	 *            the IFigure
	 * @param x
	 *            the x coordinate relative to the {@link IFigure}'s location
	 * @param y
	 *            the y coordinate relative to the {@link IFigure}'s location
	 */
    @Action
    public void click(IFigure figure, int x, int y) {
        checkFigure(figure);
        checkFigureShowing(figure);
        clickInternal(figure, x, y, SWT.BUTTON1);
    }

    /**
	 * Clicks one or more mouse buttons at the center of an {@link IFigure}.
	 * 
	 * @param figure
	 *            the IFigure
	 * @param mask
	 *            the bitwise "or" of one or more of {@link SWT#BUTTON1}, {@link SWT#BUTTON2}, and
	 *            {@link SWT#BUTTON3}.
	 */
    @Action
    public void click(final IFigure figure, final int mask) {
        checkFigure(figure);
        checkFigureShowing(figure);
        clickInternal(figure, -1, -1, mask);
    }

    /**
	 * Clicks one or more mouse buttons at a particular point in an {@link IFigure}.
	 * 
	 * @param figure
	 *            the IFigure
	 * @param x
	 *            the x coordinate relative to the {@link IFigure}'s location
	 * @param y
	 *            the y coordinate relative to the {@link IFigure}'s location
	 * @param mask
	 *            the bitwise "or" of one or more of {@link SWT#BUTTON1}, {@link SWT#BUTTON2}, and
	 *            {@link SWT#BUTTON3}.
	 */
    @Action
    public void click(IFigure figure, int x, int y, int mask) {
        checkFigure(figure);
        checkFigureShowing(figure);
        clickInternal(figure, x, y, mask);
    }

    /**
	 * Clicks one or more mouse buttons at a particular point in an {@link IFigure}.
	 * 
	 * @param figure
	 *            an IFigure
	 * @param x
	 *            the x coordinate relative to the {@link IFigure}'s location
	 * @param y
	 *            the y coordinate relative to the {@link IFigure}'s location
	 * @param mask
	 *            the bitwise "or" of one or more of {@link SWT#BUTTON1}, {@link SWT#BUTTON2}, and
	 *            {@link SWT#BUTTON3}.
	 */
    private void clickInternal(IFigure figure, int x, int y, final int mask) {
        Point p = getPoint(figure, x, y);
        Canvas canvas = findCanvas(figure);
        displayTester(canvas).click(p.x, p.y, mask);
    }

    /**
	 * Moves the mouse pointer to the center of an {@link IFigure}.
	 * 
	 * @param figure
	 *            the IFigure
	 */
    @Action
    public void mouseMove(IFigure figure) {
        checkFigure(figure);
        checkFigureShowing(figure);
        mouseMoveInternal(figure, -1, -1);
    }

    /**
	 * Moves the mouse pointer to a specified point in an {@link IFigure}.
	 * 
	 * @param figure
	 *            the IFigure
	 * @param x
	 *            the x coordinate relative to the {@link IFigure}'s location
	 * @param y
	 *            the y coordinate relative to the {@link IFigure}'s location
	 */
    @Action
    public void mouseMove(IFigure figure, int x, int y) {
        checkFigure(figure);
        checkFigureShowing(figure);
        mouseMoveInternal(figure, x, y);
    }

    /**
	 * Moves the mouse pointer to a specified point in an {@link IFigure}.
	 * <p>
	 * <b>Note:</b> Does not check arguments.
	 * 
	 * @param figure
	 *            the IFigure
	 * @param x
	 *            the x coordinate relative to the {@link IFigure}'s location
	 * @param y
	 *            the y coordinate relative to the {@link IFigure}'s location
	 */
    private void mouseMoveInternal(IFigure figure, int x, int y) {
        final Point p = getPoint(figure, x, y);
        displayTester(findCanvas(figure)).mouseMove(p.x, p.y);
    }

    private Point getPoint(IFigure figure, int x, int y) {
        Rectangle bounds = getBounds(figure);
        if (x == -1 && y == -1) {
            x = bounds.width / 2;
            y = bounds.height / 2;
        }
        return new Point(bounds.x + x, bounds.y + y);
    }

    @Action
    public void dragDrop(IFigure source, IFigure target) {
        dragDrop(source, SWT.BUTTON1, target);
    }

    @Action
    public void dragDrop(IFigure source, int mask, IFigure target) {
        checkFigure(source);
        checkFigureShowing(source);
        checkFigure(target);
        checkFigureShowing(target);
        dragDropInternal(source, -1, -1, mask, target, -1, -1);
    }

    @Action
    public void dragDrop(IFigure source, IFigure target, int tx, int ty) {
        dragDrop(source, SWT.BUTTON1, target, tx, ty);
    }

    @Action
    public void dragDrop(IFigure source, int mask, IFigure target, int tx, int ty) {
        checkFigure(source);
        checkFigureShowing(source);
        checkFigure(target);
        checkFigureShowing(target);
        dragDropInternal(source, -1, -1, mask, target, tx, ty);
    }

    @Action
    public void dragDrop(IFigure source, int tx, int ty) {
        dragDrop(source, SWT.BUTTON1, tx, ty);
    }

    @Action
    public void dragDrop(IFigure source, int mask, int tx, int ty) {
        checkFigure(source);
        checkFigureShowing(source);
        dragDropInternal(source, -1, -1, mask, tx, ty);
    }

    @Action
    public void dragDrop(IFigure source, int sx, int sy, IFigure target) {
        dragDrop(source, sx, sy, SWT.BUTTON1, target);
    }

    @Action
    public void dragDrop(IFigure source, int sx, int sy, int mask, IFigure target) {
        checkFigure(source);
        checkFigureShowing(source);
        checkFigure(target);
        checkFigureShowing(target);
        dragDropInternal(source, sx, sy, mask, target, -1, -1);
    }

    @Action
    public void dragDrop(IFigure source, int sx, int sy, IFigure target, int tx, int ty) {
        dragDrop(source, sx, sy, SWT.BUTTON1, target, tx, ty);
    }

    @Action
    public void dragDrop(IFigure source, int sx, int sy, int mask, IFigure target, int tx, int ty) {
        checkFigure(source);
        checkFigureShowing(source);
        checkFigure(target);
        checkFigureShowing(target);
        dragDropInternal(source, sx, sy, mask, target, tx, ty);
    }

    @Action
    public void dragDrop(IFigure source, int sx, int sy, int tx, int ty) {
        dragDrop(source, sx, sy, SWT.BUTTON1, tx, ty);
    }

    @Action
    public void dragDrop(IFigure source, int sx, int sy, int mask, int tx, int ty) {
        checkFigure(source);
        checkFigureShowing(source);
        dragDropInternal(source, sx, sy, mask, tx, ty);
    }

    /**
	 * <p>
	 * <b>Note:</b> Does not check arguments.
	 * 
	 * @param sourceFigure
	 * @param sx
	 * @param sy
	 * @param mask
	 * @param targetFigure
	 * @param tx
	 * @param ty
	 */
    private void dragDropInternal(IFigure sourceFigure, int sx, int sy, int mask, IFigure targetFigure, int tx, int ty) {
        FigureCanvas canvas = findCanvas(sourceFigure);
        Rectangle source = toDisplay(canvas, toAbsolute(sourceFigure, sourceFigure.getBounds()));
        if (sx == -1 && sy == -1) {
            sx = source.width / 2;
            sy = source.height / 2;
        }
        Rectangle target = toDisplay(canvas, toAbsolute(targetFigure, targetFigure.getBounds()));
        if (tx == -1 && ty == -1) {
            tx = target.width / 2;
            ty = target.height / 2;
        }
        displayTester(canvas).dragDrop(source.x + sx, source.y + sy, target.x + tx, target.y + ty, mask);
    }

    /**
	 * Drag from a location within a source rectangle and drop at specified display coordinates.
	 * <p>
	 * <b>Note:</b> Does not check arguments.
	 * 
	 * @param sourceFigure
	 *            the bounding {@link Rectangle} of the drag source
	 * @param sx
	 *            the x coordinate within <code>source</code>.
	 * @param sy
	 *            the y coordinate within <code>source</code>.
	 * @param mask
	 *            zero or more accelerator keys (e.g., {@link SWT#SHIFT})
	 * @param tx
	 *            the x coordinate of the drop target location in display coordinates
	 * @param ty
	 *            the y coordinate of the drop target location in display coordinates
	 */
    private void dragDropInternal(IFigure sourceFigure, int sx, int sy, int mask, int tx, int ty) {
        Rectangle source = getBounds(sourceFigure);
        if (sx == -1 && sy == -1) {
            sx = source.width / 2;
            sy = source.height / 2;
        }
        Canvas canvas = findCanvas(sourceFigure);
        displayTester(canvas).dragDrop(source.x + sx, source.y + sy, tx, ty, mask);
    }

    /**
	 * Clicks a menu item in an {@link IFigure}'s context (pop-up) menu.
	 */
    @Action
    public void clickMenuItem(IFigure figure, ItemPath menuPath) {
        checkFigure(figure);
        clickMenuItemInternal(figure, menuPath);
    }

    /**
	 * Clicks a menu item in a {@link Widget}'s context (pop-up) menu.
	 */
    @Action
    public void clickMenuItem(IFigure figure, String menuPath) {
        clickMenuItem(figure, ItemPath.fromString(menuPath));
    }

    /**
	 * Clicks a menu item in a {@link Widget}'s context (pop-up) menu.
	 */
    @Action
    public void clickMenuItem(IFigure figure, String menuPath, String delimiter) {
        clickMenuItem(figure, ItemPath.fromString(menuPath, delimiter));
    }

    void clickMenuItemInternal(IFigure figure, ItemPath menuPath) {
        CanvasTester canvasTester = CanvasTester.getCanvasTester();
        MenuTester menuTester = MenuTester.getMenuTester();
        click(figure, SWTConstants.POPUP_MASK);
        FigureCanvas canvas = findCanvas(figure);
        Menu menu = canvasTester.getMenu(canvas);
        Assert.assertNotNull(menu);
        menuTester.waitVisible(menu);
        menuTester.clickItem(menu, menuPath);
    }

    void clickMenuItemInternal(ItemPath path) {
        Control control = DisplayTester.getDefault().getCursorControl();
        if (control == null || !(control instanceof FigureCanvas)) throw new ActionFailedException("cursor is not over a FigureCanvas");
        FigureCanvasTester canvasTester = FigureCanvasTester.getFigureCanvasTester();
        Menu menu = canvasTester.getMenu(control);
        if (menu == null) throw new ActionFailedException("no menu");
        DisplayTester.getDefault().click(SWTConstants.POPUP_MASK);
        MenuTester menuTester = MenuTester.getMenuTester();
        menuTester.waitVisible(menu);
        menuTester.clickItem(menu, path);
    }

    /**
	 * @param figure
	 *            the {@link IFigure}
	 * @throws IllegalArgumentException
	 *             if figure is null
	 */
    protected void checkFigure(IFigure figure) {
        if (figure == null) throw new IllegalArgumentException("figure is null");
    }

    /**
	 * @param figure
	 *            the {@link IFigure}
	 * @throws IllegalArgumentException
	 *             if figure is not showing
	 * @see IFigure#isShowing()
	 */
    protected void checkFigureShowing(IFigure figure) {
        if (!figure.isShowing()) throw new IllegalArgumentException("figure not showing");
    }

    /**
	 * Runs a {@link Runnable} while the receiver's {@link AbstractTester} is set to automatically
	 * wait after generating an event for the input event queue to be empty.
	 * 
	 * @param runnable
	 *            the Runnable to {@link Runnable}
	 * @see AbstractTester#setAutoWaitForIdle(boolean)
	 */
    protected void autoWaitForIdleDuring(Runnable runnable) {
        runnable.run();
    }
}
