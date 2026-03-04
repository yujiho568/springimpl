package edu.pnu.myspring.dispatcher;

import java.util.Objects;

public class UserRequest {
    private String method;
    private String uri;
    private String jsonBody;
    public UserRequest(String method, String uri, String jsonBody) {
        this.method = method;
        this.uri = uri;
        this.jsonBody = jsonBody;
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public String getJsonBody() {
        return jsonBody;
    }
    public void setJsonBody(String jsonBody) {
        this.jsonBody = jsonBody;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRequest that = (UserRequest) o;
        return Objects.equals(method, that.method) && Objects.equals(uri, that.uri) && Objects.equals(jsonBody, that.jsonBody);
    }
    @Override
    public int hashCode() {
        return Objects.hash(method, uri, jsonBody);
    }
    @Override
    public String toString() {
        return "UserRequest{" +
                "method='" + method + '\'' +
                ", uri='" + uri + '\'' +
                ", jsonBody='" + jsonBody + '\'' +
                '}';
    }
}
