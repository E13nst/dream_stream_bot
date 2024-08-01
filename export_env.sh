#!/bin/bash

if [ ! -f .env.app ]; then
  echo "File .env.app not found!"
  exit 1
fi

while IFS= read -r line; do
  if [[ -z "$line" || "$line" =~ ^# ]]; then
    continue
  fi

  IFS='=' read -r name value <<< "$line"

  name=$(echo "$name" | xargs)
  value=$(echo "$value" | xargs)

  echo "$name=$value"
  export "$name=$value"
done < .env.app

echo "DONE"
