 DROP VIEW public.v_depassement;

DROP VIEW public.v_reglement_globale;

CREATE OR REPLACE VIEW public.v_reglement_globale
 AS
 SELECT ad.matricule,
    ad.nom_ad,
    ad.prenom_ad,
	ad.code_carnet,
	reg.when_done,
    sum(reg.montant_reglement)::bigint AS total_annuel,
    reg.annee
   FROM v_reglement reg,
    adherent ad
  WHERE reg.id_adherent = ad.id AND reg.on_deleted IS FALSE
  GROUP BY reg.annee, ad.matricule, ad.nom_ad, ad.prenom_ad,ad.code_carnet,reg.when_done
  ORDER BY ad.matricule;

  
CREATE OR REPLACE VIEW public.v_depassement
 AS
 SELECT v_reglement_globale.matricule,
    v_reglement_globale.nom_ad,
    v_reglement_globale.prenom_ad,
    v_reglement_globale.total_annuel,
    v_reglement_globale.annee
   FROM v_reglement_globale,
    params pr
  WHERE v_reglement_globale.annee::text = pr.gestion::text AND v_reglement_globale.total_annuel >= pr.plafond;

