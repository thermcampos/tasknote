#!/bin/bash

echo "Creating task no description"
curl -X POST -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/tasks \
  -d '{
    "urls": ["https://gentoo.org"],
    "dueDate": "2026-10-11",
    "highPriority": true,
    "tags": ["bash", "test"]
  }' | jq

echo ""
echo "Creating task blank description"
curl -X POST -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/tasks \
  -d '{
    "description": "",
    "urls": ["https://gentoo.org"],
    "dueDate": "2026-10-11",
    "highPriority": true,
    "tags": ["bash", "test"]
  }' | jq

echo ""
echo "Creating task big description size > 180"
curl -X POST -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/tasks \
  -d '{
    "description": "task from shell script testing api task from shell script testing api task from shell script testing api task from shell script testing api task from shell script testing api task from shell script testing api",
    "urls": ["https://gentoo.org"],
    "dueDate": "2026-10-11",
    "highPriority": true,
    "tags": ["bash", "test"]
  }' | jq

echo ""
echo "Creating task big url size > 180"
curl -X POST -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/tasks \
  -d '{
    "description": "task from shell script testing api",
    "urls": ["task from shell script testing api task from shell script testing api task from shell script testing api task from shell script testing api task from shell script testing api task from shell script testing api"],
    "dueDate": "2026-10-11",
    "highPriority": true,
    "tags": ["bash", "test"]
  }' | jq

echo ""
echo "Creating task invalid url"
curl -X POST -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/tasks \
  -d '{
    "description": "task from shell script testing api",
    "urls": ["task from shell script testing api"],
    "dueDate": "2026-10-11",
    "highPriority": true,
    "tags": ["bash", "test"]
  }' | jq

echo ""
echo "Creating task big dueDate size > 10"
curl -X POST -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/tasks \
  -d '{
    "description": "task from shell script testing api",
    "urls": ["https://gentoo.org"],
    "dueDate": "this is a long text",
    "highPriority": true,
    "tags": ["bash", "test"]
  }' | jq

echo ""
echo "Creating task wrong dueDate format"
curl -X POST -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/tasks \
  -d '{
    "description": "task from shell script testing api",
    "urls": ["https://gentoo.org"],
    "dueDate": "202a-10/11",
    "highPriority": true,
    "tags": ["bash", "test"]
  }' | jq

echo ""
echo "Creating task big tag size > 20"
curl -X POST -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/tasks \
  -d '{
    "description": "task from shell script testing api",
    "urls": ["https://gentoo.org"],
    "dueDate": "2026-10-11",
    "highPriority": true,
    "tags": ["bashsuperhighgianttagname", "test"]
  }' | jq
