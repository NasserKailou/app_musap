package controllers;

import com.google.inject.Inject;

import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.CotisationServices;
import utils.Permission;
import utils.Permissions;
import utils.Secured;

/**
 * Paramètrages > Taux de cotisation : taux par lien d'ayant droit, nombre
 * maximum pris en compte, contrôle des écarts et recalcul des adhérents.
 *
 * @author nasser
 */
@Security.Authenticated(Secured.class)
@Permission(Permissions.COTISATION_PARAMETRER)
public class CotisationCtrl extends Controller {

	private final CotisationServices cotisationServices;
	private final FormFactory formFactory;

	@Inject
	public CotisationCtrl(CotisationServices cotisationServices, FormFactory formFactory) {
		this.cotisationServices = cotisationServices;
		this.formFactory = formFactory;
	}

	public Result index(Request request) {
		return ok(views.html.cotisations.render(cotisationServices.listTaux(), cotisationServices.controler(), request));
	}

	public Result saveTaux(Request request) {
		DynamicForm f = formFactory.form().bindFromRequest(request);
		String res = cotisationServices.enregistrerTaux(f.get("code"), f.get("libelle"), parseDouble(f.get("taux")),
				parseInt(f.get("nombreMax")), parseInt(f.get("ordre")), request.session().get("login").orElse(""));
		if ("ok".equals(res))
			return redirect(routes.CotisationCtrl.index()).flashing("success", "Taux enregistré. Vérifiez les écarts"
					+ " puis appliquez le recalcul pour mettre à jour les adhérents.");
		return redirect(routes.CotisationCtrl.index()).flashing("error", res);
	}

	public Result deleteTaux(Request request) {
		String res = cotisationServices.supprimerTaux(formFactory.form().bindFromRequest(request).get("code"));
		if ("ok".equals(res))
			return redirect(routes.CotisationCtrl.index()).flashing("success", "Lien supprimé");
		return redirect(routes.CotisationCtrl.index()).flashing("error", res);
	}

	public Result recalculer(Request request) {
		int n = cotisationServices.recalculerTous();
		return redirect(routes.CotisationCtrl.index()).flashing("success",
				n + " adhérent(s) mis à jour avec les taux actuels");
	}

	private static Double parseDouble(String s) {
		try {
			return s == null || s.trim().isEmpty() ? null : Double.valueOf(s.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer parseInt(String s) {
		try {
			return s == null || s.trim().isEmpty() ? null : Integer.valueOf(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
