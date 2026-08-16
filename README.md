# 🌐 Polyglot Chat

A real-time chat application that breaks language barriers — every user picks their preferred language, and messages are automatically translated for them, with the option to view the original text at any time.

Built as a full-stack project to explore real-time messaging architecture, third-party API integration, and authentication systems at a production-lite scale.

---

## ✨ Features

- **Real-time messaging** via WebSocket (STOMP over SockJS) — no polling, no refresh needed
- **Automatic translation** of every message into each recipient's preferred language, with a toggle to view the original text
- **Multiple sign-in methods** — Google OAuth and phone number OTP, powered by Firebase Authentication
- **JWT-based session management** on the backend, decoupled from Firebase after initial login
- **Direct and group chat rooms**, with per-room participant language tracking
- **Profile management** — display name, username, preferred language, theme, and profile picture (uploaded via Cloudinary)
- **Graceful translation fallback** — if the translation service is temporarily unavailable, the original message is still delivered rather than lost
- **Dark/light theme support**

---

## 🛠️ Tech Stack

**Frontend**
- React (Vite)
- Tailwind CSS
- Framer Motion (animations)
- STOMP.js + SockJS (WebSocket client)
- Firebase Authentication SDK

**Backend**
- Spring Boot (Java)
- Spring WebSocket (STOMP messaging)
- Spring Security + JWT
- MongoDB (via Spring Data MongoDB)
- Firebase Admin SDK (token verification)
- Cloudinary (image storage)
- WebClient (reactive HTTP client for the translation service)

**Translation**
- Third-party translation model hosted on Hugging Face Spaces (Gradio-based REST API)

**Database**
- MongoDB

---

## 🏗️ Architecture Overview

```
┌─────────────┐        WebSocket/REST        ┌──────────────────┐
│   React     │ ────────────────────────────▶│   Spring Boot     │
│  Frontend   │◀──────────────────────────── │    Backend        │
└─────────────┘                               └────────┬─────────┘
      │                                                 │
      │ Firebase Auth (Google / Phone OTP)              │
      ▼                                                 ▼
┌─────────────┐                              ┌──────────────────┐
│  Firebase   │                              │     MongoDB       │
│    Auth     │                              │   (chat data)     │
└─────────────┘                              └──────────────────┘
                                                         │
                                                         ▼
                                              ┌──────────────────┐
                                              │  Translation API  │
                                              │ (Hugging Face     │
                                              │  Space, Gradio)   │
                                              └──────────────────┘
```

**Message flow:**
1. User sends a message via WebSocket to `/app/chat.sendMessage`
2. Backend identifies all participant languages in the room
3. Backend requests translations (in parallel) for each required target language
4. Message — original text plus all translations — is persisted to MongoDB
5. Backend broadcasts the enriched message to `/topic/room/{roomId}`
6. All connected clients in that room receive the update in real time, each rendering the message in their own preferred language

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Node.js 18+ and npm
- MongoDB (local instance or MongoDB Atlas)
- A Firebase project (Authentication enabled — Google + Phone providers)
- A Cloudinary account (for profile image uploads)
- A Hugging Face account (for translation API access)

### Backend Setup

```bash
# Clone the repository
git clone https://github.com/SonuSk584/POLYGLOT.git
cd POLYGLOT

# Copy the example config and fill in your own values
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

Edit `src/main/resources/application.properties` with your own credentials:

```properties
spring.data.mongodb.uri=your_mongodb_connection_string

jwt.secret=your_jwt_secret

cloudinary.cloud-name=your_cloudinary_cloud_name
cloudinary.api-key=your_cloudinary_api_key
cloudinary.api-secret=your_cloudinary_api_secret

translation.use.mock=false
gradio.api.baseUrl=https://helsinki-nlp-opus-translate.hf.space
huggingface.api.token=your_huggingface_token
```

You'll also need a Firebase service account file:
1. Firebase Console → Project Settings → Service Accounts → Generate new private key
2. Save it as `src/main/resources/firebase-service-account.json`

Then run the backend:

```bash
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`.

### Frontend Setup

```bash
cd frontend
npm install
```

Create a `.env` file in `frontend/`:

```
VITE_FIREBASE_API_KEY=your_firebase_api_key
VITE_FIREBASE_AUTH_DOMAIN=your_project.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=your_project_id
VITE_FIREBASE_STORAGE_BUCKET=your_project.appspot.com
VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
VITE_FIREBASE_APP_ID=your_app_id
```

Then run the frontend:

```bash
npm run dev
```

The frontend starts on `http://localhost:5173` (default Vite port).

---

## 📁 Project Structure

```
POLYGLOT/
├── src/main/java/com/polyglot/chat/
│   ├── config/         # Security, WebSocket, Cloudinary, Firebase config
│   ├── controller/     # REST + WebSocket controllers
│   ├── dto/             # Data transfer objects
│   ├── model/           # MongoDB document models
│   ├── repository/     # Spring Data repositories
│   ├── security/        # JWT filters, auth entry points
│   └── service/          # Business logic (auth, chat, translation)
├── src/main/resources/
│   └── application-example.properties
└── frontend/
    ├── src/
    │   ├── components/  # Chat, language, layout, UI components
    │   ├── contexts/     # Auth and chat React contexts
    │   ├── pages/          # Route-level pages
    │   └── services/      # API and WebSocket clients
    └── ...
```

---

## 🔐 Security Notes

- All secrets are loaded via environment-specific config files (`application.properties`, `.env`) that are **excluded from version control**
- `application-example.properties` documents required configuration without exposing real values
- JWT tokens are used for session management after initial Firebase authentication, keeping the backend decoupled from Firebase for subsequent requests

---

## 🗺️ Roadmap

- [ ] Redis caching layer for translations (reduce repeated API calls, improve latency)
- [ ] Message read receipts and typing indicators
- [ ] Horizontal scaling support for WebSocket connections (Redis pub/sub backplane)
- [ ] Automated CI/CD pipeline via GitHub Actions
- [ ] Dockerized deployment

---

## 📄 License

This project is available for educational and portfolio purposes.

---

## 👤 Author

**Sonu Kumar**
GitHub: [@SonuSk584](https://github.com/SonuSk584)