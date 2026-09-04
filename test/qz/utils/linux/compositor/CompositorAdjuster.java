package qz.utils.linux.compositor;

import qz.utils.linux.compositor.dispatcher.Dispatcher;
import qz.utils.linux.compositor.dispatcher.KwinThemer;
import qz.utils.linux.compositor.dispatcher.MutterThemer;
import qz.utils.linux.compositor.dispatcher.XfceThemer;

import java.util.function.Supplier;

public enum CompositorAdjuster {
    KWIN(KwinThemer::new),
    MUTTER(MutterThemer::new),
    XFCE(XfceThemer::new);

    private final Supplier<Dispatcher> supplier;
    private static Dispatcher dispatcher;

    CompositorAdjuster(Supplier<Dispatcher> supplier) {
        this.supplier = supplier;
    }

    public static Dispatcher getDispatcher() {
        if(dispatcher == null) {
            dispatcher = switch(Compositor.detectCompositor()) {
                case KWIN -> KWIN.supplier.get();
                case  MUTTER -> MUTTER.supplier.get();
                case XFCE -> XFCE.supplier.get();
                default -> throw new UnsupportedOperationException("Unsupported compositor for adjusting values " +  Compositor.detectCompositor());
            };
        }
        return dispatcher;
    }
}