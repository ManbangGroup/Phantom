#!/usr/bin/env bash

./gradlew clean

./gradlew phantom-sample:plugin-component:assembleDebug
./gradlew phantom-sample:plugin-view:assembleDebug

./gradlew phantom-sample:host:assembleDebug
