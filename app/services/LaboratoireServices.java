package services;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import dto.RecuLaboVue;
import models.tables.pojos.RecuLabo;
import models.tables.pojos.RecuLaboDetail;
import models.tables.pojos.TypeExamen;
import play.Logger;
import play.libs.Json;
import utils.IConnectionHelper;
import utils.LaboUtils;

/**
 * Service du module Laboratoire (profil Percepteur) : paramétrage des types
 * d'examens, émission / annulation des reçus, journal de caisse et statistiques.
 *
 * Les tables du module ne font pas partie du code jOOQ généré : on utilise
 * donc le SQL avec paramètres liés et fetchInto vers les POJOs générés (models.tables.pojos).
 *
 * @author nasser
 */
@Singleton
public class LaboratoireServices {

	private static final Logger.ALogger logger = Logger.of(LaboratoireServices.class);

	public static final String[] MODES_PAIEMENT = { "ESPECES", "MOBILE MONEY", "CHEQUE", "VIREMENT" };

	/** DDL idempotent : identique à db/patch_laboratoire_20261003.sql */
	private static final String[] DDL = {
			"CREATE TABLE IF NOT EXISTS type_examen (id BIGSERIAL PRIMARY KEY, code VARCHAR(30), libelle VARCHAR(255) NOT NULL,"
					+ " categorie VARCHAR(100), montant BIGINT NOT NULL DEFAULT 0, montant_adherent BIGINT,"
					+ " on_deleted BOOLEAN NOT NULL DEFAULT FALSE, who_done VARCHAR(100), when_done TIMESTAMP DEFAULT now())",
			"CREATE TABLE IF NOT EXISTS recu_labo (id BIGSERIAL PRIMARY KEY, numero_recu VARCHAR(30) UNIQUE,"
					+ " type_patient VARCHAR(20) NOT NULL, adherent BIGINT, ayant_droit BIGINT, nom_patient VARCHAR(255) NOT NULL,"
					+ " telephone VARCHAR(50), structure_origine VARCHAR(255), montant_total BIGINT NOT NULL DEFAULT 0,"
					+ " mode_paiement VARCHAR(30), date_recu TIMESTAMP NOT NULL DEFAULT now(), is_annule BOOLEAN NOT NULL DEFAULT FALSE,"
					+ " motif_annulation VARCHAR(500), who_annule VARCHAR(100), when_annule TIMESTAMP, who_done VARCHAR(100),"
					+ " when_done TIMESTAMP DEFAULT now())",
			"CREATE TABLE IF NOT EXISTS recu_labo_detail (id BIGSERIAL PRIMARY KEY, recu_labo BIGINT NOT NULL REFERENCES recu_labo(id),"
					+ " type_examen BIGINT REFERENCES type_examen(id), libelle_examen VARCHAR(255) NOT NULL,"
					+ " prix_unitaire BIGINT NOT NULL DEFAULT 0, quantite INTEGER NOT NULL DEFAULT 1, montant BIGINT NOT NULL DEFAULT 0)",
			"CREATE INDEX IF NOT EXISTS idx_recu_labo_date ON recu_labo(date_recu)",
			"CREATE INDEX IF NOT EXISTS idx_recu_labo_detail_recu ON recu_labo_detail(recu_labo)",
			"CREATE INDEX IF NOT EXISTS idx_reglement_date_payement ON reglement(date_payement)",
			"CREATE INDEX IF NOT EXISTS idx_reglement_detail_reglement ON reglement_detail(reglement)" };

	private static final String SELECT_RECU = "select r.*, ad.matricule,"
			+ " trim(coalesce(ad.nom_ad,'') || ' ' || coalesce(ad.prenom_ad,'')) as adherent_principal, ay.lien"
			+ " from recu_labo r left join adherent ad on ad.id = r.adherent"
			+ " left join ayant_droit ay on ay.id = r.ayant_droit";

	private final IConnectionHelper con;
	private volatile boolean schemaOk = false;

	@Inject
	public LaboratoireServices(IConnectionHelper con) {
		this.con = con;
		ensureSchema();
	}

	/**
	 * Crée les tables du module si elles n'existent pas encore
	 */
	public synchronized boolean ensureSchema() {
		if (schemaOk)
			return true;
		try {
			for (String ddl : DDL) {
				con.connection().execute(ddl);
			}
			schemaOk = true;
		} catch (Exception e) {
			logger.error("Impossible d'initialiser les tables du module laboratoire", e);
		}
		return schemaOk;
	}

	// =====================================================================
	// TYPES D'EXAMENS
	// =====================================================================

	public List<TypeExamen> listTypesExamen() {
		if (!ensureSchema())
			return new ArrayList<>();
		return con.connection()
				.fetch("select * from type_examen where on_deleted is false order by categorie nulls last, libelle")
				.into(TypeExamen.class);
	}

	public TypeExamen findTypeExamen(Long id) {
		if (id == null || !ensureSchema())
			return null;
		Record r = con.connection().fetchOne("select * from type_examen where id = ?", id);
		return r == null ? null : r.into(TypeExamen.class);
	}

	public List<String> listCategories() {
		List<String> res = new ArrayList<>();
		if (!ensureSchema())
			return res;
		for (Record r : con.connection().fetch("select distinct categorie from type_examen"
				+ " where on_deleted is false and categorie is not null and trim(categorie) <> '' order by 1")) {
			res.add(r.get(0, String.class));
		}
		return res;
	}

	public String saveTypeExamen(TypeExamen t, boolean creation) {
		try {
			if (t.getLibelle() == null || t.getLibelle().trim().isEmpty())
				return "Le libellé est obligatoire";
			if (t.getMontant() == null || t.getMontant() < 0)
				return "Le montant est obligatoire et doit être positif";
			if (t.getMontantAdherent() != null && t.getMontantAdherent() < 0)
				return "Le montant adhérent doit être positif";
			Record doublon = con.connection().fetchOne(
					"select id from type_examen where on_deleted is false and upper(trim(libelle)) = upper(trim(?)) and id <> ?",
					t.getLibelle(), creation || t.getId() == null ? -1L : t.getId());
			if (doublon != null)
				return "Un examen portant le libellé '" + t.getLibelle() + "' existe déjà";
			if (creation) {
				con.connection().execute("insert into type_examen (code, libelle, categorie, montant, montant_adherent,"
						+ " on_deleted, who_done, when_done) values (?, ?, ?, ?, ?, false, ?, ?)", trim(t.getCode()),
						t.getLibelle().trim(), trim(t.getCategorie()), t.getMontant(), t.getMontantAdherent(),
						t.getWhoDone(), now());
			} else {
				int n = con.connection().execute("update type_examen set code = ?, libelle = ?, categorie = ?, montant = ?,"
						+ " montant_adherent = ?, who_done = ?, when_done = ? where id = ?", trim(t.getCode()),
						t.getLibelle().trim(), trim(t.getCategorie()), t.getMontant(), t.getMontantAdherent(),
						t.getWhoDone(), now(), t.getId());
				if (n == 0)
					return "Examen introuvable";
			}
			return "ok";
		} catch (Exception e) {
			logger.error("Erreur enregistrement type examen", e);
			return e.getMessage();
		}
	}

	public String deleteTypeExamen(Long id, String who) {
		try {
			con.connection().execute("update type_examen set on_deleted = true, who_done = ?, when_done = ? where id = ?",
					who, now(), id);
			return "ok";
		} catch (Exception e) {
			logger.error("Erreur suppression type examen", e);
			return e.getMessage();
		}
	}

	// =====================================================================
	// BÉNÉFICIAIRES (adhérents / ayants droit)
	// =====================================================================

	/**
	 * Recherche d'adhérents (nom, prénom, matricule, code carnet, ou nom d'un de leurs
	 * ayants droit) avec la liste de leurs ayants droit actifs, pour choisir le
	 * bénéficiaire de l'examen : l'adhérent lui-même ou un de ses ayants droit.
	 */
	public List<Map<String, Object>> rechercherBeneficiaires(String query) {
		List<Map<String, Object>> res = new ArrayList<>();
		String q = query == null ? "" : query.trim();
		if (q.length() < 2)
			return res;
		String like = "%" + q + "%";
		Map<Long, Map<String, Object>> parId = new LinkedHashMap<>();
		for (Record r : con.connection().fetch("select ad.id, trim(coalesce(ad.nom_ad,'') || ' ' || coalesce(ad.prenom_ad,'')) as nom,"
				+ " ad.matricule, ad.code_carnet, ad.telephone, ad.sexe from adherent ad"
				+ " where coalesce(ad.on_deleted,false) = false"
				+ " and (coalesce(ad.nom_ad,'') || ' ' || coalesce(ad.prenom_ad,'') ilike ? or ad.matricule ilike ?"
				+ " or ad.code_carnet ilike ? or exists (select 1 from ayant_droit ay where ay.adherent = ad.id"
				+ " and coalesce(ay.on_deleted,false) = false and ay.nom_ay ilike ?))"
				+ " order by nom limit 20", like, like, like, like)) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("idAdherent", r.get("id", Long.class));
			m.put("nom", r.get("nom", String.class));
			m.put("matricule", r.get("matricule", String.class));
			m.put("codeCarnet", r.get("code_carnet", String.class));
			m.put("telephone", r.get("telephone", String.class));
			m.put("sexe", r.get("sexe", String.class));
			m.put("ayantsDroit", new ArrayList<Map<String, Object>>());
			parId.put(r.get("id", Long.class), m);
			res.add(m);
		}
		if (parId.isEmpty())
			return res;
		StringBuilder in = new StringBuilder();
		for (int i = 0; i < parId.size(); i++)
			in.append(i == 0 ? "?" : ",?");
		String qMin = q.toLowerCase();
		for (Record r : con.connection().fetch("select id, adherent, nom_ay, upper(trim(lien)) as lien, genre,"
				+ " date_part('year', age(date_naiss))::int as age from ayant_droit"
				+ " where coalesce(on_deleted,false) = false and adherent in (" + in + ")"
				+ " order by adherent, date_naiss nulls last, id", parId.keySet().toArray())) {
			Map<String, Object> ay = new LinkedHashMap<>();
			ay.put("id", r.get("id", Long.class));
			ay.put("nom", r.get("nom_ay", String.class));
			ay.put("lien", r.get("lien", String.class));
			ay.put("genre", r.get("genre", String.class));
			ay.put("age", r.get("age", Integer.class));
			String nom = r.get("nom_ay", String.class);
			// l'ayant droit dont le nom correspond à la recherche est présélectionné
			ay.put("correspond", nom != null && nom.toLowerCase().contains(qMin));
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> liste = (List<Map<String, Object>>) parId.get(r.get("adherent", Long.class))
					.get("ayantsDroit");
			liste.add(ay);
		}
		return res;
	}

	// =====================================================================
	// REÇUS
	// =====================================================================

	/**
	 * Crée un reçu avec ses examens. Les montants sont TOUJOURS recalculés à
	 * partir des tarifs en base (jamais à partir des montants envoyés par le navigateur).
	 *
	 * @return l'id du reçu créé
	 * @throws IllegalArgumentException si les données sont invalides
	 */
	public synchronized Long creerRecu(RecuLabo recu, List<Long> examens, List<Integer> quantites, String who) {
		if (!ensureSchema())
			throw new IllegalStateException("Tables du module laboratoire indisponibles");
		if (examens == null || examens.isEmpty())
			throw new IllegalArgumentException("Veuillez sélectionner au moins un examen");

		// ---- Identification du patient ----
		String type = recu.getTypePatient() == null ? "" : recu.getTypePatient().trim().toUpperCase();
		if (LaboUtils.TYPE_ADHERENT.equals(type)) {
			Record ad = recu.getAdherent() == null ? null
					: con.connection().fetchOne("select nom_ad, prenom_ad, telephone from adherent"
							+ " where id = ? and coalesce(on_deleted,false) = false", recu.getAdherent());
			if (ad == null)
				throw new IllegalArgumentException("Adhérent introuvable, veuillez le rechercher à nouveau");
			recu.setAyantDroit(null);
			recu.setNomPatient((nvl(ad.get("nom_ad", String.class)) + " " + nvl(ad.get("prenom_ad", String.class))).trim());
			if (isBlank(recu.getTelephone()))
				recu.setTelephone(ad.get("telephone", String.class));
			recu.setStructureOrigine("MUSAPOSTE");
		} else if (LaboUtils.TYPE_AYANT_DROIT.equals(type)) {
			Record ay = recu.getAyantDroit() == null ? null
					: con.connection().fetchOne("select ay.nom_ay, ay.adherent, ad.telephone from ayant_droit ay"
							+ " join adherent ad on ad.id = ay.adherent where ay.id = ? and coalesce(ay.on_deleted,false) = false",
							recu.getAyantDroit());
			if (ay == null)
				throw new IllegalArgumentException("Ayant droit introuvable, veuillez le rechercher à nouveau");
			if (recu.getAdherent() != null && !recu.getAdherent().equals(ay.get("adherent", Long.class)))
				throw new IllegalArgumentException("L'ayant droit choisi n'appartient pas à l'adhérent sélectionné");
			recu.setAdherent(ay.get("adherent", Long.class));
			recu.setNomPatient(ay.get("nom_ay", String.class));
			if (isBlank(recu.getTelephone()))
				recu.setTelephone(ay.get("telephone", String.class));
			recu.setStructureOrigine("MUSAPOSTE");
		} else if (LaboUtils.TYPE_EXTERNE.equals(type)) {
			if (isBlank(recu.getNomPatient()))
				throw new IllegalArgumentException("Le nom du patient est obligatoire");
			recu.setAdherent(null);
			recu.setAyantDroit(null);
		} else {
			throw new IllegalArgumentException("Type de patient invalide");
		}
		recu.setTypePatient(type);
		boolean membre = !LaboUtils.TYPE_EXTERNE.equals(type);

		// ---- Calcul des lignes à partir des tarifs en base ----
		Map<Long, TypeExamen> tarifs = new HashMap<>();
		for (TypeExamen t : listTypesExamen())
			tarifs.put(t.getId(), t);

		List<RecuLaboDetail> lignes = new ArrayList<>();
		long total = 0;
		for (int i = 0; i < examens.size(); i++) {
			Long idExamen = examens.get(i);
			if (idExamen == null)
				continue;
			TypeExamen t = tarifs.get(idExamen);
			if (t == null)
				throw new IllegalArgumentException("Examen inconnu ou supprimé (id " + idExamen + ")");
			int qte = (quantites != null && i < quantites.size() && quantites.get(i) != null) ? quantites.get(i) : 1;
			if (qte < 1 || qte > 100)
				throw new IllegalArgumentException("Quantité invalide pour l'examen " + t.getLibelle());
			RecuLaboDetail d = new RecuLaboDetail();
			d.setTypeExamen(t.getId());
			d.setLibelleExamen(t.getLibelle());
			d.setPrixUnitaire(LaboUtils.tarif(t, membre));
			d.setQuantite(qte);
			d.setMontant(d.getPrixUnitaire() * qte);
			total += d.getMontant();
			lignes.add(d);
		}
		if (lignes.isEmpty())
			throw new IllegalArgumentException("Veuillez sélectionner au moins un examen");

		String mode = isBlank(recu.getModePaiement()) ? "ESPECES" : recu.getModePaiement().trim().toUpperCase();
		final long montantTotal = total;
		final Timestamp maintenant = now();
		final Long[] idRecu = new Long[1];

		// ---- Enregistrement atomique en-tête + lignes ----
		con.connection().transaction(cfg -> {
			DSLContext ctx = DSL.using(cfg);
			Record r = ctx.fetchOne("insert into recu_labo (type_patient, adherent, ayant_droit, nom_patient, telephone,"
					+ " structure_origine, montant_total, mode_paiement, date_recu, is_annule, who_done, when_done)"
					+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?, ?) returning id", recu.getTypePatient(),
					recu.getAdherent(), recu.getAyantDroit(), recu.getNomPatient().trim(), trim(recu.getTelephone()),
					trim(recu.getStructureOrigine()), montantTotal, mode, maintenant, who, maintenant);
			Long id = r.get(0, Long.class);
			String numero = String.format("LAB%d-%06d", maintenant.toLocalDateTime().getYear(), id);
			ctx.execute("update recu_labo set numero_recu = ? where id = ?", numero, id);
			for (RecuLaboDetail d : lignes) {
				ctx.execute("insert into recu_labo_detail (recu_labo, type_examen, libelle_examen, prix_unitaire, quantite, montant)"
						+ " values (?, ?, ?, ?, ?, ?)", id, d.getTypeExamen(), d.getLibelleExamen(), d.getPrixUnitaire(),
						d.getQuantite(), d.getMontant());
			}
			idRecu[0] = id;
		});
		return idRecu[0];
	}

	public RecuLaboVue findRecu(Long id) {
		if (id == null || !ensureSchema())
			return null;
		Record r = con.connection().fetchOne(SELECT_RECU + " where r.id = ?", id);
		return r == null ? null : r.into(RecuLaboVue.class);
	}

	public List<RecuLaboDetail> detailsRecu(Long idRecu) {
		return con.connection().fetch("select * from recu_labo_detail where recu_labo = ? order by id", idRecu)
				.into(RecuLaboDetail.class);
	}

	/**
	 * Reçus émis entre deux dates incluses
	 */
	public List<RecuLaboVue> listRecus(LocalDate debut, LocalDate fin) {
		if (!ensureSchema())
			return new ArrayList<>();
		return con.connection().fetch(SELECT_RECU + " where r.date_recu >= ? and r.date_recu < ? order by r.id desc limit 5000",
				debutJour(debut), debutJour(fin.plusDays(1))).into(RecuLaboVue.class);
	}

	public String annulerRecu(Long id, String motif, String who) {
		if (isBlank(motif))
			return "Le motif d'annulation est obligatoire";
		try {
			int n = con.connection().execute("update recu_labo set is_annule = true, motif_annulation = ?, who_annule = ?,"
					+ " when_annule = ? where id = ? and is_annule is false", motif.trim(), who, now(), id);
			return n == 1 ? "ok" : "Reçu introuvable ou déjà annulé";
		} catch (Exception e) {
			logger.error("Erreur annulation reçu labo", e);
			return e.getMessage();
		}
	}

	// =====================================================================
	// CAISSE & STATISTIQUES
	// =====================================================================

	/**
	 * Situation de caisse sur une période : totaux, par examen, par mode de paiement, par jour
	 */
	public ObjectNode situationCaisse(LocalDate debut, LocalDate fin) {
		ObjectNode res = Json.newObject();
		if (!ensureSchema()) {
			res.put("disponible", false);
			return res;
		}
		Timestamp d = debutJour(debut);
		Timestamp f = debutJour(fin.plusDays(1));
		String periode = " r.date_recu >= ? and r.date_recu < ?";

		Record g = con.connection().fetchOne("select count(*) filter (where not r.is_annule) as nb,"
				+ " coalesce(sum(r.montant_total) filter (where not r.is_annule),0) as total,"
				+ " count(*) filter (where r.is_annule) as nb_annules,"
				+ " coalesce(sum(r.montant_total) filter (where r.is_annule),0) as total_annules,"
				+ " coalesce(sum(r.montant_total) filter (where not r.is_annule and r.type_patient <> 'EXTERNE'),0) as total_membres,"
				+ " coalesce(sum(r.montant_total) filter (where not r.is_annule and r.type_patient = 'EXTERNE'),0) as total_externes,"
				+ " count(*) filter (where not r.is_annule and r.type_patient <> 'EXTERNE') as nb_membres,"
				+ " count(*) filter (where not r.is_annule and r.type_patient = 'EXTERNE') as nb_externes"
				+ " from recu_labo r where" + periode, d, f);
		res.put("disponible", true);
		for (String k : new String[] { "nb", "total", "nb_annules", "total_annules", "total_membres", "total_externes",
				"nb_membres", "nb_externes" }) {
			res.put(camel(k), l(g, k));
		}

		ArrayNode examens = Json.newArray();
		for (Record r : con.connection().fetch("select d.libelle_examen, sum(d.quantite) as qte, sum(d.montant) as montant"
				+ " from recu_labo_detail d join recu_labo r on r.id = d.recu_labo"
				+ " where not r.is_annule and" + periode + " group by d.libelle_examen order by montant desc", d, f)) {
			ObjectNode o = Json.newObject();
			o.put("examen", r.get("libelle_examen", String.class));
			o.put("quantite", l(r, "qte"));
			o.put("montant", l(r, "montant"));
			examens.add(o);
		}
		res.set("parExamen", examens);

		ArrayNode modes = Json.newArray();
		for (Record r : con.connection().fetch("select coalesce(r.mode_paiement,'N/R') as mode, count(*) as nb,"
				+ " sum(r.montant_total) as montant from recu_labo r where not r.is_annule and" + periode
				+ " group by 1 order by 3 desc", d, f)) {
			ObjectNode o = Json.newObject();
			o.put("mode", r.get("mode", String.class));
			o.put("nb", l(r, "nb"));
			o.put("montant", l(r, "montant"));
			modes.add(o);
		}
		res.set("parModePaiement", modes);

		ArrayNode jours = Json.newArray();
		for (Record r : con.connection().fetch("select to_char(r.date_recu::date,'DD/MM') as jour, count(*) as nb,"
				+ " sum(r.montant_total) as montant from recu_labo r where not r.is_annule and" + periode
				+ " group by r.date_recu::date order by r.date_recu::date", d, f)) {
			ObjectNode o = Json.newObject();
			o.put("jour", r.get("jour", String.class));
			o.put("nb", l(r, "nb"));
			o.put("montant", l(r, "montant"));
			jours.add(o);
		}
		res.set("parJour", jours);
		return res;
	}

	/**
	 * Indicateurs du laboratoire pour le tableau de bord administrateur
	 */
	public ObjectNode statistiquesDashboard(String annee) {
		ObjectNode res = Json.newObject();
		if (!ensureSchema()) {
			res.put("disponible", false);
			return res;
		}
		try {
			boolean filtre = annee != null && !annee.isEmpty();
			String w = " where not r.is_annule" + (filtre ? " and extract(year from r.date_recu) = ?" : "");
			Object[] b = filtre ? new Object[] { Integer.valueOf(annee) } : new Object[0];

			Record g = con.connection().fetchOne("select count(*) as nb, coalesce(sum(r.montant_total),0) as total,"
					+ " coalesce(sum(r.montant_total) filter (where r.type_patient <> 'EXTERNE'),0) as total_membres,"
					+ " coalesce(sum(r.montant_total) filter (where r.type_patient = 'EXTERNE'),0) as total_externes,"
					+ " count(*) filter (where r.type_patient <> 'EXTERNE') as nb_membres,"
					+ " count(*) filter (where r.type_patient = 'EXTERNE') as nb_externes"
					+ " from recu_labo r" + w, b);
			res.put("disponible", true);
			res.put("nbRecus", l(g, "nb"));
			res.put("recette", l(g, "total"));
			res.put("recetteMembres", l(g, "total_membres"));
			res.put("recetteExternes", l(g, "total_externes"));
			res.put("nbMembres", l(g, "nb_membres"));
			res.put("nbExternes", l(g, "nb_externes"));

			ArrayNode mois = Json.newArray();
			if (filtre) {
				long[] m = new long[12];
				for (Record r : con.connection().fetch("select extract(month from r.date_recu)::int as m,"
						+ " sum(r.montant_total) as montant from recu_labo r" + w + " group by 1", b)) {
					Integer i = r.get("m", Integer.class);
					if (i != null && i >= 1 && i <= 12)
						m[i - 1] = l(r, "montant");
				}
				for (long v : m)
					mois.add(v);
			}
			res.set("recetteParMois", mois);

			ArrayNode top = Json.newArray();
			for (Record r : con.connection().fetch("select d.libelle_examen, sum(d.quantite) as qte, sum(d.montant) as montant"
					+ " from recu_labo_detail d join recu_labo r on r.id = d.recu_labo" + w
					+ " group by d.libelle_examen order by qte desc limit 10", b)) {
				ObjectNode o = Json.newObject();
				o.put("examen", r.get("libelle_examen", String.class));
				o.put("quantite", l(r, "qte"));
				o.put("montant", l(r, "montant"));
				top.add(o);
			}
			res.set("topExamens", top);
		} catch (Exception e) {
			logger.error("Erreur statistiques laboratoire", e);
			res.put("disponible", false);
		}
		return res;
	}

	public List<String> listAnnees() {
		List<String> res = new ArrayList<>();
		if (!ensureSchema())
			return res;
		try {
			for (Record r : con.connection()
					.fetch("select distinct extract(year from date_recu)::int as a from recu_labo order by 1 desc")) {
				res.add(String.valueOf(r.get("a", Integer.class)));
			}
		} catch (Exception e) {
			logger.error("Erreur années laboratoire", e);
		}
		return res;
	}

	// =====================================================================
	// Utilitaires
	// =====================================================================

	private static Timestamp now() {
		return new Timestamp(System.currentTimeMillis());
	}

	private static Timestamp debutJour(LocalDate d) {
		return Timestamp.valueOf(d.atStartOfDay());
	}

	private static long l(Record r, String f) {
		Long v = r.get(f, Long.class);
		return v == null ? 0L : v;
	}

	private static String camel(String s) {
		StringBuilder sb = new StringBuilder();
		boolean up = false;
		for (char c : s.toCharArray()) {
			if (c == '_') {
				up = true;
			} else {
				sb.append(up ? Character.toUpperCase(c) : c);
				up = false;
			}
		}
		return sb.toString();
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static String trim(String s) {
		return isBlank(s) ? null : s.trim();
	}

	private static String nvl(String s) {
		return s == null ? "" : s;
	}
}
