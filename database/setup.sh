#!/bin/bash

# ==============================================================================
# Bus System Project Setup Script
# Description: Prompts for MySQL credentials, creates the database, and loads
#              the schema and sample data for the Bus Search System.
# Usage: ./setup.sh
# ==============================================================================

# --- Configuration ---
DB_NAME="bus_system_db"
SQL_FILE="schema_setup.sql"

# --- Check for SQL File ---
if [ ! -f "$SQL_FILE" ]; then
    echo "❌ Error: SQL data file not found."
    echo "Please ensure '$SQL_FILE' is in the same directory as this script."
    exit 1
fi

echo "======================================================================"
echo " BUS SYSTEM DATABASE SETUP"
echo "======================================================================"

# --- Prompt for Credentials ---
read -p "Enter MySQL Username (e.g., root): " DB_USER

# Use 'read -s' for silent input of the password
read -s -p "Enter MySQL Password: " DB_PASS
echo
echo "----------------------------------------------------------------------"

# --- 1. Create Database if it doesn't exist ---
echo "Attempting to create database '$DB_NAME'..."

# The '|| exit 1' ensures the script stops if the connection fails (wrong credentials)
mysql -u "$DB_USER" -p"$DB_PASS" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"
if [ $? -eq 0 ]; then
    echo "✅ Database '$DB_NAME' created or already exists."
else
    echo "❌ ERROR: Failed to connect to MySQL or create the database. Please check credentials and server status."
    exit 1
fi

# --- 2. Load Schema and Sample Data ---
echo "Loading schema and sample data from '$SQL_FILE' into '$DB_NAME'..."

# Execute the SQL file, redirecting stdin from the file
mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$SQL_FILE"

if [ $? -eq 0 ]; then
    echo "✅ Successfully loaded schema and sample data into '$DB_NAME'."
    echo "----------------------------------------------------------------------"
    echo "Setup Complete!"
    echo "The database is ready. You can now start the Spring Boot application."
else
    echo "❌ ERROR: Failed to load data from '$SQL_FILE'."
    echo "Please check the MySQL server logs for detailed SQL errors."
    exit 1
fi

echo "======================================================================"