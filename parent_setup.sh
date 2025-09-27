#!/bin/bash

# ==============================================================================
# Bus System Project PARENT SETUP Script
# Automates MySQL Docker setup, database initialization, and application launch.
# ==============================================================================

# --- Configuration ---
DB_CONTAINER_NAME="bus-system-mysql"
DB_IMAGE="mysql:8.0"
DB_PORT="3306"
DB_NAME="bus_system_db"
SQL_FILE="database/schema_setup.sql" # Ensure this file is present
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

# --- Step 1: Check Dependencies ---
echo -e "${YELLOW}--- 1. Checking required dependencies (Docker, Java, Maven) ---${NC}"
check_dependency "docker"
check_dependency "java"
check_dependency "mvn"

if [ ! -f "$SQL_FILE" ]; then
    echo -e "${RED}❌ Error: SQL data file '$SQL_FILE' not found.${NC}"
    exit 1
fi

# --- Step 2: Prompt for Credentials ---
echo -e "\n${YELLOW}--- 2. MySQL Credential Setup ---${NC}"
read -p "Enter desired MySQL Username for Docker: " DB_USER
read -s -p "Enter desired MySQL Password for Docker: " DB_PASS
echo

# --- Step 3: Run MySQL Docker Container ---
echo -e "\n${YELLOW}--- 3. Starting MySQL Container ($DB_IMAGE) ---${NC}"

# Stop and remove any existing container with the same name
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

echo -e "${GREEN}✅ MySQL container started. Waiting for database service to be healthy...${NC}"

# Wait for the database service to be ready
HEALTH_STATUS=""
for i in {1..20}; do
    HEALTH_STATUS=$(docker inspect -f {{.State.Health.Status}} $DB_CONTAINER_NAME)
    if [ "$HEALTH_STATUS" == "healthy" ]; then
        echo -e "${GREEN}✅ Database service is healthy and ready.${NC}"
        break
    fi
    echo -n "."
    sleep 3
done

if [ "$HEALTH_STATUS" != "healthy" ]; then
    echo -e "\n${RED}❌ ERROR: MySQL container failed to become healthy. Check Docker logs. Exiting.${NC}"
    exit 1
fi

# --- Step 4: Load Schema and Sample Data ---
echo -e "\n${YELLOW}--- 4. Loading Schema and Sample Data ---${NC}"

# Execute the SQL file inside the Docker container
docker exec -i $DB_CONTAINER_NAME mysql -u$DB_USER -p$DB_PASS $DB_NAME < "$SQL_FILE"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Successfully loaded schema and sample data.${NC}"
else
    echo -e "${RED}❌ ERROR: Failed to execute SQL data load. Check $SQL_FILE for errors. Exiting.${NC}"
    exit 1
fi

# --- Step 5: Inform User to Update Configuration ---
echo -e "\n${YELLOW}--- 5. Configuration Update Required ---${NC}"
echo -e "Please update your application configuration file (${APP_CONFIG_FILE}) with the following details:"
echo -e "  - ${GREEN}spring.datasource.url${NC}=jdbc:mysql://localhost:$DB_PORT/$DB_NAME?..."
echo -e "  - ${GREEN}spring.datasource.username${NC}=$DB_USER"
echo -e "  - ${GREEN}spring.datasource.password${NC}=$DB_PASS"
echo -e "  - ${YELLOW}NOTE: DO NOT FORGET TO CONFIGURE ELASTIC CLOUD CREDENTIALS!${NC}"

read -p "Press [Y] and Enter when you have updated the configuration: " CONFIRM

if [[ "$CONFIRM" != "Y" && "$CONFIRM" != "y" ]]; then
    echo -e "${RED}Setup aborted by user. Exiting.${NC}"
    exit 1
fi

# --- Step 6: Build and Run Spring Boot Application ---
echo -e "\n${YELLOW}--- 6. Building and Starting Spring Boot Application ---${NC}"

# Clean build and run
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