package homelesspartners.client.model;

import java.io.Serializable;

public class Story implements Serializable {

    private static final String SPACE = " ";

    private static final long serialVersionUID = 1L;

    public static final char MALE = 'M';

    public static final char FEMALE = 'F';

    public static final int MAX_GIFTS = 4;

    private int id = -1;

    private String assignedId = null;

    private String firstName = null;

    private String lastInitial = null;

    private String story = null;

    private Shelter shelter = null;

    private char gender = MALE;

    private Gift[] gifts = new Gift[MAX_GIFTS];

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(id);
        buffer.append(SPACE);
        buffer.append(assignedId);
        buffer.append(SPACE);
        buffer.append(firstName);
        buffer.append(SPACE);
        buffer.append(lastInitial);
        buffer.append(SPACE);
        buffer.append(story);
        buffer.append(SPACE);
        buffer.append(shelter.getId());
        buffer.append(SPACE);
        buffer.append(gender);
        for (int x = 0; x < gifts.length; x++) {
            if (gifts[x] != null) {
                buffer.append(SPACE);
                buffer.append(gifts[x].toString());
            }
        }
        return buffer.toString();
    }

    public String getAssignedId() {
        return assignedId;
    }

    public void setAssignedId(String assignedId) {
        this.assignedId = assignedId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLastInitial() {
        return lastInitial;
    }

    public void setLastInitial(String lastInitial) {
        this.lastInitial = lastInitial;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public void addGift(Gift aGift) {
        for (int x = 0; x < gifts.length; x++) {
            if (gifts[x] == null) {
                gifts[x] = aGift;
                aGift.setStory(this);
                return;
            }
            if (gifts[x].equals(aGift)) {
                return;
            }
        }
    }

    public void removeGift(Gift aGift) {
        for (int x = 0; x < gifts.length; x++) {
            if (gifts[x] != null) {
                if (gifts[x].getId() == aGift.getId()) {
                    gifts[x] = null;
                    for (int y = x; y < gifts.length - 1; y++) {
                        gifts[y] = gifts[y + 1];
                        gifts[y + 1] = null;
                    }
                    return;
                }
            }
        }
    }

    public Shelter getShelter() {
        return shelter;
    }

    public void setShelter(Shelter shelter) {
        this.shelter = shelter;
    }

    public boolean equals(Object obj) {
        Story compared = (Story) obj;
        if (this.getId() == compared.getId()) {
            return true;
        }
        return false;
    }

    public char getGender() {
        return gender;
    }

    public void setGender(char gender) {
        this.gender = gender;
    }

    public String getHeaderText() {
        StringBuffer headerText = new StringBuffer(firstName);
        headerText.append(SPACE);
        headerText.append(lastInitial);
        headerText.append(", ");
        if (gender == MALE) {
            headerText.append("Male");
        } else {
            headerText.append("Female");
        }
        headerText.append(", ID#");
        headerText.append(assignedId);
        return headerText.toString();
    }

    public String validate() {
        if (shelter == null) {
            return "Please choose a shelter.";
        }
        if (firstName == null || firstName.length() == 0) {
            return "Please enter a first name.";
        }
        if (lastInitial == null || lastInitial.length() != 1) {
            return "Please enter a valid last initial.";
        }
        if (gender != MALE && gender != FEMALE) {
            return "Please enter a valid gender.";
        }
        if (assignedId == null || assignedId.length() == 0) {
            return "Please enter an id number for this person.";
        }
        if (story == null || story.length() == 0) {
            return "Please enter a story for this person.";
        }
        if (story.length() > 1024) {
            return "The story details are greater than the maximum allowed length.  Please edit the details so it is less than 1000 characters.";
        }
        if (gifts[0] == null) {
            return "Please add at least one gift request (you must click 'Add Gift' for the gift to be added).";
        }
        return null;
    }

    public Gift[] getGifts() {
        return gifts;
    }

    public int getGiftCount() {
        int count = 0;
        for (int x = 0; x < gifts.length; x++) {
            if (gifts[x] != null) {
                count++;
            }
        }
        return count;
    }
}
