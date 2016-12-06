/** Post-Install Cleanup **/

var app = getArg(0);

if (app) {
    removeStartupEntries(app);
} else {
    WScript.Echo("No app name provided.  Exiting.");
    WScript.Quit(1);
}

WScript.Quit(0);

/**
 * Cycles through all users on system and removes matching startup entries.
 */
function removeStartupEntries(app) {
    WScript.Echo('Removing startup entries for all users matching "' + app + '"');
    var shell = new ActiveXObject("WScript.shell");
    // get all users
    var proc = shell.Exec('reg.exe query HKU');
    var users = proc.StdOut.ReadAll().split(/[\r\n]+/);
    for (var i = 0; i < users.length; i++) {
        try {
            var key = trim(users[i]) + "\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\\" + app;
            shell.RegDelete(key);
            WScript.Echo(' - [success] Removed "' + app + '" startup in ' + users[i]);
        } catch(ignore) {}
    }
}

/*
 * Gets then nth argument passed into this script
 * Returns defaultVal if argument wasn't found
 */
function getArg(index, defaultVal) {
    if (index >= WScript.Arguments.length || trim(WScript.Arguments(index)) == "") {
        return defaultVal;
    }
    return WScript.Arguments(index);
}

/*
 * Functional equivalent of foo.trim()
 */
function trim(val) {
    return val.replace(/^\s+/,'').replace(/\s+$/,'');
}