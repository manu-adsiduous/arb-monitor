# Environment Configuration Guide

## Required Environment Variables

Create a `.env` file in the project root with the following variables:

```bash
# OpenAI API Configuration
OPENAI_API_KEY=your-openai-api-key-here

# Apify API Configuration  
APIFY_TOKEN=your-apify-token-here
APIFY_ORG_ID=your-apify-org-id-here

# Database Configuration (Optional - defaults to H2)
DATABASE_URL=jdbc:h2:file:./data/arbmonitor
DATABASE_USERNAME=sa
DATABASE_PASSWORD=password

# Media Storage Path (Optional)
MEDIA_STORAGE_PATH=./media
```

## Getting API Keys

### OpenAI API Key
1. Go to [OpenAI Platform](https://platform.openai.com/)
2. Sign up/Login to your account
3. Navigate to API Keys section
4. Create a new API key
5. Copy the key and add it to your `.env` file

### Apify API Token
1. Go to [Apify Console](https://console.apify.com/)
2. Sign up/Login to your account  
3. Navigate to Settings → Integrations → API tokens
4. Create a new token
5. Copy the token and organization ID
6. Add both to your `.env` file

## Development Setup

1. **Copy environment template**:
   ```bash
   cp .env.example .env
   ```

2. **Fill in your API keys** in the `.env` file

3. **Start the application**:
   ```bash
   # Backend
   cd backend && ./mvnw spring-boot:run
   
   # Frontend  
   cd frontend && npm run dev
   ```

## Production Deployment

For production, set these environment variables in your deployment platform:
- AWS: Use Parameter Store or Secrets Manager
- Heroku: Use Config Vars
- Docker: Use environment variables or secrets

## Security Notes

- ⚠️ **Never commit API keys to version control**
- ✅ Use environment variables for all sensitive data
- ✅ The `.env` file is already in `.gitignore`
- ✅ Use different API keys for development and production
