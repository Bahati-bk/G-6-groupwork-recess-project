package org.example.client;

import org.json.JSONObject;

public class ClientHandler {
    User user;

    // Constructor to initialize the ClientController with a User object
    public ClientHandler(User user) {
        this.user = user;
    }

    // Method to handle login logic based on server response
    private User login(JSONObject response) {
        // If the login is successful
        if (response.getBoolean("status")) {
            // Update user attributes based on the response
            this.user.id = response.getInt("participant_id");
            this.user.username = response.getString("username");
            this.user.email = response.getString("email");
            this.user.registration_number = response.getString("registration_number");
            this.user.schoolName = response.getString("schoolName");
            this.user.isStudent = response.getBoolean("isStudent");
            this.user.isAuthenticated = response.getBoolean("isAuthenticated");

            // Set a success message for the user
            this.user.output = "✓✓ Successfully logged in as a " + this.user.username + (this.user.isStudent ? "(Student)" : "(School Representative)");
        } else {
            // Set a failure message for the user
            this.user.output = "!! " + response.get("reason").toString();
        }
        return this.user;
    }

    // Method to handle registration logic based on server response
    private User register(JSONObject response) {
        // If the registration is successful
        if (response.getBoolean("status")) {
            // Set a success message for the user
            this.user.output = "✓✓ " + response.get("reason").toString();
        } else {
            // Set a failure message for the user
            this.user.output = "!! " + response.get("reason").toString();
        }
        return this.user;
    }


    // Main method to execute the appropriate action based on the command in the response data
    public User exec(String responseData) {
        JSONObject response = new JSONObject(responseData);
        switch (response.get("command").toString()) {
            case "login":
                return this.login(response);

            case "register":
                return this.register(response);

            default:throw new IllegalStateException("Invalid response");
        }
    }
}

