package edu.pnu.myspring.dispatcher;

import edu.pnu.myspring.core.MyApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ControllerRegistry {
    private MyApplicationContext context;
    private Map<Class<?>, Object> controllerCache = new HashMap<>();
    public ControllerRegistry(MyApplicationContext context) {
        this.context = context;
    }

    public <T> T getController(Class<T> type) {
        if(!controllerCache.containsKey(type)){
            controllerCache.put(type, context.getBean(type));
        }
        return type.cast(controllerCache.get(type));
    }

    public Set<Class<?>> getAllControllerClasses() {
        return context.getControllerClasses().keySet();
    }
}
