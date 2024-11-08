package org.dyno.visual.swing.base;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.dyno.visual.swing.VisualSwingPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class ImageSelectionDialog extends Dialog {

    private TreeViewer view;

    private Label label;

    private class ProjectTreeContent implements ITreeContentProvider {

        public Object[] getChildren(Object parentElement) {
            try {
                if (parentElement instanceof IJavaProject) {
                    IJavaProject prj = (IJavaProject) parentElement;
                    IJavaElement[] children = prj.getChildren();
                    List<Object> list = new ArrayList<Object>();
                    for (IJavaElement jElement : children) {
                        if (jElement instanceof IPackageFragmentRoot) {
                            IPackageFragmentRoot pkgRoot = (IPackageFragmentRoot) jElement;
                            if (!pkgRoot.isArchive()) list.add(jElement);
                        }
                    }
                    return list.toArray();
                } else if (parentElement instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot packageRoot = (IPackageFragmentRoot) parentElement;
                    IJavaElement[] children = packageRoot.getChildren();
                    Object[] nonJavaResources = packageRoot.getNonJavaResources();
                    List<Object> list = new ArrayList<Object>();
                    if (children != null) {
                        for (Object child : children) {
                            list.add(child);
                        }
                    }
                    if (nonJavaResources != null) {
                        for (Object resource : nonJavaResources) {
                            if (resource instanceof IFile) {
                                IFile file = (IFile) resource;
                                String name = file.getName();
                                name = name.toLowerCase();
                                if (name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".jpg")) {
                                    list.add(file);
                                }
                            }
                        }
                    }
                    return list.toArray();
                } else if (parentElement instanceof IPackageFragment) {
                    IPackageFragment pkg = (IPackageFragment) parentElement;
                    Object[] nonJavaResources = pkg.getNonJavaResources();
                    List<Object> list = new ArrayList<Object>();
                    for (Object resource : nonJavaResources) {
                        if (resource instanceof IFile) {
                            IFile file = (IFile) resource;
                            String name = file.getName();
                            if (name != null) {
                                name = name.toLowerCase();
                                if (name.endsWith(".gif") || name.endsWith(".png") || name.endsWith(".jpg")) {
                                    list.add(file);
                                }
                            }
                        }
                    }
                    return list.toArray();
                }
            } catch (Exception e) {
                VisualSwingPlugin.getLogger().error(e);
            }
            return new Object[0];
        }

        public Object getParent(Object element) {
            return null;
        }

        public boolean hasChildren(Object element) {
            try {
                IJavaElement[] children = null;
                if (element instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot packageRoot = (IPackageFragmentRoot) element;
                    children = packageRoot.getChildren();
                    Object[] nonJavaResources = packageRoot.getNonJavaResources();
                    List<Object> list = new ArrayList<Object>();
                    if (children != null) {
                        for (Object child : children) {
                            list.add(child);
                        }
                    }
                    if (nonJavaResources != null) {
                        for (Object resource : nonJavaResources) {
                            if (resource instanceof IFile) {
                                IFile file = (IFile) resource;
                                String name = file.getName();
                                name = name.toLowerCase();
                                if (name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".jpg")) {
                                    list.add(file);
                                }
                            }
                        }
                    }
                    return !list.isEmpty();
                } else if (element instanceof IPackageFragment) {
                    IPackageFragment pkg = (IPackageFragment) element;
                    Object[] nonJavaResources = pkg.getNonJavaResources();
                    for (Object resource : nonJavaResources) {
                        if (resource instanceof IFile) {
                            IFile file = (IFile) resource;
                            String name = file.getName();
                            if (name != null) {
                                name = name.toLowerCase();
                                if (name.endsWith(".gif") || name.endsWith(".png") || name.endsWith(".jpg")) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }
                return children != null && children.length > 0;
            } catch (Exception e) {
                VisualSwingPlugin.getLogger().error(e);
            }
            return false;
        }

        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    class ProjectLabelProvider extends LabelProvider implements ILabelProvider {

        @Override
        public Image getImage(Object element) {
            if (element == null) return null;
            if (element instanceof IPackageFragmentRoot) {
                return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKFRAG_ROOT);
            } else if (element instanceof IPackageFragment) {
                return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
            } else if (element instanceof IFile) {
                return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJ_FILE);
            }
            return null;
        }

        @Override
        public String getText(Object element) {
            if (element == null) return "";
            if (element instanceof IJavaElement) {
                IJavaElement java = (IJavaElement) element;
                String name = java.getElementName();
                if (name == null || name.trim().length() == 0) {
                    if (java instanceof IPackageFragment) {
                        return "(default package)";
                    }
                }
                return name;
            } else if (element instanceof IFile) {
                IFile file = (IFile) element;
                String name = file.getName();
                return name;
            }
            return element.toString();
        }

        @Override
        public void dispose() {
        }
    }

    public ImageSelectionDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        area.setLayout(layout);
        view = new TreeViewer(area, SWT.BORDER);
        GridData data = new GridData();
        data.widthHint = 350;
        data.heightHint = 200;
        view.getTree().setLayoutData(data);
        view.setUseHashlookup(true);
        view.setContentProvider(new ProjectTreeContent());
        view.setLabelProvider(new ProjectLabelProvider());
        view.setInput(VisualSwingPlugin.getCurrentProject());
        view.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                view_selectionChanged(event);
            }
        });
        view.getTree().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                treeDoubleClicked(e);
            }
        });
        Group group = new Group(area, SWT.NONE);
        group.setText("Preview");
        group.setLayout(new FillLayout());
        data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        data.heightHint = 50;
        group.setLayoutData(data);
        label = new Label(group, SWT.CENTER);
        return parent;
    }

    private static final int NULL_ID = IDialogConstants.CLIENT_ID + 1;

    private static final int IMPORT_ID = IDialogConstants.CLIENT_ID + 2;

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IMPORT_ID, "Import", false);
        createButton(parent, NULL_ID, "Null", false);
        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == NULL_ID) {
            imgFile = null;
            super.okPressed();
        } else if (buttonId == IMPORT_ID) {
            TreeSelection sel = (TreeSelection) view.getSelection();
            if (sel != null && !sel.isEmpty()) {
                Object element = sel.getFirstElement();
                String path = null;
                if (element instanceof IPackageFragmentRoot) {
                    path = ((IPackageFragmentRoot) element).getResource().getRawLocation().toFile().getAbsolutePath();
                } else if (element instanceof IPackageFragment) {
                    path = ((IPackageFragment) element).getResource().getRawLocation().toFile().getAbsolutePath();
                }
                if (path != null) {
                    FileDialog dialog = new FileDialog(getShell());
                    dialog.setText("Import images");
                    dialog.setFilterExtensions(new String[] { "*.png;*.gif;*.jpg" });
                    String imgPath = dialog.open();
                    if (imgPath != null) {
                        copy(imgPath, path);
                    }
                }
            }
        } else {
            super.buttonPressed(buttonId);
        }
    }

    private void copy(String imgPath, String path) {
        try {
            File input = new File(imgPath);
            File output = new File(path, input.getName());
            if (output.exists()) {
                if (!MessageDialog.openQuestion(getShell(), "Overwrite", "There is already an image file " + input.getName() + " under the package.\n Do you really want to overwrite it?")) return;
            }
            byte[] data = new byte[1024];
            FileInputStream fis = new FileInputStream(imgPath);
            BufferedInputStream bis = new BufferedInputStream(fis);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
            int length;
            while ((length = bis.read(data)) > 0) {
                bos.write(data, 0, length);
                bos.flush();
            }
            bos.close();
            fis.close();
            IJavaProject ijp = VisualSwingPlugin.getCurrentProject();
            if (ijp != null) {
                ijp.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
                view.refresh();
                view.expandAll();
            }
        } catch (Exception e) {
            VisualSwingPlugin.getLogger().error(e);
        }
    }

    private boolean buildPath(Object target, Object root, Stack<Object> stack) {
        stack.push(root);
        if (root.equals(target)) {
            return true;
        }
        ITreeContentProvider provider = (ITreeContentProvider) view.getContentProvider();
        Object[] children = provider.getChildren(root);
        for (Object child : children) {
            if (buildPath(target, child, stack)) return true;
        }
        stack.pop();
        return false;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control ctrl = super.createContents(parent);
        if (imgFile != null) {
            IJavaProject prj = VisualSwingPlugin.getCurrentProject();
            Stack<Object> stack = new Stack<Object>();
            if (buildPath(imgFile, prj, stack)) {
                TreePath path = new TreePath(stack.toArray());
                view.expandToLevel(path, 0);
                view.expandAll();
                view.setSelection(new StructuredSelection(imgFile));
            }
        }
        return ctrl;
    }

    private void treeDoubleClicked(MouseEvent e) {
        TreeSelection sel = (TreeSelection) view.getSelection();
        if (sel != null && !sel.isEmpty()) {
            updateButton(sel.getFirstElement());
            if (getButton(IDialogConstants.OK_ID).isEnabled()) {
                buttonPressed(IDialogConstants.OK_ID);
            }
        }
    }

    private void view_selectionChanged(SelectionChangedEvent event) {
        TreeSelection sel = (TreeSelection) event.getSelection();
        Object selected = sel.getFirstElement();
        updateButton(selected);
    }

    private void updateButton(Object selected) {
        if (selected != null) {
            if (selected instanceof IFile) {
                IFile file = (IFile) selected;
                String name = file.getName();
                name = name.toLowerCase();
                if (name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".jpg")) {
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                    getButton(IMPORT_ID).setEnabled(false);
                    this.imgFile = file;
                    updatePicture();
                    return;
                }
            } else if (selected instanceof IPackageFragment) {
                getButton(IMPORT_ID).setEnabled(true);
            } else {
                getButton(IMPORT_ID).setEnabled(false);
            }
        }
        Button btn = getButton(IDialogConstants.OK_ID);
        if (btn != null) btn.setEnabled(false);
    }

    private Image image;

    private void updatePicture() {
        if (imgFile != null) {
            try {
                ImageDescriptor d = ImageDescriptor.createFromURL(imgFile.getLocationURI().toURL());
                if (image != null) image.dispose();
                image = d.createImage();
                label.setImage(image);
            } catch (Exception e) {
                VisualSwingPlugin.getLogger().error(e);
            }
        }
    }

    @Override
    public boolean close() {
        if (image != null) image.dispose();
        return super.close();
    }

    @Override
    protected void cancelPressed() {
        imgFile = null;
        super.cancelPressed();
    }

    public IFile getImageFile() {
        return imgFile;
    }

    public void setImageFile(IFile file) {
        this.imgFile = file;
    }

    private IFile imgFile;
}
