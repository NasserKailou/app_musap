package utils;

//import api.ILoginService;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import modules.IConnectionHelper;

import javax.inject.Provider;

import org.jooq.DSLContext;

public class AppModule extends AbstractModule {
	@Override
	public void configure() {

		// bind(OnStartUp.class).asEagerSingleton();
		bind(DSLContext.class).toProvider(DSLContextProvider.class).in(Scopes.SINGLETON);
		bind(utils.IConnectionHelper.class).toProvider(ConnectionHelperAppImp.class);
		bind(utils.ConnectionProvider.class);
		// rôles & permissions chargés au démarrage (utilisés par les menus des vues)
		bind(services.RoleServices.class).asEagerSingleton();
		// bind(controllers.util.Helpers.class).asEagerSingleton();

	}

}
