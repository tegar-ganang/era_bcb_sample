package de.str.prettysource.demoweb;

import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import de.str.prettysource.format.html.HtmlOutputFormat;

public class ProcessSourceComponent extends PrettySourceComponent {

    private String sourceText = null;

    public String getSourceText() {
        return sourceText;
    }

    /**
	 * Sets the source text which shall be made pretty.
	 * 
	 * @param sourceText
	 */
    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public void actionListener(ActionEvent e) {
        FacesContext fc = FacesContext.getCurrentInstance();
        try {
            HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
            ServletOutputStream out = response.getOutputStream();
            response.setContentType("text/HTML");
            HtmlOutputFormat formatter = super.getSelectedFormatAsInstance();
            Reader reader = new StringReader(this.sourceText);
            Writer writer = new OutputStreamWriter(out);
            formatter.format(reader, writer);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        fc.renderResponse();
        fc.responseComplete();
    }
}
