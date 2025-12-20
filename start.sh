#!/bin/bash

echo "Starting build process..."

echo
echo "=== Running Maven build ==="
if ! ./mvnw clean package -DskipTests; then
    echo "Maven build failed!"
    read -p "Press Enter to continue..."
    exit 1
fi
echo "Maven build completed successfully!"

echo
echo "=== Starting Docker Compose ==="
if ! docker-compose up -d --build; then
    echo "Docker Compose failed!"
    read -p "Press Enter to continue..."
    exit 1
fi
echo "Docker Compose started successfully!"

echo
echo "All operations completed successfully!"
echo "You can now access the application at http://localhost:8080"

read -p "Press Enter to continue..."