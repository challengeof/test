package lib;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author bowen
 */
public class Const {
    public final static Injector INJECT = Guice.createInjector(new AbstractModule() {
        @Override
        protected void configure() {

        }
    });
}
