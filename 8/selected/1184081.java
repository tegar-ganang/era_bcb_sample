package org.gvt.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.graphics.Color;
import org.gvt.ChisioMain;
import org.gvt.model.NodeModel;
import org.gvt.model.BioPAXGraph;
import org.gvt.model.biopaxl3.Actor;
import org.gvt.model.biopaxl3.ChbComplex;
import org.patika.mada.graph.Node;
import org.patika.mada.graph.GraphObject;
import org.patika.mada.algorithm.BFS;
import java.util.*;

/**
 *
 * @author Ozgun Babur
 *
 * Copyright: Bilkent Center for Bioinformatics, 2007 - present
 */
public class DebugButtonAction extends Action {

    ChisioMain main;

    /**
	 * Constructor
	 */
    public DebugButtonAction(ChisioMain main) {
        super("Debug button");
        this.setToolTipText("Debug Button - You can run any\n" + "code after pressing this button.\n" + "Insert your code in the class\n" + "DebugButtonAction");
        setImageDescriptor(ImageDescriptor.createFromFile(ChisioMain.class, "icon/bug.png"));
        this.main = main;
    }

    public void run() {
        debug();
    }

    public void debug() {
        ScrollingGraphicalViewer viewer = main.getViewer();
        Iterator selectedObjects = ((IStructuredSelection) viewer.getSelection()).iterator();
        System.out.println("");
        while (selectedObjects.hasNext()) {
            Object o = ((EditPart) selectedObjects.next()).getModel();
            if (o instanceof NodeModel) {
                NodeModel model = (NodeModel) o;
                if (model instanceof Actor) {
                    Actor actor = (Actor) model;
                    System.out.println(actor.getEntity().l3pe.getRDFId());
                } else if (o instanceof ChbComplex) {
                    ChbComplex cmp = (ChbComplex) o;
                    System.out.println(cmp.getComplex().getRDFId());
                }
            }
        }
    }
}
