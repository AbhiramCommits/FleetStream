#!/usr/bin/env bash
set -e

mkdir -p results

echo "Running Sweep 1: Low rate (100 devices @ 5 msg/s)"
sbt "loadgen/runMain fleetstream.loadgen.Main --devices 100 --rate 5 --duration 5 --out results/"

echo "Running Sweep 2: Mid rate (500 devices @ 10 msg/s)"
sbt "loadgen/runMain fleetstream.loadgen.Main --devices 500 --rate 10 --duration 5 --out results/"

echo "Running Sweep 3: High saturation rate (1000 devices @ 50 msg/s)"
sbt "loadgen/runMain fleetstream.loadgen.Main --devices 1000 --rate 50 --duration 5 --out results/"

echo "Benchmark sweep complete. Results stored in results/"
