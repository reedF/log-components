package test.zipkin.log.es;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

public class MySpringJUnit4ClassRunner extends SpringJUnit4ClassRunner {

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    public MySpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        //http://stefanbirkner.github.io/system-rules/#EnvironmentVariables
        environmentVariables.set("es.set.netty.runtime.available.processors", "false");
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }
}