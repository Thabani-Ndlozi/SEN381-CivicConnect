# Team comparison checklist

Use this when comparing Anele's, Masia's and Thaban's code proposals. Select one controlled team direction rather than combining every idea.

- Does the proposal follow the selected React + Spring Boot + PostgreSQL stack?
- Does backend structure match the modular-monolith architecture or provide stronger evidence for another approved architecture?
- Are correctness-critical request and audit changes atomic?
- Is the status-transition catalogue treated as provisional until A-005 is validated?
- Is optimistic concurrency or another defensible lost-update control present?
- Is the Observer/event decision used only for secondary reactions, not mandatory audit correctness?
- Is the browser/backend network boundary explicit while internal modules avoid unnecessary HTTP?
- Are authorisation and secrets controls visible without pretending the final production identity mechanism is decided?
- Is the schema versioned through migrations rather than uncontrolled auto-generation?
- Is there at least one real requester -> backend -> database functional path?
- Are tests/checks meaningful and repeatable?
- Are README/setup/current limitations accurate?
- Can each implementation artefact be traced back to requirements/ASRs/ADRs?
- Can every team member explain and modify the chosen code?
