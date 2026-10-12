package dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Filtre des rapports d'opérations : journée, mois ou période libre, type
 * (BON / PRISE EN CHARGE) et région de l'émetteur.
 *
 * @author nasser
 */
public class FiltreRapport {

	private static final DateTimeFormatter AFFICHAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private String periode = "jour";
	private LocalDate date;
	private YearMonth mois;
	private LocalDate debut;
	private LocalDate fin;
	private String type = "";
	private Long region;
	private String libelleRegion;

	/** Construit le filtre à partir des paramètres de la requête (valeurs invalides ignorées) */
	public static FiltreRapport depuis(String periode, String date, String mois, String debut, String fin, String type,
			String region, String periodeParDefaut) {
		FiltreRapport f = new FiltreRapport();
		f.periode = "mois".equals(periode) || "periode".equals(periode) || "jour".equals(periode) ? periode
				: periodeParDefaut;
		LocalDate aujourdhui = LocalDate.now();
		f.date = parseDate(date, aujourdhui);
		f.mois = parseMois(mois, YearMonth.from(aujourdhui));
		if ("jour".equals(f.periode)) {
			f.debut = f.date;
			f.fin = f.date;
		} else if ("mois".equals(f.periode)) {
			f.debut = f.mois.atDay(1);
			f.fin = f.mois.atEndOfMonth();
		} else {
			f.debut = parseDate(debut, aujourdhui.withDayOfMonth(1));
			f.fin = parseDate(fin, aujourdhui);
			if (f.fin.isBefore(f.debut)) {
				LocalDate t = f.debut;
				f.debut = f.fin;
				f.fin = t;
			}
		}
		f.type = type != null && (type.equalsIgnoreCase("BC") || type.equalsIgnoreCase("PC")) ? type.toUpperCase() : "";
		try {
			f.region = region == null || region.trim().isEmpty() ? null : Long.valueOf(region.trim());
		} catch (NumberFormatException e) {
			f.region = null;
		}
		return f;
	}

	private static LocalDate parseDate(String s, LocalDate defaut) {
		try {
			return s == null || s.trim().isEmpty() ? defaut : LocalDate.parse(s.trim());
		} catch (Exception e) {
			return defaut;
		}
	}

	private static YearMonth parseMois(String s, YearMonth defaut) {
		try {
			return s == null || s.trim().isEmpty() ? defaut : YearMonth.parse(s.trim());
		} catch (Exception e) {
			return defaut;
		}
	}

	public String getPeriode() { return periode; }
	public LocalDate getDate() { return date; }
	public YearMonth getMois() { return mois; }
	public LocalDate getDebut() { return debut; }
	public LocalDate getFin() { return fin; }
	public String getType() { return type; }
	public Long getRegion() { return region; }
	public String getLibelleRegion() { return libelleRegion; }
	public void setLibelleRegion(String libelleRegion) { this.libelleRegion = libelleRegion; }

	/** Libellé lisible de la période : "Journée du 10/10/2026", "Mois d'octobre 2026"... */
	public String getLibellePeriode() {
		if ("jour".equals(periode))
			return "Journée du " + date.format(AFFICHAGE);
		if ("mois".equals(periode)) {
			String m = mois.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
			return "Mois " + ("aeiouy".indexOf(m.charAt(0)) >= 0 ? "d'" : "de ") + m + " " + mois.getYear();
		}
		return "Du " + debut.format(AFFICHAGE) + " au " + fin.format(AFFICHAGE);
	}

	public String getLibelleType() {
		return "BC".equals(type) ? "Bons" : "PC".equals(type) ? "Prises en charge" : "Bons et prises en charge";
	}

	/** Paramètres de requête reproduisant ce filtre (export, retour après restauration) */
	public String query() {
		return "periode=" + periode + "&date=" + date + "&mois=" + mois + "&debut=" + debut + "&fin=" + fin
				+ "&type=" + type + "&region=" + (region == null ? "" : region);
	}
}
