package playPlugins;

import com.sun.tools.attach.VirtualMachine;
import org.aspectj.weaver.loadtime.Agent;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.vfs.VirtualFile;

import java.lang.management.ManagementFactory;

/**
 * @author bowen
 */
public class AopPlugin extends PlayPlugin {
    @Override
    public void onLoad() {
        System.setProperty("org.aspectj.weaver.loadtime.configuration", "file:" + Play.getFile("/conf/aop.xml").getAbsolutePath());
        System.setProperty("aj.weaving.loadersToSkip", "com.google.inject.internal.BytecodeGen$BridgeClassLoader," + "java.net.URLClassLoader,org.codehaus.groovy.runtime.callsite.CallSiteClassLoader," + "org.codehaus.groovy.reflection.SunClassLoader," + "play.templates.GroovyTemplate$TClassLoader," + "sun.misc.Launcher$AppClassLoader," + "sun.misc.Launcher$ExtClassLoader," + "org.apache.catalina.loader.StandardClassLoader," + "groovy.lang.GroovyClassLoader.InnerLoader," + "org.apache.catalina.loader.WebappClassLoader," + "groovy.lang.GroovyClassLoader," + "groovy.lang.GroovyClassLoader$InnerLoader");
        if (Play.initialized) {
            checkOrLoadAspectJAgent();
        } else if (Play.usePrecompiled) {
            Play.javaPath.add(VirtualFile.fromRelativePath("precompiled/java/"));
            checkOrLoadAspectJAgent();
        }
    }

    public static boolean checkOrLoadAspectJAgent() {
        try {
            Agent.getInstrumentation();
        } catch (NoClassDefFoundError e) {
            throw e;
        } catch (UnsupportedOperationException e) {
            return dynamicallyLoadAspectJAgent();
        }
        return true;
    }

    public static boolean dynamicallyLoadAspectJAgent() {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        String pid = nameOfRunningVM.substring(0, p);
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            String jarFilePath = Agent.class.getResource("").toString().replace("jar:file:", "").split("!")[0];

            String os = System.getProperties().getProperty("os.name");
            if (os.toLowerCase().startsWith("win") && jarFilePath.startsWith("/")) {
                jarFilePath = jarFilePath.substring(1);
            }

            Logger.info("aspectj agent load:" + jarFilePath);
            vm.loadAgent(jarFilePath);
            vm.detach();
        } catch (Exception e) {
            Logger.info(e, e.getMessage());
            return false;
        }
        return true;
    }
}
