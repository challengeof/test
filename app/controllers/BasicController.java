package controllers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lib.BizException;
import lib.Debug;
import lib.JsonHelper;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.db.jpa.NoTransaction;
import play.mvc.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bowen
 */
public abstract class BasicController extends Controller {

    protected static ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public static void handleArgsErrors() {
        if (Validation.hasErrors()) {
            String message = Validation.errors().stream().map(error -> error.getKey() + ":" + error.message()).collect(Collectors.joining(","));
            Logger.info("url: " + request.url + ", 参数非法 " + message);
            badRequest(message);
        }
    }

    @Util
    protected static void renderJsonP(Object obj) {
        if (obj == null) renderText("");
        String str;
        if (obj instanceof String) {
            str = obj.toString();
        } else {
            str = JsonHelper.toString(obj);
        }
        String callback = request.params.get("_callback");
        if (StringUtils.isNotBlank(callback)) {
            renderText(callback + "(" + str + ")");
        } else {
            renderText(StringUtils.stripToEmpty(str));
        }
    }

    @Util
    protected static void ok(final Object data) {
        Map<String, Object> result = new HashMap<String, Object>() {{
            put("ret", 200);
            put("data", data);
        }};
        renderJsonP(result);
    }

    @Util
    protected static void ok() {
        Map<String, Object> result = new HashMap<String, Object>() {{
            put("ret", 200);
            put("data", Collections.emptyMap());
        }};
        renderJsonP(result);
    }

    @Util
    public static void err(BizException bizException) {
        Map<String, Object> map = ImmutableMap.of("ret", 500, "msg", bizException.getMessage(), "code", bizException.code);
        Logger.error("response status:%s, %s %s %s %s.", response.status, getRealIp(), request.method, request.url, map);
        renderJsonP(map);
    }

    private static ThreadLocal<Long> requestTimeTL = new ThreadLocal<>();

    @Before
    public static void startTime() {
        requestTimeTL.set(System.currentTimeMillis());
    }

    @Finally
    public static void endTime() {
        long executionTime = System.currentTimeMillis() - (requestTimeTL.get() == null ? 0 : requestTimeTL.get());
        Map<String, String> map = request.params.allSimple();
        map.remove("body");

        if (!request.actionMethod.equals("healthCheck")) {
            Logger.info("response status:%s, %s %s %s %s spent %sms.", response.status, getRealIp(), request.method, request.url, map, executionTime);
        }
    }

    @Util
    public static String getRealIp() {
        String ip;
        if (request.headers.containsKey("x-forwarded-for") && !request.headers.get("x-forwarded-for").values.isEmpty()) {
            ip = request.headers.get("x-forwarded-for").values.get(0);
        } else if (request.headers.containsKey("client-ip") && !request.headers.get("client-ip").values.isEmpty()) {
            ip = request.headers.get("client-ip").values.get(0);
        } else if (request.headers.containsKey("x-real-ip") && !request.headers.get("x-real-ip").values.isEmpty()) {
            ip = request.headers.get("x-real-ip").values.get(0);
        } else {
            ip = request.remoteAddress;
        }
        if (ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(","));
        }
        return ip;
    }

    @Util
    protected static Map<String, Object> getParams() {
        Map<String, String[]> params = request.params.all();
        params.remove("controller");
        params.remove("action");
        params.remove("body");

        Map<String, Object> paramMap = new HashMap<>();
        for (String key : params.keySet()) {
            if (!key.contains("[]")) {
                paramMap.put(key, params.get(key)[0]);
            } else {
                paramMap.put(key.replaceFirst("\\[\\]", ""), params.get(key));
            }
        }
        return paramMap;
    }

    @Util
    protected static <T> T getParameter(Class<?> cls) {
        return restore(getParams(), (Class<T>) cls);
    }

    @Util
    protected static <T> T restore(Map<String, Object> map, Class<T> clazz) {
        if (map != null) {
            try {
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                T param = objectMapper.convertValue(map, clazz);
                Field[] fields = clazz.getFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Required.class)) {
                        if (field.get(param) == null) {
                            badRequest(String.format("%s required", field.getName()));
                        }
                    }
                }
                return param;
            } catch (Exception e) {
                Logger.error(e, e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    protected static void render(Object... args) {
        throw new RuntimeException("not supported");
    }

    protected static void renderTemplate(Object object) {
        renderTemplate(JsonHelper.toMap(object));
    }

    protected static void renderTemplate(Map<String, Object> args) {
        Map<String, Object> businessArgs = new HashMap<>(args);

        if (Debug.isOpen()) {
            businessArgs.put("debug", true);
            businessArgs.put("debugInfo", Debug.fetch());
        }


        Controller.renderTemplate(businessArgs);
    }

    protected static void renderTemplate(String templateName, Map<String, Object> args) {
        Map<String, Object> businessArgs = new HashMap<>(args);

        if (Debug.isOpen()) {
            businessArgs.put("debug", true);
            businessArgs.put("debugInfo", Debug.fetch());
        }


        Controller.renderTemplate(templateName, businessArgs);
    }

    protected static void renderTemplate() {
        renderTemplate(ImmutableMap.of());
    }

    protected static void renderTemplate(String templateName) {
        renderTemplate(templateName, ImmutableMap.of());
    }

    @Catch(BizException.class)
    public static void cacheBizException(BizException bizException) {
        err(bizException);
    }

}
