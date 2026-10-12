package controllers;

import java.util.Map;

import com.google.inject.Inject;

import dto.TransitionRetraite;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.RetraiteServices;
import utils.Permission;
import utils.Permissions;
import utils.Secured;

/**
 * Adhérents > Retraites : aperçu des changements de situation liés à l'âge
 * (fin de couverture, archivage des ayants droit des retraités, cotisations de
 * pré-retraite) puis application et historique.
 *
 * @author nasser
 */
@Security.Authenticated(Secured.class)
@Permission(Permissions.RETRAITE_GERER)
public class RetraiteCtrl extends Controller {

	private final RetraiteServices retraite;

	@Inject
	public RetraiteCtrl(RetraiteServices retraite) {
		this.retraite = retraite;
	}

	public Result index(Request request) {
		return ok(views.html.retraites.render(retraite.apercu(), retraite.journal(200), retraite.parametres(), request));
	}

	public Result appliquer(Request request) {
		Map<String, Integer> res = retraite.appliquer(request.session().get("login").orElse(""));
		int total = res.values().stream().mapToInt(Integer::intValue).sum();
		if (total == 0)
			return redirect(routes.RetraiteCtrl.index()).flashing("success", "Aucun changement à appliquer");
		return redirect(routes.RetraiteCtrl.index()).flashing("success", "Changements appliqués : "
				+ res.get(TransitionRetraite.FIN_COUVERTURE) + " fin(s) de couverture, "
				+ res.get(TransitionRetraite.ARCHIVAGE_AYANTS_DROIT) + " retraité(s) dont les ayants droit sont archivés, "
				+ res.get(TransitionRetraite.COTISATION) + " cotisation(s) mise(s) à jour");
	}
}
