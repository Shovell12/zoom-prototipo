package uni.pe.cliente;

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.function.Consumer;

class ReunionEventBus {

    private final EnumMap<ReunionEvent.Type, List<Consumer<ReunionEvent>>> handlers =
            new EnumMap<>(ReunionEvent.Type.class);

    void subscribe(ReunionEvent.Type type, Consumer<ReunionEvent> handler) {
        handlers.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
    }

    void publish(ReunionEvent event) {
        if (SwingUtilities.isEventDispatchThread()) {
            dispatch(event);
        } else {
            SwingUtilities.invokeLater(() -> dispatch(event));
        }
    }

    private void dispatch(ReunionEvent event) {
        List<Consumer<ReunionEvent>> list = handlers.get(event.type());
        if (list != null) new ArrayList<>(list).forEach(h -> h.accept(event));
    }
}
