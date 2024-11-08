package coollemon.dataBase;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jxl.write.Label;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.*;
import coollemon.kernel.*;

public class XLSX extends DataFormat {

    public static XLSX xlsx = new XLSX();

    private XLSX() {
    }

    ;

    public ContactManager readFile(String filename) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        try {
            XSSFWorkbook xwb = new XSSFWorkbook(filename);
            XSSFSheet sheet = xwb.getSheetAt(0);
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                XSSFRow row = sheet.getRow(i);
                XSSFCell cell = null;
                cell = row.getCell(0);
                String name = cell.getStringCellValue();
                cell = row.getCell(1);
                String phoneNum = cell.getStringCellValue().replaceAll(" ", "");
                cell = row.getCell(2);
                String fixedTel = cell.getStringCellValue().replaceAll(" ", "");
                cell = row.getCell(3);
                String email = cell.getStringCellValue();
                cell = row.getCell(4);
                String qq = cell.getStringCellValue().replaceAll(" ", "");
                cell = row.getCell(5);
                String nick = cell.getStringCellValue();
                cell = row.getCell(6);
                String sexStr = cell.getStringCellValue();
                int sex = 0;
                if (sexStr.equalsIgnoreCase("M")) sex = 1; else if (sexStr.equalsIgnoreCase("F")) sex = 2; else ;
                cell = row.getCell(7);
                BirthDay birth = new BirthDay(cell.getStringCellValue());
                cell = row.getCell(8);
                String icon = cell.getStringCellValue();
                cell = row.getCell(9);
                String addr = cell.getStringCellValue();
                cell = row.getCell(10);
                String workplace = cell.getStringCellValue();
                cell = row.getCell(11);
                String zipCode = cell.getStringCellValue().replaceAll(" ", "");
                cell = row.getCell(12);
                String homePage = cell.getStringCellValue();
                String regex = "([^;]+);*";
                cell = row.getCell(13);
                String others = cell.getStringCellValue();
                ArrayList<String> otherWay = new ArrayList<String>();
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(others);
                while (matcher.find()) {
                    String str = matcher.group(1);
                    otherWay.add(str);
                }
                cell = row.getCell(14);
                String normals = cell.getStringCellValue();
                ArrayList<NormalTag> normal = new ArrayList<NormalTag>();
                matcher = pattern.matcher(normals);
                while (matcher.find()) {
                    String str = matcher.group(1);
                    normal.add(NormalTag.createNormalTag(str));
                }
                ArrayList<RelationTag> relation = null;
                Contact contact = Contact.createContact(name, phoneNum, fixedTel, email, qq, nick, sex, birth, icon, addr, workplace, zipCode, homePage, otherWay, normal, null);
                contacts.add(contact);
                cell = row.getCell(15);
                String relations = cell.getStringCellValue();
                regex = "(<([^;]+),([^;]+),([^;]+)>);*";
                pattern = Pattern.compile(regex);
                matcher = pattern.matcher(relations);
                while (matcher.find()) {
                    String tag = matcher.group(2);
                    String name1 = matcher.group(3);
                    String name2 = matcher.group(4);
                    RelationTag relationTag = RelationTag.createRelationTag(tag);
                    ArrayList<Contact> B = new ArrayList<Contact>();
                    for (int j = 0; j < contacts.size(); j++) {
                        if (contacts.get(j).getName().equals(name2)) B.add(contacts.get(j));
                    }
                    if (contact.getName().equals(name1)) {
                        for (int k = 0; k < B.size(); k++) {
                            ContactManager.addTagToContact(new Pair<Contact, Contact>(contact, B.get(k)), relationTag);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ContactManager(contacts);
    }

    public boolean writeFile(ContactManager conM, String filename) {
        try {
            XSSFWorkbook wb = new XSSFWorkbook();
            FileOutputStream fileOut = new FileOutputStream(filename);
            XSSFSheet sheet = wb.createSheet("Sheet_1");
            XSSFRow row = sheet.createRow(0);
            int type = XSSFCell.CELL_TYPE_STRING;
            row.createCell(0).setCellValue("name");
            row.createCell(1).setCellValue("phoneNumber");
            row.createCell(2).setCellValue("fixedTel");
            row.createCell(3).setCellValue("email");
            row.createCell(4).setCellValue("qq");
            row.createCell(5).setCellValue("nick");
            row.createCell(6).setCellValue("sex");
            row.createCell(7).setCellValue("birthday");
            row.createCell(8).setCellValue("icon");
            row.createCell(9).setCellValue("address");
            row.createCell(10).setCellValue("workspace");
            row.createCell(11).setCellValue("zipCode");
            row.createCell(12).setCellValue("homePage");
            row.createCell(13).setCellValue("otherWay");
            row.createCell(14).setCellValue("normal");
            row.createCell(15).setCellValue("relation");
            ArrayList<Contact> contacts = conM.getContacts();
            for (int i = 0; i < contacts.size(); i++) {
                Contact contact = contacts.get(i);
                row = sheet.createRow(i + 1);
                row.createCell(0, Cell.CELL_TYPE_STRING).setCellValue(contact.getName());
                row.createCell(1, Cell.CELL_TYPE_STRING).setCellValue(" " + contact.getPhoneNumber() + " ");
                row.createCell(2, Cell.CELL_TYPE_STRING).setCellValue(" " + contact.getFixedTel() + " ");
                row.createCell(3, Cell.CELL_TYPE_STRING).setCellValue(contact.getEmail());
                row.createCell(4, Cell.CELL_TYPE_STRING).setCellValue(" " + contact.getQq() + " ");
                row.createCell(5, Cell.CELL_TYPE_STRING).setCellValue(contact.getNick());
                switch(contact.getSex()) {
                    case 1:
                        row.createCell(6, Cell.CELL_TYPE_STRING).setCellValue("M");
                        break;
                    case 2:
                        row.createCell(6, Cell.CELL_TYPE_STRING).setCellValue("F");
                        break;
                    default:
                        row.createCell(6, Cell.CELL_TYPE_STRING).setCellValue("");
                        break;
                }
                String birthday = contact.getBirthday().toString();
                row.createCell(7, Cell.CELL_TYPE_STRING).setCellValue(birthday);
                row.createCell(8, Cell.CELL_TYPE_STRING).setCellValue(contact.getIcon());
                row.createCell(9, Cell.CELL_TYPE_STRING).setCellValue(contact.getAddress());
                row.createCell(10, Cell.CELL_TYPE_STRING).setCellValue(contact.getWorkplace());
                row.createCell(11, Cell.CELL_TYPE_STRING).setCellValue(" " + contact.getZipCode() + " ");
                row.createCell(12, Cell.CELL_TYPE_STRING).setCellValue(contact.getHomepage());
                ArrayList<String> otherWays = contact.getOtherWay();
                String otherWayStr = "";
                if (otherWays.size() > 0) {
                    for (int j = 0; j < otherWays.size() - 1; j++) {
                        otherWayStr += otherWays.get(j);
                        otherWayStr += ";";
                    }
                    otherWayStr += otherWays.get(otherWays.size() - 1);
                }
                row.createCell(13, Cell.CELL_TYPE_STRING).setCellValue(otherWayStr);
                ArrayList<NormalTag> normals = contact.getNormal();
                String normalStr = "";
                if (normals.size() > 0) {
                    for (int j = 0; j < normals.size() - 1; j++) {
                        normalStr += normals.get(j).getName();
                        normalStr += ";";
                    }
                    normalStr += normals.get(normals.size() - 1).getName();
                }
                row.createCell(14, Cell.CELL_TYPE_STRING).setCellValue(normalStr);
                ArrayList<RelationTag> relations = contact.getRelation();
                String relationStr = "";
                for (int j = 0; j < relations.size(); j++) {
                    RelationTag relationTag = relations.get(j);
                    ArrayList<Contact> cons = relationTag.getContacts(contact);
                    for (int k = 0; k < cons.size(); k++) {
                        relationStr = relationStr + "<" + relationTag.getName() + "," + contact.getName() + "," + cons.get(k).getName() + ">" + ";";
                    }
                }
                if (!relationStr.isEmpty()) relationStr = relationStr.substring(0, relationStr.length() - 1);
                row.createCell(15, Cell.CELL_TYPE_STRING).setCellValue(relationStr);
            }
            wb.write(fileOut);
            fileOut.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        ContactManager conM = xlsx.readFile("./data/Test.xlsx");
        xlsx.writeFile(conM, "./data/Test_write.xlsx");
        ContactManager conM1 = xlsx.readFile("./data/Test_write.xlsx");
        xlsx.writeFile(conM1, "./data/Test_write1.xlsx");
    }
}
