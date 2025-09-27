#!/bin/bash

# ==============================================================================
# Bus System Project PARENT SETUP Script
# Checks for local services (MySQL/Redis), then automates Docker and app launch.
# ==============================================================================

# --- Configuration ---
DB_CONTAINER_NAME="bus-system-mysql"
DB_IMAGE="mysql:8.0"
DB_PORT="3306"
DB_NAME="bus_system_db"

REDIS_CONTAINER_NAME="bus-system-redis"
REDIS_IMAGE="redis:7.2-alpine"
REDIS_PORT="6379"

SQL_FILE="sample_data.sql" # Ensure this file is present
APP_CONFIG_FILE="src/main/resources/application.properties" # Adjust if using application.yml

# --- Colors ---
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# --- Utility Functions ---

check_dependency() {
    if ! command -v "$1" &> /dev/null; then
        echo -e "${RED}❌ Error: '$1' is required but not installed or not in PATH.${NC}"
        exit 1
    fi
}

# Function to check if a port is open locally (using nc or netstat)
is_port_open() {
    if command -v nc &> /dev/null; then
        # nc -z checks if the port is open and listening
        nc -z 127.0.0.1 "$1" > /dev/null 2>&1
        return $?
    elif command -v netstat &> /dev/null; then
        # netstat check for listening port
        # This is less reliable across OSes but better than nothing
        netstat -an | grep "LISTEN" | grep ":$1 " > /dev/null 2>&1
        return $?
    else
        # If no common tool is found, assume it's not running but warn the user
        return 1
    fi
}

wait_for_mysql_docker() {
    echo -e "${GREEN}✅ MySQL container started. Waiting for database service to be healthy...${NC}"
    for i in {1..20}; do
        HEALTH_STATUS=$(docker inspect -f {{.State.Health.Status}} "$DB_CONTAINER_NAME" 2>/dev/null)
        if [ "$HEALTH_STATUS" == "healthy" ]; then
            echo -e "\n${GREEN}✅ Database service is healthy and ready.${NC}"
            return 0
        fi
        echo -n "."
        sleep 3
    done

    echo -e "\n${RED}❌ ERROR: MySQL container failed to become healthy. Exiting.${NC}"
    exit 1
}

# --- Step 1: Check Dependencies ---
echo -e "${YELLOW}--- 1. Checking required dependencies (Docker, Java, Maven, Networking Tools) ---${NC}"
check_dependency "docker"
check_dependency "java"
check_dependency "mvn"
if ! command -v nc &> /dev/null && ! command -v netstat &> /dev/null; then
    echo -e "${RED}⚠️ Warning: Networking tools (nc/netstat) not found. Cannot reliably check for local services.${NC}"
fi

if [ ! -f "$SQL_FILE" ]; then
    echo -e "${RED}❌ Error: SQL data file '$SQL_FILE' not found.${NC}"
    exit 1
fi

# --- Step 2: Prompt for Credentials ---
echo -e "\n${YELLOW}--- 2. Database Credential Setup ---${NC}"
read -p "Enter desired MySQL Username: " DB_USER
read -s -p "Enter desired MySQL Password: " DB_PASS
echo

# ====================================================================
# --- Step 3: Check Local MySQL and Start Docker if needed ---
# ====================================================================
echo -e "\n${YELLOW}--- 3. Checking for Local MySQL Service on Port $DB_PORT ---${NC}"

RUN_DB_IN_DOCKER=true

if is_port_open "$DB_PORT"; then
    echo -e "${GREEN}✅ Local MySQL detected on port $DB_PORT. Skipping Docker run.${NC}"
    RUN_DB_IN_DOCKER=false
else
    echo -e "MySQL service not found locally. Proceeding to start via Docker."

    docker stop $DB_CONTAINER_NAME > /dev/null 2>&1
    docker rm $DB_CONTAINER_NAME > /dev/null 2>&1

    # Run the MySQL container
    docker run -d \
        --name $DB_CONTAINER_NAME \
        -p $DB_PORT:$DB_PORT \
        -e MYSQL_ROOT_PASSWORD=$DB_PASS \
        -e MYSQL_USER=$DB_USER \
        -e MYSQL_PASSWORD=$DB_PASS \
        -e MYSQL_DATABASE=$DB_NAME \
        --health-cmd="mysqladmin ping -u$DB_USER -p$DB_PASS -h 127.0.0.1" \
        --health-interval=10s \
        --health-timeout=5s \
        --health-retries=10 \
        $DB_IMAGE

    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ ERROR: Failed to start MySQL Docker container. Exiting.${NC}"
        exit 1
    fi

    wait_for_mysql_docker
fi

# ====================================================================
# --- Step 4: Check Local Redis and Start Docker if needed ---
# ====================================================================
echo -e "\n${YELLOW}--- 4. Checking for Local Redis Service on Port $REDIS_PORT ---${NC}"

RUN_REDIS_IN_DOCKER=true

if is_port_open "$REDIS_PORT"; then
    echo -e "${GREEN}✅ Local Redis detected on port $REDIS_PORT. Skipping Docker run.${NC}"
    RUN_REDIS_IN_DOCKER=false
else
    echo -e "Redis service not found locally. Proceeding to start via Docker."

    docker stop $REDIS_CONTAINER_NAME > /dev/null 2>&1
    docker rm $REDIS_CONTAINER_NAME > /dev/null 2>&1

    docker run -d \
        --name $REDIS_CONTAINER_NAME \
        -p $REDIS_PORT:$REDIS_PORT \
        $REDIS_IMAGE

    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ ERROR: Failed to start Redis Docker container. Exiting.${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ Redis container started on port ${REDIS_PORT}.${NC}"
fi

# ====================================================================
# --- Step 5: Load Schema and Sample Data ---
# ====================================================================
echo -e "\n${YELLOW}--- 5. Loading Schema and Sample Data ---${NC}"

# Execute the SQL load (via Docker exec if container was run, or local CLI otherwise)
if $RUN_DB_IN_DOCKER; then
    docker exec -i $DB_CONTAINER_NAME mysql -u$DB_USER -p$DB_PASS $DB_NAME < "$SQL_FILE"
else
    # Attempt to use local CLI for the database (assumes user has local mysql client installed)
    mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$SQL_FILE"
fi

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Successfully created database and loaded sample data.${NC}"
else
    echo -e "${RED}❌ ERROR: Failed to execute SQL data load. Check credentials/permissions. Exiting.${NC}"
    exit 1
fi

# --- Step 6: Inform User to Update Configuration ---
echo -e "\n${YELLOW}--- 6. Configuration Update Required ---${NC}"
echo -e "Please update your application configuration file (${APP_CONFIG_FILE}) with the following details:"
echo -e "  - ${GREEN}MySQL URL${NC}: jdbc:mysql://localhost:$DB_PORT/$DB_NAME?..."
echo -e "  - ${GREEN}MySQL Username${NC}: $DB_USER"
echo -e "  - ${GREEN}MySQL Password${NC}: $DB_PASS"
echo -e "  - ${GREEN}Redis Host/Port${NC}: localhost:$REDIS_PORT"
echo -e "  - ${YELLOW}CRITICAL: DO NOT FORGET TO CONFIGURE ELASTIC CLOUD CREDENTIALS!${NC}"

read -p "Press [Y] and Enter when you have updated the configuration: " CONFIRM

if [[ "$CONFIRM" != "Y" && "$CONFIRM" != "y" ]]; then
    echo -e "${RED}Setup aborted by user. Exiting.${NC}"
    exit 1
fi

# --- Step 7: Build and Run Spring Boot Application ---
echo -e "\n${YELLOW}--- 7. Building and Starting Spring Boot Application ---${NC}"

mvn clean install spring-boot:run

# --- Final Message ---
if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}======================================================================${NC}"
    echo -e "${GREEN}✅ Application is running! Access Swagger UI at http://localhost:8080/swagger-ui.html${NC}"
    echo -e "${GREEN}REMEMBER TO RUN: POST /api/admin/sync/full TO INITIALIZE ELASTICSEARCH${NC}"
    echo -e "${GREEN}======================================================================${NC}"
else
    echo -e "\n${RED}❌ Application failed to start. Check Maven and Spring Boot logs.${NC}"
fi