package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;

import java.util.HashMap;
import java.util.Map;

import org.jooq.Record;
import org.junit.Assume;
import org.junit.Test;

import play.Application;
import play.api.test.CSRFTokenHelper;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import utils.IConnectionHelper;

/**
 * Tests de non-régression (base locale). Les saisies envoyées sont volontairement
 * invalides : aucune écriture n'est faite en base.
 */
public class RegressionTest extends WithApplication {

	@Override
	protected Application provideApplication() {
		return new GuiceApplicationBuilder()
				.configure("play.filters.hosts.allowed", java.util.Collections.singletonList(".")) // hôte "localhost" des tests
				.build();
	}

	private Http.RequestBuilder get(String uri, String droit) {
		Http.RequestBuilder r = new Http.RequestBuilder().method(GET).uri(uri);
		return droit == null ? r : session(r, droit);
	}

	private Http.RequestBuilder post(String uri, Map<String, String> form, String droit) {
		Http.RequestBuilder r = new Http.RequestBuilder().method(POST).uri(uri).bodyForm(form);
		r = CSRFTokenHelper.addCSRFToken(r);
		return droit == null ? r : session(r, droit);
	}

	private Http.RequestBuilder session(Http.RequestBuilder r, String droit) {
		return r.session("login", "test-regression").session("droit", droit).session("nomUser", "Test")
				.session("gestion", currentGestion()).session("plafond", "0").session("seuilAlerte", "0");
	}

	private IConnectionHelper db() {
		return app.injector().instanceOf(IConnectionHelper.class);
	}

	private String currentGestion() {
		Record r = db().connection()
				.fetchOne("select gestion from params where is_deleted is false order by gestion desc limit 1");
		return r == null ? String.valueOf(java.time.LocalDate.now().getYear()) : r.get(0, String.class);
	}

	private static Map<String, String> form(String... kv) {
		Map<String, String> m = new HashMap<>();
		for (int i = 0; i < kv.length; i += 2)
			m.put(kv[i], kv[i + 1]);
		return m;
	}

	// ===================== Page de connexion =====================

	@Test
	public void pageConnexion_saffiche_sans_ressource_externe() {
		Result result = route(app, get("/", null));
		assertEquals(200, result.status());
		String html = contentAsString(result);
		assertTrue(html.contains("id=\"loginForm\""));
		assertTrue(html.contains("name=\"passe\""));
		assertTrue(html.contains("name=\"gestion\""));
		assertTrue("police chargée localement", html.contains("dist/css/googleapis.css"));
		assertFalse("aucun appel à Google Fonts", html.contains("fonts.googleapis.com"));
	}

	@Test
	public void connexion_sans_gestion_ne_plante_plus() {
		Result result = route(app, post("/auth", form("login", "inconnu", "passe", "x"), null));
		assertEquals(303, result.status());
		assertEquals("/login", result.redirectLocation().get());
		assertTrue(result.flash().get("error").get().contains("gestion"));
	}

	@Test
	public void connexion_formulaire_vide_ne_plante_plus() {
		Result result = route(app, post("/auth", form(), null));
		assertEquals(303, result.status());
		assertTrue(result.flash().get("error").isPresent());
	}

	@Test
	public void connexion_utilisateur_inconnu_refusee() {
		Result result = route(app, post("/auth",
				form("login", "utilisateur-inexistant-xyz", "passe", "x", "gestion", currentGestion()), null));
		assertEquals(303, result.status());
		assertEquals("/login", result.redirectLocation().get());
		assertTrue(result.flash().get("error").get().contains("Incorrect"));
	}

	@Test
	public void connexion_mauvais_mot_de_passe_refusee() {
		Record u = db().connection().fetchOne("select login from users where etat is true limit 1");
		Assume.assumeTrue(u != null);
		Result result = route(app, post("/auth", form("login", u.get(0, String.class), "passe",
				"mauvais-mot-de-passe-xyz", "gestion", currentGestion()), null));
		assertEquals(303, result.status());
		assertEquals("/login", result.redirectLocation().get());
		assertTrue("pas de session ouverte",
				result.session() == null || !result.session().get("droit").isPresent());
	}

	// ===================== Erreur de production : détails de règlement =====================

	private Record detailExistant() {
		return db().connection().fetchOne("select d.id, d.reglement, r.adherent from reglement_detail d"
				+ " join reglement r on r.id = d.reglement where d.on_deleted is false and r.on_deleted is false"
				+ " order by d.id desc limit 1");
	}

	private Map<String, String> formDetail(Record d, String viewMode, String... kv) {
		Map<String, String> m = form(kv);
		m.put("viewMode", viewMode);
		m.put("typeOP", "BC");
		m.put("adh", String.valueOf(d.get("adherent")));
		m.put("reglement", String.valueOf(d.get("reglement")));
		return m;
	}

	@Test
	public void detail_modification_prix_vide_ne_plante_plus() {
		Record d = detailExistant();
		Assume.assumeTrue(d != null);
		Result result = route(app, post("/Reglement-detail/save", formDetail(d, "EDIT", "id",
				String.valueOf(d.get("id")), "intitule", "Test", "quantite", "", "prixUnitaire", ""), "Admin"));
		assertEquals(303, result.status());
		assertTrue(result.flash().get("error").isPresent());
	}

	@Test
	public void detail_modification_valeur_non_numerique_ne_plante_plus() {
		Record d = detailExistant();
		Assume.assumeTrue(d != null);
		Result result = route(app, post("/Reglement-detail/save", formDetail(d, "EDIT", "id",
				String.valueOf(d.get("id")), "intitule", "Test", "quantite", "abc", "prixUnitaire", "12,5"), "Admin"));
		assertEquals(303, result.status());
		assertTrue(result.flash().get("error").isPresent());
	}

	@Test
	public void detail_saisie_multiple_prix_vide_rejete_sans_erreur() {
		Record d = detailExistant();
		Assume.assumeTrue(d != null);
		Result result = route(app, post("/Reglement-detail/save", formDetail(d, "CREATE",
				"intitules[1]", "Ligne test", "quantites[1]", "1", "prixUnitaires[1]", ""), "Admin"));
		assertEquals(303, result.status());
		assertFalse(result.flash().get("success").isPresent());
	}

	@Test
	public void detail_requete_incomplete_ne_plante_plus() {
		Result result = route(app, post("/Reglement-detail/save", form("viewMode", "EDIT"), "Admin"));
		assertEquals(303, result.status());
		assertTrue(result.flash().get("error").isPresent());
	}

	@Test
	public void detail_identifiant_inexistant_ne_plante_plus() {
		Record d = detailExistant();
		Assume.assumeTrue(d != null);
		Result result = route(app,
				post("/Reglement-detail/save", formDetail(d, "DELETE", "id", "999999999"), "Admin"));
		assertEquals(303, result.status());
		assertTrue(result.flash().get("error").isPresent());
	}

	// ===================== Suppression d'une ligne de détail =====================

	/** Bon dont l'adhérent a un plafond défini (sinon l'enregistrement est refusé en amont) */
	private Record bonAvecPlafond() {
		return db().connection().fetchOne("select r.id, r.adherent from reglement r"
				+ " join v_adherent v on v.id = r.adherent where r.on_deleted is false"
				+ " and v.total_credit_a_consomer is not null order by r.id desc limit 1");
	}

	private Long creerLigneTest(Long idReglement) {
		return db().connection().fetchOne("insert into reglement_detail (reglement, intitule, montant, quantite,"
				+ " prix_unitaire, on_deleted, who_done, when_done) values (?, 'TEST-REGRESSION', 100, 1, 100, false,"
				+ " 'test-regression', now()) returning id", idReglement).get(0, Long.class);
	}

	private void supprimerLigneTest(Long id) {
		db().connection().execute("delete from reglement_detail where id = ?", id);
	}

	@Test
	public void detail_suppression_effective() {
		Record bon = bonAvecPlafond();
		Assume.assumeTrue(bon != null);
		Long idLigne = creerLigneTest(bon.get("id", Long.class));
		try {
			Map<String, String> f = form("viewMode", "DELETE", "typeOP", "BC", "id", String.valueOf(idLigne),
					"adh", String.valueOf(bon.get("adherent")), "reglement", String.valueOf(bon.get("id")));
			Result result = route(app, post("/Reglement-detail/save", f, "Admin"));
			assertEquals(303, result.status());
			assertTrue(result.flash().get("success").isPresent());
			Record ligne = db().connection().fetchOne(
					"select on_deleted, montant, intitule from reglement_detail where id = ?", idLigne);
			assertTrue("ligne marquée supprimée", ligne.get("on_deleted", Boolean.class));
			assertEquals(Long.valueOf(0L), ligne.get("montant", Long.class));
			assertEquals("intitulé conservé", "TEST-REGRESSION", ligne.get("intitule", String.class));
		} finally {
			supprimerLigneTest(idLigne);
		}
	}

	@Test
	public void detail_suppression_ligne_dun_autre_bon_refusee() {
		Record bon = bonAvecPlafond();
		Assume.assumeTrue(bon != null);
		Record autreBon = db().connection().fetchOne("select r.id from reglement r join v_adherent v on v.id = r.adherent"
				+ " where r.on_deleted is false and v.total_credit_a_consomer is not null and r.id <> ? limit 1",
				bon.get("id", Long.class));
		Assume.assumeTrue(autreBon != null);
		Long idLigne = creerLigneTest(bon.get("id", Long.class));
		try {
			Map<String, String> f = form("viewMode", "DELETE", "typeOP", "BC", "id", String.valueOf(idLigne),
					"adh", String.valueOf(bon.get("adherent")), "reglement", String.valueOf(autreBon.get(0)));
			Result result = route(app, post("/Reglement-detail/save", f, "Admin"));
			assertEquals(303, result.status());
			assertTrue(result.flash().get("error").isPresent());
			Record ligne = db().connection().fetchOne(
					"select on_deleted, reglement from reglement_detail where id = ?", idLigne);
			assertFalse("ligne intacte", ligne.get("on_deleted", Boolean.class));
			assertEquals("toujours rattachée à son bon", bon.get("id", Long.class), ligne.get("reglement", Long.class));
		} finally {
			supprimerLigneTest(idLigne);
		}
	}

	@Test
	public void detail_page_envoie_bien_la_suppression() {
		Record bon = bonAvecPlafond();
		Assume.assumeTrue(bon != null);
		Result result = route(app, get("/Reglement-detail/form?subAction=CREATE&typeOperation=BC&idReglement="
				+ bon.get("id") + "&idAdherent=" + bon.get("adherent") + "&idDetails=0", "Admin"));
		assertEquals(200, result.status());
		String html = contentAsString(result);
		assertTrue(html.contains("document.getElementById('formViewMode').value = 'DELETE'"));
		assertFalse("plus de simple rechargement", html.contains("subAction=DELETE"));
	}

	// ===================== Tableau de bord =====================

	@Test
	public void tableauDeBord_admin() {
		assertEquals(200, route(app, get("/accueil", "Admin")).status());
		Result stats = route(app, get("/api/stats/reglements-structure?annee=" + currentGestion(), "Admin"));
		assertEquals(200, stats.status());
		assertTrue(contentAsString(stats).contains("\"totalBons\""));
		assertEquals(200, route(app, get("/api/stats/reglements-structure?annee=ALL&refresh=1", "Admin")).status());
		assertEquals(200, route(app, get("/api/stats/bons?annee=ALL&confirmes=0", "Admin")).status());
	}

	@Test
	public void tableauDeBord_interdit_hors_admin() {
		assertEquals(403, route(app, get("/api/stats/reglements-structure", "saisie")).status());
	}

	// ===================== Laboratoire =====================

	@Test
	public void laboratoire_percepteur() {
		assertEquals(303, route(app, get("/accueil", "Percepteur")).status());
		assertEquals(200, route(app, get("/laboratoire/caisse", "Percepteur")).status());
		assertEquals(200, route(app, get("/laboratoire/recu/nouveau", "Percepteur")).status());
		assertEquals(200, route(app, get("/laboratoire/types-examen/CREATE/?idExamen=0", "Percepteur")).status());
		Result json = route(app, get("/laboratoire/json/beneficiaires?q=ab", "Percepteur"));
		assertEquals(200, json.status());
		assertTrue(contentAsString(json).contains("\"options\""));
	}

	@Test
	public void laboratoire_recu_sans_examen_refuse() {
		Result result = route(app, post("/laboratoire/recu/save",
				form("typePatient", "EXTERNE", "nomPatient", "Test", "modePaiement", "ESPECES"), "Percepteur"));
		assertEquals(303, result.status());
		assertTrue(result.flash().get("error").isPresent());
	}

	@Test
	public void laboratoire_interdit_aux_autres_profils() {
		assertEquals(303, route(app, get("/laboratoire/caisse", "saisie")).status());
	}

	// ===================== Rôles & permissions =====================

	private services.RoleServices roles() {
		return app.injector().instanceOf(services.RoleServices.class);
	}

	@Test
	public void roles_droits_par_defaut_identiques_aux_droits_historiques() {
		services.RoleServices r = roles();
		assertTrue(r.peut("Admin", utils.Permissions.ROLES_GERER));
		assertTrue(r.peut("saisie", utils.Permissions.BON_CREER));
		assertTrue(r.peut("saisie", utils.Permissions.ADHERENT_LISTE));
		assertFalse(r.peut("saisie", utils.Permissions.ADHERENT_CREER));
		assertFalse(r.peut("saisie", utils.Permissions.TABLEAU_BORD));
		assertTrue(r.peut("Validateur", utils.Permissions.BON_VALIDER));
		assertFalse(r.peut("Validateur", utils.Permissions.BON_ARCHIVER));
		assertTrue(r.peut("Percepteur", utils.Permissions.LABO_RECU));
		assertFalse(r.peut("Percepteur", utils.Permissions.ADHERENT_LISTE));
		assertTrue(r.peut("Adherent", utils.Permissions.ADHERENT_ESPACE));
		assertFalse(r.peut("Adherent", utils.Permissions.ADHERENT_LISTE));
		assertFalse(r.peut("RoleInexistant", utils.Permissions.ADHERENT_LISTE));
	}

	@Test
	public void roles_ecran_reserve_a_la_gestion_des_roles() {
		assertEquals(200, route(app, get("/parametres/roles", "Admin")).status());
		Result refuse = route(app, get("/parametres/roles", "saisie"));
		assertEquals(303, refuse.status());
		assertEquals("/accueil", refuse.redirectLocation().get());
		assertEquals(200, route(app, get("/parametres/cotisations", "Admin")).status());
		assertEquals(303, route(app, get("/parametres/cotisations", "Validateur")).status());
		// contrôleur auparavant accessible à tout utilisateur, même non connecté
		assertEquals(303, route(app, get("/etude-consommations/import", "saisie")).status());
		assertEquals(303, route(app, get("/etude-consommations/import", null)).status());
	}

	@Test
	public void roles_cycle_de_vie_complet_d_un_role_personnalise() {
		String code = "TEST_REG_ROLE";
		db().connection().execute("delete from app_role where code = ?", code);
		try {
			Result cree = route(app, post("/parametres/roles/save",
					form("mode", "CREATE", "code", code, "libelle", "Rôle de test", "actif", "true"), "Admin"));
			assertEquals(303, cree.status());
			assertTrue(cree.flash().get("success").isPresent());

			Map<String, String> perms = form("code", code, "permissions[]", utils.Permissions.LABO_CAISSE);
			assertEquals(303, route(app, post("/parametres/roles/permissions", perms, "Admin")).status());

			// la permission accordée ouvre l'écran, les autres restent fermés
			assertEquals(200, route(app, get("/laboratoire/caisse", code)).status());
			assertEquals(303, route(app, get("/laboratoire/recu/nouveau", code)).status());
			assertEquals(303, route(app, get("/parametres/roles", code)).status());

			// rôle désactivé : plus aucun accès
			route(app, post("/parametres/roles/save", form("mode", "EDIT", "code", code, "libelle", "Rôle de test"), "Admin"));
			assertFalse(roles().estActif(code));
			assertEquals(303, route(app, get("/laboratoire/caisse", code)).status());

			// le rôle Admin ne peut pas perdre ses permissions
			assertTrue(roles().definirPermissions("Admin", new java.util.HashSet<>(), "test").contains("toujours"));

			Result supprime = route(app, post("/parametres/roles/delete", form("code", code), "Admin"));
			assertTrue(supprime.flash().get("success").isPresent());
			assertTrue(roles().findRole(code) == null);
		} finally {
			db().connection().execute("delete from app_role where code = ?", code);
		}
	}

	@Test
	public void utilisateurs_page_sans_fuite_du_mot_de_passe() {
		Record u = db().connection().fetchOne("select login from users where etat is true limit 1");
		Assume.assumeTrue(u != null);
		String login;
		try {
			login = java.net.URLEncoder.encode(u.get(0, String.class), "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		Result page = route(app, get("/user?viewMode=EDIT&login=" + login, "Admin"));
		assertEquals(200, page.status());
		String html = contentAsString(page);
		assertFalse("le hash BCrypt ne doit jamais être envoyé au navigateur", html.contains("$2a$"));
		assertTrue(html.contains("Percepteur (laboratoire)"));
	}

	@Test
	public void utilisateurs_modification_sans_mot_de_passe_conserve_le_hash() {
		Record u = db().connection().fetchOne("select id, login, droit, nom_prenom, passe, region_code, who_done, when_done"
				+ " from users where etat is true and droit = 'saisie' limit 1");
		Assume.assumeTrue(u != null);
		String hashAvant = u.get("passe", String.class);
		try {
			Result r = route(app, post("/user/save", form("viewMode", "EDIT", "id", String.valueOf(u.get("id")),
					"login", u.get("login", String.class), "nomPrenom", u.get("nom_prenom", String.class),
					"droit", u.get("droit", String.class), "passe", "", "etat", "true",
					"regionCode", u.get("region_code") == null ? "" : String.valueOf(u.get("region_code"))), "Admin"));
			assertEquals(303, r.status());
			assertTrue(r.flash().get("success").isPresent()); // ne bloque plus sur "existe déjà"
			String hashApres = db().connection().fetchOne("select passe from users where id = ?", u.get("id"))
					.get(0, String.class);
			assertEquals("mot de passe inchangé", hashAvant, hashApres);
		} finally {
			db().connection().execute("update users set who_done = ?, when_done = ? where id = ?", u.get("who_done"),
					u.get("when_done"), u.get("id"));
		}
	}

	// ===================== Cotisations =====================

	private services.CotisationServices cotisations() {
		return app.injector().instanceOf(services.CotisationServices.class);
	}

	@Test
	public void cotisation_nouveau_calcul_identique_aux_valeurs_enregistrees() {
		// le calcul paramétré doit reproduire exactement les taux existants (aucun écart)
		assertTrue(cotisations().controler().isEmpty());
	}

	@Test
	public void cotisation_ajout_puis_suppression_ayant_droit() {
		Record ad = db().connection().fetchOne("select id from adherent"
				+ " where coalesce(on_deleted,false) = false order by id desc limit 1");
		Assume.assumeTrue(ad != null);
		Long idAdherent = ad.get("id", Long.class);
		double avant = cotisations().detail(idAdherent).getTotal();
		try {
			Result ajout = route(app, post("/AyantDroit/save", form("viewMode", "CREATE", "nomAy", "TEST-REGRESSION-AY",
					"lien", "ENFANT", "genre", "MASCULIN", "adherent", String.valueOf(idAdherent), "tmpDate", "2015-05-10"),
					"Admin"));
			assertEquals(303, ajout.status());
			assertTrue(ajout.flash().get("success").get().contains("cotisation"));
			assertEquals(avant + 1.0, totalEnregistre(idAdherent), 0.0001);

			Long idAy = db().connection().fetchOne("select id from ayant_droit where nom_ay = 'TEST-REGRESSION-AY'"
					+ " and adherent = ? order by id desc limit 1", idAdherent).get(0, Long.class);
			Result suppression = route(app, post("/AyantDroit/save", form("viewMode", "DELETE", "id", String.valueOf(idAy),
					"adherent", String.valueOf(idAdherent), "nomAy", "x", "lien", "ENFANT"), "Admin"));
			assertEquals(303, suppression.status());
			Record ay = db().connection().fetchOne("select genre from ayant_droit where id = ?", idAy);
			assertEquals("le genre n'est plus écrasé par le lien", "MASCULIN", ay.get("genre", String.class));
			assertEquals(avant, totalEnregistre(idAdherent), 0.0001);

			// réactivation : le taux est de nouveau compté
			route(app, get("/AyantDroit/restaure/?idOrd=" + idAy, "Admin"));
			assertEquals(avant + 1.0, totalEnregistre(idAdherent), 0.0001);
		} finally {
			db().connection().execute("delete from ayant_droit where nom_ay = 'TEST-REGRESSION-AY'");
			cotisations().recalculer(idAdherent);
		}
		assertEquals(avant, totalEnregistre(idAdherent), 0.0001);
	}

	private double totalEnregistre(Long idAdherent) {
		return db().connection().fetchOne("select pourcentage_total_retenue from adherent where id = ?", idAdherent)
				.get(0, Double.class);
	}

	@Test
	public void cotisation_espace_adherent_limite_a_sa_fiche() {
		Record ad = db().connection().fetchOne("select id from adherent where coalesce(on_deleted,false) = false limit 1");
		Assume.assumeTrue(ad != null);
		// un adhérent (login = téléphone) ne peut pas ouvrir la fiche d'un autre adhérent
		Result r = route(app, get("/AyantDroit/show/CREATE/?idOrd=0&idConsult=" + ad.get(0), "Adherent"));
		assertEquals(303, r.status());
	}

	// ===================== Laboratoire : bénéficiaire et impression =====================

	@Test
	public void laboratoire_recherche_renvoie_les_ayants_droit() {
		Record ay = db().connection().fetchOne("select ad.matricule from ayant_droit ay join adherent ad on ad.id = ay.adherent"
				+ " where coalesce(ay.on_deleted,false) = false and coalesce(ad.on_deleted,false) = false"
				+ " and ad.matricule is not null and length(ad.matricule) >= 2 limit 1");
		Assume.assumeTrue(ay != null);
		Result json = route(app, get("/laboratoire/json/beneficiaires?q=" + ay.get(0, String.class), "Percepteur"));
		assertEquals(200, json.status());
		assertTrue(contentAsString(json).contains("\"ayantsDroit\":[{"));
	}

	@Test
	public void laboratoire_ayant_droit_d_un_autre_adherent_refuse() {
		Record ay = db().connection().fetchOne("select ay.id, ay.adherent from ayant_droit ay"
				+ " where coalesce(ay.on_deleted,false) = false limit 1");
		Record autre = ay == null ? null : db().connection().fetchOne("select id from adherent where id <> ? limit 1",
				ay.get("adherent", Long.class));
		Assume.assumeTrue(ay != null && autre != null);
		Result r = route(app, post("/laboratoire/recu/save", form("typePatient", "AYANT_DROIT",
				"idAdherent", String.valueOf(autre.get(0)), "idAyantDroit", String.valueOf(ay.get("id")),
				"examens[]", "999999", "modePaiement", "ESPECES"), "Percepteur"));
		assertEquals(303, r.status());
		assertTrue(r.flash().get("error").get().contains("n'appartient pas"));
	}

	// ===================== Rapports des opérations et des annulations =====================

	private services.RapportOperationsServices rapports() {
		return app.injector().instanceOf(services.RapportOperationsServices.class);
	}

	@Test
	public void rapport_operations_et_annulations_reserves_aux_admins() {
		Result jour = route(app, get("/rapports/operations", "Admin"));
		assertEquals(200, jour.status());
		String html = contentAsString(jour);
		assertTrue(html.contains("TYPES PRESTATION") || html.contains("Aucune opération"));
		assertEquals(200, route(app, get("/rapports/operations?periode=mois&mois=2026-01&type=PC&region=5", "Admin")).status());
		assertEquals(200, route(app, get("/rapports/annulations?periode=periode&debut=2000-01-01", "Admin")).status());
		assertEquals(303, route(app, get("/rapports/operations", "saisie")).status());
		assertEquals(303, route(app, get("/rapports/annulations", "Validateur")).status());
	}

	@Test
	public void rapport_contenu_conforme_aux_donnees() {
		java.time.LocalDate debut = java.time.LocalDate.of(2000, 1, 1), fin = java.time.LocalDate.now().plusDays(1);
		long annulesEnBase = db().connection().fetchOne("select count(*) from reglement where on_deleted is true")
				.get(0, Long.class);
		assertEquals(annulesEnBase, rapports().operations(debut, fin, true, "", null).size());
		long pcActives = db().connection().fetchOne("select count(*) from reglement where coalesce(on_deleted,false) = false"
				+ " and upper(type_reglement) = 'PC' and coalesce(when_done, date_payement) < ?",
				java.sql.Timestamp.valueOf(fin.plusDays(1).atStartOfDay())).get(0, Long.class);
		assertEquals(pcActives, rapports().operations(debut, fin, false, "PC", null).size());
		for (dto.OperationVue o : rapports().operations(debut, fin, false, "", 5L)) {
			assertEquals("filtre région de l'émetteur", "TAHOUA", o.getRegion());
			assertTrue(o.getBeneficiaire() != null && !o.getBeneficiaire().isEmpty());
		}
	}

	@Test
	public void rapport_export_excel() {
		Result csv = route(app, get("/rapports/export?rapport=annulations&periode=periode&debut=2000-01-01", "Admin"));
		assertEquals(200, csv.status());
		assertTrue(csv.contentType().get().contains("csv"));
		assertTrue(contentAsString(csv).contains("N°;TYPES PRESTATION;EMETTEUR;REGION;BENEFICIAIRE"));
	}

	@Test
	public void annulation_tracee_puis_restauration() {
		Record r = db().connection().fetchOne("select id, adherent, on_deleted from reglement"
				+ " where coalesce(on_deleted,false) = false order by id desc limit 1");
		Assume.assumeTrue(r != null);
		Long id = r.get("id", Long.class);
		Boolean etatInitial = r.get("on_deleted", Boolean.class);
		try {
			// annulation depuis l'écran des bons (AJAX) avec motif
			Http.RequestBuilder annul = new Http.RequestBuilder().method(POST)
					.uri("/reglement/soft-delete/" + id + "/" + r.get("adherent")).bodyForm(form("motif", "Doublon de saisie"))
					.header("X-Requested-With", "XMLHttpRequest");
			annul = session(CSRFTokenHelper.addCSRFToken(annul), "Admin");
			assertEquals(200, route(app, annul).status());
			dto.OperationVue o = rapports().findOperation(id);
			assertEquals("Doublon de saisie", o.getMotif());
			assertTrue(db().connection().fetchOne("select on_deleted from reglement where id = ?", id).get(0, Boolean.class));

			// restauration refusée sans la permission, puis acceptée pour l'Admin
			assertEquals(303, route(app, post("/rapports/annulations/restaurer/" + id, form("motif", "x"), "saisie")).status());
			assertTrue(db().connection().fetchOne("select on_deleted from reglement where id = ?", id).get(0, Boolean.class));
			Result res = route(app, post("/rapports/annulations/restaurer/" + id,
					form("motif", "Annulation par erreur", "retour", "periode=mois"), "Admin"));
			assertEquals(303, res.status());
			assertTrue(res.flash().get("success").isPresent());
			assertTrue(res.redirectLocation().get().endsWith("/rapports/annulations?periode=mois"));
			assertFalse(db().connection().fetchOne("select on_deleted from reglement where id = ?", id).get(0, Boolean.class));
			assertEquals(2L, (long) db().connection().fetchOne("select count(*) from reglement_annulation where reglement = ?"
					+ " and who_done = 'test-regression'", id).get(0, Long.class));

			// une restauration ne peut pas être rejouée
			assertTrue(rapports().changerEtat(id, false, null, "test-regression").contains("pas annulé"));
		} finally {
			db().connection().execute("update reglement set on_deleted = ? where id = ?", etatInitial, id);
			db().connection().execute("delete from reglement_annulation where reglement = ? and who_done = 'test-regression'", id);
		}
	}

	@Test
	public void restauration_retour_jamais_vers_une_adresse_externe() {
		Result res = route(app, post("/rapports/annulations/restaurer/999999999",
				form("retour", "https://exemple.invalide/piege"), "Admin"));
		assertEquals(303, res.status());
		assertTrue(res.redirectLocation().get().startsWith("/rapports/annulations"));
		assertTrue(res.flash().get("error").isPresent());
	}

	@Test
	public void laboratoire_impression_formats_ticket_et_a5() {
		Record recu = db().connection().fetchOne("select id from recu_labo order by id desc limit 1");
		Assume.assumeTrue(recu != null);
		Result page = route(app, get("/laboratoire/recu/imprimer/" + recu.get(0), "Percepteur"));
		assertEquals(200, page.status());
		String html = contentAsString(page);
		assertTrue(html.contains("size: 80mm auto"));
		assertTrue(html.contains("size: A5 portrait"));
		assertTrue(html.contains("max-width: 100mm"));
	}
}
