# My Family CIBIL Tracker

Free Android starter app for personal and family credit/loan tracking.

## Features in this starter
- Family profiles with manually entered CIBIL score
- Loan records linked to a family profile
- Outstanding loan balance and monthly EMI tracking
- EMI calculator
- Link to official CIBIL free-score page
- Local-only storage; no ads, paid APIs, or subscription

## Build
This project is built by the GitHub Actions workflow at `.github/workflows/my-family-cibil-tracker.yml`.
Open the repository's Actions tab, select **Build My Family CIBIL Tracker APK**, then run the workflow. Download the artifact named `my-family-cibil-tracker-debug-apk` and extract `app-debug.apk`.

## Important privacy and accuracy notes
- This starter does not connect to TransUnion CIBIL and cannot retrieve or verify an official credit report. Users must visit the official CIBIL site; scores shown here are manually entered.
- Add family member information only with that person's informed permission.
- This MVP does not yet encrypt local data or provide PIN/biometric protection. Do not enter sensitive identity numbers or upload actual credit reports.
- EMI is an estimate, not a lender quote.
