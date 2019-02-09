#!/usr/bin/env bash

set -e

sbt update compile test assembly
cp ${WORKSPACE}/the-station-bot/target/scala-2.12/the-station-bot.jar /mnt/data/the-station-bot
cp ${WORKSPACE}/launcher/target/scala-2.12/launcher.jar /mnt/data/the-station-bot
chown jenkins:datawrite /mnt/data/the-station-bot/the-station-bot.jar
