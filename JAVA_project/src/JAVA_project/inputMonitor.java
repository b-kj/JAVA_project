package JAVA_project;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;

public class inputMonitor {
	
    public static long getIdleTime() {

        WinUser.LASTINPUTINFO info =
                new WinUser.LASTINPUTINFO();

        info.cbSize = info.size();

        User32.INSTANCE.GetLastInputInfo(info);

        return Kernel32.INSTANCE.GetTickCount()
                - info.dwTime;
    }
}