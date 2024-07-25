package org.example.server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;

public class DatabaseConnection {
    // Database connection parameters
    String url = "jdbc:mysql://localhost:3306/mtc_challenge_comp20";
    String username = "root";
    String password = "";
    Connection connection;
    Statement statement;

    // Constructor to establish database connection
    public DatabaseConnection() throws SQLException, ClassNotFoundException {
        // Load MySQL JDBC driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Connect to the database
        this.connection = DriverManager.getConnection(this.url, this.username, this.password);
        this.statement = connection.createStatement();
    }

    // Method to execute a create SQL command
    public void create(String sqlCommand) throws SQLException {
        this.statement.execute(sqlCommand);
    }

    // Method to execute a read SQL command and return ResultSet
    public ResultSet read(String sqlCommand) throws SQLException {
        return this.statement.executeQuery(sqlCommand);
    }

    // Method to execute an update SQL command
    public void update(String sqlCommand) throws SQLException {
        this.statement.execute(sqlCommand);
    }

    // Method to execute a delete SQL command
    public void delete(String sqlCommand) throws SQLException {
        this.statement.execute(sqlCommand);
    }

    // Method to close database connection and statement
    public void close() throws SQLException {
        if (this.statement != null) this.statement.close();
        if (this.connection != null) this.connection.close();
    }

    // Method to create a participant entry in the database
    public void createParticipant(String username, String firstname, String lastname, String emailAddress, String dob, String registration_number, String imagePath) throws SQLException {
        String sql = "INSERT INTO `participant` (`username`, `firstname`, `lastname`, `emailAddress`, `dob`, `registration_number`, `imagePath`) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, firstname);
            stmt.setString(3, lastname);
            stmt.setString(4, emailAddress);
            stmt.setString(5, dob);
            stmt.setString(6, registration_number);
            stmt.setString(7, imagePath);
            stmt.executeUpdate();
        }
    }

    public ResultSet getRejectedParticipant(String username, String email, String registrationNumber) throws SQLException {
        String query = "SELECT * FROM rejectedparticipant WHERE username = ? AND emailAddress = ? AND registration_number = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, username);
        pstmt.setString(2, email);
        pstmt.setString(3, registrationNumber);
        return pstmt.executeQuery();
    }


    // Method to retrieve representative details from the database by registration number
    public ResultSet getRepresentative(String registration_number) throws SQLException {
        String sqlCommand = "SELECT * FROM `schools` WHERE registration_number = " + registration_number + ";";
        return this.statement.executeQuery(sqlCommand);
    }
}

