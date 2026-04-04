package qz.ui.headless;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import qz.build.jlink.Parsable;
import qz.common.Constants;
import qz.utils.ArgValue;

import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;

public class Endpoint {
    private static final Logger log = LogManager.getLogger(Endpoint.class);

    public interface Promptable {
        JSONObject prompt(Endpoint endpoint, JSONObject data);
    }

    public enum Type implements Parsable<Type> {
        FILE_PROMPT,
        REST_URL;
    }

    private final URL url;
    private final Path path;
    private final Type type;

    public Endpoint(Type type, Path path) {
        this.type = type;
        this.path = path;
        this.url = null;
    }

    public Endpoint(Type type, URL url) {
        this.type = type;
        this.path = null;
        this.url = url;
    }

    public Type getType() {
        return type;
    }

    public Path getPath() {
        return path;
    }

    public Path getRequestPath() {
        return path == null ? null : path.resolve("request");
    }

    public Path getResponsePath() {
        return path == null ? null : path.resolve("response");
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return path != null ? path.toString() : (url != null ? url.toString() : null);
    }

    public static Endpoint parse(String value) {
        try {
            String lower = value.toLowerCase(Locale.ENGLISH);
            Type type = lower.matches("https?:")? Type.REST_URL:Type.FILE_PROMPT;
            switch(type) {
                case REST_URL:
                    return new Endpoint(type, new URL(value));
                case FILE_PROMPT:
                default:
                    return new Endpoint(type, Path.of(value));
            }
        }
        catch(Exception e) {
            log.warn("Unable to parse headless dialog endpoint {}.  If you're using {} in headless mode, certificates must be allowed manually via '{}'.", value, Constants.ABOUT_TITLE, ArgValue.ALLOW.getMatch(), e);
        }
        return null;
    }
}
