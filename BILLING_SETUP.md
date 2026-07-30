# Setup - Facturation avec Stripe

Ce guide explique comment configurer la facturation automatique avec Stripe pour notezen.

## Phase 1 : Configuration de Stripe

### 1. Créer un compte Stripe

1. Aller sur [stripe.com](https://stripe.com)
2. Créer un compte (gratuit, inclut 5 ans de test)
3. Dashboard → "Develop" → Copier les clés API test

### 2. Créer le produit et le prix

Dans le dashboard Stripe :

1. **Products** → **Add Product**
   - Name: "NoteZen Premium"
   - Description: "Monthly subscription for NoteZen"
   
2. **Add pricing**
   - Price: 20.00 EUR
   - Billing period: Monthly
   - Enable free trial: 14 days
   - Copier le `Price ID` (price_xxxxx)

### 3. Configurer les webhooks

1. **Developers** → **Webhooks** → **Add endpoint**
   - URL: `https://your-domain.com/api/billing/webhook` (remplacer par votre domaine)
   - Events à écouter:
     - `customer.subscription.created`
     - `customer.subscription.updated`
     - `customer.subscription.deleted`
     - `invoice.payment_succeeded`
     - `invoice.payment_failed`
   - Copier le **Signing secret** (whsec_xxxxx)

---

## Phase 2 : Configuration PostgreSQL (VPS Ubuntu 22.04)

### 1. Installation via SSH

```bash
ssh user@your-vps-ip
sudo apt update && sudo apt upgrade -y
sudo apt install postgresql postgresql-contrib -y
psql --version
```

### 2. Créer la base de données et l'utilisateur

```bash
sudo -u postgres psql
```

Dans le shell psql :

```sql
CREATE DATABASE notezen_db;
CREATE USER notezen_user WITH PASSWORD 'your_secure_password';
ALTER ROLE notezen_user SET client_encoding TO 'utf8';
ALTER ROLE notezen_user SET default_transaction_isolation TO 'read committed';
ALTER ROLE notezen_user SET timezone TO 'UTC';
GRANT ALL PRIVILEGES ON DATABASE notezen_db TO notezen_user;
\q
```

### 3. Vérifier l'accès

```bash
psql -U notezen_user -d notezen_db -c "SELECT now();"
# Devrait afficher l'heure actuelle
```

### 4. Configurer PostgreSQL pour accès local uniquement

```bash
sudo nano /etc/postgresql/15/main/postgresql.conf
# Chercher et vérifier:
# listen_addresses = 'localhost'  # ← Déjà par défaut

sudo systemctl restart postgresql
```

---

## Phase 3 : Configuration locale (.env)

### 1. Copier le fichier d'exemple

```bash
cp .env.example .env
```

### 2. Remplir les variables d'environnement

```env
# Database - Pour local (H2), laisser en commentaire
# SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/notezen_db
# SPRING_DATASOURCE_USERNAME=notezen_user
# SPRING_DATASOURCE_PASSWORD=...

# Stripe
STRIPE_API_KEY=sk_test_xxxxx          # Copié du dashboard
STRIPE_WEBHOOK_SECRET=whsec_xxxxx     # Copié des webhooks
STRIPE_PRICING_MONTHLY_PRICE_ID=price_xxxxx
STRIPE_PRICING_TRIAL_DAYS=14

# JWT (générer une clé sécurisée)
JWT_SECRET=votre_clé_secrète_très_longue_min_30_caractères

# Google OAuth (configurer dans Google Cloud Console)
GOOGLE_CLIENT_ID=...apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=...
GOOGLE_REDIRECT_URI=http://localhost:8080/api/google/callback
GOOGLE_AI_API_KEY=...

# App
APP_FRONT_URL=https://www.notezen.fr
APP_MOCK_USER_EMAIL=bchevriaut@gmail.com
```

### 3. Générer les secrets sécurisés

Pour JWT (Linux/Mac) :
```bash
openssl rand -base64 30
```

Pour Windows PowerShell :
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object {[byte](Get-Random -Maximum 256)}))
```

---

## Phase 4 : Lancer l'application

### Dev local (H2 in-memory)

```bash
mvn spring-boot:run
```

Accéder à : http://localhost:8080

### Production (PostgreSQL sur VPS)

#### Option A : JAR direct

```bash
# Compiler localement
mvn clean package

# Copier sur le VPS
scp target/notezen-0.0.1-SNAPSHOT.jar user@vps-ip:/opt/notezen/

# Sur le VPS
cd /opt/notezen
source .env
java -jar notezen-0.0.1-SNAPSHOT.jar
```

#### Option B : Docker Compose (recommandé)

```bash
# Créer Dockerfile
cat > Dockerfile << 'EOF'
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
CMD ["java", "-jar", "app.jar"]
EOF

# Sur le VPS, créer docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: notezen_db
      POSTGRES_USER: notezen_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

  app:
    build: .
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/notezen_db
      SPRING_DATASOURCE_USERNAME: notezen_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      STRIPE_API_KEY: ${STRIPE_API_KEY}
      STRIPE_WEBHOOK_SECRET: ${STRIPE_WEBHOOK_SECRET}
      JWT_SECRET: ${JWT_SECRET}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    restart: unless-stopped

volumes:
  postgres_data:
EOF

# Lancer
docker-compose up -d
```

---

## Testing

### Scénario 1 : Nouveau client (pas dans allowlist)

1. Aller sur l'app
2. Cliquer sur "Login with Google"
3. Utiliser un email **NOT** dans allowlist
4. Redirection vers pricing (pas encore de subscription)
5. Cliquer "Subscribe"
6. Test card Stripe: `4242 4242 4242 4242`
7. Succès → accès accordé

### Scénario 2 : Client allowlist

1. Login avec email dans allowlist (`admin@notezen.fr`, `dev@notezen.fr`, etc)
2. Accès immédiat (pas de vérification paiement)

### Scénario 3 : Paiement échoué

1. Subscribe avec test card: `4000 0000 0000 0002` (paiement toujours échoué)
2. Attendreque le webhook `invoice.payment_failed` arrive
3. Subscription status → "past_due"
4. Reconnexion → message d'erreur "Paiement échoué"

### Webhook testing (locale)

```bash
# Utiliser Stripe CLI pour tester les webhooks localement
brew install stripe/stripe-cli/stripe  # ou apt-get

stripe login  # Connecter à votre compte

stripe listen --forward-to localhost:8080/api/billing/webhook

stripe trigger invoice.payment_succeeded
```

---

## Migration H2 → PostgreSQL

Si vous avez déjà des données H2 :

### Approach 1 : Export → Import

```bash
# Export H2 (ajouter endpoint temporaire)
# Ou via H2 CLI : SELECT SCRIPT FROM INFORMATION_SCHEMA.INFORMATION_SCHEMA;

# Import PostgreSQL
psql -U notezen_user -d notezen_db < export.sql
```

### Approach 2 : Laisser Hibernate créer les tables

```yaml
# D'abord démarrer avec create
spring.jpa.hibernate.ddl-auto: create

# Une fois stable, passer à validate
spring.jpa.hibernate.ddl-auto: validate
```

---

## Troubleshooting

### Erreur : "Stripe API key not found"
- Vérifier que `.env` existe et contient `STRIPE_API_KEY`
- Redémarrer l'app après modification de `.env`

### Erreur : "Unknown database"
- Vérifier PostgreSQL est running: `sudo systemctl status postgresql`
- Vérifier connexion: `psql -U notezen_user -d notezen_db`

### Webhooks ne reçoivent pas d'événements
- Vérifier URL webhook dans Stripe Dashboard
- Vérifier signature webhook valide (voir BillingController)
- En local: utiliser Stripe CLI pour tester

### Utilisateur ne peut pas se reconnecter après paiement échoué
- Vérifier subscription status en BD: `SELECT status FROM subscriptions WHERE user_id = ...;`
- Manuellement: `UPDATE subscriptions SET status = 'active' WHERE ...;`

---

## Endpoints principaux

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/billing/checkout` | POST | Initier checkout |
| `/api/billing/subscription` | GET | Info subscription actuelle |
| `/api/billing/cancel` | POST | Annuler subscription |
| `/api/billing/webhook` | POST | Webhook Stripe (signature validée) |

---

## Monitoring et Logs

Tous les événements Stripe sont loggés :

```bash
# Sur VPS
tail -f /var/log/notezen/app.log | grep -i stripe
tail -f /var/log/notezen/app.log | grep -i payment
```

---

## Support Stripe

- Documentation: https://stripe.com/docs
- Java SDK: https://github.com/stripe/stripe-java
- Dashboard: https://dashboard.stripe.com
