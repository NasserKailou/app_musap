package dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Détail du calcul de la cotisation d'un adhérent.
 *
 * @author nasser
 */
public class CotisationAdherent {

	/** Ligne du calcul : un ayant droit et le taux qu'il apporte */
	public static class Ligne {
		private final Long idAyantDroit;
		private final String nom;
		private final String lien;
		private final double taux;
		private final boolean compte;

		public Ligne(Long idAyantDroit, String nom, String lien, double taux, boolean compte) {
			this.idAyantDroit = idAyantDroit;
			this.nom = nom;
			this.lien = lien;
			this.taux = taux;
			this.compte = compte;
		}

		public Long getIdAyantDroit() { return idAyantDroit; }
		public String getNom() { return nom; }
		public String getLien() { return lien; }
		public double getTaux() { return taux; }
		/** faux si l'ayant droit dépasse le nombre maximum pris en compte pour son lien */
		public boolean isCompte() { return compte; }
	}

	private Long idAdherent;
	private double base;
	private double total;
	private Double totalEnregistre;
	private final List<Ligne> lignes = new ArrayList<>();
	private final Map<String, Integer> nombreParLien = new LinkedHashMap<>();

	public Long getIdAdherent() { return idAdherent; }
	public void setIdAdherent(Long idAdherent) { this.idAdherent = idAdherent; }

	public double getBase() { return base; }
	public void setBase(double base) { this.base = base; }

	public double getTotal() { return total; }
	public void setTotal(double total) { this.total = total; }

	public Double getTotalEnregistre() { return totalEnregistre; }
	public void setTotalEnregistre(Double totalEnregistre) { this.totalEnregistre = totalEnregistre; }

	public List<Ligne> getLignes() { return lignes; }
	public Map<String, Integer> getNombreParLien() { return nombreParLien; }

	/** Nombre d'ayants droit actifs par lien, en JSON (pour l'estimation côté navigateur) */
	public String nombreParLienJson() {
		return play.libs.Json.toJson(nombreParLien).toString().replace("<", "\\u003c");
	}

	public boolean aUnEcart() {
		return totalEnregistre == null || Math.abs(totalEnregistre - total) > 0.0001;
	}
}
