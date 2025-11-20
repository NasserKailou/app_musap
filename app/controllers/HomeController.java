package controllers;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;

import play.data.Form;
import play.data.FormFactory;
import play.db.Database;
import play.mvc.*;
import play.mvc.Http.Request;
import services.AdherentMainServices;
import services.ParamsServices;
import utils.Login;

/**
 * This controller contains an action to handle HTTP requests to the
 * application's home page.
 */

public class HomeController extends Controller {

	FormFactory formatFactory;
	AdherentMainServices consultationServices;
	ParamsServices paramsService;
	
	@Inject
	public HomeController(FormFactory formatFactory, AdherentMainServices consultationServices,ParamsServices paramsService) {

		this.formatFactory = formatFactory;
		this.consultationServices = consultationServices;
		this.paramsService = paramsService;
		

	}

	public Result index(Request request) {
		//return ok(views.html.login.render(request));
		return ok(views.html.index.render(paramsService.listesParams(),request));
	}


	public Result acceuil(Request request) {
		//System.out.println("les sessions sont login:" +request.session().get("login") +" droit :"+ request.session().get("droit") +" nonUser :" + request.session().get("nomUser"));
		//System.out.println("Con url:" +db.getUrl() +" Con Name :"+db.getName() +" Con other connexion:" + db.getConnection()+" Con others datasources" + db.getDataSource());
		
		if (request.session().get("login") == null) {
			return ok(views.html.index.render(paramsService.listesParams(),request));
		} else {
			
			// System.out.println("les elemensts sont :" + element);
			return ok(views.html.acceuil.render(request));
		}
	}
	/**
	 * Déconnexion d'un utilisateur
	 *
	 * @return
	 */
	public Result deconnecter(Request request) {	
		return redirect(controllers.routes.HomeController.index()).withNewSession().flashing("success", "vous avez été déconnecté");
	}

	/**
	 * @return
	 */
	public Result authentification(Request request) {

		Form<Login> forms = formatFactory.form(Login.class).bindFromRequest(request);
		if (forms.hasErrors()) {
			
			return redirect(controllers.routes.HomeController.index()).flashing("error", " Erreur de saisie");

		} else {
			Login login = forms.get();
			String log = formatFactory.form().bindFromRequest(request).get("login");
			String pass = formatFactory.form().bindFromRequest(request).get("pass_word");

			if (1 == 1) {
				
				return redirect(controllers.routes.HomeController.index()).flashing("error", "wrong email/password");
			} else {
				
				return redirect(controllers.routes.HomeController.acceuil()).addingToSession(request, "login", login.getLogin());
			}
		}
	}
}
