# TinyBrain version split script
Set-Location "E:\1\tinybrain"

Write-Host "=== TinyBrain Version Setup ===" -ForegroundColor Cyan

Write-Host "[1/4] Committing on v1-handcrafted..."
git add -A
git commit -m "chore: v1-handcrafted baseline - hand-written AI layer"

Write-Host "[2/4] Switching to master..."
git checkout master

Write-Host "[3/4] Creating v2-spring-ai-alibaba from master..."
git checkout -b v2-spring-ai-alibaba

Write-Host "[4/4] Branches:" -ForegroundColor Yellow
git branch -a

Write-Host "=== Done! ===" -ForegroundColor Green
