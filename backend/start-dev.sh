#!/bin/bash

echo "=========================================="
echo "MyNottingham Backend - Development Mode"
echo "Using H2 In-Memory Database"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

echo "Starting backend server..."
echo ""

mvn spring-boot:run -Dspring-boot.run.profiles=dev
