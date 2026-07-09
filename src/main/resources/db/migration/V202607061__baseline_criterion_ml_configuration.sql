ALTER TABLE criteres_evaluation
    ADD COLUMN IF NOT EXISTS criterion_family VARCHAR(100),
    ADD COLUMN IF NOT EXISTS configuration_version VARCHAR(40),
    ADD COLUMN IF NOT EXISTS applicability VARCHAR(120),
    ADD COLUMN IF NOT EXISTS required_evidence_types TEXT,
    ADD COLUMN IF NOT EXISTS optional_evidence_types TEXT,
    ADD COLUMN IF NOT EXISTS positive_evidence_expectations TEXT,
    ADD COLUMN IF NOT EXISTS negative_evidence_expectations TEXT,
    ADD COLUMN IF NOT EXISTS permitted_evidence_natures TEXT,
    ADD COLUMN IF NOT EXISTS minimum_supporting_evidence_count INTEGER,
    ADD COLUMN IF NOT EXISTS scoring_policy VARCHAR(120),
    ADD COLUMN IF NOT EXISTS eliminatory_decision_rule VARCHAR(120),
    ADD COLUMN IF NOT EXISTS source_authority_rules TEXT;

UPDATE criteres_evaluation
SET configuration_version = COALESCE(configuration_version, '2026-07.1'),
    applicability = COALESCE(applicability, 'APPLICABLE_TO_ALL_PROJECTS')
WHERE actif = TRUE;

UPDATE criteres_evaluation
SET configuration_version = '2026-07.1'
WHERE actif = TRUE AND (configuration_version IS NULL OR TRIM(configuration_version) = '');

UPDATE criteres_evaluation
SET applicability = 'APPLICABLE_TO_ALL_PROJECTS'
WHERE actif = TRUE AND (applicability IS NULL OR TRIM(applicability) = '');

UPDATE criteres_evaluation
SET criterion_family = COALESCE(criterion_family, 'DELIVERABLE_AVAILABILITY'),
    description = COALESCE(description, 'Verifier que les livrables essentiels autorises sont presents, accessibles et rattaches au projet.'),
    required_evidence_types = COALESCE(required_evidence_types, 'PDF,DOCX,TXT,MARKDOWN,SOURCE_ARCHIVE,GIT_REFERENCE,STRUCTURED_REPORT,IMAGE'),
    positive_evidence_expectations = COALESCE(positive_evidence_expectations, 'authorised deliverable record exists; content or authorised reference is retrievable; deliverable belongs to the evaluated project; deliverable status is valid'),
    negative_evidence_expectations = COALESCE(negative_evidence_expectations, 'confirmed mandatory deliverable absence after the submission deadline'),
    permitted_evidence_natures = COALESCE(permitted_evidence_natures, 'DETERMINISTIC'),
    minimum_supporting_evidence_count = COALESCE(minimum_supporting_evidence_count, 1),
    eliminatory_decision_rule = COALESCE(eliminatory_decision_rule, 'OK_WITH_ACCESSIBLE_REQUIRED_DELIVERABLE_ELSE_INDETERMINATE')
WHERE LOWER(libelle) LIKE '%livrable%essentiel%' OR LOWER(libelle) LIKE '%livrables essentiels%';

UPDATE criteres_evaluation
SET criterion_family = COALESCE(criterion_family, 'SOURCE_CODE_AVAILABILITY'),
    description = COALESCE(description, 'Verifier qu un depot Git valide, un manifest de depot ou une archive source validee rend le code source disponible.'),
    required_evidence_types = COALESCE(required_evidence_types, 'GIT_REPOSITORY,GIT_MANIFEST,SOURCE_ARCHIVE,VERIFIED_SOURCE_TREE'),
    positive_evidence_expectations = COALESCE(positive_evidence_expectations, 'validated repository, trusted manifest or validated source archive exists'),
    negative_evidence_expectations = COALESCE(negative_evidence_expectations, 'source code is mandatory and Spring verifies that no valid source exists'),
    permitted_evidence_natures = COALESCE(permitted_evidence_natures, 'DETERMINISTIC'),
    minimum_supporting_evidence_count = COALESCE(minimum_supporting_evidence_count, 1),
    eliminatory_decision_rule = COALESCE(eliminatory_decision_rule, 'OK_WITH_VALID_SOURCE_ELSE_INDETERMINATE')
WHERE LOWER(libelle) LIKE '%git%' OR LOWER(libelle) LIKE '%code source%';

UPDATE criteres_evaluation
SET criterion_family = COALESCE(criterion_family, 'EXECUTABILITY'),
    description = COALESCE(description, 'Verifier une preuve reelle de demarrage, health check, execution CI, test bout en bout ou demonstration reussie.'),
    required_evidence_types = COALESCE(required_evidence_types, 'VERIFIED_STARTUP_RESULT,HEALTH_CHECK_RESULT,END_TO_END_TEST_RESULT,CI_EXECUTION_RESULT,DEPLOYMENT_URL_CHECK,EXECUTABLE_ARTIFACT,VERIFIED_DOCKER_STARTUP,DEMONSTRATION_RECORD'),
    positive_evidence_expectations = COALESCE(positive_evidence_expectations, 'real execution or demonstration succeeded'),
    negative_evidence_expectations = COALESCE(negative_evidence_expectations, 'confirmed execution attempt failed because the delivered application is not operational'),
    permitted_evidence_natures = COALESCE(permitted_evidence_natures, 'DETERMINISTIC,STRUCTURED_REPORT'),
    minimum_supporting_evidence_count = COALESCE(minimum_supporting_evidence_count, 1),
    eliminatory_decision_rule = COALESCE(eliminatory_decision_rule, 'OK_WITH_VERIFIED_EXECUTION_ELSE_INDETERMINATE')
WHERE LOWER(libelle) LIKE '%executable%' OR LOWER(libelle) LIKE '%demontrable%' OR LOWER(libelle) LIKE '%démontrable%';

UPDATE criteres_evaluation
SET criterion_family = COALESCE(criterion_family, 'MINIMUM_PROJECT_DOCUMENTATION'),
    description = COALESCE(description, 'Verifier qu au moins une documentation projet lisible, specifique et autorisee est disponible.'),
    required_evidence_types = COALESCE(required_evidence_types, 'PROJECT_README,PROJECT_PDF,PROJECT_DOCX,PROJECT_MARKDOWN,USER_GUIDE,INSTALLATION_GUIDE,API_DOCUMENTATION'),
    positive_evidence_expectations = COALESCE(positive_evidence_expectations, 'readable project-specific documentation source exists'),
    negative_evidence_expectations = COALESCE(negative_evidence_expectations, 'documents are unreadable, unrelated, evaluator documentation, templates or generic references'),
    permitted_evidence_natures = COALESCE(permitted_evidence_natures, 'SEMANTIC,DETERMINISTIC'),
    minimum_supporting_evidence_count = COALESCE(minimum_supporting_evidence_count, 1),
    eliminatory_decision_rule = COALESCE(eliminatory_decision_rule, 'OK_WITH_PROJECT_SPECIFIC_DOCUMENTATION_ELSE_INDETERMINATE')
WHERE LOWER(libelle) LIKE '%documentation minimale%' OR LOWER(libelle) LIKE '%minimum documentation%';

UPDATE criteres_evaluation
SET criterion_family = COALESCE(criterion_family, 'SECURITY_BLOCKERS'),
    description = COALESCE(description, 'Verifier l absence de probleme critique ou blocker confirme par un rapport securite structure supporte.'),
    required_evidence_types = COALESCE(required_evidence_types, 'SONAR_REPORT,SAST_REPORT,DEPENDENCY_VULNERABILITY_REPORT,SECRET_SCAN_REPORT,SECURITY_TEST_REPORT'),
    positive_evidence_expectations = COALESCE(positive_evidence_expectations, 'valid supported security report contains no blocking issue according to the configured severity rule'),
    negative_evidence_expectations = COALESCE(negative_evidence_expectations, 'valid supported report confirms a blocker or critical issue under the business rule'),
    permitted_evidence_natures = COALESCE(permitted_evidence_natures, 'STRUCTURED_REPORT'),
    minimum_supporting_evidence_count = COALESCE(minimum_supporting_evidence_count, 1),
    eliminatory_decision_rule = COALESCE(eliminatory_decision_rule, 'OK_WITH_CLEAN_SECURITY_REPORT_NOT_OK_WITH_BLOCKER_ELSE_INDETERMINATE')
WHERE LOWER(libelle) LIKE '%secur%' OR LOWER(libelle) LIKE '%sécur%' OR LOWER(libelle) LIKE '%critique%';

UPDATE criteres_evaluation
SET criterion_family = COALESCE(criterion_family, 'BUSINESS_FIT'),
    description = COALESCE(description, 'Evaluer l adequation entre besoin metier, utilisateurs cibles, fonctionnalites livrees et resultat attendu.'),
    required_evidence_types = COALESCE(required_evidence_types, 'PROJECT_REQUIREMENTS,USE_CASES,IMPLEMENTED_FEATURES,USER_STORIES,ACCEPTANCE_EVIDENCE,FUNCTIONAL_DEMONSTRATION,PROJECT_DOCUMENTATION'),
    positive_evidence_expectations = COALESCE(positive_evidence_expectations, 'identified business need; explicit target users; implemented features linked to the need; measurable or demonstrable outcome'),
    negative_evidence_expectations = COALESCE(negative_evidence_expectations, 'generic technical descriptions without project-specific business linkage'),
    permitted_evidence_natures = COALESCE(permitted_evidence_natures, 'SEMANTIC,DETERMINISTIC'),
    minimum_supporting_evidence_count = COALESCE(minimum_supporting_evidence_count, 1),
    scoring_policy = COALESCE(scoring_policy, 'ML_ASSISTED_EVIDENCE_CONDITIONED')
WHERE LOWER(libelle) LIKE '%besoin%metier%' OR LOWER(libelle) LIKE '%besoin métier%' OR LOWER(libelle) LIKE '%adequation%';

UPDATE criteres_notes
SET poids = COALESCE(poids, 30)
WHERE id IN (SELECT id FROM criteres_evaluation WHERE LOWER(libelle) LIKE '%besoin%metier%' OR LOWER(libelle) LIKE '%besoin métier%' OR LOWER(libelle) LIKE '%adequation%');

UPDATE criteres_evaluation
SET criterion_family = COALESCE(criterion_family, 'TECHNICAL_QUALITY'),
    description = COALESCE(description, 'Evaluer architecture, structure source, tests, metriques qualite, dependances, erreurs, base de donnees et configuration.'),
    required_evidence_types = COALESCE(required_evidence_types, 'VALIDATED_SOURCE_STRUCTURE,ARCHITECTURE_DOCUMENTATION,TESTS,SONAR_METRICS,DEPENDENCY_STRUCTURE,ERROR_HANDLING,DATABASE_DESIGN,CONFIGURATION_QUALITY,TECHNICAL_DIAGRAMS'),
    positive_evidence_expectations = COALESCE(positive_evidence_expectations, 'coherent architecture; maintainable implementation; tests or quality evidence; appropriate error handling; justified technical choices'),
    negative_evidence_expectations = COALESCE(negative_evidence_expectations, 'generic architecture mention without project-specific technical evidence'),
    permitted_evidence_natures = COALESCE(permitted_evidence_natures, 'SEMANTIC,DETERMINISTIC,STRUCTURED_REPORT'),
    minimum_supporting_evidence_count = COALESCE(minimum_supporting_evidence_count, 1),
    scoring_policy = COALESCE(scoring_policy, 'ML_ASSISTED_EVIDENCE_CONDITIONED')
WHERE LOWER(libelle) LIKE '%qualite technique%' OR LOWER(libelle) LIKE '%qualité technique%';

UPDATE criteres_notes
SET poids = COALESCE(poids, 25)
WHERE id IN (SELECT id FROM criteres_evaluation WHERE LOWER(libelle) LIKE '%qualite technique%' OR LOWER(libelle) LIKE '%qualité technique%');

UPDATE criteres_evaluation
SET criterion_family = COALESCE(criterion_family, 'EXPLOITABILITY_REUSABILITY'),
    description = COALESCE(description, 'Evaluer installation reproductible, configuration, modularite, composants reutilisables et procedure de deploiement operationnelle.'),
    required_evidence_types = COALESCE(required_evidence_types, 'PROJECT_DEPLOYMENT_GUIDE,INSTALLATION_PROCEDURE,DOCKER_OR_COMPOSE,ENVIRONMENT_CONFIGURATION,API_DOCUMENTATION,MODULAR_ARCHITECTURE,REUSABLE_SERVICES,VERIFIED_DEPLOYMENT_EVIDENCE'),
    positive_evidence_expectations = COALESCE(positive_evidence_expectations, 'reproducible installation; documented configuration; modularity; reusable components; operational deployment procedure'),
    negative_evidence_expectations = COALESCE(negative_evidence_expectations, 'future recommendations or missing support statements'),
    permitted_evidence_natures = COALESCE(permitted_evidence_natures, 'SEMANTIC,DETERMINISTIC,STRUCTURED_REPORT'),
    minimum_supporting_evidence_count = COALESCE(minimum_supporting_evidence_count, 1),
    scoring_policy = COALESCE(scoring_policy, 'ML_ASSISTED_EVIDENCE_CONDITIONED')
WHERE LOWER(libelle) LIKE '%exploitabil%' OR LOWER(libelle) LIKE '%reutilis%' OR LOWER(libelle) LIKE '%réutilis%';

UPDATE criteres_notes
SET poids = COALESCE(poids, 20)
WHERE id IN (SELECT id FROM criteres_evaluation WHERE LOWER(libelle) LIKE '%exploitabil%' OR LOWER(libelle) LIKE '%reutilis%' OR LOWER(libelle) LIKE '%réutilis%');

UPDATE criteres_evaluation
SET criterion_family = COALESCE(criterion_family, 'DOCUMENTATION_QUALITY'),
    description = COALESCE(description, 'Evaluer la qualite de la documentation projet: vue d ensemble, prerequis, installation, configuration, usage, architecture et deploiement.'),
    required_evidence_types = COALESCE(required_evidence_types, 'PROJECT_README,PROJECT_PDF,PROJECT_DOCX,PROJECT_MARKDOWN,USER_GUIDE,INSTALLATION_GUIDE,CONFIGURATION_GUIDE,API_DOCUMENTATION,ARCHITECTURE_DOCUMENTATION'),
    positive_evidence_expectations = COALESCE(positive_evidence_expectations, 'project overview; prerequisites; installation; configuration; usage; architecture; deployment; troubleshooting where applicable'),
    negative_evidence_expectations = COALESCE(negative_evidence_expectations, 'evaluator documentation, templates or generic references'),
    permitted_evidence_natures = COALESCE(permitted_evidence_natures, 'SEMANTIC,DETERMINISTIC'),
    minimum_supporting_evidence_count = COALESCE(minimum_supporting_evidence_count, 1),
    scoring_policy = COALESCE(scoring_policy, 'ML_ASSISTED_EVIDENCE_CONDITIONED')
WHERE LOWER(libelle) LIKE '%qualite de la documentation%' OR LOWER(libelle) LIKE '%qualité de la documentation%';

UPDATE criteres_notes
SET poids = COALESCE(poids, 15)
WHERE id IN (SELECT id FROM criteres_evaluation WHERE LOWER(libelle) LIKE '%qualite de la documentation%' OR LOWER(libelle) LIKE '%qualité de la documentation%');

UPDATE criteres_evaluation
SET criterion_family = COALESCE(criterion_family, 'DECISION_VALUE_REPORTING'),
    description = COALESCE(description, 'Evaluer la valeur decisionnelle des resultats, indicateurs, rapports et interpretations actionnables propres au projet.'),
    required_evidence_types = COALESCE(required_evidence_types, 'PROJECT_REPORTS,DASHBOARDS,INDICATORS,DECISION_RULES,EXPORTED_RESULTS,BI_DOCUMENTATION,VALIDATION_EXAMPLES'),
    positive_evidence_expectations = COALESCE(positive_evidence_expectations, 'understandable outputs; relevant indicators; traceable results; actionable interpretation; support for business or industrialisation decisions'),
    negative_evidence_expectations = COALESCE(negative_evidence_expectations, 'documentation describing the EspritTECH evaluator itself rather than the evaluated project'),
    permitted_evidence_natures = COALESCE(permitted_evidence_natures, 'SEMANTIC,STRUCTURED_REPORT,DETERMINISTIC'),
    minimum_supporting_evidence_count = COALESCE(minimum_supporting_evidence_count, 1),
    scoring_policy = COALESCE(scoring_policy, 'ML_ASSISTED_EVIDENCE_CONDITIONED')
WHERE LOWER(libelle) LIKE '%valeur decisionnelle%' OR LOWER(libelle) LIKE '%valeur décisionnelle%';

UPDATE criteres_notes
SET poids = COALESCE(poids, 10)
WHERE id IN (SELECT id FROM criteres_evaluation WHERE LOWER(libelle) LIKE '%valeur decisionnelle%' OR LOWER(libelle) LIKE '%valeur décisionnelle%');
UPDATE criteres_evaluation
SET description = 'Critere configure pour l evaluation ML multimodale version 2026-07.1.'
WHERE actif = TRUE
  AND criterion_family IS NOT NULL
  AND (description IS NULL OR TRIM(description) = '');
