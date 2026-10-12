/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;

import com.google.inject.Inject;

import dto.RoleVue;
import models.tables.pojos.Users;

import java.sql.Timestamp;
import java.util.List;

import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.*;
import play.mvc.Http.Request;

import services.RoleServices;
import services.UserService;
import utils.BCryptHash;
import utils.Permission;
import utils.Permissions;
import utils.Secured;
import utils.ViewMode;

/**
 * Gestion des utilisateurs : le rôle (users.droit) est choisi parmi les rôles
 * actifs définis dans Paramètrages > Rôles & permissions.
 *
 * @author anasser
 */
@Security.Authenticated(Secured.class)
@Permission(Permissions.UTILISATEURS_GERER)
public class UserCtrl extends Controller {

	private final FormFactory formFactory;
	private final UserService userService;
	private final RoleServices roleServices;

	@Inject
	public UserCtrl(FormFactory formFactory, UserService userService, RoleServices roleServices) {
		this.userService = userService;
		this.formFactory = formFactory;
		this.roleServices = roleServices;
	}

	public Result index(Request request) {
		return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, ""));
	}

	public Result show(String subAction, String login, Request request) {
		String viewMode;
		Users a;
		List<Users> users = userService.getAllUser();
		if (null == login || login.equals("")) {
			a = new Users();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			a = userService.getUserByLogin(login);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			a = userService.getUserByLogin(login);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			a = userService.getUserByLogin(login);

		}
		if (a == null) {
			a = new Users();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		}
		return ok(views.html.users.render(viewMode, users, userService.listRegion(), a, roleServices.listRoles(),
				request));

	}

	public Result save(Request request) {
		DynamicForm f = formFactory.form().bindFromRequest(request);
		final String viewMode = f.get("viewMode") == null ? "" : f.get("viewMode");
		Result retour = redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, ""));

		String login = trim(f.get("login"));
		String nomPrenom = trim(f.get("nomPrenom"));
		String droit = trim(f.get("droit"));
		String passe = f.get("passe") == null ? "" : f.get("passe");
		Long region = parseLong(f.get("regionCode"));
		boolean etat = !"false".equalsIgnoreCase(f.get("etat"));

		if (login.isEmpty() || nomPrenom.isEmpty())
			return retour.flashing("error", "Le login et le nom sont obligatoires");
		RoleVue role = roleServices.findRole(droit);
		if (role == null || !role.estActif())
			return retour.flashing("error", "Veuillez choisir un rôle actif");

		Timestamp now = new Timestamp(System.currentTimeMillis());
		String auteur = request.session().get("login").orElse("");

		if (ViewMode.VIEW_MODE_CREATE.equals(viewMode)) {
			if (userService.isUserExist(login))
				return retour.flashing("error", "utilisateur avec login " + login + " existe déja ");
			if (passe.length() < 4)
				return retour.flashing("error", "Le mot de passe initial doit contenir au moins 4 caractères");
			Users a = new Users();
			a.setLogin(login);
			a.setNomPrenom(nomPrenom);
			a.setDroit(role.getCode());
			a.setEtat(etat);
			a.setRegionCode(region);
			a.setPasse(passe); // haché par saveLogical
			a.setConnectFirst(true); // changement de mot de passe à la première connexion
			a.setWhenDone(now);
			a.setWhoDone(auteur);
			if (userService.saveLogical(a, true).equals("ok"))
				return retour.flashing("success", "utilisateur " + login + " ajouté avec succès");
			return retour.flashing("error", "utilisateur " + login + " non ajouté");
		}

		if (ViewMode.VIEW_MODE_EDIT.equals(viewMode)) {
			// Avant : la vérification "login existe déjà" bloquait toute modification et le
			// mot de passe était re-haché (même vide). On modifie désormais l'utilisateur existant.
			Long id = parseLong(f.get("id"));
			Users existant = id == null ? null : userService.fetchOneById(id);
			if (existant == null)
				existant = userService.getUserByLogin(trim(f.get("log")).isEmpty() ? login : trim(f.get("log")));
			if (existant == null)
				return retour.flashing("error", "Utilisateur introuvable");
			if (!existant.getLogin().equals(login) && userService.isUserExist(login))
				return retour.flashing("error", "Le login " + login + " est déjà utilisé");
			boolean sonPropreCompte = existant.getLogin().equals(auteur);
			if (sonPropreCompte && Permissions.ROLE_ADMIN.equals(existant.getDroit())
					&& !Permissions.ROLE_ADMIN.equals(role.getCode())) {
				return retour.flashing("error", "Vous ne pouvez pas retirer votre propre rôle Administrateur");
			}
			existant.setLogin(login);
			existant.setNomPrenom(nomPrenom);
			existant.setDroit(role.getCode());
			existant.setRegionCode(region);
			if (f.get("etat") != null && !(sonPropreCompte && !etat))
				existant.setEtat(etat);
			if (!passe.isEmpty()) {
				if (passe.length() < 4)
					return retour.flashing("error", "Le nouveau mot de passe doit contenir au moins 4 caractères");
				existant.setPasse(BCryptHash.hashPassword(passe));
				existant.setConnectFirst(true); // réinitialisation : à changer à la prochaine connexion
			}
			existant.setWhenDone(now);
			existant.setWhoDone(auteur);
			try {
				userService.update(existant);
				return retour.flashing("success", "utilisateur " + login + " modifié avec succès"
						+ (passe.isEmpty() ? "" : " (mot de passe réinitialisé)"));
			} catch (Exception e) {
				return retour.flashing("error", "utilisateur " + login + " non modifié");
			}
		}
		return retour;
	}

	public Result delete(String login, boolean etat, Request request) {
		if (!etat && login.equals(request.session().get("login").orElse(""))) {
			return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, "")).flashing("error",
					"Vous ne pouvez pas désactiver votre propre compte");
		}
		if (userService.getUserByLogin(login) != null && userService.ChangeEtat(login, etat).equals("ok")) {

			return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, "")).flashing("success",
					"état utilisateur " + login + " changé avec success");
		} else {

			return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, "")).flashing("error",
					"état utilisateur " + login + " non changé");
		}
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}

	private static Long parseLong(String s) {
		try {
			return s == null || s.trim().isEmpty() ? null : Long.valueOf(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
