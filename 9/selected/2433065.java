package org.insightech.er.editor.controller.command.common;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.insightech.er.editor.ERDiagramEditor;
import org.insightech.er.editor.controller.command.AbstractCommand;
import org.insightech.er.editor.controller.editpart.element.ERDiagramEditPart;
import org.insightech.er.editor.model.ERDiagram;
import org.insightech.er.editor.model.diagram_contents.element.node.Location;
import org.insightech.er.editor.model.diagram_contents.element.node.NodeElement;
import org.insightech.er.editor.model.diagram_contents.element.node.NodeSet;
import org.insightech.er.editor.model.diagram_contents.element.node.table.ERTable;
import org.insightech.er.editor.model.diagram_contents.element.node.table.column.Column;
import org.insightech.er.editor.model.diagram_contents.not_element.group.ColumnGroup;
import org.insightech.er.editor.model.diagram_contents.not_element.group.GroupSet;

public class PasteCommand extends AbstractCommand {

    private ERDiagram diagram;

    private GraphicalViewer viewer;

    private NodeSet nodeElements;

    private GroupSet columnGroups;

    /**
	 * 貼り付けコマンドを作成します。
	 * 
	 * @param editor
	 * @param nodeElements
	 */
    public PasteCommand(ERDiagramEditor editor, NodeSet nodeElements, int x, int y) {
        this.viewer = editor.getGraphicalViewer();
        this.diagram = (ERDiagram) viewer.getContents().getModel();
        this.nodeElements = nodeElements;
        this.columnGroups = new GroupSet();
        for (NodeElement nodeElement : nodeElements) {
            nodeElement.setLocation(new Location(nodeElement.getX() + x, nodeElement.getY() + y, nodeElement.getWidth(), nodeElement.getHeight()));
            if (nodeElement instanceof ERTable) {
                ERTable table = (ERTable) nodeElement;
                for (Column column : table.getColumns()) {
                    if (column instanceof ColumnGroup) {
                        ColumnGroup group = (ColumnGroup) column;
                        if (!diagram.getDiagramContents().getGroups().contains(group)) {
                            columnGroups.add(group);
                        }
                    }
                }
            }
        }
    }

    /**
	 * 貼り付け処理を実行する
	 */
    @Override
    protected void doExecute() {
        ERDiagramEditPart.setUpdateable(false);
        GroupSet columnGroupSet = this.diagram.getDiagramContents().getGroups();
        for (NodeElement nodeElement : this.nodeElements) {
            this.diagram.addContent(nodeElement);
        }
        for (ColumnGroup columnGroup : this.columnGroups) {
            columnGroupSet.add(columnGroup);
        }
        ERDiagramEditPart.setUpdateable(true);
        this.diagram.changeAll();
        this.setFocus();
    }

    /**
	 * 貼り付け処理を元に戻す
	 */
    @Override
    protected void doUndo() {
        ERDiagramEditPart.setUpdateable(false);
        GroupSet columnGroupSet = this.diagram.getDiagramContents().getGroups();
        for (NodeElement nodeElement : this.nodeElements) {
            this.diagram.removeContent(nodeElement);
        }
        for (ColumnGroup columnGroup : this.columnGroups) {
            columnGroupSet.remove(columnGroup);
        }
        ERDiagramEditPart.setUpdateable(true);
        this.diagram.changeAll();
    }

    /**
	 * 貼り付けられたテーブルを選択状態にします。
	 */
    private void setFocus() {
        for (NodeElement nodeElement : this.nodeElements) {
            EditPart editPart = (EditPart) viewer.getEditPartRegistry().get(nodeElement);
            this.viewer.getSelectionManager().appendSelection(editPart);
        }
    }
}
