copy public.adherent (
    nom_ad,
    prenom_ad,
    fonction,
    date_naiss,
    date_prise_service,
    picture,
    when_done,
    who_done,
    on_deleted,
    direction,
    division,
    service,
    telephone,
    matricule,
    structure,
    structure_sigle,
    code_programme,
    categorie,
    sexe,
    mat,
    email,
    is_member,
    is_ok_for_printing,
    date_retraite,
    code_carnet
)
FROM 'C:/adherent.csv'
DELIMITER ';'
CSV HEADER
ENCODING 'UTF8';


UPDATE public.adherent
SET
	picture = 'C:\Users\kailo\eclipse-workspace\mutuel-poste/public/images/adherents//355.png',
    is_member = TRUE,                -- ou FALSE
    is_ok_for_printing = false,       -- ou FALSE
    when_done = NOW(),               -- date/heure actuelle
    who_done = 'admin',              -- celui qui fait la modification
    on_deleted = FALSE               -- ou TRUE selon logique
WHERE id = 123;   

SELECT * FROM categorie;
UPDATE public.adherent
SET
   
    categorie = 'A2'