package edu.umn.cs5115.scheduler.entities;

import edu.umn.cs5115.scheduler.SchedulerDocument;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Vector;

public class CourseFactory {

    public static List<Course> getAllCourses(SchedulerDocument document) {
        List<Course> courses = new Vector<Course>();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
        Course course1 = new Course(document);
        course1.setDepartment("CSCI");
        course1.setCourseNumber("1103");
        course1.setName("Introduction to Computer Programming in Java");
        course1.setDescription("Student may contact the instructor or department for information.");
        MeetingTime lecTime;
        Section lecture = new Section(course1);
        lecture.setSectionNumber("001");
        lecture.setStaff("Sturtivant, Carl");
        lecture.setType("LEC");
        try {
            lecTime = new MeetingTime(document, lecture);
            lecTime.addMeetingDay(Weekday.THURSDAY);
            lecTime.setStartDate(dateFormatter.parse("09/05/2006"));
            lecTime.setEndDate(dateFormatter.parse("12/13/2006"));
            lecTime.setStartTime(timeFormatter.parse("18:30"));
            lecTime.setEndTime(timeFormatter.parse("21:00"));
            lecTime.setLocation("EE/CSci 3-111, TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        lecture.addMeetingTime(lecTime);
        MeetingTime disc1Time;
        Section discussion1 = new Section(lecture);
        discussion1.setSectionNumber("002");
        discussion1.setStaff("Sturtivant, Carl");
        discussion1.setType("DIS");
        discussion1.setCredits(4);
        try {
            disc1Time = new MeetingTime(document, discussion1);
            disc1Time.addMeetingDay(Weekday.MONDAY);
            disc1Time.setStartDate(dateFormatter.parse("09/05/2006"));
            disc1Time.setEndDate(dateFormatter.parse("12/13/2006"));
            disc1Time.setStartTime(timeFormatter.parse("16:00"));
            disc1Time.setEndTime(timeFormatter.parse("17:55"));
            disc1Time.setLocation("EE/CSci 1-260, TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        discussion1.addMeetingTime(disc1Time);
        MeetingTime disc2Time;
        Section discussion2 = new Section(lecture);
        discussion2.setSectionNumber("003");
        discussion2.setStaff("Sturtivant, Carl");
        discussion2.setType("DIS");
        discussion2.setCredits(4);
        try {
            disc2Time = new MeetingTime(document, discussion2);
            disc2Time.addMeetingDay(Weekday.MONDAY);
            disc2Time.setStartDate(dateFormatter.parse("09/05/2006"));
            disc2Time.setEndDate(dateFormatter.parse("12/13/2006"));
            disc2Time.setStartTime(timeFormatter.parse("18:30"));
            disc2Time.setEndTime(timeFormatter.parse("20:25"));
            disc2Time.setLocation("EE/CSci 1-260, TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        discussion2.addMeetingTime(disc2Time);
        courses.add(course1);
        Course course2 = new Course(document);
        course2.setDepartment("CSCI");
        course2.setCourseNumber("1113");
        course2.setName("Introduction to C/C++ Programming for Scientists and Engineers");
        course2.setDescription("This course will cover algorithm development and the principles of computer programming using C and C++. Topics include introduction to computers and computing, program development, C/C++ programming language syntax, and elementary numerical methods for scientists and engineers. The prerequisite of one semester of calculus indicates the level of mathematical reasoning used in the class.");
        course2.setPrereqs("Math 1271 or Math 1371");
        Section[] lectures = new Section[2];
        lectures[0] = new Section(course2);
        lectures[0].setSectionNumber("001");
        lectures[0].setStaff("Swanson,Charles D");
        lectures[0].setType("LEC");
        lectures[1] = new Section(course2);
        lectures[1].setSectionNumber("010");
        lectures[1].setStaff("Swanson,Charles D");
        lectures[1].setType("LEC");
        MeetingTime[] lecTimes = new MeetingTime[2];
        try {
            lecTimes[0] = new MeetingTime(document, lectures[0]);
            lecTimes[0].addMeetingDay(Weekday.MONDAY);
            lecTimes[0].addMeetingDay(Weekday.WEDNESDAY);
            lecTimes[0].addMeetingDay(Weekday.FRIDAY);
            lecTimes[0].setStartDate(dateFormatter.parse("09/05/2006"));
            lecTimes[0].setEndDate(dateFormatter.parse("12/13/2006"));
            lecTimes[0].setStartTime(timeFormatter.parse("15:35"));
            lecTimes[0].setEndTime(timeFormatter.parse("16:25"));
            lecTimes[0].setLocation("EE/CSci  3-210, TCEASTBANK");
            lecTimes[1] = new MeetingTime(document, lectures[1]);
            lecTimes[1].addMeetingDay(Weekday.TUESDAY);
            lecTimes[1].setStartDate(dateFormatter.parse("09/05/2006"));
            lecTimes[1].setEndDate(dateFormatter.parse("12/13/2006"));
            lecTimes[1].setStartTime(timeFormatter.parse("18:30"));
            lecTimes[1].setEndTime(timeFormatter.parse("21:00"));
            lecTimes[1].setLocation("EE/CSci  3-230, TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        lectures[0].addMeetingTime(lecTimes[0]);
        lectures[1].addMeetingTime(lecTimes[1]);
        Section[] labs1 = new Section[4];
        labs1[0] = new Section(lectures[0]);
        labs1[0].setSectionNumber("002");
        labs1[0].setStaff("Swanson,Charles D");
        labs1[0].setType("LAB");
        labs1[0].setCredits(4);
        labs1[1] = new Section(lectures[0]);
        labs1[1].setSectionNumber("003");
        labs1[1].setStaff("Swanson,Charles D");
        labs1[1].setType("LAB");
        labs1[1].setCredits(4);
        labs1[2] = new Section(lectures[0]);
        labs1[2].setSectionNumber("004");
        labs1[2].setStaff("Swanson,Charles D");
        labs1[2].setType("LAB");
        labs1[2].setCredits(4);
        labs1[3] = new Section(lectures[0]);
        labs1[3].setSectionNumber("005");
        labs1[3].setStaff("Swanson,Charles D");
        labs1[3].setType("LAB");
        labs1[3].setCredits(4);
        MeetingTime[] lab1times = new MeetingTime[4];
        try {
            lab1times[0] = new MeetingTime(document, labs1[0]);
            lab1times[0].addMeetingDay(Weekday.MONDAY);
            lab1times[0].setStartDate(dateFormatter.parse("09/05/2006"));
            lab1times[0].setEndDate(dateFormatter.parse("12/13/2006"));
            lab1times[0].setStartTime(timeFormatter.parse("17:45"));
            lab1times[0].setEndTime(timeFormatter.parse("21:45"));
            lab1times[0].setLocation("EE/CSci 2-120, TCEASTBANK");
            lab1times[1] = new MeetingTime(document, labs1[1]);
            lab1times[1].addMeetingDay(Weekday.TUESDAY);
            lab1times[1].setStartDate(dateFormatter.parse("09/05/2006"));
            lab1times[1].setEndDate(dateFormatter.parse("12/13/2006"));
            lab1times[1].setStartTime(timeFormatter.parse("13:25"));
            lab1times[1].setEndTime(timeFormatter.parse("17:25"));
            lab1times[1].setLocation("EE/CSci 2-120, TCEASTBANK");
            lab1times[2] = new MeetingTime(document, labs1[2]);
            lab1times[2].addMeetingDay(Weekday.TUESDAY);
            lab1times[2].setStartDate(dateFormatter.parse("09/05/2006"));
            lab1times[2].setEndDate(dateFormatter.parse("12/13/2006"));
            lab1times[2].setStartTime(timeFormatter.parse("17:45"));
            lab1times[2].setEndTime(timeFormatter.parse("21:45"));
            lab1times[2].setLocation("EE/CSci 2-120, TCEASTBANK");
            lab1times[3] = new MeetingTime(document, labs1[3]);
            lab1times[3].addMeetingDay(Weekday.WEDNESDAY);
            lab1times[3].setStartDate(dateFormatter.parse("09/05/2006"));
            lab1times[3].setEndDate(dateFormatter.parse("12/13/2006"));
            lab1times[3].setStartTime(timeFormatter.parse("09:05"));
            lab1times[3].setEndTime(timeFormatter.parse("13:05"));
            lab1times[3].setLocation("EE/CSci 2-120, TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        labs1[0].addMeetingTime(lab1times[0]);
        labs1[1].addMeetingTime(lab1times[1]);
        labs1[2].addMeetingTime(lab1times[2]);
        labs1[3].addMeetingTime(lab1times[3]);
        Section[] labs2 = new Section[2];
        labs2[0] = new Section(lectures[1]);
        labs2[0].setSectionNumber("011");
        labs2[0].setStaff("Sturtivant, Carl");
        labs2[0].setType("LAB");
        labs2[0].setCredits(4);
        labs2[1] = new Section(lectures[1]);
        labs2[1].setSectionNumber("012");
        labs2[1].setStaff("Sturtivant, Carl");
        labs2[1].setType("LAB");
        labs2[1].setCredits(4);
        MeetingTime[] lab2times = new MeetingTime[2];
        try {
            lab2times[0] = new MeetingTime(document, labs2[0]);
            lab2times[0].addMeetingDay(Weekday.WEDNESDAY);
            lab2times[0].setStartDate(dateFormatter.parse("09/05/2006"));
            lab2times[0].setEndDate(dateFormatter.parse("12/13/2006"));
            lab2times[0].setStartTime(timeFormatter.parse("17:45"));
            lab2times[0].setEndTime(timeFormatter.parse("21:45"));
            lab2times[0].setLocation("EE/CSci 2-120, TCEASTBANK");
            lab2times[1] = new MeetingTime(document, labs2[1]);
            lab2times[1].addMeetingDay(Weekday.THURSDAY);
            lab2times[1].setStartDate(dateFormatter.parse("09/05/2006"));
            lab2times[1].setEndDate(dateFormatter.parse("12/13/2006"));
            lab2times[1].setStartTime(timeFormatter.parse("17:45"));
            lab2times[1].setEndTime(timeFormatter.parse("21:45"));
            lab2times[1].setLocation("EE/CSci 2-120, TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        labs2[0].addMeetingTime(lab2times[0]);
        labs2[1].addMeetingTime(lab2times[1]);
        courses.add(course2);
        Course course3 = new Course(document);
        course3.setDepartment("ENGL");
        course3.setCourseNumber("1117W");
        course3.setName("The Story of King Arthur ");
        course3.setDescription("Of all the stories familiar to the western world, few have exerted greater influence than the legend of King Arthur and his Round Table. Creative artists and their audiences continue to be fascinated by stories about Arthur, Merlin, Lancelot, Guinevere, Gawain, and Tristan. In this course, we will study adaptations of the legend in order to understand how literary writers and their readers remade the story to fit specific, historical circumstances. Texts read may include novels by T.H. White, Mark Twain, and Marion Zimmer Bradley; Middle English alliterative poems and chronicles; twentieth-century poems; and Welsh sources.");
        course3.setLibEds("Literature Core");
        course3.setLibEds("Writing Intensive");
        lecture = new Section(course3);
        lecture.setSectionNumber("001");
        lecture.setStaff("Krug,Rebecca L");
        lecture.setType("LET");
        lecture.setCredits(3);
        try {
            lecTime = new MeetingTime(document, lecture);
            lecTime.addMeetingDay(Weekday.MONDAY);
            lecTime.addMeetingDay(Weekday.WEDNESDAY);
            lecTime.addMeetingDay(Weekday.FRIDAY);
            lecTime.setStartDate(dateFormatter.parse("09/05/2006"));
            lecTime.setEndDate(dateFormatter.parse("12/13/2006"));
            lecTime.setStartTime(timeFormatter.parse("13:25"));
            lecTime.setEndTime(timeFormatter.parse("14:15"));
            lecTime.setLocation("AkerH 319, TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        lecture.addMeetingTime(lecTime);
        courses.add(course3);
        Course course4 = new Course(document);
        course4.setDepartment("CHEN");
        course4.setCourseNumber("4001");
        course4.setName("Material and Energy Balances");
        course4.setPrereqs("[Chem 2302 or concurrent enrollment Chem 2302], [Chem 3501 or concurrent enrollment Chem 3501 or equiv], [Math 2273 or concurrent enrollment Math 2373 or equiv], [Math 2374 or concurrent enrollment Math 2374 or equiv], Phys 1302");
        course4.setDescription("Student may contact the instructor or department for information.");
        lecture = new Section(course4);
        lecture.setSectionNumber("001");
        lecture.setStaff("Dorfman, Kevin David, Srienc, Friedrich");
        lecture.setType("LEC");
        lecture.setCredits(4);
        try {
            lecTime = new MeetingTime(document, lecture);
            lecTime = new MeetingTime(document, lecture);
            lecTime.addMeetingDay(Weekday.MONDAY);
            lecTime.addMeetingDay(Weekday.WEDNESDAY);
            lecTime.addMeetingDay(Weekday.FRIDAY);
            lecTime.setStartDate(dateFormatter.parse("09/05/2006"));
            lecTime.setEndDate(dateFormatter.parse("12/13/2006"));
            lecTime.setStartTime(timeFormatter.parse("15:35"));
            lecTime.setEndTime(timeFormatter.parse("16:25"));
            lecTime.setLocation("AmundH B75, TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        lecture.addMeetingTime(lecTime);
        Section[] discussions1 = new Section[4];
        discussions1[0] = new Section(lecture);
        discussions1[0].setSectionNumber("003");
        discussions1[0].setStaff("Dorfman, Kevin David, Srienc, Friedrich");
        discussions1[0].setType("DIS");
        discussions1[0].setCredits(4);
        discussions1[1] = new Section(lecture);
        discussions1[1].setSectionNumber("004");
        discussions1[1].setStaff("Dorfman, Kevin David, Srienc, Friedrich");
        discussions1[1].setType("DIS");
        discussions1[1].setCredits(4);
        discussions1[2] = new Section(lecture);
        discussions1[2].setSectionNumber("005");
        discussions1[2].setStaff("Dorfman, Kevin David, Srienc, Friedrich");
        discussions1[2].setType("DIS");
        discussions1[2].setCredits(4);
        discussions1[3] = new Section(lecture);
        discussions1[3].setSectionNumber("006");
        discussions1[3].setStaff("Dorfman, Kevin David, Srienc, Friedrich");
        discussions1[3].setType("DIS");
        discussions1[3].setCredits(4);
        MeetingTime[] discussion1times = new MeetingTime[4];
        try {
            discussion1times[0] = new MeetingTime(document, discussions1[0]);
            discussion1times[0].addMeetingDay(Weekday.TUESDAY);
            discussion1times[0].addMeetingDay(Weekday.THURSDAY);
            discussion1times[0].setStartDate(dateFormatter.parse("09/05/2006"));
            discussion1times[0].setEndDate(dateFormatter.parse("12/13/2006"));
            discussion1times[0].setStartTime(timeFormatter.parse("10:10"));
            discussion1times[0].setEndTime(timeFormatter.parse("11:00"));
            discussion1times[0].setLocation("AmundH 120, TCEASTBANK");
            discussion1times[1] = new MeetingTime(document, discussions1[1]);
            discussion1times[1].addMeetingDay(Weekday.TUESDAY);
            discussion1times[1].addMeetingDay(Weekday.THURSDAY);
            discussion1times[1].setStartDate(dateFormatter.parse("09/05/2006"));
            discussion1times[1].setEndDate(dateFormatter.parse("12/13/2006"));
            discussion1times[1].setStartTime(timeFormatter.parse("11:15"));
            discussion1times[1].setEndTime(timeFormatter.parse("12:05"));
            discussion1times[1].setLocation("AmundH 240, TCEASTBANK ");
            discussion1times[2] = new MeetingTime(document, discussions1[2]);
            discussion1times[2].addMeetingDay(Weekday.TUESDAY);
            discussion1times[2].addMeetingDay(Weekday.THURSDAY);
            discussion1times[2].setStartDate(dateFormatter.parse("09/05/2006"));
            discussion1times[2].setEndDate(dateFormatter.parse("12/13/2006"));
            discussion1times[2].setStartTime(timeFormatter.parse("12:20"));
            discussion1times[2].setEndTime(timeFormatter.parse("13:10"));
            discussion1times[2].setLocation("AmundH 116, TCEASTBANK");
            discussion1times[3] = new MeetingTime(document, discussions1[3]);
            discussion1times[3].addMeetingDay(Weekday.TUESDAY);
            discussion1times[3].addMeetingDay(Weekday.THURSDAY);
            discussion1times[3].setStartDate(dateFormatter.parse("09/05/2006"));
            discussion1times[3].setEndDate(dateFormatter.parse("12/13/2006"));
            discussion1times[3].setStartTime(timeFormatter.parse("15:35"));
            discussion1times[3].setEndTime(timeFormatter.parse("16:25"));
            discussion1times[3].setLocation("AkerH 313, TCEASTBANK ");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        discussions1[0].addMeetingTime(discussion1times[0]);
        discussions1[1].addMeetingTime(discussion1times[1]);
        discussions1[2].addMeetingTime(discussion1times[2]);
        discussions1[3].addMeetingTime(discussion1times[3]);
        courses.add(course4);
        Course course5 = new Course(document);
        course5.setDepartment("CHEN");
        course5.setCourseNumber("4006");
        course5.setName("Mass Transport and Separation Processes");
        course5.setPrereqs("[4001 or dept consent]");
        course5.setDescription("Student may contact the instructor or department for information.");
        lecture = new Section(course5);
        lecture.setSectionNumber("001");
        lecture.setStaff("Cussler Jr,Edward L");
        lecture.setType("LEC");
        lecture.setCredits(4);
        try {
            lecTime = new MeetingTime(document, lecture);
            lecTime = new MeetingTime(document, lecture);
            lecTime.addMeetingDay(Weekday.MONDAY);
            lecTime.addMeetingDay(Weekday.WEDNESDAY);
            lecTime.addMeetingDay(Weekday.FRIDAY);
            lecTime.setStartDate(dateFormatter.parse("09/05/2006"));
            lecTime.setEndDate(dateFormatter.parse("12/13/2006"));
            lecTime.setStartTime(timeFormatter.parse("12:25"));
            lecTime.setEndTime(timeFormatter.parse("14:15"));
            lecTime.setLocation("AmundH  B75 , TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        lecture.addMeetingTime(lecTime);
        discussions1 = new Section[3];
        discussions1[0] = new Section(lecture);
        discussions1[0].setSectionNumber("003");
        discussions1[0].setStaff("Cussler Jr,Edward L");
        discussions1[0].setType("DIS");
        discussions1[0].setCredits(4);
        discussions1[1] = new Section(lecture);
        discussions1[1].setSectionNumber("004");
        discussions1[1].setStaff("Cussler Jr,Edward L");
        discussions1[1].setType("DIS");
        discussions1[1].setCredits(4);
        discussions1[2] = new Section(lecture);
        discussions1[2].setSectionNumber("005");
        discussions1[2].setStaff("Cussler Jr,Edward L");
        discussions1[2].setType("DIS");
        discussions1[2].setCredits(4);
        discussion1times = new MeetingTime[4];
        try {
            discussion1times[0] = new MeetingTime(document, discussions1[0]);
            discussion1times[0].addMeetingDay(Weekday.TUESDAY);
            discussion1times[0].addMeetingDay(Weekday.THURSDAY);
            discussion1times[0].setStartDate(dateFormatter.parse("09/05/2006"));
            discussion1times[0].setEndDate(dateFormatter.parse("12/13/2006"));
            discussion1times[0].setStartTime(timeFormatter.parse("10:10"));
            discussion1times[0].setEndTime(timeFormatter.parse("11:00"));
            discussion1times[0].setLocation("AmundH  158 , TCEASTBANK");
            discussion1times[1] = new MeetingTime(document, discussions1[1]);
            discussion1times[1].addMeetingDay(Weekday.TUESDAY);
            discussion1times[1].addMeetingDay(Weekday.THURSDAY);
            discussion1times[1].setStartDate(dateFormatter.parse("09/05/2006"));
            discussion1times[1].setEndDate(dateFormatter.parse("12/13/2006"));
            discussion1times[1].setStartTime(timeFormatter.parse("12:20"));
            discussion1times[1].setEndTime(timeFormatter.parse("13:10"));
            discussion1times[1].setLocation("AmundH  156 , TCEASTBANK");
            discussion1times[2] = new MeetingTime(document, discussions1[2]);
            discussion1times[2].addMeetingDay(Weekday.TUESDAY);
            discussion1times[2].addMeetingDay(Weekday.THURSDAY);
            discussion1times[2].setStartDate(dateFormatter.parse("09/05/2006"));
            discussion1times[2].setEndDate(dateFormatter.parse("12/13/2006"));
            discussion1times[2].setStartTime(timeFormatter.parse("14:30"));
            discussion1times[2].setEndTime(timeFormatter.parse("15:20"));
            discussion1times[2].setLocation("AmundH  156 , TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        discussions1[0].addMeetingTime(discussion1times[0]);
        discussions1[1].addMeetingTime(discussion1times[1]);
        discussions1[2].addMeetingTime(discussion1times[2]);
        courses.add(course5);
        Course course6 = new Course(document);
        course6.setDepartment("CSCL");
        course6.setCourseNumber("3461");
        course6.setName("Monsters, Robots, Cyborgs  ");
        course6.setDescription("Monsters, Robots and Cyborgs will be a theoretical and historical investigation of these three figures of radical difference. The monster not only gives birth to robot and cyborg, it embodies the fantasies of aberrant reproduction and indestructibility that will characterize its technologized mutations. The goals of the course is to familiarize students with critical issues in the study of Comparative Literature and Film. By providing students with a critical genealogy of the monster, robot and cyborg, this course will emphasize relationships between oral tradition and literary forms, Ancient perspectives and Modern practices, myth and technology, monstrosity and human rights, and psychoanalysis and science fiction and cyborgs and the body politic.");
        course6.setLibEds("Literature Core");
        lecture = new Section(course6);
        lecture.setSectionNumber("002");
        lecture.setStaff("Stout,Graeme Allen");
        lecture.setType("LEC");
        lecture.setCredits(3);
        try {
            lecTime = new MeetingTime(document, lecture);
            lecTime.addMeetingDay(Weekday.MONDAY);
            lecTime.addMeetingDay(Weekday.WEDNESDAY);
            lecTime.addMeetingDay(Weekday.FRIDAY);
            lecTime.setStartDate(dateFormatter.parse("09/05/2006"));
            lecTime.setEndDate(dateFormatter.parse("12/13/2006"));
            lecTime.setStartTime(timeFormatter.parse("12:20"));
            lecTime.setEndTime(timeFormatter.parse("13:10"));
            lecTime.setLocation("NichH 125, TCEASTBANK");
        } catch (ParseException p) {
            p.printStackTrace();
            throw new RuntimeException(p);
        }
        lecture.addMeetingTime(lecTime);
        courses.add(course6);
        return courses;
    }
}
