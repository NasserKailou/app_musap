-- =====================================================================
-- Rôles & permissions + taux de cotisation paramétrables - 2026-10-10
-- Script idempotent. Les tables sont aussi créées (et initialisées avec les
-- droits et taux historiques) automatiquement au démarrage par
-- services.RoleServices et services.CotisationServices.
-- =====================================================================

-- Rôles applicatifs (le code correspond à users.droit)
CREATE TABLE IF NOT EXISTS app_role (
    code        VARCHAR(50) PRIMARY KEY,
    libelle     VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    systeme     BOOLEAN NOT NULL DEFAULT FALSE,   -- rôle non supprimable (Admin)
    actif       BOOLEAN NOT NULL DEFAULT TRUE,    -- rôle désactivé => connexion refusée
    who_done    VARCHAR(100),
    when_done   TIMESTAMP DEFAULT now()
);

-- Fonctionnalités accordées à chaque rôle (codes définis dans utils.Permissions)
CREATE TABLE IF NOT EXISTS app_role_permission (
    role_code  VARCHAR(50) NOT NULL REFERENCES app_role(code) ON DELETE CASCADE ON UPDATE CASCADE,
    permission VARCHAR(60) NOT NULL,
    PRIMARY KEY (role_code, permission)
);

-- Taux de cotisation (% du salaire) : part de base (ADHERENT) et part par lien d'ayant droit
CREATE TABLE IF NOT EXISTS taux_cotisation (
    code       VARCHAR(30) PRIMARY KEY,
    libelle    VARCHAR(100) NOT NULL,
    taux       NUMERIC(6,3) NOT NULL DEFAULT 0,
    nombre_max INTEGER,                    -- nombre max d'ayants droit de ce lien pris en compte (NULL = illimité)
    ordre      INTEGER NOT NULL DEFAULT 0,
    who_done   VARCHAR(100),
    when_done  TIMESTAMP DEFAULT now()
);

-- Valeurs historiques (codées en dur auparavant dans AdherentCtrl / AyantDroitCtrl)
INSERT INTO taux_cotisation (code, libelle, taux, ordre, who_done) VALUES
    ('ADHERENT', 'Adhérent (part de base)', 2.5, 0, 'initialisation'),
    ('CONJOINT', 'Conjoint(e)',             1.5, 1, 'initialisation'),
    ('ENFANT',   'Enfant',                  1.0, 2, 'initialisation'),
    ('PARENT',   'Parent',                  3.0, 3, 'initialisation')
ON CONFLICT (code) DO NOTHING;

-- Les rôles existants (Admin, saisie, Validateur, Percepteur, Adherent) et leurs
-- droits historiques sont créés automatiquement au premier démarrage.
