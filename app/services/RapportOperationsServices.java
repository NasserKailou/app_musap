package services;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import dto.OperationVue;
import play.Logger;
import utils.IConnectionHelper;

/**
 * Rapports des opérations (bons de commande et prises en charge) par période,
 * liste des annulations, et journal des annulations / restaurations.
 *
 * L'émetteur est l'utilisateur ayant saisi l'opération (reglement.who_done) et la
 * région est celle de son profil (users.region_code).
 *
 * @author nasser
 */
@Singleton
public class RapportOperationsServices {

	private static final Logger.ALogger logger = Logger.of(RapportOperationsServices.class);

	public static final String ANNULATION = "ANNULATION";
	public static final String RESTAURATION = "RESTAURATION";

	private static final String[] DDL = {
			"CREATE TABLE IF NOT EXISTS reglement_annulation (id BIGSERIAL PRIMARY KEY, reglement BIGINT NOT NULL"
					+ " REFERENCES reglement(id), action VARCHAR(20) NOT NULL, motif VARCHAR(500), who_done VARCHAR(100),"
					+ " when_done TIMESTAMP NOT NULL DEFAULT now())",
			"CREATE INDEX IF NOT EXISTS idx_reglement_annulation_reglement ON reglement_annulation(reglement)" };

	/** Date de l'opération : date de saisie, à défaut date du bon */
	private static final String DATE_OP = "coalesce(r.when_done, r.date_payement)";

	private static final String SELECT = "select r.id, upper(coalesce(r.type_reglement,'BC')) as type_reglement,"
			+ " " + DATE_OP + " as date_operation, r.who_done as login_emetteur,"
			+ " coalesce(nullif(trim(u.nom_prenom),''), r.who_done) as emetteur,"
			+ " coalesce(rg.libelle, 'NON RENSEIGNÉE') as region,"
			+ " case when r.ayant_droit is not null then ay.nom_ay"
			+ "   else trim(coalesce(ad.nom_ad,'') || ' ' || coalesce(ad.prenom_ad,'')) end as beneficiaire,"
			+ " case when r.ayant_droit is not null then coalesce(nullif(upper(trim(ay.lien)),''),'AYANT DROIT')"
			+ "   else 'ADHERENT' end as qualite,"
			+ " ad.id as id_adherent, trim(coalesce(ad.nom_ad,'') || ' ' || coalesce(ad.prenom_ad,'')) as adherent,"
			+ " ad.matricule, s.libelle as structure, tp.prestation, r.is_confirmed_bon,"
			+ " coalesce((select sum(d.montant) from reglement_detail d where d.reglement = r.id"
			+ "   and coalesce(d.on_deleted,false) = false),0)::bigint as montant,"
			+ " j.annule_par, j.annule_le, j.motif"
			+ " from reglement r"
			+ " left join users u on u.login = r.who_done"
			+ " left join region rg on rg.id = u.region_code"
			+ " left join adherent ad on ad.id = r.adherent"
			+ " left join ayant_droit ay on ay.id = r.ayant_droit"
			+ " left join structure_partenaire s on s.id = r.structure"
			+ " left join type_prestation tp on tp.id = r.type_prestation"
			+ " left join lateral (select coalesce(nullif(trim(ua.nom_prenom),''), a.who_done) as annule_par,"
			+ "   a.when_done as annule_le, a.motif from reglement_annulation a"
			+ "   left join users ua on ua.login = a.who_done"
			+ "   where a.reglement = r.id and a.action = 'ANNULATION' order by a.when_done desc limit 1) j on true";

	private final IConnectionHelper con;
	private volatile boolean pret = false;

	@Inject
	public RapportOperationsServices(IConnectionHelper con) {
		this.con = con;
		initialiser();
	}

	public synchronized boolean initialiser() {
		if (pret)
			return true;
		try {
			for (String ddl : DDL)
				con.connection().execute(ddl);
			pret = true;
		} catch (Exception e) {
			logger.error("Initialisation du journal des annulations impossible", e);
		}
		return pret;
	}

	/**
	 * Opérations (annulées ou non) d'une période.
	 *
	 * @param annulees faux : opérations actives saisies sur la période ; vrai : opérations annulées
	 *                 sur la période (date d'annulation si connue, sinon date de saisie)
	 * @param type     "BC", "PC" ou vide pour tous
	 * @param region   identifiant de la région de l'émetteur, ou null pour toutes
	 */
	public List<OperationVue> operations(LocalDate debut, LocalDate fin, boolean annulees, String type, Long region) {
		if (!initialiser())
			return new ArrayList<>();
		List<Object> binds = new ArrayList<>();
		String dateFiltre = annulees ? "coalesce(j.annule_le, " + DATE_OP + ")" : DATE_OP;
		StringBuilder sql = new StringBuilder(SELECT)
				.append(" where coalesce(r.on_deleted,false) = ").append(annulees ? "true" : "false")
				.append(" and ").append(dateFiltre).append(" >= ? and ").append(dateFiltre).append(" < ?");
		binds.add(Timestamp.valueOf(debut.atStartOfDay()));
		binds.add(Timestamp.valueOf(fin.plusDays(1).atStartOfDay()));
		if (type != null && (type.equalsIgnoreCase("BC") || type.equalsIgnoreCase("PC"))) {
			sql.append(" and upper(coalesce(r.type_reglement,'BC')) = ?");
			binds.add(type.toUpperCase());
		}
		if (region != null) {
			sql.append(" and u.region_code = ?");
			binds.add(region);
		}
		sql.append(" order by ").append(dateFiltre).append(" desc, r.id desc limit 10000");
		return con.connection().fetch(sql.toString(), binds.toArray()).into(OperationVue.class);
	}

	public OperationVue findOperation(Long id) {
		if (id == null || !initialiser())
			return null;
		Record r = con.connection().fetchOne(SELECT + " where r.id = ?", id);
		return r == null ? null : r.into(OperationVue.class);
	}

	/** Régions (y compris la région nationale) pour le filtre */
	public List<Map<String, Object>> regions() {
		List<Map<String, Object>> res = new ArrayList<>();
		for (Record r : con.connection().fetch("select id, libelle from region order by libelle")) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", r.get("id", Long.class));
			m.put("libelle", r.get("libelle", String.class));
			res.add(m);
		}
		return res;
	}

	/** Nombre d'opérations et montant par clé (type, région, émetteur), du plus grand au plus petit */
	public static Map<String, long[]> regrouper(List<OperationVue> ops, Function<OperationVue, String> cle) {
		Map<String, long[]> m = new LinkedHashMap<>();
		for (OperationVue o : ops) {
			String k = cle.apply(o) == null ? "—" : cle.apply(o);
			long[] v = m.computeIfAbsent(k, x -> new long[2]);
			v[0]++;
			v[1] += o.getMontant() == null ? 0 : o.getMontant();
		}
		List<Map.Entry<String, long[]>> l = new ArrayList<>(m.entrySet());
		l.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
		Map<String, long[]> trie = new LinkedHashMap<>();
		for (Map.Entry<String, long[]> e : l)
			trie.put(e.getKey(), e.getValue());
		return trie;
	}

	// =====================================================================
	// Annulation / restauration (avec journal)
	// =====================================================================

	/**
	 * Annule (suppression logique) ou restaure une opération et l'inscrit au journal.
	 *
	 * @return "ok" ou un message d'erreur lisible
	 */
	public synchronized String changerEtat(Long id, boolean annuler, String motif, String who) {
		if (id == null || !initialiser())
			return "Journal des annulations indisponible";
		Record r = con.connection().fetchOne("select coalesce(on_deleted,false) as annule from reglement where id = ?", id);
		if (r == null)
			return "Bon ou prise en charge introuvable";
		boolean dejaAnnule = Boolean.TRUE.equals(r.get("annule", Boolean.class));
		if (annuler && dejaAnnule)
			return "Ce bon est déjà annulé";
		if (!annuler && !dejaAnnule)
			return "Ce bon n'est pas annulé";
		String m = motif == null || motif.trim().isEmpty() ? null
				: motif.trim().substring(0, Math.min(500, motif.trim().length()));
		try {
			con.connection().transaction(cfg -> {
				DSLContext ctx = DSL.using(cfg);
				ctx.execute("update reglement set on_deleted = ? where id = ?", annuler, id);
				ctx.execute("insert into reglement_annulation (reglement, action, motif, who_done, when_done)"
						+ " values (?, ?, ?, ?, ?)", id, annuler ? ANNULATION : RESTAURATION, m, who,
						new Timestamp(System.currentTimeMillis()));
			});
			return "ok";
		} catch (Exception e) {
			logger.error("Erreur changement d'état du règlement " + id, e);
			return e.getMessage();
		}
	}
}
