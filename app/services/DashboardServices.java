package services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.jooq.Record;
import org.jooq.Result;

import play.Logger;
import play.libs.Json;
import utils.IConnectionHelper;

/**
 * Service du tableau de bord : toutes les statistiques sont agrégées directement
 * en base (GROUP BY) au lieu de charger toutes les lignes en mémoire, et le
 * résultat est mis en cache par année pour éviter de surcharger la base lors
 * des actualisations successives.
 *
 * @author nasser
 */
@Singleton
public class DashboardServices {

	private static final Logger.ALogger logger = Logger.of(DashboardServices.class);

	/** Durée de vie du cache (ms) */
	private static final long CACHE_TTL = 2 * 60 * 1000L;

	/** Nombre maximum de bons renvoyés pour les listes détaillées */
	private static final int MAX_BONS = 2000;

	private static final String V_BON = "v_bon_de_commande";

	private final IConnectionHelper con;
	private final LaboratoireServices laboServices;
	private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

	@Inject
	public DashboardServices(IConnectionHelper con, LaboratoireServices laboServices) {
		this.con = con;
		this.laboServices = laboServices;
	}

	private static class CacheEntry {
		final long time;
		final ObjectNode data;

		CacheEntry(ObjectNode data) {
			this.time = System.currentTimeMillis();
			this.data = data;
		}

		boolean isValid() {
			return System.currentTimeMillis() - time < CACHE_TTL;
		}
	}

	/**
	 * Normalise le paramètre année : "" ou "ALL" = toutes les années
	 */
	public static String normaliserAnnee(String annee) {
		if (annee == null)
			return "";
		String a = annee.trim();
		if (a.isEmpty() || a.equalsIgnoreCase("ALL") || !a.matches("\\d{4}"))
			return "";
		return a;
	}

	public void invalidate() {
		cache.clear();
	}

	/**
	 * Liste des années disponibles (bons de commande + reçus laboratoire + année courante)
	 */
	public List<String> listAnnees() {
		TreeSet<String> annees = new TreeSet<>();
		annees.add(String.valueOf(java.time.LocalDate.now().getYear()));
		try {
			Result<Record> r = con.connection()
					.fetch("select distinct annee from " + V_BON + " where annee is not null and on_deleted is false");
			for (Record rec : r) {
				String a = rec.get("annee", String.class);
				if (a != null && a.matches("\\d{4}"))
					annees.add(a);
			}
		} catch (Exception e) {
			logger.error("Erreur chargement des années", e);
		}
		annees.addAll(laboServices.listAnnees());
		return new ArrayList<>(annees.descendingSet());
	}

	/**
	 * Statistiques complètes du tableau de bord pour une année (ou toutes)
	 */
	public ObjectNode statistiques(String anneeParam, boolean forceRefresh) {
		String annee = normaliserAnnee(anneeParam);
		String key = annee.isEmpty() ? "ALL" : annee;
		CacheEntry entry = cache.get(key);
		if (!forceRefresh && entry != null && entry.isValid()) {
			return entry.data;
		}
		// la connexion JDBC est partagée : on sérialise les calculs
		synchronized (this) {
			entry = cache.get(key);
			if (!forceRefresh && entry != null && entry.isValid()) {
				return entry.data;
			}
			ObjectNode data = calculer(annee);
			cache.put(key, new CacheEntry(data));
			return data;
		}
	}

	private Object[] binds(String annee) {
		return annee.isEmpty() ? new Object[0] : new Object[] { annee };
	}

	private String where(String annee) {
		return " where on_deleted is false" + (annee.isEmpty() ? "" : " and annee = ?");
	}

	private long l(Record r, String f) {
		Long n = r.get(f, Long.class);
		return n == null ? 0L : n;
	}

	private ObjectNode calculer(String annee) {
		long debut = System.currentTimeMillis();
		ObjectNode result = Json.newObject();
		result.put("annee", annee.isEmpty() ? "ALL" : annee);
		String w = where(annee);
		Object[] b = binds(annee);

		// ===== 1) Indicateurs globaux =====
		Record g = con.connection().fetchOne("select count(*) as total,"
				+ " count(*) filter (where is_confirmed_bon is true) as confirmes,"
				+ " count(*) filter (where coalesce(is_confirmed_bon,false) = false and date_expiration is not null and date_expiration < now()) as expires,"
				+ " coalesce(sum(montant_total),0) as montant_total,"
				+ " coalesce(sum(montant_reglement),0) as montant_couvert,"
				+ " coalesce(sum(montant_paye),0) as montant_paye,"
				+ " coalesce(sum(montant_reglement) filter (where is_confirmed_bon is true),0) as montant_couvert_confirme,"
				+ " count(distinct id_adherent) as adherents_beneficiaires,"
				+ " count(*) filter (where ayant_droit is null) as bons_adherents,"
				+ " count(*) filter (where ayant_droit is not null) as bons_ayants_droit"
				+ " from " + V_BON + w, b);

		long total = l(g, "total");
		long confirmes = l(g, "confirmes");
		long montantTotal = l(g, "montant_total");
		result.put("totalBons", total);
		result.put("totalBonsConfirmes", confirmes);
		result.put("totalBonsNonConfirmes", total - confirmes);
		result.put("totalBonsExpires", l(g, "expires"));
		result.put("montantTotal", montantTotal);
		result.put("montantCouvert", l(g, "montant_couvert"));
		result.put("montantPaye", l(g, "montant_paye"));
		result.put("montantCouvertConfirme", l(g, "montant_couvert_confirme"));
		result.put("panierMoyen", total > 0 ? Math.round((double) montantTotal / total) : 0);
		result.put("adherentsBeneficiaires", l(g, "adherents_beneficiaires"));
		result.put("bonsAdherents", l(g, "bons_adherents"));
		result.put("bonsAyantsDroit", l(g, "bons_ayants_droit"));
		result.put("tauxConfirmation", total > 0 ? Math.round(confirmes * 1000.0 / total) / 10.0 : 0);

		// ===== 2) Population (indépendante de l'année) =====
		// Population couverte : adhérents non archivés de moins de l'âge de fin de couverture et
		// ayants droit actifs d'adhérents non retraités. Archivés, fins de couverture et ayants
		// droit des retraités sont affichés à part, hors total.
		String regles = "(select coalesce(max(age_pre_retraite),55) as apr, coalesce(max(age_retraite),60) as ar,"
				+ " coalesce(max(age_fin_couverture),65) as af from parametre_retraite) p";
		Record pa = con.connection().fetchOne("select"
				+ " count(*) filter (where not a.arch and coalesce(a.age,0) < p.af) as couverts,"
				+ " count(*) filter (where not a.arch and coalesce(a.age,0) < p.apr) as actifs,"
				+ " count(*) filter (where not a.arch and a.age >= p.apr and a.age < p.ar) as pre_retraite,"
				+ " count(*) filter (where not a.arch and a.age >= p.ar and a.age < p.af) as retraites,"
				+ " count(*) filter (where not a.arch and a.age >= p.af) as fin_couverture,"
				+ " count(*) filter (where a.arch) as archives,"
				+ " max(p.apr) as age_pre_retraite, max(p.ar) as age_retraite, max(p.af) as age_fin_couverture"
				+ " from (select coalesce(on_deleted,false) as arch, date_part('year', age(current_date, date_naiss::date)) as age"
				+ " from adherent) a cross join " + regles);
		Record py = con.connection().fetchOne("select"
				+ " count(*) filter (where not y.arch and coalesce(y.age_ad,0) < p.ar) as couverts,"
				+ " count(*) filter (where not y.arch and y.age_ad >= p.ar) as non_couverts,"
				+ " count(*) filter (where y.arch) as archives"
				+ " from (select coalesce(ay.on_deleted,false) or coalesce(ad.on_deleted,false) as arch,"
				+ " date_part('year', age(current_date, ad.date_naiss::date)) as age_ad"
				+ " from ayant_droit ay join adherent ad on ad.id = ay.adherent) y cross join " + regles);
		Record ps = con.connection().fetchOne(
				"select count(*) from structure_partenaire where coalesce(is_deleted,false) = false");
		result.put("totalAdherents", l(pa, "couverts"));
		result.put("totalAyantsDroit", l(py, "couverts"));
		result.put("totalStructuresPartenaires", ps.get(0, Long.class));
		result.put("adherentsActifs", l(pa, "actifs"));
		result.put("adherentsPreRetraite", l(pa, "pre_retraite"));
		result.put("adherentsRetraites", l(pa, "retraites"));
		result.put("adherentsFinCouverture", l(pa, "fin_couverture"));
		result.put("adherentsArchives", l(pa, "archives"));
		result.put("ayantsDroitNonCouverts", l(py, "non_couverts"));
		result.put("ayantsDroitArchives", l(py, "archives"));
		result.put("agePreRetraite", l(pa, "age_pre_retraite"));
		result.put("ageRetraite", l(pa, "age_retraite"));
		result.put("ageFinCouverture", l(pa, "age_fin_couverture"));

		ArrayNode sexeLabels = Json.newArray();
		ArrayNode sexeValues = Json.newArray();
		for (Record r : con.connection().fetch("select coalesce(nullif(trim(upper(sexe)),''),'N/R') as sexe, count(*) as nb"
				+ " from adherent where coalesce(on_deleted,false) = false"
				+ " and coalesce(date_part('year', age(current_date, date_naiss::date)),0) < " + l(pa, "age_fin_couverture")
				+ " group by 1 order by 2 desc")) {
			sexeLabels.add(r.get("sexe", String.class));
			sexeValues.add(l(r, "nb"));
		}
		result.set("sexeLabels", sexeLabels);
		result.set("sexeValues", sexeValues);

		// ===== 3) Par structure =====
		ArrayNode structures = Json.newArray();
		ArrayNode bonsConfirmes = Json.newArray();
		ArrayNode bonsNonConfirmes = Json.newArray();
		ArrayNode structMontantTotal = Json.newArray();
		ArrayNode structMontantCouvert = Json.newArray();
		for (Record r : con.connection().fetch("select structure,"
				+ " count(*) filter (where is_confirmed_bon is true) as confirmes,"
				+ " count(*) filter (where coalesce(is_confirmed_bon,false) = false) as non_confirmes,"
				+ " coalesce(sum(montant_total),0) as montant_total,"
				+ " coalesce(sum(montant_reglement),0) as montant_couvert"
				+ " from " + V_BON + w + " and structure is not null group by structure order by count(*) desc", b)) {
			structures.add(r.get("structure", String.class));
			bonsConfirmes.add(l(r, "confirmes"));
			bonsNonConfirmes.add(l(r, "non_confirmes"));
			structMontantTotal.add(l(r, "montant_total"));
			structMontantCouvert.add(l(r, "montant_couvert"));
		}
		result.set("structures", structures);
		result.set("bonsConfirmes", bonsConfirmes);
		result.set("bonsNonConfirmes", bonsNonConfirmes);
		result.set("structuresMontantTotal", structMontantTotal);
		result.set("structuresMontantCouvert", structMontantCouvert);
		result.put("totalStructures", structures.size());

		// ===== 4) Par type de prestation =====
		groupBy(result, "prestation", "coalesce(nullif(trim(prestation),''),'Non renseignée')", w, b,
				"typePrestations", "bonsByTypePrestation", "montantByTypePrestation");

		// ===== 5) Par type de structure (pharmacie, hôpital, ...) =====
		groupBy(result, "type_structure", "coalesce(nullif(trim(upper(type_structure)),''),'N/R')", w, b,
				"typeStructures", "bonsByTypeStructure", "montantByTypeStructure");

		// ===== 6) Par statut de structure (public / privé) =====
		groupBy(result, "statut_structure", "coalesce(nullif(trim(upper(statut_structure)),''),'N/R')", w, b,
				"statutStructures", "bonsByStatutStructure", "montantByStatutStructure");

		// ===== 7) Par bénéficiaire (adhérent lui-même / lien de l'ayant droit) =====
		groupBy(result, "lien",
				"case when ayant_droit is null then 'ADHERENT' else coalesce(nullif(trim(upper(lien)),''),'AYANT DROIT') end",
				w, b, "liens", "bonsByLien", "montantByLien");

		// ===== 8) Évolution : par mois si une année est choisie, sinon par année =====
		ArrayNode evoLabels = Json.newArray();
		ArrayNode evoBons = Json.newArray();
		ArrayNode evoMontantTotal = Json.newArray();
		ArrayNode evoMontantCouvert = Json.newArray();
		if (!annee.isEmpty()) {
			String[] mois = { "Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc" };
			long[] nb = new long[12], mt = new long[12], mc = new long[12];
			for (Record r : con.connection().fetch("select extract(month from date_payement)::int as m, count(*) as nb,"
					+ " coalesce(sum(montant_total),0) as mt, coalesce(sum(montant_reglement),0) as mc"
					+ " from " + V_BON + w + " and date_payement is not null group by 1", b)) {
				Integer m = r.get("m", Integer.class);
				if (m != null && m >= 1 && m <= 12) {
					nb[m - 1] = l(r, "nb");
					mt[m - 1] = l(r, "mt");
					mc[m - 1] = l(r, "mc");
				}
			}
			for (int i = 0; i < 12; i++) {
				evoLabels.add(mois[i]);
				evoBons.add(nb[i]);
				evoMontantTotal.add(mt[i]);
				evoMontantCouvert.add(mc[i]);
			}
		} else {
			for (Record r : con.connection().fetch("select annee, count(*) as nb,"
					+ " coalesce(sum(montant_total),0) as mt, coalesce(sum(montant_reglement),0) as mc"
					+ " from " + V_BON + w + " and annee is not null group by annee order by annee", b)) {
				evoLabels.add(r.get("annee", String.class));
				evoBons.add(l(r, "nb"));
				evoMontantTotal.add(l(r, "mt"));
				evoMontantCouvert.add(l(r, "mc"));
			}
		}
		result.set("evolutionLabels", evoLabels);
		result.set("evolutionBons", evoBons);
		result.set("evolutionMontantTotal", evoMontantTotal);
		result.set("evolutionMontantCouvert", evoMontantCouvert);

		// ===== 9) Top 10 des adhérents consommateurs =====
		ArrayNode top = Json.newArray();
		for (Record r : con.connection().fetch("select id_adherent, max(matricule) as matricule,"
				+ " max(nom_ad) as nom_ad, max(prenom_ad) as prenom_ad, count(*) as nb,"
				+ " coalesce(sum(montant_total),0) as mt, coalesce(sum(montant_reglement),0) as mc"
				+ " from " + V_BON + w + " group by id_adherent order by mc desc limit 10", b)) {
			ObjectNode o = Json.newObject();
			o.put("matricule", nvl(r.get("matricule", String.class)));
			o.put("nom", (nvl(r.get("nom_ad", String.class)) + " " + nvl(r.get("prenom_ad", String.class))).trim());
			o.put("nbBons", l(r, "nb"));
			o.put("montantTotal", l(r, "mt"));
			o.put("montantCouvert", l(r, "mc"));
			top.add(o);
		}
		result.set("topAdherents", top);

		// ===== 10) Laboratoire =====
		result.set("laboratoire", laboServices.statistiquesDashboard(annee));

		result.put("dureeCalculMs", System.currentTimeMillis() - debut);
		result.put("genereLe", new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date()));
		return result;
	}

	private void groupBy(ObjectNode result, String alias, String expr, String w, Object[] b, String labelsKey,
			String countKey, String montantKey) {
		ArrayNode labels = Json.newArray();
		ArrayNode counts = Json.newArray();
		ArrayNode montants = Json.newArray();
		for (Record r : con.connection().fetch("select " + expr + " as " + alias + ", count(*) as nb,"
				+ " coalesce(sum(montant_reglement),0) as mc from " + V_BON + w + " group by 1 order by 2 desc", b)) {
			labels.add(r.get(alias, String.class));
			counts.add(l(r, "nb"));
			montants.add(l(r, "mc"));
		}
		result.set(labelsKey, labels);
		result.set(countKey, counts);
		result.set(montantKey, montants);
	}

	private static String nvl(String s) {
		return s == null ? "" : s;
	}

	/**
	 * Liste des bons (confirmés ou non) d'une année, chargée uniquement à la demande
	 */
	public ArrayNode listBons(String anneeParam, boolean confirmes) {
		String annee = normaliserAnnee(anneeParam);
		String sql = "select id, nom_ad, prenom_ad, structure, prestation, montant_total, montant_reglement,"
				+ " to_char(date_payement,'DD/MM/YYYY') as date_bon, to_char(date_expiration,'DD/MM/YYYY') as date_exp,"
				+ " (date_expiration is not null and date_expiration < now()) as expire"
				+ " from " + V_BON + where(annee)
				+ (confirmes ? " and is_confirmed_bon is true" : " and coalesce(is_confirmed_bon,false) = false")
				+ " order by id desc limit " + MAX_BONS;
		ArrayNode arr = Json.newArray();
		synchronized (this) {
			for (Record r : con.connection().fetch(sql, binds(annee))) {
				ObjectNode o = Json.newObject();
				o.put("id", r.get("id", Long.class));
				o.put("adherent", (nvl(r.get("nom_ad", String.class)) + " " + nvl(r.get("prenom_ad", String.class))).trim());
				o.put("structure", nvl(r.get("structure", String.class)));
				o.put("prestation", nvl(r.get("prestation", String.class)));
				o.put("montantTotal", l(r, "montant_total"));
				o.put("montantCouvert", l(r, "montant_reglement"));
				o.put("dateBon", nvl(r.get("date_bon", String.class)));
				o.put("dateExpiration", nvl(r.get("date_exp", String.class)));
				Boolean exp = r.get("expire", Boolean.class);
				o.put("expire", exp != null && exp);
				arr.add(o);
			}
		}
		return arr;
	}
}
