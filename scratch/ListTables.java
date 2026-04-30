import java.sql.*;
import java.util.*;

public class ListTables {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/attendance?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";
        String user = "root"; // Assuming common default
        String password = ""; // Assuming common default
        
        // Try to load config if possible, but let's try defaults first or common ones
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getTables(null, null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                System.out.println(rs.getString(3));
            }
        } catch (Exception e) {
            System.err.println("Failed with default: " + e.getMessage());
            // Try reading config
            // ... (omitted for brevity, let's just use the tool's grep if needed)
        }
    }
}
