#!/bin/bash

START_FILE="/tmp/jvn2_start"

cleanup() {
    echo
    echo "=== Nettoyage ==="

    rm -f "$START_FILE"

    pkill -f "irc.CounterBurstTest" 2>/dev/null || true
    pkill -f "irc.CounterReaderTest" 2>/dev/null || true
    pkill -f "irc.CounterFinalReadTest" 2>/dev/null || true
    pkill -f "irc.CounterInit" 2>/dev/null || true
    pkill -f "jvn.JvnCoordImpl" 2>/dev/null || true

    echo "=== FIN ==="
}

trap cleanup EXIT


echo "=== Compilation JVN2 ==="

rm -rf bin
mkdir bin

javac -d bin src/jvn/*.java src/irc/*.java || exit 1

echo "Compilation OK"


echo
echo "=== Nettoyage anciens processus ==="

pkill -f "irc.CounterBurstTest" 2>/dev/null || true
pkill -f "irc.CounterReaderTest" 2>/dev/null || true
pkill -f "irc.CounterFinalReadTest" 2>/dev/null || true
pkill -f "irc.CounterInit" 2>/dev/null || true
pkill -f "jvn.JvnCoordImpl" 2>/dev/null || true

rm -f "$START_FILE"

sleep 1


echo
echo "=== Demarrage coordinateur ==="

java -cp bin jvn.JvnCoordImpl &
COORD_PID=$!

sleep 2


echo
echo "=== Initialisation COUNTER ==="

java -cp bin irc.CounterInit &
INIT_PID=$!

sleep 2


echo
echo "=== Preparation : 3 writers + 1 reader ==="


# Writer 1
(
    while [ ! -f "$START_FILE" ]; do
        sleep 0.01
    done

    timeout 5s java -cp bin irc.CounterBurstTest "Client 1"
) &
P1=$!


# Writer 2
(
    while [ ! -f "$START_FILE" ]; do
        sleep 0.01
    done

    timeout 5s java -cp bin irc.CounterBurstTest "Client 2"
) &
P2=$!


# Writer 3
(
    while [ ! -f "$START_FILE" ]; do
        sleep 0.01
    done

    timeout 5s java -cp bin irc.CounterBurstTest "Client 3"
) &
P3=$!


# Reader
(
    while [ ! -f "$START_FILE" ]; do
        sleep 0.01
    done

    timeout 5s java -cp bin irc.CounterReaderTest "Reader"
) &
P4=$!


# Laisser les 4 clients arriver à la barrière
sleep 1


echo
echo "================================================"
echo " GO ! 3 writers + 1 reader demarrent ensemble"
echo "================================================"


# Départ commun
touch "$START_FILE"

# Laisser le temps aux 3 writers de terminer leurs 20 écritures
sleep 2

echo
echo "=== Lecture finale du compteur ==="

timeout 3s java -cp bin irc.CounterFinalReadTest

# Attendre la fin de tous les clients
wait $P1 || true
wait $P2 || true
wait $P3 || true
wait $P4 || true


echo
echo "=== Test JVN2 termine ==="
echo "Writers : 3 clients x 20 increments"
echo "Reader  : 20 lectures"
echo "Valeur finale attendue : 60"