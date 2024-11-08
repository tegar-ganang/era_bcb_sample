package jdos.win.builtin.user32;

import jdos.win.Win;
import jdos.win.builtin.WinAPI;
import jdos.win.system.StaticData;
import jdos.win.system.WinPoint;
import jdos.win.system.WinRect;
import jdos.win.utils.Error;

public class WinPos extends WinAPI {

    public static int ClientToScreen(int hWnd, int lpPoint) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null) return FALSE;
        WinPoint offset = window.getScreenOffset();
        writed(lpPoint, readd(lpPoint) + offset.x);
        writed(lpPoint + 4, readd(lpPoint + 4) + offset.y);
        return TRUE;
    }

    public static int IsIconic(int hWnd) {
        return BOOL((WinWindow.GetWindowLongA(hWnd, GWL_STYLE) & WS_MINIMIZE) != 0);
    }

    public static int GetClientRect(int hWnd, int lpRect) {
        WinRect rect = new WinRect();
        if (WinWindow.WIN_GetRectangles(hWnd, COORDS_CLIENT, null, rect)) {
            rect.write(lpRect);
            return TRUE;
        }
        return FALSE;
    }

    public static int WIN_GetClientRect(int hWnd, WinRect rect) {
        if (WinWindow.WIN_GetRectangles(hWnd, COORDS_CLIENT, null, rect)) {
            return TRUE;
        }
        return FALSE;
    }

    public static int GetWindowPlacement(int hWnd, int lpwndpl) {
        WinWindow pWnd = WinWindow.get(hWnd);
        if (pWnd == null) return FALSE;
        WINDOWPLACEMENT wp = new WINDOWPLACEMENT();
        if (GetWindowPlacement(pWnd, wp)) {
            wp.write(lpwndpl);
            return TRUE;
        }
        return FALSE;
    }

    public static boolean GetWindowPlacement(WinWindow pWnd, WINDOWPLACEMENT wndpl) {
        if (pWnd.handle == StaticData.desktopWindow) {
            wndpl.showCmd = SW_SHOWNORMAL;
            wndpl.flags = 0;
            wndpl.ptMinPosition.x = -1;
            wndpl.ptMinPosition.y = -1;
            wndpl.ptMaxPosition.x = -1;
            wndpl.ptMaxPosition.y = -1;
            wndpl.rcNormalPosition.copy(pWnd.rectWindow);
            return true;
        }
        if ((pWnd.dwStyle & WS_MINIMIZE) != 0) {
            pWnd.min_pos.x = pWnd.rectWindow.left;
            pWnd.min_pos.y = pWnd.rectWindow.top;
        } else if ((pWnd.dwStyle & WS_MAXIMIZE) != 0) {
            pWnd.max_pos.x = pWnd.rectWindow.left;
            pWnd.max_pos.y = pWnd.rectWindow.top;
        } else {
            pWnd.normal_rect.copy(pWnd.rectWindow);
        }
        if ((pWnd.dwStyle & WS_MINIMIZE) != 0) wndpl.showCmd = SW_SHOWMINIMIZED; else wndpl.showCmd = (pWnd.dwStyle & WS_MAXIMIZE) != 0 ? SW_SHOWMAXIMIZED : SW_SHOWNORMAL;
        if ((pWnd.flags & WIN_RESTORE_MAX) != 0) wndpl.flags = WPF_RESTORETOMAXIMIZED; else wndpl.flags = 0;
        wndpl.ptMinPosition.copy(pWnd.min_pos);
        wndpl.ptMaxPosition.copy(pWnd.max_pos);
        wndpl.rcNormalPosition.copy(pWnd.normal_rect);
        return true;
    }

    public static int GetWindowRect(int hwnd, int pRect) {
        WinRect rect = new WinRect();
        if (WinWindow.WIN_GetRectangles(hwnd, COORDS_SCREEN, rect, null)) {
            rect.write(pRect);
            return TRUE;
        }
        return FALSE;
    }

    public static int MapWindowPoints(int hWndFrom, int hWndTo, int lpPoints, int cPoints) {
        WinWindow from = WinWindow.get(hWndFrom);
        WinWindow to = WinWindow.get(hWndTo);
        if (from == null || to == null) {
            SetLastError(Error.ERROR_INVALID_HANDLE);
            return 0;
        }
        WinPoint fromOffset = from.getScreenOffset();
        WinPoint toOffset = to.getScreenOffset();
        int cx = fromOffset.x - toOffset.x;
        int cy = fromOffset.y - toOffset.y;
        for (int i = 0; i < cPoints; i++) {
            int px = readd(lpPoints + i * 8);
            int py = readd(lpPoints + i * 8 + 4);
            px += cx;
            py += cy;
            writed(lpPoints + i * 8, px);
            writed(lpPoints + i * 8 + 4, py);
        }
        return MAKELONG(LOWORD(cx), LOWORD(cy));
    }

    public static int MoveWindow(int hWnd, int X, int Y, int nWidth, int nHeight, int bRepaint) {
        int flags = SWP_NOZORDER | SWP_NOACTIVATE;
        if (bRepaint == 0) flags |= SWP_NOREDRAW;
        return SetWindowPos(hWnd, 0, X, Y, nWidth, nHeight, flags);
    }

    public static int ScreenToClient(int hWnd, int lpPoint) {
        WinWindow window = WinWindow.get(hWnd);
        if (window == null) return FALSE;
        WinPoint offset = window.getScreenOffset();
        writed(lpPoint, readd(lpPoint) - offset.x);
        writed(lpPoint + 4, readd(lpPoint + 4) - offset.y);
        return TRUE;
    }

    public static int SetWindowPos(int hWnd, int hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags) {
        WinRect newWindowRect = new WinRect();
        WinRect newClientRect = new WinRect();
        WinWindow window = WinWindow.get(hWnd);
        int orig_flags;
        if (window == null) return FALSE;
        orig_flags = uFlags;
        if ((uFlags & SWP_NOZORDER) == 0) {
            if (hWndInsertAfter == 0xffff) hWndInsertAfter = HWND_TOPMOST; else if (hWndInsertAfter == 0xfffe) hWndInsertAfter = HWND_NOTOPMOST;
            if (hWndInsertAfter != HWND_TOP && hWndInsertAfter != HWND_BOTTOM && hWndInsertAfter != HWND_TOPMOST && hWndInsertAfter != HWND_NOTOPMOST) {
                int parent = WinWindow.GetAncestor(hWnd, GA_PARENT);
                int insertafter_parent = WinWindow.GetAncestor(hWndInsertAfter, GA_PARENT);
                if (insertafter_parent == 0) return FALSE;
                if (insertafter_parent != parent) return FALSE;
            }
        }
        if ((uFlags & SWP_NOMOVE) == 0) {
            if (X < -32768) X = -32768; else if (X > 32767) X = 32767;
            if (Y < -32768) Y = -32768; else if (Y > 32767) Y = 32767;
        }
        if ((uFlags & SWP_NOSIZE) == 0) {
            if (cx < 0) cx = 0; else if (cx > 32767) cx = 32767;
            if (cy < 0) cy = 0; else if (cy > 32767) cy = 32767;
        }
        WINDOWPOS pos = new WINDOWPOS(hWnd, hWndInsertAfter, X, Y, cx, cy, uFlags);
        if (!SWP_DoWinPosChanging(pos, newWindowRect, newClientRect)) return FALSE;
        boolean zOrderChanged = false;
        if ((uFlags & SWP_NOZORDER) == 0) {
            int hParent = 0;
            if (window.parent != 0) hParent = window.parent; else hParent = StaticData.desktopWindow;
            if (hParent != 0) {
                WinWindow parent = WinWindow.get(hParent);
                parent.children.remove(window);
                switch(hWndInsertAfter) {
                    case HWND_TOPMOST:
                        log("SetWindowPos HWND_TOPMOST not supported yet");
                    case HWND_TOP:
                        parent.children.addFirst(window);
                        break;
                    case HWND_BOTTOM:
                        parent.children.addLast(window);
                        break;
                    case HWND_NOTOPMOST:
                        parent.children.addFirst(window);
                        log("SetWindowPos HWND_NOTOPMOST not supported yet");
                        break;
                    default:
                        WinWindow sibling = WinWindow.get(hWndInsertAfter);
                        if (sibling == null) Win.panic("SetWindowPos hWndInsertAfter=" + hWndInsertAfter + " was not found");
                        int index = parent.children.indexOf(sibling);
                        if (index < 0) Win.panic("SetWindowPos hWndInsertAfter=" + hWndInsertAfter + " is not a sibling window");
                        parent.children.add(index, window);
                }
            }
        }
        if ((uFlags & (SWP_FRAMECHANGED | SWP_NOSIZE)) != SWP_NOSIZE) {
            WinRect window_rect = new WinRect();
            WinRect client_rect = new WinRect();
            WinWindow.WIN_GetRectangles(pos.hwnd, COORDS_PARENT, window_rect, client_rect);
            int posAddress = pos.allocTemp();
            NCCALCSIZE_PARAMS params = new NCCALCSIZE_PARAMS(newWindowRect, window_rect, client_rect, posAddress);
            int paramsAddress = params.allocTemp();
            Message.SendMessageA(window.handle, WM_NCCALCSIZE, TRUE, paramsAddress);
            newClientRect = new WinRect(paramsAddress);
        }
        if (window.rectClient.width() != newClientRect.width() || window.rectClient.height() != newClientRect.height()) {
            uFlags &= ~SWP_NOCLIENTSIZE;
        } else {
            uFlags |= SWP_NOCLIENTSIZE;
        }
        if (window.rectClient.left != newClientRect.left || window.rectClient.top != newClientRect.top) {
            uFlags &= ~SWP_NOCLIENTMOVE;
        } else {
            uFlags |= SWP_NOCLIENTMOVE;
        }
        boolean geometryChanged = false;
        if (window.rectWindow.width() != newWindowRect.width() || window.rectWindow.height() != newWindowRect.height() || window.rectWindow.left != newWindowRect.left || window.rectWindow.top != newWindowRect.top) {
            geometryChanged = true;
        }
        window.rectWindow.copy(newWindowRect);
        window.rectClient.copy(newClientRect);
        if ((uFlags & SWP_SHOWWINDOW) != 0) window.dwStyle |= WS_VISIBLE; else if ((uFlags & SWP_HIDEWINDOW) != 0) window.dwStyle &= ~WS_VISIBLE;
        if ((orig_flags & SWP_DEFERERASE) == 0 && ((orig_flags & SWP_HIDEWINDOW) != 0 || ((orig_flags & SWP_SHOWWINDOW) == 0 && geometryChanged))) {
            int parent = WinWindow.GetAncestor(hWnd, GA_PARENT);
            if (parent != 0) WinWindow.get(parent).invalidate(window.rectWindow); else DesktopWindow.invalidate();
        }
        if ((uFlags & SWP_HIDEWINDOW) != 0) Caret.HideCaret(hWnd); else if ((uFlags & SWP_SHOWWINDOW) != 0) {
            Caret.ShowCaret(hWnd);
            window.invalidate(null);
        }
        if ((uFlags & (SWP_NOACTIVATE | SWP_HIDEWINDOW)) == 0) {
            if ((window.dwStyle & (WS_CHILD | WS_POPUP)) == WS_CHILD) Message.SendMessageA(window.handle, WM_CHILDACTIVATE, 0, 0); else Focus.SetForegroundWindow(hWnd);
        }
        if (geometryChanged || zOrderChanged) {
            pos.x = newWindowRect.left;
            pos.y = newWindowRect.top;
            pos.cx = newWindowRect.right - newWindowRect.left;
            pos.cy = newWindowRect.bottom - newWindowRect.top;
            int address = pos.allocTemp();
            Message.SendMessageA(window.handle, WM_WINDOWPOSCHANGED, 0, address);
        }
        return TRUE;
    }

    public static int ShowWindow(int hWnd, int nCmdShow) {
        WinWindow wndPtr = WinWindow.get(hWnd);
        if (wndPtr == null) return FALSE;
        int style = wndPtr.dwStyle;
        boolean wasVisible = (style & WS_VISIBLE) != 0;
        boolean showFlag = true;
        WinRect newPos = new WinRect();
        int swp = 0;
        switch(nCmdShow) {
            case SW_HIDE:
                if (!wasVisible) return FALSE;
                showFlag = false;
                swp |= SWP_HIDEWINDOW | SWP_NOSIZE | SWP_NOMOVE;
                if ((style & WS_CHILD) != 0) swp |= SWP_NOACTIVATE | SWP_NOZORDER;
                break;
            case SW_SHOWMINNOACTIVE:
            case SW_MINIMIZE:
            case SW_FORCEMINIMIZE:
                swp |= SWP_NOACTIVATE | SWP_NOZORDER;
            case SW_SHOWMINIMIZED:
                swp |= SWP_SHOWWINDOW | SWP_FRAMECHANGED;
                swp |= WINPOS_MinMaximize(wndPtr, nCmdShow, newPos);
                if ((style & WS_MINIMIZE) != 0 && wasVisible) return TRUE;
                break;
            case SW_SHOWMAXIMIZED:
                if (!wasVisible) swp |= SWP_SHOWWINDOW;
                swp |= SWP_FRAMECHANGED;
                swp |= WINPOS_MinMaximize(wndPtr, SW_MAXIMIZE, newPos);
                if ((style & WS_MAXIMIZE) != 0 && wasVisible) return TRUE;
                break;
            case SW_SHOWNA:
                swp |= SWP_NOACTIVATE | SWP_SHOWWINDOW | SWP_NOSIZE | SWP_NOMOVE;
                if ((style & WS_CHILD) != 0) swp |= SWP_NOZORDER;
                break;
            case SW_SHOW:
                if (wasVisible) return TRUE;
                swp |= SWP_SHOWWINDOW | SWP_NOSIZE | SWP_NOMOVE;
                if ((style & WS_CHILD) != 0) swp |= SWP_NOACTIVATE | SWP_NOZORDER;
                break;
            case SW_SHOWNOACTIVATE:
                swp |= SWP_NOACTIVATE | SWP_NOZORDER;
            case SW_RESTORE:
            case SW_SHOWNORMAL:
            case SW_SHOWDEFAULT:
                if (!wasVisible) swp |= SWP_SHOWWINDOW;
                if ((style & (WS_MINIMIZE | WS_MAXIMIZE)) != 0) {
                    swp |= SWP_FRAMECHANGED;
                    swp |= WINPOS_MinMaximize(wndPtr, nCmdShow, newPos);
                } else {
                    if (wasVisible) return TRUE;
                    swp |= SWP_NOSIZE | SWP_NOMOVE;
                }
                if ((style & WS_CHILD) != 0 && (swp & SWP_STATECHANGED) == 0) swp |= SWP_NOACTIVATE | SWP_NOZORDER;
                break;
            default:
                return BOOL(wasVisible);
        }
        if ((showFlag != wasVisible || nCmdShow == SW_SHOWNA) && nCmdShow != SW_SHOWMAXIMIZED && (swp & SWP_STATECHANGED) == 0) {
            Message.SendMessageA(wndPtr.handle, WM_SHOWWINDOW, BOOL(showFlag), 0);
            if (WinWindow.IsWindow(hWnd) == 0) return BOOL(wasVisible);
        }
        int parent = WinWindow.GetAncestor(hWnd, GA_PARENT);
        if (parent != 0 && WinWindow.IsWindowVisible(parent) == 0 && (swp & SWP_STATECHANGED) == 0) {
            if (showFlag) wndPtr.dwStyle |= WS_VISIBLE; else wndPtr.dwStyle &= ~WS_VISIBLE;
        } else {
            SetWindowPos(hWnd, HWND_TOP, newPos.left, newPos.top, newPos.right - newPos.left, newPos.bottom - newPos.top, swp);
        }
        if (nCmdShow == SW_HIDE) {
            if (hWnd == Focus.GetActiveWindow()) WINPOS_ActivateOtherWindow(hWnd);
            int hFocus = Focus.GetFocus();
            if (hWnd == hFocus) {
                Focus.SetFocus(parent);
            }
            return BOOL(wasVisible);
        }
        if ((wndPtr.flags & WIN_NEED_SIZE) != 0) {
            int wParam = SIZE_RESTORED;
            WinRect client = new WinRect();
            int lparam;
            WinWindow.WIN_GetRectangles(hWnd, COORDS_PARENT, null, client);
            lparam = MAKELONG(client.right - client.left, client.bottom - client.top);
            wndPtr.flags &= ~WIN_NEED_SIZE;
            if ((wndPtr.dwStyle & WS_MAXIMIZE) != 0) wParam = SIZE_MAXIMIZED; else if ((wndPtr.dwStyle & WS_MINIMIZE) != 0) {
                wParam = SIZE_MINIMIZED;
                lparam = 0;
            }
            Message.SendMessageA(wndPtr.handle, WM_SIZE, wParam, lparam);
            Message.SendMessageA(wndPtr.handle, WM_MOVE, 0, MAKELONG(client.left, client.top));
        }
        if ((style & WS_MINIMIZE) != 0) Focus.SetFocus(hWnd);
        return BOOL(wasVisible);
    }

    public static int WindowFromPoint(int Point) {
        WinPoint p = new WinPoint(Point);
        return WinWindow.get(StaticData.desktopWindow).findWindowFromPoint(p.x, p.y).handle;
    }

    private static boolean SWP_DoWinPosChanging(WINDOWPOS pos, WinRect pNewWindowRect, WinRect pNewClientRect) {
        WinRect window_rect = new WinRect();
        WinRect client_rect = new WinRect();
        if ((pos.flags & SWP_NOSENDCHANGING) == 0) {
            int address = pos.allocTemp();
            Message.SendMessageA(pos.hwnd, WM_WINDOWPOSCHANGING, 0, address);
        }
        WinWindow wndPtr = WinWindow.get(pos.hwnd);
        if (wndPtr == null) return false;
        WinWindow.WIN_GetRectangles(pos.hwnd, COORDS_PARENT, window_rect, client_rect);
        pNewWindowRect.copy(window_rect);
        pNewClientRect.copy((wndPtr.dwStyle & WS_MINIMIZE) != 0 ? window_rect : client_rect);
        if ((pos.flags & SWP_NOSIZE) == 0) {
            if ((wndPtr.dwStyle & WS_MINIMIZE) != 0) {
                pNewWindowRect.right = pNewWindowRect.left + SysParams.GetSystemMetrics(SM_CXICON);
                pNewWindowRect.bottom = pNewWindowRect.top + SysParams.GetSystemMetrics(SM_CYICON);
            } else {
                pNewWindowRect.right = pNewWindowRect.left + pos.cx;
                pNewWindowRect.bottom = pNewWindowRect.top + pos.cy;
            }
        }
        if ((pos.flags & SWP_NOMOVE) == 0) {
            pNewWindowRect.left = pos.x;
            pNewWindowRect.top = pos.y;
            pNewWindowRect.right += pos.x - window_rect.left;
            pNewWindowRect.bottom += pos.y - window_rect.top;
            pNewClientRect.offset(pos.x - window_rect.left, pos.y - window_rect.top);
        }
        return true;
    }

    private static int WINPOS_MinMaximize(WinWindow wndPtr, int cmd, WinRect rect) {
        int swpFlags = 0;
        WinPoint size = new WinPoint();
        int old_style;
        WINDOWPLACEMENT wpl = new WINDOWPLACEMENT();
        GetWindowPlacement(wndPtr, wpl);
        if (Hook.HOOK_CallHooks(WH_CBT, HCBT_MINMAX, wndPtr.handle, cmd) != 0) return SWP_NOSIZE | SWP_NOMOVE;
        if (IsIconic(wndPtr.handle) != 0) {
            switch(cmd) {
                case SW_SHOWMINNOACTIVE:
                case SW_SHOWMINIMIZED:
                case SW_FORCEMINIMIZE:
                case SW_MINIMIZE:
                    return SWP_NOSIZE | SWP_NOMOVE;
            }
            if (Message.SendMessageA(wndPtr.handle, WM_QUERYOPEN, 0, 0) == 0) return SWP_NOSIZE | SWP_NOMOVE;
            swpFlags |= SWP_NOCOPYBITS;
        }
        switch(cmd) {
            case SW_SHOWMINNOACTIVE:
            case SW_SHOWMINIMIZED:
            case SW_FORCEMINIMIZE:
            case SW_MINIMIZE:
                if (WinWindow.get(wndPtr.handle) == null) return 0;
                if ((wndPtr.dwStyle & WS_MAXIMIZE) != 0) wndPtr.flags |= WIN_RESTORE_MAX; else wndPtr.flags &= ~WIN_RESTORE_MAX;
                old_style = wndPtr.dwStyle;
                wndPtr.dwStyle |= WS_MINIMIZE;
                wndPtr.dwStyle &= ~WS_MAXIMIZE;
                wpl.ptMinPosition = WINPOS_FindIconPos(wndPtr, wpl.ptMinPosition);
                if ((old_style & WS_MINIMIZE) != 0) swpFlags |= SWP_STATECHANGED;
                rect.set(wpl.ptMinPosition.x, wpl.ptMinPosition.y, wpl.ptMinPosition.x + SysParams.GetSystemMetrics(SM_CXICON), wpl.ptMinPosition.y + SysParams.GetSystemMetrics(SM_CYICON));
                swpFlags |= SWP_NOCOPYBITS;
                break;
            case SW_MAXIMIZE:
                old_style = wndPtr.dwStyle;
                if ((old_style & WS_MAXIMIZE) != 0 && (old_style & WS_VISIBLE) != 0) return SWP_NOSIZE | SWP_NOMOVE;
                WINPOS_GetMinMaxInfo(wndPtr, size, wpl.ptMaxPosition, null, null);
                old_style = wndPtr.dwStyle;
                wndPtr.dwStyle |= WS_MAXIMIZE;
                wndPtr.dwStyle &= ~WS_MINIMIZE;
                if ((old_style & WS_MINIMIZE) != 0) {
                    wndPtr.flags |= WIN_RESTORE_MAX;
                    WINPOS_ShowIconTitle(wndPtr, false);
                }
                if ((old_style & WS_MAXIMIZE) == 0) swpFlags |= SWP_STATECHANGED;
                rect.set(wpl.ptMaxPosition.x, wpl.ptMaxPosition.y, wpl.ptMaxPosition.x + size.x, wpl.ptMaxPosition.y + size.y);
                break;
            case SW_SHOWNOACTIVATE:
                wndPtr.flags &= ~WIN_RESTORE_MAX;
            case SW_SHOWNORMAL:
            case SW_RESTORE:
            case SW_SHOWDEFAULT:
                old_style = wndPtr.dwStyle;
                wndPtr.dwStyle &= ~WS_MINIMIZE | WS_MAXIMIZE;
                if ((old_style & WS_MINIMIZE) != 0) {
                    boolean restore_max;
                    WINPOS_ShowIconTitle(wndPtr, false);
                    restore_max = (wndPtr.flags & WIN_RESTORE_MAX) != 0;
                    if (restore_max) {
                        WINPOS_GetMinMaxInfo(wndPtr, size, wpl.ptMaxPosition, null, null);
                        wndPtr.dwStyle |= WS_MAXIMIZE;
                        swpFlags |= SWP_STATECHANGED;
                        rect.set(wpl.ptMaxPosition.x, wpl.ptMaxPosition.y, wpl.ptMaxPosition.x + size.x, wpl.ptMaxPosition.y + size.y);
                        break;
                    }
                } else if ((old_style & WS_MAXIMIZE) == 0) break;
                swpFlags |= SWP_STATECHANGED;
                rect.copy(wpl.rcNormalPosition);
                break;
        }
        return swpFlags;
    }

    public static void WINPOS_ActivateOtherWindow(int hwnd) {
        int hwndTo = 0;
        boolean done = false;
        if ((WinWindow.GetWindowLongA(hwnd, GWL_STYLE) & WS_POPUP) != 0 && (hwndTo = WinWindow.GetWindow(hwnd, GW_OWNER)) != 0) {
            hwndTo = WinWindow.GetAncestor(hwndTo, GA_ROOT);
            if (can_activate_window(hwndTo)) done = true;
        }
        if (!done) {
            hwndTo = hwnd;
            while (true) {
                if ((hwndTo = WinWindow.GetWindow(hwndTo, GW_HWNDNEXT)) == 0) break;
                if (can_activate_window(hwndTo)) break;
            }
        }
        int fg = Focus.GetForegroundWindow();
        if (fg == 0 || (hwnd == fg)) {
            if (Focus.SetForegroundWindow(hwndTo) != 0) return;
        }
        if (Focus.SetActiveWindow(hwndTo) == 0) Focus.SetActiveWindow(0);
    }

    static boolean can_activate_window(int hwnd) {
        int style;
        if (hwnd == 0) return false;
        style = WinWindow.GetWindowLongA(hwnd, GWL_STYLE);
        if ((style & WS_VISIBLE) == 0) return false;
        if ((style & (WS_POPUP | WS_CHILD)) == WS_CHILD) return false;
        return (style & WS_DISABLED) != 0;
    }

    static WinPoint WINPOS_FindIconPos(WinWindow wndPtr, WinPoint pt) {
        return new WinPoint();
    }

    private static void WINPOS_GetMinMaxInfo(WinWindow wndPtr, WinPoint maxSize, WinPoint maxPos, WinPoint minTrack, WinPoint maxTrack) {
    }

    private static boolean WINPOS_ShowIconTitle(WinWindow wndPtr, boolean bShow) {
        return false;
    }
}
