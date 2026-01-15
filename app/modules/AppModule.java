package modules;

//import api.ILoginService;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.jooq.DSLContext;

public class AppModule extends AbstractModule {
    @Override
    public void configure() {
    
       // bind(OnStartUp.class).asEagerSingleton();
        bind(DSLContext.class).toProvider(DSLContextProvider.class).in(Scopes.SINGLETON);
        bind(IConnectionHelper.class).toProvider(ConnectionHelperAppImp.class);
        bind(modules.ConnectionProvider.class);
       // bind(controllers.util.Helpers.class).asEagerSingleton();

    
    }

}
