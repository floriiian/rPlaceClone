package org.florian.rplace.db;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class Database {

    private static Connection CONNECTION =  null;
    private static final Logger LOGGER = LogManager.getLogger();


    public static void main() {

        /* Creates the database itself and with that also the table that holds all canvas's */

        try {
            Class.forName("org.sqlite.JDBC");
            CONNECTION = DriverManager.getConnection("jdbc:sqlite:rplace.db");

            LOGGER.debug("Connection to database initialized.");

            Statement stmt = CONNECTION.createStatement();

            String createStorage =
                    "CREATE TABLE IF NOT EXISTS canvasStorage" + "(id INTEGER PRIMARY KEY AUTOINCREMENT," + "canvas_code TEXT NOT NULL)";
            stmt.executeUpdate(createStorage);
            stmt.close();

            String createPixelStorage = "CREATE TABLE IF NOT EXISTS pixelStorage (" +
                    "pixel_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "canvas_code TEXT NOT NULL, " +
                    "x INT NOT NULL, " +
                    "y INT NOT NULL, " +
                    "color TEXT NOT NULL, " +
                    "userid TEXT NOT NULL, " +
                    "FOREIGN KEY (canvas_code) REFERENCES canvasStorage(canvas_code))";

            stmt.executeUpdate(createPixelStorage);
            stmt.close();

            addCanvasToDatabase("BR12N");
            addPixelToCanvas("BR12N", 2, 2, "#21124F", "asa");

        }
        catch(Exception e) {
            LOGGER.debug(e);
        }
    }

    public static boolean addCanvasToDatabase(String canvasCode) {
        try {
            Statement stmt = CONNECTION.createStatement();

            String newCanvas = "INSERT INTO canvasStorage (canvas_code) VALUES ('" + canvasCode + "')";

            stmt.executeUpdate(newCanvas);
            stmt.close();
        }
        catch (Exception e) {
            LOGGER.debug(e);
            return false;
        }
        LOGGER.debug("Canvas with code: {} has been successfully added.", canvasCode);
        return true;
    }

    public static boolean addPixelToCanvas(String canvasCode, Integer x, Integer y, String color, String userId) {

        try {
            Statement stmt = CONNECTION.createStatement();

            String checkPixelExistence = "SELECT pixel_id FROM pixelStorage WHERE canvas_code = " + "'" + canvasCode + "'" +" AND x = " + x + " AND y = " + y;
            ResultSet resultSet = stmt.executeQuery(checkPixelExistence);


            if (resultSet.next()) {
                String pixelUpdate = "UPDATE pixelStorage SET color = '" + color + "', userid = '" + userId + "' " +
                        "WHERE canvas_code = " + "'" + canvasCode + "'" + " AND x = " + x + " AND y = " + y;
                stmt.executeUpdate(pixelUpdate);
                LOGGER.debug("Pixel has been updated.");
            }
            else {
                String insertNewPixel = "INSERT INTO pixelStorage (canvas_code, x, y, color, userid) " +
                        "VALUES (" + "'" +canvasCode + "'" + ", " + x + ", " + y + ", '" + color + "', '" + userId + "')";
                stmt.executeUpdate(insertNewPixel);
                LOGGER.debug("Pixel has been added.");
            }

            stmt.close();
            resultSet.close();
        }
        catch (Exception e) {
            LOGGER.debug(e);
            return false;
        }
        return true;
    }

}
