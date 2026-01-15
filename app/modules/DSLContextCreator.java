package modules;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import play.Logger;
import play.db.Database;
import play.inject.ApplicationLifecycle;

import java.sql.Connection;
import java.sql.SQLClientInfoException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * @author khalid.alkassoum@advinteck.com
 * DSLContextCreator for DSLContextProvider
 * 20171010 - v0.1
 */
//@Singleton -- CAN'T BE A SINGLETON NO MORE
public class DSLContextCreator implements IDSLContextCreator {
    private final Logger.ALogger logger = Logger.of(this.getClass());
    /**
     * Avoid caching Connection in ConnectionProvider
     * This practice has been seen a couple of times on the internet, in various sample projects, blog posts, etc.
     * It is generally not a good idea to cache the Connection in a `ConnectionProvider`, as the `ConnectionProvider`
     * should be stateless and threadsafe to be more generally useful, e.g. when injected via DI
     */

    private Database db;
    private Connection con;
    private DSLContext sqlContext;

    @Inject
    //public DSLContextCreator(Database db, ApplicationLifecycle lifecycle, @Assisted String username){
    public DSLContextCreator(Database db) {
        this.db = db;
        //this.con = db.getConnection();

    }

    @Override
    public DSLContext create(String username) {
        try {
            //String username_ = "cegib_admin";
            this.con = db.getDataSource().getConnection(username, "admin");
            //this.con = db.getConnection();
            //this.con.prepareStatement("set role to 'cegib_admin'").execute();


            Properties p = this.con.getClientInfo(); //new Properties();
            p.put("ApplicationName", "INS-APP");
            p.put("ClientUser", username);
            this.con.setClientInfo(p);
            System.out.println(this.con.getClientInfo().toString());
           
            System.out.println(username + "-----------------------------------------------------------------------------");

            this.sqlContext = DSL.using(this.con, SQLDialect.POSTGRES_9_5);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            logger.error("*********************DB connection ERROR in create !****************************");
        }
        if (null == this.sqlContext) {
            ////shouldn't be null.  this.sqlContext = DSL.using(this.db.getConnection(), SQLDialect.POSTGRES_9_5);
            logger.error("*********************sqlContext connection is NULL!****************************");
        }

        return this.sqlContext;
    }

}
