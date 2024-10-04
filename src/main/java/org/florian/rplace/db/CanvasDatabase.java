package org.florian.rplace.db;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.florian.rplace.Main;
import org.florian.rplace.session.CanvasSession;

import java.io.*;
import java.sql.*;
import java.util.Arrays;

public class CanvasDatabase {

    private static Connection CONNECTION =  null;
    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean initiateDatabase(){
        try {
            Class.forName("org.sqlite.JDBC");
            CONNECTION = DriverManager.getConnection("jdbc:sqlite:rplace.db");

            LOGGER.debug("Connection to database initialized.");

            Statement stmt = CONNECTION.createStatement();

            String createStorage =
                    "CREATE TABLE IF NOT EXISTS canvasStorage" + "(id INTEGER PRIMARY KEY AUTOINCREMENT," + "canvas_code TEXT NOT NULL," + "canvas_data BLOB NOT NULL" +")";
            stmt.executeUpdate(createStorage);
            stmt.close();
            return true;
        }
        catch(Exception e) {
            LOGGER.debug(e);
            return false;
        }
    }

    public static boolean addCanvasToDatabase(CanvasSession session) throws IOException {

        String canvasCode = session.canvasCode;
        byte [] canvasData = getCanvasDataAsBytes(session);

        try {
            Statement stmt = CONNECTION.createStatement();

            String newCanvas = "INSERT INTO canvasStorage (canvas_code, canvas_data) VALUES ('" + canvasCode + "', " + Arrays.toString(canvasData) +  ")";

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

    public static void  removeCanvasFromDatabase(String canvasCode){
        try{
            Statement stmt = CONNECTION.createStatement();
            String deleteCanvas = "DELETE FROM canvasStorage WHERE canvas_code = '" + canvasCode + "'";

            stmt.executeUpdate(deleteCanvas);
            stmt.close();
        }
        catch (Exception e){
            LOGGER.debug(e);
        }
    }

    public static void backupCanvasData() throws IOException {

        for(CanvasSession session : Main.ACTIVE_CANVAS_SESSIONS){
            try{
                String canvasCode = session.canvasCode;
                byte [] canvasData = getCanvasDataAsBytes(session);

                Statement stmt = CONNECTION.createStatement();
                ResultSet rs = stmt.executeQuery( "SELECT id FROM canvasStorage WHERE canvas_code =" + "'"  + canvasCode + "'" + ")";

                if ( rs.next() ) {
                    String newCanvas = "UPDATE canvasStorage WHERE cavas_code =" + "'"+canvasCode+"'"+"VALUES ('" + canvasData + "')";
                    stmt.executeUpdate(newCanvas);
                    stmt.close();
                }
                else{
                    LOGGER.debug("{} no longer exists.", canvasCode);
                }
                stmt.close();
            }
            catch (Exception e){
                LOGGER.debug(e);
            }
        }
    }

    public static byte[] getCanvasDataAsBytes(CanvasSession session) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

        objectOutputStream.writeObject(session);
        objectOutputStream.flush();
        objectOutputStream.close();

        return  byteArrayOutputStream.toByteArray();
    }

    public static CanvasSession getCanvasDataFromBytes(byte[] canvasBytes) throws IOException, ClassNotFoundException {

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(canvasBytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        return (CanvasSession) objectInputStream.readObject();
    }

    public static byte[] getCanvasBytesFromDatabase(String canvasCode){
        try{
            String untreatedString;
            byte [] canvasData;

            Statement stmt = CONNECTION.createStatement();
            ResultSet rs = stmt.executeQuery( "SELECT canvas_data FROM canvasStorage WHERE canvas_code =" + "'"  + canvasCode + "'" + ")";

            if ( rs.next() ) {
                untreatedString = rs.getString("canvas_code");
                stmt.close();

            return untreatedString.getBytes();
            }
            else{
                LOGGER.debug("{} no longer exists.", canvasCode);
            }
            stmt.close();
        }
        catch (Exception e){
            LOGGER.debug(e);
        }
        return null;
    }

}
