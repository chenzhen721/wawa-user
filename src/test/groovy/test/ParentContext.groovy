package test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * date: 13-9-9 下午5:05
 *
 * @author: yangyang.cong@ttpod.com
 */
final class ParentContext implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public void initialize(ConfigurableApplicationContext applicationContext) {
        ApplicationContext parent = new GenericXmlApplicationContext("classpath*:spring/*.xml");
        applicationContext.setParent(parent);
        BaseTest.injectParentContext(parent);
    }
}
