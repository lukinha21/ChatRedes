
package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:chatapp.db";

    public static Connection connect() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }

    public static void init() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS usuarios (nome TEXT, login TEXT, email TEXT, senha TEXT, status TEXT);");
            stmt.execute("CREATE TABLE IF NOT EXISTS aceite_privado (remetente TEXT, destinatario TEXT, PRIMARY KEY (remetente, destinatario));");
            stmt.execute("CREATE TABLE IF NOT EXISTS grupo_usuario (grupo TEXT, usuario TEXT, PRIMARY KEY (grupo, usuario));");
        } catch (SQLException e) {
            System.out.println("Erro ao criar o banco: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
