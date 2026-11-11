#!/bin/bash
# Збирання і запуск моделі (Java 17+, без сторонніх бібліотек).
cd "$(dirname "$0")" || exit 1
rm -rf build && mkdir -p build results
javac -encoding UTF-8 -d build asutp/*.java || exit 1
java -cp build asutp.Main results | tee results/run_log.txt
# Експериментальне дослідження (розділ 4)
java -cp build asutp.Experiment results | tee results/experiment_log.txt
# Пробні запуски пропозицій (розділ 5)
java -cp build asutp.Proposals results | tee results/proposals_log.txt
