package jobs;

import com.google.inject.Inject;
import lib.Const;
import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.mvc.Controller;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * @author bowen
 */
@OnApplicationStart
public class Bootstrap extends Job {

    @Override
    public void doJob() {

        try {

            System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
            controllerInject();
        } catch (Exception e) {
            Logger.error(e, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void controllerInject() throws Exception{
        List<ApplicationClasses.ApplicationClass> applicationClasses = Play.classes.getAssignableClasses(Controller.class);
        for (ApplicationClasses.ApplicationClass applicationClass : applicationClasses) {
            System.out.println(applicationClass.name);
            Field[] fields = applicationClass.javaClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Inject.class) && Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    Class<?> fieldClazz = field.getType();
                    Object fieldObj = Const.INJECT.getInstance(fieldClazz);
                    field.set(null, fieldObj);
                }
            }
        }
    }
}
