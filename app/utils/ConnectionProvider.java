package utils;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.exception.DataAccessException;
import play.Logger;

/**
 * @author khalid.alkassoum@advinteck.com
 * encapsulation de ConnectionProvider en le dérivant cette fois de {@link DSLContextProvider}
 * plutôt que import java.sql.Connection comme cela a été fait du côté de cegib.expenditure.models
 * Lors de la fabrication du jar model il faut exclure les classes de connextion dont celle portant le même nom
 * que celle-ci, de sorte que ce soit celle-ci qui soit injectée dans les services de expenditure-model
 *
 * mais les modules de expenditure.model utilisent principalement ConnexionHelper qui est en fait une
 * encapsulation de DSLContext sous le nom de connection. voir aussi @ConnexionHelper
 */

@Singleton
public class ConnectionProvider implements org.jooq.ConnectionProvider {
    private final Logger.ALogger logger = Logger.of(this.getClass());

    Connection connection = null;
    private final DSLContextProvider dslContextProvider;

    @Inject
    public ConnectionProvider(DSLContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }


    @Override
    public Connection acquire() throws DataAccessException {
        if (connection == null) {
            connection = this.dslContextProvider.get().configuration().connectionProvider().acquire();
        }
        return this.connection;
    }

    @Override
    public void release(Connection released) throws DataAccessException {
        if (this.connection != released) {
            throw new IllegalArgumentException("Connexion attendue " + this.connection + ", or la connexion obtenue est:" + released);
        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error("Erreur lors de la fermeture de la connexion:" + connection + " message d'erreur" + e.getSQLState());
        }
    }
}
