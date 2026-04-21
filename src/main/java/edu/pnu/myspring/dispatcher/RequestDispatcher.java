package edu.pnu.myspring.dispatcher;

import edu.pnu.myspring.core.MyApplicationContext;
import edu.pnu.myspring.utils.InputProvider;
import edu.pnu.myspring.utils.MyJsonParser;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RequestDispatcher {

    private final MyHandlerMapping handlerMapping;
    private final MyHandlerAdapter handlerAdapter;
    private final InputProvider inputProvider;

    public RequestDispatcher(MyApplicationContext context, InputProvider inputProvider) {
        this.handlerMapping = new MyHandlerMapping(new ControllerRegistry(context));
        this.handlerAdapter = new MyHandlerAdapter(handlerMapping);
        this.inputProvider = inputProvider;
    }

    private void dispatch(UserRequest userRequest) {
        if (userRequest == null) return;
        try {
            Method handler = handlerMapping.getHandler(userRequest.getMethod(), userRequest.getUri());
            if (handler == null) {
                System.out.println("ERROR: No mapping found for " + userRequest.getMethod() + " " + userRequest.getUri());
                return;
            }
            Map<String, Object> params = new HashMap<>();
            
            if (userRequest.getMethod().equals("POST") || userRequest.getMethod().equals("PUT")) {
                String jsonBody = userRequest.getJsonBody();
                if (jsonBody != null && !jsonBody.isEmpty()) {
                    params.putAll(MyJsonParser.parse(jsonBody));
                }
            }
            
            Map<String, String> pathVariables = handlerMapping.extractPathVariables(userRequest);
            if (pathVariables != null) {
                params.putAll(pathVariables);
            }
            
            Object[] args = handlerMapping.extractArgsForMethod(handler, params);
            UserResponse response = handlerAdapter.handle(userRequest, handler, args);
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.out.println("ERROR during dispatch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startListening() {
        while (true) {
            UserRequest userRequest = inputProvider.getInput();
            if (userRequest == null || "EXIT".equalsIgnoreCase(userRequest.getMethod())) {
                System.out.println("Exiting application...");
                break;
            }
            if ("CONTINUE".equalsIgnoreCase(userRequest.getMethod())) {
                continue;
            }
            dispatch(userRequest);
        }
    }
}
