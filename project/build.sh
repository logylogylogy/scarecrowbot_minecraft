#!/bin/bash

# ScarecrowBot Build Script

echo "Building ScarecrowBot..."
echo "========================"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed!"
    echo "Please install Maven 3.6+ to build this project."
    echo ""
    echo "Installation:"
    echo "  Ubuntu/Debian: sudo apt install maven"
    echo "  macOS: brew install maven"
    echo "  Windows: Download from https://maven.apache.org/"
    exit 1
fi

# Clean and build
echo "Running: mvn clean package"
mvn clean package

if [ $? -eq 0 ]; then
    echo ""
    echo "========================"
    echo "Build successful!"
    echo "Output JAR: target/ScarecrowBot-1.0-SNAPSHOT.jar"
    echo ""
    echo "Installation:"
    echo "1. Copy JAR to your Paper server's plugins/ folder"
    echo "2. Start/restart the server"
    echo "3. Configure plugins/ScarecrowBot/config.yml"
    echo "4. Use /scarecrow create to spawn the bot"
else
    echo ""
    echo "Build failed! Check the errors above."
    exit 1
fi
