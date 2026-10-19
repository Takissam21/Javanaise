#!/bin/bash

cleanup() {
    echo
    echo "=== Nettoyage ==="

    pkill -f "irc.ReaderCrashTest" 2>/dev/null || true
    pkill -f "irc.WriterCrashTest" 2>/dev/null || true
    pkill -f "irc.CounterBurstTest" 2>/dev/null || true
    pkill -f "irc.CounterInit" 2>/dev/null || true
    pkill -f "jvn.JvnCoordImpl" 2>/dev/null || true
}

trap cleanup EXIT


echo "=========================================="
echo " TEST EXTENSION : PANNE CLIENT"
echo "=========================================="

echo
echo "=== Compilation ==="

rm -rf bin
mkdir bin

javac -d bin src/jvn/*.java src/irc/*.java || exit 1

echo "Compilation OK"


# ==================================================
# TEST 1 : PANNE D'UN READER
# ==================================================

echo
echo "=========================================="
echo " TEST 1 : PANNE D'UN READER"
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
echo "Demarrage d'un Reader..."

java -cp bin irc.ReaderCrashTest &
READER_PID=$!

sleep 2

echo
echo ">>> Simulation panne Reader"
echo ">>> kill -9 PID $READER_PID"

kill -9 $READER_PID 2>/dev/null || true

sleep 1

echo
echo ">>> Lancement d'un client apres la panne"

timeout 5s java -cp bin irc.CounterBurstTest "Client apres panne Reader" || true

echo
echo ">>> TEST PANNE READER TERMINE"


# ==================================================
# RESET
# ==================================================

cleanup
sleep 2


# ==================================================
# TEST 2 : PANNE D'UN WRITER
# ==================================================

echo
echo "=========================================="
echo " TEST 2 : PANNE D'UN WRITER"
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
echo "Demarrage d'un Writer..."

java -cp bin irc.WriterCrashTest &
WRITER_PID=$!

sleep 2

echo
echo ">>> Simulation panne Writer"
echo ">>> kill -9 PID $WRITER_PID"

kill -9 $WRITER_PID 2>/dev/null || true

sleep 1

echo
echo ">>> Lancement d'un client apres la panne"

timeout 5s java -cp bin irc.CounterBurstTest "Client apres panne Writer" || true

echo
echo ">>> TEST PANNE WRITER TERMINE"


echo
echo "=========================================="
echo " TOUS LES TESTS SONT TERMINES"
echo "=========================================="
