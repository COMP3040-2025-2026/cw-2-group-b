#!/bin/bash

echo "=========================================="
echo "MyNottingham Backend - Production Mode"
echo "Using MySQL Database"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

echo "Please ensure MySQL is running and configured!"
echo ""
echo "Starting backend server..."
echo ""

mvn spring-boot:run
