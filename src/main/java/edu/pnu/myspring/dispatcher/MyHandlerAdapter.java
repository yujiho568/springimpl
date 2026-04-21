package edu.pnu.myspring.dispatcher;

import edu.pnu.myspring.annotations.MyRequestMapping;
import edu.pnu.myspring.annotations.MyPostMapping;

import java.lang.reflect.Method;

public class MyHandlerAdapter {
    private MyHandlerMapping handlerMapping;

    public MyHandlerAdapter(MyHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }
    public boolean supports(Object handler){
        return (handler instanceof Method) &&
                (((Method)handler).isAnnotationPresent(MyRequestMapping.class)||((Method)handler).isAnnotationPresent(MyPostMapping.class));
    }

    public UserResponse handle(UserRequest userRequest, Object handler, Object[] args) throws Exception {
        if(!(handler instanceof Method)){
            throw new IllegalArgumentException("Handler is not a method");
        }
        if(!supports(handler)){
            throw new IllegalArgumentException("Handler method is not annotated with MyRequestMapping or MyPostMapping");
        }
        Method method = (Method) handler;
        Object controller = handlerMapping.getControllerForMethod(method);
        Object result = method.invoke(controller, args);

        String responseBody = result.toString();

        return UserResponse.success(responseBody);
    }

}
