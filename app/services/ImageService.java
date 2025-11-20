package services;

import java.nio.file.Paths;

import com.google.inject.Inject;

import models.tables.pojos.Adherent;
import models.tables.pojos.AyantDroit;
import play.libs.Files.TemporaryFile;
import utils.IConnectionHelper;


public class ImageService {

	private final IConnectionHelper con;

	private final AdherentMainServices ad;
	private final AyantDroitMainServices ay;
	
	@Inject
	public ImageService(IConnectionHelper con,  AdherentMainServices ad, AyantDroitMainServices ay) {
		super();
		this.con = con;
		this.ad = ad;
		this.ay = ay;
	}


	public String updateObject(Long id, String chemin, String extension) {
	
		System.out.println(String.valueOf(id));
		System.out.println(ad.getByMatricule(String.valueOf(id)).getId());
		Adherent c = ad.findById(ad.getByMatricule(String.valueOf(id)).getId());
		c.setPicture(chemin + "/" + id + "." + extension);
		System.out.println(c);
		return ad.saveLogical(c, false);

	}

	public String updateObject2(Long id, String chemin, String extension) {
	//	AyantDroit c = ay.findById(ad.getByMatricule(String.valueOf(id)).getId());
		AyantDroit c = ay.findById(id);	
	c.setPicture(chemin + "/" + id + "." + extension);
		System.out.println(c);
		return ay.saveLogical(c, false);

	}

	public String getExtension(String fileName) {
		String extension = "";

		int i = fileName.lastIndexOf('.');
		if (i > 0) {
			extension = fileName.substring(i + 1);
		}
		return extension;
	}

	public String isok(String chemin, String idElement, String extension, TemporaryFile file) {

		file.copyTo(Paths.get(chemin + "" + idElement + "." + extension), true);

		return updateObject(Long.parseLong(idElement), chemin, extension);
	}

	public String isok2(String chemin, String idElement, String extension, TemporaryFile file) {

		file.copyTo(Paths.get(chemin + "" + idElement + "." + extension), true);
		return updateObject2(Long.parseLong(idElement), chemin, extension);
	}

}
