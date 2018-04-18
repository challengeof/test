package domains;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lib.JsonHelper;
import play.Logger;
import play.db.jpa.GenericModel;

import java.util.Map;

/**
 * @author bowen
 */
@JsonIgnoreProperties({"entityId", "persistent"})
public class BaseModel extends GenericModel {

    public final static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap() {
        return (Map<String, Object>) mapper.convertValue(this, Map.class);
    }


    public String toJson() {
        return JsonHelper.toString(this);
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
