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

## Run the full local rating workflow

```powershell
mvn package
java -cp target\classes RecommendationWebServer
```

The server opens `http://localhost:8000/` and keeps the enhanced user-rating flow local:

- 12 movies are displayed for rating
- movie posters are resolved from TMDb when configured, otherwise Wikipedia or an inline placeholder is used
- at least 5 ratings are required before submission
- recommendations are shown in an HTML table with the richer card and table layout

To run on a different port:

```powershell
java -cp target\classes RecommendationWebServer 8080
```

## Data

CSV data files live in `src/main/resources/data`.

## Notes

- The new implementation is self-contained and does not depend on Duke's `edu.duke`
  runtime library.
- Step Five includes a `RecommendationRunner` that implements `Recommender` and
  prints an HTML recommendation table, while `RecommendationWebServer` provides
  the enhanced local browser-based rating workflow.
- `src/main/scripts/package-step-five.ps1` builds a minimal Duke upload zip from
  Java 8-compatible class files, using `src/main/submission/RecommendationRunner.java`
  for the safer submission variant.
