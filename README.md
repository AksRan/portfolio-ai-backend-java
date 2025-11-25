📈 Portfolio AI — Backend (Java Spring Boot)
Portfolio AI is an intelligent investment‑recommendation backend built with Java Spring Boot.
It powers the Portfolio AI website by calculating:

User risk scores

Recommended portfolio tiers

Optimized ETF allocations

Constraint‑based equity/bond balancing

Interpretations and explanations for the final portfolio

This backend provides the API endpoints consumed by the Wix‑based front‑end.

🚀 Live Frontend (Test Site)
🔗 Wix Test Site:
Add your test link here

🔗 Backend API (Render):
Add your Render service URL here (e.g., https://portfolio-ai.onrender.com)

🧠 What the Backend Does
The backend processes input from the user:

risk: "low" | "medium" | "high"
timeFrame: "1-3y" | "3-5y" | "5+y"
growthGoal: "income" | "balanced" | "growth"
action: "buy" | "hold" | "sell"
It then:

Computes a risk score

Maps that score to a portfolio tier (Conservative, Balanced, Aggressive)

Applies constraints (equity/bond minimums and maximums)

Generates ETF allocations (VOO, VXUS, BND)

Returns a human‑readable explanation

All responses are returned in JSON for the Wix frontend.

📡 API Endpoints
POST /api/portfolio/recommend
Generates a full portfolio recommendation.

Sample Request
{
  "risk": "high",
  "timeFrame": "1-3y",
  "growthGoal": "growth",
  "action": "buy"
}
Sample Response
{
  "riskTier": "BALANCED",
  "riskScore": 4,
  "constraints": {
    "eq_min": 0.55,
    "eq_max": 0.75,
    "bond_min": 0.20
  },
  "weights": {
    "VOO": 0.345,
    "VXUS": 0.335,
    "BND": 0.320
  },
  "explanation": "Portfolio leans 68% equities and 32% bonds to match your risk appetite and horizon."
}
🛠 Tech Stack
Java 17

Spring Boot

Maven

Render (free hosting)

REST API

📂 Project Structure
src/main/java/com.portfolioai
├── controllers       # REST endpoints
├── services          # risk logic, optimizers
├── models            # request/response DTOs
├── optimizers        # portfolio allocation logic
└── PortfolioAiApplication.java
💻 Run Locally
1. Clone the repo
git clone https://github.com/AksRan/portfolio-ai-backend-java.git
2. Build
mvn clean install
3. Run
mvn spring-boot:run
4. Test the API
Visit:
http://localhost:8080/api/portfolio/recommend

Use Thunder Client, Postman, or curl.

🌐 Deployment (Render)
This backend is deployed using Render’s free tier.

Render automatically detects:

Java version

Maven build

Spring Boot application entry point

Deployment steps followed:

Pushed repo to GitHub

Connected Render → New Web Service

Build command:

mvn clean install
Start command:

java -jar target/portfolio-ai.jar
Exposed port: 8080

🧪 Wix Frontend Integration
The Wix website calls this backend using:

fetch("https://your-render-backend-url/api/portfolio/recommend", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(requestBody)
})
The returned JSON is rendered directly into the UI.

📬 Contact
AksRan
Developer — Portfolio AI
GitHub: https://github.com/AksRan

