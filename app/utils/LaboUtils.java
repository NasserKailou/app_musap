package utils;

import models.tables.pojos.RecuLabo;
import models.tables.pojos.TypeExamen;

/**
 * Règles métier du module laboratoire, gardées hors des POJOs jOOQ
 * (app/models est régénéré par le générateur jOOQ).
 *
 * @author nasser
 */
public final class LaboUtils {

	public static final String TYPE_ADHERENT = "ADHERENT";
	public static final String TYPE_AYANT_DROIT = "AYANT_DROIT";
	public static final String TYPE_EXTERNE = "EXTERNE";

	private LaboUtils() {
	}

	/** Tarif applicable selon que le patient est membre de la mutuelle ou non */
	public static long tarif(TypeExamen t, boolean membre) {
		if (membre && t.getMontantAdherent() != null)
			return t.getMontantAdherent();
		return t.getMontant() == null ? 0L : t.getMontant();
	}

	public static boolean estAnnule(RecuLabo r) {
		return r.getIsAnnule() != null && r.getIsAnnule();
	}

	public static String libelleTypePatient(String typePatient) {
		if (TYPE_ADHERENT.equals(typePatient))
			return "Adhérent";
		if (TYPE_AYANT_DROIT.equals(typePatient))
			return "Ayant droit";
		return "Externe";
	}
}
