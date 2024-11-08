package com.wzg.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import com.newbee.database.DataEngine;
import com.newbee.database.Table;
import com.newbee.util.Format;
import com.newbee.util.GetMac;
import com.newbee.webcontext.Context;

public class ExamGenerator {

    public ExamGenerator() {
    }

    @SuppressWarnings("unchecked")
    public Question[] ExamService(SetModel obj) {
        Question[] qs = null;
        if (Context.getSession().getAttribute("qs") != null) {
            qs = (Question[]) Context.getSession().getAttribute("qs");
        }
        HashMap<String, String> ts = new HashMap<String, String>();
        if (Context.getSession().getAttribute("ts") != null) {
            ts = (HashMap<String, String>) Context.getSession().getAttribute("ts");
        }
        if (obj != null) {
            String examModelType = obj.getExamModelType();
            String examCourse = obj.getExamCourse();
            String examTotalPoint = obj.getExamTotalPoint();
            String examKnowledgePoint = obj.getExamKnowledgePoint();
            String[] examTypeSelected = obj.getExamTypeSelected();
            String[] examPointInput = obj.getExamPointInput();
            String[] examCountInput = obj.getExamCountInput();
            String[] easy = obj.getEasy();
            String[] easyToo = obj.getEasyToo();
            String[] midd = obj.getMidd();
            String[] hard = obj.getHard();
            String[] hardToo = obj.getHardToo();
            String[] lowDistinct = obj.getLowDistinct();
            String[] middDistinct = obj.getMiddDistinct();
            String[] highDistinct = obj.getHighDistinct();
            String[] lowRequire = obj.getLowRequire();
            String[] middRequire = obj.getMiddRequire();
            String[] highRequire = obj.getHighRequire();
            int allCount = 0;
            for (int i = 0; i < examCountInput.length; i++) {
                allCount = allCount + Integer.parseInt(examCountInput[i]);
            }
            Hashtable typeCount = new Hashtable();
            for (int i = 0; i < examCountInput.length; i++) {
                typeCount.put("0" + (i + 1), Integer.valueOf(examCountInput[i]));
            }
            Hashtable allParam = new Hashtable();
            for (int i = 0; i < examTypeSelected.length; i++) {
                Hashtable arrayMap0 = new Hashtable();
                arrayMap0.put("01", Integer.valueOf(lowRequire[i]));
                arrayMap0.put("02", Integer.valueOf(middRequire[i]));
                arrayMap0.put("03", Integer.valueOf(highRequire[i]));
                Hashtable arrayMap00 = new Hashtable();
                arrayMap00.put("01", Integer.valueOf(lowDistinct[i]));
                arrayMap00.put("02", Integer.valueOf(middRequire[i]));
                arrayMap00.put("03", Integer.valueOf(highRequire[i]));
                Hashtable arrayMap000 = new Hashtable();
                arrayMap000.put("01", Integer.valueOf(easy[i]));
                arrayMap000.put("02", Integer.valueOf(easyToo[i]));
                arrayMap000.put("03", Integer.valueOf(midd[i]));
                arrayMap000.put("04", Integer.valueOf(hard[i]));
                arrayMap000.put("05", Integer.valueOf(hardToo[i]));
                Hashtable arrayMap0000 = new Hashtable();
                arrayMap0000.put("Require", arrayMap0);
                arrayMap0000.put("Distinct", arrayMap00);
                arrayMap0000.put("Hard", arrayMap000);
                allParam.put("0" + (i + 1), arrayMap0000);
            }
            String selectSql = "select strQuestionId,strQuestionTypeId,strDifficultyId,strDistinguishId,strDemandId from Question ";
            StringBuffer whereSql = new StringBuffer(" where  isDelete = 0 ");
            if (!examModelType.equals("0")) {
                whereSql.append(" and STRQUESTIONID like '" + GetMac.getMacAddressIP().split(",")[0] + "%'");
            }
            if (examCourse != null && !examCourse.equals("")) {
                whereSql.append(" and STRCOURSEID ='" + examCourse + "'");
            }
            if (examTypeSelected != null && examTypeSelected.length > 0) {
                StringBuffer sbf = new StringBuffer();
                for (int i = 0; i < examTypeSelected.length; i++) {
                    if (examTypeSelected[i].equals("0")) {
                        continue;
                    } else {
                        sbf.append("'" + examTypeSelected[i] + "',");
                        ts.put(examTypeSelected[i], examTypeSelected[i]);
                    }
                }
                if (sbf.length() > 0) {
                    sbf.setLength(sbf.length() - 1);
                    whereSql.append(" and strQuestionTypeId in(" + sbf + ")");
                }
            }
            if (examKnowledgePoint != null && !examKnowledgePoint.equals("")) {
                String insql = getKnowledgePoints(examKnowledgePoint);
                if (!insql.equals("")) {
                    insql = "''," + insql;
                } else {
                    insql = "''";
                }
                whereSql.append(" and (strFirstPoint in(" + insql + ") or strSecondPoint in(" + insql + ") or strThirdPoint in(" + insql + "))");
            }
            ArrayList<String> list = new ArrayList<String>();
            long startTime = System.currentTimeMillis();
            try {
                Table allItems = DataEngine.getInstance().executeQuery(selectSql + whereSql.toString());
                if (allItems.getRowCount() == 0) {
                    return new Question[0];
                }
                String strQuestionId = "";
                String strQuestionTypeId = "";
                do {
                    long endTime = System.currentTimeMillis();
                    if ((endTime - startTime) <= (300 * allCount)) {
                        int rowNum = Format.getRandom(0, allItems.getRowCount() - 1);
                        strQuestionId = allItems.getCellValue(rowNum, 0).trim();
                        strQuestionTypeId = allItems.getCellValue(rowNum, 1).trim();
                        String strDifficultyId = allItems.getCellValue(rowNum, 2).trim();
                        String strDistinguishId = allItems.getCellValue(rowNum, 3).trim();
                        String strDemandId = allItems.getCellValue(rowNum, 4).trim();
                        Hashtable t2 = null;
                        Integer count2 = null;
                        Hashtable t3 = null;
                        Integer count3 = null;
                        Hashtable t4 = null;
                        Integer count4 = null;
                        if (list.contains(strQuestionId)) {
                            continue;
                        }
                        Integer count5 = (Integer) typeCount.get(strQuestionTypeId);
                        if (count5 <= 0) {
                            continue;
                        }
                        Hashtable t1 = (Hashtable) allParam.get(strQuestionTypeId);
                        t2 = (Hashtable) t1.get("Hard");
                        count2 = (Integer) t2.get(strDifficultyId);
                        if (count2 <= 0) {
                            continue;
                        }
                        t3 = (Hashtable) t1.get("Distinct");
                        count3 = (Integer) t3.get(strDistinguishId);
                        if (count3 <= 0) {
                            continue;
                        }
                        t4 = (Hashtable) t1.get("Require");
                        count4 = (Integer) t4.get(strDemandId);
                        if (count4 <= 0) {
                            continue;
                        }
                        t2.put(strDifficultyId, --count2);
                        t3.put(strDistinguishId, --count3);
                        t4.put(strDemandId, --count4);
                        typeCount.put(strQuestionTypeId, --count5);
                    } else if ((endTime - startTime) > (300 * allCount) && (endTime - startTime) <= (6000 * allCount)) {
                        int rowNum = Format.getRandom(0, allItems.getRowCount() - 1);
                        strQuestionId = allItems.getCellValue(rowNum, 0).trim();
                        strQuestionTypeId = allItems.getCellValue(rowNum, 1).trim();
                        if (list.contains(strQuestionId)) {
                            continue;
                        }
                        Integer count5 = (Integer) typeCount.get(strQuestionTypeId);
                        if (count5 <= 0) {
                            continue;
                        }
                        typeCount.put(strQuestionTypeId, --count5);
                    } else {
                        return new Question[0];
                    }
                    list.add(strQuestionId);
                } while (list.size() < allCount);
                int[] PointInputs = new int[examPointInput.length];
                for (int i = 0; i < PointInputs.length; i++) {
                    PointInputs[i] = Integer.parseInt(examPointInput[i]);
                }
                int[] CountInputs = new int[examCountInput.length];
                for (int i = 0; i < CountInputs.length; i++) {
                    CountInputs[i] = Integer.parseInt(examCountInput[i]);
                }
                Hashtable fenshu = new Hashtable();
                for (int i = 0; i < CountInputs.length; i++) {
                    if (CountInputs[i] == 0) {
                        continue;
                    }
                    int[] tmp = new int[CountInputs[i]];
                    int step = PointInputs[i] / CountInputs[i];
                    for (int j = 0; j < tmp.length; j++) {
                        tmp[j] = step;
                    }
                    if (PointInputs[i] - tmp.length * step > 0) {
                        int syc = PointInputs[i] - tmp.length * step;
                        for (int j = 0; j < tmp.length; j++) {
                            tmp[j] = tmp[j] + 1;
                            syc = syc - 1;
                            if (syc == 0) {
                                break;
                            }
                        }
                    }
                    fenshu.put("0" + (i + 1), tmp);
                }
                HashMap<String, String> map0 = new HashMap<String, String>();
                StringBuffer b01 = new StringBuffer("'',");
                for (int i = 0; i < list.size(); i++) {
                    map0.put(list.get(i), list.get(i));
                }
                Iterator<String> iter = map0.keySet().iterator();
                while (iter.hasNext()) {
                    String next = iter.next();
                    b01.append("'" + next + "',");
                }
                b01.setLength(b01.length() - 1);
                DataEngine instance = DataEngine.getInstance();
                Table qTable = instance.executeQuery("select strQuestionId,strQuestionTypeId,strName,strDifficultyId,strDistinguishId,strDemandId from Question where  isDelete = 0 and strQuestionId in(" + b01.toString() + ")");
                qs = getResultData(qTable, fenshu);
                Context.getSession().setAttribute(Context.getSession().getId() + "qs", qs);
                Context.getSession().setAttribute(Context.getSession().getId() + "ts", ts);
                System.out.println("exam sessionid=" + Context.getSession().getId());
                return qs;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new Question[0];
    }

    private Question[] getResultData(Table q, Hashtable fenshu) {
        try {
            DataEngine instance = DataEngine.getInstance();
            Question[] qs = new Question[q.getRowCount()];
            for (int i = 0; i < q.getRowCount(); i++) {
                qs[i] = new Question();
                qs[i].setStrQuestionId(q.getCellValue(i, 0).trim());
                qs[i].setStrContext(q.getCellValue(i, 2).trim());
                qs[i].setStrType(q.getCellValue(i, 1).trim());
                qs[i].setPoint(getOnePoint(qs[i].getStrType(), fenshu));
                Table aTable = instance.executeQuery("select strQuestionId,strName,nIsRight,nSequence from Answer where isDelete = 0 and strQuestionId = '" + qs[i].getStrQuestionId() + "'");
                for (int j = 0; j < aTable.getRowCount(); j++) {
                    Answer ans = new Answer();
                    ans.setIsRight(Integer.valueOf(aTable.getCellValue(j, 2)));
                    ans.setStrContext(aTable.getCellValue(j, 1));
                    ans.setNumSequence(Integer.parseInt(aTable.getCellValue(j, 3)));
                    qs[i].addAnswer(ans);
                }
            }
            return qs;
        } catch (Exception e) {
            e.printStackTrace();
            return new Question[0];
        }
    }

    private int getOnePoint(String strType, Hashtable fenshu) {
        int[] t01 = (int[]) fenshu.get(strType);
        int t001 = t01[0];
        int[] t02 = new int[t01.length - 1];
        for (int i = 0; i < t02.length; i++) {
            t02[i] = t01[i + 1];
        }
        fenshu.put(strType, t02);
        return t001;
    }

    private String getKnowledgePoints(String kp) {
        Table table = null;
        StringBuffer kp0 = new StringBuffer(kp);
        StringBuffer kp1 = new StringBuffer();
        StringBuffer kp2 = new StringBuffer();
        HashMap<String, String> map = new HashMap<String, String>();
        String[] strings = kp.split(",");
        for (int i = 0; i < strings.length; i++) {
            map.put(strings[i], strings[i]);
        }
        try {
            do {
                table = DataEngine.getInstance().executeQuery("select nKnowledgePointId from KnowledgePoint where  isDelete = 0 and  nParentId in(" + kp0.toString() + ")");
                if (table != null && table.getRowCount() > 0) {
                    kp0 = new StringBuffer();
                    for (int i = 0; i < table.getRowCount(); i++) {
                        map.put(table.getCellValue(i, 0), table.getCellValue(i, 0));
                        kp0.append((i == 0 ? "" : ",") + table.getCellValue(i, 0));
                    }
                }
            } while (table != null && table.getRowCount() != 0);
            Iterator<String> its = map.keySet().iterator();
            while (its.hasNext()) {
                String next = its.next();
                kp1.append(next + ",");
            }
            kp1.setLength(kp1.length() - 1);
            table = DataEngine.getInstance().executeQuery("select strKnowledgePointId from KnowledgePoint where  isDelete = 0 and  nKnowledgePointId in(" + kp1.toString() + ")");
            if (table != null && table.getRowCount() > 0) {
                for (int i = 0; i < table.getRowCount(); i++) {
                    kp2.append("'" + table.getCellValue(i, 0) + "',");
                }
                kp2.setLength(kp2.length() - 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kp2.toString();
    }

    public KPoint[] getKnowledgePoints(String strCourseId, int option) {
        Table table = null;
        KPoint[] kps = null;
        try {
            table = DataEngine.getInstance().executeQuery("select nKnowledgePointId,nParentId,strName from KnowledgePoint where isDelete = 0 and strCourseId='" + strCourseId + "' order by STRORDER");
            if (table != null || table.getRowCount() > 0) {
                kps = new KPoint[table.getRowCount()];
                for (int i = 0; i < table.getRowCount(); i++) {
                    kps[i] = new KPoint(table.getCellValue(i, 0), table.getCellValue(i, 1), table.getCellValue(i, 2));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kps;
    }

    public KPoint[] getCoursePoints() {
        Table table = null;
        KPoint[] kps = null;
        try {
            table = DataEngine.getInstance().executeQuery("select strCourseId, strName from Course where isDelete = 0 order by nOrder asc");
            if (table != null || table.getRowCount() > 0) {
                kps = new KPoint[table.getRowCount()];
                for (int i = 0; i < table.getRowCount(); i++) {
                    kps[i] = new KPoint(table.getCellValue(i, 0), "0", table.getCellValue(i, 1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kps;
    }

    public Question[] quesionChange(String oldId, String newId) {
        Table table = null;
        Question newQuestion = null;
        Question[] qs = null;
        try {
            table = DataEngine.getInstance().executeQuery("select strQuestionId,strQuestionTypeId,strName from Question where   isDelete = 0 and strQuestionId ='" + newId + "'");
            if (table != null || table.getRowCount() == 1) {
                newQuestion = new Question();
                newQuestion.setStrQuestionId(table.getCellValue(0, 0).trim());
                newQuestion.setStrContext(table.getCellValue(0, 2).trim());
                newQuestion.setStrType(table.getCellValue(0, 1).trim());
                Table aTable = DataEngine.getInstance().executeQuery("select strQuestionId,strName,nIsRight,nSequence from Answer where   isDelete = 0 and strQuestionId = '" + newQuestion.getStrQuestionId() + "'");
                for (int j = 0; j < aTable.getRowCount(); j++) {
                    Answer ans = new Answer();
                    ans.setIsRight(Integer.valueOf(aTable.getCellValue(j, 2)));
                    ans.setStrContext(aTable.getCellValue(j, 1));
                    ans.setNumSequence(Integer.parseInt(aTable.getCellValue(j, 3)));
                    newQuestion.addAnswer(ans);
                }
            }
            System.out.println("change sessionid=" + Context.getSession().getId());
            qs = (Question[]) Context.getSession().getAttribute(Context.getSession().getId() + "qs");
            for (int i = 0; i < qs.length; i++) {
                if (qs[i].strQuestionId.equalsIgnoreCase(oldId)) {
                    newQuestion.setPoint(qs[i].getPoint());
                    qs[i] = newQuestion;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return qs;
    }

    public Map getCourseTypeMap(String modelId, String courseId, String examKnowledgePoint) {
        Table table = null;
        HashMap ctMap = null;
        try {
            String sql = "select strQuestionTypeId as typeId,count(strQuestionTypeId) as typeCount from Question ";
            String sqlWhere = " where isDelete = 0 and  STRCOURSEID ='" + courseId + "'";
            if (modelId.equals("1")) {
                sqlWhere += " and STRQUESTIONID like '" + GetMac.getMacAddressIP().split(",")[0] + "%'";
            }
            if (examKnowledgePoint != null && !examKnowledgePoint.equals("")) {
                String insql = getKnowledgePoints(examKnowledgePoint);
                if (insql.equals("")) {
                    insql = "'err'";
                }
                sqlWhere += " and (strFirstPoint in(" + insql + ") or strSecondPoint in(" + insql + ") or strThirdPoint in(" + insql + "))";
            }
            String sqlGroup = " group by strQuestionTypeId ";
            table = DataEngine.getInstance().executeQuery(sql + sqlWhere + sqlGroup);
            if (table != null || table.getRowCount() > 0) {
                ctMap = new HashMap();
                for (int j = 0; j < table.getRowCount(); j++) {
                    ctMap.put(table.getCellValue(j, 0), table.getCellValue(j, 1));
                }
            }
            return ctMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap();
    }

    public static void main(String[] args) {
        System.out.println(Integer.valueOf("1061598715"));
    }
}
