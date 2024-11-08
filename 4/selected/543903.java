package GUI;

import Controller.CrumbData;
import Controller.CrumbData.CrumbType;
import Controller.CrumbData.Parameter;
import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nmills
 */
public class CrumbPanel extends JLabel implements Serializable {

    static final int HITBOX_BUFFER = 12;

    static final int PARAMETER_SPACING = 6;

    static final int DEFAULT_CRUMB_WIDTH = 80;

    static final int DEFAULT_CRUMB_HEIGHT = 34;

    static final int SMALLER_CRUMB_HEIGHT = 27;

    static final int MINIMUM_CRUMB_HOLE_WIDTH = 27;

    static final int TINY_SPACE = 1;

    static int heightLastCrumb = 0;

    Point myPoint;

    public void setGuiController(GuiController guiController) {
        this.guiController = guiController;
        for (Component c : this.getComponents()) {
            if (c.getClass() == ParameterHole.class) {
                ParameterHole paramHole = (ParameterHole) c;
                paramHole.guiController = this.guiController;
                paramHole.addDocumentListenerThings();
            }
        }
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    Workspace workspace;

    transient GuiController guiController;

    CrumbData crumbData;

    CrumbPanel above = null;

    CrumbPanel below = null;

    CrumbPanel parent = null;

    CrumbPanel child = null;

    CrumbPanel begin = null;

    CrumbPanel side = null;

    CrumbPanel end = null;

    ParameterHole parentHole = null;

    Hitbox hitbox;

    List<ParameterHole> parameterHoles = new ArrayList<ParameterHole>();

    Boolean isTopLinkable = true;

    Boolean isBottomLinkable = true;

    Boolean isParamLinkable = false;

    Boolean isParamCrumb = false;

    Boolean isFatherCrumb = false;

    Boolean isBeginCrumb = false;

    Boolean isSideCrumb = false;

    Boolean isEndCrumb = false;

    Boolean useCrumbImage = false;

    public CrumbPanel(Workspace workspace, CrumbData crumbData, GuiController guiController) {
        this.workspace = workspace;
        this.crumbData = crumbData;
        this.guiController = guiController;
        if (crumbData.getName().equals("Side")) {
            this.isSideCrumb = true;
            this.isTopLinkable = false;
            this.isBottomLinkable = false;
            if (useCrumbImage == false) {
            } else {
                setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/purple.png")));
            }
        } else if (crumbData.getName().equals("End")) {
            this.isEndCrumb = true;
            this.isTopLinkable = false;
            if (useCrumbImage == false) {
            } else {
                setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/purple.png")));
            }
        } else if (crumbData.getName().equals("Father")) {
            isTopLinkable = false;
            isFatherCrumb = true;
            if (useCrumbImage == false) {
            } else {
                setIcon(new javax.swing.ImageIcon(getClass().getResource("/GUI/button.png")));
            }
        } else if (crumbData.getName().equals("While") || crumbData.getName().equals("Loop") || crumbData.getName().equals("If") || crumbData.getName().equals("Else If") || crumbData.getName().equals("Move Servo")) {
            isParamLinkable = true;
        } else if (crumbData.getType() == CrumbData.CrumbType.Conditional || crumbData.getType() == CrumbData.CrumbType.Expression) {
            this.isTopLinkable = false;
            this.isBottomLinkable = false;
            this.isParamCrumb = true;
            this.isParamLinkable = true;
            this.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.RAISED));
        }
        if (this.getCrumbData().getCategory().equalsIgnoreCase("Variables") || this.getCrumbData().getCategory().equalsIgnoreCase("Functions")) {
            Color varColor = new Color(172, 225, 238);
            this.setForeground(varColor);
        } else this.setForeground(Color.white);
        this.setOpaque(true);
        layoutParameters();
        updateHitBox();
        Color color1 = new Color(41, 74, 73);
        Color color2 = new Color(130, 191, 147);
        Color color4 = new Color(189, 110, 70);
        Color color5 = new Color(69, 59, 44);
        Color color6 = new Color(74, 37, 31);
        Color color8 = new Color(120, 114, 72);
        Color color9 = new Color(178, 148, 71);
        Color color10 = new Color(224, 156, 72);
        Color color11 = new Color(86, 22, 75);
        Color color12 = new Color(12, 54, 1);
        Color color13 = new Color(48, 90, 61);
        if (crumbData.getCategory().equalsIgnoreCase("led")) {
            this.setBackground(color2);
        } else if (crumbData.getCategory().equalsIgnoreCase("control")) {
            this.setBackground(color1);
        } else if (crumbData.getCategory().equalsIgnoreCase("dc motor")) {
            this.setBackground(Color.MAGENTA);
        } else if (crumbData.getCategory().equalsIgnoreCase("servo")) {
            this.setBackground(color6);
        } else if (crumbData.getCategory().equalsIgnoreCase("conditionals")) {
            this.setBackground(color4);
        } else if (crumbData.getCategory().equalsIgnoreCase("variables")) {
            this.setBackground(color8);
        } else if (crumbData.getCategory().equalsIgnoreCase("expressions")) {
            this.setBackground(color11);
        } else if (crumbData.getCategory().equalsIgnoreCase("functions")) {
            this.setBackground(color9);
        } else if (crumbData.getCategory().equalsIgnoreCase("arduino funcs")) {
            this.setBackground(color12);
        } else if (crumbData.getCategory().equalsIgnoreCase("read/write")) {
            this.setBackground(color13);
        } else {
            this.setBackground(color5);
        }
        if (this.getCrumbData().getType().equals(CrumbType.Statement)) {
        } else if (this.getCrumbData().getType().equals(CrumbType.Conditional)) {
        } else if (this.getCrumbData().getType().equals(CrumbType.Control)) {
            this.isBeginCrumb = true;
        } else if (this.getCrumbData().getType().equals(CrumbType.Expression)) {
        } else if (this.getCrumbData().getType().equals(CrumbType.Other)) {
        }
        addCrumbMouseListeners();
    }

    void addCrumbMouseListeners() {
        addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                crumbMousePressed(evt);
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

            public void mouseDragged(java.awt.event.MouseEvent evt) {
                crumbMouseDragged(evt);
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                crumbMouseReleased(evt);
            }
        });
    }

    private void layoutParameters() {
        if (crumbData.getName().equalsIgnoreCase("side")) {
            this.setText("");
            this.setSize(GuiController.LOOP_SHIFT, GuiController.LOOP_GAP);
            return;
        } else if (crumbData.getName().equalsIgnoreCase("end")) {
            this.setText("");
            this.setSize(DEFAULT_CRUMB_WIDTH, SMALLER_CRUMB_HEIGHT);
            return;
        } else if (crumbData.getType() == CrumbData.CrumbType.Conditional || crumbData.getType() == CrumbData.CrumbType.Expression) {
            this.setSize(DEFAULT_CRUMB_WIDTH, SMALLER_CRUMB_HEIGHT);
        } else {
            this.setSize(DEFAULT_CRUMB_WIDTH, DEFAULT_CRUMB_HEIGHT);
        }
        if (crumbData.getParameters().isEmpty()) {
            if (crumbData.getDisplayName() != null) {
                this.setText(crumbData.getDisplayName());
            } else {
                this.setText(crumbData.getName());
            }
            this.setSize(this.getPreferredSize().width + PARAMETER_SPACING * 2, this.getHeight());
            if (this.getWidth() < DEFAULT_CRUMB_WIDTH && this.getCrumbData().getType() == CrumbType.Control) this.setSize(DEFAULT_CRUMB_WIDTH, this.getHeight());
            this.setHorizontalAlignment(CENTER);
            return;
        }
        FontMetrics fm = this.getFontMetrics(this.getFont());
        int x = PARAMETER_SPACING;
        int y = this.getHeight() / 2 - fm.getHeight() / 2;
        for (Parameter p : crumbData.getParameters()) {
            if ("".equals(p.getDesc())) {
                continue;
            }
            String[] desc = p.getDesc().split(" ");
            for (String s : desc) {
                if (s.equals(p.getName())) {
                    ParameterHole ph = new ParameterHole(0, 0, 0, 0, p, this, guiController);
                    ph.setLocation(x, (this.getHeight() - ph.getPreferredSize().height) / 2);
                    int compWidth = ph.getPreferredSize().width + 1;
                    if (compWidth < MINIMUM_CRUMB_HOLE_WIDTH) {
                        compWidth = MINIMUM_CRUMB_HOLE_WIDTH;
                    }
                    ph.setSize(compWidth, ph.getPreferredSize().height);
                    ph.updateHitBox();
                    ph.setBackground(Color.WHITE);
                    this.add(ph);
                    this.parameterHoles.add(ph);
                    x += ph.getWidth() + PARAMETER_SPACING;
                } else {
                    JLabel label = new JLabel();
                    label.setText(s);
                    label.setLocation(x, y);
                    label.setSize(label.getPreferredSize());
                    label.setForeground(this.getForeground());
                    this.add(label);
                    x += label.getWidth() + PARAMETER_SPACING;
                }
            }
        }
        this.setSize(x + PARAMETER_SPACING, this.getHeight());
    }

    void relayoutParameters(boolean relayParentCrumb) {
        int xloc = 0;
        int yloc = 0;
        int compWidth = 0;
        int compHeight = 0;
        boolean firstcomponent = true;
        for (Component c : this.getComponents()) {
            if (firstcomponent) {
                xloc = c.getLocation().x;
                yloc = c.getLocation().y;
                firstcomponent = false;
            }
            if (c.getClass() == ParameterHole.class) {
                ParameterHole paramHole = (ParameterHole) c;
                yloc = c.getLocation().y;
                paramHole.setLocation(xloc, yloc);
                if (paramHole.parameterCrumb != null) {
                    compWidth = paramHole.parameterCrumb.getWidth();
                    paramHole.setVisible(false);
                    Point paramPoint = new Point(this.getLocation().x, this.getLocation().y);
                    paramPoint.translate(xloc, yloc);
                    paramHole.parameterCrumb.setLocation(paramPoint.x, paramPoint.y);
                    if (!paramHole.parameterCrumb.parameterHoles.isEmpty()) {
                        paramHole.parameterCrumb.relayoutParameters(false);
                    }
                } else {
                    compWidth = paramHole.getPreferredSize().width + 1;
                    if (compWidth < MINIMUM_CRUMB_HOLE_WIDTH) {
                        compWidth = MINIMUM_CRUMB_HOLE_WIDTH;
                    }
                    paramHole.setVisible(true);
                }
                compHeight = paramHole.getPreferredSize().height;
                paramHole.setSize(compWidth, compHeight);
                paramHole.updateHitBox();
                xloc += compWidth + PARAMETER_SPACING;
            } else {
                yloc = c.getLocation().y;
                c.setLocation(xloc, yloc);
                xloc += c.getWidth() + PARAMETER_SPACING;
            }
        }
        if (xloc != PARAMETER_SPACING) {
            this.setSize(xloc + PARAMETER_SPACING, this.getHeight());
        }
        if (this.parentHole != null && this.parentHole.parentCrumb != null && relayParentCrumb == true) {
            this.parentHole.parentCrumb.relayoutParameters(true);
        }
    }

    private void updateHitBox() {
        Point topLeft = new Point(this.getLocation());
        topLeft.translate(0, -1 * HITBOX_BUFFER);
        Point bottomRight = new Point(this.getLocation());
        bottomRight.translate(this.getWidth(), this.getHeight() + HITBOX_BUFFER);
        if (hitbox == null) {
            if (crumbData.getType().toString().equalsIgnoreCase("Conditional")) {
                this.hitbox = new Hitbox(topLeft, bottomRight, Hitbox.HitboxType.conditional);
            } else {
                this.hitbox = new Hitbox(topLeft, bottomRight, Hitbox.HitboxType.standard);
            }
        } else {
            hitbox.setTopLeft(topLeft);
            hitbox.setBottomRight(bottomRight);
        }
        if (!this.parameterHoles.isEmpty()) {
            for (ParameterHole param : parameterHoles) {
                param.updateHitBox();
            }
        }
    }

    public void snap() {
        if (this.above != null) {
            Point abovePoint = above.getLocation();
            abovePoint.translate(0, above.getHeight() + TINY_SPACE - GuiController.TAB_HEIGHT);
            moveMeAndAllConnected(abovePoint);
        } else if (this.parent != null) {
            Point abovePoint = parent.getLocation();
            abovePoint.translate(GuiController.LOOP_SHIFT, parent.getHeight() + TINY_SPACE - GuiController.TAB_HEIGHT);
            moveMeAndAllConnected(abovePoint);
        } else {
            Point thisPoint = this.getLocation();
            moveMeAndAllConnected(thisPoint);
        }
    }

    public void snapAllCrumbsInGroup() {
        CrumbPanel theFirstCrumb = this.findFirstCrumb();
        Point firstPoint = theFirstCrumb.getLocation();
        theFirstCrumb.moveMeAndAllConnected(firstPoint);
    }

    public void moveMeAndAllConnected(Point location) {
        this.setLocation(location);
        updateHitBox();
        if (parameterHoles.isEmpty() == false) {
            for (ParameterHole parameterHole : parameterHoles) {
                if (parameterHole.parameterCrumb != null) {
                    Point paramHolePoint = new Point(location.x, location.y);
                    paramHolePoint.translate(parameterHole.getLocation().x, parameterHole.getLocation().y);
                    parameterHole.parameterCrumb.setLocation(paramHolePoint);
                    parameterHole.parameterCrumb.bringMeAndAllConnectedToFront();
                    if (!parameterHole.parameterCrumb.parameterHoles.isEmpty()) {
                        parameterHole.parameterCrumb.moveMeAndAllConnected(paramHolePoint);
                    }
                }
            }
        }
        heightLastCrumb = this.getHeight();
        boolean hasChild = false;
        if (below != null) {
            location.translate(0, this.getHeight() + TINY_SPACE - GuiController.TAB_HEIGHT);
            below.moveMeAndAllConnected(location);
        }
        if (side != null) {
            location.translate(0, this.getHeight());
            side.moveMeAndAllConnected(location);
        }
        if (child != null) {
            hasChild = true;
            location.translate(GuiController.LOOP_SHIFT, -GuiController.TAB_HEIGHT + TINY_SPACE);
            child.moveMeAndAllConnected(location);
        }
        if (end != null) {
            int horizShift = 0;
            int vertShift = 0;
            if (hasChild == true) {
                horizShift = -GuiController.LOOP_SHIFT;
                vertShift = heightLastCrumb + TINY_SPACE;
            } else {
                vertShift = GuiController.LOOP_GAP;
            }
            location.translate(horizShift, vertShift);
            end.moveMeAndAllConnected(location);
            int sideHeight = this.end.getLocation().y - (this.getLocation().y + this.getHeight());
            int sideWidth = 20 - TINY_SPACE;
            this.side.setSize(sideWidth, sideHeight);
        }
    }

    private void crumbMousePressed(java.awt.event.MouseEvent evt) {
        workspace.removePopupMenu();
        if (evt.getButton() == 3) {
            if (!(this.isFatherCrumb) && (this.begin == null || !(this.begin.isFatherCrumb))) {
                workspace.popupMenu(this, evt);
            }
        } else {
            myPoint = evt.getPoint();
            bringMeAndAllConnectedToFront();
        }
    }

    public void deleteHelper() {
        if (isBeginCrumb) {
            delete(end);
            delete(side);
        }
        if (isEndCrumb) {
            delete(begin);
            delete(begin.side);
        }
        if (isSideCrumb) {
            delete(begin);
            delete(begin.end);
        }
        delete(this);
        snapAllCrumbsInGroup();
    }

    private void delete(CrumbPanel crumb) {
        CrumbPanel theLastCrumb;
        if (crumb.parentHole != null) {
            crumb.parentHole.parameterCrumb = null;
            crumb.parentHole.parentCrumb.relayoutParameters(true);
        }
        if (crumb.above != null) {
            if (crumb.below != null) {
                crumb.getAbove().setBelow(crumb.below);
                crumb.getBelow().setAbove(crumb.above);
            } else if (crumb.child != null) {
                crumb.getAbove().setBelow(crumb.child);
                crumb.getChild().setParent(null);
                crumb.getChild().setAbove(crumb.above);
                if (crumb.end != null && crumb.end.below != null) {
                    theLastCrumb = crumb.child.findLastCrumb();
                    theLastCrumb.setBelow(crumb.end.below);
                    crumb.end.below.setAbove(theLastCrumb);
                }
            } else if (crumb.end != null && crumb.end.below != null) {
                crumb.getAbove().setBelow(crumb.end.below);
                crumb.end.getBelow().setAbove(crumb.above);
            } else {
                crumb.getAbove().setBelow(null);
            }
        }
        if (crumb.parent != null) {
            if (crumb.below != null) {
                crumb.getparent().setChild(crumb.below);
                crumb.getBelow().setAbove(null);
                crumb.getBelow().setParent(crumb.parent);
            } else if (crumb.child != null) {
                crumb.getparent().setChild(crumb.child);
                crumb.getChild().setParent(crumb.parent);
                if (crumb.end != null && crumb.end.below != null) {
                    theLastCrumb = crumb.child.findLastCrumb();
                    theLastCrumb.setBelow(crumb.end.below);
                    crumb.end.below.setAbove(theLastCrumb);
                }
            } else if (crumb.end != null && crumb.end.below != null) {
                crumb.getparent().setChild(crumb.end.below);
                crumb.end.getBelow().setParent(crumb.parent);
            } else {
                crumb.getparent().setChild(null);
            }
        }
        Workspace w = (Workspace) workspace;
        w.deleteCrumb(crumb);
    }

    void bringMeAndAllConnectedToFront() {
        workspace.setComponentZOrder(this, 0);
        if (below != null) {
            below.bringMeAndAllConnectedToFront();
        }
        if (child != null) {
            child.bringMeAndAllConnectedToFront();
        }
        if (side != null) {
            side.bringMeAndAllConnectedToFront();
        }
        if (end != null) {
            end.bringMeAndAllConnectedToFront();
        }
    }

    public String toString() {
        return this.hitbox.getTopLeft().toString();
    }

    private void crumbMouseDragged(java.awt.event.MouseEvent evt) {
        if (this.crumbData.getName().equals("End") || this.crumbData.getName().equals("Side")) {
            return;
        }
        moveMeAndAllConnected(getLocationOnParent(evt));
    }

    private void crumbMouseReleased(java.awt.event.MouseEvent evt) {
        Workspace w = (Workspace) workspace;
        if (this.crumbData.getType().equals(CrumbType.Conditional) || this.crumbData.getType().equals(CrumbType.Expression)) {
            w.checkParamHits(this);
        } else {
            w.checkForHits(this, true);
            CrumbPanel theLastCrumb = this.findLastCrumb();
            if (this != theLastCrumb) {
                w.checkForHits(theLastCrumb, false);
            }
        }
    }

    public CrumbPanel findLastCrumb() {
        CrumbPanel lastCrumb;
        if (below != null) {
            lastCrumb = below.findLastCrumb();
        } else if (end != null) {
            lastCrumb = end.findLastCrumb();
        } else {
            lastCrumb = this;
        }
        return lastCrumb;
    }

    public CrumbPanel findFirstCrumb() {
        String name = this.getName();
        CrumbPanel firstCrumb;
        if (above != null) {
            firstCrumb = above.findFirstCrumb();
        } else if (parent != null) {
            firstCrumb = parent.findFirstCrumb();
        } else if (begin != null) {
            firstCrumb = begin.findFirstCrumb();
        } else {
            firstCrumb = this;
        }
        return firstCrumb;
    }

    public Point getLocationOnParent(java.awt.event.MouseEvent e) {
        Point point = workspace.getLocationOnScreen();
        double x = e.getXOnScreen() - point.getX() - myPoint.getX();
        double y = e.getYOnScreen() - point.getY() - myPoint.getY();
        point.setLocation(x, y);
        return point;
    }

    @Override
    public void setLocation(int x, int y) {
        super.setLocation(x, y);
        updateHitBox();
    }

    @Override
    public void setLocation(Point p) {
        super.setLocation(p);
        updateHitBox();
    }

    public CrumbPanel getAbove() {
        return this.above;
    }

    public CrumbPanel getBelow() {
        return this.below;
    }

    public CrumbPanel getparent() {
        return this.parent;
    }

    public CrumbPanel getChild() {
        return this.child;
    }

    public Hitbox getHitbox() {
        return hitbox;
    }

    public CrumbPanel getEnd() {
        return end;
    }

    public void setAbove(CrumbPanel above) {
        this.above = above;
        this.snap();
    }

    public void setBelow(CrumbPanel below) {
        this.below = below;
    }

    public void setParent(CrumbPanel parent) {
        this.parent = parent;
    }

    public void setChild(CrumbPanel child) {
        this.child = child;
    }

    public CrumbData getCrumbData() {
        return crumbData;
    }

    public List<ParameterHole> getParams() {
        return parameterHoles;
    }
}
