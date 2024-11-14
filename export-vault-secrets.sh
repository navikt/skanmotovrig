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

if test -f /var/run/secrets/nais.io/srvjiradokdistavstemming/username;
then
    echo "Setting skanmotovrig_jira_username"
    export skanmotovrig_jira_username=$(cat /var/run/secrets/nais.io/srvjiradokdistavstemming/username)
fi

if test -f /var/run/secrets/nais.io/srvjiradokdistavstemming/password;
then
    echo "Setting skanmotovrig_jira_password"
    export skanmotovrig_jira_password=$(cat /var/run/secrets/nais.io/srvjiradokdistavstemming/password)
fi
