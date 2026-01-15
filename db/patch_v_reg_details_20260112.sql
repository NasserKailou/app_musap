DROP VIEW public.v_reglement_details;

CREATE OR REPLACE VIEW public.v_reglement_details
 AS
 SELECT det.id,
    det.reglement,
    det.intitule,
    det.montant,
    det.quantite,
    det.prix_unitaire,
    reg.code_carnet,
    det.who_done,
    det.when_done,
    det.on_deleted,
    det.last_update,
    reg.ref_facture,
    reg.type_reglement,
    reg.id AS id_bon,
    reg.nom_ad,
    reg.prenom_ad,
    reg.matricule,
    reg.structure,
    reg.taux_effectif,
    reg.date_payement,
    reg.prestation,
    reg.is_confirmed_bon,
    reg.when_confirmed_bon,
    reg.date_expiration,
	reg.ayant_droit,
	reg.nom_ay,
	reg.lien,
    COALESCE(det.montant, 0::bigint) * det.quantite * reg.taux_effectif / 100 AS total_couvert,
    COALESCE(det.montant, 0::bigint) * det.quantite * (100 - reg.taux_effectif::integer)::bigint / 100 AS total_restant
   FROM reglement_detail det
     JOIN v_bon_de_commande reg ON det.reglement = reg.id;