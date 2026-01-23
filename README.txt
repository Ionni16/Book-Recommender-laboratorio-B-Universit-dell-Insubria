BOOK RECOMMENDER – README

Progetto di Laboratorio B – Architettura Client/Server
Tecnologie: Java, Maven, PostgreSQL, JDBC, JavaFX


========================
STRUTTURA DEL PROGETTO
========================

- src/
  Contiene i sorgenti Maven multi-modulo:
  - common : classi condivise (model, DTO, networking)
  - serverBR : server e accesso al database
  - clientBR : client con interfaccia grafica JavaFX

- bin/
  Contiene i file JAR eseguibili:
  - clientBR.jar
  - serverBR.jar

- doc/
  Contiene la documentazione:
  - Manuale Utente (PDF)
  - Manuale Tecnico (PDF)
  - JavaDoc

- schema.sql
  Script SQL per la creazione del database (tabelle e vincoli)

- pom.xml
  POM Maven principale (multi-modulo)


========================
REQUISITI
========================

- Java JDK 17 (o superiore)
- Maven
- PostgreSQL
- Sistema operativo: Windows (comandi riportati per Windows)


========================
DATABASE (PostgreSQL)
========================

Il progetto utilizza un database PostgreSQL accessibile tramite JDBC.

È fornito lo script:
- src/serverBR/src/main/resources/schema.sql

Lo script crea tutte le tabelle e i vincoli necessari.
Il database viene inizialmente creato VUOTO.

Non è incluso un popolamento iniziale (seed) per evitare di
appesantire la consegna.
I dati (libri, utenti, valutazioni) vengono inseriti tramite
le funzionalità dell’applicazione client.


------------------------
CREAZIONE DATABASE
------------------------

ATTENZIONE: il server deve essere SPENTO durante questi passaggi.

1) Creare il database (come utente amministratore PostgreSQL):

"C:\Program Files\PostgreSQL\16\bin\createdb.exe" -U postgres bookrecommender

2) Dare i permessi all’utente applicativo (es. br_user):

"C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -d bookrecommender -c "GRANT ALL PRIVILEGES ON DATABASE bookrecommender TO br_user;"

3) Applicare lo schema SQL:

"C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -d bookrecommender -f src/serverBR/src/main/resources/schema.sql


========================
BUILD DEL PROGETTO
========================

Dalla cartella root del progetto (dove si trova pom.xml):

mvn -U clean install


========================
AVVIO DEL SERVER
========================

Il server viene avviato tramite Maven.

PowerShell / CMD:

mvn -pl src/serverBR exec:java "-Dexec.mainClass=bookrecommender.server.MainServer"

All’avvio il server richiede:
- host (es. localhost)
- porta (es. 5432)
- nome database (bookrecommender)
- username PostgreSQL (es. br_user)
- password


========================
AVVIO DEL CLIENT
========================

Il client utilizza JavaFX.

PowerShell / CMD:

mvn -pl src/clientBR javafx:run


========================
UTILIZZO DELL’APPLICAZIONE
========================

Una volta avviati server e client è possibile:
- registrare utenti
- inserire libri nella libreria
- inserire valutazioni
- ottenere raccomandazioni di libri

Il database viene popolato dinamicamente durante l’utilizzo
dell’applicazione.


========================
NOTE IMPORTANTI
========================

- Il database non è condiviso: ogni utente può creare il proprio DB locale.
- Non è richiesto alcun dataset iniziale dalle specifiche del progetto.
- La presenza dello script schema.sql garantisce la riproducibilità del sistema.
- I file JAR presenti nella cartella /bin sono quelli da utilizzare per la consegna.


========================
AUTORI
========================

Vedi file autori.txt
