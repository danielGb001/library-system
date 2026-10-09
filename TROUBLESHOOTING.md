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

## Takeaways

- Several of the hardest bugs here (#8, #9, #10) came from the project being on **Spring Boot 4.1.0**, a major version with real breaking changes to testing APIs that predate most available tutorials and training material. When something that "should just work" doesn't, checking the specific version in `pom.xml` against official migration notes is often faster than assuming the code is wrong.
- Two bugs (#2, #11) were really IDE/tooling behavior, not code bugs — auto-import picking the wrong candidate, and the browser's file-naming convention for repeat downloads.
- One "bug" (#7) wasn't a bug at all — worth always checking the underlying data before assuming the code is at fault.
