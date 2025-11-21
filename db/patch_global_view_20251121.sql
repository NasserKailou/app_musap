DROP VIEW public.v_situation_structure_prestation_adherent;


DROP VIEW public.v_situation_structure_adherent;

DROP VIEW public.v_reglement_global_par_type;


DROP VIEW public.v_reglement_global_by_adherent;


DROP VIEW public.v_nbr_prestation_par_adherent;


 DROP VIEW public.v_nbr_benefiaire_par_prestation;


DROP VIEW public.v_depassement;


DROP VIEW public.v_reglement_globale;


DROP VIEW public.v_cumule_by_type_prestation;

DROP VIEW public.v_cumule_by_structure_type_prestation;


DROP VIEW public.v_cumule_by_structure;

DROP VIEW public.v_reglement;

CREATE OR REPLACE VIEW public.v_reglement
 AS
 SELECT reg.id,
    ad.nom_ad,
    ad.prenom_ad,
    ad.id AS id_adherent,
    ad.matricule,
    reg.ref_facture,
    struc.libelle AS structure,
    reg.date_payement,
    reg.who_done,
    reg.when_done,
	reg.is_confirmed_bon,
	reg.when_confirmed_bon,
	reg.date_expiration,
    "substring"(reg.date_payement::character varying::text, 1, 4)::character varying AS annee,
    reg.on_deleted,
    ty.prestation,
    ty.couverture,
    ay.nom_ay,
    ay.lien,
    ay.genre,
    reg.ayant_droit,
    COALESCE(( SELECT sum(COALESCE(reglement_detail.montant, 0::bigint)) AS sum
           FROM reglement_detail
          WHERE reglement_detail.reglement = reg.id AND reglement_detail.on_deleted IS FALSE), 0::bigint::numeric)::bigint AS montant_total,
    COALESCE(( SELECT sum(COALESCE(reglement_detail.montant, 0::bigint)) * ty.couverture::bigint::numeric / 100::numeric AS sum
           FROM reglement_detail
          WHERE reglement_detail.reglement = reg.id AND reglement_detail.on_deleted IS FALSE), 0::bigint::numeric)::bigint AS montant_reglement,
    COALESCE(( SELECT sum(COALESCE(reglement_detail.montant, 0::bigint)) * (100::bigint - ty.couverture::bigint)::numeric / 100::numeric AS sum
           FROM reglement_detail
          WHERE reglement_detail.reglement = reg.id AND reglement_detail.on_deleted IS FALSE), 0::bigint::numeric)::bigint AS montant_paye,
    struc.id AS id_structure
   FROM structure_partenaire struc,
    adherent ad,
    type_prestation ty,
    reglement reg
     LEFT JOIN ayant_droit ay ON reg.ayant_droit = ay.id
  WHERE reg.structure = struc.id AND reg.adherent = ad.id AND reg.type_prestation = ty.id AND reg.on_deleted IS FALSE
  ORDER BY reg.id;
  
CREATE OR REPLACE VIEW public.v_cumule_by_structure
 AS
 SELECT reg.structure,
    sum(reg.montant_reglement) AS total_annuel,
    "substring"(reg.when_done::character varying::text, 1, 4)::character varying AS annee
   FROM v_reglement reg
  WHERE reg.on_deleted IS FALSE
  GROUP BY ("substring"(reg.when_done::character varying::text, 1, 4)), reg.structure
  ORDER BY reg.structure;

ALTER TABLE public.v_cumule_by_structure
    OWNER TO postgres;

CREATE OR REPLACE VIEW public.v_cumule_by_structure_type_prestation
 AS
 SELECT reg.structure,
    sum(reg.montant_reglement) AS total_annuel,
    reg.prestation,
    "substring"(reg.when_done::character varying::text, 1, 4)::character varying AS annee
   FROM v_reglement reg
  WHERE reg.on_deleted IS FALSE
  GROUP BY ("substring"(reg.when_done::character varying::text, 1, 4)), reg.structure, reg.prestation
  ORDER BY reg.structure;

CREATE OR REPLACE VIEW public.v_cumule_by_type_prestation
 AS
 SELECT reg.prestation,
    sum(reg.montant_reglement) AS total_annuel,
    "substring"(reg.when_done::character varying::text, 1, 4)::character varying AS annee
   FROM v_reglement reg
  WHERE reg.on_deleted IS FALSE
  GROUP BY ("substring"(reg.when_done::character varying::text, 1, 4)), reg.prestation
  ORDER BY ("substring"(reg.when_done::character varying::text, 1, 4)), reg.prestation;


CREATE OR REPLACE VIEW public.v_reglement_globale
 AS
 SELECT ad.matricule,
    ad.nom_ad,
    ad.prenom_ad,
    sum(reg.montant_reglement)::bigint AS total_annuel,
    reg.annee
   FROM v_reglement reg,
    adherent ad
  WHERE reg.id_adherent = ad.id AND reg.on_deleted IS FALSE
  GROUP BY reg.annee, ad.matricule, ad.nom_ad, ad.prenom_ad
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
CREATE OR REPLACE VIEW public.v_nbr_benefiaire_par_prestation
 AS
 SELECT reg.prestation,
    count(DISTINCT reg.matricule) AS nbr_adherent_ayant_beneficier,
    count(reg.matricule) AS nbr_prestation_effectuer,
    "substring"(reg.when_done::character varying::text, 1, 4)::character varying AS annee
   FROM v_reglement reg
  WHERE reg.on_deleted IS FALSE
  GROUP BY ("substring"(reg.when_done::character varying::text, 1, 4)), reg.prestation
  ORDER BY ("substring"(reg.when_done::character varying::text, 1, 4)), reg.prestation;

CREATE OR REPLACE VIEW public.v_nbr_prestation_par_adherent
 AS
 SELECT reg.matricule,
    reg.nom_ad,
    reg.prenom_ad,
    reg.prestation,
    count(reg.id) AS nombre_prestation,
    "substring"(reg.when_done::character varying::text, 1, 4)::character varying AS annee
   FROM v_reglement reg
  WHERE reg.on_deleted IS FALSE
  GROUP BY ("substring"(reg.when_done::character varying::text, 1, 4)), reg.prestation, reg.matricule, reg.nom_ad, reg.prenom_ad
  ORDER BY ("substring"(reg.when_done::character varying::text, 1, 4)), reg.prestation, reg.matricule, reg.nom_ad, reg.prenom_ad;

CREATE OR REPLACE VIEW public.v_reglement_global_by_adherent
 AS
 SELECT ad.matricule,
    ad.nom_ad,
    ad.prenom_ad,
    sum(reg.montant_reglement)::bigint AS total_annuel,
    reg.annee,
    ad.email
   FROM v_reglement reg,
    adherent ad
  WHERE reg.id_adherent = ad.id AND reg.on_deleted IS FALSE
  GROUP BY reg.annee, ad.matricule, ad.nom_ad, ad.prenom_ad, ad.email
  ORDER BY ad.matricule;

CREATE OR REPLACE VIEW public.v_reglement_global_par_type
 AS
 SELECT ad.matricule,
    ad.nom_ad,
    ad.prenom_ad,
    reg.prestation,
    sum(reg.montant_reglement) AS total_annuel,
    "substring"(reg.when_done::character varying::text, 1, 4)::character varying AS annee
   FROM v_reglement reg,
    adherent ad
  WHERE reg.id_adherent = ad.id AND reg.on_deleted IS FALSE
  GROUP BY ("substring"(reg.when_done::character varying::text, 1, 4)), ad.matricule, ad.nom_ad, ad.prenom_ad, reg.prestation
  ORDER BY ad.matricule;

 
CREATE OR REPLACE VIEW public.v_situation_structure_adherent
 AS
 SELECT ad.matricule,
    ad.nom_ad,
    ad.prenom_ad,
    reg.structure,
    sum(reg.montant_reglement) AS total_annuel,
    "substring"(reg.when_done::character varying::text, 1, 4)::character varying AS annee
   FROM v_reglement reg,
    adherent ad
  WHERE reg.id_adherent = ad.id AND reg.on_deleted IS FALSE
  GROUP BY ("substring"(reg.when_done::character varying::text, 1, 4)), ad.matricule, ad.nom_ad, ad.prenom_ad, reg.structure
  ORDER BY ad.matricule;

  CREATE OR REPLACE VIEW public.v_situation_structure_prestation_adherent
 AS
 SELECT ad.matricule,
    ad.nom_ad,
    ad.prenom_ad,
    reg.structure,
    reg.prestation,
    sum(reg.montant_reglement) AS total_annuel,
    "substring"(reg.when_done::character varying::text, 1, 4)::character varying AS annee
   FROM v_reglement reg,
    adherent ad
  WHERE reg.id_adherent = ad.id AND reg.on_deleted IS FALSE
  GROUP BY ("substring"(reg.when_done::character varying::text, 1, 4)), ad.matricule, ad.nom_ad, ad.prenom_ad, reg.structure, reg.prestation
  ORDER BY ad.matricule;
