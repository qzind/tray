package qz.utils.linux;

import java.io.IOException;

public interface DeTheme {
    boolean isDarkDesktop();
    String getTheme() throws IOException;
    void setTheme(String themeName) throws IOException;
    void setTheme(boolean isDark) throws IOException;
}
