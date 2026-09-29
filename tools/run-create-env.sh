#!/bin/bash

TARGET="$1"

if [ "$TARGET" == "back" ]; then
    if [ -f "server/.env" ]; then
        echo "Env file in place. Leaving..."
        exit 0
    fi

    echo "Creating basic env file for database and back-end"
    cd server

    echo "POSTGRES_DB=tasknote" >> .env
    echo "POSTGRES_HOST=db" >> .env
    echo "POSTGRES_USER=tasknoteuser" >> .env
    echo "POSTGRES_PASSWORD=default" >> .env
    echo "POSTGRES_PORT=5432" >> .env
    echo "SERVER_SERVLET_CONTEXT_PATH=/" >> .env
    echo "CORS_ALLOWED_ORIGINS=http://localhost:5000" >> .env
    echo "SECURITY_KEY=this-is-a-very-long-security-key-for-dev" >> .env
    echo "RESEND_APIKEY=invalid-api-key-only-placeholder" >> .env
elif [ "$TARGET" == "front" ]; then
    if [ -f "client/.env" ]; then
        echo "Env file in place. Leaving..."
        exit 0
    fi

    echo "Creating basic env file for the front-end app"
    cd client

    echo "VITE_BACKEND_SERVER=http://localhost:8585" >> .env
    echo "VITE_BUILD=nightly" >> .env
else
    echo "Wrong parameter..."
fi

echo "Done!"
