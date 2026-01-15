package services;

import com.google.inject.Inject;

import models.tables.daos.EtudeConsommationsDao;
import utils.IConnectionHelper;

public class ImportExcelClassServiceImpl extends EtudeConsommationsDao{
    
    private final IConnectionHelper con;

	@Inject
	public ImportExcelClassServiceImpl(IConnectionHelper con) {
		super();
		this.con = con;
		
		setConfiguration(con.connection().configuration());
	}
}
