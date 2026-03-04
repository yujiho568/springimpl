package edu.pnu.myspring.dispatcher;

import edu.pnu.myspring.annotations.MyRequestMapping;
import edu.pnu.myspring.annotations.PathVariable;
import edu.pnu.myspring.annotations.PostMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyHandlerMapping {
    private final ControllerRegistry registry;
    private final Map<String, Method> handlerMappings = new HashMap<>();
    private final Map<Method, Object> methodToControllerMap = new HashMap<>();
    private final Map<String, String> patternMappings = new HashMap<>();
    public MyHandlerMapping(ControllerRegistry registry){
        this.registry = registry;
        initializeHandlerMappings();
    }
    private void initializeHandlerMappings(){
        for(Class<?> controllerClass : registry.getAllControllerClasses()){
            for(Method method : controllerClass.getDeclaredMethods()){
                if(method.isAnnotationPresent(MyRequestMapping.class)){
                    MyRequestMapping get = method.getAnnotation(MyRequestMapping.class);
                    String key = generateKeyPattern(get.method(), get.value());
                    handlerMappings.put(key,method);
                    patternMappings.put(key, convertToPattern(key));
                }else if(method.isAnnotationPresent(PostMapping.class)){
                    PostMapping post = method.getAnnotation(PostMapping.class);
                    String key = generateKeyPattern(post.method(), post.value());
                    handlerMappings.put(key,method);
                    patternMappings.put(key, convertToPattern(key));

                }
                methodToControllerMap.put(method, registry.getController(controllerClass));
            }
        }
    }
    private String generateKeyPattern(String method, String uri){
        return method + " " + uri;
    }
    private String findMatchingPattern(String requestMethod, String path){
        String input = requestMethod + " " + path;
        for(String key : patternMappings.keySet()){
            if(Pattern.compile(patternMappings.get(key)).matcher(input).matches()){
                return key;
            }
        }
        return null;
    }
    public Method getHandler(String requestMethod, String path){
        String matchingPattern = findMatchingPattern(requestMethod, path);
        if(matchingPattern!=null){
            return handlerMappings.get(matchingPattern);
        }
        return null;
    }
    public Map<String, String> extractPathVariables(UserRequest userRequest){
        Map<String, String> pathVariables = new HashMap<>();
        String uriTemplate = findMatchingPattern(userRequest.getMethod(), userRequest.getUri());
        List<String> placeHolders = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{\\w+\\}");
        Matcher matcher = pattern.matcher(uriTemplate);
        while(matcher.find()){
            placeHolders.add(matcher.group(0).replaceAll("\\{|\\}",""));
        }
        String regex = uriPatternToRegex(uriTemplate);
        pattern = Pattern.compile(regex);
        matcher = pattern.matcher(generateKeyPattern(userRequest.getMethod(), userRequest.getUri()));
        if(matcher.matches()){
            for (int i = 1; i < matcher.groupCount(); i++) {
                String name = placeHolders.get(i-1);
                String value = matcher.group(i);
                pathVariables.put(name, value);
            }
        }
        return pathVariables;
    }

    private String uriPatternToRegex(String uriPattern){
        return uriPattern.replaceAll("\\{\\w+\\}", "([^/]+)");
    }
    private String convertToPattern(String value){
        return "^" + value.replace("/","\\/").replaceAll("\\{\\w+\\}", "\\\\w+")+"$";
    }

    public Object[] extractArgsForMethod(Method method, Map<String, Object> params) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            PathVariable pathVariableAnnotation = parameter.getAnnotation(PathVariable.class);
            if(pathVariableAnnotation!=null){
                String pathVariableName = pathVariableAnnotation.value();
                if(params.containsKey(pathVariableName)){
                    args[i] = convertToType(params.get(pathVariableName), parameter.getType());
                }
            }
            else{

            }
        }
        return args;
    }
    private Object convertToType(Object value, Class<?> type){
        if(type.isInstance(value)){
            return type.cast(value);
        }
        else if(type.equals(Integer.class) || type.equals(int.class)){
            return Integer.parseInt(value.toString());
        }
        else if(type.equals(Long.class)){
            return Long.valueOf(value.toString());
        }
        throw new UnsupportedOperationException("Conversion not supported for type: " + type);
    }
    public Object getControllerForMethod(Method method){
        return methodToControllerMap.get(method);
    }
}
