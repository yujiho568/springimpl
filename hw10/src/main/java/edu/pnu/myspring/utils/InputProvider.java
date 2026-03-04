package edu.pnu.myspring.utils;

import edu.pnu.myspring.dispatcher.UserRequest;

import java.util.Scanner;

public class InputProvider {
    private Scanner scanner = new Scanner(System.in);
    public UserRequest getInput() {
        System.out.println("\nSupported HTTP methods: POST, GET, PUT, DELETE, EXIT");
        System.out.print("Enter HTTP method: ");
        String method = scanner.nextLine().toUpperCase();
        if ("EXIT".equals(method)) {
            return new UserRequest("EXIT", "", "");
        }
        System.out.print("Enter URI (e.g., /students or /students/1): ");
        String uri = scanner.nextLine();
        String jsonBody = "";
        if ("POST".equals(method) || "PUT".equals(method)) {
            System.out.println("Enter JSON data (type END on a new line to finish):");
            StringBuilder jsonData = new StringBuilder();
            String line;
            while (!(line = scanner.nextLine()).equals("END")) {
                jsonData.append(line);
            }
            jsonBody = jsonData.toString();
        }
        return new UserRequest(method, uri, jsonBody);
    }
}

