# GitHub work — do this only after reviewing this complete M3 transfer package

**No GitHub actions were performed while producing this ZIP.** The M3 brief requires an authentic progressive repository history, protected main, two meaningful non-author approvals for each substantive PR, CI evidence, a tagged release candidate and exact submitted version. This document is a future **checklist**, not evidence that those activities already occurred.

## 1. Prepare the existing team repository (do not start a replacement history)

1. Sign in to GitHub in the browser and open your **existing CivicConnect team repository**. Confirm your three members can push feature branches and review each other's PRs. The approved original PED names: Thaban Ndlozi, Anele Nkayi and Lebogang Masia. Each member should use their own account; never share passwords, tokens or approvals.
2. Clone **the existing repository**, not a newly initialised ZIP directory:

   ```bash
   git clone https://github.com/YOUR-ORG/YOUR-EXISTING-CIVICCONNECT-REPO.git
   cd YOUR-EXISTING-CIVICCONNECT-REPO
   git switch main
   git pull --ff-only origin main
   git log --oneline --decorate -12
   ```

3. Keep the existing `.git`, original code, prior PED and original live RTM and registers intact. Compare/merge the M3 ZIP contents into your working tree. **Do not overwrite** an original editable RTM with `RTM_CONTINUED.csv`: reconcile its original rows/history with the PDF-derived continuation; add the M3 columns and cite new test/defect evidence. If a team original PED edit source exists, evolve it rather than replace it; our PDF preserves the original v2 pages and appended M3 evidence.
4. Before adding anything, check `.gitignore`: never commit `.env`, `.env.staging`, `target/`, `node_modules/`, secrets or build output. Run `git status --short` and `python3 scripts/check_source.py`. Review each file by a human, including AI-generated code, acceptance criteria, dependency versions and the compatibility of the target stack.

## 2. Establish real GitHub configuration control

In **Settings → Rules → Rulesets** (or **Settings → Branches → Add branch protection rule** on a classic repo), target `main`. Enable: require a pull request before merging; require **2 approvals** from members **other than the author**; dismiss stale approvals after code changes if supported; require approval of the most recent push if available; require resolution of conversations; require CI checks to pass; prohibit force-push and deletion. Turn on enforce rules for administrators when appropriate and permitted by your course/repository plan. The required check names will appear **after** the workflow has executed at least once; select the matching backend, frontend, security-static and staging-smoke jobs as mandatory. In **Settings → Actions** allow required marketplace actions and set least-privilege repository permissions. Don't enable workflows with secrets you haven't audited.

Assign one team member a specific issue/task per component if possible (request lifecycle/ownership, requester history/reporting, verification and staging). Create real issue descriptions, acceptance criteria, reviewers and risk links. Link issue IDs to the relevant PR in its description.

## 3. Integrate this prepared M3 snapshot through genuine reviewed PRs

**One reasonable route** if your existing repository contains the M2 code: keep all M3 work together in **one substantive feature PR** if your team is under a deadline, but preserve your already-authentic M1/M2 commit history and demand thorough review by both other members. More focused PRs are preferable for traceability when time permits, but **do not create fake historic commits, forged approvals, or artificially dated reviews** to meet the history requirement.

```bash
# From existing repo after pulling main
git switch -c feature/m3-integrated-release
# Copy/merge the new M3 source files from this ZIP, keeping the original .git and RTM history.
git status --short
git diff --check
git add backend frontend scripts docs evidence .github compose.staging.yaml .env.staging.example GITHUB_AFTER_READY.md README.md PED_v3.0_CivicConnect.pdf
# Check the exact staged files before committing
git diff --cached --stat
git diff --cached --check
git commit -m "feat(m3): integrate CivicConnect candidate, verification and staging"
git push -u origin feature/m3-integrated-release
```

Open GitHub → **Pull requests → New pull request** targeting `main`. Explain what changed from M2, enumerate FR/NFR trace IDs and ADRs, identify tests you actually ran, and include evidence links. Request the **other two team members** as reviewers. Each should run the app or inspect relevant code/tests, comment substantively (e.g. identity privacy, transaction atomicity, input BVA, upgrade version), then approve only after concerns are resolved. If requested changes result, create real follow-up commits and rerun checks. **Never self-approve or bypass reviews.** Merge only after checks pass.

If separate PRs are practical, natural code-review groupings are: requester/staff ownership and audit; UI/reporting; tests/security/CI; staging/PED/RTM/quality records. They are review boundaries, not a requirement to reimplement code piece by piece.

## 4. Run GitHub CI and actual staging before claiming evidence

The `.github/workflows/ci.yml` workflow triggers on PRs and pushes to `main`. Watch **Actions → CivicConnect M3 quality gates** and inspect every job. Its hard gates include:

- Java 25 Maven `verify` and uploaded Surefire XML.
- Node 24 `npm ci`, Vitest, TypeScript/Vite build and production dependency audit.
- Source/static pattern check and verified secret scan.
- Disposable Compose staging with generated secrets; live API/system E2E suite and 40-request load script; uploaded JSON evidence.

Do not write “passed” in PED/RTM/defect register until **that exact run** passed. If any job fails, fix the actual error on the PR branch, push, rerun and keep the failure history. A failing check is valuable defect evidence. If a marketplace scan cannot run due to plan/API restrictions, document the constraint and implement a comparable feasible scan **with review**, rather than silently disabling quality/security gates.

For local staging from the completed ZIP:

```bash
python3 scripts/setup_staging.py
docker compose --env-file .env.staging -f compose.staging.yaml up -d --build
curl -f http://localhost:8082/actuator/health
curl -f http://localhost:8082/api/release
python3 scripts/smoke.py
python3 scripts/performance.py
```

Show the actual browser at `http://localhost:8081`; credentials are in local `.env.staging` and must **not** appear on screen during the demo or in CI logs. Save real CI run links and smoke/load outputs. Record p50/p95, errors, throughput and environment; explain limitations. Ask stakeholders to approve A-005 (provisional status/category/overdue rules), and conduct the human UI acceptance walk-through before removing the acceptance blocker.

## 5. Identify and tag the precise assessed release candidate

After merging reviews, make sure the **committed** PED/RTM correspond to current source and CI. Choose a final label such as `m3-rc1` before your final commits; remove the placeholder in the version block and staging config. Ensure it displays when running that version. Then:

```bash
git switch main
git pull --ff-only origin main
git rev-parse HEAD
git tag -a m3-rc1 -m "CivicConnect M3 reviewed release candidate"
git push origin m3-rc1
# This resolves an annotated tag to its assessed source commit:
git rev-parse 'm3-rc1^{commit}'
```

A tag by itself is not proof of passing tests. Check that the successful `main` workflow run references the **same commit**. Set **`RC_VERSION=m3-rc1`** in local untracked `.env.staging` and restart `api`/staging after builds. Check `curl http://localhost:8082/api/release` reports `m3-rc1`. Show `git status --short` is clean and record the full commit hash in submission information. If code changes after that, do not quietly reuse an older “tested” label; retest and publish a new tagged candidate (e.g. `m3-rc2`).

## 6. Finalise PED v3.0, RTM and actual submission information

This ZIP includes **`PED_v3.0_CivicConnect.pdf`** as the original PED v2.0 pages plus M3 continuation and clearly marked placeholders. After merge, update the matching editable M3 continuation (and any team editable original source) and regenerate the **same continuing PED**, not an unrelated new report. Fill in:

- Team: names and student numbers verified against existing PED.
- **Existing GitHub repo URL** (actual, not example).
- **Final RC tag/version and full commit SHA**.
- Actual CI run URL and artifacts, actual current RTM and register paths/links.
- Real or local staging demo instructions and `api/release` result; CI/test/load results and any defects.
- Date of submission, final release decision and any stakeholder/risk sign-offs.

### Moodle/formal hand-in

Per SEN381 M3 section 5.2, submit **PED v3.0 as PDF, the GitHub repository URL, the exact M3 release tag/commit, and staging/demo access**. Do not submit a disconnected code ZIP as a substitute for the controlled repository unless your lecturer asks. This ZIP is supplied for your convenience so you can perform the **GitHub portion later** as requested.

**Caution:** It is impossible to generate authentic review comments, team approvals, historical Git commits, a real build log, a deployed staging machine, or performance numbers merely by preparing ZIP files. Do those activities as yourself, and keep links to the true results.
