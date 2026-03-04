package edu.pnu.myspring.core;

import edu.pnu.myspring.annotations.MyComponent;
import edu.pnu.myspring.dispatcher.ControllerRegistry;
import edu.pnu.myspring.dispatcher.MyHandlerAdapter;
import edu.pnu.myspring.dispatcher.MyHandlerMapping;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MyApplicationContext {



    private Map<Class<?>, Object> beanRegistry = new HashMap<>();

    private List<Object> beansToAutowire = new ArrayList<>();

    private Map<Class<?>, Object> controllerClasses = new HashMap<>();



    private final MyHandlerMapping handlerMapping;

    private final MyHandlerAdapter handlerAdapter;



    public MyApplicationContext(String basePackage) {

        scanAndRegisterBeans(basePackage);

        ControllerRegistry controllerRegistry = new ControllerRegistry(this);
        this.handlerMapping = new MyHandlerMapping(controllerRegistry);

        this.handlerAdapter = new MyHandlerAdapter(handlerMapping);

        processAutowiring();

    }



    public MyHandlerAdapter getHandlerAdapter() {

        return handlerAdapter;

    }

    public MyHandlerMapping getHandlerMapping() {

        return handlerMapping;

    }



    public void scanAndRegisterBeans(String basePackage) {

        try {

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            String path = basePackage.replace('.', '/');



            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()) {

                URL resource = resources.nextElement();

                String protocol = resource.getProtocol();



                if (protocol.equals("file")) {

                    File directory = new File(resource.getFile());

                    for (File file : directory.listFiles()) {

                        processFile(file, basePackage);

                    }

                } else if (protocol.equals("jar")) {

                    JarURLConnection conn = (JarURLConnection) resource.openConnection();

                    JarFile jarFile = conn.getJarFile();



                    Enumeration<JarEntry> entries = jarFile.entries();

                    while (entries.hasMoreElements()) {

                        JarEntry entry = entries.nextElement();

                        String entryName = entry.getName();

                        if (entryName.endsWith(".class") && entryName.startsWith(path)) {

                            String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);

                            Class<?> clazz = Class.forName(className);

                            processClass(clazz);

                        }

                    }

                }

            }

        } catch (Exception e) {

            throw new RuntimeException("Failed to scan and register beans", e);

        }

    }



    private void processFile(File file, String basePackage) throws ClassNotFoundException {

        if (file.isDirectory()) {

            for (File subFile : file.listFiles()) {

                processFile(subFile, basePackage + "." + file.getName());

            }

        } else if (file.getName().endsWith(".class")) {

            String className = basePackage + '.' + file.getName().substring(0, file.getName().length() - 6);

            Class<?> clazz = Class.forName(className);

            processClass(clazz);

        }

    }



    private void processClass(Class<?> clazz) {

        if (hasAnnotationOrMetaAnnotation(clazz, MyComponent.class) /*... any other annotations*/) {

            System.out.println(" - Registering bean: " + clazz.getName());

            registerBean(clazz);

        }

    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> clazz) {
        return (T) beanRegistry.get(clazz);
    }

    public Map<Class<?>, Object> getControllerClasses() {
        return controllerClasses;
    }

    private void registerBean(Class<?> clazz) {
        try {
            Object bean = clazz.getDeclaredConstructor().newInstance();
            beanRegistry.put(clazz, bean);
            if (isController(clazz)) {
                controllerClasses.put(clazz, bean);
            }
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(edu.pnu.myspring.annotations.MyAutowired.class)) {
                    beansToAutowire.add(bean);
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register bean: " + clazz.getName(), e);
        }
    }

    private boolean isController(Class<?> clazz) {
        return hasAnnotationOrMetaAnnotation(clazz, edu.pnu.myspring.annotations.MyRestController.class);
    }

    private void processAutowiring() {
        for (Object bean : beansToAutowire) {
            for (java.lang.reflect.Field field : bean.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(edu.pnu.myspring.annotations.MyAutowired.class)) {
                    Class<?> dependency = field.getType();
                    Object dependencyBean = getBean(dependency);
                    if (dependencyBean != null) {
                        try {
                            field.setAccessible(true);
                            field.set(bean, dependencyBean);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Failed to autowire field: " + field.getName(), e);
                        }
                    } else {
                        throw new RuntimeException("Unsatisfied dependency for field: " + field.getName());
                    }
                }
            }
        }
    }

    private boolean hasAnnotationOrMetaAnnotation(Class<?> clazz, Class<? extends java.lang.annotation.Annotation> annotationType) {
        if (clazz.isAnnotationPresent(annotationType)) {
            return true;
        }
        for (java.lang.annotation.Annotation annotation : clazz.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(annotationType)) {
                return true;
            }
        }
        return false;
    }
}
