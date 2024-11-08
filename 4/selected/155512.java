package gg.de.sbmp3.actions.edit.playlist;

import gg.de.sbmp3.actions.AbstractAction;
import gg.de.sbmp3.backend.BEFactory;
import gg.de.sbmp3.backend.PlaylistBE;
import gg.de.sbmp3.backend.ViewBE;
import gg.de.sbmp3.backend.data.FileBean;
import gg.de.sbmp3.backend.data.FilterBean;
import gg.de.sbmp3.backend.data.PlaylistBean;
import gg.de.sbmp3.backend.data.UserBean;
import gg.de.sbmp3.common.Globals;
import gg.de.sbmp3.forms.PlaylistEntryFormBean;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created: 03.07.2004  20:54:43
 */
public class ShowEditPlaylistEntry extends AbstractAction {

    /**
	 * example method
	 *
	 * @param mapping  The ActionMapping used to select this instance
	 * @param form     The optional ActionForm bean for this request
	 * @param request  The servlet request we are processing
	 * @param response The servlet response we are creating
	 */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        PlaylistEntryFormBean playlistEntryForm = (PlaylistEntryFormBean) form;
        UserBean currentUser = getCurrentUser(request);
        if (playlistEntryForm.getPlaylistId() <= 0) {
            request.setAttribute("message", "invalid playlist id.");
            return mapping.findForward("error");
        }
        PlaylistBE playlistBe = BEFactory.getPlaylistBE(currentUser);
        PlaylistBean playlist = playlistBe.getPlaylist(playlistEntryForm.getPlaylistId());
        if (playlist == null) {
            request.setAttribute("message", "no such playlist.");
            return mapping.findForward("error");
        }
        if ((playlist.getOwner().getId() != currentUser.getId()) && (playlist.getVisibility() == Globals.VISIBILITY_PRIVATE)) {
            request.setAttribute("message", "ACCESS DENIED - Playlist is owned by " + playlist.getOwner().getName() + " and marked as private.");
            return mapping.findForward("error");
        }
        if ((playlistEntryForm.getAction() != null) || (playlistEntryForm.getDeletebt() != null)) {
            if ((playlist.getOwner().getId() != currentUser.getId()) && (playlist.getVisibility() != Globals.VISIBILITY_PREAD_WRITE)) {
                request.setAttribute("message", "ACCESS DENIED - Playlist is owned by " + playlist.getOwner().getName() + " and not marked as read/write.");
                return mapping.findForward("error");
            }
            if (playlistEntryForm.getAction() != null) {
                if (playlistEntryForm.getAction().equals("up")) {
                    if (playlistEntryForm.getFileId() > 0) playlistBe.moveFile(playlist.getId(), playlistEntryForm.getFileId(), true);
                } else if (playlistEntryForm.getAction().equals("down")) {
                    if (playlistEntryForm.getFileId() > 0) playlistBe.moveFile(playlist.getId(), playlistEntryForm.getFileId(), false);
                } else if (playlistEntryForm.getAction().equals("del")) {
                    if (playlistEntryForm.getFileId() > 0) playlistBe.removeFile(playlist.getId(), playlistEntryForm.getFileId());
                }
            } else if (playlistEntryForm.getDeletebt() != null) {
                if (playlistEntryForm.getFileIds() != null) {
                    int[] fileIds = playlistEntryForm.getFileIds();
                    if ((fileIds != null) && (fileIds.length > 0)) for (int i = 0; i < fileIds.length; i++) if (fileIds[i] > 0) playlistBe.removeFile(playlist.getId(), fileIds[i]);
                }
            }
        }
        ViewBE viewBe = BEFactory.getViewBE(currentUser);
        viewBe.clearFilter(Globals.FILTER_STREAM_PLEDITVIEW);
        viewBe.addFilter(Globals.FILTER_STREAM_PLEDITVIEW, new FilterBean(FilterBean.FILTER_TYPE_PLAYLIST_ORDERBYPOS, playlistEntryForm.getPlaylistId()));
        FileBean[] fileList = viewBe.getFiles(Globals.FILTER_STREAM_PLEDITVIEW);
        request.setAttribute("fileList", fileList);
        request.setAttribute("playlistEntryFormBean", playlistEntryForm);
        return mapping.findForward("success");
    }
}
