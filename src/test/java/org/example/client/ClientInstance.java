package org.example.client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ClientInstance {
    // Define attributes for the ClientInstance object
    String hostname;
    int port;
    String clientId;
    User user;
    byte cache;
    boolean isStudent;
    boolean isAuthenticated;

    // Constructor to initialize ClientInstance with hostname, port, and user
    public ClientInstance(String hostname, int port, User user) {
        this.hostname = hostname;
        this.port = port;
        this.user = user;
    }

    // Method to validate if the input string is a valid JSON object
    public static boolean isValid(String input) {
        String regex = "^\\{.*\\}$";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

        return pattern.matcher(input).matches();
    }

    // Method to start the client instance and interact with the server
    public void start() throws IOException {
        // Execute code for interacting with the server
        try (
                Socket socket = new Socket(hostname, port);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
        ) {
            // Initialize client ID and serializer
            this.clientId = (String) socket.getInetAddress().getHostAddress();
            Serializer serializer = new Serializer(this.user);

            System.out.println();
            System.out.print("-------------------COMMANDS TO BE ENTERED---------------------------");
            System.out.println("\nregister username firstname lastname email_address d0b(yyyy-mm-dd) RegNo image_path" +
                    "\nlogin\nviewApplicants(confirm yes <username>/confirm no <username>)\nviewChallenges\nattemptChallenges(attemptChallenge <challengeNo>)\nlogout");
            System.out.println("--------------------------------------------------------------------------");

            // Prompt user for a command
            System.out.print("[Enter the command] (" + this.user.username + "): ");

            // Continuously read from the console and send to the server
            ClientController clientController = new ClientController(user);
            String regex = "^\\{.*\\}$";
            Pattern pattern = Pattern.compile(regex);

            String userInput;
            while ((userInput = consoleInput.readLine()) != null) {
                // Handle logout command
                if (userInput.equals("logout") && (this.user.isAuthenticated)) {
                    System.out.println("✓✓ Session successfully logged out");
                    this.user.logout();
                    System.out.print("[Enter the command] (" + (!this.user.username.isBlank() ? this.user.username : null) + "): ");
                    continue;
                }

                // Serialize the user input command
                String serializedCommand = serializer.serialize(userInput);

                // Check if the serialized command is valid
                if (isValid(serializedCommand)) {
                    // Send the command to the server
                    output.println(serializedCommand);

                    // Read response from the server
                    String response = input.readLine();

                    // Execute the response using the client controller
                    this.user = clientController.exec(response);

                    // Check if the user's output is a valid JSON object
                    if (!pattern.matcher(this.user.output).matches()) {
                        System.out.println("\n" + user.output + "\n");
                    } else {
                        // Parse questions from the user's output and collect answers
                        JSONObject questions = new JSONObject(this.user.output);
//
                    }
                } else {
                    // Print invalid serialized command
                    System.out.println(serializedCommand);
                }
                // Prompt for the next instruction
                System.out.print("[Enter the command] (" + this.user.username + "): ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Print connection timeout message
            System.out.println("Connection with the server timeout");
        }
    }
}

