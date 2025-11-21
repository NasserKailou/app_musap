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
import models.tables.pojos.Adherent;
import models.tables.pojos.AyantDroit;
import models.tables.pojos.TypePrestation;
import services.AyantDroitMainServices;
import services.TypePrestationMainService;
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
	
	@Inject
	public HomeController(FormFactory formatFactory, AdherentMainServices consultationServices,
			ParamsServices paramsService, ReglementMainServices reglementServices,
			StructureMainServices structureServices, AyantDroitMainServices ayantDroitServices,
			TypePrestationMainService typePrestationServices) {

		this.formatFactory = formatFactory;
		this.consultationServices = consultationServices;
		this.paramsService = paramsService;
		this.reglementServices = reglementServices;
		this.structureServices = structureServices;
		this.ayantDroitServices = ayantDroitServices;
		this.typePrestationServices = typePrestationServices;
		

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
	 * API pour récupérer les statistiques des bons de commande par structure partenaire
	 * @param request
	 * @return JSON avec les données pour le graphique
	 */
	public Result getStatsReglementsByStructure(Request request) {
		try {
			// Récupérer tous les règlements (bons de commande) non supprimés
			List<Reglement> reglements = reglementServices.findAll();
			
			// Récupérer toutes les structures partenaires
			List<StructurePartenaire> structures = structureServices.findAll();
			
			// Récupérer les adhérents et ayants droit
			List<Adherent> adherents = consultationServices.findAll();
			List<AyantDroit> ayantsDroit = ayantDroitServices.findAll();
			
			// Récupérer tous les types de prestations
			List<TypePrestation> typePrestations = typePrestationServices.getAllTypePrestation();
			
			// Map pour compter les bons de commande par structure
			Map<Long, Integer> bonsConfirmesCount = new HashMap<>();
			Map<Long, Integer> bonsNonConfirmesCount = new HashMap<>();
			
			// Map pour compter les bons par type de prestation
			Map<Long, Integer> bonsByTypePrestation = new HashMap<>();
			
			// Initialiser les compteurs pour chaque type de prestation
			for (TypePrestation tp : typePrestations) {
				bonsByTypePrestation.put(tp.getId(), 0);
			}
			
			// Initialiser les compteurs pour chaque structure
			for (StructurePartenaire struct : structures) {
				bonsConfirmesCount.put(struct.getId(), 0);
				bonsNonConfirmesCount.put(struct.getId(), 0);
			}
			
			// Compter les bons de commande confirmés et non confirmés par structure
			int totalBonsConfirmes = 0;
			int totalBonsNonConfirmes = 0;
			
			for (Reglement reg : reglements) {
				if (reg.getStructure() != null) {
					// Vérifier si le bon est confirmé
					if (reg.getIsConfirmedBon() != null && reg.getIsConfirmedBon()) {
						// Bon confirmé (consommé)
						bonsConfirmesCount.put(reg.getStructure(), 
							bonsConfirmesCount.getOrDefault(reg.getStructure(), 0) + 1);
						totalBonsConfirmes++;
					} else {
						// Bon non confirmé (isConfirmedBon est false ou null)
						bonsNonConfirmesCount.put(reg.getStructure(), 
							bonsNonConfirmesCount.getOrDefault(reg.getStructure(), 0) + 1);
						totalBonsNonConfirmes++;
					}
				}
				
				// Compter par type de prestation
				if (reg.getTypePrestation() != null) {
					bonsByTypePrestation.put(reg.getTypePrestation(), 
						bonsByTypePrestation.getOrDefault(reg.getTypePrestation(), 0) + 1);
				}
			}
			
			// Construire le JSON de réponse
			ArrayNode structuresArray = Json.newArray();
			ArrayNode bonsConfirmesArray = Json.newArray();
			ArrayNode bonsNonConfirmesArray = Json.newArray();
			
			for (StructurePartenaire struct : structures) {
				structuresArray.add(struct.getLibelle() != null ? struct.getLibelle() : "Structure #" + struct.getId());
				bonsConfirmesArray.add(bonsConfirmesCount.get(struct.getId()));
				bonsNonConfirmesArray.add(bonsNonConfirmesCount.get(struct.getId()));
			}
			
			// Construire les données par type de prestation
			ArrayNode typePrestationsArray = Json.newArray();
			ArrayNode bonsByTypePrestationArray = Json.newArray();
			
			for (TypePrestation tp : typePrestations) {
				int count = bonsByTypePrestation.get(tp.getId());
				if (count > 0) { // N'inclure que les prestations avec des bons
					typePrestationsArray.add(tp.getPrestation() != null ? tp.getPrestation() : "Prestation #" + tp.getId());
					bonsByTypePrestationArray.add(count);
				}
			}
			
			ObjectNode result = Json.newObject();
			result.set("structures", structuresArray);
			result.set("bonsConfirmes", bonsConfirmesArray);
			result.set("bonsNonConfirmes", bonsNonConfirmesArray);
			result.put("totalAdherents", adherents.size());
			result.put("totalAyantsDroit", ayantsDroit.size());
			result.put("totalBonsConfirmes", totalBonsConfirmes);
			result.put("totalBonsNonConfirmes", totalBonsNonConfirmes);
			result.put("totalStructures", structures.size());
			result.set("typePrestations", typePrestationsArray);
			result.set("bonsByTypePrestation", bonsByTypePrestationArray);
			
			return ok(result);
			
		} catch (Exception e) {
			ObjectNode error = Json.newObject();
			error.put("error", "Erreur lors de la récupération des statistiques: " + e.getMessage());
			return internalServerError(error);
		}
	}
}
