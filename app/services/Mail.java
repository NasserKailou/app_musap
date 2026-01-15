package services;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mail {
	public static String sendMail(String toEmail, String subject, String text) {
		System.out.println("Start------");
		final String username = "mains@ins.ne";
		final String password = "Didi@2014";
		String fromEmail = "mains@ins.ne";
		// System.out.println("Variables defined------");

		Properties properties = new Properties();
		properties.put("mail.smtp.auth", "true");
		// properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.ssl.protocols", "true");
		properties.put("mail.smtp.host", "mail.ins.ne");
		// properties.put("mail.smtp.port", "7071");

		// System.out.println("Propertie defined------");

		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		MimeMessage msg = new MimeMessage(session);

		System.out.println("Mime created------");

		try {
			System.out.println("Begin send mail------");
			msg.setFrom(new InternetAddress(fromEmail));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
			msg.setSubject(subject);
			msg.setText(text);
			Transport.send(msg);
			System.out.println("Mail send ok------");
		} catch (Exception e) {
			System.out.println("Erreur send mail : " + e.getMessage());
		}

		return "ok";
	}

}
