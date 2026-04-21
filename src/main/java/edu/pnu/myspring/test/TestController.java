package edu.pnu.myspring.test;

import edu.pnu.myspring.annotations.MyRestController;
import edu.pnu.myspring.annotations.MyPathVariable;
import edu.pnu.myspring.annotations.MyRequestMapping;

@MyRestController
public class TestController {

    @MyRequestMapping(value = "/hello", method = "GET")
    public String hello() {
        return "Hello, MySpring World!";
    }

    @MyRequestMapping(value = "/echo", method = "POST")
    public String echo(@MyPathVariable("id") Integer id, @MyPathVariable("body") String body) {
        return "You sent: " + id + " - " + body;
    }
}
