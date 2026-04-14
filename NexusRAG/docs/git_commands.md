# Git Push Commands

To push changes to GitHub using your specific SSH key, use the following commands:

## 1. Configure User Identity (First time)
Run these so your commits show your name:
```bash
git config --global user.name "AnhLM027"
git config --global user.email "boy14acf@gmail.com"
```

## 2. Commit Changes
```bash
git add .
git commit -m "Your commit message"
```

## 2. Push to GitHub
Using the specific SSH key `id_anh_lm027_github`:

```bash
GIT_SSH_COMMAND="ssh -i ~/.ssh/id_anh_lm027_github" git push origin main
```

## 3. SSH Configuration (Optional)
If you have configured `~/.ssh/config` as follows, you can just run `git push`:

```text
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_anh_lm027_github
```

Then simply:
```bash
git push origin main
```
