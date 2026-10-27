# Troubleshooting Log — library-system

A record of real problems hit while building this project, how each one was diagnosed, and how it was fixed. Kept as a reference for future debugging and as a demonstration of actual problem-solving, not just working code.

---

## 1. UUID vs Long type mismatch

**Symptom:** Early version of `LoanServiceImpl` wouldn't compile — ID types didn't line up between entities and service methods.

**Root cause:** Entities were inconsistently using `Long` in some places and `UUID` in others.

**Fix:** Standardized every entity (`Author`, `Book`, `Member`, `Loan`) on `UUID` IDs throughout — entity fields, repository generics, and service method signatures.

---

## 2. Stray auto-generated `Valid.java` shadowing the real annotation

**Symptom:** `@Valid` wasn't behaving like a validation annotation — IntelliJ had silently created a local class instead of resolving the import.

**Root cause:** IntelliJ auto-import picked the wrong candidate and generated an empty `Valid.java` class in the `controller` package, which shadowed `jakarta.validation.Valid`.

**Fix:** Deleted the stray generated file so the real import resolved correctly.

---

## 3. Missing validation dependency + malformed `pom.xml`

**Symptom:** Bean validation annotations (`@NotBlank`, `@Email`, etc.) had no effect.

**Root cause:** `spring-boot-starter-validation` wasn't on the classpath yet, and a manual edit had placed a new `<dependency>` block outside the closing `</dependencies>` tag, making the whole file structurally invalid.

**Fix:** Added the missing dependency and corrected the XML structure.

---

## 4. Hibernate schema mismatch: `CHAR(36)` vs `BINARY(16)`

**Symptom:** `SchemaManagementException: wrong column type in column [id] in table [author]; found char, expecting binary(16)`.

**Root cause:** Default Hibernate 6/7 UUID mapping expects `BINARY(16)`, but the table had been created as `CHAR(36)` from an earlier manual fix — schema and entity mapping disagreed.

**Fix:** Added `@JdbcTypeCode(SqlTypes.CHAR)` explicitly on the `id` field of all four entities, forcing UUID to map to `CHAR(36)` consistently. Dropped and let Hibernate rebuild the tables cleanly, then confirmed via DBeaver.

---

## 5. GitHub push denied — wrong cached credentials

**Symptom:** `git push` failed with `Permission to danielGb001/library-system.git denied to paulayo93`.

**Root cause:** macOS Keychain had cached GitHub credentials for a different account that had previously used the same machine.

**Fix:** Cleared the stale credentials (`security delete-internet-password -s github.com`, repeated until none remained, plus `security delete-generic-password -s github.com`), generated a new Personal Access Token for the correct account, and re-authenticated on push.

---

## 6. Double-returning a loan caused a raw 500 instead of a clean error

**Symptom:** Returning an already-returned loan threw an unhandled exception straight to the client.

**Root cause:** `GlobalExceptionHandler` had no handler for `IllegalStateException`, which is what `LoanServiceImpl` throws for business-rule conflicts like double-return.

**Fix:** Added an `IllegalStateException` handler mapping to `409 CONFLICT`.

---

## 7. False alarm: "available copies" looked wrong but wasn't

**Symptom:** `availableCopies` showed 2 instead of an expected 3 after returning a book.

**Root cause:** Not a bug — the book had actually been borrowed twice (confirmed via `GET /api/loans`, which showed two separate loan records, one returned and one still active).

**Lesson:** Always check the actual data before assuming the code is wrong.

---

## 8. `pom.xml` corrupted with wrong artifact names and an entire stray Kotlin toolchain

**Symptom:** Build failed with invalid/non-existent Maven artifacts (`spring-boot-starter-webmvc` instead of `-web`, duplicated/nonexistent test starters), plus a full `kotlin-maven-plugin` setup injected into a pure-Java project.

**Root cause:** Unclear exact origin — likely from an unrelated tool or editor action, not a deliberate change. The Kotlin plugin's own `test-compile` execution phase interfered with how Maven assembled the classpath for ordinary Java test sources.

**Fix:** Removed the Kotlin plugin block entirely, removed the two stray Kotlin dependencies, corrected the misnamed artifacts, and collapsed a duplicated `maven-compiler-plugin` configuration down to one clean block with just the Lombok annotation processor.

---

## 9. Spring Boot 4 removed `@MockBean` and moved `@WebMvcTest`

**Symptom:** New controller tests failed to compile: `package org.springframework.boot.test.autoconfigure.web.servlet does not exist`, `package org.springframework.boot.test.mock.mockito does not exist`, `cannot find symbol: class WebMvcTest`, `cannot find symbol: class MockBean`.

**Root cause:** The project's `spring-boot-starter-parent` is pinned to **4.1.0** — Spring Boot 4 made breaking changes to testing APIs that were only deprecated (not yet removed) in 3.4, which is what most tutorials and training material still assume:
- `@MockBean` (`org.springframework.boot.test.mock.mockito`) was **removed entirely**, replaced by `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`), which now comes from Spring Framework 7 itself rather than Spring Boot.
- `@WebMvcTest` moved from `org.springframework.boot.test.autoconfigure.web.servlet` to `org.springframework.boot.webmvc.test.autoconfigure`, and now requires its own dedicated test starter (`spring-boot-starter-webmvc-test`) rather than coming bundled in `spring-boot-starter-test`.

**Fix:** Added `spring-boot-starter-webmvc-test` to `pom.xml` (replacing the old `spring-boot-starter-test`, since the new starter pulls it in transitively), updated the two broken imports to their new locations, and swapped `@MockBean` for `@MockitoBean`.

---

## 10. `@WebMvcTest` no longer auto-provides a Jackson `ObjectMapper` bean

**Symptom:** Even after fixing the imports above, all 8 new controller tests failed at runtime with `NoSuchBeanDefinitionException: No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper'`.

**Root cause:** A further Spring Boot 4 behavior change — `@WebMvcTest` no longer reliably configures a Jackson `ObjectMapper` bean for autowiring, particularly when the main application still uses the classic `spring-boot-starter-web` starter rather than the newer modular `spring-boot-starter-webmvc`.

**Fix:** Stopped relying on Spring to inject the `ObjectMapper` entirely. Since it was only being used to serialize request bodies to JSON strings — a plain utility operation — replaced `@Autowired private ObjectMapper objectMapper;` with a directly instantiated `private final ObjectMapper objectMapper = new ObjectMapper();`. Sidesteps the auto-configuration gap completely without touching main runtime dependencies.

---

## 11. Downloaded file version mix-ups

**Symptom:** Several rounds of "I fixed it" followed by "the problem is still there" — most memorably with `pom.xml` and `AuthorControllerTest.java` both appearing to be unchanged after being replaced.

**Root cause:** Each time a corrected file was downloaded, the browser didn't overwrite the existing file in `~/Downloads` — it saved a new copy with `(1)`, `(2)`, `(3)` appended. Running `cp ~/Downloads/pom.xml ...` kept grabbing the original, un-fixed file, since that one kept its plain, unnumbered name.

**Fix:** Checked `ls -la` on the relevant filenames, sorted by timestamp, and copied specifically the most recently downloaded (highest-numbered) version each time. Longer-term fix: clear out old numbered duplicates from Downloads after each resolved issue so this can't silently repeat.

---

## 12. Shell quoting: tilde doesn't expand inside double quotes

**Symptom:** `cp "~/Downloads/pom(2).xml" ...` failed with `No such file or directory`, even though the file existed.

**Root cause:** In bash/zsh, `~` is only expanded by the shell when it's unquoted or at the very start of an unquoted word. Wrapping the whole path in double quotes (done to protect the parentheses) also disabled the `~` expansion, so it was read literally as a folder named `~`.

**Fix:** Kept `~` outside the quotes and quoted only the part containing the parentheses: `cp ~/Downloads/"pom(2).xml" ...`.

---

## 13. `git push` rejected: "Password authentication is not supported"

**Symptom:** `remote: Invalid username or token. Password authentication is not supported for Git operations. fatal: Authentication failed`, even though the account password was correct.

**Root cause:** Two things. GitHub no longer accepts the account password for Git over HTTPS (since 2021); Git needs a personal access token in the password field. And the token cached in the macOS Keychain from an earlier session had expired, because classic tokens default to a 30-day lifetime.

**Fix:** Generated a new personal access token (classic) under the correct account, cleared the stale credential from the Keychain (`security delete-internet-password -s github.com`, repeated until none remained), and pushed again, entering the token as the password. The token needed the **`workflow`** scope as well as `repo`, because GitHub rejects pushes that add or change files under `.github/workflows/` without it.

**Lesson:** An authentication error after weeks of working pushes usually means an expired credential, not a broken repo. Put token expiry dates in a calendar.

---

## 14. Database password committed to a public repository

**Symptom:** Preparing to add CI, a check of the history (`git log -p -- src/main/resources/application.properties | grep -i password`) showed the real database password in a committed version of the file. The repository is public.

**Root cause:** The `application.properties` file was tracked by git, and the original README setup steps told readers to put their credentials directly in it, so the real password was committed along with everything else.

**Fix:**
1. **Rotated the password** in MySQL (`ALTER USER 'libraryUser'@'localhost' IDENTIFIED BY '...'`). Once the old value no longer works, its presence in history is harmless.
2. **Moved the secret out of the committed file.** Spring Boot automatically loads `config/application.properties` from the project root and lets it override the main file, so the password now lives there, and `config/` was added to `.gitignore`. The committed file keeps a comment pointing to the README instead of a value.
3. **Updated the README** setup steps to describe the git-ignored override.
4. Re-ran the full suite to confirm the override works (77 tests still passing, with Hibernate connecting using the password from `config/`).

**Why history wasn't rewritten:** Removing a secret from a public repo's history doesn't un-leak it, since clones, forks and caches may already hold it, and a force-push is disruptive. Rotating the credential is what actually closes the exposure.

**Lesson:** Treat any secret that has been committed (or pasted into a chat or ticket) as compromised, and rotate it rather than trying to hide it. Keep secrets out of tracked files from the start.

---

## 15. Running the tests in CI: `ddl-auto=validate` against an empty database

**Symptom (anticipated and avoided):** Adding GitHub Actions meant `LibrarySystemApplicationTests` would start the full application on a clean runner, connecting to a brand-new MySQL instance with no tables.

**Root cause:** The committed config uses `spring.jpa.hibernate.ddl-auto=validate`, which checks the existing schema against the entities and fails at startup if the tables are missing. The same problem affects anyone cloning the repo onto a fresh database.

**Fix:** The CI workflow starts a throwaway MySQL 8.4 service container and overrides settings through environment variables, which Spring Boot gives priority over `application.properties`: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `SPRING_JPA_HIBERNATE_DDL_AUTO=update` so Hibernate creates the tables on the empty database. No application code or committed config had to change. The README's setup steps now tell local users to set `ddl-auto=update` for their first run too. Both CI runs passed (about a minute each).

**Lesson:** Spring Boot's property override order (environment variables over config files) makes it possible to run the same build against different environments without touching the code.

---

## Takeaways

- Several of the hardest bugs here (#8, #9, #10) came from the project being on **Spring Boot 4.1.0**, a major version with real breaking changes to testing APIs that predate most available tutorials and training material. When something that "should just work" doesn't, checking the specific version in `pom.xml` against official migration notes is often faster than assuming the code is wrong.
- Two bugs (#2, #11) were really IDE/tooling behavior, not code bugs — auto-import picking the wrong candidate, and the browser's file-naming convention for repeat downloads.
- One "bug" (#7) wasn't a bug at all — worth always checking the underlying data before assuming the code is at fault.
- Secrets need their own handling from day one (#13, #14): git-ignored local config, environment variables in CI, and rotation as the first response to any exposure. Token expiry is the other credential trap worth planning for.
