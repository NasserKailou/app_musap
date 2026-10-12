package services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import dto.RoleVue;
import play.Logger;
import utils.IConnectionHelper;
import utils.Permissions;

/**
 * Gestion des rôles applicatifs et de l'octroi des permissions.
 *
 * Le code du rôle est celui stocké dans users.droit (et en session sous la clé
 * "droit"), ce qui garde la compatibilité avec les utilisateurs existants.
 * Le rôle Admin est un rôle système qui possède toujours toutes les permissions
 * (impossible de se retirer l'accès par erreur).
 *
 * @author nasser
 */
@Singleton
public class RoleServices {

	private static final Logger.ALogger logger = Logger.of(RoleServices.class);

	/** Instance accessible depuis les vues Twirl (voir utils.Acces) */
	private static volatile RoleServices instance;

	private static final String[] DDL = {
			"CREATE TABLE IF NOT EXISTS app_role (code VARCHAR(50) PRIMARY KEY, libelle VARCHAR(100) NOT NULL,"
					+ " description VARCHAR(255), systeme BOOLEAN NOT NULL DEFAULT FALSE, actif BOOLEAN NOT NULL DEFAULT TRUE,"
					+ " who_done VARCHAR(100), when_done TIMESTAMP DEFAULT now())",
			"CREATE TABLE IF NOT EXISTS app_role_permission (role_code VARCHAR(50) NOT NULL REFERENCES app_role(code)"
					+ " ON DELETE CASCADE ON UPDATE CASCADE, permission VARCHAR(60) NOT NULL, PRIMARY KEY (role_code, permission))" };

	private final IConnectionHelper con;
	private final Map<String, Set<String>> cache = new ConcurrentHashMap<>();
	private final Map<String, Boolean> actifs = new ConcurrentHashMap<>();
	private volatile boolean pret = false;

	@Inject
	public RoleServices(IConnectionHelper con) {
		this.con = con;
		initialiser();
		instance = this;
	}

	public static RoleServices instance() {
		return instance;
	}

	/**
	 * Crée les tables et, à la première exécution, les rôles existants avec les
	 * droits qu'ils avaient avant (aucun changement de comportement).
	 */
	public synchronized boolean initialiser() {
		if (pret)
			return true;
		try {
			for (String ddl : DDL)
				con.connection().execute(ddl);
			Record n = con.connection().fetchOne("select count(*) from app_role");
			if (n.get(0, Long.class) == 0L) {
				Timestamp now = new Timestamp(System.currentTimeMillis());
				Set<String> crees = new HashSet<>();
				for (String[] r : Permissions.rolesInitiaux()) {
					creerAvecDefauts(r[0], r[1], r[2], Permissions.ROLE_ADMIN.equals(r[0]), now);
					crees.add(r[0]);
				}
				// rôles éventuellement présents dans users mais inconnus du catalogue
				for (Record u : con.connection().fetch("select distinct droit from users where droit is not null")) {
					String d = u.get(0, String.class).trim();
					if (!d.isEmpty() && !crees.contains(d)) {
						creerAvecDefauts(d, d, "Rôle repris des utilisateurs existants", false, now);
						crees.add(d);
					}
				}
			}
			pret = true;
			rechargerCache();
		} catch (Exception e) {
			logger.error("Initialisation des rôles impossible", e);
		}
		return pret;
	}

	private void creerAvecDefauts(String code, String libelle, String description, boolean systeme, Timestamp now) {
		con.connection().execute("insert into app_role (code, libelle, description, systeme, actif, who_done, when_done)"
				+ " values (?, ?, ?, ?, true, 'initialisation', ?)", code, libelle, description, systeme, now);
		for (String p : Permissions.defauts(code))
			con.connection().execute("insert into app_role_permission (role_code, permission) values (?, ?)", code, p);
	}

	private void rechargerCache() {
		Map<String, Set<String>> m = new LinkedHashMap<>();
		Map<String, Boolean> a = new LinkedHashMap<>();
		for (Record r : con.connection().fetch("select code, actif from app_role")) {
			m.put(r.get("code", String.class), new HashSet<>());
			a.put(r.get("code", String.class), Boolean.TRUE.equals(r.get("actif", Boolean.class)));
		}
		for (Record r : con.connection().fetch("select role_code, permission from app_role_permission"))
			m.computeIfAbsent(r.get(0, String.class), k -> new HashSet<>()).add(r.get(1, String.class));
		cache.clear();
		cache.putAll(m);
		actifs.clear();
		actifs.putAll(a);
	}

	// =====================================================================
	// Contrôle d'accès
	// =====================================================================

	/** Vrai si le rôle possède au moins une des permissions demandées */
	public boolean peut(String role, String... permissions) {
		if (role == null)
			return false;
		if (Permissions.ROLE_ADMIN.equals(role))
			return true;
		if (!estActif(role))
			return false;
		Set<String> accordees = cache.get(role);
		if (accordees == null) {
			// base indisponible au démarrage : on retombe sur les droits historiques
			accordees = pret ? Collections.emptySet() : Permissions.defauts(role);
		}
		for (String p : permissions)
			if (accordees.contains(p))
				return true;
		return false;
	}

	public boolean estActif(String role) {
		if (Permissions.ROLE_ADMIN.equals(role))
			return true;
		Boolean a = actifs.get(role);
		return a == null ? !pret : a;
	}

	public Set<String> permissions(String role) {
		if (Permissions.ROLE_ADMIN.equals(role))
			return Permissions.codes();
		return Collections.unmodifiableSet(cache.getOrDefault(role, Collections.emptySet()));
	}

	// =====================================================================
	// Gestion des rôles
	// =====================================================================

	public List<RoleVue> listRoles() {
		List<RoleVue> roles = new ArrayList<>();
		if (!initialiser())
			return roles;
		for (Record r : con.connection().fetch("select r.code, r.libelle, r.description, r.systeme, r.actif,"
				+ " (select count(*) from users u where u.droit = r.code) as nb_utilisateurs"
				+ " from app_role r order by r.systeme desc, r.libelle")) {
			RoleVue v = r.into(RoleVue.class);
			v.setPermissions(new TreeSet<>(permissions(v.getCode())));
			roles.add(v);
		}
		return roles;
	}

	public List<RoleVue> listRolesActifs() {
		List<RoleVue> res = new ArrayList<>();
		for (RoleVue r : listRoles())
			if (r.estActif())
				res.add(r);
		return res;
	}

	public RoleVue findRole(String code) {
		if (code == null)
			return null;
		for (RoleVue r : listRoles())
			if (r.getCode().equals(code))
				return r;
		return null;
	}

	/**
	 * Crée ou modifie un rôle.
	 *
	 * @return "ok" ou un message d'erreur lisible
	 */
	public synchronized String enregistrerRole(String code, String libelle, String description, boolean actif,
			boolean creation, String who) {
		String c = code == null ? "" : code.trim();
		String l = libelle == null ? "" : libelle.trim();
		if (c.isEmpty() || l.isEmpty())
			return "Le code et le libellé du rôle sont obligatoires";
		if (!c.matches("[A-Za-z0-9_\\-]{2,50}"))
			return "Le code du rôle ne doit contenir que des lettres, chiffres, tirets ou soulignés (2 à 50 caractères)";
		try {
			Timestamp now = new Timestamp(System.currentTimeMillis());
			Record existant = con.connection().fetchOne("select code, systeme from app_role where lower(code) = lower(?)", c);
			if (creation) {
				if (existant != null)
					return "Un rôle avec le code '" + c + "' existe déjà";
				con.connection().execute("insert into app_role (code, libelle, description, systeme, actif, who_done,"
						+ " when_done) values (?, ?, ?, false, ?, ?, ?)", c, l, vide(description), actif, who, now);
			} else {
				if (existant == null)
					return "Rôle introuvable";
				boolean systeme = Boolean.TRUE.equals(existant.get("systeme", Boolean.class));
				con.connection().execute("update app_role set libelle = ?, description = ?, actif = ?, who_done = ?,"
						+ " when_done = ? where code = ?", l, vide(description), systeme || actif, who, now,
						existant.get("code", String.class));
			}
			rechargerCache();
			return "ok";
		} catch (Exception e) {
			logger.error("Erreur enregistrement rôle", e);
			return e.getMessage();
		}
	}

	/** Remplace l'ensemble des permissions d'un rôle (transaction) */
	public synchronized String definirPermissions(String code, Set<String> permissions, String who) {
		if (Permissions.ROLE_ADMIN.equals(code))
			return "Le rôle Administrateur possède toujours toutes les permissions";
		if (findRole(code) == null)
			return "Rôle introuvable";
		Set<String> valides = new HashSet<>();
		for (String p : permissions)
			if (Permissions.existe(p))
				valides.add(p);
		try {
			con.connection().transaction(cfg -> {
				DSLContext ctx = DSL.using(cfg);
				ctx.execute("delete from app_role_permission where role_code = ?", code);
				for (String p : valides)
					ctx.execute("insert into app_role_permission (role_code, permission) values (?, ?)", code, p);
				ctx.execute("update app_role set who_done = ?, when_done = ? where code = ?", who,
						new Timestamp(System.currentTimeMillis()), code);
			});
			rechargerCache();
			return "ok";
		} catch (Exception e) {
			logger.error("Erreur octroi des permissions", e);
			return e.getMessage();
		}
	}

	public synchronized String supprimerRole(String code) {
		RoleVue r = findRole(code);
		if (r == null)
			return "Rôle introuvable";
		if (r.estSysteme())
			return "Le rôle système " + r.getLibelle() + " ne peut pas être supprimé";
		if (r.getNbUtilisateurs() != null && r.getNbUtilisateurs() > 0)
			return "Impossible de supprimer le rôle " + r.getLibelle() + " : " + r.getNbUtilisateurs()
					+ " utilisateur(s) y sont rattaché(s). Désactivez-le ou réaffectez les utilisateurs.";
		try {
			con.connection().execute("delete from app_role where code = ?", code);
			rechargerCache();
			return "ok";
		} catch (Exception e) {
			logger.error("Erreur suppression rôle", e);
			return e.getMessage();
		}
	}

	private static String vide(String s) {
		return s == null || s.trim().isEmpty() ? null : s.trim();
	}
}
