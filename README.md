# Perform to Platform

Perform to Platform is a hands-on course for experienced COBOL developers learning enterprise Java and Spring Boot. You will carry one synthetic credit-request workflow from a business rule through a persisted service and HTTP API.

## Start here

Begin with [Module 0: Set up and trace a Java program](modules/00-orientation/README.md). It introduces the course toolchain and follows an input through a small Java program. Then continue with [Module 1: Carry a business rule into Java](modules/01-business-rules/README.md).

The labs are independent Maven projects that build on the same workflow. Use the [curriculum](CURRICULUM.md) to see the learning sequence and current material status. Read [course purpose and design](PURPOSE.md) for the audience, scope, and teaching principles.

## Course materials

The `modules/` directory contains runnable lab projects and their guides. The `course/` directory contains shared reference material and a locally packaged HTML course with every lesson. Its pages and linked source examples work from the archive; links to external reference documentation require network access. Learners also need the matching course checkout to build and change the Maven projects.

## Maintainer checks

The course build uses Node.js 22 or later. From the repository root, run:

```sh
npm --prefix course ci
npm --prefix course run build
npm --prefix course run check
```

The build replaces `course/site/`. To distribute the course site, create an archive of that directory's contents so `index.html` is at the archive root. Give learners the archive and the matching course checkout.

The [instructor preflight](course/instructor-preflight.md) describes workstation checks for the current pilot. The [GitHub Actions workflow](.github/workflows/verify-labs.yml) builds the runnable Maven labs on Windows and Linux.
