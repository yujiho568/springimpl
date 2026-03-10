package edu.pnu.myspring.test;

import edu.pnu.myspring.annotations.MySpringApplication;
import edu.pnu.myspring.boot.MySpringApplicationRunner;

@MySpringApplication
public class TestApplication {
    public static void main(String[] args) {
        MySpringApplicationRunner.run(TestApplication.class, args);
    }
}
