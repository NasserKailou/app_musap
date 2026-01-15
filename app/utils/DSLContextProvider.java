package utils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import play.Logger;
import play.db.Database;
import play.inject.ApplicationLifecycle;

import java.util.concurrent.CompletableFuture;

/**
@author nasser
 *
 * !must be singleton because of the lifecycle hook
 */
@Singleton
public class DSLContextProvider implements Provider<DSLContext> {

    private final Logger.ALogger logger = Logger.of(this.getClass());
    /**
     * Avoid caching Connection in ConnectionProvider
     * This practice has been seen a couple of times on the internet, in various sample projects, blog posts, etc.
     * It is generally not a good idea to cache the Connection in a `ConnectionProvider`, as the `ConnectionProvider`
     * should be stateless and threadsafe to be more generally useful, e.g. when injected via DI
     */

    private Database db;

    private DSLContext sqlContext;

    @Inject
    public DSLContextProvider(Database db, ApplicationLifecycle lifecycle) {
        this.db = db;
        this.sqlContext = DSL.using(db.getConnection(), SQLDialect.POSTGRES_9_5);

        //avoiding memory leaks
        //for that (too) this class must be a singleton
        lifecycle.addStopHook(() -> {
            sqlContext.close();
            db.shutdown(); //check if necessary TODO @Khalid
            return CompletableFuture.completedFuture(null);
        });
    }

    @Override
    public DSLContext get() {

        //shouldn't be null
        if (null == this.db) {
            logger.error("*********************DB connection is NULL!********SHOULD NOT BE NULL**********");
        }

        if (null == this.sqlContext) {
            ////shouldn't be null.  this.sqlContext = DSL.using(this.db.getConnection(), SQLDialect.POSTGRES_9_5);
            logger.error("*********************sqlContext connection is NULL!***SHOULD NOT BE NULL*******");

        }

        return this.sqlContext;
    }
}
