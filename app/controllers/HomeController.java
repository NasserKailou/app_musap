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
import services.ParamsServices;
import services.ReglementMainServices;
import services.StructureMainServices;
import utils.Login;
import models.tables.pojos.Reglement;
import models.tables.pojos.StructurePartenaire;
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
	
	@Inject
	public HomeController(FormFactory formatFactory, AdherentMainServices consultationServices,
			ParamsServices paramsService, ReglementMainServices reglementServices,
			StructureMainServices structureServices) {

		this.formatFactory = formatFactory;
		this.consultationServices = consultationServices;
		this.paramsService = paramsService;
		this.reglementServices = reglementServices;
		this.structureServices = structureServices;
		

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
	
	/**
	 * API pour récupérer les statistiques des règlements par structure partenaire
	 * @param request
	 * @return JSON avec les données pour le graphique
	 */
	public Result getStatsReglementsByStructure(Request request) {
		try {
			// Récupérer tous les règlements non supprimés
			List<Reglement> reglements = reglementServices.findAll();
			
			// Récupérer toutes les structures partenaires
			List<StructurePartenaire> structures = structureServices.findAll();
			
			// Map pour compter les règlements par structure
			Map<Long, Integer> reglementCount = new HashMap<>();
			Map<Long, Integer> bonCount = new HashMap<>();
			
			// Initialiser les compteurs pour chaque structure
			for (StructurePartenaire struct : structures) {
				reglementCount.put(struct.getId(), 0);
				bonCount.put(struct.getId(), 0);
			}
			
			// Compter les règlements et bons de commande par structure
			for (Reglement reg : reglements) {
				if (reg.getStructure() != null) {
					// Compter les règlements
					reglementCount.put(reg.getStructure(), 
						reglementCount.getOrDefault(reg.getStructure(), 0) + 1);
					
					// Compter les bons de commande confirmés
					if (reg.getIsConfirmedBon() != null && reg.getIsConfirmedBon()) {
						bonCount.put(reg.getStructure(), 
							bonCount.getOrDefault(reg.getStructure(), 0) + 1);
					}
				}
			}
			
			// Construire le JSON de réponse
			ArrayNode structuresArray = Json.newArray();
			ArrayNode reglementsArray = Json.newArray();
			ArrayNode bonsArray = Json.newArray();
			
			for (StructurePartenaire struct : structures) {
				structuresArray.add(struct.getLibelle() != null ? struct.getLibelle() : "Structure #" + struct.getId());
				reglementsArray.add(reglementCount.get(struct.getId()));
				bonsArray.add(bonCount.get(struct.getId()));
			}
			
			ObjectNode result = Json.newObject();
			result.set("structures", structuresArray);
			result.set("reglements", reglementsArray);
			result.set("bons", bonsArray);
			
			return ok(result);
			
		} catch (Exception e) {
			ObjectNode error = Json.newObject();
			error.put("error", "Erreur lors de la récupération des statistiques: " + e.getMessage());
			return internalServerError(error);
		}
	}
}
