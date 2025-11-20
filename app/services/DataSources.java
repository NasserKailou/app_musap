/**
 * 
 */
package services;

import java.util.Properties;

import javax.sql.DataSource;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 
 * @author nasser
 *
 */
public class DataSources extends AbstractModule {

	public static final DataSource DATA_SOURCE;
	public static final String DB_URL;
	public static final String DB_USER_NAME;
	public static final String DB_PASSWORD;
	public static final String DB_DRIVER;

	@Provides
	@Singleton
	DataSource provideDataSource() {
		return DATA_SOURCE;
	}

	@Override
	protected void configure() {

	}

	static {
		try {
			Properties p = new Properties();
			p.load(DataSources.class.getResourceAsStream("/jooq-persistence.properties"));
			HikariDataSource dataSource = new HikariDataSource();
			dataSource.setDriverClassName(p.getProperty("db.driver"));
			DB_DRIVER = p.getProperty("db.driver");
			dataSource.setJdbcUrl(p.getProperty("db.url"));
			DB_URL = p.getProperty("db.url");
			dataSource.setUsername(p.getProperty("db.username"));
			DB_USER_NAME = p.getProperty("db.username");
			dataSource.setPassword(p.getProperty("db.password"));
			DB_PASSWORD = p.getProperty("db.password");
			dataSource.setMaximumPoolSize(5);

			DATA_SOURCE = dataSource;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
