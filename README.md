# Javanaise — Guide d'exécution

Projet **Javanaise** — UE Systèmes et Applications Répartis (M2 GI).

Le dépôt contient trois branches :

- `main` : JVN1
- `jvn2` : JVN2 avec annotations et proxy dynamique
- `extensions` : extensions réalisées et scripts de test

## Prérequis

Vérifier Java :

```bash
java -version
javac -version
```

Sous Ubuntu / WSL :

```bash
sudo apt update
sudo apt install default-jdk
```

## Récupérer le projet

```bash
git clone https://github.com/Takissam21/Javanaise.git
cd Javanaise
git branch -a
```

# 1. Branche `main` — JVN1

JVN1 utilise explicitement les verrous Javanaise.

```bash
git switch main
git pull origin main
chmod +x run_jvn1_tests.sh
./run_jvn1_tests.sh
```

Le script compile le projet, démarre le coordinateur, crée `COUNTER`, puis lance plusieurs clients writers ainsi qu'un reader afin de tester les verrous READ/WRITE et les invalidations.

Exemple d'utilisation JVN1 :

```java
jo.jvnLockWrite();

Counter counter =
    (Counter) jo.jvnGetObjectState();

counter.increment();

jo.jvnUnLock();
```

# 2. Branche `jvn2` — JVN2

JVN2 rend la gestion des verrous transparente grâce aux annotations `@JvnRead`, `@JvnWrite` et au proxy dynamique `JvnProxy`.

```bash
git switch jvn2
git pull origin jvn2
chmod +x run_jvn2_tests.sh
./run_jvn2_tests.sh
```

Exemple :

```java
public interface CounterItf {

    @JvnWrite
    void increment();

    @JvnRead
    int getValue();
}
```

Puis :

```java
CounterItf counter =
    (CounterItf) JvnProxy.newInstance(jo);

counter.increment();
int value = counter.getValue();
```

Le proxy effectue automatiquement :

```text
appel applicatif
      |
      v
   JvnProxy
      |
      +--> @JvnRead  -> jvnLockRead()
      |
      +--> @JvnWrite -> jvnLockWrite()
      |
      v
méthode métier
      |
      v
 jvnUnLock()
```

# 3. Branche `extensions`

Cette branche contient les trois extensions réalisées :

1. gestion des pannes client ;
2. support des clients multi-threadés ;
3. saturation du cache client.

```bash
git switch extensions
git pull origin extensions
```

## Test des pannes client

```bash
chmod +x run_client_failure_tests.sh
./run_client_failure_tests.sh
```

Ce test simule notamment des pannes brutales de reader/writer avec `kill -9`. Le coordinateur détecte les erreurs RMI et permet aux autres clients de continuer.

## Test multi-thread

```bash
chmod +x run_multithread_tests.sh
./run_multithread_tests.sh
```

Le test vérifie les règles suivantes :

```text
READ + READ   -> autorisé
WRITE + READ  -> exclusion
WRITE + WRITE -> exclusion
```

Exemple de résultat attendu :

```text
10 threads x 1000 increments
Valeur attendue = 10000
Valeur obtenue = 10000
```

## Test de saturation du cache

```bash
chmod +x run_cache_tests.sh
./run_cache_tests.sh
```

Le test vérifie :

- le flush manuel d'un objet ;
- la conservation de son état côté coordinateur ;
- le nouveau `lookup` après flush ;
- le flush automatique lorsque le cache dépasse sa taille maximale.

Dans notre implémentation :

```java
private static final int MAX_CACHE_SIZE = 3;
```

Exemple :

```text
CACHE_OBJECT_1 cree et enregistre.
CACHE_OBJECT_2 cree et enregistre.
CACHE_OBJECT_3 cree et enregistre.
Cache sature : flush automatique de l'objet 0
CACHE_OBJECT_4 cree et enregistre.
```

La politique actuelle choisit un objet disponible dans la `HashMap`, en évitant l'objet qui vient d'être ajouté. Ce n'est pas une politique LRU ou FIFO.

# Résumé rapide

## JVN1

```bash
git switch main
git pull origin main
chmod +x run_jvn1_tests.sh
./run_jvn1_tests.sh
```

## JVN2

```bash
git switch jvn2
git pull origin jvn2
chmod +x run_jvn2_tests.sh
./run_jvn2_tests.sh
```

## Extensions

```bash
git switch extensions
git pull origin extensions

chmod +x run_client_failure_tests.sh
chmod +x run_multithread_tests.sh
chmod +x run_cache_tests.sh

./run_client_failure_tests.sh
./run_multithread_tests.sh
./run_cache_tests.sh
```

# Nettoyage RMI

Si un ancien test a laissé des processus Java actifs :

```bash
pkill -f jvn.JvnCoordImpl
pkill -f irc.CounterInit
```

Pour voir les processus Java :

```bash
ps aux | grep java
```

# Changement de branche avec modifications locales

Si Git refuse le changement de branche :

```text
Your local changes would be overwritten by checkout
```

Vérifier :

```bash
git status
```

Puis soit commit :

```bash
git add .
git commit -m "Save current work"
```

soit stash :

```bash
git stash
git switch NOM_DE_LA_BRANCHE
```

et pour récupérer les changements :

```bash
git stash pop
```

# Documentation

La documentation complète du projet est disponible dans `docs/` sur la branche `extensions`.
