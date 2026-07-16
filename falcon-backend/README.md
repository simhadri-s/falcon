
# Falcon E-Commerce Platform

This repository contains the backend and setup instructions for the **Falcon** E-Commerce platform. The application is a full-stack system consisting of a Spring Boot (Java 21) backend, a MongoDB database, and a React + Vite frontend dashboard.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Backend Setup (Spring Boot)](#backend-setup-spring-boot)
    - [1. Environment Variables / Properties](#1-environment-variables--properties)
    - [2. Google Service Account (Firebase Admin SDK)](#2-google-service-account-firebase-admin-sdk)
    - [3. Building and Running](#3-building-and-running)
3. [Frontend Setup (React + Vite)](#frontend-setup-react--vite)
    - [1. Environment Variables](#1-environment-variables)
    - [2. Building and Running](#2-building-and-running-1)
4. [Mobile App Setup (Flutter)](#mobile-app-setup-flutter)
    - [1. Environment Variables](#1-environment-variables-1)
    - [2. Building and Running](#2-building-and-running-2)
5. [External Services Configuration](#external-services-configuration)
6. [Production Deployment Guide](#production-deployment-guide)

---

## Prerequisites

Ensure you have the following installed on your local machine:
- **Java Development Kit (JDK) 21**
- **Maven** (3.8+)
- **Node.js** (v18+) & **npm** / **yarn**
- **Flutter SDK** (for the mobile application)
- **MongoDB** (Local instance or MongoDB Atlas cluster)

---

## Backend Setup (Spring Boot)

The backend is located in the `falcon` directory.

### 1. Environment Variables / Properties

The backend uses an `application.properties` file located at `src/main/resources/application.properties`. You must configure the following properties with your own credentials before running the application:

```properties
spring.application.name=falcon
server.port=8080

# --- MongoDB Configuration ---
# Replace with your local or Atlas MongoDB URI
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster-url>/falcon
spring.data.mongodb.auto-index-creation=true

# --- JWT Security ---
# Secret key for signing JWTs (keep this secure in production)
app.jwtSecret=your_very_long_secure_jwt_secret_key_here
# Token expiration time in milliseconds (e.g., 604800000 for 7 days)
app.jwtExpirationMs=604800000

# --- Cloudinary (Image/PDF Storage) ---
# Sign up at cloudinary.com to get these credentials
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret

# --- File Upload Limits ---
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

# --- Email (SMTP) Configuration ---
# If using Gmail, you must generate an "App Password"
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# --- Google OAuth2 Authentication ---
# Get these from Google Cloud Console -> APIs & Services -> Credentials
spring.security.oauth2.client.registration.google.client-id=your_google_client_id
spring.security.oauth2.client.registration.google.client-secret=your_google_client_secret
spring.security.oauth2.client.registration.google.scope=profile,email

# --- WhatsApp Cloud API Configuration ---
whatsapp.access.token=your_whatsapp_access_token
whatsapp.phone.number.id=your_whatsapp_phone_number_id
```

> **Security Note:** Never commit production secrets or passwords to version control. In a production environment, use environment variables (e.g. `${MONGO_URI}`) instead of hardcoding them in `application.properties`.

### 2. Google Service Account (Firebase Admin SDK)

The backend uses Firebase (FCM) for push notifications. You must provide a Google Service Account JSON key.

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Open your project settings > **Service accounts**.
3. Click **Generate new private key**. This will download a JSON file.
4. Rename the downloaded file to `service-account.json`.
5. Place the file inside the backend resources folder at the following exact path:
   `src/main/resources/credintials/service-account.json`

*(Note: The folder name `credintials` is deliberately spelled as it appears in the codebase's `FirebaseConfig.java` resource loader).*

### 3. Building and Running

1. Open a terminal in the backend root directory (`falcon`).
2. Run the following Maven command to compile and verify the project:
   ```bash
   mvn clean compile
   ```
3. To start the Spring Boot server, run:
   ```bash
   mvn spring-boot:run
   ```
4. The API will be accessible at `http://localhost:8080`.

---

## Frontend Setup (React + Vite)

The frontend is located in the `falcon-frontend` directory. Navigate to the specific admin dashboard (`falcon-frontend/falcon-admin`) to begin.

### 1. Environment Variables

Create a `.env` file in the root of the frontend directory (`falcon-frontend/falcon-admin/.env`) and add the API URL pointing to your Spring Boot backend:

```env
VITE_API_URL=http://localhost:8080
```

### 2. Building and Running

1. Open a terminal in the `falcon-frontend/falcon-admin` directory.
2. Install the necessary NPM dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
4. The admin dashboard will typically be accessible at `http://localhost:5173`.

---

## Mobile App Setup (Flutter)

The customer-facing mobile application is built using Flutter and is located in the `falcon-app/flutter_application` directory.

### 1. Environment Variables

Create a `.env` file in the root of the Flutter project (`falcon-app/flutter_application/.env`).

```env
# Replace with your local machine's IP address (e.g. 192.168.1.x) or your production API URL.
# Note: Using 'localhost' will NOT work on physical Android/iOS devices!
HOST_URL=http://192.168.1.77:8080
```

### 2. Building and Running

1. Open a terminal in the `falcon-app/flutter_application` directory.
2. Fetch the Flutter dependencies:
   ```bash
   flutter pub get
   ```
3. Connect a physical device or launch an Android/iOS emulator.
4. Run the application:
   ```bash
   flutter run
   ```

---

## External Services Configuration

To fully run this application, you must set up the following third-party integrations:

1. **MongoDB Atlas**: Create a cluster, whitelist your IP address, and copy the connection string into the `application.properties`.
2. **Cloudinary**: Create a free account. Navigate to your dashboard to find your Cloud Name, API Key, and API Secret for media uploads.
3. **Google Cloud Console**: Set up an OAuth Consent Screen and generate Web Application Credentials for Google Login. Add `http://localhost:8080/login/oauth2/code/google` to your authorized redirect URIs.
4. **Firebase Admin SDK (Service Account)**: Create a Firebase project and generate a Service Account private key JSON file to enable push notifications.
5. **Meta Developer (WhatsApp API)**: Set up a WhatsApp Business App to get the Access Token and Phone Number ID for system notifications.
6. **Google App Passwords (SMTP)**: For sending system emails via Gmail, enable 2-Step Verification on your Google Account and generate an App Password to use as the `spring.mail.password`.

---

## Production Deployment Guide

When transitioning from local development to a live production environment, follow these best practices for building and deploying each component.

### 1. Deploying the Backend (Spring Boot)

For production, you should run the pre-compiled `.jar` file rather than running through Maven.

1. **Build the Production JAR:**
   In the root of the `falcon` backend directory, run:
   ```bash
   mvn clean package -DskipTests
   ```
   This generates an executable JAR file inside the `target/` directory (e.g., `falcon-0.0.1-SNAPSHOT.jar`).

2. **Environment Configuration:**
   Do **not** hardcode credentials in `application.properties`. Instead, pass them as environment variables on your production server (AWS EC2, DigitalOcean, Heroku, etc.):
   ```bash
   export SPRING_DATA_MONGODB_URI="mongodb+srv://..."
   export APP_JWTSECRET="your_secure_random_string"
   export CLOUDINARY_API_KEY="..."
   # ... export other necessary variables
   ```

3. **Run the Application:**
   Execute the JAR file:
   ```bash
   java -jar target/falcon-0.0.1-SNAPSHOT.jar
   ```
   *(Note: In a real production setup, use `systemd` or Docker to keep the process running continuously).*

### 2. Deploying the Frontend (React + Vite)

The frontend should be compiled into static HTML/JS/CSS files, which can be served by any static web host (Vercel, Netlify, Nginx, AWS S3).

1. **Set Production Environment Variables:**
   Update your `.env` file or CI/CD pipeline to point to your live backend domain (ensure it uses HTTPS):
   ```env
   VITE_API_URL=https://api.yourdomain.com
   ```

2. **Build the Static Assets:**
   In the `falcon-frontend/falcon-admin` directory, run:
   ```bash
   npm run build
   ```
   This creates a `dist/` folder containing the highly optimized production build.

3. **Hosting:**
   Upload the contents of the `dist/` folder to your web server (e.g., configure Nginx to serve this folder) or push the repository to platforms like **Vercel** or **Netlify** which automate this process.

### 3. Deploying the Mobile App (Flutter)

To release the mobile application to the Google Play Store or Apple App Store:

1. **Set Production Variables:**
   Update the `falcon-app/flutter_application/.env` file to point to your live production API:
   ```env
   HOST_URL=https://api.yourdomain.com
   ```

2. **Build for Android:**
   To build an Android App Bundle (AAB) for the Play Store:
   ```bash
   flutter build appbundle
   ```
   To build an APK for direct distribution:
   ```bash
   flutter build apk --release
   ```

3. **Build for iOS:**
   *(Requires a Mac with Xcode installed)*
   ```bash
   flutter build ipa
   ```
   This generates an Xcode archive that you can upload to App Store Connect via Transporter or Xcode.