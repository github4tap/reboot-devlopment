# AEM Team Lead Interview Q&A

Prepared for offline study. Answers are framed at a **team lead / senior architect** depth: concepts, trade-offs, and practical recommendations for AEM on-prem / AMS and AEM as a Cloud Service (AEMaCS).

---

## 1. What is Principal ACL w.r.t. Adobe Experience Manager?

### Short answer

In AEM (Jackrabbit / Oak), a **Principal** is *who* is being authorized (a user, a group, or a system/service identity). An **ACL** (Access Control List) is the set of **ACE**s (Access Control Entries) on a repository path that grant or deny privileges to that principal.

**Principal ACL** usually means: the ACL evaluation and storage model where permissions are attached to content nodes and evaluated against the **principals** of the authenticated session—not against “login names” alone.

### How it works in AEM

1. A user logs in → AEM builds a subject with one or more **principals** (the user principal + all group principals they belong to).
2. For a requested path/action, Oak evaluates ACEs on that path (and ancestors, depending on inheritance).
3. Privileges (`jcr:read`, `jcr:write`, `crx:replicate`, `jcr:all`, etc.) are allowed or denied.
4. **Deny** typically wins over allow when conflicting (order and best-effort rules matter; do not rely on subtle order tricks in production design).

### Principal vs User vs Group

| Concept | Meaning |
|--------|---------|
| User | Account that can authenticate |
| Group | Collection of users (also a principal) |
| Principal | Security identity used in ACEs |
| ACL | List of ACEs on a path |
| ACE | One allow/deny of privileges for one principal |

### Example

Path: `/content/wknd/us/en`

| Principal | Privileges | Type |
|-----------|------------|------|
| `everyone` | `jcr:read` | Allow |
| `content-authors` | `jcr:write`, `jcr:versionManagement` | Allow |
| `anonymous` | `jcr:all` | Deny (or simply no write) |

A user in `content-authors` can edit that tree; anonymous can only read (if publish policies allow).

### Lead-level talking points

- Prefer **group-based ACLs**, not per-user ACLs.
- Keep ACLs **shallow and consistent** (site root / brand / locale), not deep per-page noise.
- Service users should get **least privilege** via dedicated principals + repo-init (especially on AEMaCS).
- On AEMaCS, mutable runtime ACL edits are fragile after restart/redeploy unless expressed as **immutable config** (repo-init / content packages with clear ownership).

---

## 2. How should we handle users / access (ACL permissions) during migration from AEM on-prem to AEM as a Cloud Service?

### Core principle

**Do not “lift and shift” users, groups, and ACLs blindly.** AEMaCS changes identity, mutability, and how permissions should be declared.

### What changes on AEMaCS

| Area | On-prem / AMS | AEMaCS |
|------|---------------|--------|
| User management | Often local users + LDAP/SAML | Prefer **IMS / Admin Console** enterprise IDs; local users discouraged for humans |
| ACL persistence | Can be changed live on Author | Treat permissions as **code/config**; runtime-only ACL drift is risky |
| Service users | Created via UI / scripts | Prefer **repo-init** + service user mapping |
| `/home` content | Often packaged | Generally **do not migrate** `/home` as mutable content |
| Permissions model | Same Oak concepts | Same concepts, different **delivery** (Cloud Manager + repo-init) |

### Recommended migration approach

1. **Inventory**
   - Export/list groups, custom privileges, path-level ACL patterns, closed user groups (CUG), service users, impersonation use cases.
   - Map each principal to: human (IMS group), system (service user), or obsolete.

2. **Redesign identity**
   - Humans → Adobe Admin Console product profiles / IMS groups synced to AEM.
   - Authors/admins → Admin Console role assignment, not package-deployed local users.
   - System integrations → OSGi service users defined via **repo-init**.

3. **Recreate groups intentionally**
   - Recreate only needed business groups (`wknd-authors`, `wknd-approvers`, etc.).
   - Align naming with IMS groups where possible.

4. **Express ACLs as code**
   - Put path ACLs and service-user ACLs in **repo-init** scripts under `ui.config` (or equivalent).
   - Keep content packages free of accidental `/home` and ad-hoc ACE dumps unless deliberately owned.

5. **Validate least privilege**
   - Re-test authoring paths, DAM folders, workflow models, replication/publish actions, GraphQL/Assets APIs.
   - Remove legacy “admin-like” groups that existed for convenience on-prem.

6. **Cutover checklist**
   - Confirm IMS login works in Stage.
   - Confirm group membership mirrors Admin Console.
   - Confirm critical paths: `/content`, `/content/dam`, `/conf`, `/var` as needed.
   - Confirm anonymous/publish read model and CUGs still behave with CDN/Dispatcher rules.

### What not to do

- Package and install production `/home/users` and `/home/groups` as the primary identity strategy.
- Rely on one-off ACL fixes only in Production Author UI without committing them to repo-init/content-as-code.
- Copy old “admin” service users with broad `jcr:all` on `/`.

### Example (service user via repo-init mindset)

On-prem: someone created `wknd-asset-service` in the UI and set ACLs manually.

AEMaCS: define create-service-user + set ACL on `/content/dam/wknd` in repo-init, map the bundle to that user in OSGi config. Redeploys recreate the intended state.

---

## 3. Let’s assume we need to migrate DAM assets worth 100 GB into AEM 6.5. What are the best practices?

### Goal

Move ~100 GB of binaries + metadata into AEM 6.5 DAM with integrity, acceptable runtime impact, and correct processing (renditions, metadata, workflows).

### Best practices

1. **Assess before move**
   - Count assets, folder depth, largest files, MIME types, custom metadata schemas, smart tags, versions, references from pages/XFs.
   - Decide: full history/versions needed or latest-only?
   - Check datastore type (File Data Store / S3 / Azure) and available disk/IOPS.

2. **Prefer bulk tooling over manual packages for 100 GB**
   - **Adobe Best Practices Importer / ACS Commons tools / custom Asset migration utilities** as appropriate for the landscape.
   - For many enterprises: stage binaries close to AEM (same network/region), then ingest.
   - Avoid giant single content packages (`.zip`) for 100 GB—package size, timeouts, and memory become painful.

3. **Use a staged pipeline**
   - Export inventory (CSV of path, size, checksum, lastModified).
   - Transfer binaries to a landing zone.
   - Ingest in **batches** (by folder/brand/date), not one mega-job.
   - Validate checksums / spot-check renditions after each batch.

4. **Tune for ingest load**
   - Temporarily adjust workflow launchers (DAM Update Asset) if bulk ingest would overwhelm CPU/queues—or use “skip processing / deferred processing” patterns where supported by your approach, then reprocess selectively.
   - Scale Author CPU/heap/datastore I/O for the migration window.
   - Run heavy ingest off peak; monitor `JobManager`, workflow failures, disk.

5. **Metadata & taxonomy**
   - Migrate metadata schemas first.
   - Preserve `dc:title`, custom namespaces, tags; map old tag IDs if tag structure changes.
   - Keep folder ACLs/policies in mind (folder permissions after tree creation).

6. **Binary storage strategy**
   - Ensure datastore has enough headroom (100 GB assets ≈ more with renditions).
   - Renditions can multiply storage significantly—plan capacity (often 1.5x–3x+ depending on profiles).
   - If using shared/cloud datastore patterns in AMS, validate consistency and GC implications.

7. **Reference integrity**
   - After migration, fix/page-check references (pages pointing to old DAM paths).
   - Prefer stable path strategy; path renames are costly at 100 GB scale.

8. **Validation & cutover**
   - Sampling QA + automated path existence checks.
   - Compare counts/sizes between source and target.
   - Publish tree in waves; verify Dispatcher/CDN cache behavior for Assets.

### Example batch plan

- Week 1: `/content/dam/wknd/en/adventures` (20 GB) + metadata schema.
- Week 2: brand B folders (30 GB).
- Continuous: failed assets retry queue + checksum report.
- Final: freeze source writes, delta sync, switch authors to new DAM paths.

### Lead caution

“100 GB” is medium-large—not only binary copy, but **workflow CPU**, **Oak indexing**, and **rendition storage**. Success is measured by usable, searchable, correctly processed assets—not just files on disk.

---

## 4. What extra info does an OSGi bundle (AEM `core` installed in system console) contain compared to a `.jar` (executable Java archive)?

### Short answer

An OSGi bundle **is** a JAR, but with **OSGi metadata** that lets the Apache Felix framework install, resolve, start, update, and isolate it inside AEM. A plain executable JAR is typically meant for a standalone JVM (`main`), not for the OSGi module system.

### Extra / different information in a bundle

Primarily in `META-INF/MANIFEST.MF`:

| Header / concept | Why it matters in AEM |
|------------------|------------------------|
| `Bundle-SymbolicName` | Unique module identity |
| `Bundle-Version` | Versioning / updates |
| `Bundle-Activator` | Optional start/stop hook |
| `Export-Package` | Packages visible to other bundles |
| `Import-Package` | Dependencies resolved from the framework |
| `Private-Package` | Internal packages (via BND tooling) |
| `Provide-Capability` / `Require-Capability` | Richer dependency model |
| `Bundle-Name`, `Bundle-Description`, vendor headers | Console readability |
| `Service-Component` / SCR descriptors | Declarative services (OSGi DS) for `@Component` |
| Embedded OSGi service registrations | Runtime services visible in Felix console |

Build tooling (Maven Bundle Plugin / BND) also shapes package exports and may embed descriptors under `OSGI-INF/`.

### What a typical “executable JAR” has instead

- `Main-Class` (and maybe `Class-Path`)
- Application classes meant to run with `java -jar`
- No OSGi package wiring; classpath is flat/fat-jar style

### Example

`core` module of a WKND-style project:

- Produces `com.adobe.aem.guides.wknd.core-x.y.z.jar`
- Installs via Cloud Manager / `install` folder / bundle console
- Exports Sling Models / services; imports `org.apache.sling.*`, `com.day.cq.*`, etc.
- Felix resolves imports against other bundles already in AEM
- Components appear under **OSGi > Components**; bundle under **OSGi > Bundles**

If you dropped a Spring Boot fat JAR with only `Main-Class` into AEM, it would **not** integrate as a proper AEM module (wrong lifecycle, wrong classloading, no DS components wired into Sling).

### Lead-level nuance

- Same file extension (`.jar`), different **contract**.
- AEM cares about **bundle resolution state** (`Installed` / `Resolved` / `Active`).
- Versioning and package wiring mistakes cause the classic “bundle not Active” production incident.

---

## 4b. What are repo-init, versioned ClientLibs, and Dynamic Media—and their importance (on-prem and AEMaCS)?

*(Renumbered as 4b because the source list had two “4.” items.)*

### A) Repo-init

**What it is:** Declarative scripts (Sling Repo Init) executed at repository initialization / deployment to create users, groups, ACLs, paths, and related JCR setup.

**Why important**

| On-prem / AMS | AEMaCS |
|---------------|--------|
| Useful for consistent environment bootstrap | **Essential**—preferred way to define service users & ACLs as code |
| Can still be mixed with UI setup | UI-only setup does not survive cleanly as “desired state” across deploys |
| Helps multi-env parity | Required pattern for Cloud Manager–delivered config |

**Example**

```text
create service user wknd-replication-service with path system/wknd
set ACL on /content/wknd
  allow jcr:read, crx:replicate for wknd-replication-service
```

Importance: repeatable security baseline; no manual Author clicking after each new environment.

---

### B) Versioned ClientLibs (`allowProxy` + versioned libraries / long-cache pattern)

**What it is:** Client-side libraries (JS/CSS) managed as AEM ClientLibs. “Versioned clientlibs” generally means generating **cache-friendly URLs** (hash/version in path or file name) so browsers/CDN can cache aggressively while updates bust cache automatically.

In modern AEM projects this often involves:

- `cq:ClientLibraryFolder` with categories
- ACS Commons / AEM clientlib versioning / frontend build hashing
- Proxy servlet (`/etc.clientlibs/...`) instead of exposing `/apps` or `/libs`

**Why important**

| On-prem / AMS | AEMaCS |
|---------------|--------|
| Improves performance with Dispatcher TTL | Critical with **CDN + immutable deploy** model |
| Avoids authors seeing stale CSS/JS | Pipeline deploys need reliable cache busting |
| Proxy hides `/apps` | Same, plus Cloud CDN best practices |

**Example**

Frontend webpack emits hashed bundles into `ui.apps` clientlibs. Page references `/etc.clientlibs/wknd/clientlibs/clientlib-site.min.css` with a versioned path/query. After deploy, hash changes → CDN fetches new file; old hash can remain cached safely.

---

### C) Dynamic Media

**What it is:** Adobe’s media delivery/optimization layer (Scene7 / Dynamic Media) for imaging and video: smart imaging, rendition URLs, presets, viewers, responsive delivery—often via Dynamic Media hybrid or Dynamic Media Scene7 with AEM Assets.

**Why important**

| On-prem / AMS | AEMaCS |
|---------------|--------|
| Offloads rendition delivery from AEM Publish | Strongly aligned with cloud delivery; reduces Publish/Assets runtime load |
| Needs correct cloud config, DMS7 settings, replication to DM | Configure via Cloud Services / IMS-era setups; validate CDN behavior |
| Great for large media businesses | Often preferred over generating every rendition only inside AEM |

**Example**

Instead of serving 10 static pixel renditions from AEM Publisher for a hero image, use a Dynamic Media URL with width/quality parameters so the CDN delivers an optimized derivative. Authors still manage the asset in DAM; delivery is Dynamic Media.

---

### Combined lead takeaway

- **Repo-init** = identity & ACL as code  
- **Versioned ClientLibs** = performant, cache-safe front-end delivery  
- **Dynamic Media** = scalable media runtime  

All three matter on-prem, but on **AEMaCS** they become closer to **default architecture expectations**.

---

## 5. What are best practices for AEM Cloud migration?

### Strategic practices

1. **Treat it as a transformation, not a rehost**
   - Remove unsupported features (custom runmodes sprawl, classic UI dependence, certain workflow/replication patterns, Oak index anti-patterns, mutable installs on Author).
   - Align to AEMaCS [development guidelines](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/development-guidelines) and deprecated API lists.

2. **Adopt Cloud-ready project structure**
   - Maven archetype modules: `core`, `ui.apps`, `ui.apps.structure`, `ui.config`, `ui.content`, `ui.frontend`, `all`, tests.
   - Separate **code**, **config**, and **mutable content**.
   - OSGi configs as **cfg.json** (or proper Cloud formats), not only repository nodes edited live.

3. **Content migration plan**
   - Use Adobe Content Transfer Tool (CTT) / Cloud Acceleration Manager patterns where applicable.
   - Run analysis (Best Practices Analyzer / Cloud Readiness) early; remediate blockers before final move.
   - Migrate content in waves; include delta migration before cutover.

4. **Code remediation hotspots**
   - Replication API usage → think **Sling Content Distribution / publish persistence model**.
   - Heavy `Session` / JCR writes in request thread → async / safer patterns.
   - Custom indexes → define Oak index definitions as code; avoid over-indexing.
   - File system writes on Author/Publish → not durable on Cloud.
   - Binary-heavy custom processes → Assets microservices / async jobs.

5. **Security & identity**
   - IMS + Admin Console for users.
   - Repo-init for service users/ACLs.
   - Secrets via Cloud/ENV mechanisms—not hardcoded.

6. **Environments & quality gates**
   - Dev → Stage → Prod via Cloud Manager.
   - Local SDK parity for developers.
   - Automated unit, integration (`it.tests`), and UI (`ui.tests`) gates.

7. **Dispatcher / CDN**
   - Move rules to Cloud Manager Dispatcher config conventions.
   - Design for Adobe-managed CDN; avoid assuming old AMS Apache-only behaviors.

8. **Operations readiness**
   - Logging/monitoring via Cloud tools.
   - Runbooks for pipeline failure, content rollback strategy, index reindexing expectations.
   - Performance tests on Stage with production-like content sizes.

9. **Cutover**
   - Freeze or control writes on source.
   - Final delta content sync.
   - DNS / vanity / SSL cutover.
   - Post-go-live hypercare (authoring, Assets processing, cache purge strategy).

### Example remediation

On-prem custom servlet wrote debug files under `crx-quickstart/logs/custom`. On AEMaCS that path is not a durable ops strategy—emit to logger, use Cloud logging, store durable artifacts in appropriate external storage if required.

---

## 6. What are different pipelines configured for AEMaCS / different programs?

### Context

In **Adobe Cloud Manager**, each **Program** (Sites, Assets, or combined) contains **Environments** (Prod, Stage, Dev, RDE, etc.) and **Pipelines** that build and deploy.

Exact names vary by program setup, but these are the common pipeline **types**:

### Full Stack Pipeline

- Builds the full AEM Maven project (code + mutable content packages as configured).
- Runs quality gates: code scanning, unit tests, security/quality ratings, build.
- Deploys to target environment (often Stage then Prod promotion pattern).
- Executes **Custom Functional Testing** (`it.tests`) and **Custom UI Testing** (`ui.tests`) when configured.
- Typical path for WKND-style Java stack projects.

### Frontend Pipeline (or Front End Pipeline)

- Builds and deploys frontend-only artifacts (site theme / SPA / clientlib-oriented front-end) faster than full stack.
- Used when front-end teams iterate independently of Java/OSGi changes.
- Important for sites using modern front-end decoupling patterns supported by AEMaCS.

### Web Tier / Dispatcher Config Pipeline

- Deploys Dispatcher / web-tier configuration (and related CDN rules packaging as applicable).
- Lets ops/dev update caching/security rules without rebuilding the entire AEM application stack.

### Config Pipeline (where enabled / applicable)

- Focused on configuration deployment patterns (program-dependent naming).
- Used to push config changes with a narrower scope than full stack.

### Asset Compute / related specialty pipelines (Assets-heavy programs)

- For Asset Compute workers / custom processing projects associated with AEM Assets as a Cloud Service.
- Separates media-processing code lifecycle from Sites full-stack code.

### CI vs CD considerations (lead talking points)

- **Non-production pipelines**: auto or branch-triggered to Dev/Stage for continuous validation.
- **Production pipelines**: gated approvals, scheduled go-lives, stricter quality gates.
- **Program types** differ:
  - **Sites program**: full stack + frontend + web tier commonly.
  - **Assets program**: Assets-focused envs + asset processing related pipelines.
  - **Sandbox vs Production programs**: sandbox is for learning/PoC; production programs have contractual envs and stricter ops.

### Example (Sites production program)

1. Developer merges to `main`.
2. **Full Stack Pipeline** builds `all` package, deploys to Stage, runs UI tests.
3. Content authors validate on Stage.
4. Pipeline proceeds / separate Prod deploy step pushes same build to Production.
5. Parallel: **Front End Pipeline** for a CSS-only hotfix to Stage/Prod without waiting on Java rebuild.
6. **Web Tier Pipeline** updates Dispatcher filter rules for a new API path.

### What interviewers often want to hear

- Pipelines are the **only supported deploy path** (no direct package install to Prod like old AMS habits).
- Quality gates can **block** Prod.
- Split pipelines exist to **optimize speed and blast radius** (app vs front-end vs dispatcher).

---

## Quick revision sheet

| Topic | One-liner |
|-------|-----------|
| Principal ACL | Permissions evaluated for security principals (user/group/service) via ACEs on paths |
| ACL migration to AEMaCS | Recreate via IMS groups + repo-init; don’t lift `/home` blindly |
| 100 GB DAM to 6.5 | Batch ingest, tune workflows/datastore, validate checksums & references |
| OSGi bundle vs JAR | Bundle = JAR + OSGi manifest/wiring/DS lifecycle for Felix |
| Repo-init | Declarative users/ACLs/paths as deployable config |
| Versioned ClientLibs | Cache-busting, proxy-safe JS/CSS delivery |
| Dynamic Media | Optimized media delivery offloaded from AEM runtime |
| Cloud migration | Remediatate + CTT/BPA + cloud project structure + pipelines |
| AEMaCS pipelines | Full stack, frontend, web tier (dispatcher), plus specialty pipelines by program |

---

## Suggested follow-up questions (to ask the interviewer / practice aloud)

1. How do you design Oak indexes for a high-read Sites + Assets catalog?
2. What breaks if mutable content is accidentally shipped in every full-stack deploy?
3. How do Sling Content Distribution and classic replication differ operationally on AEMaCS?
4. When do you choose Dynamic Media vs AEM-generated renditions only?

---

*Document generated for interview prep. Validate environment-specific pipeline names against the Adobe Cloud Manager UI / current Experience League docs for your program type.*
