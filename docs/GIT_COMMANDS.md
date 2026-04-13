# Manual Git Commands to Fix .gitignore Issue

## The Problem
`dist-with-jvm/` folder shows in git changes despite being in `.gitignore`

## The Solution
Run these commands in your terminal (Git Bash, Command Prompt, or PowerShell):

```bash
# Navigate to project directory
cd C:\Users\ashis\IdeaProjects\db-explorer

# Check what's currently tracked
git status

# Remove dist-with-jvm from git tracking (keep files in working directory)
git rm --cached -r dist-with-jvm/

# Verify the removal
git status

# Commit the change
git commit -m "Remove dist-with-jvm from git tracking"

# Verify it's fixed
git status
```

## What This Does
- `git rm --cached` removes files from git's index but keeps them in your working directory
- The `-r` flag removes directories recursively
- Files stay on disk but are no longer tracked by git
- Future builds won't track these files

## Alternative: Use the Batch Script
If manual commands don't work, run:
```batch
fix_gitignore.bat
```

## Result
After running these commands:
- ✅ `dist-with-jvm/` files remain in your working directory
- ✅ No longer tracked by git
- ✅ Clean git status
- ✅ `.gitignore` works for future builds

---
*Quick fix for .gitignore tracking issue*
