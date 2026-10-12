package services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import dto.CotisationAdherent;
import dto.EcartCotisation;
import dto.ParametreRetraite;
import dto.TauxCotisation;
import play.Logger;
import utils.IConnectionHelper;

/**
 * Calcul des parts de cotisation (en % du salaire) d'un adhérent :
 * part de base de l'adhérent + part de chaque ayant droit actif selon son lien
 * (conjoint, enfant, parent...), dans la limite éventuelle d'un nombre maximum
 * d'ayants droit pris en compte par lien.
 *
 * La part de base dépend de l'âge (voir ParametreRetraite) :
 * - avant 55 ans : taux ADHERENT (2,5 %) ;
 * - de 55 à 59 ans : taux PRE_RETRAITE (5 %), pour financer la couverture après la retraite ;
 * - à partir de 60 ans : taux RETRAITE (0 %, plus de salaire) et ayants droit non couverts.
 *
 * Le total est TOUJOURS recalculé entièrement à partir des ayants droit actifs
 * (plus de mise à jour par écarts successifs, source de dérives : réactivation
 * non prise en compte, modifications concurrentes, valeur obsolète du formulaire).
 *
 * @author nasser
 */
@Singleton
public class CotisationServices {

	private static final Logger.ALogger logger = Logger.of(CotisationServices.class);

	private static final String[] DDL = {
			"CREATE TABLE IF NOT EXISTS taux_cotisation (code VARCHAR(30) PRIMARY KEY, libelle VARCHAR(100) NOT NULL,"
					+ " taux NUMERIC(6,3) NOT NULL DEFAULT 0, nombre_max INTEGER, ordre INTEGER NOT NULL DEFAULT 0,"
					+ " who_done VARCHAR(100), when_done TIMESTAMP DEFAULT now())",
			"CREATE TABLE IF NOT EXISTS parametre_retraite (id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),"
					+ " age_pre_retraite INTEGER NOT NULL DEFAULT 55, age_retraite INTEGER NOT NULL DEFAULT 60,"
					+ " age_fin_couverture INTEGER NOT NULL DEFAULT 65, plafond_retraite BIGINT NOT NULL DEFAULT 500000,"
					+ " seuil_alerte_retraite BIGINT NOT NULL DEFAULT 250000, who_done VARCHAR(100),"
					+ " when_done TIMESTAMP DEFAULT now())",
			"INSERT INTO parametre_retraite (id, who_done) VALUES (1, 'initialisation') ON CONFLICT (id) DO NOTHING" };

	/** Valeurs historiques (codées en dur avant ce paramétrage) */
	private static final Object[][] TAUX_INITIAUX = { { TauxCotisation.CODE_ADHERENT, "Adhérent (part de base)", 2.5, 0 },
			{ "CONJOINT", "Conjoint(e)", 1.5, 1 }, { "ENFANT", "Enfant", 1.0, 2 }, { "PARENT", "Parent", 3.0, 3 } };

	/** Parts de base liées à la retraite (ajoutées même si la table existe déjà) */
	private static final Object[][] TAUX_RETRAITE = {
			{ TauxCotisation.CODE_PRE_RETRAITE, "Adhérent de 55 à 59 ans (pré-retraite)", 5.0, 0 },
			{ TauxCotisation.CODE_RETRAITE, "Adhérent retraité (60 ans et plus)", 0.0, 0 } };

	/** Âge en années révolues, calculé par la base */
	private static final String AGE = "date_part('year', age(current_date, date_naiss::date))::int";

	private final IConnectionHelper con;
	private volatile boolean pret = false;

	@Inject
	public CotisationServices(IConnectionHelper con) {
		this.con = con;
		initialiser();
	}

	public synchronized boolean initialiser() {
		if (pret)
			return true;
		try {
			for (String ddl : DDL)
				con.connection().execute(ddl);
			Record n = con.connection().fetchOne("select count(*) from taux_cotisation");
			if (n.get(0, Long.class) == 0L) {
				for (Object[] t : TAUX_INITIAUX)
					con.connection().execute("insert into taux_cotisation (code, libelle, taux, ordre, who_done)"
							+ " values (?, ?, ?, ?, 'initialisation')", t[0], t[1], t[2], t[3]);
			}
			for (Object[] t : TAUX_RETRAITE)
				con.connection().execute("insert into taux_cotisation (code, libelle, taux, ordre, who_done)"
						+ " values (?, ?, ?, ?, 'initialisation') on conflict (code) do nothing", t[0], t[1], t[2], t[3]);
			pret = true;
		} catch (Exception e) {
			logger.error("Initialisation des taux de cotisation impossible", e);
		}
		return pret;
	}

	// =====================================================================
	// Paramètres de retraite
	// =====================================================================

	public ParametreRetraite parametresRetraite() {
		ParametreRetraite p = new ParametreRetraite();
		if (!initialiser())
			return p;
		Record r = con.connection().fetchOne("select age_pre_retraite, age_retraite, age_fin_couverture, plafond_retraite,"
				+ " seuil_alerte_retraite from parametre_retraite where id = 1");
		return r == null ? p : r.into(ParametreRetraite.class);
	}

	public synchronized String enregistrerParametresRetraite(ParametreRetraite p, String who) {
		if (p.getAgePreRetraite() == null || p.getAgeRetraite() == null || p.getAgeFinCouverture() == null
				|| p.getPlafondRetraite() == null || p.getSeuilAlerteRetraite() == null)
			return "Tous les paramètres de retraite sont obligatoires";
		if (!(p.getAgePreRetraite() <= p.getAgeRetraite() && p.getAgeRetraite() <= p.getAgeFinCouverture()))
			return "Les âges doivent être croissants : pré-retraite ≤ retraite ≤ fin de couverture";
		if (p.getPlafondRetraite() < 0 || p.getSeuilAlerteRetraite() < 0
				|| p.getSeuilAlerteRetraite() > p.getPlafondRetraite())
			return "Le seuil d'alerte doit être positif et inférieur ou égal au plafond";
		try {
			con.connection().execute("update parametre_retraite set age_pre_retraite = ?, age_retraite = ?,"
					+ " age_fin_couverture = ?, plafond_retraite = ?, seuil_alerte_retraite = ?, who_done = ?,"
					+ " when_done = ? where id = 1", p.getAgePreRetraite(), p.getAgeRetraite(), p.getAgeFinCouverture(),
					p.getPlafondRetraite(), p.getSeuilAlerteRetraite(), who, new Timestamp(System.currentTimeMillis()));
			return "ok";
		} catch (Exception e) {
			logger.error("Erreur enregistrement paramètres retraite", e);
			return e.getMessage();
		}
	}

	// =====================================================================
	// Taux
	// =====================================================================

	public List<TauxCotisation> listTaux() {
		if (!initialiser())
			return new ArrayList<>();
		return con.connection().fetch("select code, libelle, taux::float8 as taux, nombre_max, ordre"
				+ " from taux_cotisation order by ordre, code").into(TauxCotisation.class);
	}

	/** Liens d'ayants droit (tous les taux sauf les parts de base) */
	public List<TauxCotisation> listLiens() {
		List<TauxCotisation> res = new ArrayList<>();
		for (TauxCotisation t : listTaux())
			if (!t.estBase())
				res.add(t);
		return res;
	}

	/** Part de base d'un adhérent sans ayant droit (2,5 % par défaut) */
	public double tauxBase() {
		TauxCotisation t = tauxParCode().get(TauxCotisation.CODE_ADHERENT);
		return t == null || t.getTaux() == null ? 2.5 : t.getTaux();
	}

	private Map<String, TauxCotisation> tauxParCode() {
		Map<String, TauxCotisation> m = new HashMap<>();
		for (TauxCotisation t : listTaux())
			m.put(t.getCode(), t);
		return m;
	}

	/**
	 * Crée ou met à jour un taux. Ne recalcule PAS les adhérents : l'impact sur la
	 * paie doit être contrôlé puis appliqué explicitement (voir recalculerTous).
	 */
	public synchronized String enregistrerTaux(String code, String libelle, Double taux, Integer nombreMax,
			Integer ordre, String who) {
		String c = code == null ? "" : code.trim().toUpperCase();
		if (c.isEmpty() || libelle == null || libelle.trim().isEmpty())
			return "Le code et le libellé sont obligatoires";
		if (!c.matches("[A-Z0-9_]{2,30}"))
			return "Le code ne doit contenir que des lettres majuscules, chiffres ou soulignés";
		if (taux == null || taux < 0 || taux > 100)
			return "Le taux doit être compris entre 0 et 100";
		if (nombreMax != null && nombreMax < 0)
			return "Le nombre maximum doit être positif (laisser vide pour illimité)";
		try {
			Timestamp now = new Timestamp(System.currentTimeMillis());
			int n = con.connection().execute("update taux_cotisation set libelle = ?, taux = ?, nombre_max = ?,"
					+ " ordre = coalesce(?, ordre), who_done = ?, when_done = ? where code = ?", libelle.trim(), taux,
					nombreMax, ordre, who, now, c);
			if (n == 0)
				con.connection().execute("insert into taux_cotisation (code, libelle, taux, nombre_max, ordre, who_done,"
						+ " when_done) values (?, ?, ?, ?, coalesce(?, 99), ?, ?)", c, libelle.trim(), taux, nombreMax,
						ordre, who, now);
			return "ok";
		} catch (Exception e) {
			logger.error("Erreur enregistrement taux", e);
			return e.getMessage();
		}
	}

	public synchronized String supprimerTaux(String code) {
		TauxCotisation t = new TauxCotisation();
		t.setCode(code);
		if (t.estBase())
			return "Les parts de base de l'adhérent ne peuvent pas être supprimées";
		Record utilise = con.connection().fetchOne("select count(*) from ayant_droit where upper(trim(lien)) = ?"
				+ " and coalesce(on_deleted,false) = false", code);
		if (utilise.get(0, Long.class) > 0)
			return "Ce lien est utilisé par " + utilise.get(0, Long.class)
					+ " ayant(s) droit actif(s) : il ne peut pas être supprimé";
		con.connection().execute("delete from taux_cotisation where code = ?", code);
		return "ok";
	}

	// =====================================================================
	// Calcul
	// =====================================================================

	private static double arrondi(double v) {
		return Math.round(v * 1000.0) / 1000.0;
	}

	private static String normaliserLien(String lien) {
		return lien == null ? "" : lien.trim().toUpperCase();
	}

	private static double taux(Map<String, TauxCotisation> taux, String code, double defaut) {
		TauxCotisation t = taux.get(code);
		return t == null || t.getTaux() == null ? defaut : t.getTaux();
	}

	/** Calcul à partir de l'âge et des ayants droit actifs (ordonnés du plus âgé au plus jeune) */
	private CotisationAdherent calculer(Long idAdherent, Double totalEnregistre, Integer age, List<Record> ayantsDroit,
			Map<String, TauxCotisation> taux, ParametreRetraite regles) {
		CotisationAdherent c = new CotisationAdherent();
		c.setIdAdherent(idAdherent);
		c.setTotalEnregistre(totalEnregistre);
		c.setAge(age);
		String statut = regles.statut(age);
		c.setStatut(statut);
		boolean retraite = ParametreRetraite.RETRAITE.equals(statut) || ParametreRetraite.FIN_COUVERTURE.equals(statut);
		double base = taux(taux, TauxCotisation.CODE_ADHERENT, 0.0);
		if (retraite)
			base = taux(taux, TauxCotisation.CODE_RETRAITE, 0.0);
		else if (ParametreRetraite.PRE_RETRAITE.equals(statut))
			base = taux(taux, TauxCotisation.CODE_PRE_RETRAITE, base);
		c.setBase(base);
		double total = base;
		for (Record r : ayantsDroit) {
			String lien = normaliserLien(r.get("lien", String.class));
			int rang = c.getNombreParLien().merge(lien, 1, Integer::sum);
			TauxCotisation t = taux.get(lien);
			// à partir de la retraite, seul l'adhérent est couvert
			boolean compte = !retraite && t != null && (t.getNombreMax() == null || rang <= t.getNombreMax());
			double part = compte && t.getTaux() != null ? t.getTaux() : 0.0;
			total += part;
			c.getLignes().add(new CotisationAdherent.Ligne(r.get("id", Long.class), r.get("nom_ay", String.class), lien,
					part, compte));
		}
		c.setTotal(arrondi(total));
		return c;
	}

	private List<Record> ayantsDroitActifs(DSLContext ctx, Long idAdherent) {
		return ctx.fetch("select id, nom_ay, lien from ayant_droit where adherent = ? and coalesce(on_deleted,false) = false"
				+ " order by date_naiss nulls last, id", idAdherent);
	}

	/** Détail du calcul pour un adhérent (sans rien enregistrer) */
	public CotisationAdherent detail(Long idAdherent) {
		if (idAdherent == null || !initialiser())
			return null;
		Record ad = con.connection().fetchOne("select pourcentage_total_retenue, " + AGE + " as age from adherent where id = ?",
				idAdherent);
		if (ad == null)
			return null;
		return calculer(idAdherent, ad.get(0, Double.class), ad.get("age", Integer.class),
				ayantsDroitActifs(con.connection(), idAdherent), tauxParCode(), parametresRetraite());
	}

	/**
	 * Recalcule et enregistre la cotisation d'un adhérent et la part de chacun de
	 * ses ayants droit (transaction).
	 *
	 * @return le nouveau taux total, ou null si l'adhérent est introuvable
	 */
	public synchronized Double recalculer(Long idAdherent) {
		if (idAdherent == null || !initialiser())
			return null;
		Map<String, TauxCotisation> taux = tauxParCode();
		ParametreRetraite regles = parametresRetraite();
		final Double[] resultat = new Double[1];
		try {
			con.connection().transaction(cfg -> {
				DSLContext ctx = DSL.using(cfg);
				Record ad = ctx.fetchOne("select pourcentage_total_retenue, " + AGE + " as age from adherent"
						+ " where id = ? for update", idAdherent);
				if (ad == null)
					return;
				CotisationAdherent c = calculer(idAdherent, ad.get(0, Double.class), ad.get("age", Integer.class),
						ayantsDroitActifs(ctx, idAdherent), taux, regles);
				for (CotisationAdherent.Ligne l : c.getLignes())
					ctx.execute("update ayant_droit set pourcentage_retenue = ? where id = ?"
							+ " and pourcentage_retenue is distinct from ?", l.getTaux(), l.getIdAyantDroit(), l.getTaux());
				ctx.execute("update adherent set pourcentage_total_retenue = ? where id = ?", c.getTotal(), idAdherent);
				resultat[0] = c.getTotal();
			});
		} catch (Exception e) {
			logger.error("Erreur recalcul cotisation adhérent " + idAdherent, e);
			return null;
		}
		return resultat[0];
	}

	/** Adhérents actifs dont le taux enregistré diffère du taux calculé avec les règles actuelles */
	public List<EcartCotisation> controler() {
		List<EcartCotisation> ecarts = new ArrayList<>();
		if (!initialiser())
			return ecarts;
		Map<String, TauxCotisation> taux = tauxParCode();
		ParametreRetraite regles = parametresRetraite();
		Map<Long, List<Record>> parAdherent = new LinkedHashMap<>();
		for (Record r : con.connection().fetch("select adherent, id, nom_ay, lien from ayant_droit"
				+ " where coalesce(on_deleted,false) = false order by adherent, date_naiss nulls last, id"))
			parAdherent.computeIfAbsent(r.get("adherent", Long.class), k -> new ArrayList<>()).add(r);
		for (Record ad : con.connection().fetch("select id, matricule, nom_ad, prenom_ad, pourcentage_total_retenue, "
				+ AGE + " as age from adherent where coalesce(on_deleted,false) = false order by nom_ad, prenom_ad")) {
			Long id = ad.get("id", Long.class);
			List<Record> ays = parAdherent.getOrDefault(id, new ArrayList<>());
			CotisationAdherent c = calculer(id, ad.get("pourcentage_total_retenue", Double.class),
					ad.get("age", Integer.class), ays, taux, regles);
			if (c.aUnEcart()) {
				EcartCotisation e = new EcartCotisation();
				e.setIdAdherent(id);
				e.setMatricule(ad.get("matricule", String.class));
				e.setNom((nvl(ad.get("nom_ad", String.class)) + " " + nvl(ad.get("prenom_ad", String.class))).trim());
				e.setEnregistre(c.getTotalEnregistre());
				e.setCalcule(c.getTotal());
				e.setNbAyantsDroit(ays.size());
				e.setStatut(c.getStatut());
				ecarts.add(e);
			}
		}
		return ecarts;
	}

	/** Applique le calcul à tous les adhérents en écart ; renvoie le nombre corrigé */
	public synchronized int recalculerTous() {
		int n = 0;
		for (EcartCotisation e : controler())
			if (recalculer(e.getIdAdherent()) != null)
				n++;
		return n;
	}

	private static String nvl(String s) {
		return s == null ? "" : s;
	}
}
