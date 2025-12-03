package services;

import javax.inject.Singleton;

import com.google.inject.Inject;

import models.tables.daos.OtpMessageDao;
import models.tables.pojos.OtpMessage;
import models.tables.pojos.VReglement;
import play.libs.ws.WSResponse;
import utils.IConnectionHelper;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.concurrent.CompletionStage;

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

      String message = "Alerte avertissement !!! Cher(e) adherent, vous avez dépassé votre credit annuel de: 999999F Moderez votre consommation afin d'eviter la suspension. Merci";
        smsClient.sendSms(phoneE164, message);
    }

    public void sendSMSBC(VReglement reglement) {
    
    OtpMessage sms = new OtpMessage();
    
     String messageCourt = "Cher(e) adherent, votre bon " + reglement.getId() + " d'un montant de " + reglement.getMontantReglement() + " F CFA a ete emis avec succes. Si vous netes pas l'auteur, contactez la MUSAPOSTE. Merci.";
    
     try {
            CompletionStage<WSResponse> future = smsClient.sendSms(reglement.getTelephone(), messageCourt);

            WSResponse response = future.toCompletableFuture().get();

            System.out.println("Retour API : " + response.getBody());

            sms.setIsSent(true);
            sms.setIsUsed(true);
            sms.setSentResponse(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erreur envoi SMS : " + e.getMessage());

            sms.setIsSent(false);
            sms.setIsUsed(false);
            sms.setSentResponse("ERROR: " + e.getMessage());
        }


    sms.setBonCommande(reglement.getId());
    sms.setPhone(reglement.getTelephone());
    sms.setMessageTexte(messageCourt);
    sms.setCreatedAt(new Timestamp(System.currentTimeMillis()));
       try{
        //messageService.insert(sms);
       } catch(Exception e){
        System.out.println("Save error"+ e.getMessage());
       }
   //System.out.println("Save retour :"+ this.saveLogical(sms, false)); 
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