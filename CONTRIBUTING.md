# Log Store

## Advice on pull requests
Pull requests are the easiest way to contribute changes to git repos at GitHub. They are the preferred contribution method, as they offer a nice way for commenting and amending the proposed changes.

You need a local "fork" of the GitHub repo.

Use a "feature branch" for your changes. That separates the changes in the pull request from your other changes and makes it easy to edit/amend commits in the pull request. Workflow using "feature_x" as the example:

Update your local git fork to the tip (of the master, usually)
Create the feature branch with git checkout -b feature_x
Edit changes and commit them locally
Push them to your GitHub fork by git push -u origin feature_x. That creates the "feature_x" branch at your GitHub fork and sets it as the remote of this branch
When you now visit GitHub, you should see a proposal to create a pull request
If you later need to add new commits to the pull request, you can simply commit the changes to the local branch and then use git push to automatically update the pull request.

If you need to change something in the existing pull request (e.g. to add a missing signed-off-by line to the commit message), you can use git push -f to overwrite the original commits. That is easy and safe when using a feature branch. Example workflow:

## Checkout the feature branch by git checkout feature_x
Edit changes and commit them locally. If you are just updating the commit message in the last commit, you can use git commit --amend to do that
If you added several new commits or made other changes that require cleaning up, you can use git rebase -i HEAD~X (X = number of commits to edit) to possibly squash some commits
Push the changed commits to GitHub with git push -f to overwrite the original commits in the "feature_x" branch with the new ones. The pull request gets automatically updated
If you have commit access
Do NOT use git push --force.
Do NOT commit to other maintainer's packages without their consent.
Use Pull Requests if you are unsure and to suggest changes to other maintainers.
Gaining commit access
We will gladly grant commit access to responsible contributors who have made useful pull requests and/or feedback or patches to this repository in general. Please include your request for commit access in your next pull request or ticket.
