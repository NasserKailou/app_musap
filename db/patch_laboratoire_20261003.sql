-- =====================================================================
-- Module LABORATOIRE (profil Percepteur) - 2026-10-03
-- Ce script est idempotent. Il est aussi exécuté automatiquement au
-- démarrage par services.LaboratoireServices.ensureSchema().
-- =====================================================================

-- Types d'examens paramétrables avec leurs montants
CREATE TABLE IF NOT EXISTS type_examen (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(30),
    libelle          VARCHAR(255) NOT NULL,
    categorie        VARCHAR(100),
    montant          BIGINT NOT NULL DEFAULT 0,      -- tarif normal (personnes externes)
    montant_adherent BIGINT,                         -- tarif adhérent / ayant droit (NULL = tarif normal)
    on_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    who_done         VARCHAR(100),
    when_done        TIMESTAMP DEFAULT now()
);

-- En-tête des reçus de laboratoire
CREATE TABLE IF NOT EXISTS recu_labo (
    id                BIGSERIAL PRIMARY KEY,
    numero_recu       VARCHAR(30) UNIQUE,
    type_patient      VARCHAR(20) NOT NULL,           -- ADHERENT | AYANT_DROIT | EXTERNE
    adherent          BIGINT,                         -- adherent.id (adhérent ou adhérent principal de l'ayant droit)
    ayant_droit       BIGINT,                         -- ayant_droit.id
    nom_patient       VARCHAR(255) NOT NULL,
    telephone         VARCHAR(50),
    structure_origine VARCHAR(255),                   -- structure d'origine des personnes externes
    montant_total     BIGINT NOT NULL DEFAULT 0,
    mode_paiement     VARCHAR(30),
    date_recu         TIMESTAMP NOT NULL DEFAULT now(),
    is_annule         BOOLEAN NOT NULL DEFAULT FALSE,
    motif_annulation  VARCHAR(500),
    who_annule        VARCHAR(100),
    when_annule       TIMESTAMP,
    who_done          VARCHAR(100),
    when_done         TIMESTAMP DEFAULT now()
);

-- Lignes des reçus (un reçu = un ou plusieurs examens)
CREATE TABLE IF NOT EXISTS recu_labo_detail (
    id             BIGSERIAL PRIMARY KEY,
    recu_labo      BIGINT NOT NULL REFERENCES recu_labo(id),
    type_examen    BIGINT REFERENCES type_examen(id),
    libelle_examen VARCHAR(255) NOT NULL,             -- copie du libellé au moment du reçu
    prix_unitaire  BIGINT NOT NULL DEFAULT 0,         -- copie du tarif au moment du reçu
    quantite       INTEGER NOT NULL DEFAULT 1,
    montant        BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_recu_labo_date ON recu_labo(date_recu);
CREATE INDEX IF NOT EXISTS idx_recu_labo_detail_recu ON recu_labo_detail(recu_labo);

-- Index pour accélérer le tableau de bord
CREATE INDEX IF NOT EXISTS idx_reglement_date_payement ON reglement(date_payement);
CREATE INDEX IF NOT EXISTS idx_reglement_detail_reglement ON reglement_detail(reglement);

-- Création d'un utilisateur Percepteur : écran Paramètrages > Utilisateurs,
-- choisir le rôle "Percepteur".
