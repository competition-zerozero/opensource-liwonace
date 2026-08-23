# Branch Protection Settings

GitHub repository admin must apply these settings in `Settings > Branches`.

## main

- Require a pull request before merging: enabled
- Required approvals: 1
- Dismiss stale pull request approvals when new commits are pushed: enabled
- Require status checks to pass before merging: enabled
- Required status check: `Backend Test and Build`
- Require branches to be up to date before merging: enabled
- Require conversation resolution before merging: enabled
- Do not allow bypassing the above settings: enabled
- Restrict who can push to matching branches: enabled, no direct push users
- Allow force pushes: disabled
- Allow deletions: disabled

## dev

- Require a pull request before merging: enabled
- Required approvals: 1
- Dismiss stale pull request approvals when new commits are pushed: enabled
- Require status checks to pass before merging: enabled
- Required status check: `Backend Test and Build`
- Require branches to be up to date before merging: enabled
- Require conversation resolution before merging: enabled
- Allow force pushes: disabled
- Allow deletions: disabled
