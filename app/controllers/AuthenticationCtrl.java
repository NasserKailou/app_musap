package controllers;

import com.google.inject.Inject;

import models.tables.pojos.Connexion;
import models.tables.pojos.Users;
import play.api.Configuration;
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

	// @Inject
	// private Helpers helpers;

	@Inject
	public AuthenticationCtrl(FormFactory formFactory, UserService userService, ParamsServices paramsService,
			ConnexionMainServices connexion) {
		this.userService = userService;
		this.paramsService = paramsService;
		this.formFactory = formFactory;
		this.connexion = connexion;

	}

	public Result login(Request request) {

		return ok(index.render(paramsService.listesParams(), request));
	}

	public Result authentification(Request request) {

		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("MM"));

		Integer a = Integer.valueOf(now_string);
		// System.out.println(now_string +" + "+a);
		Form<Users> form = formFactory.form(Users.class).bindFromRequest(request);
		Users u = form.get();
		Users us = new Users();

		// recuperation de la gestion
		final String gestion = formFactory.form().bindFromRequest(request).get("gestion").replace(" ", "");
		System.out.println("Gestion exercice :" + gestion);

		if (a >= 13) {
			// flash("error", "Connexion Impossible, si ce problème persiste veillez
			// contacter le service Informatique!!!");
			return redirect(controllers.routes.AuthenticationCtrl.login()).flashing("error",
					"Connexion Impossible, si ce problème persiste veillez contacter le service Informatique!!!");
			// return TODO;
		} else {

			if (form.hasErrors() || userService.getUserByLogin(u.getLogin()) == null) {
				// flash("error", " Erreur de saisie");
				return redirect(controllers.routes.AuthenticationCtrl.login()).flashing("error", " Erreur de saisie");
			} else {

				if (BCryptHash.checkPassword(u.getPasse(), userService.getUserByLogin(u.getLogin()).getPasse())) {
					String password = userService.getUserByLogin(u.getLogin()).getPasse();
					us = userService.authentification(u.getLogin(), password);
				} else
					us = null;

				if (us == null) {
					// flash("error", "wrong email/password");
					return redirect(controllers.routes.AuthenticationCtrl.login()).flashing("error",
							" Erreur de saisie, Login ou Mot de Passe Incorrect!!!");
				} else {
					// System.out.println("Le User connecter est: "+ us);
//					session("login", (us.getLogin()));
//					session("droit", (us.getDroit()));
//					session("nomUser", (us.getNomPrenom()));
//					
					if (us.getConnectFirst()) {
						return redirect(controllers.routes.AuthenticationCtrl.returnFirtPageConnexion(us.getLogin()));
						// return ok(views.html.changePassword.render(us, request));
						// return TODO(request);
					} else {
						Connexion conn = new Connexion();
						conn.setUser(us.getLogin() + " - " + us.getNomPrenom());
						conn.setProfil(us.getDroit());
						conn.setWhenDown(new Timestamp(System.currentTimeMillis()));
						connexion.insert(conn);
						return redirect(routes.HomeController.acceuil()).removingFromSession(request, "droit")
								.addingToSession(request, "droit", us.getDroit()).removingFromSession(request, "login")
								.addingToSession(request, "login", us.getLogin())
								.removingFromSession(request, "nomUser")
								.addingToSession(request, "nomUser", us.getNomPrenom())
								.removingFromSession(request, "gestion").addingToSession(request, "gestion", gestion)
								.removingFromSession(request, "plafond").addingToSession(request, "plafond",
										String.valueOf(paramsService.paramsByGestion(gestion).getPlafond()))
								.removingFromSession(request, "seuilAlerte").addingToSession(request, "seuilAlerte",
										String.valueOf(paramsService.paramsByGestion(gestion).getSeuilAlerte()));
					}
				}
			}
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
			a.setWhoDone("Admin_mutuel");
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
