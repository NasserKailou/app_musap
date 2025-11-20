package services;

import java.util.List;

import com.google.inject.Inject;

import org.jooq.Configuration;

import models.tables.daos.CategorieDao;
import models.tables.pojos.Categorie;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class CategorieMainServices extends CategorieDao {

	private final IConnectionHelper con;
	

	@Inject
	public CategorieMainServices(IConnectionHelper con) {
		super();
		this.con = con;
		
		setConfiguration(con.connection().configuration());
	}


	public String saveLogical(Categorie cat, boolean b) {
		try {
			if (b)
				super.insert(cat);
			else
				super.update(cat);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public List<Categorie> findAll() {
		List<Categorie> c = con.connection().selectFrom(models.Tables.CATEGORIE)
				.where(models.Tables.CATEGORIE.ON_DELETED.isFalse()).fetchInto(Categorie.class);
		con.connection().close();
		return c;

	}
}
