# GitHub Tweet Search

Search tweets for GitHub repositories using CLI.

## Usage

    sbt run [search string]
    
1. `search string` will be used to query GitHub repositories.
2. For each repository found will be collected the most recent tweets.
3. A result summary will be saved as JSON inside the `output/` folder.
