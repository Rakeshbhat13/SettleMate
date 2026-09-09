# SplitEasy Web (English)
A Java + Spring Boot web app to easily split expenses with friends, family, or coworkers.
Create a group, add who paid, who participated, and the amount. The app automatically calculates who owes whom.
You can:

## How to run it on the web
1. Open:
   ```bash
   worldwide-farrah-spliteasy-ac46700c.koyeb.app/
2. Create a new group (pick a name).
3. Share the group link with friends: anyone with the link can view/edit the same group.
4. If you don’t share the link, you can keep personal groups tied to your session.

## How to run locally

1. Clone the repo:
   ```bash
   git clone https://github.com/cotibodereau/SplitEasy-web.git
   cd SplitEasy-web
2. Make sure you have Java 17 and Maven installed.
3. Run:
   ```bash
   mvn spring-boot:run
4. Open in your browser: 
   ```bash
   http://localhost:8080
# SplitEasy Web (Español)
Una aplicación web en Java + Spring Boot para dividir gastos fácilmente con amigos, familiares o compañeros de trabajo.
Creá un grupo, agregá quién pagó, quién participó y el monto. La app calcula automáticamente quién le debe a quién.
Podes:

## Cómo usarlo en la web
1. Abrir:
   ```bash
   worldwide-farrah-spliteasy-ac46700c.koyeb.app/
2. Crear un nuevo grupo (elegí un nombre).
3. Compartir el enlace del grupo con amigos: cualquiera con el enlace puede ver/editar el mismo grupo.
4. Si no compartís el enlace, podes mantener grupos personales vinculados a tu sesión.

## Cómo ejecutarlo localmente

1. Cloná el repositorio:
   ```bash
   git clone https://github.com/cotibodereau/SplitEasy-web.git
   cd SplitEasy-web
2. Asegúrate de tener instalado Java 17 y Maven.
3. Ejecutá:
   ```bash
   mvn spring-boot:run
4. Abrí en tu navegador: 
   ```bash
   http://localhost:8080
# SettleMate

Smart Group Expense Management & Settlement.

SettleMate helps groups record shared expenses, understand balances, and produce concise settlement plans.

## Features

- Create groups and add members
- Record expenses with multiple payers and participants
- Calculate balances and suggested transfers
- Export expenses, balances, and transfers as CSV
- Responsive server-rendered interface with light and dark themes
- H2 for local development and PostgreSQL for production

## Settlement Optimization

SettleMate calculates each member's net balance, separates debtors from creditors, and matches them with a deterministic greedy algorithm. This produces a practical settlement plan instead of displaying every pairwise debt.

## Technology Stack

- Java 21
- Spring Boot 3.3
- Spring MVC and Thymeleaf
- Spring Data JPA and Hibernate
- H2 and PostgreSQL
- Maven and Docker

## Architecture

The application uses server-rendered Thymeleaf views, Spring MVC controllers, a settlement service, Spring Data repositories, and a relational database.

## Database

Local development uses the file database at `./data/devdb`. Production uses PostgreSQL configured with `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, and `DATABASE_PASSWORD`.

## Installation

Install Java 21, then clone the repository:

```powershell
git clone https://github.com/Rakeshbhat13/SettleMate.git
cd SettleMate
```

## Running Locally

```powershell
./mvnw.cmd spring-boot:run
```

Open http://localhost:8080.

## Testing

```powershell
./mvnw.cmd test
```

## Docker

```powershell
docker build -t settlemate .
docker run --rm -p 8080:8080 settlemate
```

## API Overview

The current application exposes browser routes for groups, members, expenses, settlement summaries, and CSV exports. A dedicated JSON API is planned after the domain model is normalized.

## Screenshots

Screenshots will be added after the SettleMate dashboard redesign is complete.

## Future Improvements

Planned work includes percentage and exact splits, settlement history, categories, analytics, budgets, recurring expenses, authentication, and share-token authorization. These are not yet implemented.
