package com.itextpdf.devoxx.sections;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import com.itextpdf.devoxx.dao.EventDAO;
import com.itextpdf.devoxx.dao.PresentationDAO;
import com.itextpdf.devoxx.dao.RoomDAO;
import com.itextpdf.devoxx.dao.ScheduleDAO;
import com.itextpdf.devoxx.helpers.MyObjectFactory;
import com.itextpdf.devoxx.pojos.ConferenceDay;
import com.itextpdf.devoxx.pojos.Event;
import com.itextpdf.devoxx.pojos.Section;
import com.itextpdf.devoxx.pojos.Presentation;
import com.itextpdf.devoxx.pojos.Room;
import com.itextpdf.devoxx.pojos.ScheduleItem;
import com.itextpdf.devoxx.pojos.TimeSlot;
import com.itextpdf.devoxx.properties.Dimensions;
import com.itextpdf.devoxx.properties.MyColors;
import com.itextpdf.devoxx.properties.MyFonts;
import com.itextpdf.devoxx.properties.MyProperties;
import com.itextpdf.devoxx.properties.Dimensions.Dimension;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.GrayColor;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Creates single page PDF files containing a schedule overview
 * and a program guide containing the university and conference schedules.
 */
public class Schedules {

    /** Section information (retrieved from a JSON properties file). */
    public static final Section INFO = MyProperties.getMiddle()[1];

    /** The width of the Schedule page. */
    public static final float WIDTH = Dimensions.getDimension(true, Dimension.BODY).getWidth();

    /** The line width of the borders. */
    public static final float LINEWIDTH = 0.5f;

    /** The height of the titles with the room names. */
    public static final float ROOM_HEIGHT = 15;

    /** The spacing between the room name titles and the slots. */
    public static final float ROOM_SPACING = 8;

    /** The height of the slot titles. */
    public static final float PRESENTATION_TITLES = 11;

    /** The height of a normal time slot. */
    public static final float TIMESLOT_HEIGHT_NORMAL = 70;

    /** The height of a normal time slot. */
    public static final float TIMESLOT_HEIGHT_KEYNOTE = 40;

    /** The height of a plenary time slot. */
    public static final float TIMESLOT_HEIGHT_PLENARY = 15;

    /** The width of the time slot titles (to the left). */
    public static final float TIMESLOT_TITLE_WIDTH = 55;

    /** The width of the time slot bracket. */
    public static final float TIMESLOT_BRACKET = 15;

    /** The spacing between the time slots (actually half of it). */
    public static final float TIMESLOT_SPACING = 1.5f;

    private static final String CONFERENCE = "CONFERENCE";

    private static final String UNIVERSITY = "UNIVERSITY";

    private static final String KEYNOTE = "Keynote";

    private static final String KEYNOTES = "Keynotes";

    /**
	 * Creates a PDF containing the program guide.
	 * @param eventUri the event URI
	 * @param eventUri the resulting PDF file
	 * @throws IOException
	 * @throws DocumentException
	 */
    public void createPdf(final String eventUri) throws IOException, DocumentException {
        final Rectangle rect = Dimensions.getDimension(true, Dimension.BODY);
        final Document document = new Document(rect, 0, 0, 0, 0);
        final PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(INFO.getOutput()));
        writer.setInitialLeading(18);
        document.open();
        Image image;
        Event event = EventDAO.getEvent(eventUri);
        Date today = event.getFrom();
        Paragraph p;
        ConferenceDay[] days = MyProperties.getDays();
        for (int i = 0; i < days.length; i++) {
            image = getSchedule(writer, ScheduleDAO.getScheduleForDay(eventUri, i + 1), days[i].getType(), days[i].getTitle());
            if (writer.getVerticalPosition(false) - image.getScaledHeight() < rect.getBottom()) {
                document.newPage();
            }
            p = new Paragraph(MyObjectFactory.dateFormat(today), MyFonts.DATE);
            p.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(p);
            today = MyObjectFactory.incrementDate(today, 1);
            document.add(image);
        }
        final PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(90);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        PdfReader reader = new PdfReader(MyProperties.getPlan1());
        table.addCell(Image.getInstance(writer.getImportedPage(reader, 1)));
        reader = new PdfReader(MyProperties.getPlan2());
        table.addCell(Image.getInstance(writer.getImportedPage(reader, 1)));
        document.add(table);
        document.close();
    }

    /**
	 * Gets a schedule as an Image object.
	 * @param writer the writer to which the Schedule has to be added.
	 * @param items a set of ScheduleItem objects
	 * @param type the type for which you want the schedule
	 * @param color a color that will be used for the background
	 * @return an Image object
	 * @throws IOException
	 * @throws DocumentException
	 */
    public static Image getSchedule(final PdfWriter writer, final Set<ScheduleItem> items, final String type, final String color) throws IOException, DocumentException {
        final PdfReader reader = new PdfReader(getSchedule(items, type, color));
        final PdfImportedPage page = writer.getImportedPage(reader, 1);
        return Image.getInstance(page);
    }

    /**
	 * Creates a single page PDF document, containing a schedule overview.
	 * @param items a Set of ScheduleItems
	 * @param type a type of presentation
	 * @param color
     * @return a byte array containing a single page PDF file
	 * @throws IOException
	 * @throws DocumentException
	 */
    public static byte[] getSchedule(final Set<ScheduleItem> items, final String type, final String color) throws IOException, DocumentException {
        SortedSet<TimeSlot> timeSlots = getTimeSlots(items, type);
        final HashMap<String, Float> startY = new HashMap<String, Float>();
        TimeSlot timeslot;
        float height = 0;
        while (!timeSlots.isEmpty()) {
            timeslot = timeSlots.last();
            height += timeslot.isPlenary() ? TIMESLOT_HEIGHT_PLENARY : timeslot.isKeynote() ? TIMESLOT_HEIGHT_KEYNOTE : TIMESLOT_HEIGHT_NORMAL;
            startY.put(timeslot.toString(), height);
            timeSlots.remove(timeslot);
        }
        height += ROOM_HEIGHT + ROOM_SPACING;
        final SortedSet<Room> rooms = getRooms(items, type);
        final HashMap<String, Float> startX = new HashMap<String, Float>();
        float roomWidth = (WIDTH - TIMESLOT_TITLE_WIDTH - TIMESLOT_BRACKET) / rooms.size();
        float x = TIMESLOT_TITLE_WIDTH + TIMESLOT_BRACKET;
        for (final Room room : rooms) {
            startX.put(room.getName(), x);
            x += roomWidth;
        }
        final Document document = new Document(new Rectangle(WIDTH, height));
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();
        final PdfContentByte canvas = writer.getDirectContent();
        float llx, lly, urx, ury;
        if (!(CONFERENCE.equalsIgnoreCase(type) || UNIVERSITY.equalsIgnoreCase(type))) {
            llx = 0;
            lly = height - ROOM_HEIGHT;
            urx = TIMESLOT_TITLE_WIDTH + TIMESLOT_BRACKET;
            ury = height;
            ColumnText column = new ColumnText(canvas);
            column.setSimpleColumn(llx, lly, urx, ury);
            column.setLeading(0, 1.5f);
            column.setText(new Phrase(type, MyFonts.SMALL));
            column.go();
        }
        timeSlots = getTimeSlots(items, type);
        for (final TimeSlot time : timeSlots) {
            llx = 0;
            urx = TIMESLOT_TITLE_WIDTH;
            ury = startY.get(time.toString());
            lly = ury - (time.isPlenary() ? TIMESLOT_HEIGHT_PLENARY : time.isKeynote() ? TIMESLOT_HEIGHT_KEYNOTE : TIMESLOT_HEIGHT_NORMAL);
            canvas.saveState();
            canvas.setLineWidth(LINEWIDTH);
            canvas.moveTo(urx + TIMESLOT_BRACKET, ury - TIMESLOT_SPACING);
            canvas.lineTo(urx, ury - TIMESLOT_SPACING);
            canvas.lineTo(urx, lly + TIMESLOT_SPACING);
            canvas.lineTo(urx + TIMESLOT_BRACKET, lly + TIMESLOT_SPACING);
            canvas.stroke();
            canvas.restoreState();
            Font f = new Font(MyFonts.SMALL);
            if (!time.isPlenary()) f.setColor(MyColors.getColor(color));
            MyObjectFactory.centerText(canvas, new Phrase(time.toString(), f), llx, lly, urx, ury);
        }
        Rectangle rectangle;
        for (final Room room : rooms) {
            llx = startX.get(room.getName());
            urx = llx + roomWidth;
            lly = height - ROOM_HEIGHT;
            ury = height;
            rectangle = new Rectangle(llx, lly, urx, ury);
            rectangle.setBackgroundColor(getRoomColor(room, rooms));
            canvas.rectangle(rectangle);
            getRoomColor(room, rooms);
            MyObjectFactory.centerText(canvas, new Phrase(room.getName().toUpperCase(), MyFonts.BOLD), llx, lly, urx, ury);
        }
        Room room;
        TimeSlot time;
        BaseColor background;
        String presentationURI;
        for (final ScheduleItem item : items) {
            if (!type.equals(item.getType())) {
                continue;
            }
            room = RoomDAO.getRoom(item.getRoom());
            time = new TimeSlot(item);
            ury = startY.get(time.toString()) - TIMESLOT_SPACING;
            lly = ury - (time.isPlenary() ? TIMESLOT_HEIGHT_PLENARY : time.isKeynote() ? TIMESLOT_HEIGHT_KEYNOTE : TIMESLOT_HEIGHT_NORMAL) + 2 * TIMESLOT_SPACING;
            if (room.isPlenary()) {
                llx = TIMESLOT_TITLE_WIDTH + TIMESLOT_BRACKET;
                urx = WIDTH;
                background = MyColors.getColor(color);
            } else if (KEYNOTE.equals(item.getKind())) {
                llx = TIMESLOT_TITLE_WIDTH + TIMESLOT_BRACKET;
                urx = WIDTH;
                background = getRoomColor(room, rooms);
            } else {
                llx = startX.get(room.getName());
                urx = llx + roomWidth;
                background = getRoomColor(room, rooms);
            }
            presentationURI = item.getPresentationUri();
            if (presentationURI == null) {
                drawEmptyPresentation(canvas, llx, lly, urx, ury, background, item);
            } else {
                rectangle = new Rectangle(llx, lly, urx, ury);
                rectangle.setBackgroundColor(background);
                rectangle.setBorderColor(GrayColor.GRAYBLACK);
                rectangle.setBorder(Rectangle.BOTTOM);
                rectangle.setBorderWidth(LINEWIDTH);
                canvas.rectangle(rectangle);
                rectangle = new Rectangle(llx, ury - PRESENTATION_TITLES, urx, ury);
                rectangle.setBackgroundColor(new GrayColor(0.3f));
                canvas.rectangle(rectangle);
                final Presentation p = PresentationDAO.getPresentation(presentationURI);
                if (p == null) {
                    drawEmptyPresentation(canvas, llx, lly, urx, ury, background, item);
                } else if (KEYNOTE.equals(item.getKind())) {
                    MyObjectFactory.centerText(canvas, new Phrase(KEYNOTES, MyFonts.SMALLCAPS_WHITE), llx, ury - PRESENTATION_TITLES, urx, ury);
                    MyObjectFactory.centerText(canvas, new Phrase("Keynote happens in room 8 and overflow in room 5, 4 and 9", MyFonts.SMALL), urx - roomWidth, lly, urx, ury - PRESENTATION_TITLES);
                    MyObjectFactory.centerText(canvas, MyObjectFactory.getPresentation(p, MyFonts.SMALL, MyFonts.SMALLCAPS_TINY), llx, lly, urx - roomWidth, ury - PRESENTATION_TITLES);
                } else {
                    MyObjectFactory.centerText(canvas, new Phrase(p.getTrack(), MyFonts.SMALLCAPS_WHITE), llx, ury - PRESENTATION_TITLES, urx, ury);
                    MyObjectFactory.centerText(canvas, MyObjectFactory.getPresentation(p, MyFonts.SMALL, MyFonts.SMALLCAPS_TINY), llx, lly, urx, ury - PRESENTATION_TITLES);
                }
            }
        }
        document.close();
        return baos.toByteArray();
    }

    private static void drawEmptyPresentation(final PdfContentByte canvas, final float llx, final float lly, final float urx, final float ury, final BaseColor background, final ScheduleItem item) throws DocumentException {
        Rectangle rectangle;
        rectangle = new Rectangle(llx, lly, urx, ury);
        rectangle.setBackgroundColor(background);
        canvas.rectangle(rectangle);
        MyObjectFactory.centerText(canvas, new Phrase(item.getCode(), MyFonts.SMALLCAPS), llx, lly, urx, ury);
    }

    /**
	 * Returns a sorted set containing TimeSlot objects that are used in
	 * a series of scheduled presentations of a given type.
	 * @param items a set of scheduled items
	 * @param type a given type of presentation
	 * @return a SortedSet of TimeSlot objects
	 */
    public static SortedSet<TimeSlot> getTimeSlots(final Set<ScheduleItem> items, final String type) {
        final SortedSet<TimeSlot> timeSlots = new TreeSet<TimeSlot>();
        for (final ScheduleItem item : items) {
            if (type.equals(item.getType())) {
                timeSlots.add(new TimeSlot(item));
            }
        }
        return timeSlots;
    }

    /**
	 * Returns a sorted set containing Room objects of rooms that are used in
	 * a series of scheduled presentations of a given type.
	 * @param items a set of scheduled items
	 * @param type a given type of presentation
	 * @return a SortedSet of Room objects
	 */
    public static SortedSet<Room> getRooms(final Set<ScheduleItem> items, final String type) {
        final SortedSet<Room> rooms = new TreeSet<Room>();
        Room room;
        for (final ScheduleItem item : items) {
            room = RoomDAO.getRoom(item.getRoom());
            if (type.equals(item.getType()) && !room.isPlenary()) {
                rooms.add(room);
            }
        }
        return rooms;
    }

    /**
	 * Returns the background color of a room, based on its index in a sorted set of rooms.
	 * @param room the room for which we want the background color
	 * @param rooms the sorted set of rooms
	 * @return a BaseColor
	 */
    public static BaseColor getRoomColor(final Room room, final SortedSet<Room> rooms) {
        return new GrayColor(0.95f - (0.05f * rooms.headSet(room).size()));
    }

    public static void main(String[] args) throws IOException, DocumentException {
        new Schedules().createPdf(MyProperties.getEventURI());
    }
}
