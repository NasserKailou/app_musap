package services.compta;

import java.util.List;

import com.google.inject.Inject;

import models.tables.daos.ClassCompteDao;
import models.tables.pojos.ClassCompte;
import models.tables.pojos.VAdherentAyantDroit;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class ClassComptaServicesImpl extends ClassCompteDao {

	private final IConnectionHelper con;

	@Inject
	public ClassComptaServicesImpl(IConnectionHelper con) {

		this.con = con;
		setConfiguration(con.connection().configuration());

	}

	public String saveLogical(ClassCompte c, boolean b) {
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

	public List<ClassCompte> listeCompte() {
		List<ClassCompte> c = con.connection().selectFrom(models.Tables.CLASS_COMPTE)
				.where(models.Tables.CLASS_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CLASS_COMPTE.IS_FOR_BUDGET.isFalse()).fetchInto(ClassCompte.class);
		con.connection().close();
		return c;
	}

	public List<ClassCompte> listeCompteByCategorieC(Long categorie) {
		List<ClassCompte> c = con.connection().selectFrom(models.Tables.CLASS_COMPTE)
				.where(models.Tables.CLASS_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CLASS_COMPTE.CATEGORIE.eq(categorie))
				.and(models.Tables.CLASS_COMPTE.IS_FOR_BUDGET.isFalse()).fetchInto(ClassCompte.class);
		con.connection().close();
		return c;
	}

	public List<ClassCompte> listeCompteByCategorieB(Long categorie) {
		List<ClassCompte> c = con.connection().selectFrom(models.Tables.CLASS_COMPTE)
				.where(models.Tables.CLASS_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CLASS_COMPTE.CATEGORIE.eq(categorie))
				.and(models.Tables.CLASS_COMPTE.IS_FOR_BUDGET.isTrue()).fetchInto(ClassCompte.class);
		con.connection().close();
		return c;
	}
	
	public List<ClassCompte> findLikeNumCompteC(String query) {
		String detail = null == query ? "" : query;
		detail = detail.endsWith("%") ? detail : detail + "%";
		List<ClassCompte> detailsList = con.connection().selectFrom(models.Tables.CLASS_COMPTE)
				.where(models.Tables.CLASS_COMPTE.NUM_COMPTE.like(detail))
				.and(models.Tables.CLASS_COMPTE.IS_FOR_BUDGET.isFalse()).fetchInto(ClassCompte.class);
		con.connection().close();
		return detailsList;
	}

	public List<ClassCompte> findLikeNumCompteB(String query) {
		String detail = null == query ? "" : query;
		detail = detail.endsWith("%") ? detail : detail + "%";
		List<ClassCompte> detailsList = con.connection().selectFrom(models.Tables.CLASS_COMPTE)
				.where(models.Tables.CLASS_COMPTE.NUM_COMPTE.like(detail))
				.and(models.Tables.CLASS_COMPTE.IS_FOR_BUDGET.isTrue()).fetchInto(ClassCompte.class);
		con.connection().close();
		return detailsList;
	}
	
	public List<ClassCompte> listeCompteBudget() {
		List<ClassCompte> c = con.connection().selectFrom(models.Tables.CLASS_COMPTE)
				.where(models.Tables.CLASS_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CLASS_COMPTE.IS_FOR_BUDGET.isTrue()).fetchInto(ClassCompte.class);
		con.connection().close();
		return c;
	}

	public List<ClassCompte> listeCompteByCategorieBudget(Long categorie) {
		List<ClassCompte> c = con.connection().selectFrom(models.Tables.CLASS_COMPTE)
				.where(models.Tables.CLASS_COMPTE.IS_DELETED.isFalse())
				.and(models.Tables.CLASS_COMPTE.CATEGORIE.eq(categorie))
				.and(models.Tables.CLASS_COMPTE.IS_FOR_BUDGET.isTrue()).fetchInto(ClassCompte.class);
		con.connection().close();
		return c;
	}
	
	public List<ClassCompte> findLikeNumCompteForBudget(String query) {
		String detail = null == query ? "" : query;
		detail = detail.endsWith("%") ? detail : detail + "%";
		List<ClassCompte> detailsList = con.connection().selectFrom(models.Tables.CLASS_COMPTE)
				.where(models.Tables.CLASS_COMPTE.NUM_COMPTE.like(detail))
				.and(models.Tables.CLASS_COMPTE.IS_FOR_BUDGET.isTrue()).fetchInto(ClassCompte.class);
		con.connection().close();
		return detailsList;
	}
}
