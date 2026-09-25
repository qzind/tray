package qz.ui.component.iconcache;

import org.apache.commons.lang3.tuple.Pair;
import qz.ui.component.IconCache;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Type {
    SYSTEM_TRAY(16, 20, 24, 32, 40, 48),
    TASK_BAR(16, 20, 24, 32, 40, 48),
    DIALOG(45),
    LOGO(260),
    MENU(16);

    final int[] sizes;

    Type(int ... sizes) {
        this.sizes = sizes;
    }

    public int[] getSizes() {
        return sizes;
    }
}
