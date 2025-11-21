package services;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.jooq.Configuration;

import com.google.inject.Inject;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;

import models.tables.daos.AdherentDao;
import models.tables.pojos.Adherent;
import models.tables.pojos.VAdherent;
import models.tables.pojos.VReglementGlobalByAdherent;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class AdherentMainServices extends AdherentDao {

	private final IConnectionHelper con;

	@Inject
	public AdherentMainServices(IConnectionHelper con) {

		this.con = con;
		setConfiguration(con.connection().configuration());

	}

	public String saveLogical(Adherent adherent, boolean b) {
		try {
			if (b)
				super.insert(adherent);
			else
				super.update(adherent);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	/**
	 * cette fonction retour la liste des consultations par user
	 * 
	 * @param owner
	 * @return
	 */
	public List<VAdherent> listeAdherents() {
		List<VAdherent> c = con.connection().selectFrom(models.Tables.V_ADHERENT)
				.where(models.Tables.V_ADHERENT.ON_DELETED.isFalse()).orderBy(models.Tables.V_ADHERENT.ID.desc())
				.fetchInto(VAdherent.class);
		con.connection().close();
		return c;
	}

	public Adherent getById(Long id) {
		Adherent c = con.connection().selectFrom(models.Tables.ADHERENT)
				.where(models.Tables.ADHERENT.ON_DELETED.isFalse()).and(models.Tables.ADHERENT.ID.eq(id))
				.fetchOneInto(Adherent.class);
		con.connection().close();
		return c;
	}

		public Adherent getByMatricule(String mat) {
		Adherent c = con.connection().selectFrom(models.Tables.ADHERENT)
				.where(models.Tables.ADHERENT.MATRICULE.eq(mat))
				.fetchOneInto(Adherent.class);
		con.connection().close();
		return c;
	}

	public List<VAdherent> getByTelNumber(String telephone) {
		List<VAdherent> c = con.connection().selectFrom(models.Tables.V_ADHERENT)
				.where(models.Tables.V_ADHERENT.ON_DELETED.isFalse())
				.and(models.Tables.V_ADHERENT.TELEPHONE.eq(telephone)).orderBy(models.Tables.V_ADHERENT.ID.desc())
				.fetchInto(VAdherent.class);
		con.connection().close();
		return c;
	}

	//Recuperer la consommation global d'un adhérent
	public VReglementGlobalByAdherent getVAdherentReglGlobal(String email, String gestion) {
		VReglementGlobalByAdherent c = con.connection().selectFrom(models.Tables.V_REGLEMENT_GLOBAL_BY_ADHERENT)
				.where(models.Tables.V_REGLEMENT_GLOBAL_BY_ADHERENT.ANNEE.eq(gestion))
				.and(models.Tables.V_REGLEMENT_GLOBAL_BY_ADHERENT.EMAIL.eq(email))
				.fetchOneInto(VReglementGlobalByAdherent.class);
		con.connection().close();
		return c;

	}

	/**
	 * 
	 * @param montant
	 * @return
	 */
	public String getMontantLettre(Long montant) {

		String montantLettres = "";

		NumberFormat formatter = new RuleBasedNumberFormat(Locale.FRANCE, RuleBasedNumberFormat.SPELLOUT);
		String montantL = formatter.format(montant);
		montantLettres = montantL.substring(0, 1).toUpperCase() + montantL.substring(1);

		return montantLettres;

	}

	public Timestamp getDateT(String d) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate localDate = LocalDate.parse(d, formatter);
		LocalDateTime dateV = null;

		if (null != d && !d.trim().isEmpty()) {
			try {
				// System.out.println("la date est :"+dateV+tmpDate);

				dateV = LocalDateTime.of(localDate, LocalTime.of(0, 0));

			} catch (Exception e) {
				e.getMessage();
			}
		}

		return Timestamp.valueOf(dateV);

	}

	public void backup() throws IOException {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		try {
			Runtime.getRuntime().exec("/usr/bin/pg_dump --file \"/home/ins/Bureau/backups/" + timestamp
					+ "test_dicko.backup\" --host \"127.0.0.1\" --port \"5432\" --username \"postgres\" --no-password --verbose --role \"postgres\" --format=c --blobs --encoding \"UTF8\" \"dicko\"");
			System.out.println("backup effectuer");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}
	
	public List<Adherent> findAll() {
		List<Adherent> adherents = con.connection().selectFrom(models.Tables.ADHERENT)
				.where(models.Tables.ADHERENT.ON_DELETED.isFalse())
				.fetchInto(Adherent.class);
		con.connection().close();
		return adherents;
	}
}
