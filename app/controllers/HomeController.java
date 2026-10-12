package controllers;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;

import play.data.Form;
import play.data.FormFactory;
import play.db.Database;
import play.mvc.*;
import play.mvc.Http.Request;
import services.AdherentMainServices;
import services.DashboardServices;
import services.ParamsServices;
import services.ReglementMainServices;
import services.StructureMainServices;
import utils.Acces;
import utils.Login;
import utils.Permissions;
import utils.ViewMode;
import models.tables.pojos.Reglement;
import models.tables.pojos.StructurePartenaire;
import models.tables.pojos.Adherent;
import models.tables.pojos.AyantDroit;
import models.tables.pojos.TypePrestation;
import models.tables.pojos.VBonDeCommande;
import services.AyantDroitMainServices;
import services.TypePrestationMainService;
import services.VBonDeCommandeServices;
import play.libs.Json;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * This controller contains an action to handle HTTP requests to the
 * application's home page.
 */

public class HomeController extends Controller {

	FormFactory formatFactory;
	AdherentMainServices consultationServices;
	ParamsServices paramsService;
	ReglementMainServices reglementServices;
	StructureMainServices structureServices;
	AyantDroitMainServices ayantDroitServices;
	TypePrestationMainService typePrestationServices;
	VBonDeCommandeServices bonDeCommandeServices;
	DashboardServices dashboardServices;

	private static final play.Logger.ALogger logger = play.Logger.of(HomeController.class);
	
	@Inject
	public HomeController(FormFactory formatFactory, AdherentMainServices consultationServices,
			ParamsServices paramsService, ReglementMainServices reglementServices,
			StructureMainServices structureServices, AyantDroitMainServices ayantDroitServices,
			TypePrestationMainService typePrestationServices, VBonDeCommandeServices bonDeCommandeServices,
			DashboardServices dashboardServices) {

		this.formatFactory = formatFactory;
		this.consultationServices = consultationServices;
		this.paramsService = paramsService;
		this.reglementServices = reglementServices;
		this.structureServices = structureServices;
		this.ayantDroitServices = ayantDroitServices;
		this.typePrestationServices = typePrestationServices;
		this.bonDeCommandeServices = bonDeCommandeServices;
		this.dashboardServices = dashboardServices;
		

	}

	public Result index(Request request) {
		//return ok(views.html.login.render(request));
		return ok(views.html.index.render(paramsService.listesParams(),request));
	}


	public Result acceuil(Request request) {
		//System.out.println("les sessions sont login:" +request.session().get("login") +" droit :"+ request.session().get("droit") +" nonUser :" + request.session().get("nomUser"));
		//System.out.println("Con url:" +db.getUrl() +" Con Name :"+db.getName() +" Con other connexion:" + db.getConnection()+" Con others datasources" + db.getDataSource());
		
		if (!request.session().get("login").isPresent() || !request.session().get("droit").isPresent()) {
			return ok(views.html.index.render(paramsService.listesParams(),request));
		}
		// page d'accueil = première fonctionnalité autorisée par le rôle (même ordre qu'avant :
		// Admin -> tableau de bord, Validateur -> bons en instance, Percepteur -> caisse...)
		if (Acces.peut(request, Permissions.TABLEAU_BORD)) {
			String anneeCourante = String.valueOf(java.time.LocalDate.now().getYear());
			return ok(views.html.acceuil.render(dashboardServices.listAnnees(), anneeCourante, request));
		}
		if (Acces.peut(request, Permissions.BON_VALIDER))
			return redirect(routes.ReglementCtrl.showBonToValidate());
		if (Acces.peut(request, Permissions.LABO_CAISSE))
			return redirect(routes.LaboratoireCtrl.caisse("", ""));
		if (Acces.peut(request, Permissions.LABO_RECU))
			return redirect(routes.LaboratoireCtrl.nouveauRecu());
		if (Acces.peut(request, Permissions.ADHERENT_LISTE, Permissions.ADHERENT_ESPACE))
			return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L));
		if (Acces.peut(request, Permissions.RAPPORT_SITUATION_PERSONNEL))
			return redirect(routes.ReglementCtrl.showRapSynthese());
		if (Acces.peut(request, Permissions.RAPPORT_SUPERVISION))
			return redirect(routes.ReglementCtrl.showRapSuperviseur());
		if (Acces.peut(request, Permissions.UTILISATEURS_GERER))
			return redirect(routes.UserCtrl.index());
		if (Acces.peut(request, Permissions.ROLES_GERER))
			return redirect(routes.RoleCtrl.index(""));
		if (Acces.peut(request, Permissions.LABO_EXAMENS))
			return redirect(routes.LaboratoireCtrl.typesExamen(ViewMode.VIEW_MODE_CREATE, 0L));
		return redirect(controllers.routes.AuthenticationCtrl.login()).withNewSession().flashing("error",
				"Aucune fonctionnalité n'est attribuée à votre profil, contactez l'administrateur");
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
	
	/**
	 * API des statistiques du tableau de bord, filtrables par année.
	 * Paramètres (query string) : annee=YYYY|ALL, refresh=1 pour ignorer le cache.
	 * Les agrégations sont faites en base et mises en cache (voir DashboardServices).
	 */
	public Result getStatsReglementsByStructure(Request request) {
		if (!isAdmin(request)) {
			return forbidden(Json.newObject().put("error", "Accès non autorisé"));
		}
		try {
			String annee = request.getQueryString("annee");
			boolean refresh = "1".equals(request.getQueryString("refresh"));
			return ok(dashboardServices.statistiques(annee, refresh));
		} catch (Exception e) {
			logger.error("Erreur lors du calcul des statistiques du tableau de bord", e);
			ObjectNode error = Json.newObject();
			error.put("error", "Erreur lors de la récupération des statistiques: " + e.getMessage());
			return internalServerError(error);
		}
	}

	/**
	 * Liste des bons (confirmés / non confirmés) d'une année, chargée à l'ouverture des fenêtres de détail.
	 */
	public Result getBonsDashboard(Request request) {
		if (!isAdmin(request)) {
			return forbidden(Json.newObject().put("error", "Accès non autorisé"));
		}
		try {
			boolean confirmes = !"0".equals(request.getQueryString("confirmes"));
			ObjectNode result = Json.newObject();
			result.set("data", dashboardServices.listBons(request.getQueryString("annee"), confirmes));
			return ok(result);
		} catch (Exception e) {
			logger.error("Erreur lors du chargement des bons du tableau de bord", e);
			return internalServerError(Json.newObject().put("error", e.getMessage()));
		}
	}

	private boolean isAdmin(Request request) {
		return Acces.peut(request, Permissions.TABLEAU_BORD);
	}
}
