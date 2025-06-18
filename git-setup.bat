@echo off
echo Setting up Git repository...
git remote remove origin
git remote add origin https://github.com/YounesMoudaoui/web4jobsbackendj.git
git add .
git commit -m "Initial commit"
git push -u origin master
echo Done!
pause 