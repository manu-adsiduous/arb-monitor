# Arb Monitor 🎯

> **Production-Ready Ad Compliance Monitoring System**  
> Ensure your Meta ads comply with Google AFS & RSOC terms

[![CI/CD](https://github.com/your-username/arb-monitor/workflows/CI/CD%20Pipeline/badge.svg)](https://github.com/your-username/arb-monitor/actions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![AWS](https://img.shields.io/badge/AWS-Ready-orange.svg)](infrastructure/)

Arb Monitor is a comprehensive SaaS platform that helps businesses ensure their Meta advertisements comply with Google AdSense for Search (AFS) and Revenue Share Optimization Compliance (RSOC) terms. Get detailed compliance analysis, automated scoring, and shareable reports for $50 per domain.

## ✨ **Current Application State**

### **🎯 Fully Implemented Features**
- ✅ **Professional Landing Page**: Complete with pricing, features, testimonials
- ✅ **Authentication System**: NextAuth.js with demo credentials
- ✅ **Glass Morphism Dashboard**: Apple iCloud-inspired design with liquid glass UI
- ✅ **Domain Management**: Add, view, manage domains with real-time processing status
- ✅ **Ad Scraping Integration**: Apify API with intelligent mock data fallback
- ✅ **Notifications System**: Complete CRUD operations with dropdown UI
- ✅ **Cross-tab Synchronization**: Real-time status updates across browser tabs
- ✅ **Media Storage & Serving**: Local file storage with HTTP serving endpoints
- ✅ **Responsive Design**: Mobile-first approach with dark/light mode
- ✅ **Database Persistence**: File-based H2 database (domains persist between restarts)
- ✅ **Production Infrastructure**: Docker, CI/CD, Terraform, AWS-ready

### **📊 Demo Data**
- **5 Active Domains**: livelocally.net, needtheinfo.com, dealsbysearch.com, bestoptions.net, testanimation.com
- **25 Mock Ads**: 5 realistic ads per domain with high-quality Unsplash images
- **Complete User Flow**: From landing page → authentication → domain management → ad analysis

### **🔐 Demo Access**
- **URL**: `http://localhost:3000`
- **Email**: `demo@arbmonitor.com`
- **Password**: `demo123`

## 🚀 **Quick Start**

### **1. Development Setup**
```bash
# Clone repository
git clone https://github.com/your-username/arb-monitor.git
cd arb-monitor

# Start backend (Terminal 1)
cd backend && ./mvnw spring-boot:run

# Start frontend (Terminal 2)
cd frontend && npm install && npm run dev

# Open browser
open http://localhost:3000
```

### **2. Docker Setup**
```bash
# Start all services with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## 🏗️ **Production Architecture**

### **Frontend Stack**
- **Next.js 15** with React 18 and TypeScript
- **Tailwind CSS** with custom glass morphism design system
- **Framer Motion** for smooth animations
- **Shadcn/ui** components with Radix UI primitives
- **NextAuth.js** for authentication
- **Sonner** for toast notifications

### **Backend Stack**
- **Spring Boot 3.2** with Java 17
- **Spring Security** with JWT authentication
- **Spring Data JPA** with Hibernate
- **H2 Database** (dev) → **PostgreSQL** (prod)
- **Spring Boot Actuator** for monitoring
- **Maven** for dependency management

### **AWS Production Infrastructure**
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   CloudFront    │    │  Application     │    │   ECS Fargate   │
│   + S3 Static   │────│  Load Balancer   │────│   Backend API   │
│   Assets        │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                       ┌──────────────────┐    ┌─────────────────┐
                       │   ElastiCache    │    │   RDS Multi-AZ  │
                       │   Redis          │    │   PostgreSQL    │
                       └──────────────────┘    └─────────────────┘
```

### **Estimated AWS Costs**
- **Development**: ~$30/month
- **Production**: ~$114/month
- **Scale (1000+ users)**: ~$500/month

## 📁 **Project Structure**

```
arb-monitor/
├── 🎨 frontend/                 # Next.js Application
│   ├── src/app/                # App Router pages
│   ├── src/components/         # Reusable components
│   ├── src/hooks/             # Custom React hooks
│   ├── src/lib/               # Utilities and API client
│   ├── Dockerfile             # Frontend containerization
│   └── package.json           # Dependencies
│
├── ⚙️  backend/                 # Spring Boot API
│   ├── src/main/java/         # Java source code
│   ├── src/main/resources/    # Configuration files
│   ├── Dockerfile             # Backend containerization
│   └── pom.xml               # Maven configuration
│
├── 🏗️ infrastructure/          # AWS Infrastructure
│   └── terraform/            # Infrastructure as Code
│       ├── main.tf           # Main configuration
│       ├── variables.tf      # Input variables
│       └── modules/          # Reusable modules
│
├── 🚀 .github/workflows/       # CI/CD Pipeline
│   └── ci-cd.yml            # GitHub Actions
│
├── 📋 logs/                    # Development logs
├── 📚 docs/                    # Documentation
├── 🐳 docker-compose.yml       # Local development
└── 📖 PRODUCTION_ROADMAP.md    # Complete roadmap
```

## 🔧 **Development Commands**

### **Backend**
```bash
cd backend
./mvnw spring-boot:run          # Start development server
./mvnw test                     # Run tests
./mvnw clean package           # Build JAR
./mvnw spring-boot:build-image # Build Docker image
```

### **Frontend**
```bash
cd frontend
npm run dev                    # Start development server
npm run build                  # Production build
npm run lint                   # ESLint check
npm run type-check            # TypeScript check
```

### **Infrastructure**
```bash
cd infrastructure/terraform
terraform init                 # Initialize Terraform
terraform plan                 # Preview changes
terraform apply               # Deploy infrastructure
terraform destroy             # Clean up resources
```

## 🚢 **Deployment**

### **1. GitHub Repository Setup**
```bash
# Run the automated setup script
./setup-github.sh

# Or manually:
gh repo create arb-monitor --private
git remote add origin https://github.com/username/arb-monitor.git
git push -u origin main
```

### **2. AWS Infrastructure**
```bash
# Set up Terraform backend
aws s3 mb s3://arb-monitor-terraform-state

# Deploy infrastructure
cd infrastructure/terraform
terraform init
terraform apply
```

### **3. Environment Variables**
Set up GitHub Secrets for CI/CD:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `APIFY_TOKEN`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `STRIPE_SECRET_KEY`
- `NEXTAUTH_SECRET`

## 💳 **Payment Integration (Stripe)**

### **Pricing Model**
- **One-time Payment**: $50 per domain analysis
- **Subscription** (planned): $30/month per domain
- **Enterprise** (planned): Custom pricing for agencies

### **Implementation Status**
- [ ] Stripe Dashboard setup
- [ ] Payment flow integration
- [ ] Webhook handling
- [ ] User provisioning
- [ ] Billing dashboard

## 🔮 **Roadmap & Missing Features**

### **Phase 1: Core Completion (2-4 weeks)**
- [ ] **Header Search Fix**: Complete the 99% finished search functionality
- [ ] **Compliance Engine**: Implement rule-based ad analysis
- [ ] **Real Ad Scraping**: Meta Ad Library API integration
- [ ] **Admin Dashboard**: User and rule management

### **Phase 2: Production Launch (4-6 weeks)**
- [ ] **Stripe Integration**: Payment processing
- [ ] **AWS Deployment**: Production infrastructure
- [ ] **User Authentication**: Replace demo auth with real accounts
- [ ] **Monitoring & Alerts**: CloudWatch, error tracking

### **Phase 3: Growth Features (6-12 weeks)**
- [ ] **OCR Analysis**: AWS Textract for image text extraction
- [ ] **Landing Page Analysis**: Automated claim verification
- [ ] **API Access**: Programmatic access for agencies
- [ ] **White-label Solution**: Custom branding options

## 🛠️ **API Documentation**

### **Authentication**
```bash
# Demo authentication
POST /api/auth/signin
{
  "email": "demo@arbmonitor.com",
  "password": "demo123"
}
```

### **Domain Management**
```bash
# Add domain
POST /api/domains
Header: X-User-ID: 1
{
  "domainName": "example.com"
}

# Get domains
GET /api/domains?page=0&size=20
Header: X-User-ID: 1

# Get domain ads
GET /api/ads/domain/example.com
Header: X-User-ID: 1
```

### **Ad Operations**
```bash
# Refresh ads
POST /api/ads/refresh/example.com
Header: X-User-ID: 1

# Create mock ads (development)
POST /api/ads/mock/example.com
Header: X-User-ID: 1
```

## 🔍 **Monitoring & Health Checks**

### **Application Health**
- Backend: `http://localhost:8080/actuator/health`
- Frontend: `http://localhost:3000`
- Database: H2 Console at `http://localhost:8080/h2-console`

### **Logs & Debugging**
- Backend logs: `./backend/*.log`
- Frontend logs: Browser console
- Database file: `./backend/data/arbmonitor.mv.db`

## 🤝 **Contributing**

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 **Support**

- **Documentation**: See `/docs` folder
- **Issues**: GitHub Issues
- **Email**: support@arbmonitor.com (planned)

---

**Ready to launch your ad compliance monitoring SaaS? 🚀**

Follow the [PRODUCTION_ROADMAP.md](PRODUCTION_ROADMAP.md) for complete deployment instructions.
   npm install
   npm run dev
   ```

3. **Backend Setup**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

4. **Environment Variables**
   Copy `.env.example` to `.env` and fill in your API keys:
   - Meta Ad Library API credentials
   - AWS credentials
   - Stripe API keys

## 🔧 Configuration

### Meta Ad Library API Setup
1. Create a Meta Developer account
2. Create a new app and get access token
3. Complete identity verification process
4. Add credentials to environment variables

### AWS Setup
1. Configure AWS CLI with your credentials
2. Deploy infrastructure using CloudFormation
3. Update environment variables with AWS resource ARNs

## 📊 Compliance Rules

The system includes comprehensive Google AFS/RSOC compliance rules:

- **Creative Content Guidelines**
- **Landing Page Requirements**
- **Claim Substantiation Rules**
- **Referrer Parameter Validation**
- **Image Text Analysis**

## 🚀 Deployment

### Production Deployment
```bash
# Deploy infrastructure
cd infrastructure
terraform apply

# Deploy frontend
cd ../frontend
npm run build
npm run deploy

# Deploy backend
cd ../backend
./mvnw clean package
docker build -t arb-monitor-api .
# Push to ECR and deploy to ECS
```

## 💰 Pricing

- **$50 per domain** monitored
- Unlimited compliance checks
- Shareable reports included
- 24/7 monitoring

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support, email support@arbmonitor.com or create an issue in this repository.

---

Built with ❤️ for ad compliance professionals
