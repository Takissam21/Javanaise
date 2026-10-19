#!/bin/bash

cleanup() {
    echo
    echo "=== Nettoyage ==="

    pkill -f "irc.MultiThreadCounterTest" 2>/dev/null || true
    pkill -f "irc.MultiThreadReadWriteTest" 2>/dev/null || true
    pkill -f "irc.CounterInit" 2>/dev/null || true
    pkill -f "jvn.JvnCoordImpl" 2>/dev/null || true
}

trap cleanup EXIT


echo "=========================================="
echo " TEST EXTENSION : CLIENT MULTI-THREAD"
echo "=========================================="


echo
echo "=== Compilation ==="

rm -rf bin
mkdir bin

javac -d bin src/jvn/*.java src/irc/*.java || exit 1

echo "Compilation OK"


# ==================================================
# TEST 1 : 10 THREADS EN ECRITURE
# ==================================================

echo
echo "=========================================="
echo " TEST 1 : 10 THREADS WRITE"
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
echo ">>> 10 threads x 1000 increments"
echo ">>> Valeur attendue : 10000"
echo

timeout 10s java -cp bin irc.MultiThreadCounterTest || true


# ==================================================
# RESET
# ==================================================

cleanup
sleep 2


# ==================================================
# TEST 2 : READ + WRITE
# ==================================================

echo
echo "=========================================="
echo " TEST 2 : READ + WRITE"
echo "=========================================="

echo "Demarrage coordinateur..."
java -cp bin jvn.JvnCoordImpl &
COORD_PID=$!

sleep 2

echo "Creation du COUNTER..."
java -cp bin irc.CounterInit &
INIT_PID=$!

sleep 2

echo
echo ">>> 5 threads WRITE x 1000"
echo ">>> 5 threads READ  x 1000"
echo ">>> Valeur finale attendue : 5000"
echo

timeout 10s java -cp bin irc.MultiThreadReadWriteTest || true


echo
echo "=========================================="
echo " TESTS MULTI-THREAD TERMINES"
echo "=========================================="
