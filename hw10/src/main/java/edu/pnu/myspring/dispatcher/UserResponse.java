package edu.pnu.myspring.dispatcher;

public class UserResponse<T> {

    private T data;

    private String message;

    private boolean success;



    public UserResponse(T data, String message, boolean success) {

        this.data = data;

        this.message = message;

        this.success = success;

    }



    public static <T> UserResponse<T> success(T data) {

        return new UserResponse<>(data, "Success", true);

    }



    public static <T> UserResponse<T> error(String message) {

        return new UserResponse<>(null, message, false);

    }

}

