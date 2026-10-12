package dto;

import java.util.Set;
import java.util.TreeSet;

/**
 * Rôle applicatif avec ses permissions et le nombre d'utilisateurs rattachés.
 *
 * @author nasser
 */
public class RoleVue {

	private String code;
	private String libelle;
	private String description;
	private Boolean systeme;
	private Boolean actif;
	private Long nbUtilisateurs;
	private Set<String> permissions = new TreeSet<>();

	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }

	public String getLibelle() { return libelle; }
	public void setLibelle(String libelle) { this.libelle = libelle; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public Boolean getSysteme() { return systeme; }
	public void setSysteme(Boolean systeme) { this.systeme = systeme; }

	public Boolean getActif() { return actif; }
	public void setActif(Boolean actif) { this.actif = actif; }

	public Long getNbUtilisateurs() { return nbUtilisateurs; }
	public void setNbUtilisateurs(Long nbUtilisateurs) { this.nbUtilisateurs = nbUtilisateurs; }

	public Set<String> getPermissions() { return permissions; }
	public void setPermissions(Set<String> permissions) { this.permissions = permissions; }

	public boolean estSysteme() { return systeme != null && systeme; }
	public boolean estActif() { return actif == null || actif; }
	public boolean aPermission(String p) { return permissions.contains(p); }
}
