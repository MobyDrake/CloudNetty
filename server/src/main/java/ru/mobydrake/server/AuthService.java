package ru.mobydrake.server;


import java.sql.*;

public class AuthService {
    private static Connection connection;
    private static Statement statement;


    public static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:server/Users_db.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean auth(String login, String password) {
        String sql = String.format("SELECT * FROM users where login = '%s' and password = '%s'", login, password);
        try {
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
