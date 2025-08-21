#!/bin/bash

# Script to build the QZ Tray Windows installer on a Debian-based Linux system (like Ubuntu/WSL)

# --- 1. Install Dependencies ---
echo "--- Updating package lists ---"
sudo apt-get update

echo "--- Installing dependencies (git, nsis, openjdk) ---"
# The build script for QZ Tray seems to handle its own JDK for the jlink process,
# but having a system JDK is good practice for 'ant' itself.
sudo apt-get install -y git nsis openjdk-21-jdk ant

# --- 2. Set Environment Variables ---
echo "--- Setting environment variables ---"

# Set JAVA_HOME. This command finds the path to the installed OpenJDK 21.
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
echo "JAVA_HOME set to: $JAVA_HOME"

# The 'ant' command will be available in the PATH after installation via apt-get,
# so setting ANT_HOME is not strictly necessary if installed this way.

# --- 3. Build the Installer ---
echo "--- Starting the Ant build process for the Windows installer ---"
# The 'nsis' target is used to create the Windows installer.
ant nsis

# --- 4. Completion ---
echo "--- Build complete! ---"
# The build output from ant indicates the final installer location.
# Based on the build process, the installer should be located at:
# ./out/qz-tray-*-x86_64.exe
# The version number might change with future updates.
INSTALLER_PATH=$(find ./out -name "qz-tray-*-x86_64.exe")

if [ -f "$INSTALLER_PATH" ]; then
  echo "Windows installer successfully created at: $INSTALLER_PATH"
else
  echo "Build finished, but the installer could not be found at the expected location."
  echo "Please check the build logs above for any errors."
fi
