package controllers;

import static play.libs.Json.toJson;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import dto.RecuLaboVue;
import models.tables.pojos.RecuLabo;
import models.tables.pojos.RecuLaboDetail;
import models.tables.pojos.TypeExamen;
import play.Logger;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.DashboardServices;
import services.LaboratoireServices;
import utils.Permission;
import utils.Permissions;
import utils.Secured;
import utils.ViewMode;

/**
 * Module Laboratoire : profil Percepteur (et Admin).
 * - paramétrage des types d'examens et de leurs montants
 * - émission de reçus pour un ou plusieurs examens (adhérents, ayants droit ou personnes externes)
 * - journal / situation de caisse, annulation de reçus
 *
 * @author nasser
 */
@Security.Authenticated(Secured.class)
public class LaboratoireCtrl extends Controller {

	private static final Logger.ALogger logger = Logger.of(LaboratoireCtrl.class);

	public static final String DROIT_PERCEPTEUR = "Percepteur";

	private final LaboratoireServices laboServices;
	private final DashboardServices dashboardServices;
	private final FormFactory formFactory;

	@Inject
	public LaboratoireCtrl(LaboratoireServices laboServices, DashboardServices dashboardServices,
			FormFactory formFactory) {
		this.laboServices = laboServices;
		this.dashboardServices = dashboardServices;
		this.formFactory = formFactory;
	}

	private String login(Request request) {
		return request.session().get("login").orElse("");
	}

	// =====================================================================
	// CAISSE (page d'accueil du percepteur)
	// =====================================================================

	@Permission(Permissions.LABO_CAISSE)
	public Result caisse(Request request, String debut, String fin) {
		LocalDate d = parseDate(debut, LocalDate.now());
		LocalDate f = parseDate(fin, d);
		if (f.isBefore(d)) {
			LocalDate t = d;
			d = f;
			f = t;
		}
		List<RecuLaboVue> recus = new ArrayList<>();
		String statsJson = "{\"disponible\":false}";
		try {
			recus = laboServices.listRecus(d, f);
			statsJson = jsonPourScript(laboServices.situationCaisse(d, f));
		} catch (Exception e) {
			logger.error("Erreur chargement caisse laboratoire", e);
		}
		return ok(views.html.laboCaisse.render(recus, d.toString(), f.toString(), statsJson, request));
	}

	// =====================================================================
	// TYPES D'EXAMENS
	// =====================================================================

	@Permission(Permissions.LABO_EXAMENS)
	public Result typesExamen(Request request, String subAction, Long idExamen) {
		String viewMode;
		TypeExamen t = null;
		if (idExamen != null && idExamen > 0)
			t = laboServices.findTypeExamen(idExamen);
		if (t == null) {
			t = new TypeExamen();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_EDIT;
		}
		return ok(views.html.laboTypesExamen.render(viewMode, laboServices.listTypesExamen(), t,
				laboServices.listCategories(), request));
	}

	@Permission(Permissions.LABO_EXAMENS)
	public Result saveTypeExamen(Request request) {
		DynamicForm form = formFactory.form().bindFromRequest(request);
		String viewMode = form.get("viewMode");
		Result retour = redirect(routes.LaboratoireCtrl.typesExamen(ViewMode.VIEW_MODE_CREATE, 0L));

		if (ViewMode.VIEW_MODE_DELETE.equals(viewMode)) {
			Long id = parseLong(form.get("id"));
			String res = laboServices.deleteTypeExamen(id, login(request));
			return "ok".equals(res) ? retour.flashing("success", "Examen supprimé avec succès")
					: retour.flashing("error", "Examen non supprimé : " + res);
		}

		TypeExamen t = new TypeExamen();
		t.setId(parseLong(form.get("id")));
		t.setCode(form.get("code"));
		t.setLibelle(form.get("libelle"));
		t.setCategorie(form.get("categorie"));
		t.setMontant(parseLong(form.get("montant")));
		t.setMontantAdherent(parseLong(form.get("montantAdherent")));
		t.setWhoDone(login(request));

		boolean creation = !ViewMode.VIEW_MODE_EDIT.equals(viewMode);
		String res = laboServices.saveTypeExamen(t, creation);
		if ("ok".equals(res))
			return retour.flashing("success",
					"Examen " + t.getLibelle() + (creation ? " ajouté" : " modifié") + " avec succès");
		return retour.flashing("error", res);
	}

	// =====================================================================
	// REÇUS
	// =====================================================================

	@Permission(Permissions.LABO_RECU)
	public Result nouveauRecu(Request request) {
		return ok(views.html.laboRecu.render(laboServices.listTypesExamen(), LaboratoireServices.MODES_PAIEMENT,
				request));
	}

	@Permission(Permissions.LABO_RECU)
	public Result saveRecu(Request request) {
		Map<String, String[]> data = request.body().asFormUrlEncoded();
		if (data == null)
			data = new HashMap<>();

		RecuLabo recu = new RecuLabo();
		recu.setTypePatient(first(data, "typePatient"));
		recu.setAdherent(parseLong(first(data, "idAdherent")));
		recu.setAyantDroit(parseLong(first(data, "idAyantDroit")));
		recu.setNomPatient(first(data, "nomPatient"));
		recu.setTelephone(first(data, "telephone"));
		recu.setStructureOrigine(first(data, "structureOrigine"));
		recu.setModePaiement(first(data, "modePaiement"));

		List<Long> examens = new ArrayList<>();
		List<Integer> quantites = new ArrayList<>();
		String[] ex = data.getOrDefault("examens[]", new String[0]);
		String[] qt = data.getOrDefault("quantites[]", new String[0]);
		for (int i = 0; i < ex.length; i++) {
			Long id = parseLong(ex[i]);
			if (id == null)
				continue;
			examens.add(id);
			Long q = i < qt.length ? parseLong(qt[i]) : null;
			quantites.add(q == null ? 1 : q.intValue());
		}

		try {
			Long id = laboServices.creerRecu(recu, examens, quantites, login(request));
			dashboardServices.invalidate();
			return redirect(routes.LaboratoireCtrl.imprimerRecu(id, true)).flashing("success",
					"Reçu enregistré avec succès");
		} catch (IllegalArgumentException e) {
			return redirect(routes.LaboratoireCtrl.nouveauRecu()).flashing("error", e.getMessage());
		} catch (Exception e) {
			logger.error("Erreur enregistrement reçu laboratoire", e);
			return redirect(routes.LaboratoireCtrl.nouveauRecu()).flashing("error",
					"Erreur lors de l'enregistrement du reçu, veuillez réessayer");
		}
	}

	@Permission({ Permissions.LABO_RECU, Permissions.LABO_CAISSE })
	public Result imprimerRecu(Request request, Long id, Boolean auto) {
		RecuLaboVue recu = laboServices.findRecu(id);
		if (recu == null)
			return redirect(routes.LaboratoireCtrl.caisse("", "")).flashing("error", "Reçu introuvable");
		List<RecuLaboDetail> details = laboServices.detailsRecu(id);
		return ok(views.html.laboRecuPrint.render(recu, details, auto != null && auto, request));
	}

	@Permission(Permissions.LABO_ANNULER)
	public Result annulerRecu(Request request, Long id) {
		DynamicForm form = formFactory.form().bindFromRequest(request);
		String res = laboServices.annulerRecu(id, form.get("motif"), login(request));
		String debut = form.get("debut") == null ? "" : form.get("debut");
		String fin = form.get("fin") == null ? "" : form.get("fin");
		Result retour = redirect(routes.LaboratoireCtrl.caisse(debut, fin));
		if ("ok".equals(res)) {
			dashboardServices.invalidate();
			return retour.flashing("success", "Reçu annulé avec succès");
		}
		return retour.flashing("error", res);
	}

	// =====================================================================
	// JSON
	// =====================================================================

	@Permission(Permissions.LABO_RECU)
	public Result jsonBeneficiaires(Request request, String q) {
		Map<String, Object> response = new HashMap<>();
		try {
			response.put("options", laboServices.rechercherBeneficiaires(q));
		} catch (Exception e) {
			logger.error("Erreur recherche bénéficiaires", e);
			response.put("options", new ArrayList<>());
		}
		return ok(toJson(response));
	}

	// =====================================================================
	// Utilitaires
	// =====================================================================

	/** Sérialise un JSON pour l'insérer sans risque dans une balise script */
	static String jsonPourScript(ObjectNode node) {
		return node.toString().replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
	}

	private static String first(Map<String, String[]> data, String key) {
		String[] v = data.get(key);
		return v == null || v.length == 0 ? null : v[0];
	}

	private static Long parseLong(String s) {
		if (s == null)
			return null;
		String v = s.replaceAll("[\\s\\u00A0]", "");
		if (v.isEmpty())
			return null;
		try {
			return Long.valueOf(v);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static LocalDate parseDate(String s, LocalDate defaut) {
		if (s == null || s.trim().isEmpty())
			return defaut;
		try {
			return LocalDate.parse(s.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (Exception e) {
			return defaut;
		}
	}
}
