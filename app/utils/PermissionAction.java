package utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import play.Logger;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

/**
 * Vérifie la permission demandée par {@link Permission} avant d'exécuter l'action.
 * - non connecté : retour à la page de connexion
 * - appel AJAX / JSON : réponse 403 JSON
 * - navigation : retour à l'accueil avec un message
 *
 * @author nasser
 */
public class PermissionAction extends Action<Permission> {

	private static final Logger.ALogger logger = Logger.of(PermissionAction.class);

	@Override
	public CompletionStage<Result> call(Http.Request request) {
		if (!request.session().get("login").isPresent()) {
			return CompletableFuture.completedFuture(
					Results.redirect("/").flashing("error", "Veuillez vous connecter"));
		}
		if (Acces.peut(request, configuration.value())) {
			return delegate.call(request);
		}
		logger.warn("Accès refusé : " + request.session().get("login").orElse("?") + " (" + Acces.role(request)
				+ ") -> " + request.method() + " " + request.path());
		String message = "Vous n'avez pas les droits nécessaires pour cette opération";
		if (estAppelAjax(request)) {
			return CompletableFuture.completedFuture(Results.forbidden(Json.newObject().put("error", message)));
		}
		return CompletableFuture.completedFuture(Results.redirect("/accueil").flashing("error", message));
	}

	private static boolean estAppelAjax(Http.Request request) {
		String xrw = request.header("X-Requested-With").orElse("");
		String accept = request.header("Accept").orElse("");
		return "XMLHttpRequest".equalsIgnoreCase(xrw) || request.path().startsWith("/api/")
				|| request.path().contains("json")
				|| (accept.contains("application/json") && !accept.contains("text/html"));
	}
}
