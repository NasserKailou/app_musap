package controllers;

import com.google.inject.Inject;

import models.tables.pojos.Connexion;
import models.tables.pojos.Params;
import models.tables.pojos.Users;
import play.api.Configuration;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import static play.mvc.Results.redirect;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import services.ConnexionMainServices;
import services.ParamsServices;
import services.RoleServices;
import services.UserService;
import utils.BCryptHash;
import utils.ViewMode;
import views.html.*;

/**
 * 
 * @author nasser
 *
 */
public class AuthenticationCtrl extends Controller {

	private final UserService userService;
	private final ParamsServices paramsService;
	private final ConnexionMainServices connexion;
	private final FormFactory formFactory;
	private final RoleServices roleServices;

	// @Inject
	// private Helpers helpers;

	@Inject
	public AuthenticationCtrl(FormFactory formFactory, UserService userService, ParamsServices paramsService,
			ConnexionMainServices connexion, RoleServices roleServices) {
		this.userService = userService;
		this.paramsService = paramsService;
		this.formFactory = formFactory;
		this.connexion = connexion;
		this.roleServices = roleServices;

	}

	public Result login(Request request) {

		return ok(index.render(paramsService.listesParams(), request));
	}

	public Result authentification(Request request) {

		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("MM"));
		Integer a = Integer.valueOf(now_string);

		if (a >= 13) {
			return redirect(controllers.routes.AuthenticationCtrl.login()).flashing("error",
					"Connexion Impossible, si ce problème persiste veillez contacter le service Informatique!!!");
		}

		// Lecture défensive : un champ absent ne doit jamais provoquer d'erreur 500
		DynamicForm data = formFactory.form().bindFromRequest(request);
		String login = data.get("login") == null ? "" : data.get("login").trim();
		String passe = data.get("passe") == null ? "" : data.get("passe");
		final String gestion = data.get("gestion") == null ? "" : data.get("gestion").replace(" ", "");

		if (login.isEmpty() || passe.isEmpty()) {
			return redirect(controllers.routes.AuthenticationCtrl.login()).flashing("error",
					"Veuillez saisir votre identifiant et votre mot de passe");
		}
		Params params = gestion.isEmpty() ? null : paramsService.paramsByGestion(gestion);
		if (params == null) {
			return redirect(controllers.routes.AuthenticationCtrl.login()).flashing("error",
					"Veuillez sélectionner une année de gestion valide");
		}

		Users enBase = userService.getUserByLogin(login);
		Users us = null;
		if (enBase != null && motDePasseValide(passe, enBase.getPasse())) {
			// vérifie aussi que le compte est actif (etat = true)
			us = userService.authentification(login, enBase.getPasse());
		}
		if (us == null) {
			return redirect(controllers.routes.AuthenticationCtrl.login()).flashing("error",
					" Erreur de saisie, Login ou Mot de Passe Incorrect!!!");
		}

		if (!roleServices.estActif(us.getDroit())) {
			return redirect(controllers.routes.AuthenticationCtrl.login()).flashing("error",
					"Votre profil est désactivé, contactez l'administrateur");
		}

		if (Boolean.TRUE.equals(us.getConnectFirst())) {
			return redirect(controllers.routes.AuthenticationCtrl.returnFirtPageConnexion(us.getLogin()));
		}

		Connexion conn = new Connexion();
		conn.setUser(us.getLogin() + " - " + us.getNomPrenom());
		conn.setProfil(us.getDroit());
		conn.setWhenDown(new Timestamp(System.currentTimeMillis()));
		connexion.insert(conn);
		return redirect(routes.HomeController.acceuil()).removingFromSession(request, "droit")
				.addingToSession(request, "droit", String.valueOf(us.getDroit())).removingFromSession(request, "login")
				.addingToSession(request, "login", us.getLogin())
				.removingFromSession(request, "nomUser")
				.addingToSession(request, "nomUser", String.valueOf(us.getNomPrenom()))
				.removingFromSession(request, "region")
				.addingToSession(request, "region", String.valueOf(us.getRegionCode()))
				.removingFromSession(request, "gestion").addingToSession(request, "gestion", gestion)
				.removingFromSession(request, "plafond")
				.addingToSession(request, "plafond", String.valueOf(params.getPlafond()))
				.removingFromSession(request, "seuilAlerte")
				.addingToSession(request, "seuilAlerte", String.valueOf(params.getSeuilAlerte()));
	}

	/** Vérification BCrypt sans exception (hash absent ou au mauvais format => refus) */
	private static boolean motDePasseValide(String saisi, String hash) {
		try {
			return BCryptHash.checkPassword(saisi, hash);
		} catch (Exception e) {
			return false;
		}
	}

	public Result logout(Request request) {
		// session().clear();
		// flash("success", "Vous etes déconnecté(e)");
		return redirect(controllers.routes.AuthenticationCtrl.login()).withNewSession().flashing("success",
				" Vous etes déconnecté(e)");
	}

	public Result firstConnexion(Request request) {

		final String confirm = formFactory.form().bindFromRequest(request).get("passeconfirm");
		Form<Users> uForm = formFactory.form(Users.class).bindFromRequest(request);
		Users a = uForm.get();
		if (a.getPasse().equals(confirm)) {
			a.setConnectFirst(false);
			a.setWhenDone(new Timestamp(System.currentTimeMillis()));
			if (userService.saveLogical(a, false).equals("ok")) {
				return redirect(controllers.routes.AuthenticationCtrl.login()).withNewSession().flashing("success",
						" Votre mot de passe a été changé avec succès, veillez vous reconnecter avec votre nouveau mot de passe");
			} else {
				return redirect(controllers.routes.AuthenticationCtrl.login()).withNewSession().flashing("error",
						" une erreur s'est produite !!!");
			}
		} else {
			return redirect(controllers.routes.AuthenticationCtrl.returnFirtPageConnexion(a.getLogin()))
					.withNewSession().flashing("error", "Incohérence entre vos mot de passe, recommencez SVP!!!");
		}

	}

	public Result returnFirtPageConnexion(String email, Request request) {
		return ok(views.html.changePassword.render(userService.getUserByLogin(email), request));
	}
	/*
	 * public static class AuthenticatedUser { public String login; public String
	 * password;
	 * 
	 * public String validate() { if (User.authenticate(login, password) == null) {
	 * return "oups! Essaye encore une fois"; } return null; }
	 * 
	 * }
	 * 
	 * public Result login() { return ok(login.render("form")); }
	 * 
	 * public Result authenticate() {
	 * 
	 * //Form<AuthenticatedUser> loginForm =
	 * formFactory.form(AuthenticatedUser.class).bindFromRequest(); session("login",
	 * "user"); return redirect("/compagnie/form");
	 * 
	 * if (loginForm.hasErrors()) { return badRequest(login.render(loginForm)); }
	 * else { session("email", loginForm.get().email); return
	 * redirect(routes.Application.index()); }
	 * 
	 * }
	 * 
	 * // Fermer la session public static Result logout() { session().clear();
	 * flash("success", "Vous etes déconnecté(e)"); return
	 * redirect("routes.Authentication.login()"); }
	 */
}
