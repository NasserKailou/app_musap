package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catalogue des permissions (fonctionnalités) de l'application.
 *
 * Les permissions sont définies dans le code car elles correspondent à des
 * fonctionnalités ; les rôles et l'octroi des permissions aux rôles sont
 * gérés en base depuis l'écran Paramètrages > Rôles & permissions.
 *
 * Les droits par défaut reproduisent exactement les accès qui étaient codés
 * en dur avant l'introduction de la gestion des rôles.
 *
 * @author nasser
 */
public final class Permissions {

	public static final String ROLE_ADMIN = "Admin";

	// ---- Tableau de bord
	public static final String TABLEAU_BORD = "TABLEAU_BORD";

	// ---- Adhérents et ayants droit
	public static final String ADHERENT_LISTE = "ADHERENT_LISTE";
	public static final String ADHERENT_ESPACE = "ADHERENT_ESPACE";
	public static final String ADHERENT_CREER = "ADHERENT_CREER";
	public static final String ADHERENT_MODIFIER = "ADHERENT_MODIFIER";
	public static final String ADHERENT_ARCHIVER = "ADHERENT_ARCHIVER";
	public static final String ADHERENT_IMPRIMER = "ADHERENT_IMPRIMER";
	public static final String PHOTO_GERER = "PHOTO_GERER";
	public static final String AYANT_DROIT_CONSULTER = "AYANT_DROIT_CONSULTER";
	public static final String AYANT_DROIT_GERER = "AYANT_DROIT_GERER";

	// ---- Bons de commande / prises en charge
	public static final String BON_CREER = "BON_CREER";
	public static final String BON_MODIFIER = "BON_MODIFIER";
	public static final String BON_SUPPRIMER_LIGNE = "BON_SUPPRIMER_LIGNE";
	public static final String BON_SYNTHESE = "BON_SYNTHESE";
	public static final String BON_VALIDER = "BON_VALIDER";
	public static final String BON_REACTUALISER = "BON_REACTUALISER";
	public static final String BON_ARCHIVER = "BON_ARCHIVER";
	public static final String BON_IMPRIMER = "BON_IMPRIMER";

	// ---- Rapports
	public static final String RAPPORT_SUPERVISION = "RAPPORT_SUPERVISION";
	public static final String RAPPORT_SITUATION_PERSONNEL = "RAPPORT_SITUATION_PERSONNEL";
	public static final String RAPPORT_COTISATIONS = "RAPPORT_COTISATIONS";
	public static final String RAPPORT_ETATS = "RAPPORT_ETATS";
	public static final String RAPPORT_OPERATIONS = "RAPPORT_OPERATIONS";
	public static final String RAPPORT_ANNULATIONS = "RAPPORT_ANNULATIONS";
	public static final String ALERTE_EMAIL = "ALERTE_EMAIL";

	// ---- Laboratoire
	public static final String LABO_RECU = "LABO_RECU";
	public static final String LABO_CAISSE = "LABO_CAISSE";
	public static final String LABO_ANNULER = "LABO_ANNULER";
	public static final String LABO_EXAMENS = "LABO_EXAMENS";

	// ---- Paramétrage
	public static final String UTILISATEURS_GERER = "UTILISATEURS_GERER";
	public static final String ROLES_GERER = "ROLES_GERER";
	public static final String COTISATION_PARAMETRER = "COTISATION_PARAMETRER";
	public static final String PRESTATIONS_GERER = "PRESTATIONS_GERER";
	public static final String STRUCTURES_GERER = "STRUCTURES_GERER";
	public static final String CONSOMMATIONS_IMPORTER = "CONSOMMATIONS_IMPORTER";
	public static final String COMPTABILITE = "COMPTABILITE";

	/** Description d'une permission pour l'écran de gestion */
	public static final class Definition {
		public final String code;
		public final String module;
		public final String libelle;
		public final String description;

		Definition(String code, String module, String libelle, String description) {
			this.code = code;
			this.module = module;
			this.libelle = libelle;
			this.description = description;
		}

		public String getCode() { return code; }
		public String getModule() { return module; }
		public String getLibelle() { return libelle; }
		public String getDescription() { return description; }
	}

	private static final List<Definition> CATALOGUE = new ArrayList<>();
	private static final Map<String, Set<String>> DEFAUTS = new LinkedHashMap<>();

	private static void def(String code, String module, String libelle, String description, String... roles) {
		CATALOGUE.add(new Definition(code, module, libelle, description));
		for (String r : roles)
			DEFAUTS.computeIfAbsent(r, k -> new HashSet<>()).add(code);
	}

	static {
		String tb = "Tableau de bord", adh = "Adhérents & ayants droit", bon = "Bons de commande & prises en charge",
				rap = "Rapports", lab = "Laboratoire", par = "Paramétrage";

		def(TABLEAU_BORD, tb, "Tableau de bord", "Statistiques générales de la mutuelle");

		def(ADHERENT_LISTE, adh, "Liste de tous les adhérents", "Consulter et rechercher l'ensemble des adhérents",
				"saisie", "Validateur");
		def(ADHERENT_ESPACE, adh, "Espace adhérent", "Consulter et modifier uniquement sa propre fiche", "Adherent");
		def(ADHERENT_CREER, adh, "Créer un adhérent", "Enregistrer un nouvel adhérent");
		def(ADHERENT_MODIFIER, adh, "Modifier un adhérent", "Modifier la fiche de n'importe quel adhérent");
		def(ADHERENT_ARCHIVER, adh, "Archiver / réactiver un adhérent", "Suppression logique et réactivation");
		def(ADHERENT_IMPRIMER, adh, "Imprimer situations et cartes", "Situation des paiements, cartes et badges");
		def(PHOTO_GERER, adh, "Gérer les photos", "Charger la photo d'un adhérent ou d'un ayant droit", "Adherent");
		def(AYANT_DROIT_CONSULTER, adh, "Consulter les ayants droit", "Voir les personnes à charge d'un adhérent",
				"Adherent");
		def(AYANT_DROIT_GERER, adh, "Gérer les ayants droit",
				"Ajouter, modifier, supprimer un ayant droit (recalcule la cotisation)");

		def(BON_CREER, bon, "Créer bons et prises en charge", "Créer un bon et saisir ses lignes", "saisie",
				"Validateur");
		def(BON_MODIFIER, bon, "Modifier un bon", "Modifier un bon non confirmé");
		def(BON_SUPPRIMER_LIGNE, bon, "Supprimer une ligne de bon", "Supprimer une ligne de détail d'un bon");
		def(BON_SYNTHESE, bon, "Synthèse financière", "Voir la synthèse des consommations et plafonds de l'adhérent");
		def(BON_VALIDER, bon, "Valider les bons", "Confirmer les bons en instance (y compris expirés)", "Validateur");
		def(BON_REACTUALISER, bon, "Réactualiser un bon", "Annuler la confirmation d'un bon", "Validateur");
		def(BON_ARCHIVER, bon, "Archiver / réactiver un bon", "Suppression logique et réactivation d'un bon");
		def(BON_IMPRIMER, bon, "Imprimer les bons", "Imprimer un bon de prise en charge", "saisie", "Validateur");

		def(RAPPORT_SUPERVISION, rap, "Supervision", "Rapport de supervision des consommations");
		def(RAPPORT_SITUATION_PERSONNEL, rap, "Situation du personnel", "Synthèse par agent", "saisie");
		def(RAPPORT_COTISATIONS, rap, "Cotisations & personnes à charge", "Taux de cotisation et ayants droit");
		def(RAPPORT_ETATS, rap, "États et impressions", "États périodiques, situation globale, badges");
		def(RAPPORT_OPERATIONS, rap, "Opérations du jour / du mois",
				"Bons et prises en charge émis par période, émetteur et région");
		def(RAPPORT_ANNULATIONS, rap, "Bons & PEC annulés",
				"Liste des annulations (restauration selon la permission « Archiver / réactiver un bon »)");
		def(ALERTE_EMAIL, rap, "Alertes e-mail", "Envoi d'alertes par e-mail");

		def(LABO_RECU, lab, "Émettre des reçus", "Encaisser les examens et imprimer les reçus", "Percepteur");
		def(LABO_CAISSE, lab, "Caisse & journal", "Consulter la caisse et le journal des reçus", "Percepteur");
		def(LABO_ANNULER, lab, "Annuler un reçu", "Annuler un reçu avec motif", "Percepteur");
		def(LABO_EXAMENS, lab, "Types d'examens", "Paramétrer les examens et leurs tarifs", "Percepteur");

		def(UTILISATEURS_GERER, par, "Utilisateurs", "Créer, modifier, activer/désactiver les utilisateurs");
		def(ROLES_GERER, par, "Rôles & permissions", "Créer des rôles et leur octroyer des permissions");
		def(COTISATION_PARAMETRER, par, "Taux de cotisation", "Paramétrer les taux et recalculer les cotisations");
		def(PRESTATIONS_GERER, par, "Prestations", "Types de prestations et taux de couverture");
		def(STRUCTURES_GERER, par, "Structures partenaires", "Hôpitaux, pharmacies, laboratoires partenaires");
		def(CONSOMMATIONS_IMPORTER, par, "Import des consommations", "Import Excel de l'étude des consommations");
		def(COMPTABILITE, par, "Comptabilité & budget", "Budget, comptes, dépenses et écritures");
	}

	private Permissions() {
	}

	public static List<Definition> catalogue() {
		return Collections.unmodifiableList(CATALOGUE);
	}

	/** Permissions groupées par module, dans l'ordre d'affichage */
	public static Map<String, List<Definition>> parModule() {
		Map<String, List<Definition>> m = new LinkedHashMap<>();
		for (Definition d : CATALOGUE)
			m.computeIfAbsent(d.module, k -> new ArrayList<>()).add(d);
		return m;
	}

	public static Set<String> codes() {
		Set<String> s = new HashSet<>();
		for (Definition d : CATALOGUE)
			s.add(d.code);
		return s;
	}

	public static boolean existe(String code) {
		return codes().contains(code);
	}

	/** Permissions accordées par défaut à un rôle existant (initialisation) */
	public static Set<String> defauts(String role) {
		if (ROLE_ADMIN.equals(role))
			return codes();
		return DEFAUTS.getOrDefault(role, Collections.emptySet());
	}

	/** Rôles présents avant la gestion des rôles, créés à l'initialisation */
	public static List<String[]> rolesInitiaux() {
		return Arrays.asList(
				new String[] { ROLE_ADMIN, "Administrateur", "Accès complet à toutes les fonctionnalités" },
				new String[] { "saisie", "Agent de saisie", "Saisie des bons et prises en charge" },
				new String[] { "Validateur", "Validateur", "Validation des bons de commande" },
				new String[] { "Percepteur", "Percepteur (laboratoire)", "Encaissement des examens de laboratoire" },
				new String[] { "Adherent", "Adhérent", "Espace personnel de l'adhérent" });
	}
}
