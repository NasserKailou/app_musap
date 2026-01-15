/**
 * 
 */
package utils;

import com.typesafe.config.Config;
import play.mvc.Http.Request;
import services.ParamsServices;
import play.mvc.Result;
import play.mvc.Security;
import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Secured extends Security.Authenticator {
    public static final String SESSION_TIMEOUT_CONFIG_KEY = "application.session.timeout";
    public static final String USER_LAST_TIME = "userLastTime";
    public static final String USER_ID_FIELD = "login";

    private final long timeout;
    @Inject
    ParamsServices paramsService;
    
    @Inject
    public Secured(Config config) {
        super();
        if (config.hasPath(SESSION_TIMEOUT_CONFIG_KEY)) {
            timeout = config.getDuration(SESSION_TIMEOUT_CONFIG_KEY, TimeUnit.MILLISECONDS);
        } else {
            timeout = 0L;
        }
    }

    @Override
    public Optional<String> getUsername(Request ctx) {

        // check session expired
        if (timeout > 0L) {
        	String previousTick="";
        	if(ctx.session().getOptional(USER_LAST_TIME).isPresent())
        		previousTick = ctx.session().getOptional(USER_LAST_TIME).get();
        	
        	//String previousTick = lastTime.;
            if (previousTick != null && !previousTick.equals("")) {
                long previousT = Long.valueOf(previousTick);
                long currentT = new Date().getTime();
                if ((currentT - previousT) > timeout) {
                    // session expired
                    ctx.session().removing("email");
                    ctx.flash().adding("error", "La session a expiré après une inactivité de plus de :" + timeout / 1000 / 60 + " minutes.");

                    return ctx.session().get(USER_ID_FIELD);
                }
            }
            // update time in session
            String tickString = Long.toString(new Date().getTime());
            ctx.session().adding(USER_LAST_TIME, tickString);
           
        }
       // System.out.println("le login in session est :"+ctx.session().getOptional(USER_ID_FIELD).get());
        return ctx.session().getOptional(USER_ID_FIELD); //-this is naturaly :=) set by login
    }

    @Override
    public Result onUnauthorized(Request ctx) {
        return ok(views.html.index.render(paramsService.listesParams(),ctx))
        		.flashing("error", "La session a expiré après une inactivité de plus de :" + timeout / 1000 / 60 + " minutes.");
    }

}
    


