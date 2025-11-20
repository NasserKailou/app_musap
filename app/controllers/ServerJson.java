package controllers;

import static play.libs.Json.toJson;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import models.tables.pojos.ClassCompte;
import models.tables.pojos.VAdherentAyantDroit;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.AyantDroitMainServices;
import services.compta.ClassComptaServicesImpl;
import utils.Secured;

/**
 * 
 * @author nasser
 *
 */
@Security.Authenticated(Secured.class)
public class ServerJson extends Controller{

	@Inject
	AyantDroitMainServices ayService;
	
	@Inject
	ClassComptaServicesImpl cptServices;
	
	
	/**
	 * @author nasser
	 * @param query
	 * @return
	 */
	public Result jsonAyantDroit(String query) {
		//System.out.println("ICI JSON patient");
		String lQuery = null == query ? "" : query;
		List<VAdherentAyantDroit> details = ayService.findLikeAyantDroit(lQuery);
		HashMap<String, List<VAdherentAyantDroit>> response = new HashMap<>();
		response.put("options", details);
		//System.out.println("json result here :");
		return ok(toJson(response));
	}
	
	
	public Result jsonCompte(String query) {
		//System.out.println("ICI JSON patient");
		String lQuery = null == query ? "" : query;
		List<ClassCompte> details = cptServices.findLikeNumCompteC(lQuery);
		HashMap<String, List<ClassCompte>> response = new HashMap<>();
		response.put("options", details);
		//System.out.println("json result here :");
		return ok(toJson(response));
	}
	
	public Result jsonCompteB(String query) {
		//System.out.println("ICI JSON patient");
		String lQuery = null == query ? "" : query;
		List<ClassCompte> details = cptServices.findLikeNumCompteB(lQuery);
		HashMap<String, List<ClassCompte>> response = new HashMap<>();
		response.put("options", details);
		//System.out.println("json result here :");
		return ok(toJson(response));
	}
	
//	/**
//	 * @author nasser
//	 * @param query
//	 * @return
//	 */
//	public Result jsonTypeConsultation(String query) {
//		//System.out.println("ICI JSON");
//		String lQuery = null == query ? "" : query;
//		List<TypeConsultation> details = typeConsService.findLikeName(lQuery);
//		HashMap<String, List<TypeConsultation>> response = new HashMap<>();
//		response.put("options", details);
//		return ok(toJson(response));
//	}
	
}
