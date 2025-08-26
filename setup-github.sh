#!/bin/bash

# Arb Monitor - GitHub Repository Setup Script
# This script initializes the git repository and pushes to GitHub

set -e

echo "🚀 Setting up Arb Monitor GitHub Repository..."

# Check if GitHub CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) is not installed. Please install it first:"
    echo "   brew install gh (macOS)"
    echo "   https://cli.github.com/manual/installation (other platforms)"
    exit 1
fi

# Check if user is authenticated with GitHub CLI
if ! gh auth status &> /dev/null; then
    echo "🔐 Please authenticate with GitHub CLI first:"
    echo "   gh auth login"
    exit 1
fi

# Repository configuration
REPO_NAME="arb-monitor"
REPO_DESCRIPTION="Ad Compliance Monitoring System for Google AFS & RSOC Terms"

echo "📋 Repository Details:"
echo "   Name: $REPO_NAME"
echo "   Description: $REPO_DESCRIPTION"
echo "   Visibility: Private"
echo ""

read -p "Do you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Setup cancelled."
    exit 1
fi

# Initialize git repository if not already initialized
if [ ! -d ".git" ]; then
    echo "📦 Initializing git repository..."
    git init
else
    echo "📦 Git repository already initialized."
fi

# Create GitHub repository
echo "🏗️  Creating GitHub repository..."
if gh repo create $REPO_NAME --private --description "$REPO_DESCRIPTION" --source=. --remote=origin --push; then
    echo "✅ Repository created successfully!"
else
    echo "⚠️  Repository might already exist. Adding remote..."
    git remote add origin https://github.com/$(gh api user --jq .login)/$REPO_NAME.git 2>/dev/null || true
fi

# Add all files to git
echo "📁 Adding files to git..."
git add .

# Create initial commit
echo "💾 Creating initial commit..."
if git diff --cached --quiet; then
    echo "ℹ️  No changes to commit."
else
    git commit -m "Initial commit: Complete Arb Monitor application

Features included:
- ✅ Landing page with pricing and features
- ✅ Authentication system with NextAuth.js
- ✅ Dashboard with glass morphism design
- ✅ Domain management with real-time status
- ✅ Ad scraping integration (Apify + mock data)
- ✅ Notifications system with CRUD operations
- ✅ Cross-tab synchronization
- ✅ Responsive mobile-friendly UI
- ✅ Database persistence (H2 file-based)
- ✅ Media storage and serving
- ✅ Docker configuration for deployment
- ✅ GitHub Actions CI/CD pipeline
- ✅ Terraform infrastructure as code
- ✅ Production-ready configuration

Ready for:
- AWS deployment
- Stripe integration
- Compliance engine implementation
- Admin dashboard
- Production launch"
fi

# Push to GitHub
echo "🚀 Pushing to GitHub..."
git branch -M main
git push -u origin main

# Set up branch protection (optional)
echo "🛡️  Setting up branch protection..."
gh api repos/:owner/:repo/branches/main/protection \
  --method PUT \
  --field required_status_checks='{"strict":true,"contexts":["test-backend","test-frontend"]}' \
  --field enforce_admins=true \
  --field required_pull_request_reviews='{"required_approving_review_count":1}' \
  --field restrictions=null \
  2>/dev/null || echo "⚠️  Branch protection setup skipped (might require admin access)"

# Display repository information
echo ""
echo "🎉 GitHub repository setup complete!"
echo ""
echo "📊 Repository Information:"
gh repo view --web
echo ""
echo "🔗 Repository URL: https://github.com/$(gh api user --jq .login)/$REPO_NAME"
echo ""
echo "📋 Next Steps:"
echo "1. Set up GitHub Secrets for CI/CD:"
echo "   - AWS_ACCESS_KEY_ID"
echo "   - AWS_SECRET_ACCESS_KEY"
echo "   - APIFY_TOKEN"
echo "   - DATABASE_PASSWORD"
echo "   - JWT_SECRET"
echo "   - STRIPE_SECRET_KEY"
echo "   - STRIPE_WEBHOOK_SECRET"
echo "   - NEXTAUTH_SECRET"
echo ""
echo "2. Review and customize:"
echo "   - README.md"
echo "   - Environment variables"
echo "   - Terraform variables"
echo ""
echo "3. Set up AWS infrastructure:"
echo "   cd infrastructure/terraform"
echo "   terraform init"
echo "   terraform plan"
echo ""
echo "✨ Happy coding!"
