package org.example.server;

import org.json.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ServerController {
    JSONObject obj;

    // Constructor to initialize ServerController with JSONObject
    public ServerController(JSONObject obj) {
        this.obj = obj;
    }

    // Method to handle login logic
    private JSONObject login(JSONObject obj) throws SQLException, ClassNotFoundException {
        // Initialize database connection
        DatabaseConnection dbConnection = new DatabaseConnection();

        // Extract username and email from tokens
        JSONArray tokens = obj.getJSONArray("tokens");
        String username = tokens.get(1).toString();
        String email = tokens.get(2).toString();

        // Prepare client response JSON object
        JSONObject clientResponse = new JSONObject();
        clientResponse.put("command", "login");
        clientResponse.put("username", username);
        clientResponse.put("email", email);

        // Query participant table for matching username and email
        String readParticipantQuery = "SELECT * FROM participant";
        ResultSet participantResultSet = dbConnection.read(readParticipantQuery);
        while (participantResultSet.next()) {
            if (username.equals(participantResultSet.getString("username")) &&
                    email.equals(participantResultSet.getString("emailAddress"))) {
                // Matching participant found
                String registration_number = participantResultSet.getString("registration_number");

                // Populate client response for participant
                clientResponse.put("participant_id", participantResultSet.getInt("participant_id"));
                clientResponse.put("registration_number", registration_number);
                clientResponse.put("schoolName", "undefined");
                clientResponse.put("isStudent", true);
                clientResponse.put("isAuthenticated", true);
                clientResponse.put("status", true);

                return clientResponse;
            }
        }

        // Query school table for matching representative username and email
        String readRepresentativeQuery = "SELECT * FROM schools";
        ResultSet representativeResultSet = dbConnection.read(readRepresentativeQuery);
        while (representativeResultSet.next()) {
            if (username.equals(representativeResultSet.getString("representative_name")) &&
                    email.equals(representativeResultSet.getString("representative_email"))) {
                // Matching representative found
                String schoolName = representativeResultSet.getString("name");
                String registration_number = representativeResultSet.getString("registration_number");

                // Populate client response for representative
                clientResponse.put("participant_id", 0);
                clientResponse.put("schoolName", schoolName);
                clientResponse.put("registration_number", registration_number);
                clientResponse.put("isStudent", false);
                clientResponse.put("isAuthenticated", true);
                clientResponse.put("status", true);

                return clientResponse;
            }
        }

        // No matching credentials found
        clientResponse.put("isStudent", false);
        clientResponse.put("isAuthenticated", false);
        clientResponse.put("status", false);
        clientResponse.put("reason", "Invalid credentials. Check the details provided");

        return clientResponse;
    }

    // Method to handle student registration logic
    private JSONObject register(JSONObject obj) throws IOException, MessagingException, SQLException, ClassNotFoundException {
        // Initialize email agent and database connection
        EmailSending emailAgent = new EmailSending();
        DatabaseConnection dbConnection = new DatabaseConnection();

        // Extract registration details from tokens
        JSONArray tokens = obj.getJSONArray("tokens");
        JSONObject participantObj = new JSONObject();
        participantObj.put("username", tokens.get(1));
        participantObj.put("firstname", tokens.get(2));
        participantObj.put("lastname", tokens.get(3));
        participantObj.put("emailAddress", tokens.get(4));
        participantObj.put("dob", tokens.get(5));
        participantObj.put("registration_number", tokens.get(6));
        participantObj.put("imagePath", tokens.get(7));

        // Prepare client response JSON object
        JSONObject clientResponse = new JSONObject();
        clientResponse.put("command", "register");

        // Query representative table to get representative email
        ResultSet rs = dbConnection.getRepresentative(participantObj.getString("registration_number"));
        String representativeEmail;
        if (rs.next()) {
            representativeEmail = rs.getString("representative_email");
        } else {
            // If no representative found for given regNo
            clientResponse.put("status", false);
            clientResponse.put("reason", "School does not exist in our database");

            return clientResponse;
        }

        // Initialize file storage for participants
        FileStorage fileStorage = new FileStorage("participantsfile.json");
        if (!fileStorage.read().toString().contains(participantObj.toString())) {
            // Add participant if not already exists
            fileStorage.add(participantObj);
            clientResponse.put("status", true);
            clientResponse.put("reason", "Participant created successfully awaiting School representative approval");
            // Send registration request email to representative
            emailAgent.sendParticipantRegistrationRequestEmail(representativeEmail, participantObj.getString("emailAddress"), participantObj.getString("username"));

            return clientResponse;
        }

        // Participant already exists
        clientResponse.put("status", false);
        clientResponse.put("reason", "Participant creation failed. An existing participant object found");

        return clientResponse;
    }


    // Main method to run appropriate logic based on command received
    public JSONObject run() throws IOException, SQLException, ClassNotFoundException, MessagingException {
        switch (this.obj.get("command").toString()) {
            case "login":
                // Call login logic
                return this.login(this.obj);

            case "register":
                // Call registration logic
                return this.register(this.obj);

            default:
                // Unrecognized command
                JSONObject outputObj = new JSONObject();
                outputObj.put("command", "exception");
                outputObj.put("reason", "Invalid command");

                return outputObj;
        }
    }
}

