package controllers;

import com.google.inject.Inject;

import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import dto.ParametreRetraite;
import services.CotisationServices;
import services.RetraiteServices;
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
	private final RetraiteServices retraite;

	@Inject
	public CotisationCtrl(CotisationServices cotisationServices, FormFactory formFactory, RetraiteServices retraite) {
		this.cotisationServices = cotisationServices;
		this.formFactory = formFactory;
		this.retraite = retraite;
	}

	public Result index(Request request) {
		return ok(views.html.cotisations.render(cotisationServices.listTaux(), cotisationServices.controler(),
				cotisationServices.parametresRetraite(), request));
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

	public Result saveRetraite(Request request) {
		DynamicForm f = formFactory.form().bindFromRequest(request);
		ParametreRetraite p = new ParametreRetraite();
		p.setAgePreRetraite(parseInt(f.get("agePreRetraite")));
		p.setAgeRetraite(parseInt(f.get("ageRetraite")));
		p.setAgeFinCouverture(parseInt(f.get("ageFinCouverture")));
		Integer plafond = parseInt(f.get("plafondRetraite"));
		Integer seuil = parseInt(f.get("seuilAlerteRetraite"));
		p.setPlafondRetraite(plafond == null ? null : plafond.longValue());
		p.setSeuilAlerteRetraite(seuil == null ? null : seuil.longValue());
		String res = cotisationServices.enregistrerParametresRetraite(p, request.session().get("login").orElse(""));
		retraite.viderCache();
		if ("ok".equals(res))
			return redirect(routes.CotisationCtrl.index()).flashing("success",
					"Règles de retraite enregistrées. Appliquez les changements depuis Adhérents > Retraites.");
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
