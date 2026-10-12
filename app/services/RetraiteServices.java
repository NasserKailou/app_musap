package services;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import dto.EcartCotisation;
import dto.ParametreRetraite;
import dto.TransitionRetraite;
import play.Logger;
import utils.IConnectionHelper;

/**
 * Prise en charge des retraités :
 * - 55 à 59 ans : cotisation de base 5 % (voir CotisationServices) ;
 * - 60 à 64 ans : retraité, seul l'adhérent est couvert (ayants droit archivés),
 *   plafond annuel fixe (vue v_adherent) ;
 * - 65 ans et plus : fin de couverture, adhérent archivé, aucune prestation.
 *
 * Les blocages (bons, prises en charge, laboratoire, ajout d'ayants droit) sont
 * calculés à partir de l'âge et s'appliquent immédiatement. L'archivage effectif
 * et la mise à jour des cotisations sont appliqués depuis l'écran Retraites
 * (aperçu puis validation), et tracés dans retraite_journal.
 *
 * @author nasser
 */
@Singleton
public class RetraiteServices {

	private static final Logger.ALogger logger = Logger.of(RetraiteServices.class);

	private static volatile RetraiteServices instance;

	private static final String[] DDL = {
			"CREATE TABLE IF NOT EXISTS retraite_journal (id BIGSERIAL PRIMARY KEY, adherent BIGINT NOT NULL"
					+ " REFERENCES adherent(id), action VARCHAR(40) NOT NULL, details VARCHAR(500), who_done VARCHAR(100),"
					+ " when_done TIMESTAMP NOT NULL DEFAULT now())",
			"CREATE INDEX IF NOT EXISTS idx_retraite_journal_adherent ON retraite_journal(adherent)" };

	private static final String AGE = "date_part('year', age(current_date, ad.date_naiss::date))::int";

	private final IConnectionHelper con;
	private final CotisationServices cotisations;
	private volatile boolean pret = false;
	private volatile ParametreRetraite cacheParametres;
	private volatile long cacheDate = 0L;

	@Inject
	public RetraiteServices(IConnectionHelper con, CotisationServices cotisations) {
		this.con = con;
		this.cotisations = cotisations;
		initialiser();
		instance = this;
	}

	public static RetraiteServices instance() {
		return instance;
	}

	public synchronized boolean initialiser() {
		if (pret)
			return true;
		try {
			for (String ddl : DDL)
				con.connection().execute(ddl);
			appliquerVueAdherent();
			pret = true;
		} catch (Exception e) {
			logger.error("Initialisation du module retraite impossible", e);
		}
		return pret;
	}

	/** Remplace v_adherent par la version tenant compte de l'âge (mêmes colonnes), une seule fois */
	private void appliquerVueAdherent() {
		Record def = con.connection().fetchOne("select pg_get_viewdef('v_adherent'::regclass, true)");
		if (def != null && def.get(0, String.class) != null && def.get(0, String.class).contains("parametre_retraite"))
			return;
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("sql/v_adherent_retraite.sql")) {
			if (in == null) {
				logger.error("Ressource sql/v_adherent_retraite.sql introuvable : plafonds des retraités non appliqués");
				return;
			}
			String sql;
			try (Scanner sc = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
				sql = sc.hasNext() ? sc.next() : "";
			}
			con.connection().execute(sql);
			logger.info("Vue v_adherent mise à jour (plafonds selon l'âge)");
		} catch (Exception e) {
			logger.error("Mise à jour de la vue v_adherent impossible", e);
		}
	}

	/** Paramètres de retraite (cache de 30 secondes : utilisés pour chaque ligne des listes) */
	public ParametreRetraite parametres() {
		long maintenant = System.currentTimeMillis();
		ParametreRetraite p = cacheParametres;
		if (p == null || maintenant - cacheDate > 30_000L) {
			p = cotisations.parametresRetraite();
			cacheParametres = p;
			cacheDate = maintenant;
		}
		return p;
	}

	public void viderCache() {
		cacheParametres = null;
	}

	public static String statut(Integer age) {
		RetraiteServices rs = instance;
		return (rs == null ? new ParametreRetraite() : rs.parametres()).statut(age);
	}

	/** Libellé du statut pour un âge (utilisé par les vues) */
	public static String libelleStatut(Integer age) {
		return ParametreRetraite.libelleStatut(statut(age));
	}

	// =====================================================================
	// Contrôles (appliqués immédiatement, à partir de l'âge)
	// =====================================================================

	/**
	 * Vérifie qu'une prestation (bon, prise en charge, examen) est possible.
	 *
	 * @return null si la prestation est autorisée, sinon le motif du refus
	 */
	public String verifierPrestation(Long idAdherent, Long idAyantDroit) {
		if (idAdherent == null)
			return "Adhérent introuvable";
		Record ad = con.connection().fetchOne("select " + AGE + " as age, coalesce(ad.on_deleted,false) as archive,"
				+ " trim(coalesce(ad.nom_ad,'') || ' ' || coalesce(ad.prenom_ad,'')) as nom from adherent ad where ad.id = ?",
				idAdherent);
		if (ad == null)
			return "Adhérent introuvable";
		ParametreRetraite p = parametres();
		Integer age = ad.get("age", Integer.class);
		String statut = p.statut(age);
		if (ParametreRetraite.FIN_COUVERTURE.equals(statut))
			return ad.get("nom", String.class) + " a " + age + " ans : fin de couverture à partir de "
					+ p.getAgeFinCouverture() + " ans, aucune prestation possible (ni bon, ni prise en charge)";
		if (Boolean.TRUE.equals(ad.get("archive", Boolean.class)))
			return ad.get("nom", String.class) + " est archivé : aucune prestation possible";
		if (idAyantDroit != null) {
			if (ParametreRetraite.RETRAITE.equals(statut))
				return ad.get("nom", String.class) + " est retraité (" + age + " ans) : à partir de " + p.getAgeRetraite()
						+ " ans seul l'adhérent est couvert, ses ayants droit ne le sont plus";
			Record ay = con.connection().fetchOne("select coalesce(on_deleted,false) as archive, adherent from ayant_droit"
					+ " where id = ?", idAyantDroit);
			if (ay == null || !idAdherent.equals(ay.get("adherent", Long.class)))
				return "Ayant droit introuvable pour cet adhérent";
			if (Boolean.TRUE.equals(ay.get("archive", Boolean.class)))
				return "Cet ayant droit est archivé : il n'est plus couvert";
		}
		return null;
	}

	/** Ajout / réactivation d'un ayant droit : impossible à partir de l'âge de la retraite */
	public String verifierAjoutAyantDroit(Long idAdherent) {
		Record ad = idAdherent == null ? null
				: con.connection().fetchOne("select " + AGE + " as age from adherent ad where ad.id = ?", idAdherent);
		if (ad == null)
			return "Adhérent introuvable";
		ParametreRetraite p = parametres();
		Integer age = ad.get("age", Integer.class);
		if (age != null && age >= p.getAgeRetraite())
			return "L'adhérent a " + age + " ans : à partir de " + p.getAgeRetraite()
					+ " ans seul l'adhérent est couvert, aucun ayant droit ne peut être ajouté ou réactivé";
		return null;
	}

	// =====================================================================
	// Aperçu et application des changements de situation
	// =====================================================================

	/** Changements à appliquer : fins de couverture, archivage des ayants droit, cotisations */
	public List<TransitionRetraite> apercu() {
		List<TransitionRetraite> res = new ArrayList<>();
		if (!initialiser())
			return res;
		ParametreRetraite p = parametres();
		Set<Long> dejaListes = new HashSet<>();
		for (Record r : con.connection().fetch("select ad.id, ad.matricule,"
				+ " trim(coalesce(ad.nom_ad,'') || ' ' || coalesce(ad.prenom_ad,'')) as nom, " + AGE + " as age,"
				+ " (select count(*) from ayant_droit ay where ay.adherent = ad.id and coalesce(ay.on_deleted,false) = false) as nb_ay"
				+ " from adherent ad where coalesce(ad.on_deleted,false) = false and " + AGE + " >= ?"
				+ " order by age desc, nom", p.getAgeRetraite())) {
			Integer age = r.get("age", Integer.class);
			int nbAy = r.get("nb_ay", Integer.class);
			String statut = p.statut(age);
			if (ParametreRetraite.FIN_COUVERTURE.equals(statut)) {
				res.add(new TransitionRetraite(r.get("id", Long.class), r.get("matricule", String.class),
						r.get("nom", String.class), age, statut, TransitionRetraite.FIN_COUVERTURE,
						"Archiver l'adhérent" + (nbAy > 0 ? " et ses " + nbAy + " ayant(s) droit" : ""), nbAy));
				dejaListes.add(r.get("id", Long.class));
			} else if (nbAy > 0) {
				res.add(new TransitionRetraite(r.get("id", Long.class), r.get("matricule", String.class),
						r.get("nom", String.class), age, statut, TransitionRetraite.ARCHIVAGE_AYANTS_DROIT,
						"Archiver " + nbAy + " ayant(s) droit (seul l'adhérent reste couvert)", nbAy));
				dejaListes.add(r.get("id", Long.class));
			}
		}
		// cotisations à mettre à jour pour les adhérents en pré-retraite / retraite
		for (EcartCotisation e : cotisations.controler()) {
			if (ParametreRetraite.ACTIF.equals(e.getStatut()) || dejaListes.contains(e.getIdAdherent()))
				continue;
			res.add(new TransitionRetraite(e.getIdAdherent(), e.getMatricule(), e.getNom(), null, e.getStatut(),
					TransitionRetraite.COTISATION,
					"Cotisation " + pct(e.getEnregistre()) + " % → " + pct(e.getCalcule()) + " %", e.getNbAyantsDroit()));
		}
		return res;
	}

	private static String pct(Double v) {
		return v == null ? "—" : String.format("%.2f", v).replace('.', ',');
	}

	/**
	 * Applique tous les changements de l'aperçu (transaction par adhérent) et
	 * recalcule les cotisations concernées.
	 *
	 * @return nombre de changements appliqués par type
	 */
	public synchronized Map<String, Integer> appliquer(String who) {
		Map<String, Integer> res = new LinkedHashMap<>();
		res.put(TransitionRetraite.FIN_COUVERTURE, 0);
		res.put(TransitionRetraite.ARCHIVAGE_AYANTS_DROIT, 0);
		res.put(TransitionRetraite.COTISATION, 0);
		for (TransitionRetraite t : apercu()) {
			try {
				Timestamp now = new Timestamp(System.currentTimeMillis());
				Double avant = cotisations.detail(t.getIdAdherent()) == null ? null
						: cotisations.detail(t.getIdAdherent()).getTotalEnregistre();
				if (!TransitionRetraite.COTISATION.equals(t.getAction())) {
					con.connection().transaction(cfg -> {
						DSLContext ctx = DSL.using(cfg);
						int nbAy = ctx.execute("update ayant_droit set on_deleted = true where adherent = ?"
								+ " and coalesce(on_deleted,false) = false", t.getIdAdherent());
						String details = nbAy + " ayant(s) droit archivé(s) à " + t.getAge() + " ans";
						if (TransitionRetraite.FIN_COUVERTURE.equals(t.getAction())) {
							ctx.execute("update adherent set on_deleted = true where id = ?", t.getIdAdherent());
							details = "Adhérent archivé à " + t.getAge() + " ans (fin de couverture)"
									+ (nbAy > 0 ? ", " + details : "");
						}
						ctx.execute("insert into retraite_journal (adherent, action, details, who_done, when_done)"
								+ " values (?, ?, ?, ?, ?)", t.getIdAdherent(), t.getAction(), details, who, now);
					});
				}
				Double apres = cotisations.recalculer(t.getIdAdherent());
				if (apres != null && (avant == null || Math.abs(avant - apres) > 0.0001))
					con.connection().execute("insert into retraite_journal (adherent, action, details, who_done, when_done)"
							+ " values (?, ?, ?, ?, ?)", t.getIdAdherent(), TransitionRetraite.COTISATION,
							"Cotisation " + pct(avant) + " % → " + pct(apres) + " %", who, now);
				res.merge(t.getAction(), 1, Integer::sum);
			} catch (Exception e) {
				logger.error("Application des règles de retraite impossible pour l'adhérent " + t.getIdAdherent(), e);
			}
		}
		return res;
	}

	/** Derniers changements appliqués */
	public List<Map<String, Object>> journal(int limite) {
		List<Map<String, Object>> res = new ArrayList<>();
		if (!initialiser())
			return res;
		for (Record r : con.connection().fetch("select j.when_done, j.action, j.details, j.who_done, ad.matricule,"
				+ " trim(coalesce(ad.nom_ad,'') || ' ' || coalesce(ad.prenom_ad,'')) as nom from retraite_journal j"
				+ " join adherent ad on ad.id = j.adherent order by j.when_done desc, j.id desc limit ?", limite)) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("date", r.get("when_done", Timestamp.class));
			m.put("action", r.get("action", String.class));
			m.put("details", r.get("details", String.class));
			m.put("auteur", r.get("who_done", String.class));
			m.put("matricule", r.get("matricule", String.class));
			m.put("nom", r.get("nom", String.class));
			res.add(m);
		}
		return res;
	}
}
