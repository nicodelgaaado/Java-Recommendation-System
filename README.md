# Java Recommendation System

This repository now contains a clean, buildable implementation of the Duke University
capstone assignment described in `Recommendation System Assignment.pdf`.

The original exercise folders are still present as references:

- `StepOneStarterProgram`
- `StepTwo-Simple Recommendations`
- `StepThree-Interfaces, Filters, Database`
- `StepFour-Weighted Averages`
- `StepFive-Final`

The actual project to build is under `src/main`.

## Build

```powershell
mvn package
```

## Run examples

```powershell
java -cp target\classes FirstRatings
java -cp target\classes MovieRunnerAverage
java -cp target\classes MovieRunnerWithFilters
java -cp target\classes MovieRunnerSimilarRatings
java -cp target\classes RecommendationRunner
```

To enable TMDb poster lookups for the final HTML output, set one of these
environment variables before running `RecommendationRunner`:

```powershell
$env:TMDB_API_KEY="your_tmdb_v3_key"
```

or

```powershell
$env:TMDB_BEARER_TOKEN="your_tmdb_read_access_token"
```

If neither is set, the project falls back to an inline placeholder poster instead
of showing a broken image.

## Data

CSV data files live in `src/main/resources/data`.

## Notes

- The new implementation is self-contained and does not depend on Duke's `edu.duke`
  runtime library.
- Step Five includes a `RecommendationRunner` that implements `Recommender` and
  prints an HTML recommendation table.
- `src/main/scripts/package-step-five.ps1` builds the project and zips the compiled
  Step Five class files.
