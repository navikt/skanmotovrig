#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvskanmotovrig/username;
then
    echo "Setting SERVICEUSER_USERNAME"
    export skanmotovrig_SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvskanmotovrig/username)
fi

if test -f /var/run/secrets/nais.io/srvskanmotovrig/password;
then
    echo "Setting SERVICEUSER_PASSWORD"
    export skanmotovrig_SERVICEUSER_PASSWORD=$(cat /var/run/secrets/nais.io/srvskanmotovrig/password)
fi
