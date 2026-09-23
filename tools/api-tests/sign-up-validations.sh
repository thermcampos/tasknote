#!/bin/bash

# Create user
echo "1/9 Creating user no email"
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{
    "password":"Teste@123",
    "passwordAgain":"Teste@123",
    "lang":"en"
  }'

echo ""
echo "2/9 Creating user no empty email"
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{
    "email":"",
    "password":"Teste@123",
    "passwordAgain":"Teste@123",
    "lang":"en"
  }'

echo ""
echo "3/9 Creating user null email"
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{
    "email":null,
    "password":"Teste@123",
    "passwordAgain":"Teste@123",
    "lang":"en"
  }'

echo ""
echo "4/9 Creating user invalid email"
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{
    "email":"email@site.",
    "password":"Teste@123",
    "passwordAgain":"Teste@123",
    "lang":"en"
  }'

echo ""
echo "5/9 Creating user big email size > 100"
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{
    "email":"email@site.comemail@site.comemail@site.comemail@site.comemail@site.comemail@site.comemail@site.comemail@site.com",
    "password":"Teste@123",
    "passwordAgain":"Teste@123",
    "lang":"en"
  }'

echo ""
echo "6/9 Creating user no password"
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{
    "email":"email@site.com",
    "passwordAgain":"Teste@123",
    "lang":"en"
  }'

echo ""
echo "6/9 Creating user empty password"
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{
    "email":"email@site.com",
    "password":"",
    "passwordAgain":"Teste@123",
    "lang":"en"
  }'

echo ""
echo "7/9 Creating user null password"
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{
    "email":"email@site.com",
    "password":null,
    "passwordAgain":"Teste@123",
    "lang":"en"
  }'

echo ""
echo "8/9 Creating user big password size > 30"
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{
    "email":"email@site.com",
    "password":"thisisavery-long password to be created for testing",
    "passwordAgain":"Teste@123",
    "lang":"en"
  }'

echo ""
echo "9/9 Creating user big lang size > 2"
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{
    "email":"email@site.com",
    "password":"thisisavery@asdA",
    "passwordAgain":"Teste@123",
    "lang":"enn"
  }'