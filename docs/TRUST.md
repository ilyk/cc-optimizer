# Trust Registry — External Source Analysis Log

This document tracks all external sources that have been analyzed for security.

## Trust Levels
- ✅ **TRUSTED**: Analyzed, no issues, approved for use
- ⚠️ **CONDITIONAL**: Has findings, approved with awareness
- ❌ **REJECTED**: Not approved for use
- 🔄 **PENDING**: Awaiting analysis

## Risk Levels
- 🟢 **CLEAN** (0-24): No suspicious patterns
- 🟡 **SUSPICIOUS** (25-74): Patterns warrant review
- 🔴 **MALICIOUS** (75-100): Known malware signatures

---

## Analyzed Sources

### Git Repositories

| Date | Repository | Commit/Tag | Risk | Decision | Notes |
|------|------------|------------|------|----------|-------|
| _example_ | github.com/owner/repo | abc1234 | 🟢 | ✅ | No issues |

### Archives / Downloads

| Date | Source URL | SHA256 | Risk | Decision | Notes |
|------|------------|--------|------|----------|-------|
| _example_ | https://example.com/file.zip | sha256... | 🟡 | ⚠️ | Contains eval() |

### Dependencies

| Date | Package | Version | Registry | Risk | Decision | Notes |
|------|---------|---------|----------|------|----------|-------|
| _example_ | some-package | 1.2.3 | npm | 🟢 | ✅ | Official registry |

---

## Detailed Analysis Reports

<!--
### [Source Name] — YYYY-MM-DD

**Source**: [URL or path]
**Type**: git-repo / archive / download / paste
**Hash**: [SHA256]
**Risk Level**: 🟢/🟡/🔴 (Score: XX/100)
**Decision**: ✅/⚠️/❌
**Analyzed by**: source-analyst

#### Findings
- [Finding 1 with file:line reference]
- [Finding 2 with file:line reference]

#### Justification
[Why this decision was made]

#### Restrictions (if CONDITIONAL)
- [Restriction 1]
- [Restriction 2]

---
-->

---

## Trust Policy

### Auto-Trusted Sources
These sources are trusted by default (still logged):
- Official package registries (npm, PyPI, crates.io, Maven Central)
- Repos owned by: <!-- Add trusted GitHub orgs/users here -->

### Always Analyze
These sources always require analysis:
- Any git repo not in auto-trusted list
- Archives from any external source
- Pasted code from unknown origins
- Dependencies from non-official registries

### Trust Expiration
- **Git repos**: Trust valid for specific commit/tag only
- **Archives**: Trust valid for specific SHA256 only
- **Packages**: Trust valid for specific version only
- **SUSPICIOUS approvals**: Expire after 30 days
- **MALICIOUS approvals**: Single-use only (require re-approval each time)

### Review Frequency
| Risk Level | Review Frequency |
|------------|------------------|
| 🟢 CLEAN | Annual |
| 🟡 SUSPICIOUS | Monthly |
| 🔴 MALICIOUS | Per-use |
