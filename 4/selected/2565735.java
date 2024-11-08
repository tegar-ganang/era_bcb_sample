package net.homeip.tinwiki.web.actions.addSection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.homeip.tinwiki.web.forms.WebPage;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import orgx.jdom.Document;
import orgx.jdom.Attribute;
import orgx.jdom.Element;
import orgx.jdom.input.SAXBuilder;
import orgx.jdom.output.Format;
import orgx.jdom.output.XMLOutputter;

public class addSectionAction extends Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ActionErrors errors = new ActionErrors();
        ActionForward forward = new ActionForward();
        WebPage webPage = (WebPage) form;
        try {
            System.out.println("## Inserting Section is started. ##");
            System.out.println("webPage.getSectionName()" + webPage.getSectionName());
            System.out.println("webPage.getFileName()" + webPage.getFileName());
            String realPath = getServlet().getServletContext().getRealPath("/");
            String myFile = realPath + File.separatorChar + webPage.getFileName();
            File inputFile = new File(myFile);
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(inputFile);
            List listBodyDivs = doc.getRootElement().getChild("body").getChildren("div");
            Element currentElement = null;
            int tempDivPosition = 0;
            boolean isDivFound = false;
            for (int i = 0; i < (listBodyDivs.size()); i++) {
                currentElement = (Element) listBodyDivs.get(i);
                System.out.println(currentElement.getAttributeValue("id"));
                if (webPage.getSectionName().equals(currentElement.getAttributeValue("id"))) {
                    System.out.println("first match found at " + i);
                    tempDivPosition = i;
                    i = listBodyDivs.size();
                    isDivFound = true;
                }
            }
            if (!isDivFound) {
                tempDivPosition = listBodyDivs.size();
            }
            Element divToBeAdded = new Element("div");
            Element h1ToBeAdded = new Element("h1");
            Element pToBeAdded = new Element("p");
            h1ToBeAdded.setText("New Section");
            pToBeAdded.setText("Added Here!");
            divToBeAdded.setContent(h1ToBeAdded);
            divToBeAdded.addContent(pToBeAdded);
            divToBeAdded.setAttribute(new Attribute("id", "section" + tempDivPosition));
            for (int i = tempDivPosition; i < listBodyDivs.size(); i++) {
                currentElement = (Element) listBodyDivs.get(i);
                listBodyDivs.set(i, divToBeAdded);
                divToBeAdded = currentElement;
            }
            listBodyDivs.add(divToBeAdded);
            Attribute tempAtt;
            for (int i = 0; i < listBodyDivs.size(); i++) {
                currentElement = (Element) listBodyDivs.get(i);
                currentElement.setAttribute(new Attribute("id", "section" + i));
                listBodyDivs.set(i, currentElement);
            }
            Format myFormat = Format.getPrettyFormat();
            myFormat.setIndent("\t");
            myFormat.setEncoding("ISO-8859-1");
            myFormat.setOmitDeclaration(false);
            StringWriter in = new StringWriter();
            XMLOutputter outp = new XMLOutputter(myFormat);
            outp.output(doc, in);
            StringBuffer output = in.getBuffer();
            String entireDocument = output.toString();
            in.close();
            System.out.println("After Insertion of new Div: " + entireDocument);
            System.out.println("Writing to File.....");
            File outputFile = null;
            outputFile = new File(myFile);
            if (outputFile.exists()) {
                System.out.println("Files Exists.");
                backupOriginalFile(myFile);
            }
            webPage.setContent(entireDocument);
            BufferedWriter bufferedOut = null;
            FileWriter out = null;
            out = new FileWriter(outputFile);
            bufferedOut = new BufferedWriter(out);
            bufferedOut.write(entireDocument);
            bufferedOut.close();
            out.close();
            System.out.println("**** File written = " + myFile);
            System.out.println("## Inserting Section is finished. ##");
        } catch (Exception e) {
            System.out.println(e);
            errors.add("name", new ActionError("id"));
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
        }
        forward = mapping.findForward("success");
        return (forward);
    }

    private void backupOriginalFile(String myFile) {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_S");
        String datePortion = format.format(date);
        try {
            FileInputStream fis = new FileInputStream(myFile);
            FileOutputStream fos = new FileOutputStream(myFile + "-" + datePortion + "_UserID" + ".html");
            FileChannel fcin = fis.getChannel();
            FileChannel fcout = fos.getChannel();
            fcin.transferTo(0, fcin.size(), fcout);
            fcin.close();
            fcout.close();
            fis.close();
            fos.close();
            System.out.println("**** Backup of file made.");
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
