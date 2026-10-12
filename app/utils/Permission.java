package utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import play.mvc.With;

/**
 * Restreint une action (ou tout un contrôleur) aux rôles possédant au moins
 * une des permissions indiquées : {@code @Permission(Permissions.LABO_RECU)}.
 *
 * @author nasser
 */
@With(PermissionAction.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
	String[] value();
}
