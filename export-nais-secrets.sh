#!/usr/bin/env sh

#Slack
if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_slack_enabled; then
  echo "Setting skanmotovrig_slack_enabled"
  export skanmotovrig_slack_enabled=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_slack_enabled)
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_slack_channel; then
  echo "Setting skanmotovrig_slack_channel"
  export skanmotovrig_slack_channel=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_slack_channel)
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_slack_token; then
  echo "Setting skanmotovrig_slack_token"
  export skanmotovrig_slack_token=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_slack_token)
fi

#PGP
if test -f /var/run/secrets/nais.io/skanmotovrig/org_pgp_passphrase; then
  echo "Setting org_pgp_passphrase"
  export org_pgp_passphrase=$(cat /var/run/secrets/nais.io/skanmotovrig/org_pgp_passphrase)
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_pgp_passphrase; then
  echo "Setting skanmotovrig_pgp_passphrase"
  export skanmotovrig_pgp_passphrase=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_pgp_passphrase)
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_pgp_privateKey; then
  echo "Setting skanmotovrig_pgp_privateKey"
  export skanmotovrig_pgp_privateKey=/var/run/secrets/nais.io/skanmotovrig/skanmotovrig_pgp_privateKey
fi

#Skanmotovrig route config
if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_ovrig_schedule; then
  echo "Setting skanmotovrig_ovrig_schedule"
  export skanmotovrig_ovrig_schedule=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_ovrig_schedule)
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_avstem_schedule
then
    echo "Setting skanmotovrig_avstem_schedule"
    export skanmotovrig_avstem_schedule=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_avstem_schedule)
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_avstem_startup; then
  echo "Setting skanmotovrig_avstem_startup"
  export skanmotovrig_avstem_startup=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_avstem_startup)
fi

#Jira
if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_jira_password; then
  echo "Setting skanmotovrig_jira_password"
  export skanmotovrig_jira_password=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_jira_password)
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_jira_username; then
  echo "Setting skanmotovrig_jira_username"
  export skanmotovrig_jira_username=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_jira_username)
fi

#SFTP
if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_sftp_privateKey; then
  echo "Setting skanmotovrig_sftp_privateKey"
  export skanmotovrig_sftp_privateKey=/var/run/secrets/nais.io/skanmotovrig/skanmotovrig_sftp_privateKey
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_sftp_hostKey; then
  echo "Setting skanmotovrig_sftp_hostKey"
  export skanmotovrig_sftp_hostKey=/var/run/secrets/nais.io/skanmotovrig/skanmotovrig_sftp_hostKey
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_sftp_host; then
  echo "Setting skanmotovrig_sftp_host"
  export skanmotovrig_sftp_host=/var/run/secrets/nais.io/skanmotovrig/skanmotovrig_sftp_host
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_sftp_port; then
  echo "Setting skanmotovrig_sftp_port"
  export skanmotovrig_sftp_port=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_sftp_port)
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_sftp_username; then
  echo "Setting skanmotovrig_sftp_username"
  export skanmotovrig_sftp_username=$(cat /var/run/secrets/nais.io/skanmotovrig/skanmotovrig_sftp_username)
fi