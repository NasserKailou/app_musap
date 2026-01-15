
CREATE OR REPLACE VIEW public.v_pharmacie
 AS
 SELECT sp.id,
    sp.libelle,
    sp.who_done,
    sp.when_done,
    sp.is_deleted,
    sp.telephone,
    sp.statut_structure,
    sp.type_structure,
    sp.suspension,
    r.libelle AS region
   FROM structure_partenaire sp,
    region r
  WHERE sp.region = r.id and sp.type_structure in ('PHARMACIE') AND sp.suspension is false;


  
CREATE OR REPLACE VIEW public.v_hopitaux
 AS
 SELECT sp.id,
    sp.libelle,
    sp.who_done,
    sp.when_done,
    sp.is_deleted,
    sp.telephone,
    sp.statut_structure,
    sp.type_structure,
    sp.suspension,
    r.libelle AS region
   FROM structure_partenaire sp,
    region r
  WHERE sp.region = r.id and sp.type_structure in ('HOPITAL','CLINIQUE','LABORATOIRE') AND sp.suspension is false;