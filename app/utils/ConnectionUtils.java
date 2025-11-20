package utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 
 * @author nasser
 *cette classe nous sert d'outil de connection pour la génération de nos états
 *on l'appel depuis notre classe CallJasperReport pour passer la connexion a jasper
 */
public class ConnectionUtils {

	public static Connection getConnection() throws SQLException, ClassNotFoundException,IOException {

		// Using Oracle
		// You may be replaced by other Database.
		return PostgreSQLConnUtils.getPosgreSQLConnection();
	}

	//
	// Test Connection ...
	//
	public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {

		System.out.println("Get connection ... ");

		// Get a Connection object
		Connection conn = ConnectionUtils.getConnection();

		System.out.println("Get connection " + conn);

		System.out.println("Done!");
	}
}
