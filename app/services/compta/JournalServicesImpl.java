package services.compta;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import models.tables.daos.JournalEcritureDao;
import models.tables.pojos.JournalEcriture;
import models.tables.pojos.VEcriture;
import utils.IConnectionHelper;

public class JournalServicesImpl extends JournalEcritureDao{


	private final IConnectionHelper con;

	@Inject
	public JournalServicesImpl(IConnectionHelper con) {

		this.con = con;
		setConfiguration(con.connection().configuration());

	}

	public String saveLogical(JournalEcriture c, boolean b) {
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
	
	public List<JournalEcriture> listeJournal() {
		List<JournalEcriture> liste = new ArrayList<JournalEcriture>();
		liste = con.connection().selectFrom(models.Tables.JOURNAL_ECRITURE)
				.where(models.Tables.JOURNAL_ECRITURE.IS_DELETED.isFalse()).fetchInto(JournalEcriture.class);
		con.connection().close();
		return liste;
	}
	
	
}
