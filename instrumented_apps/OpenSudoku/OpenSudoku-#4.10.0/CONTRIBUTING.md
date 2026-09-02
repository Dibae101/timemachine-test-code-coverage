# Contributing

Open Sudoku development is a community project, and contributions are
welcomed.

First, see if your issue haven’t been yet reported
[here](https://gitlab.com/opensudoku/opensudoku/issues), then, please, first
discuss the change you wish to make via [a new
issue](https://gitlab.com/opensudoku/opensudoku/issues/new).

## Contributing Translations

Interested in help to translate Open Sudoku? You can contribute in our
[Weblate team](https://hosted.weblate.org/projects/opensudoku/).

## Contributing Code

By default, merge requests should be opened against the **develop** branch
from your own branch, MR against the **master** branch should only be used
for critical bug fixes.

### Here are a few guidelines you should follow before submitting:

1. **License Acceptance:** All contributions must be licensed as [GNU
   GPLv3](LICENSE) to be accepted. Use `git commit --signoff` to acknowledge
   this.
2. **No Breakage**: New features or changes to existing ones must not
   degrade the user experience.
3. **Coding standards**: best-practices should be followed, comment
   generously, and avoid "clever" algorithms. Refactoring existing messes is
   great, but watch out for breakage.
4. **No large PR**: Try to limit the scope of PR only to the related issue,
   so it will be easier to review and test.
5. **Make your own branch**: When you send us a merge request, please do it
   from your own branch. Avoid using the `develop` branch.

### Merge request process

On each merge request GitLab runs a number of checks to make sure there are
no problems.

Take special note of point five of the previous paragraph. Do not use the
`develop` branch, make your own. Not following this step will cause your
merge request to be rejected without even checking it.

#### Signed commits

Commits must be signed. [See here how to set it
up](https://docs.gitlab.com/ee/user/project/repository/gpg_signed_commits/).

## Tips

### Keep your fork up to date

To keep your fork of the repository up to date

1. Add the official repository as a remote (e.g., called `upstream`)

```shell
git remote add upstream git@gitlab.com:opensudoku/opensudoku.git
```

1. Periodically pull that in to your forked copy of the `develop` branch.

```shell
git pull upstream develop
```
