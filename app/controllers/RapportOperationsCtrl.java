package controllers;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import dto.FiltreRapport;
import dto.OperationVue;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.RapportOperationsServices;
import utils.Acces;
import utils.Permission;
import utils.Permissions;
import utils.Secured;

/**
 * Rapports des opérations (bons de commande et prises en charge) du jour, du mois
 * ou d'une période, liste des annulations et restauration d'un bon annulé.
 *
 * @author nasser
 */
@Security.Authenticated(Secured.class)
public class RapportOperationsCtrl extends Controller {

	private final RapportOperationsServices service;
	private final FormFactory formFactory;

	@Inject
	public RapportOperationsCtrl(RapportOperationsServices service, FormFactory formFactory) {
		this.service = service;
		this.formFactory = formFactory;
	}

	private FiltreRapport filtre(String periode, String date, String mois, String debut, String fin, String type,
			String region, String periodeParDefaut) {
		FiltreRapport f = FiltreRapport.depuis(periode, date, mois, debut, fin, type, region, periodeParDefaut);
		if (f.getRegion() != null) {
			for (Map<String, Object> r : service.regions())
				if (f.getRegion().equals(r.get("id")))
					f.setLibelleRegion(String.valueOf(r.get("libelle")));
		}
		return f;
	}

	@Permission(Permissions.RAPPORT_OPERATIONS)
	public Result operations(Request request, String periode, String date, String mois, String debut, String fin,
			String type, String region) {
		FiltreRapport f = filtre(periode, date, mois, debut, fin, type, region, "jour");
		List<OperationVue> ops = service.operations(f.getDebut(), f.getFin(), false, f.getType(), f.getRegion());
		return ok(views.html.rapportOperations.render("operations", ops, f, service.regions(), request));
	}

	@Permission(Permissions.RAPPORT_ANNULATIONS)
	public Result annulations(Request request, String periode, String date, String mois, String debut, String fin,
			String type, String region) {
		FiltreRapport f = filtre(periode, date, mois, debut, fin, type, region, "mois");
		List<OperationVue> ops = service.operations(f.getDebut(), f.getFin(), true, f.getType(), f.getRegion());
		return ok(views.html.rapportOperations.render("annulations", ops, f, service.regions(), request));
	}

	/** Export Excel (CSV ; UTF-8) du rapport affiché */
	@Permission({ Permissions.RAPPORT_OPERATIONS, Permissions.RAPPORT_ANNULATIONS })
	public Result exporter(Request request, String rapport, String periode, String date, String mois, String debut,
			String fin, String type, String region) {
		boolean annulations = "annulations".equals(rapport);
		if (!Acces.peut(request, annulations ? Permissions.RAPPORT_ANNULATIONS : Permissions.RAPPORT_OPERATIONS))
			return redirect(routes.HomeController.acceuil()).flashing("error", "Accès non autorisé");
		FiltreRapport f = filtre(periode, date, mois, debut, fin, type, region, annulations ? "mois" : "jour");
		List<OperationVue> ops = service.operations(f.getDebut(), f.getFin(), annulations, f.getType(), f.getRegion());

		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		StringBuilder csv = new StringBuilder("﻿"); // BOM : accents corrects dans Excel
		csv.append(annulations ? "Bons et prises en charge annulés" : "Opérations").append(" - ")
				.append(f.getLibellePeriode()).append("\n");
		csv.append("N°;TYPES PRESTATION;EMETTEUR;REGION;BENEFICIAIRE;QUALITE;MATRICULE;DATE;STRUCTURE;PRESTATION;MONTANT");
		if (annulations)
			csv.append(";ANNULE PAR;ANNULE LE;MOTIF");
		csv.append("\n");
		int n = 1;
		for (OperationVue o : ops) {
			csv.append(n++).append(';').append(o.getTypeLibelle()).append(';').append(cel(o.getEmetteur())).append(';')
					.append(cel(o.getRegion())).append(';').append(cel(o.getBeneficiaire())).append(';')
					.append(cel(o.getQualiteLibelle())).append(';').append(cel(o.getMatricule())).append(';')
					.append(o.getDateOperation() == null ? "" : sdf.format(o.getDateOperation())).append(';')
					.append(cel(o.getStructure())).append(';').append(cel(o.getPrestation())).append(';')
					.append(o.getMontant() == null ? 0 : o.getMontant());
			if (annulations)
				csv.append(';').append(cel(o.getAnnulePar())).append(';')
						.append(o.getAnnuleLe() == null ? "" : sdf.format(o.getAnnuleLe())).append(';')
						.append(cel(o.getMotif()));
			csv.append("\n");
		}
		String nom = (annulations ? "annulations_" : "operations_") + f.getDebut() + "_" + f.getFin() + ".csv";
		return ok(csv.toString().getBytes(StandardCharsets.UTF_8)).as("text/csv; charset=utf-8")
				.withHeader("Content-Disposition", "attachment; filename=\"" + nom + "\"");
	}

	private static String cel(String v) {
		if (v == null)
			return "";
		String s = v.replace("\r", " ").replace("\n", " ");
		return s.contains(";") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
	}

	/** Restauration d'un bon ou d'une prise en charge annulé(e) */
	@Permission(Permissions.BON_ARCHIVER)
	public Result restaurer(Request request, Long id) {
		DynamicForm form = formFactory.form().bindFromRequest(request);
		String retour = form.get("retour");
		String res = service.changerEtat(id, false, form.get("motif"), request.session().get("login").orElse(""));
		String url = routes.RapportOperationsCtrl.annulations("", "", "", "", "", "", "").url().split("\\?")[0];
		// retour sur la même liste : paramètres de filtre uniquement, jamais une adresse externe
		if (retour != null && retour.matches("[A-Za-z0-9=&_\\-]*"))
			url = url + "?" + retour;
		if ("ok".equals(res))
			return redirect(url).flashing("success", "Bon n° " + id + " restauré : il est de nouveau actif");
		return redirect(url).flashing("error", res);
	}
}
