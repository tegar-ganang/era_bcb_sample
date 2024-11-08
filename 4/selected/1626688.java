package code.action;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.ServletRequestAware;

import code.model.Comment;
import code.model.Note;
import code.model.Program;
import code.model.User;
import code.service.ManageCommentService;
import code.service.ManageNoteService;
import code.service.ManageProgramService;

import com.opensymphony.xwork2.ActionSupport;

public class ManageProgramAction extends ActionSupport implements ServletRequestAware{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2735857096102938977L;
	private File imagefile; // the type of the uploaded file
	private String filename;  // the name of the uploaded file
	private String dir;  // the final path of the uploaded file
	private String targetFileName; // the target file name of the uploaded file
	private String resultmsg; //return msg
	
	private String programName;
	private Program program = new Program();
	private List<Program> programList = new ArrayList<Program>();
	private Note note = new Note();
	private List<Note> noteList = new ArrayList<Note>();
	private Comment comment = new Comment();
	public Comment getComment() {
		return comment;
	}

	public void setComment(Comment comment) {
		this.comment = comment;
	}

	private List<Comment> commentList = new ArrayList<Comment>();
	
	private HttpServletRequest request;
	private ManageProgramService manageProgramService;
	private ManageNoteService manageNoteService;
	private ManageCommentService manageCommentService;
	
	private final static String root="/upload";  //the root path of uploaded file in the project
	
	private boolean inputvalidate(){
		if(program.getName()==null||("").equals(program.getName().trim())){
			resultmsg = "Please enter the name of the program";
			return false;
		}
		if(program.getInfo()==null||("").equals(program.getInfo().trim())){
			resultmsg = "Please enter the info of the program";
			return false;
		}
		if(program.getRequirement()==null||("").equals(program.getRequirement().trim())){
			resultmsg ="Please enter requirment of the program";
			return false;
		}
		if(filename==null||("").equals(filename.trim())){
			resultmsg="Please select an image for the program";
			return false;
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	public String insertion(){
		try {
			if(	inputvalidate()==false)
				return INPUT;
			else{
				System.out.println("uploadFile execute");
				program.setImage(request.getContextPath()+"/upload/"+filename);
				/* do the upload*/
				String realPath = ServletActionContext.getRequest().getRealPath(root);
				String targetDirectory = realPath;
				targetFileName = filename;
				setDir(targetDirectory + "\\" + targetFileName);
				if(!dir.substring(dir.length()-3,dir.length()).equals("jpg")){
					resultmsg = "file type error, please upload an .jpg image";
					System.out.println(resultmsg);
					return INPUT;
				}
				File target = new File(targetDirectory,targetFileName);
				FileUtils.copyFile(imagefile, target);
			}			
			program.setProgramid(manageProgramService.insertProgram(program));
			programName = "";
			return SUCCESS;
		} 
		catch (IOException e) {
			e.printStackTrace();
			return ERROR;
		}
	}
	public String adminSearchProgram(){
		System.out.println("search program:"+programName);
		programList = manageProgramService.searchProgram(programName);
		if(programList.size() == 0){
			resultmsg = "No Such Program~~";
			return INPUT;
		}
		resultmsg="Total find: "+programList.size()+" pragrams";

		return SUCCESS;
	}
	public String adminShowProgram(){
		program = manageProgramService.getProgramByProgramid(program.getProgramid());
		if(program == null){
			resultmsg = "No Such Program~~";
			return INPUT;
		}
		request.getSession().setAttribute("program", program);

		return SUCCESS;
	}
	public String deleteProgram(){
		System.out.println("delete"+program.getProgramid());
		long result = manageProgramService.deleteProgram(program.getProgramid());
		if(result != program.getProgramid()){
			resultmsg = "Deletion failure, entry not found!";
			return INPUT;
		}
		programName = "";
		return SUCCESS;
	}
	public String modifyProgram(){
		System.out.println("modify"+program.getProgramid());
		program = manageProgramService.getProgramByProgramid(program.getProgramid());
		if(program == null){
			resultmsg = "Error! Program not found!";
			return INPUT;
		}
		return SUCCESS;
	}
	@SuppressWarnings("deprecation")
	public String modifyProgramSubmit(){
		if(filename!=null){
			program.setImage(request.getContextPath()+"/upload/"+filename);
			String realPath = ServletActionContext.getRequest().getRealPath(root);
			String targetDirectory = realPath;
			targetFileName = filename;
			setDir(targetDirectory + "\\" + targetFileName);
			if(!dir.substring(dir.length()-3,dir.length()).equals("jpg")){
				resultmsg = "file type error, please upload an image";
				System.out.println(resultmsg);
				return INPUT;
			}
			File target = new File(targetDirectory,targetFileName);
			try {
				FileUtils.copyFile(imagefile, target);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println(program.getProgramid());
		} 
		System.out.println("origin filename："+program.getImage());
		manageProgramService.modifyProgram(program);
		programName = "";
		return SUCCESS;
	}
	
	public String searchProgram(){
		System.out.println("search program:"+programName);
		programList = manageProgramService.searchProgram(programName);
		//programList = manageProgramService.searchProgram("");
		if(programList.size() == 0){
			resultmsg = "No Such Program~~";
			return INPUT;
		}
		resultmsg="Total find: "+programList.size()+" pragrams";

		return SUCCESS;
	}
	public String showProgram(){
		System.out.println("in showProgram action pid:"+program.getProgramid());
		program = manageProgramService.getProgramByProgramid(program.getProgramid());
		noteList = manageNoteService.getNotelistByProgramid(program.getProgramid());
		commentList = manageCommentService.getCommentlistByProgramid(program.getProgramid());
		if(program == null){
			resultmsg = "No Such Program~~";
			return INPUT;
		}
		request.getSession().setAttribute("program", program);
		request.getSession().setAttribute("noteList", noteList);
		request.getSession().setAttribute("commentList", commentList);

		return SUCCESS;
	}
	public String noteProgram(){
		System.out.println("pid:"+note.getProgramid());
		User host = (User) request.getSession().getAttribute("user");
		program = (Program) request.getSession().getAttribute("program");
		note.setDetail("想去");
		note.setUser(host);
		note.setProgram(program);
		long noteid = manageNoteService.insertNote(note);
		if(noteid < 0){
			resultmsg = "添加关注失败";
			return INPUT;
		}

		noteList = manageNoteService.getNotelistByProgramid(program.getProgramid());
		request.getSession().setAttribute("noteList", noteList);
		resultmsg = "添加关注成功";
		return SUCCESS;
	}
	public String cancelNoteProgram(){
		//long nodeid = (Long) request.getAttribute("nodeid");
		//note = manageNoteService.getNoteByNoteid(note.getNoteid());
		boolean result = manageNoteService.deleteNote(note.getNoteid());
		//System.out.println("in cancel action nid:"+note.getNoteid());
		//program.setProgramid(note.getProgramid());
		if(!result){
			resultmsg = "取消关注失败";
			return INPUT;
		}

		resultmsg = "取消关注成功";
		return SUCCESS;
	}
	public String addComment(){
		program = (Program)request.getSession().getAttribute("program");
		User host = (User)request.getSession().getAttribute("user");
		comment.setUser(host);
		comment.setProgram(program);
		comment.setAuthor(host.getName());
		comment.setGrade(0);
		comment.setCreatedate(new Timestamp(System.currentTimeMillis()));
		long commentid = manageCommentService.insertComment(comment);
		if(commentid < 0){
			resultmsg = "添加留言失败";
			return INPUT;
		}
		resultmsg = "添加留言成功";
		return SUCCESS;
	}
	public void setImagefile(File imagefile) {
		this.imagefile = imagefile;
	}
	public void setImagefileFileName(String filename){
		this.filename = filename;
	}
	public String getResultmsg() {
		return resultmsg;
	}
	public void setResultmsg(String resultmsg) {
		this.resultmsg = resultmsg;
	}
	public HttpServletRequest getRequest() {
		return request;
	}
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
	public Program getProgram() {
		return program;
	}
	public void setProgram(Program program) {
		this.program = program;
	}
	public String getDir() {
		return dir;
	}
	public void setDir(String dir) {
		this.dir = dir;
	}
	public String getTargetFileName() {
		return targetFileName;
	}
	public void setTargetFileName(String targetFileName) {
		this.targetFileName = targetFileName;
	}
	public ManageProgramService getManageProgramService() {
		return manageProgramService;
	}
	public void setManageProgramService(ManageProgramService manageProgramService) {
		this.manageProgramService = manageProgramService;
	}
	public void setProgramList(List<Program> programList) {
		this.programList = programList;
	}
	public List<Program> getProgramList() {
		return programList;
	}
	public void setProgramName(String programName) {
		this.programName = programName;
	}
	public String getProgramName() {
		return programName;
	}

	public void setManageNoteService(ManageNoteService manageNoteService) {
		this.manageNoteService = manageNoteService;
	}

	public ManageNoteService getManageNoteService() {
		return manageNoteService;
	}

	public void setNote(Note note) {
		this.note = note;
	}

	public Note getNote() {
		return note;
	}

	public void setNoteList(List<Note> noteList) {
		this.noteList = noteList;
	}

	public List<Note> getNoteList() {
		return noteList;
	}

	public List<Comment> getCommentList() {
		return commentList;
	}

	public void setCommentList(List<Comment> commentList) {
		this.commentList = commentList;
	}

	public ManageCommentService getManageCommentService() {
		return manageCommentService;
	}

	public void setManageCommentService(ManageCommentService manageCommentService) {
		this.manageCommentService = manageCommentService;
	}
}
