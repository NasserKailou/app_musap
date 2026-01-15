DROP VIEW public.v_total_ayant_droit_conjoint;
DROP VIEW public.v_total_ayant_droit_enfant;
DROP VIEW public.v_effectif_mains_ayant_droit;
DROP VIEW public.v_total_adherents;
--DROP VIEW public.v_effectif_mains_ayant_droit;
DROP VIEW public.v_effectif_mains;
DROP VIEW public.v_adherent; 


CREATE OR REPLACE VIEW public.v_adherent AS
SELECT
    ad.id,
    ad.nom_ad,
    ad.prenom_ad,
    ad.sexe,
    ad.matricule,
    ad.code_carnet,
    (cat.code::text || '-'::text) || cat.libelle::text AS categorie,
    ad.fonction,
    ad.structure_sigle,
    ad.date_naiss,
    date_part('year', age(CURRENT_DATE::timestamp with time zone,
                         ad.date_naiss::date::timestamp with time zone))::integer AS age,
    ad.date_prise_service,
    date_part('year', age(CURRENT_DATE::timestamp with time zone,
                         ad.date_prise_service::date::timestamp with time zone))::integer AS anciennete,
    ad.salaire_net,
    ad.pourcentage_total_retenue,
	ad.is_member,
	ad.email,
    ad.salaire_net::double precision * ad.pourcentage_total_retenue / 100::double precision AS total_cotisation_agent,
    ad.salaire_net::double precision * ad.pourcentage_total_retenue / 100::double precision * 3::double precision * 10::double precision AS total_credit_annuelle,
    ad.salaire_net::double precision * ad.pourcentage_total_retenue / 100::double precision * 3::double precision * 10::double precision * 2::double precision AS total_credit_a_consomer,
  COALESCE(SUM(reg.montant_total), 0)      AS montant_total_brute,
COALESCE(SUM(reg.montant_reglement), 0)  AS montant_reglement_couvert,
COALESCE(SUM(reg.montant_paye), 0)       AS montant_payer,
  ad.picture,
    ad.when_done,
    ad.who_done,
    ad.on_deleted,
    dir.libelle  AS direction,
    dvi.libelle  AS division,
    serv.libelle AS service,
    ad.telephone
FROM adherent ad
JOIN categorie cat ON cat.code::text = ad.categorie::text
JOIN direction dir ON dir.id = ad.direction
JOIN division dvi ON dvi.id = ad.division
JOIN service serv ON serv.id = ad.service
left JOIN v_reglement reg ON reg.id_adherent = ad.id
GROUP BY
    ad.id,
    ad.nom_ad,
    ad.prenom_ad,
    ad.sexe,
    ad.matricule,
    ad.code_carnet,
    cat.code,
    cat.libelle,
    ad.fonction,
    ad.structure_sigle,
    ad.date_naiss,
    ad.date_prise_service,
    ad.salaire_net,
    ad.pourcentage_total_retenue,
    ad.picture,
    ad.when_done,
    ad.who_done,
    ad.on_deleted,
    dir.libelle,
    dvi.libelle,
    serv.libelle,
    ad.telephone;



CREATE OR REPLACE VIEW public.v_effectif_mains
 AS
 SELECT v_adherent.id,
    v_adherent.nom_ad,
    v_adherent.prenom_ad,
    v_adherent.sexe,
    v_adherent.matricule,
    v_adherent.categorie,
    v_adherent.fonction,
    v_adherent.structure_sigle,
    v_adherent.date_naiss,
    v_adherent.age,
    v_adherent.date_prise_service,
    v_adherent.anciennete,
    v_adherent.picture,
    v_adherent.when_done,
    v_adherent.who_done,
    v_adherent.on_deleted,
    v_adherent.direction,
    v_adherent.division,
    v_adherent.service,
    v_adherent.telephone
   FROM v_adherent
  WHERE v_adherent.age <= 60::double precision AND (v_adherent.id IN ( SELECT reglement.adherent
           FROM reglement));

CREATE OR REPLACE VIEW public.v_effectif_mains_ayant_droit
 AS
 SELECT v_ayant_droit.id,
    v_ayant_droit.nom_ay,
    v_ayant_droit.date_naiss,
    v_ayant_droit.age,
    v_ayant_droit.adherent,
    v_ayant_droit.nom_adherent,
    v_ayant_droit.matricule,
    v_ayant_droit.when_done,
    v_ayant_droit.who_done,
    v_ayant_droit.on_deleted,
    v_ayant_droit.picture,
    v_ayant_droit.lien,
    v_ayant_droit.genre
   FROM v_ayant_droit
  WHERE (v_ayant_droit.adherent IN ( SELECT v_effectif_mains.id
           FROM v_effectif_mains));

CREATE OR REPLACE VIEW public.v_effectif_mains_ayant_droit
 AS
 SELECT v_ayant_droit.id,
    v_ayant_droit.nom_ay,
    v_ayant_droit.date_naiss,
    v_ayant_droit.age,
    v_ayant_droit.adherent,
    v_ayant_droit.nom_adherent,
    v_ayant_droit.matricule,
    v_ayant_droit.when_done,
    v_ayant_droit.who_done,
    v_ayant_droit.on_deleted,
    v_ayant_droit.picture,
    v_ayant_droit.lien,
    v_ayant_droit.genre
   FROM v_ayant_droit
  WHERE (v_ayant_droit.adherent IN ( SELECT v_effectif_mains.id
           FROM v_effectif_mains));

CREATE OR REPLACE VIEW public.v_total_ayant_droit_conjoint
 AS
 SELECT count(v_effectif_mains_ayant_droit.id) AS total_ayant_droit_conjoint,
    v_effectif_mains_ayant_droit.genre,
    v_effectif_mains_ayant_droit.lien
   FROM v_effectif_mains_ayant_droit
  WHERE v_effectif_mains_ayant_droit.lien::text = 'CONJOINT'::text
  GROUP BY v_effectif_mains_ayant_droit.genre, v_effectif_mains_ayant_droit.lien;



CREATE OR REPLACE VIEW public.v_total_ayant_droit_enfant
 AS
 SELECT count(v_effectif_mains_ayant_droit.id) AS total_ayant_droit_enfant,
    v_effectif_mains_ayant_droit.genre,
    v_effectif_mains_ayant_droit.lien
   FROM v_effectif_mains_ayant_droit
  WHERE v_effectif_mains_ayant_droit.lien::text = 'ENFANT'::text AND v_effectif_mains_ayant_droit.age <= 25
  GROUP BY v_effectif_mains_ayant_droit.genre, v_effectif_mains_ayant_droit.lien;



CREATE OR REPLACE VIEW public.v_total_adherents
 AS
 SELECT count(v_effectif_mains.id) AS total_aherent,
    v_effectif_mains.sexe
   FROM v_effectif_mains
  WHERE v_effectif_mains.age <= 60::double precision AND (v_effectif_mains.id IN ( SELECT reglement.adherent
           FROM reglement))
  GROUP BY v_effectif_mains.sexe;
