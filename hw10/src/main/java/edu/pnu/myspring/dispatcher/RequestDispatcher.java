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

        try {

            Method handler = handlerMapping.getHandler(userRequest.getMethod(), userRequest.getUri());

            if (handler == null) {

                System.out.println("ERROR: No mapping found for " + userRequest.getMethod() + " " + userRequest.getUri());

                return;

            }

            Map<String, Object> params = new HashMap<>();

            // Handle JSON body for POST and PUT requests

            if (userRequest.getMethod().equals("POST") || userRequest.getMethod().equals("PUT")) {

                String jsonBody = userRequest.getJsonBody();

                if (jsonBody != null && !jsonBody.isEmpty()) {

                    params.putAll(MyJsonParser.parse(jsonBody)); // MyJsonParser is a hypothetical parser you'd need to implement

                }

            }

            // Extract path variables from the request URI (쉬운 방법으로, 예를 들어, 메서드 파라미터명을 미리 등록해 놓음, 구현하셔도 됩니다!)

            Map<String, String> pathVariables = handlerMapping.extractPathVariables(userRequest);

            params.putAll(pathVariables);

            // Extract args based on the extracted path variables and method signature

            Object[] args = handlerMapping.extractArgsForMethod(handler, params);



            UserResponse response = handlerAdapter.handle(userRequest, handler, args);

            System.out.println("Response: " + response);

        } catch (Exception e) {

            System.out.println("ERROR: " + e.getMessage());

        }

    }



    public void startListening() {

        while (true) {

            UserRequest userRequest = inputProvider.getInput();

            if ("EXIT".equalsIgnoreCase(userRequest.getMethod())) {

                System.out.println("Exiting application...");

                break;

            }

            dispatch(userRequest);

        }

    }

}