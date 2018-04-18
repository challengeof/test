package lib;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import play.Play;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author bowen
 */
public class Debug {

    private static final ThreadLocal<Boolean> flagTL = new ThreadLocal<>();

    private static final ThreadLocal<List<String>> infoTL = new ThreadLocal<>();

    public static void open(String flag) {
        flagTL.set(StringUtils.isNotBlank(flag) && Objects.equals(Play.configuration.getProperty("debug"), flag));
    }

    public static boolean isOpen() {
        Boolean flag = flagTL.get();
        return flag != null &&flag;
    }

    public static void append(String info) {
        if (!isOpen()) {
            return;
        }
        List<String> currentInfo = infoTL.get();
        if (CollectionUtils.isEmpty(currentInfo)) {
            currentInfo = new ArrayList<>();
        }

        currentInfo.add(info);

        infoTL.set(currentInfo);
    }

    public static List<String> fetch() {
        try {
            return infoTL.get();
        } finally {
            infoTL.remove();
        }
    }
}
