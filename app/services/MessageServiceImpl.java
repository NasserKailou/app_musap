package services;


import java.util.List;

import com.google.inject.Inject;

import models.Tables;
import models.tables.daos.OtpMessageDao;
import models.tables.pojos.OtpMessage;
import utils.IConnectionHelper;

public class MessageServiceImpl extends OtpMessageDao {

	private final IConnectionHelper con;

	@Inject
	public MessageServiceImpl(IConnectionHelper con) {
		super();
		this.con = con;
		setConfiguration(con.connection().configuration());

	}



	public String saveLogical(OtpMessage message, boolean b) {
		try {
			if (b)
				super.insert(message);
			else
				super.update(message);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}


	public boolean getMessageByNumBonMontant(Long numBon, Long montant){
	List<OtpMessage>	 m = con.connection().selectFrom(Tables.OTP_MESSAGE).where(Tables.OTP_MESSAGE.BON_COMMANDE.eq(numBon))
		.and(Tables.OTP_MESSAGE.MONTANT_BON.eq(montant)).fetchInto(OtpMessage.class);

		if(m==null || m.equals(null) || m.size()==0)
			return false;
		else
			return true;
	}
    
}
