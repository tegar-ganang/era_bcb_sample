import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.activity.ActivityInterface;
import com.aetrion.flickr.activity.Event;
import com.aetrion.flickr.activity.Item;
import com.aetrion.flickr.activity.ItemList;
import com.aetrion.flickr.groups.pools.PoolsInterface;
import com.aetrion.flickr.tags.Tag;
import com.aetrion.flickr.tags.TagsInterface;
import com.aetrion.flickr.util.IOUtilities;
import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.geo.GeoInterface;

/**
 * Utility app for retrieving photos & metadata from Flickr
 *
 * @author Todd Margolis
 * @version $Id: flickr-harvester, v1.0 6/9/2011
 * 
 * Copyright 2011 Todd Margolis

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
public class getFlickrData {

    static String apiKey;

    static String sharedSecret;

    Flickr f;

    REST rest;

    RequestContext requestContext;

    Properties properties = null;

    public Set<String> tags = new HashSet<String>();

    Photo myPhoto = null;

    String photoId = null;

    Photo myPhotoTags = null;

    String groupId = null;

    PoolsInterface pi = null;

    PhotosInterface myPhotoInterface = null;

    TagsInterface myTagsInterface = null;

    PeopleInterface myPeopleInterface = null;

    GeoInterface myGeoInterface = null;

    PhotoList full_pl = null;

    InputStream is = null;

    User photoUserID = null;

    User photoUser = null;

    GeoData photoLocation = null;

    BufferedWriter outIDs, outTags, outTitles, outPeople, outLocation;

    static Boolean writeIDs = false;

    static Boolean writeTitles = false;

    static Boolean writeTags = false;

    static Boolean writePhotos = false;

    static Boolean writePeople = false;

    static Boolean writeLocation = true;

    String[] tagsFilter = {};

    int perPage = 100;

    int page = 70;

    int origPerPage = perPage;

    int origPage = page;

    int faultCounter = 0;

    int photoCounter = 0;

    int numLost = 0;

    int numPages, origNumPages, numTotal, numExpected;

    public getFlickrData() throws ParserConfigurationException, IOException {
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream("/setup.properties.txt");
            properties = new Properties();
            properties.load(in);
        } finally {
            IOUtilities.close(in);
        }
        f = new Flickr(properties.getProperty("apiKey"), properties.getProperty("secret"), new REST());
    }

    public void getPool() {
        REST myRest = null;
        try {
            myRest = new REST();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        groupId = properties.getProperty("groupID");
        pi = new PoolsInterface(properties.getProperty("apiKey"), properties.getProperty("secret"), myRest);
        myPhotoInterface = new PhotosInterface(properties.getProperty("apiKey"), properties.getProperty("secret"), myRest);
        myTagsInterface = new TagsInterface(properties.getProperty("apiKey"), properties.getProperty("secret"), myRest);
        myPeopleInterface = new PeopleInterface(properties.getProperty("apiKey"), properties.getProperty("secret"), myRest);
        myGeoInterface = new GeoInterface(properties.getProperty("apiKey"), properties.getProperty("secret"), myRest);
        full_pl = new PhotoList();
        if (writeIDs) {
            try {
                outIDs = new BufferedWriter(new FileWriter(properties.getProperty("photoIDs")));
            } catch (IOException e) {
                System.out.print("Exception ");
                e.printStackTrace();
            }
        }
        if (writeTags) {
            try {
                outTags = new BufferedWriter(new FileWriter(properties.getProperty("photoTags")));
            } catch (IOException e) {
                System.out.print("Exception ");
                e.printStackTrace();
            }
        }
        if (writeTitles) {
            try {
                outTitles = new BufferedWriter(new FileWriter(properties.getProperty("photoTitles")));
            } catch (IOException e) {
                System.out.print("Exception ");
                e.printStackTrace();
            }
        }
        if (writePeople) {
            try {
                outPeople = new BufferedWriter(new FileWriter(properties.getProperty("photoPeople")));
            } catch (IOException e) {
                System.out.print("Exception ");
                e.printStackTrace();
            }
        }
        if (writeLocation) {
            try {
                outLocation = new BufferedWriter(new FileWriter(properties.getProperty("photoLocation")));
                outLocation.write("ID\tLat\tLon");
            } catch (IOException e) {
                System.out.print("Exception ");
                e.printStackTrace();
            }
        }
        try {
            full_pl = pi.getPhotos(groupId, tagsFilter, perPage, page);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (FlickrException e) {
            e.printStackTrace();
        }
        origNumPages = numPages = full_pl.getPages();
        numTotal = full_pl.getTotal();
        numExpected = numTotal - ((page - 1) * perPage);
        System.out.println("total num results in pool = " + Integer.toString(numTotal));
        System.out.println("Grabbing pages " + page + "-" + Integer.toString(numPages));
        System.out.println("Expected num results = " + Integer.toString(numExpected));
        for (int p = page; p <= numPages; p++) {
            System.out.print("getting page " + Integer.toString(p) + " list ");
            PhotoList page_pl = new PhotoList();
            try {
                page_pl = pi.getPhotos(groupId, tagsFilter, perPage, p);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
                System.out.println("!!! ERROR retrieving list of photos for page " + Integer.toString(p));
                if (perPage == 1) {
                    System.out.println("!!!!!!!!!!!!!!!!!!!!! ERROR retrieving page for photo " + Integer.toString(p) + " so it is being skipped !!!!!!!!!!!!!!!");
                    faultCounter--;
                    numLost++;
                    continue;
                } else {
                    origPage = p;
                    page = (p - 1) * perPage + 1;
                    p = page;
                    faultCounter = perPage;
                    perPage = 1;
                    System.out.println("Going back to page " + Integer.toString(p) + " and grabbing 1 per page");
                }
                try {
                    page_pl = pi.getPhotos(groupId, tagsFilter, perPage, p);
                } catch (IOException eB) {
                    eB.printStackTrace();
                } catch (SAXException eB) {
                    eB.printStackTrace();
                    System.out.println("!!!!!!!!!!!!!!!!!!!!! ERROR retrieving page for photo " + Integer.toString(p) + " so it is being skipped !!!!!!!!!!!!!!!");
                    faultCounter--;
                    numLost++;
                    continue;
                } catch (FlickrException eB) {
                    eB.printStackTrace();
                }
                numPages = page_pl.getPages();
            } catch (FlickrException e) {
                e.printStackTrace();
            }
            System.out.print("(" + Integer.toString(page_pl.size()) + " photos)");
            for (int i = 0; i < page_pl.size(); i++) {
                myPhoto = (Photo) page_pl.get(i);
                photoId = myPhoto.getId();
                System.out.print(".");
                if (writeIDs) {
                    try {
                        outIDs.write(photoId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (writeTitles) {
                    try {
                        outTitles.write(photoId + "\t" + myPhoto.getDateAdded().getTime() + "\t" + myPhoto.getTitle());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (writePeople) {
                    try {
                        photoUserID = myPhoto.getOwner();
                        String uID = photoUserID.getId();
                        try {
                            photoUser = myPeopleInterface.getInfo(uID);
                        } catch (SAXException e) {
                            e.printStackTrace();
                        } catch (FlickrException e) {
                            e.printStackTrace();
                        }
                        outPeople.write(photoId + "\t" + photoUser.getUsername() + "\t" + photoUser.getRealName() + "\t" + photoUser.getLocation() + "\t" + Integer.toString(photoUser.getPhotosCount()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (writeTags) {
                    myPhotoTags = (Photo) page_pl.get(i);
                    writeOutTags(photoId);
                }
                if (writeLocation) {
                    try {
                        photoLocation = myGeoInterface.getLocation(photoId);
                        try {
                            outLocation.write(photoId + "\t" + photoLocation.getLatitude() + "\t" + photoLocation.getLongitude());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } catch (SAXException e1) {
                        e1.printStackTrace();
                    } catch (FlickrException e1) {
                        try {
                            outLocation.write(photoId + "\t" + null + "\t" + null);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (writeIDs) {
                    try {
                        outIDs.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (writeTitles) {
                    try {
                        outTitles.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (writePeople) {
                    try {
                        outPeople.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (writeLocation) {
                    try {
                        outLocation.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (writePhotos) {
                    try {
                        is = myPhotoInterface.getImageAsStream(myPhoto, com.aetrion.flickr.photos.Size.MEDIUM);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (FlickrException e) {
                        e.printStackTrace();
                    }
                    File f = new File(properties.getProperty("photoDir") + "/" + photoId + ".jpg");
                    try {
                        OutputStream out = new FileOutputStream(f);
                        byte buf[] = new byte[1024];
                        int len;
                        while ((len = is.read(buf)) > 0) out.write(buf, 0, len);
                        out.close();
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                photoCounter++;
            }
            if (writeIDs) {
                try {
                    outIDs.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (writeTitles) {
                try {
                    outTitles.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (writeTags) {
                try {
                    outTags.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (writePeople) {
                try {
                    outPeople.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (writeLocation) {
                try {
                    outLocation.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (faultCounter > 1) {
                System.out.println("!faultCounter: " + Integer.toString(faultCounter));
                faultCounter--;
            } else if (faultCounter == 1) {
                System.out.println("!!!faultCounter: " + Integer.toString(faultCounter));
                faultCounter--;
                perPage = origPerPage;
                p = origPage;
                numPages = origNumPages;
            }
            System.out.println("");
        }
        if (writeIDs) {
            try {
                outIDs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (writeTags) {
            try {
                outTags.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (writeTitles) {
            try {
                outTitles.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (writePeople) {
            try {
                outTitles.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (writeLocation) {
            try {
                outLocation.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Grabbed " + photoCounter + " photos");
        System.out.println("Skipped " + numLost + " photos");
        System.out.println("Missing " + Integer.toString(numExpected - photoCounter - numLost) + " photos from an expected " + numExpected);
        System.out.println("!!!!!!!!!! DONE !!!!!!!!!!");
    }

    private void writeOutTags(String photoId) {
        try {
            myPhotoTags = myTagsInterface.getListPhoto(photoId);
            if (!(myPhotoTags.getTags() == null)) {
                List<Tag> tagList = new ArrayList<Tag>(myPhotoTags.getTags());
                try {
                    outTags.write(photoId + "\t");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (Tag tag : tagList) {
                    this.tags.add(tag.getValue());
                    try {
                        outTags.write(tag.getValue() + "\t");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                outTags.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("IOException for photoID " + photoId);
            e.printStackTrace();
            writeOutTags(photoId);
        } catch (SAXException e) {
            System.out.println("SAXException for photoID " + photoId);
            e.printStackTrace();
            writeOutTags(photoId);
        } catch (FlickrException e) {
            System.out.println("FlickrException for photoID " + photoId);
            e.printStackTrace();
            writeOutTags(photoId);
        }
    }

    public static void main(String[] args) {
        try {
            getFlickrData f = new getFlickrData();
            f.getPool();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
