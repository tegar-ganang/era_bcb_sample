package com.clouds.aic.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import com.clouds.aic.Constant;
import com.clouds.aic.R;
import com.clouds.aic.activity.FolderActivity;
import com.clouds.aic.activity.VoiceActivity;
import com.clouds.aic.activity.view.CreatePopWindow;
import com.clouds.aic.activity.view.FolderItemListLayout;
import com.clouds.aic.activity.view.ItemPopUpWindow;
import com.clouds.aic.activity.view.MoveActionPopWindow;
import com.clouds.aic.activity.view.RecentListPopWindow;
import com.clouds.aic.controller.action.DownloadCallback;
import com.clouds.aic.controller.action.FileDelCallBack;
import com.clouds.aic.controller.action.FileMoveCallBack;
import com.clouds.aic.controller.action.FolderActionCallBack;
import com.clouds.aic.controller.action.UploadCallback;
import com.clouds.aic.model.FolderModel;
import com.clouds.aic.tools.AICFile;
import com.clouds.aic.tools.CloudFileManagementService;
import com.clouds.aic.tools.JsonObject;
import com.clouds.aic.tools.LocalPhoneFileManagementService;

public class FolderController {

    FolderActivity activity = null;

    FolderModel model = null;

    RecentListPopWindow popup = null;

    public FolderController(FolderActivity folderActivity, FolderModel folderModel) {
        activity = folderActivity;
        model = folderModel;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.empty, null);
        popup = new RecentListPopWindow(layout, 300, 200, activity, model, this);
    }

    public void changeFolder(FolderItemListLayout folder, String tag) {
        List<AICFile> files = new ArrayList<AICFile>();
        if (tag.startsWith("@")) {
            List<JsonObject> file2 = new ArrayList<JsonObject>();
            try {
                file2.addAll(CloudFileManagementService.get_contents_by_tag(model.getSessionId(), tag));
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < file2.size(); i++) {
                AICFile file = new AICFile();
                JsonObject jfile = file2.get(i);
                file.setName(jfile.getValue("name").toString());
                file.setKey(jfile.getValue("key").toString());
                file.setContent_type(jfile.getValue("content_type").toString());
                file.setLast_modified(jfile.getValue("last_modified").toString());
                file.setIs_shared(jfile.getValue("is_shared").toString());
                try {
                    file.setFile_type(jfile.getValue("file_type").toString());
                } catch (NullPointerException e) {
                    Log.d("CLOUD_DEBUG", "No_type");
                }
                Log.d("CLOUD_DEBUG_jfile", jfile.toString());
                files.add(file);
            }
        } else if (tag.startsWith("#")) {
            String ttag = tag.substring(1);
            List<JsonObject> file2 = new ArrayList<JsonObject>();
            String filePath = "/" + ttag;
            try {
                file2.addAll(LocalPhoneFileManagementService.get_contents_by_tag_local(filePath));
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < file2.size(); i++) {
                AICFile file = new AICFile();
                JsonObject jfile = file2.get(i);
                file.setName(jfile.getValue("name").toString());
                file.setKey("wei jiang key");
                file.setContent_type(jfile.getValue("content_type").toString());
                Log.d("content_type", jfile.getValue("content_type").toString());
                file.setLast_modified("wei jiang last modified");
                file.setIs_shared("wei jiang is shared");
                Log.d("PHONE_DEBUG", jfile.toString());
                files.add(file);
            }
        }
        folder.getModel().setFiles(files);
        folder.getModel().setFolderTag(tag);
        String[] recent = model.getRecent();
        boolean full = true;
        for (int i = 0; i < recent.length; i++) {
            if (recent[i].equals(tag)) {
                full = false;
                break;
            }
            if (recent[i].equals("")) {
                recent[i] = tag;
                full = false;
                break;
            }
        }
        if (full) {
            for (int i = 0; i < recent.length - 1; i++) {
                recent[i] = recent[i + 1];
            }
            recent[recent.length - 1] = tag;
        }
        activity.refresh();
        folder.refresh();
    }

    public void split() {
        model.setSplit(!model.isSplitted());
        activity.refresh();
    }

    public void create() {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.popup, null, false);
        CreatePopWindow pw = new CreatePopWindow(view, 200, 200, true, activity);
        pw.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    public void recent(FolderItemListLayout folder) {
        popup.refresh();
        popup.setFolder(folder);
        popup.showAtLocation(popup.getContentView(), Gravity.CENTER, 0, 0);
    }

    public void view() {
        List<AICFile> files = model.getSelectedItem();
        String total = "";
        for (int i = 0; i < files.size(); i++) {
            AICFile file = files.get(i);
            String line = "Name: " + file.getName() + " Key: " + file.getKey() + " Content_type: " + file.getContent_type() + " Last_modified: " + file.getLast_modified() + " Is_shared: " + file.getIs_shared();
            total = total + line + "\n";
        }
        Toast.makeText(activity, total, Toast.LENGTH_LONG).show();
    }

    public void trans() {
        List<AICFile> list1 = model.getSelectedItem1();
        List<AICFile> list2 = model.getSelectedItem2();
        for (AICFile file : list1) {
            String frompath = model.getPath(1);
            String topath = model.getPath(2);
            transfer(frompath, topath, file);
            model.removeFrom1(file);
            model.addTo2(file);
        }
        for (AICFile file : list2) {
            String frompath = model.getPath(2);
            String topath = model.getPath(1);
            transfer(frompath, topath, file);
            model.removeFrom2(file);
            model.addTo1(file);
        }
        activity.hardRefresh();
    }

    private void transfer(String frompath, String topath, AICFile file) {
        if (frompath.startsWith("#") || topath.startsWith("#")) {
            Toast.makeText(activity, "trans to local not supported yet", Toast.LENGTH_LONG).show();
        }
        if (file.getContent_type().equals(Constant.CLOUD_FOLDER_TYPE)) {
            Toast.makeText(activity, "folder move not supported yet", Toast.LENGTH_LONG).show();
        }
        CloudFileManagementService.move_files(model.getSessionId(), file.getKey(), topath);
        activity.refresh();
    }

    public void multiple() {
        model.setMulti(!model.isMulti());
        activity.refresh();
    }

    public void del() {
        try {
            List<AICFile> files = model.getSelectedItem1();
            for (int i = 0; i < files.size(); i++) {
                AICFile file = files.get(i);
                deleteFile(file);
                model.removeFrom1(file);
            }
            files = model.getSelectedItem2();
            for (int i = 0; i < files.size(); i++) {
                AICFile file = files.get(i);
                deleteFile(file);
                model.removeFrom2(file);
            }
            activity.hardRefresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteFile(AICFile file) {
        if (file.getContent_type().equals(Constant.CLOUD_FOLDER_TYPE)) {
            Toast.makeText(activity, "Cannot delet folder now!", Toast.LENGTH_LONG).show();
        } else {
            String del_ses_id = model.getSessionId();
            String del_key = file.getKey();
            CloudFileManagementService.remove_file(del_ses_id, del_key);
        }
    }

    public void selectFolder(AICFile file, FolderItemListLayout view) {
        if (model.isMulti()) {
            model.selectOrUnselectItem(file, view.getModel().getId());
        } else {
            String tag = view.getModel().getFolderTag();
            tag = tag + "/" + file.getName();
            changeFolder(view, tag);
        }
        activity.refresh();
    }

    public void fileMove(AICFile ori, String oriTag, String folderTag, boolean sameFolder, AICFile file) {
        if (ori == null || oriTag == null || folderTag == null) return;
        List<FolderActionCallBack> actions = new ArrayList<FolderActionCallBack>();
        if (!sameFolder && oriTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_CLOUD]) && folderTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_CLOUD])) {
            actions.add(new FileMoveCallBack(this, ori, oriTag, folderTag));
        }
        if (file != null && file.getContent_type().equals(Constant.CLOUD_FOLDER_TYPE) && oriTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_CLOUD]) && folderTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_CLOUD])) {
            actions.add(new FileMoveCallBack(this, ori, oriTag, file.getKey()));
        }
        if (oriTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_PHONE]) && folderTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_CLOUD])) {
            actions.add(new UploadCallback(this, ori, oriTag, folderTag));
        }
        if (file != null && file.getContent_type().equals(Constant.CLOUD_FOLDER_TYPE) && oriTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_PHONE]) && folderTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_CLOUD])) {
            actions.add(new UploadCallback(this, ori, oriTag, file.getKey()));
        }
        if (oriTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_CLOUD]) && folderTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_PHONE])) {
            actions.add(new DownloadCallback(this, ori, oriTag, folderTag));
        }
        if (file != null && file.getContent_type().equals(Constant.CLOUD_FOLDER_TYPE) && oriTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_CLOUD]) && folderTag.startsWith(Constant.STARTPOINT[Constant.DESKTOP_BUTTON_PHONE])) {
        }
        if (actions.isEmpty()) return;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.empty, null);
        MoveActionPopWindow actionWindow = new MoveActionPopWindow(layout, 300, 300, activity, actions);
        actionWindow.showAtLocation(popup.getContentView(), Gravity.CENTER, 0, 0);
    }

    public void move(AICFile file, String frompath, String topath) {
        transfer(frompath, topath, file);
        if (model.getSelectedItem1().contains(file)) {
            model.removeFrom1(file);
            model.addTo2(file);
        } else if (model.getSelectedItem2().contains(file)) {
            model.removeFrom2(file);
            model.addTo1(file);
        }
        activity.hardRefresh();
    }

    public void upload(AICFile file, String fromTag, String toTag) {
        String from_path = fromTag.substring(1) + "/" + file.getName();
        displayResult(CloudFileManagementService.upload_file(model.getSessionId(), from_path, toTag));
        if (!model.getSelectedItem1().contains(file)) {
            model.addTo1(file);
        } else if (!model.getSelectedItem2().contains(file)) {
            model.addTo1(file);
        }
        activity.hardRefresh();
    }

    public void download(AICFile file, String fromTag, String toTag) {
        String to_path = toTag.substring(1);
        if (!to_path.endsWith("/")) to_path = to_path + "/";
        displayResult(storeStreamIntoLocalPhoneAnyPosition(CloudFileManagementService.download_file(model.getSessionId(), file.getKey()), to_path, file.getName()));
        activity.hardRefresh();
    }

    /**
	 * store a stream into the local phone.
	 * @param stream which you want to store into the phone
	 * @param the local phone file path. eg. localFilePath = "/sdcard"
	 * @param the file name you set to this stream. eg. wei.mp3, wei.doc 
	 * @return a status string indicate whether it is successful or not
	 */
    public String storeStreamIntoLocalPhone(InputStream stream, String localFilePath, String fileName) {
        String resultJsonString = "some problem existed inside the storeStreamIntoLocalPhone() function if you see this string";
        try {
            String string = "hello world";
            Log.d("size of the stream", "" + stream.available());
            FileOutputStream fos = activity.openFileOutput(fileName, Context.MODE_WORLD_WRITEABLE);
            fos.write(InputStreamToByte(stream));
            fos.close();
            resultJsonString = "OK";
            return resultJsonString;
        } catch (Exception e) {
            return resultJsonString;
        }
    }

    public void VoiceRecorder() {
        Intent i = new Intent(activity, VoiceActivity.class);
        i.putExtra(Constant.PHP_SESSION_ID, model.getSessionId());
        activity.startActivity(i);
    }

    /**
	 * store a stream into the local phone in any position you like
	 * @param stream the stream which you want to store into the phone
	 * @param localFilePath the local phone file path you want to store into the phone. eg. localFilePath = "/sdcard/"
	 * @param fileName the name you set to this stream. eg. wei.mp3, wei.doc 
	 * @return a status string indicate whether it is successful or not
	 */
    public String storeStreamIntoLocalPhoneAnyPosition(InputStream stream, String localFilePath, String fileName) {
        String resultJsonString = "some problem existed inside the storeStreamIntoLocalPhone() function if you see this string";
        FileOutputStream out = null;
        String string = "hello world";
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                String path = localFilePath;
                File dirPath = new File(path);
                File file = new File(path + fileName);
                if (!dirPath.exists()) {
                    dirPath.mkdir();
                }
                if (!file.exists()) {
                    file.createNewFile();
                }
                out = new FileOutputStream(file);
                out.write(InputStreamToByte(stream));
                resultJsonString = "You input stream has been saved in the phone as " + fileName;
            } else {
                Toast.makeText(activity, "Please insert the SD card", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                return resultJsonString;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resultJsonString;
    }

    private byte[] InputStreamToByte(InputStream is) throws IOException {
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        int ch;
        while ((ch = is.read()) != -1) {
            bytestream.write(ch);
        }
        byte imgdata[] = bytestream.toByteArray();
        bytestream.close();
        return imgdata;
    }

    public void displayResult(String result) {
        Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
    }

    public void showItem(AICFile file) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.empty, null);
        Bitmap img = CloudFileManagementService.download_picture_thumb(model.getSessionId(), file.getKey());
        if (img == null) return;
        ItemPopUpWindow actionWindow = new ItemPopUpWindow(layout, 300, 300, img);
        actionWindow.showAtLocation(popup.getContentView(), Gravity.CENTER, 0, 0);
    }

    public void deleteNotify(AICFile file) {
        List<FolderActionCallBack> actions = new ArrayList<FolderActionCallBack>();
        actions.add(new FileDelCallBack(this, file));
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.empty, null);
        MoveActionPopWindow actionWindow = new MoveActionPopWindow(layout, 300, 300, activity, actions);
        actionWindow.showAtLocation(popup.getContentView(), Gravity.CENTER, 0, 0);
    }

    public void hardRefresh() {
        activity.hardRefresh();
    }
}
