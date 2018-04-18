package lib;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import play.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonHelper {
    public final static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String toString(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map toMap(String json) {
        if (StringUtils.isBlank(json)) return null;
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            Logger.error(e, e.getMessage());
        }
        return null;
    }

    public static <T> T fromString(String str, Class<T> valueType) {
        if (str == null) {
            return null;
        }

        try {
            byte[] bytes = str.getBytes("UTF-8");
            return mapper.readValue(bytes, 0, bytes.length, valueType);
        } catch (Exception e) {
            Logger.error(e, e.getMessage());
        }

        return null;
    }

    public static <T> T fromString(String str, JavaType javaType) {

        if (javaType == null) {
            return null;
        }
        if (str == null) {
            return null;
        }

        try {
            byte[] bytes = str.getBytes("UTF-8");
            return mapper.readValue(bytes, 0, bytes.length, javaType);
        } catch (Exception e) {
            Logger.error(e, e.getMessage());
        }

        return null;
    }

    public static <T> T fromString(String str, TypeReference valueTypeRef) {
        if (str == null) {
            return null;
        }

        try {
            byte[] bytes = str.getBytes("UTF-8");
            return mapper.readValue(bytes, 0, bytes.length, valueTypeRef);
        } catch (Exception e) {
            Logger.error(e, e.getMessage());
        }

        return null;
    }

    public static List<Map> strListToMapList(Collection<String> stringList) {
        List<Map> mapList = new ArrayList<>();
        if (stringList != null) {
            mapList.addAll(stringList.stream().map(JsonHelper::toMap).collect(Collectors.toList()));
        }

        return mapList;
    }

    public static <T> T fromString(String str, Class<?> collectionClass, Class<?>... elementClasses) {
        if (str == null) {
            return null;
        }
        try {
            byte[] bytes = str.getBytes("UTF-8");
            return mapper.readValue(bytes, 0, bytes.length, getCollectionType(collectionClass, elementClasses));
        } catch (Exception e) {
            Logger.error(e, e.getMessage());
        }

        return null;
    }

    private static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    public static Map<String, Object> toMap(Object object) {
        return (Map<String, Object>) mapper.convertValue(object, Map.class);
    }

    public static Map<String, String> toStringMap(Object object) {
        return (Map<String, String>) mapper.convertValue(object, Map.class);
    }

    public static <T> T restore(Map map, Class<T> clazz) {
        if (map != null) {
            try {
                return mapper.convertValue(map, clazz);
            } catch (Exception e) {
                Logger.error(e, e.getMessage());
            }
        }
        return null;
    }


}
