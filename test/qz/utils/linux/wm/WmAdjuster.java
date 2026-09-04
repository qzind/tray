package qz.utils.linux.wm;

import qz.utils.linux.wm.dispatcher.Dispatcher;
import qz.utils.linux.wm.dispatcher.KwinThemer;
import qz.utils.linux.wm.dispatcher.MutterThemer;
import qz.utils.linux.wm.dispatcher.XfceThemer;

import java.util.function.Supplier;

public enum WmAdjuster {
    KWIN(KwinThemer::new),
    MUTTER(MutterThemer::new),
    XFCE(XfceThemer::new);

    private final Supplier<Dispatcher> supplier;
    private static Dispatcher dispatcher;

    WmAdjuster(Supplier<Dispatcher> supplier) {
        this.supplier = supplier;
    }

    public static Dispatcher getDispatcher() {
        if(dispatcher == null) {
            dispatcher = switch(Wm.detectWm()) {
                case KWIN -> KWIN.supplier.get();
                case  MUTTER -> MUTTER.supplier.get();
                case XFCE -> XFCE.supplier.get();
                default -> throw new UnsupportedOperationException("Unsupported window manager for adjusting values " +  Wm.detectWm());
            };
        }
        return dispatcher;
    }
}