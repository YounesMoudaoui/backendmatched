-- Création de la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS web4jobs;
USE web4jobs;

-- Table des centres
CREATE TABLE IF NOT EXISTS centres (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    adresse VARCHAR(255),
    ville VARCHAR(100),
    pays VARCHAR(100),
    telephone VARCHAR(20),
    email VARCHAR(255)
);

-- Table des entreprises
CREATE TABLE IF NOT EXISTS entreprises (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    adresse VARCHAR(255),
    logo_url VARCHAR(255)
);

-- Table des utilisateurs
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL,
    is_validated BOOLEAN NOT NULL DEFAULT FALSE,
    is_intermediate_recruiter BOOLEAN NOT NULL DEFAULT FALSE,
    centre_id BIGINT,
    FOREIGN KEY (centre_id) REFERENCES centres(id)
);

-- Table d'association entre utilisateurs et entreprises
CREATE TABLE IF NOT EXISTS user_entreprises (
    user_id BIGINT NOT NULL,
    entreprise_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, entreprise_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (entreprise_id) REFERENCES entreprises(id)
);

-- Table des offres d'emploi
CREATE TABLE IF NOT EXISTS job_offers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titre_poste VARCHAR(255) NOT NULL,
    entreprise_id BIGINT NOT NULL,
    localisation VARCHAR(255) NOT NULL,
    description_detaillee TEXT NOT NULL,
    education VARCHAR(255),
    type_contrat VARCHAR(50) NOT NULL,
    duree_contrat VARCHAR(100),
    type_modalite VARCHAR(50),
    experience_souhaitee VARCHAR(255),
    langue VARCHAR(100),
    remuneration VARCHAR(100),
    recruiter_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (entreprise_id) REFERENCES entreprises(id),
    FOREIGN KEY (recruiter_id) REFERENCES users(id)
);

-- Tables pour les compétences techniques requises
CREATE TABLE IF NOT EXISTS job_offer_competences_techniques (
    job_offer_id BIGINT NOT NULL,
    competence_technique VARCHAR(255) NOT NULL,
    FOREIGN KEY (job_offer_id) REFERENCES job_offers(id)
);

-- Tables pour les compétences comportementales requises
CREATE TABLE IF NOT EXISTS job_offer_competences_comportementales (
    job_offer_id BIGINT NOT NULL,
    competence_comportementale VARCHAR(255) NOT NULL,
    FOREIGN KEY (job_offer_id) REFERENCES job_offers(id)
);

-- Tables pour les certifications demandées
CREATE TABLE IF NOT EXISTS job_offer_certifications (
    job_offer_id BIGINT NOT NULL,
    certification VARCHAR(255) NOT NULL,
    FOREIGN KEY (job_offer_id) REFERENCES job_offers(id)
);

-- Insertion de données de test si la table est vide
INSERT INTO centres (nom, ville, pays)
SELECT 'Centre de Formation Casablanca', 'Casablanca', 'Maroc'
WHERE NOT EXISTS (SELECT 1 FROM centres LIMIT 1);

INSERT INTO entreprises (nom, adresse)
SELECT 'TechCorp', 'Avenue Hassan II, Casablanca' 
WHERE NOT EXISTS (SELECT 1 FROM entreprises LIMIT 1);

INSERT INTO entreprises (nom, adresse)
SELECT 'SoftSolutions', 'Rue Mohammed V, Rabat'
WHERE NOT EXISTS (SELECT 1 FROM entreprises WHERE nom = 'SoftSolutions');

INSERT INTO entreprises (nom, adresse)
SELECT 'DataInsight', 'Boulevard Zerktouni, Marrakech'
WHERE NOT EXISTS (SELECT 1 FROM entreprises WHERE nom = 'DataInsight');

-- Mot de passe: password123 (encodé avec BCrypt)
INSERT INTO users (first_name, last_name, email, password, role, is_validated)
SELECT 'Admin', 'System', 'admin@web4jobs.com', '$2a$10$3Qw1E1Hhx.kQfmZtVNTVeegr1CF2nQqnNgMuN9JbLh8M/lKY0jlAi', 'ADMIN', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@web4jobs.com');

INSERT INTO users (first_name, last_name, email, password, role, is_validated)
SELECT 'Recruteur', 'Test', 'recruteur@web4jobs.com', '$2a$10$3Qw1E1Hhx.kQfmZtVNTVeegr1CF2nQqnNgMuN9JbLh8M/lKY0jlAi', 'RECRUTEUR', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'recruteur@web4jobs.com');

INSERT INTO users (first_name, last_name, email, password, role, is_validated)
SELECT 'Candidat', 'Test', 'candidat@web4jobs.com', '$2a$10$3Qw1E1Hhx.kQfmZtVNTVeegr1CF2nQqnNgMuN9JbLh8M/lKY0jlAi', 'APPRENANT', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'candidat@web4jobs.com');

-- Association du recruteur à l'entreprise TechCorp
INSERT INTO user_entreprises (user_id, entreprise_id)
SELECT 
    (SELECT id FROM users WHERE email = 'recruteur@web4jobs.com'), 
    (SELECT id FROM entreprises WHERE nom = 'TechCorp')
WHERE NOT EXISTS (
    SELECT 1 FROM user_entreprises 
    WHERE user_id = (SELECT id FROM users WHERE email = 'recruteur@web4jobs.com')
    AND entreprise_id = (SELECT id FROM entreprises WHERE nom = 'TechCorp')
);

-- Insertion d'offres d'emploi test
INSERT INTO job_offers (titre_poste, entreprise_id, localisation, description_detaillee, education, type_contrat, duree_contrat, experience_souhaitee, remuneration, recruiter_id, is_active)
SELECT 
    'Développeur Frontend React', 
    (SELECT id FROM entreprises WHERE nom = 'TechCorp'), 
    'Casablanca', 
    'Nous recherchons un développeur frontend React expérimenté pour rejoindre notre équipe dynamique. Vous serez responsable de la conception et du développement d''interfaces utilisateur modernes et réactives pour nos applications web.',
    'Bac+5 en informatique',
    'CDI',
    NULL,
    '2+ ans',
    '10000-15000 DH',
    (SELECT id FROM users WHERE email = 'recruteur@web4jobs.com'),
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM job_offers WHERE titre_poste = 'Développeur Frontend React' AND entreprise_id = (SELECT id FROM entreprises WHERE nom = 'TechCorp'));

INSERT INTO job_offers (titre_poste, entreprise_id, localisation, description_detaillee, education, type_contrat, duree_contrat, experience_souhaitee, remuneration, recruiter_id, is_active)
SELECT 
    'Développeur Backend Java', 
    (SELECT id FROM entreprises WHERE nom = 'SoftSolutions'), 
    'Rabat', 
    'Nous cherchons un développeur backend Java pour travailler sur nos applications d''entreprise. Vous participerez au développement de services RESTful, à l''optimisation des performances et à l''intégration avec des systèmes tiers.',
    'Bac+3 minimum en informatique',
    'CDD',
    '12 mois',
    '1+ an',
    '8000-12000 DH',
    (SELECT id FROM users WHERE email = 'recruteur@web4jobs.com'),
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM job_offers WHERE titre_poste = 'Développeur Backend Java' AND entreprise_id = (SELECT id FROM entreprises WHERE nom = 'SoftSolutions'));

INSERT INTO job_offers (titre_poste, entreprise_id, localisation, description_detaillee, education, type_contrat, duree_contrat, experience_souhaitee, remuneration, recruiter_id, is_active)
SELECT 
    'Stage - Data Analyst', 
    (SELECT id FROM entreprises WHERE nom = 'DataInsight'), 
    'Marrakech', 
    'Stage de fin d''études pour un profil Data Analyst. Vous travaillerez sur l''analyse de données, la création de visualisations et la génération de rapports pour aider à la prise de décision.',
    'En cours de formation Bac+4/5',
    'STAGE',
    '6 mois',
    'Débutant accepté',
    '3000 DH',
    (SELECT id FROM users WHERE email = 'recruteur@web4jobs.com'),
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM job_offers WHERE titre_poste = 'Stage - Data Analyst' AND entreprise_id = (SELECT id FROM entreprises WHERE nom = 'DataInsight'));

-- Insertion des compétences techniques pour l'offre "Développeur Frontend React"
INSERT INTO job_offer_competences_techniques (job_offer_id, competence_technique)
SELECT 
    (SELECT id FROM job_offers WHERE titre_poste = 'Développeur Frontend React' AND entreprise_id = (SELECT id FROM entreprises WHERE nom = 'TechCorp')),
    'React'
WHERE NOT EXISTS (
    SELECT 1 FROM job_offer_competences_techniques 
    WHERE job_offer_id = (SELECT id FROM job_offers WHERE titre_poste = 'Développeur Frontend React' AND entreprise_id = (SELECT id FROM entreprises WHERE nom = 'TechCorp'))
    AND competence_technique = 'React'
);

INSERT INTO job_offer_competences_techniques (job_offer_id, competence_technique)
SELECT 
    (SELECT id FROM job_offers WHERE titre_poste = 'Développeur Frontend React' AND entreprise_id = (SELECT id FROM entreprises WHERE nom = 'TechCorp')),
    'JavaScript'
WHERE NOT EXISTS (
    SELECT 1 FROM job_offer_competences_techniques 
    WHERE job_offer_id = (SELECT id FROM job_offers WHERE titre_poste = 'Développeur Frontend React' AND entreprise_id = (SELECT id FROM entreprises WHERE nom = 'TechCorp'))
    AND competence_technique = 'JavaScript'
);

INSERT INTO job_offer_competences_techniques (job_offer_id, competence_technique)
SELECT 
    (SELECT id FROM job_offers WHERE titre_poste = 'Développeur Frontend React' AND entreprise_id = (SELECT id FROM entreprises WHERE nom = 'TechCorp')),
    'HTML/CSS'
WHERE NOT EXISTS (
    SELECT 1 FROM job_offer_competences_techniques 
    WHERE job_offer_id = (SELECT id FROM job_offers WHERE titre_poste = 'Développeur Frontend React' AND entreprise_id = (SELECT id FROM entreprises WHERE nom = 'TechCorp'))
    AND competence_technique = 'HTML/CSS'
); 