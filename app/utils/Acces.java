package utils;

import play.mvc.Http;
import services.RoleServices;

/**
 * Contrôle d'accès utilisable dans les vues Twirl et les contrôleurs :
 * {@code @if(Acces.peut(request, Permissions.ADHERENT_CREER)) { ... }}
 *
 * @author nasser
 */
public final class Acces {

	private Acces() {
	}

	public static String role(Http.Request request) {
		return request.session().get("droit").orElse(null);
	}

	/** Vrai si l'utilisateur connecté possède au moins une des permissions */
	public static boolean peut(Http.Request request, String... permissions) {
		String role = role(request);
		if (role == null || !request.session().get("login").isPresent())
			return false;
		RoleServices rs = RoleServices.instance();
		if (rs == null)
			return Permissions.ROLE_ADMIN.equals(role) || containsAny(Permissions.defauts(role), permissions);
		return rs.peut(role, permissions);
	}

	/**
	 * Accès à la fiche d'un adhérent : soit la permission globale, soit l'espace
	 * adhérent sur sa propre fiche (l'identifiant de connexion d'un adhérent est
	 * son numéro de téléphone).
	 */
	public static boolean peutVoirAdherent(Http.Request request, String telephoneAdherent, String permissionGlobale) {
		if (peut(request, permissionGlobale))
			return true;
		return peut(request, Permissions.ADHERENT_ESPACE) && telephoneAdherent != null
				&& telephoneAdherent.equals(request.session().get("login").orElse(null));
	}

	private static boolean containsAny(java.util.Set<String> s, String... p) {
		for (String x : p)
			if (s.contains(x))
				return true;
		return false;
	}
}
