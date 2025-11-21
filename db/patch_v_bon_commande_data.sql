
DROP VIEW public.v_reglement_details;
DROP VIEW public.v_bon_de_commande;
CREATE OR REPLACE VIEW public.v_bon_de_commande
 AS
 SELECT reg.id,
    ad.nom_ad,
    ad.prenom_ad,
    ad.code_carnet,
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

 
CREATE OR REPLACE VIEW public.v_reglement_details
 AS
 SELECT det.id,
    det.reglement,
    det.intitule,
    det.montant,
    det.quantite,
    det.prix_unitaire,
    det.who_done,
    det.when_done,
    det.on_deleted,
    det.last_update,
    reg.ref_facture,
    reg.id AS id_bon,
    reg.nom_ad,
    reg.prenom_ad,
    reg.matricule,
    reg.structure,
    reg.couverture,
    reg.date_payement,
    reg.prestation,
    reg.lien,
	reg.is_confirmed_bon,
	reg.when_confirmed_bon,
	reg.date_expiration,
    COALESCE(det.montant, 0::bigint) * det.quantite * reg.couverture::bigint / 100 AS total_couvert,
    COALESCE(det.montant, 0::bigint) * det.quantite * (100 - reg.couverture::integer)::bigint / 100 AS total_restant
   FROM reglement_detail det
     JOIN v_bon_de_commande reg ON det.reglement = reg.id;
