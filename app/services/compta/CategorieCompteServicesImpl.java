package services.compta;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import models.tables.daos.CategorieCompteDao;
import models.tables.pojos.CategorieCompte;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class CategorieCompteServicesImpl extends CategorieCompteDao {

	private final IConnectionHelper con;

	@Inject
	public CategorieCompteServicesImpl(IConnectionHelper con) {

		this.con = con;
		setConfiguration(con.connection().configuration());

	}

	public String saveLogical(CategorieCompte c, boolean b) {
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

	public List<CategorieCompte> listeCategorieCompteC() {

		List<CategorieCompte> listes = new ArrayList<CategorieCompte>();
		listes = con.connection().selectFrom(models.Tables.CATEGORIE_COMPTE)
				.where(models.Tables.CATEGORIE_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CATEGORIE_COMPTE.IS_FOR_BUDGET.isFalse()).fetchInto(CategorieCompte.class);
		con.connection().close();
		return listes;
	}
	
	public List<CategorieCompte> listeCategorieCompteB() {

		List<CategorieCompte> listes = new ArrayList<CategorieCompte>();
		listes = con.connection().selectFrom(models.Tables.CATEGORIE_COMPTE)
				.where(models.Tables.CATEGORIE_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CATEGORIE_COMPTE.IS_FOR_BUDGET.isTrue()).fetchInto(CategorieCompte.class);
		con.connection().close();
		return listes;
	}

	public CategorieCompte listeCategorieCompteByNumC(Long compte) {

		CategorieCompte c = new CategorieCompte();
		c = con.connection().selectFrom(models.Tables.CATEGORIE_COMPTE)
				.where(models.Tables.CATEGORIE_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CATEGORIE_COMPTE.NUM_CATEGORE.eq(compte))
				.and(models.Tables.CATEGORIE_COMPTE.IS_FOR_BUDGET.isFalse()).fetchOneInto(CategorieCompte.class);
		con.connection().close();
		return c;
	}

	public CategorieCompte listeCategorieCompteByNumB(Long compte) {

		CategorieCompte c = new CategorieCompte();
		c = con.connection().selectFrom(models.Tables.CATEGORIE_COMPTE)
				.where(models.Tables.CATEGORIE_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CATEGORIE_COMPTE.NUM_CATEGORE.eq(compte))
				.and(models.Tables.CATEGORIE_COMPTE.IS_FOR_BUDGET.isTrue()).fetchOneInto(CategorieCompte.class);
		con.connection().close();
		return c;
	}
	
	public List<CategorieCompte> listeCategorieCompteBudget() {

		List<CategorieCompte> listes = new ArrayList<CategorieCompte>();
		listes = con.connection().selectFrom(models.Tables.CATEGORIE_COMPTE)
				.where(models.Tables.CATEGORIE_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CATEGORIE_COMPTE.IS_FOR_BUDGET.isTrue())
				.and(models.Tables.CATEGORIE_COMPTE.IS_FOR_BUDGET.isFalse()).fetchInto(CategorieCompte.class);
		con.connection().close();
		return listes;
	}

	public CategorieCompte listeCategorieCompteByNumBudget(Long compte) {

		CategorieCompte c = new CategorieCompte();
		c = con.connection().selectFrom(models.Tables.CATEGORIE_COMPTE)
				.where(models.Tables.CATEGORIE_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CATEGORIE_COMPTE.NUM_CATEGORE.eq(compte))
				.and(models.Tables.CATEGORIE_COMPTE.IS_FOR_BUDGET.isTrue()).fetchOneInto(CategorieCompte.class);
		con.connection().close();
		return c;
	}
}
