package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.inject.Singleton;

import services.DataSources;

/**
 * 
 * @author nasser cette classe nous sert d'outil de connection pour la
 *         génération de nos états
 */

public class PostgreSQLConnUtils {

	public static Connection getPosgreSQLConnection() throws SQLException, ClassNotFoundException {

		Connection conn = DriverManager.getConnection(DataSources.DB_URL, DataSources.DB_USER_NAME,
				DataSources.DB_PASSWORD);
		return conn;
	}
}
