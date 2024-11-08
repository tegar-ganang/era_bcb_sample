package action;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletContext;
import model.Facility;
import model.Image;
import model.Room;
import model.RoomType;
import model.UserAware;
import model.internal.Message;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Actions;
import org.apache.struts2.convention.annotation.InterceptorRef;
import org.apache.struts2.convention.annotation.InterceptorRefs;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.SessionAware;
import org.springframework.beans.factory.annotation.Autowired;
import service.FacilityService;
import service.ImageService;
import service.RoomService;
import service.RoomTypeService;
import service.StructureService;
import com.opensymphony.xwork2.ActionSupport;

@ParentPackage(value = "default")
@InterceptorRefs({ @InterceptorRef("userAwareStack") })
@Result(name = "notLogged", location = "/homeNotLogged.jsp")
public class UploadAction extends ActionSupport implements SessionAware, UserAware {

    private Map<String, Object> session = null;

    private File upload;

    private String uploadFileName;

    private String uploadContentType;

    private String name;

    private Message message = new Message();

    private Facility roomFacility = null;

    private Room room = null;

    private RoomType roomType = null;

    private Facility structureFacility = null;

    private Image image = null;

    private Integer idStructure;

    @Autowired
    private StructureService structureService = null;

    @Autowired
    private RoomTypeService roomTypeService = null;

    @Autowired
    private RoomService roomService = null;

    @Autowired
    private ImageService imageService = null;

    @Autowired
    private FacilityService facilityService = null;

    @Actions({ @Action(value = "/uploadFacilityIF", results = { @Result(name = "success", location = "/message_upload.jsp"), @Result(name = "error", location = "/message_upload.jsp") }), @Action(value = "/uploadRoomTypeFacilityIF", results = { @Result(name = "success", location = "/message_upload.jsp"), @Result(name = "error", location = "/message_upload.jsp") }), @Action(value = "/uploadFacility", results = { @Result(type = "json", name = "success", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }), @Result(type = "json", name = "error", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }) }), @Action(value = "/uploadRoomTypeFacility", results = { @Result(type = "json", name = "success", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }), @Result(type = "json", name = "error", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }) }) })
    public String uploadRoomOrTypeFacility() throws IOException {
        ServletContext context = null;
        String imgPath = null;
        File target = null;
        if (this.getFacilityService().findUploadedFacilityByName(this.getIdStructure(), this.getName()) != null) {
            message.setResult(Message.ERROR);
            message.setDescription(getText("facilityAlreadyPresentError"));
            return ERROR;
        }
        ;
        context = ServletActionContext.getServletContext();
        imgPath = context.getRealPath("/") + "resources/" + this.getIdStructure() + "/facilities/roomOrRoomType/";
        target = new File(imgPath + this.getUploadFileName());
        FileUtils.copyFile(this.getUpload(), target);
        this.setRoomFacility(new Facility());
        this.getRoomFacility().setName(this.getName());
        this.getRoomFacility().setFileName(this.getUploadFileName());
        this.getRoomFacility().setId_structure(this.getIdStructure());
        this.getFacilityService().insertUploadedFacility(this.getRoomFacility());
        message.setResult(Message.SUCCESS);
        message.setDescription(getText("facilityAddSuccessAction"));
        return SUCCESS;
    }

    @Actions({ @Action(value = "/uploadStructureFacilityIF", results = { @Result(name = "success", location = "/message_upload.jsp"), @Result(name = "error", location = "/message_upload.jsp") }), @Action(value = "/uploadStructureFacility", results = { @Result(type = "json", name = "success", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }), @Result(type = "json", name = "error", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }) }) })
    public String uploadStructureFacility() throws IOException {
        ServletContext context = null;
        String imgPath = null;
        File target = null;
        if (this.getFacilityService().findStructureFacilityByName(this.getIdStructure(), this.getName()) != null) {
            message.setResult(Message.ERROR);
            message.setDescription(getText("facilityAlreadyPresentError"));
            return ERROR;
        }
        context = ServletActionContext.getServletContext();
        imgPath = context.getRealPath("/") + "resources/" + this.getIdStructure() + "/facilities/structure/";
        target = new File(imgPath + this.getUploadFileName());
        FileUtils.copyFile(this.upload, target);
        this.setStructureFacility(new Facility());
        this.getStructureFacility().setName(this.getName());
        this.getStructureFacility().setFileName(this.getUploadFileName());
        this.getStructureFacility().setId_structure(this.getIdStructure());
        this.getFacilityService().insertStructureFacility(this.getStructureFacility());
        message.setResult(Message.SUCCESS);
        message.setDescription(getText("facilityAddSuccessAction"));
        return SUCCESS;
    }

    @Actions({ @Action(value = "/uploadRoomImageIF", results = { @Result(name = "success", location = "/message_upload.jsp"), @Result(name = "error", location = "/message_upload.jsp") }), @Action(value = "/uploadRoomImage", results = { @Result(type = "json", name = "success", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }), @Result(type = "json", name = "error", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }) }) })
    public String uploadRoomImage() throws IOException {
        Room aRoom = null;
        ServletContext context = null;
        String imgPath = null;
        File target = null;
        if (this.getImageService().findRoomImageByName(this.getIdStructure(), this.getName()) != null) {
            message.setResult(Message.ERROR);
            message.setDescription(getText("roomImageAlreadyPresentError"));
            return ERROR;
        }
        ;
        aRoom = this.getRoomService().findRoomById(this.getRoom().getId());
        if (aRoom == null) {
            message.setResult(Message.ERROR);
            message.setDescription(getText("roomImageNoRoomError"));
            return ERROR;
        }
        context = ServletActionContext.getServletContext();
        imgPath = context.getRealPath("/") + "resources/" + this.getIdStructure() + "/images/room/";
        target = new File(imgPath + this.getUploadFileName());
        FileUtils.copyFile(this.getUpload(), target);
        this.setImage(new Image());
        this.getImage().setName(this.getName());
        this.getImage().setFileName(this.getUploadFileName());
        this.getImage().setId_room(this.getRoom().getId());
        this.getImageService().insertRoomImage(this.getImage());
        message.setResult(Message.SUCCESS);
        message.setDescription(getText("roomImageAddSuccessAction"));
        return SUCCESS;
    }

    @Actions({ @Action(value = "/uploadRoomTypeImageIF", results = { @Result(name = "success", location = "/message_upload.jsp"), @Result(name = "error", location = "/message_upload.jsp") }), @Action(value = "/uploadRoomTypeImage", results = { @Result(type = "json", name = "success", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }), @Result(type = "json", name = "error", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }) }) })
    public String uploadRoomTypeImage() throws IOException {
        RoomType aRoomType = null;
        ServletContext context = null;
        String imgPath = null;
        File target = null;
        if (this.getImageService().findRoomTypeImageByName(this.getIdStructure(), this.getName()) != null) {
            message.setResult(Message.ERROR);
            message.setDescription(getText("roomTypeImageAlreadyPresentError"));
            return ERROR;
        }
        aRoomType = this.getRoomTypeService().findRoomTypeById(this.getRoomType().getId());
        if (aRoomType == null) {
            message.setResult(Message.ERROR);
            message.setDescription(getText("roomTypeImageNoRoomTypeError"));
            return ERROR;
        }
        context = ServletActionContext.getServletContext();
        imgPath = context.getRealPath("/") + "resources/" + this.getIdStructure() + "/images/roomType/";
        target = new File(imgPath + this.getUploadFileName());
        FileUtils.copyFile(this.getUpload(), target);
        this.setImage(new Image());
        this.getImage().setName(this.getName());
        this.getImage().setFileName(this.getUploadFileName());
        this.getImage().setId_roomType(this.getRoomType().getId());
        this.getImageService().insertRoomTypeImage(this.getImage());
        message.setResult(Message.SUCCESS);
        message.setDescription(getText("facilityImageAddSuccessAction"));
        return SUCCESS;
    }

    @Actions({ @Action(value = "/uploadStructureImageIF", results = { @Result(name = "success", location = "/message_upload.jsp"), @Result(name = "error", location = "/message_upload.jsp") }), @Action(value = "/uploadStructureImage", results = { @Result(type = "json", name = "success", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }), @Result(type = "json", name = "error", params = { "excludeProperties", "session,upload,uploadFileName,uploadContentType,name,structureService,roomTypeService,roomService,imageService,facilityService" }) }) })
    public String uploadStructureImage() throws IOException {
        ServletContext context = null;
        String imgPath = null;
        File target = null;
        if (this.getImageService().findStructureImageByName(this.getIdStructure(), this.getName()) != null) {
            message.setResult(Message.ERROR);
            message.setDescription(getText("structureImageAlreadyPresentError"));
            return ERROR;
        }
        context = ServletActionContext.getServletContext();
        imgPath = context.getRealPath("/") + "resources/" + this.getIdStructure() + "/images/structure/";
        target = new File(imgPath + this.getUploadFileName());
        FileUtils.copyFile(this.getUpload(), target);
        this.setImage(new Image());
        this.getImage().setName(this.getName());
        this.getImage().setFileName(this.getUploadFileName());
        this.getImage().setId_structure(this.getIdStructure());
        this.getImageService().insertStructureImage(this.getImage());
        message.setResult(Message.SUCCESS);
        message.setDescription(getText("structureImageAddSuccessAction"));
        return SUCCESS;
    }

    public Map<String, Object> getSession() {
        return session;
    }

    @Override
    public void setSession(Map<String, Object> session) {
        this.session = session;
    }

    public File getUpload() {
        return upload;
    }

    public void setUpload(File upload) {
        this.upload = upload;
    }

    public String getUploadFileName() {
        return uploadFileName;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    public String getUploadContentType() {
        return uploadContentType;
    }

    public void setUploadContentType(String uploadContentType) {
        this.uploadContentType = uploadContentType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Facility getRoomFacility() {
        return roomFacility;
    }

    public void setRoomFacility(Facility roomFacility) {
        this.roomFacility = roomFacility;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Facility getStructureFacility() {
        return structureFacility;
    }

    public void setStructureFacility(Facility structureFacility) {
        this.structureFacility = structureFacility;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public StructureService getStructureService() {
        return structureService;
    }

    public void setStructureService(StructureService structureService) {
        this.structureService = structureService;
    }

    public RoomTypeService getRoomTypeService() {
        return roomTypeService;
    }

    public void setRoomTypeService(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    public RoomService getRoomService() {
        return roomService;
    }

    public void setRoomService(RoomService roomService) {
        this.roomService = roomService;
    }

    public ImageService getImageService() {
        return imageService;
    }

    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    public FacilityService getFacilityService() {
        return facilityService;
    }

    public void setFacilityService(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    public Integer getIdStructure() {
        return idStructure;
    }

    public void setIdStructure(Integer idStructure) {
        this.idStructure = idStructure;
    }
}
