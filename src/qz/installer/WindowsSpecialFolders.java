/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer;

import com.sun.jna.platform.win32.*;
import qz.utils.WindowsUtilities;

/**
 * Windows XP-compatible special folder's wrapper for JNA
 *
 */
public enum WindowsSpecialFolders {
    ADMIN_TOOLS(ShlObj.CSIDL_ADMINTOOLS, KnownFolders.FOLDERID_AdminTools),
    STARTUP_ALT(ShlObj.CSIDL_ALTSTARTUP, KnownFolders.FOLDERID_Startup),
    ROAMING_APPDATA(ShlObj.CSIDL_APPDATA, KnownFolders.FOLDERID_RoamingAppData),
    RECYCLING_BIN(ShlObj.CSIDL_BITBUCKET, KnownFolders.FOLDERID_RecycleBinFolder),
    CD_BURNING(ShlObj.CSIDL_CDBURN_AREA, KnownFolders.FOLDERID_CDBurning),
    COMMON_ADMIN_TOOLS(ShlObj.CSIDL_COMMON_ADMINTOOLS, KnownFolders.FOLDERID_CommonAdminTools),
    COMMON_STARTUP_ALT(ShlObj.CSIDL_COMMON_ALTSTARTUP, KnownFolders.FOLDERID_CommonStartup),
    PROGRAM_DATA(ShlObj.CSIDL_COMMON_APPDATA, KnownFolders.FOLDERID_ProgramData),
    PUBLIC_DESKTOP(ShlObj.CSIDL_COMMON_DESKTOPDIRECTORY, KnownFolders.FOLDERID_PublicDesktop),
    PUBLIC_DOCUMENTS(ShlObj.CSIDL_COMMON_DOCUMENTS, KnownFolders.FOLDERID_PublicDocuments),
    COMMON_FAVORITES(ShlObj.CSIDL_COMMON_FAVORITES, KnownFolders.FOLDERID_Favorites),
    COMMON_MUSIC(ShlObj.CSIDL_COMMON_MUSIC, KnownFolders.FOLDERID_PublicMusic),
    COMMON_OEM_LINKS(ShlObj.CSIDL_COMMON_OEM_LINKS, KnownFolders.FOLDERID_CommonOEMLinks),
    COMMON_PICTURES(ShlObj.CSIDL_COMMON_PICTURES, KnownFolders.FOLDERID_PublicPictures),
    COMMON_PROGRAMS(ShlObj.CSIDL_COMMON_PROGRAMS, KnownFolders.FOLDERID_CommonPrograms),
    COMMON_START_MENU(ShlObj.CSIDL_COMMON_STARTMENU, KnownFolders.FOLDERID_CommonStartMenu),
    COMMON_STARTUP(ShlObj.CSIDL_COMMON_STARTUP, KnownFolders.FOLDERID_CommonStartup),
    COMMON_TEMPLATES(ShlObj.CSIDL_COMMON_TEMPLATES, KnownFolders.FOLDERID_CommonTemplates),
    COMMON_VIDEO(ShlObj.CSIDL_COMMON_VIDEO, KnownFolders.FOLDERID_PublicVideos),
    COMPUTERS_NEAR_ME(ShlObj.CSIDL_COMPUTERSNEARME, KnownFolders.FOLDERID_NetworkFolder),
    CONNECTIONS_FOLDER(ShlObj.CSIDL_CONNECTIONS, KnownFolders.FOLDERID_ConnectionsFolder),
    CONTROL_PANEL(ShlObj.CSIDL_CONTROLS, KnownFolders.FOLDERID_ControlPanelFolder),
    COOKIES(ShlObj.CSIDL_COOKIES, KnownFolders.FOLDERID_Cookies),
    DESKTOP_VIRTUAL(ShlObj.CSIDL_DESKTOP, KnownFolders.FOLDERID_Desktop),
    DESKTOP(ShlObj.CSIDL_DESKTOPDIRECTORY, KnownFolders.FOLDERID_Desktop),
    COMPUTER_FOLDER(ShlObj.CSIDL_DRIVES, KnownFolders.FOLDERID_ComputerFolder),
    FAVORITES(ShlObj.CSIDL_FAVORITES, KnownFolders.FOLDERID_Favorites),
    FONTS(ShlObj.CSIDL_FONTS, KnownFolders.FOLDERID_Fonts),
    HISTORY(ShlObj.CSIDL_HISTORY, KnownFolders.FOLDERID_History),
    INTERNET_FOLDER(ShlObj.CSIDL_INTERNET, KnownFolders.FOLDERID_InternetFolder),
    INTERNET_CACHE(ShlObj.CSIDL_INTERNET_CACHE, KnownFolders.FOLDERID_InternetCache),
    LOCAL_APPDATA(ShlObj.CSIDL_LOCAL_APPDATA, KnownFolders.FOLDERID_LocalAppData),
    MY_DOCUMENTS(ShlObj.CSIDL_MYDOCUMENTS, KnownFolders.FOLDERID_Documents),
    MY_MUSIC(ShlObj.CSIDL_MYMUSIC, KnownFolders.FOLDERID_Music),
    MY_PICTURES(ShlObj.CSIDL_MYPICTURES, KnownFolders.FOLDERID_Pictures),
    MY_VIDEOS(ShlObj.CSIDL_MYVIDEO, KnownFolders.FOLDERID_Videos),
    NETWORK_NEIGHBORHOOD(ShlObj.CSIDL_NETHOOD, KnownFolders.FOLDERID_NetHood),
    NETWORK_FOLDER(ShlObj.CSIDL_NETWORK, KnownFolders.FOLDERID_NetworkFolder),
    PERSONAL_FOLDDER(ShlObj.CSIDL_PERSONAL, KnownFolders.FOLDERID_Documents),
    PRINTERS(ShlObj.CSIDL_PRINTERS, KnownFolders.FOLDERID_PrintersFolder),
    PRINTING_NEIGHBORHOODD(ShlObj.CSIDL_PRINTHOOD, KnownFolders.FOLDERID_PrintHood),
    PROFILE_FOLDER(ShlObj.CSIDL_PROFILE, KnownFolders.FOLDERID_Profile),
    PROGRAM_FILES(ShlObj.CSIDL_PROGRAM_FILES, KnownFolders.FOLDERID_ProgramFiles),
    PROGRAM_FILESX86(ShlObj.CSIDL_PROGRAM_FILESX86, KnownFolders.FOLDERID_ProgramFilesX86),
    PROGRAM_FILES_COMMON(ShlObj.CSIDL_PROGRAM_FILES_COMMON, KnownFolders.FOLDERID_ProgramFilesCommon),
    PROGRAM_FILES_COMMONX86(ShlObj.CSIDL_PROGRAM_FILES_COMMONX86, KnownFolders.FOLDERID_ProgramFilesCommonX86),
    PROGRAMS(ShlObj.CSIDL_PROGRAMS, KnownFolders.FOLDERID_Programs),
    RECENT(ShlObj.CSIDL_RECENT, KnownFolders.FOLDERID_Recent),
    RESOURCES(ShlObj.CSIDL_RESOURCES, KnownFolders.FOLDERID_ResourceDir),
    RESOURCES_LOCALIZED(ShlObj.CSIDL_RESOURCES_LOCALIZED, KnownFolders.FOLDERID_LocalizedResourcesDir),
    SEND_TO(ShlObj.CSIDL_SENDTO, KnownFolders.FOLDERID_SendTo),
    START_MENU(ShlObj.CSIDL_STARTMENU, KnownFolders.FOLDERID_StartMenu),
    STARTUP(ShlObj.CSIDL_STARTUP, KnownFolders.FOLDERID_Startup),
    SYSTEM(ShlObj.CSIDL_SYSTEM, KnownFolders.FOLDERID_System),
    SYSTEMX86(ShlObj.CSIDL_SYSTEMX86, KnownFolders.FOLDERID_SystemX86),
    TEMPLATES(ShlObj.CSIDL_TEMPLATES, KnownFolders.FOLDERID_Templates),
    WINDOWS(ShlObj.CSIDL_WINDOWS, KnownFolders.FOLDERID_Windows);

    private int csidl;
    private Guid.GUID guid;
    WindowsSpecialFolders(int csidl, Guid.GUID guid) {
        this.csidl = csidl;
        this.guid = guid;
    }

    public String getPath() {
        if(WindowsUtilities.isWindowsXP()) {
            return Shell32Util.getSpecialFolderPath(csidl, false);
        }
        return Shell32Util.getKnownFolderPath(guid);
    }

    @Override
    public String toString() {
        return getPath();
    }
}
