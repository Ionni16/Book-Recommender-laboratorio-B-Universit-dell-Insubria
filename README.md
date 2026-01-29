[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

<p align="center">
  <img src="Docs/logoInsubria.svg" width="250" alt="Universita Logo">
</p>

# 📚 Book Recommender

**Progetto universitario per l'esame di Laboratorio Interdisciplinare B – Università degli Studi dell’Insubria (2026)**

Applicazione **client–server** scritta in **Java 17** per la ricerca, gestione e raccomandazione di libri, con:

* client desktop **JavaFX**
* backend su **PostgreSQL**
* comunicazione tramite protocollo applicativo Request/Response

Documentazione tecnica, UML e JavaDoc disponibili nella cartella `/doc`.

---

## 👥 Autori

* **Matteo Ferrario**
* **Ionut Puiu**
* **Richard Zefi**

---

## 📦 Dipendenze principali

Il progetto utilizza **Maven** per la gestione delle dipendenze (vedi `pom.xml`).

| Libreria        | Versione |
| --------------- | -------- |
| JavaFX          | 17.x     |
| PostgreSQL JDBC | 42.x     |

---

## ⚙️ Prerequisiti

* **Java JDK 17**
* **Maven 3.8+**
* **PostgreSQL 13+**
* IDE consigliato: **IntelliJ IDEA**
* Sistema operativo: Windows / Linux / macOS

---

## 🗄️ Setup Database PostgreSQL

### 1. Prerequisiti

* PostgreSQL installato e in esecuzione
* Database vuoto disponibile

---

### 2. Creazione database

```sql
CREATE DATABASE bookrecommender;
```

---

### 3. Creazione schema e popolamento DB (DBCreator) (per sviluppo)

⚠️ **TUTTI i comandi vanno eseguiti dalla directory `DBCreator`**

```bash
cd DBCreator
```

#### Creazione database e schema 

```bash
mvn -pl DBCreator clean compile
```

Questo comando crea lo schema del database e popola le tabelle iniziali.

#### Reset completo database

Se la creazione viene interrotta, parziale o presenta errori:

```bash
mvn exec:java "-Dexec.args=--reset"
```

Questo comando elimina e ricrea completamente il database.

---

## 🧱 Build progetto (per sviluppo)

Dalla root del progetto:

```bash
mvn clean package
```

Gli artefatti verranno generati in:

```
/target
```
Se dopo la build i file `ClientBR.jar`, `ServerBR.jar` e `DBCreator.jar` non sono dentro la relativa cartella `bin`, procedere nel copiarli nei seguenti percorsi:

* `src/clientBR/target/ClientBR.jar`.
* `src/serverBR/target/ServerBR.jar`.
* `src/dbcreator/target/DBCreator.jar`.

---
## ▶️ Avvio del DB (Utente finale)

Il DB deve essere inizializzato **dalla cartella bin/**, dopo aver effettuato il cambio con  `cd bin`:.
```bash
java -jar DBCreator.jar
```
NB. questo comando va eseguito una singola volta.

---

## ▶️ Avvio server (Utente finale)

Sempre dalla directory `bin`.

```bash
java -jar ServerBR.jar
```

Server disponibile su:

```
127.0.0.1:5050
```

---

## ▶️ Avvio client JavaFX (Utente finale)

Sempre dalla cartella `bin`, ci sono rispettivamente due launch script per l'avvio di JavaFx in base all'OS in utilizzo.
Per windows:

```bash
run-client.bat
```
Per Linux/Mac:
```bash
./run-client.sh
```
in caso di problemi tentare con il comando soprastante, procedere con quello sotto indicato per poi ripetere il precedente: 
```bash
chmod +x run-client.sh
```
---

## ▶️ Avvio da IDE

Classi principali:

* Server: `MainServer`
* Client: `Main`
* DB Init: `DBCreator`

---

## 🔍 Caricamento iniziale catalogo

* All’avvio e dopo Reset viene caricato un sottoinsieme del catalogo
* Limite fisso (es. 250 libri)
* Ordinamento per **ID crescente**
* Niente duplicati
* Ricerca filtrata con limite configurabile separato

---

## 📌 Note tecniche

* Il caricamento iniziale può richiedere alcuni secondi per dataset molto grandi
* Gli ID visualizzati sono **chiavi reali del database**
* Il server deve essere avviato prima del client
* Tutti i comandi Maven sono destinati **esclusivamente allo sviluppo**.  
* L’utente finale deve utilizzare solo i file presenti nella cartella `bin/`.

---

## 📄 Licenza

Distribuito sotto licenza **MIT**. Vedi file `LICENSE`.

