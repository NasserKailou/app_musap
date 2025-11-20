package services.compta;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import models.tables.daos.DepenseDao;
import models.tables.pojos.Depense;
import utils.IConnectionHelper;

public class DepenseServicesImpl extends DepenseDao {

	private final IConnectionHelper con;

	@Inject
	public DepenseServicesImpl(IConnectionHelper con) {

		this.con = con;
		setConfiguration(con.connection().configuration());

	}

	public String saveLogical(Depense c, boolean b) {
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

	public List<Depense> listDepense() {
		List<Depense> liste = new ArrayList<Depense>();
		liste = con.connection().selectFrom(models.Tables.DEPENSE).where(models.Tables.DEPENSE.IS_DELETED.isFalse())
				.fetchInto(Depense.class);
		con.connection().close();
		return liste;
	}

	public List<Depense> listDepenseBeetWeen(Timestamp d1, Timestamp d2) {
		List<Depense> liste = new ArrayList<Depense>();
		liste = con.connection().selectFrom(models.Tables.DEPENSE).where(models.Tables.DEPENSE.IS_DELETED.isFalse())
				.and(models.Tables.DEPENSE.DATE_DEPENSE.between(d1, d2)).fetchInto(Depense.class);
		con.connection().close();
		return liste;
	}
}
