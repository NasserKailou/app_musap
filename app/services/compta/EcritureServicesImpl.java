package services.compta;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import models.tables.daos.EcritureDao;
import models.tables.pojos.Ecriture;
import models.tables.pojos.VEcriture;
import utils.IConnectionHelper;

public class EcritureServicesImpl extends EcritureDao {

	private final IConnectionHelper con;

	@Inject
	public EcritureServicesImpl(IConnectionHelper con) {

		this.con = con;
		setConfiguration(con.connection().configuration());

	}

	public String saveLogical(Ecriture c, boolean b) {
		try {
			if (b)
				super.insert(c);
			else
				super.update(c);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public List<VEcriture> listeEcriture(long idDepense) {
		List<VEcriture> liste = new ArrayList<VEcriture>();
		liste = con.connection().selectFrom(models.Tables.V_ECRITURE)
				.where(models.Tables.V_ECRITURE.ID_DEPENSE.eq(idDepense))
				.and(models.Tables.V_ECRITURE.IS_DELETED.isFalse()).fetchInto(VEcriture.class);
		con.connection().close();
		return liste;
	}

	public List<VEcriture> listeEcritureBetWeen(Timestamp d1, Timestamp d2) {
		List<VEcriture> liste = new ArrayList<VEcriture>();
		liste = con.connection().selectFrom(models.Tables.V_ECRITURE)
				.where(models.Tables.V_ECRITURE.DATE_OPERATION.between(d1, d2))
				.and(models.Tables.V_ECRITURE.IS_DELETED.isFalse()).fetchInto(VEcriture.class);
		con.connection().close();
		return liste;
	}

	public List<VEcriture> listeEcritureBetWeen(Timestamp d1, Timestamp d2, long numCompte) {
		List<VEcriture> liste = new ArrayList<VEcriture>();
		liste = con.connection().selectFrom(models.Tables.V_ECRITURE)
				.where(models.Tables.V_ECRITURE.DATE_OPERATION.between(d1, d2))
				.and(models.Tables.V_ECRITURE.COMPTE.eq(numCompte)).and(models.Tables.V_ECRITURE.IS_DELETED.isFalse())
				.fetchInto(VEcriture.class);
		con.connection().close();
		return liste;
	}

	public List<VEcriture> listeEcritureByCompte(long numCompte) {
		List<VEcriture> liste = new ArrayList<VEcriture>();
		liste = con.connection().selectFrom(models.Tables.V_ECRITURE)
				.where(models.Tables.V_ECRITURE.COMPTE.eq(numCompte)).and(models.Tables.V_ECRITURE.IS_DELETED.isFalse())
				.fetchInto(VEcriture.class);
		con.connection().close();
		return liste;
	}

	public List<VEcriture> listeEcritureByCompteByDaye(Timestamp d1, long numCompte) {
		List<VEcriture> liste = new ArrayList<VEcriture>();
		liste = con.connection().selectFrom(models.Tables.V_ECRITURE)
				.where(models.Tables.V_ECRITURE.COMPTE.eq(numCompte)).and(models.Tables.V_ECRITURE.IS_DELETED.isFalse())
				.and(models.Tables.V_ECRITURE.DATE_OPERATION.eq(d1)).fetchInto(VEcriture.class);
		con.connection().close();
		return liste;
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
}
