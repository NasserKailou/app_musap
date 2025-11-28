package services;

import javax.inject.Inject;
import javax.inject.Singleton;

import models.tables.daos.OtpMessageDao;
import models.tables.pojos.OtpMessage;
import models.tables.pojos.Params;
import utils.IConnectionHelper;

import java.security.SecureRandom;

@Singleton
public class OtpService extends OtpMessageDao {

    private final IConnectionHelper con;

    private final SawkiSmsClient smsClient;
    private final SecureRandom random = new SecureRandom();
    
	@Inject
    public OtpService(IConnectionHelper con, SawkiSmsClient smsClient) {
        this.con = con;
        this.smsClient = smsClient;
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

 
    public String generateCode() {
        int code = 100000 + random.nextInt(900000); // 6 chiffres
        return String.valueOf(code);
    }

    public void sendOtp(String phoneE164) {
        String code = generateCode();

        // TODO: sauvegarder dans la base (table otp_codes) avec phone + code + expiry
        // ex: expiry = now + 5 minutes
        // OTP_TABLE.insert(phoneE164, code, Instant.now().plus(5, ChronoUnit.MINUTES));

        //String message = "MUSAP : votre code OTP est " + code + ". Il est valable 5 minutes.";
      //String message=  "MUSAP : Un bon/prise en charge a été émis à votre nom. Votre code OTP est :"+code +" Présentez-le à la structure de soins pour validation";
      String message = "Alerte avertissement !!! Cher(e) adherent, vous avez dépassé votre credit annuel de: 999999F Moderez votre consommation afin d'eviter la suspension. Merci";
        smsClient.sendSms(phoneE164, message);
    }

    public boolean verifyOtp(String phoneE164, String codeSaisi) {
        // TODO: aller chercher dans la base le dernier OTP pour ce téléphone
        // OTP otp = OTP_TABLE.findByPhone(phoneE164);

        // pseudo-code :
        /*
        if (otp == null) return false;
        if (otp.isUsed()) return false;
        if (Instant.now().isAfter(otp.getExpireAt())) return false;
        if (!otp.getCode().equals(codeSaisi)) return false;

        // marquer comme utilisé
        otp.setUsed(true);
        OTP_TABLE.update(otp);
        return true;
        */

        return false; // à remplacer par la vraie logique
    }
}
