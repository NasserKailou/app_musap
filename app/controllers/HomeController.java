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
	
	@Inject
	public HomeController(FormFactory formatFactory, AdherentMainServices consultationServices,
			ParamsServices paramsService, ReglementMainServices reglementServices,
			StructureMainServices structureServices, AyantDroitMainServices ayantDroitServices,
			TypePrestationMainService typePrestationServices, VBonDeCommandeServices bonDeCommandeServices) {

		this.formatFactory = formatFactory;
		this.consultationServices = consultationServices;
		this.paramsService = paramsService;
		this.reglementServices = reglementServices;
		this.structureServices = structureServices;
		this.ayantDroitServices = ayantDroitServices;
		this.typePrestationServices = typePrestationServices;
		this.bonDeCommandeServices = bonDeCommandeServices;
		

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
	 * Utilise la vue VBonDeCommande qui contient toutes les données consolidées
	 * @param request
	 * @return JSON avec les données pour le graphique
	 */
	public Result getStatsReglementsByStructure(Request request) {
		try {
			// Récupérer tous les bons de commande depuis la vue VBonDeCommande
			List<VBonDeCommande> bonsDeCommande = bonDeCommandeServices.findAll();
			
			// Récupérer les adhérents et ayants droit pour les totaux
			List<Adherent> adherents = consultationServices.findAll();
			List<AyantDroit> ayantsDroit = ayantDroitServices.findAll();
			
			// Map pour regrouper par structure (utilise le nom de la structure)
			Map<String, Integer> bonsConfirmesCount = new HashMap<>();
			Map<String, Integer> bonsNonConfirmesCount = new HashMap<>();
			Map<String, Long> structureIds = new HashMap<>();
			
			// Map pour compter les bons par type de prestation
			Map<String, Integer> bonsByTypePrestation = new HashMap<>();
			
			// Compter les bons de commande confirmés et non confirmés par structure
			int totalBonsConfirmes = 0;
			int totalBonsNonConfirmes = 0;
			
			for (VBonDeCommande bon : bonsDeCommande) {
				String structureName = bon.getStructure();
				if (structureName != null) {
					// Stocker l'ID de la structure
					if (!structureIds.containsKey(structureName) && bon.getIdStructure() != null) {
						structureIds.put(structureName, bon.getIdStructure());
					}
					
					// Initialiser les compteurs pour cette structure si nécessaire
					if (!bonsConfirmesCount.containsKey(structureName)) {
						bonsConfirmesCount.put(structureName, 0);
						bonsNonConfirmesCount.put(structureName, 0);
					}
					
					// Vérifier si le bon est confirmé
					if (bon.getIsConfirmedBon() != null && bon.getIsConfirmedBon()) {
						// Bon confirmé (consommé)
						bonsConfirmesCount.put(structureName, 
							bonsConfirmesCount.get(structureName) + 1);
						totalBonsConfirmes++;
					} else {
						// Bon non confirmé (isConfirmedBon est false ou null)
						bonsNonConfirmesCount.put(structureName, 
							bonsNonConfirmesCount.get(structureName) + 1);
						totalBonsNonConfirmes++;
					}
				}
				
				// Compter par type de prestation
				String prestationName = bon.getPrestation();
				if (prestationName != null && !prestationName.trim().isEmpty()) {
					bonsByTypePrestation.put(prestationName, 
						bonsByTypePrestation.getOrDefault(prestationName, 0) + 1);
				}
			}
			
			// Construire le JSON de réponse
			ArrayNode structuresArray = Json.newArray();
			ArrayNode bonsConfirmesArray = Json.newArray();
			ArrayNode bonsNonConfirmesArray = Json.newArray();
			
			// Ajouter les données pour chaque structure qui a des bons
			for (String structureName : bonsConfirmesCount.keySet()) {
				structuresArray.add(structureName);
				bonsConfirmesArray.add(bonsConfirmesCount.get(structureName));
				bonsNonConfirmesArray.add(bonsNonConfirmesCount.get(structureName));
			}
			
			// Construire les données par type de prestation
			ArrayNode typePrestationsArray = Json.newArray();
			ArrayNode bonsByTypePrestationArray = Json.newArray();
			
			for (Map.Entry<String, Integer> entry : bonsByTypePrestation.entrySet()) {
				if (entry.getValue() > 0) { // N'inclure que les prestations avec des bons
					typePrestationsArray.add(entry.getKey());
					bonsByTypePrestationArray.add(entry.getValue());
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
			result.put("totalStructures", structureIds.size());
			result.set("typePrestations", typePrestationsArray);
			result.set("bonsByTypePrestation", bonsByTypePrestationArray);
			
			return ok(result);
			
		} catch (Exception e) {
			ObjectNode error = Json.newObject();
			error.put("error", "Erreur lors de la récupération des statistiques: " + e.getMessage());
			e.printStackTrace();
			return internalServerError(error);
		}
	}
}
