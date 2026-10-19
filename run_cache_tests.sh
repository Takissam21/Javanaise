#!/bin/bash

cleanup() {
    echo
    echo "=== Nettoyage ==="

    pkill -f "irc.CacheFlushTest" 2>/dev/null || true
    pkill -f "irc.CacheSaturationTest" 2>/dev/null || true
    pkill -f "irc.CounterInit" 2>/dev/null || true
    pkill -f "jvn.JvnCoordImpl" 2>/dev/null || true
}

trap cleanup EXIT


echo "=========================================="
echo " TEST EXTENSION : SATURATION DU CACHE"
echo "=========================================="


echo
echo "=== Compilation ==="

rm -rf bin
mkdir bin

javac -d bin src/jvn/*.java src/irc/*.java || exit 1

echo "Compilation OK"


# ==================================================
# TEST 1 : FLUSH MANUEL
# ==================================================

echo
echo "=========================================="
echo " TEST 1 : FLUSH MANUEL"
echo "=========================================="

cleanup
sleep 1

echo "Demarrage coordinateur..."
java -cp bin jvn.JvnCoordImpl &
COORD_PID=$!

sleep 2

echo "Creation du COUNTER..."
java -cp bin irc.CounterInit &
INIT_PID=$!

sleep 2

echo
echo ">>> Test du flush manuel"
echo

timeout 5s java -cp bin irc.CacheFlushTest || true


# ==================================================
# RESET
# ==================================================

cleanup
sleep 2


# ==================================================
# TEST 2 : SATURATION AUTOMATIQUE
# ==================================================

echo
echo "=========================================="
echo " TEST 2 : SATURATION AUTOMATIQUE"
echo "=========================================="

echo "Demarrage coordinateur..."
java -cp bin jvn.JvnCoordImpl &
COORD_PID=$!

sleep 2

echo
echo ">>> Taille maximale du cache : 3 objets"
echo ">>> Creation de 4 objets"
echo ">>> Le 4e doit provoquer un flush automatique"
echo

timeout 5s java -cp bin irc.CacheSaturationTest || true


echo
echo "=========================================="
echo " TESTS CACHE TERMINES"
echo "=========================================="
