#!/bin/bash

# Default network name or take from first argument
NETWORK_NAME=${1:-"mp-crm-network"}

# Check if network exists
if ! docker network inspect "$NETWORK_NAME" &>/dev/null; then
    echo "Error: Network $NETWORK_NAME not found!"
    exit 1
fi

# Get all container IDs in the network and stop them
echo "Finding containers in network $NETWORK_NAME..."
CONTAINERS=$(docker network inspect -f '{{range .Containers}}{{.Name}} {{end}}' "$NETWORK_NAME")

if [ -z "$CONTAINERS" ]; then
    echo "No containers found in network $NETWORK_NAME"
    exit 0
fi

for container in $CONTAINERS; do
    echo "Stopping container: $container"
    if docker stop "$container" &>/dev/null; then
        echo "Successfully stopped container: $container"
    else
        echo "Warning: Failed to stop container $container"
    fi
done

echo
echo "All containers in network $NETWORK_NAME have been stopped."
echo

# Optional: Remove the containers as well
read -p "Do you want to remove the stopped containers? (y/n): " REMOVE_CONTAINERS
if [ "${REMOVE_CONTAINERS,,}" = "y" ]; then
    for container in $CONTAINERS; do
        echo "Removing container: $container"
        if docker rm "$container" &>/dev/null; then
            echo "Successfully removed container: $container"
        else
            echo "Warning: Failed to remove container $container"
        fi
    done
    echo "All containers have been removed."
fi

echo
echo "Operation completed."