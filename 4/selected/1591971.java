package org.sucri.floxs.html;

import com.sun.org.apache.html.internal.dom.HTMLDocumentImpl;
import java.io.*;
import org.sucri.floxs.ext.widget.Grid;
import org.sucri.floxs.ext.widget.ColumnModel;
import org.sucri.floxs.ext.widget.ExtWidget;
import org.sucri.floxs.ext.data.DataStore;
import org.sucri.floxs.ext.form.Form;
import org.sucri.floxs.mock.mockExts;

/**
 * Created by IntelliJ IDEA.
 * User: Wen Yu
 * Date: Jul 10, 2007
 * Time: 10:18:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class Ext_HtmlDoc extends HTMLDocumentImpl {

    public void toHTML(OutputStream out) throws IOException {
        Form form = mockExts.mockFormViaAnnotationModel();
        String f = "var form; Ext.onReady(function(){ init_form(); });" + ExtWidget.br();
        f = "<script>" + f + form.initFunc("form", "west-div") + "</script>";
        String html = "<html>\n" + "<head>\n" + "    <title>Simple Layout</title>\n" + "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/sucri/css/extjs/ext-all.css\"/>\n" + "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/sucri/css/extjs/xtheme-aero.css\"/>\n" + "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/sucri/css/extjs/forms.css\"/>\n" + "    <script type=\"text/javascript\" src=\"/sucri/js/ext-base.js\"></script>\n" + "    <script type=\"text/javascript\" src=\"/sucri/js/ext-all-debug.js\"></script>\n" + f + "</head>\n" + "<body>\n" + "    <div id=\"north-div\"></div>\n" + "    <div id=\"south-div\"></div>\n" + "    <div id=\"east-div\"></div>\n" + "    <div id=\"west-div\" style=\"width:300px;\"></div>\n" + "    <div id=\"center-div\"><span id = 'test_grid'>TEST</span></div>\n" + "</body>\n" + "</html>";
        out.write(html.getBytes());
    }

    public void toHTML(OutputStream out, File template) throws IOException {
        FileInputStream fis = new FileInputStream(template);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) out.write(buf, 0, i);
        fis.close();
    }

    private String exampleGrid() {
        String g = "var grid;" + "var ds;" + " " + "Ext.onReady(function(){" + "\tinit_grid();   " + "});" + " " + "function init_grid() {" + "    ds = new Ext.data.Store({" + "\t\tproxy: new Ext.data.HttpProxy({url: '/sucri?type=grid'})," + " " + "        reader: new Ext.data.JsonReader({" + "            root: 'Movies'," + "            totalProperty: 'Total'," + "            id: 'id'" + "        }, [" + "            {name: 'title', mapping: 'title'}," + "            {name: 'plot', mapping: 'plot'}," + "            {name: 'release_year', mapping: 'date'}," + "            {name: 'genre', mapping: 'genre'}," + "            {name: 'mpaa', mapping: 'mpaa'}," + "            {name: 'directed_by', mapping: 'directed_by'}" + "        ]),        " + "        // turn on remote sorting" + "        remoteSort: true\t" + "    });" + " " + "    var cm = new Ext.grid.ColumnModel" + "    \t([{" + "           id: 'title'," + "           header: \"Title\"," + "           dataIndex: 'title'," + "           width: 250" + "        },{" + "           header: \"Release Year\"," + "           dataIndex: 'release_year'," + "           width: 75" + "        },{  " + "           header: \"MPAA Rating\"," + "           dataIndex: 'mpaa'," + "           width: 75" + "        },{  " + "           header: \"Genre\"," + "           dataIndex: 'genre'," + "           width: 100" + "        },{  " + "           header: \"Director\"," + "           dataIndex: 'directed_by'," + "           width: 150" + "        }]);" + " " + "    cm.defaultSortable = true;" + " " + "    grid = new Ext.grid.Grid('test_grid', {" + "        ds: ds," + "        cm: cm," + "        selModel: new Ext.grid.RowSelectionModel({singleSelect:true})," + "\t\tautoExpandColumn: 'title'" + "    });" + " " + "    grid.render();    " + "    ds.load({params:{start:0, limit:20}});\t}";
        g = "<script>" + g + "</script>";
        return g;
    }
}
