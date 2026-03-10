package edu.pnu.myspring.utils;

import edu.pnu.myspring.dispatcher.UserRequest;

import java.util.Scanner;

public class InputProvider {
    private Scanner scanner = new Scanner(System.in);
    public UserRequest getInput() {
        System.out.println("\nSupported HTTP methods: POST, GET, PUT, DELETE, EXIT");
        System.out.print("Enter HTTP method: ");
        if (!scanner.hasNextLine()) {
            return null;
        }
        String method = scanner.nextLine().trim().toUpperCase();
        if (method.isEmpty()) {
            return new UserRequest("CONTINUE", "", ""); // Dummy method to loop back
        }
        if ("EXIT".equals(method)) {
            return new UserRequest("EXIT", "", "");
        }
        
        System.out.print("Enter URI (e.g., /students or /students/1): ");
        if (!scanner.hasNextLine()) {
            return new UserRequest("EXIT", "", "");
        }
        String uri = scanner.nextLine().trim();
        
        String jsonBody = "";
        if ("POST".equals(method) || "PUT".equals(method)) {
            System.out.println("Enter JSON data (type END on a new line to finish):");
            StringBuilder jsonData = new StringBuilder();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if ("END".equalsIgnoreCase(line.trim())) {
                    break;
                }
                jsonData.append(line);
            }
            jsonBody = jsonData.toString();
        }
        return new UserRequest(method, uri, jsonBody);
    }
}
