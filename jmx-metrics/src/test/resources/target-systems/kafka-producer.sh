#!/usr/bin/env bash

function msg() {
    while true; do
        echo 'some message'
        sleep .25
    done
}

msg | kafka-console-producer.sh --bootstrap-server kafka:9092 --topic test-topic-1
