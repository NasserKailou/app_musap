package utils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.jooq.DSLContext;

/**
 * @author kailou
 * encapsulation de ConnectionHelperAppImp en le dérivant cette fois de {@link DSLContextProvider}
 * plutôt que import java.sql.Connection comme cela a été fait du côté de cegib.expenditure.models
 * Lors de la fabrication du jar model il faut exclure les classes de connextion dont celle portant le même nom
 * que celle-ci, de sorte que ce soit celle-ci qui soit injectée dans les services de expenditure-model
 *
 * mais les modules de expenditure.model utilisent principalement ConnexionHelper (cette classe) qui est en fait une
 * encapsulation de DSLContext sous le nom de connection. voir aussi @ConnexionHelper
 */

public class ConnectionHelperAppImp implements Provider<IConnectionHelper> {
    @Inject
    private DSLContextProvider dslContextProvider;
    private ConnectionProvider pro;

    @Inject
    public ConnectionHelperAppImp(ConnectionProvider pro) {
        //on a rien à faire avec pro car on renvoie dslContextProvider.get mais cela est fait pour
        // garder la même signature que celle adoptée dans models
        this.pro = pro;
    }

    public DSLContext connection() {
        return   dslContextProvider.get();
    }

    @Override
    public IConnectionHelper get() {
        return this::connection;
    }
}
