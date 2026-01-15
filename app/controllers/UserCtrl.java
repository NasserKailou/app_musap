/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;

import com.google.inject.Inject;

import models.tables.pojos.Users;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import play.data.FormFactory;
import play.mvc.*;
import play.mvc.Http.Request;

import static play.mvc.Results.ok;
import play.data.DynamicForm;
import play.data.Form;
import services.UserService;
import utils.Secured;
import utils.ViewMode;
import views.html.*;

/**
 *
 * @author anasser
 */
@Security.Authenticated(Secured.class)
public class UserCtrl extends Controller {

	private final FormFactory formFactory;
	private final UserService userService;
	@Inject
	public UserCtrl(FormFactory formFactory, UserService userService) {
		this.userService = userService;
		this.formFactory = formFactory;
		
	}

	public Result index(Request request) {
		System.out.println("return : " + isAdmin(request)  + " - "+isSuperAdmin(request));
		if (!isAdmin(request)) {
			if(!isSuperAdmin(request))
				return redirect(routes.AuthenticationCtrl.logout());
			
		}
		return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, ""));
	}

	public Result show(String subAction, String login, Request request) {
		System.out.println("return : " + isAdmin(request)  + " - "+isSuperAdmin(request));
		if (!isAdmin(request)) {
			if(!isSuperAdmin(request))
				return redirect(routes.AuthenticationCtrl.logout());
			
		}
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
		return ok(views.html.users.render(viewMode, users,userService.listRegion(), a,request));

	}

	public Result save(Request request) {
		
		if (!isAdmin(request)) {
			if(!isSuperAdmin(request))
				return redirect(routes.AuthenticationCtrl.logout());
			
		}
		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");
		final String login = formFactory.form().bindFromRequest(request).get("log");
		Form<Users> uForm = formFactory.form(Users.class).bindFromRequest(request);
		Users a = uForm.get();
		//Personnels p = new Personnels();
		if (userService.isUserExist(a.getLogin())) {
			
			return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, "")).flashing("error", "utilisateur avec login " + a.getLogin() + " existe déja ");
		}
		a.setWhenDone(new Timestamp(System.currentTimeMillis()));
		a.setWhoDone(String.valueOf(request.session().get("login").get()));
		a.setConnectFirst(true);
		System.err.println("User saisie est :"+a);
		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			if (userService.saveLogical(a, true).equals("ok")) {
				
				return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, "")).flashing("success", "utilisateur " + a.getLogin() + " ajouter avec success");
			} else {
				
				return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, "")).flashing("error", "utilisateur " + a.getLogin() + " non ajouter ");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			a.setLogin(login);
			if (userService.saveLogical(a, false).equals("ok")) {
				
				return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, "")).flashing("success", "utilisateur " + a.getLogin() + " modifier avec success");
			} else {
				
				return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, "")).flashing("error", "utilisateur " + a.getLogin() + " non modifier");
			}
		}
		return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, ""));
	}

	public Result delete(String login, boolean etat, Request request) {
		if (!isSuperAdmin(request)) {
			return redirect(routes.AuthenticationCtrl.logout());
		}
		if (userService.ChangeEtat(login, etat).equals("ok")) {

			return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, "")).flashing("success",
					"état utilisateur " + login + " changé avec success");
		} else {

			return redirect(routes.UserCtrl.show(ViewMode.VIEW_MODE_CREATE, "")).flashing("error",
					"état utilisateur " + login + " non changé");
		}
	}

	private boolean isAdmin(Request request) {
		return String.valueOf(request.session().get("droit").get()).equals("Admin");
	}

	private boolean isSuperAdmin(Request request) {
		return String.valueOf(request.session().get("droit").get()).equals("Admin");
	}

}
