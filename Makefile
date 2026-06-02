# uwf-engine-sdk-java — release automation
#
# Usage:
#   make release          # patch bump: 1.2.1 → 1.2.2
#   make release-minor    # minor bump: 1.2.1 → 1.3.0
#   make release-major    # major bump: 1.2.1 → 2.0.0
#   make smoke-test       # run smoke tests (requires UWF_ENDPOINT)
#   make version          # print current version

MVN     := mvn
VERSIONS_PLUGIN := org.codehaus.mojo:versions-maven-plugin:2.16.2

# ── Version arithmetic ────────────────────────────────────────────────────────

CURRENT := $(shell $(MVN) help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
_PARTS   = $(subst ., ,$(CURRENT))
_MAJOR  := $(word 1,$(_PARTS))
_MINOR  := $(word 2,$(_PARTS))
_PATCH  := $(word 3,$(_PARTS))

NEXT_PATCH := $(_MAJOR).$(_MINOR).$(shell expr $(_PATCH) + 1)
NEXT_MINOR := $(_MAJOR).$(shell expr $(_MINOR) + 1).0
NEXT_MAJOR := $(shell expr $(_MAJOR) + 1).0.0

# Optional: set GPG_PASSPHRASE env var to avoid interactive prompt
GPG_ARGS := $(if $(GPG_PASSPHRASE),-Dgpg.passphrase=$(GPG_PASSPHRASE),)

.PHONY: version build release release-minor release-major smoke-test _release _require-clean

# ── Info ──────────────────────────────────────────────────────────────────────

version:
	@echo $(CURRENT)

# ── Build ─────────────────────────────────────────────────────────────────────

build:
	$(MVN) package -DskipTests -q

# ── Smoke test ────────────────────────────────────────────────────────────────
# Resolve endpoint first: vexctl rip resolve uwf-engine <region>
# Then: make smoke-test UWF_ENDPOINT=<url>   (or export UWF_ENDPOINT=...)
# Optionally pass DOCKER_NETWORK=<network> when endpoint is on a Docker network.

DOCKER_NETWORK ?=
_NET_FLAG      := $(if $(DOCKER_NETWORK),--network $(DOCKER_NETWORK),)

smoke-test: build
	@[ -n "$(UWF_ENDPOINT)" ] || \
	  (echo ""; \
	   echo "ERROR: UWF_ENDPOINT is required."; \
	   echo "       Resolve it first:  vexctl rip resolve uwf-engine <region>"; \
	   echo "       Then run:          make smoke-test UWF_ENDPOINT=<url>"; \
	   echo ""; exit 1)
	$(MVN) test-compile -q
	$(MVN) dependency:copy-dependencies -DoutputDirectory=target/deps -q
	docker run --rm $(_NET_FLAG) \
	  -e UWF_ENDPOINT=$(UWF_ENDPOINT) \
	  -v "$(PWD)/target:/app" \
	  eclipse-temurin:21-jre \
	  java -cp "/app/$(shell basename $(PWD))-$(CURRENT).jar:/app/test-classes:/app/deps/*" \
	  -ea ai.vextura.uwf_engine.SmokeTest

# ── Release ───────────────────────────────────────────────────────────────────

release:       _require-clean  ## Bump patch version and release (1.2.1 → 1.2.2)
	@$(MAKE) _release NEXT=$(NEXT_PATCH)

release-minor: _require-clean  ## Bump minor version and release (1.2.1 → 1.3.0)
	@$(MAKE) _release NEXT=$(NEXT_MINOR)

release-major: _require-clean  ## Bump major version and release (1.2.1 → 2.0.0)
	@$(MAKE) _release NEXT=$(NEXT_MAJOR)

_require-clean:
	@git diff --quiet && git diff --cached --quiet || \
	  (echo "ERROR: working tree has uncommitted changes — commit or stash first"; exit 1)

_release:
	@[ -n "$(NEXT)" ] || (echo "NEXT is not set"; exit 1)
	@echo ""
	@echo "▶  $(CURRENT) → $(NEXT)"
	@echo ""
	@# 1. Bump version in pom.xml
	@$(MVN) $(VERSIONS_PLUGIN):set -DnewVersion=$(NEXT) -DgenerateBackupPoms=false -q
	@# 2. Build (generates main jar + sources jar + javadoc jar for Central)
	@$(MVN) package -DskipTests -q
	@# 3. Publish to Maven Central — if this fails, pom.xml is reverted and
	@#    nothing is pushed to GitHub, so there is nothing to roll back remotely.
	@$(MVN) deploy -DskipTests $(GPG_ARGS) || \
	  ($(MVN) $(VERSIONS_PLUGIN):set -DnewVersion=$(CURRENT) -DgenerateBackupPoms=false -q && \
	   echo "" && echo "✗  Maven Central publish failed — pom.xml reverted to $(CURRENT)" && \
	   exit 1)
	@# 4. Commit the version bump
	@git add pom.xml
	@git commit -m "chore(release): v$(NEXT)"
	@# 5. Annotated tag
	@git tag -a v$(NEXT) -m "Release v$(NEXT)"
	@# 6. Push commits + tag to GitHub
	@git push origin main
	@git push origin v$(NEXT)
	@echo ""
	@echo "✓  v$(NEXT) published to Maven Central and pushed to GitHub"
	@echo ""
