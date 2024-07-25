package org.example.server;

import org.json.*;

import javax.mail.MessagingException;
import java.io.FileOutputStream;
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
        String username = tokens.getString(1);
        String email = tokens.getString(2);

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
        participantObj.put("username", tokens.getString(1));
        participantObj.put("firstname", tokens.getString(2));
        participantObj.put("lastname", tokens.getString(3));
        participantObj.put("emailAddress", tokens.getString(4));
        participantObj.put("dob", tokens.getString(5));
        participantObj.put("registration_number", tokens.getString(6));
        participantObj.put("imagePath", tokens.getString(7));
        participantObj.put("tokenized_image", obj.getJSONObject("tokenized_image"));

        // Prepare client response JSON object
        JSONObject clientResponse = new JSONObject();
        clientResponse.put("command", "register");

        // Check if the participant has been rejected before
        ResultSet rejectedParticipant = dbConnection.getRejectedParticipant(
                participantObj.getString("username"),
                participantObj.getString("emailAddress"),
                participantObj.getString("registration_number")
        );

        if (rejectedParticipant.next()) {
            // Participant has been rejected before
            clientResponse.put("status", false);
            clientResponse.put("reason", "You have been previously rejected from registering under this school. Registration denied.");
            return clientResponse;
        }

        // Query representative table to get representative email
        ResultSet rs = dbConnection.getRepresentative(participantObj.getString("registration_number"));
        String representativeEmail;
        if (rs.next()) {
            representativeEmail = rs.getString("representative_email");
        } else {
            // If no representative found for given registration number
            clientResponse.put("status", false);
            clientResponse.put("reason", "The school registration number does not match registered school numbers");
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

    // Method for storing images
    private static void saveProfileImage(JSONObject s, String pic_path) {
        try (FileOutputStream fileOutputStream = new FileOutputStream("C:\\xampp\\htdocs\\G_6_RECESS-2\\public\\light-bootstrap\\img\\" + pic_path)) {
            // Debug: Print the keys in the JSON object
            System.out.println("Keys in JSON object: " + s.keys());

            // Check if "data" key exists
            if (!s.has("data")) {
                System.out.println("Error: JSON object does not contain 'data' key");
                System.out.println("JSON content: " + s.toString());
                return;
            }

            // Extract the "data" array
            JSONArray arr = s.getJSONArray("data");

            // Iterate over the array and save each buffer
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                // Check if "buffer" and "size" keys exist in the object
                if (!o.has("buffer") || !o.has("size")) {
                    System.out.println("Error: Object at index " + i + " is missing 'buffer' or 'size' key");
                    continue;
                }

                // Convert JSON array to byte array and write to file
                byte[] buffer = jsonArrayToBytes(o.getJSONArray("buffer"));
                fileOutputStream.write(buffer, 0, o.getInt("size"));
            }

            System.out.println("File saved as " + pic_path);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            System.out.println("JSON parsing error: " + e.getMessage());
            System.out.println("JSON content: " + s.toString());
            e.printStackTrace();
        }
    }

    // Helper method to convert JSON array to byte array
    public static byte[] jsonArrayToBytes(JSONArray array) {
        byte[] bytes = new byte[array.length()];
        for (int i = 0; i < array.length(); i++) {
            bytes[i] = (byte) array.getInt(i);
        }
        return bytes;
    }

    // Main method to run appropriate logic based on command received
    public JSONObject run() throws IOException, SQLException, ClassNotFoundException, MessagingException {
        // Switch based on the command received in the JSON object
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
