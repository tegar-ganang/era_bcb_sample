package canvaswork;

import geomerative.*;
import java.util.Set;
import processing.core.PApplet;

/**
 *
 * @author jonatanhilden
 */
class Grob implements Selectable {

    Main main;

    private RShape shp;

    private Group parent;

    public RPoint relativePosition;

    private RPoint[] handles;

    private RShape[] sides;

    int object_type;

    int xpos, ypos;

    int cx, cy;

    int size;

    int width;

    int height;

    float scale = 1.0f;

    int mirror = 1;

    int rotation;

    int fillColor;

    int overColor = 150;

    int pressedColor = 0;

    boolean isDraggable = true;

    boolean intersects = false;

    Grob(Main main, Group parent, int id, int x, int y, int s, int r, int clr) {
        this.main = main;
        this.parent = parent;
        object_type = id;
        xpos = x;
        ypos = y;
        relativePosition = new RPoint();
        size = s;
        width = s;
        height = s;
        rotation = r;
        fillColor = clr;
        overColor = main.color(main.hue(clr), main.saturation(clr), ((main.brightness(clr)) / 2));
    }

    void update() {
        createShp();
    }

    void createShp() {
        getRelativePosition().x = 0;
        getRelativePosition().y = 0;
        if (object_type == 0) {
            shp = RShape.createRectangle(0, 0, width, height);
            sides = new RShape[4];
        } else if (object_type == 1) {
            shp = new RShape();
            sides = new RShape[5];
            shp.addMoveTo(0, 0);
            shp.addBezierTo(width * 0.5522847498f, 0, width, height * 0.5522847498f, width, height);
            shp.addLineTo(0, width);
            shp.addClose();
        } else if (object_type == 2) {
            shp = new RShape();
            sides = new RShape[3];
            shp.addLineTo(0, 0 + height);
            shp.addLineTo(0 + width, 0 + height);
            shp.addClose();
        } else if (object_type == 3) {
            shp = new RShape();
            sides = new RShape[3];
            shp.addLineTo(0, height);
            shp.addLineTo(height * (Main.sqrt(3) / 2), height / 2);
            shp.addClose();
        } else {
            Main.println("invalid object ID " + object_type);
        }
    }

    void setupTransform() {
        RPoint rotationCenter = main.getRotationCenter();
        getShape().scale(scale);
        getShape().scale(mirror, 1, getCenter());
        getRelativePosition().rotate((PApplet.radians(rotation)));
        getRelativePosition().translate(getXpos(), getYpos());
        getShape().translate(relativePosition);
        getShape().rotate((PApplet.radians(rotation)), getCenter());
        handles = getShape().getHandles();
        for (int i = 0; i < handles.length - 1; i++) {
            RShape s = new RShape();
            s.addMoveTo(handles[i]);
            s.addLineTo(handles[i + 1]);
            sides[i] = s;
        }
    }

    void draw() {
        main.fill(fillColor);
        main.stroke(fillColor);
        setOverColor();
        setupTransform();
        if (isDraggable) {
        }
        if (!isDraggable) {
            if (isSelected()) {
                main.fill(fillColor);
                main.stroke(fillColor);
            } else if (main.isOver(this) || isTemporarySelected()) {
                main.fill(overColor);
                main.stroke(fillColor);
            } else {
                main.fill(0);
                main.stroke(fillColor);
            }
        } else {
            if (isSelected()) {
                main.noFill();
                if (isDraggingGrob()) {
                    main.text(relativePosition.x + " " + relativePosition.y, xpos, ypos);
                }
                main.stroke(fillColor);
            } else if (main.isOver(this) || isTemporarySelected()) {
                main.fill(overColor);
                main.stroke(fillColor);
            } else {
                main.fill(fillColor);
                main.stroke(fillColor);
            }
        }
        RG.shape(getShape());
        main.stroke(main.hue(getFillColor() - getFillColor()), 100, ((main.brightness(getFillColor()))));
        if (main.dragGrobSelectedSideNr != -1 && !(main.dragGrobSelectedSideNr > sides.length) && isDraggingGrob()) {
            RG.shape(sides[main.dragGrobSelectedSideNr]);
        }
        if (main.closestGrobSideNr != -1 && main.snaptargetGrob == this) {
            RG.shape(sides[main.closestGrobSideNr]);
        }
    }

    void grobSelect() {
        if (main.isOver(this)) {
            if (isDraggable) {
            }
            if (main.shiftPressed) {
                if (!isSelected()) {
                    main.select(this);
                } else {
                    main.deselect(this);
                }
            } else if (main.selectionSize() <= 1 || !isSelected()) {
                main.singleSelect(this);
            }
        }
    }

    boolean isSelected() {
        return main.isSelected(this);
    }

    boolean isTemporarySelected() {
        return main.isTemporarySelected(this);
    }

    public boolean isDraggingGrob() {
        return (main.dragGrob == this);
    }

    public boolean isGroup() {
        return false;
    }

    public void setParent(Group parent) {
        this.parent = parent;
    }

    public Group getParent() {
        return parent;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public boolean hasChild(Grob grob) {
        return false;
    }

    public boolean hasChildren() {
        return false;
    }

    public RShape getShape() {
        return shp;
    }

    public void setShape(RShape shape) {
        shp = shape;
    }

    public int getSize() {
        return size;
    }

    public int getWidth() {
        return width;
    }

    public void setScale(float scale) {
        this.scale = Main.constrain(scale, 0.1f, 9.0f);
    }

    public float getScale() {
        return scale;
    }

    public void setSize(int size) {
        this.size = Main.constrain(size, main.gridSize, main.canvasWidth);
    }

    public boolean isIsDraggable() {
        return isDraggable;
    }

    public void setIsDraggable(boolean isDraggable) {
        this.isDraggable = isDraggable;
    }

    boolean contains(RGeomElem shape) {
        return (shape.intersects(getShape()) || shape.containsBounds(getShape()));
    }

    boolean intersects(RGeomElem shape) {
        return (shape.intersects(getShape()));
    }

    boolean intersects(Grob grob) {
        return (grob.getShape().intersects(getShape()));
    }

    int sideIntersects(RGeomElem shape) {
        int sidenum = 100;
        for (int i = 0; i < sides.length; i++) {
            if (sides[i] != null) {
                if (sides[i].intersects(shape)) {
                    sidenum = i;
                }
            }
        }
        return sidenum;
    }

    public RShape[] getSides() {
        return sides;
    }

    public RShape getSide(int s) {
        if (s <= sides.length - 1 && s != -1) {
            return sides[s];
        } else {
            return sides[0];
        }
    }

    public int selectSide(int accuracy) {
        float[] distances;
        distances = new float[sides.length];
        int selectedSideNr = -1;
        for (int i = 0; i < sides.length; i++) {
            if (sides[i] != null) {
                distances[i] = main.distanceToInfiniteLine(main.mouseX, main.mouseY, Math.round(sides[i].getHandles()[0].x), Math.round(sides[i].getHandles()[0].y), Math.round(sides[i].getHandles()[1].x), Math.round(sides[i].getHandles()[1].y));
            } else return selectedSideNr;
        }
        float dmin = Main.min(distances);
        for (int j = 0; j < distances.length; j++) {
            if (dmin == distances[j] && dmin < accuracy) {
                selectedSideNr = j;
            }
        }
        return selectedSideNr;
    }

    public RPoint[] getSideHandles(int s) {
        RPoint[] sideHandles;
        sideHandles = new RPoint[2];
        if (s != -1) {
            sideHandles[0] = handles[s];
            sideHandles[1] = handles[s + 1];
        }
        return sideHandles;
    }

    public RPoint[] getHandles() {
        return handles;
    }

    public RPoint getHandle(int h) {
        if (h <= sides.length - 1 && h != -1) {
            return handles[h];
        } else {
            return handles[0];
        }
    }

    public int getXpos() {
        return xpos;
    }

    public void setXpos(int x) {
        this.xpos = x;
    }

    public int getYpos() {
        return ypos;
    }

    public void setYpos(int ypos) {
        this.ypos = ypos;
    }

    void setPosition(int x, int y) {
        xpos = x;
        ypos = y;
    }

    public RPoint getRelativePosition() {
        return this.relativePosition;
    }

    void setRelativePosition(int x, int y) {
        relativePosition.x = x;
        relativePosition.y = y;
    }

    void translateRelativePosition(int x, int y) {
        relativePosition.translate(x, y);
    }

    public RPoint getCenter() {
        return getShape().getCenter();
    }

    public int getFillColor() {
        return fillColor;
    }

    public void setFillColor(float h, float s, float b) {
        this.fillColor = main.color(h, s, b);
    }

    public void setFillColor(int fill) {
        this.fillColor = fill;
    }

    public void setOverColor() {
        this.overColor = main.color(main.hue(getFillColor()), main.saturation(getFillColor()), ((main.brightness(getFillColor())) / 1.8f));
    }

    public void resize(int sizeChange) {
        setSize(getSize() + sizeChange);
    }

    public void scale(float s) {
        setScale(getScale() + s);
    }

    public void rotateGrob(int degrees) {
        rotation = rotation + degrees;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int degrees) {
        this.rotation = degrees;
    }

    void mirrorGrob() {
        mirror = -mirror;
    }

    public Grob copyGrob(Group newParent, int x, int y, int xx, int yy) {
        Grob grob = new Grob(main, newParent, object_type, ((x / main.gridSize) * main.gridSize) + xpos - xx, ((y / main.gridSize) * main.gridSize) + ypos - yy, size, rotation, fillColor);
        grob.setScale(getScale());
        grob.mirror = mirror;
        grob.update();
        return grob;
    }

    public Grob copyGrob(Group newParent, int colorMod) {
        Grob grob = new Grob(main, newParent, object_type, xpos, ypos, size, rotation, fillColor - fillColor / colorMod);
        grob.rotation = rotation;
        grob.setScale(getScale());
        grob.mirror = mirror;
        return grob;
    }

    void addToSelection(Set<Grob> selection, RGeomElem shape) {
        if (contains(shape)) {
            selection.add(this);
        }
    }

    void removeFromSelection(Set<Grob> selection, RGeomElem shape) {
        if (contains(shape)) {
            selection.remove(this);
        }
    }
}
