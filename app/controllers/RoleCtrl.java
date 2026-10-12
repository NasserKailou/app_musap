package controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;

import dto.RoleVue;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.RoleServices;
import utils.Permission;
import utils.Permissions;
import utils.Secured;

/**
 * Écran Paramètrages > Rôles & permissions : création des rôles et octroi des
 * fonctionnalités (permissions) à chaque rôle.
 *
 * @author nasser
 */
@Security.Authenticated(Secured.class)
@Permission(Permissions.ROLES_GERER)
public class RoleCtrl extends Controller {

	private final RoleServices roleServices;
	private final FormFactory formFactory;

	@Inject
	public RoleCtrl(RoleServices roleServices, FormFactory formFactory) {
		this.roleServices = roleServices;
		this.formFactory = formFactory;
	}

	private String login(Request request) {
		return request.session().get("login").orElse("");
	}

	public Result index(Request request, String code) {
		List<RoleVue> roles = roleServices.listRoles();
		RoleVue selection = null;
		if (code != null && !code.isEmpty())
			selection = roleServices.findRole(code);
		if (selection == null && !roles.isEmpty() && !"__nouveau".equals(code))
			selection = roles.get(0);
		return ok(views.html.roles.render(roles, selection, Permissions.parModule(), request));
	}

	public Result saveRole(Request request) {
		DynamicForm f = formFactory.form().bindFromRequest(request);
		boolean creation = "CREATE".equals(f.get("mode"));
		String code = f.get("code") == null ? "" : f.get("code").trim();
		String res = roleServices.enregistrerRole(code, f.get("libelle"), f.get("description"),
				"true".equals(f.get("actif")), creation, login(request));
		if ("ok".equals(res))
			return redirect(routes.RoleCtrl.index(code)).flashing("success",
					"Rôle " + f.get("libelle") + (creation ? " créé" : " modifié") + " avec succès"
							+ (creation ? " : cochez maintenant ses permissions" : ""));
		return redirect(routes.RoleCtrl.index(creation ? "__nouveau" : code)).flashing("error", res);
	}

	public Result savePermissions(Request request) {
		Map<String, String[]> data = request.body().asFormUrlEncoded();
		String code = data != null && data.get("code") != null ? data.get("code")[0] : "";
		Set<String> permissions = new HashSet<>();
		if (data != null && data.get("permissions[]") != null)
			for (String p : data.get("permissions[]"))
				permissions.add(p);
		String res = roleServices.definirPermissions(code, permissions, login(request));
		if ("ok".equals(res))
			return redirect(routes.RoleCtrl.index(code)).flashing("success", permissions.size()
					+ " permission(s) accordée(s) au rôle. Les changements s'appliquent immédiatement.");
		return redirect(routes.RoleCtrl.index(code)).flashing("error", res);
	}

	public Result deleteRole(Request request) {
		DynamicForm f = formFactory.form().bindFromRequest(request);
		String code = f.get("code");
		String res = roleServices.supprimerRole(code);
		if ("ok".equals(res))
			return redirect(routes.RoleCtrl.index("")).flashing("success", "Rôle supprimé");
		return redirect(routes.RoleCtrl.index(code)).flashing("error", res);
	}
}
