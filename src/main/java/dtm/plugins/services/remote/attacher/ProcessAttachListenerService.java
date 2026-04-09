package dtm.plugins.services.remote.attacher;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ProcessAttachListenerService {
    void attach();
    void onEvent(BiConsumer<String, String> onEvent);
    void onError(Consumer<Throwable> onError);
    void close();
    void interrupt();
    boolean isClosed();
}
