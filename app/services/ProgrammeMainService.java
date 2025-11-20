package services;

import java.util.List;
import com.google.inject.Inject;
import org.jooq.Configuration;
import models.tables.daos.ProgrammeDao;
import models.tables.pojos.Programme;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class ProgrammeMainService extends ProgrammeDao {

	private final IConnectionHelper con;

	@Inject
	public ProgrammeMainService(IConnectionHelper con) {
		super();
		this.con = con;
		
		setConfiguration(con.connection().configuration());
	}
	
	public String saveLogical(Programme pgm, boolean b) {
		try {
			if (b)
				super.insert(pgm);
			else
				super.update(pgm);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public List<Programme> findAllProgamme() {

		List<Programme> listes = con.connection().selectFrom(models.Tables.PROGRAMME)
				.where(models.Tables.PROGRAMME.ON_DELETED.isFalse()).fetchInto(Programme.class);
		con.connection().close();
		
		return listes;
	}

}
